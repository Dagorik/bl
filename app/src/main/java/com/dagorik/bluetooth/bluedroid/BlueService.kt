package com.dagorik.bluetooth.bluedroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BlueService(
  private val handler: Handler
) {

    companion object {
        public val STATE_NONE = 0
        public val STATE_LISTEN = 1
        public val STATE_CONNECTING = 2
        public val STATE_CONNECTED = 3
        public val MESSAGE_STATE_CHANGE = 1
        public val MESSAGE_READ = 2
        public val MESSAGE_WRITE = 3
        public val MESSAGE_DEVICE_NAME = 4
    }


    private val TAG = "TAG"
    private val NAME_SECURE = "Bluetooth Secure"

    private val UUID_ANDROID_DEVICE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val UUID_OTHER_DEVICE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val mAdapter: BluetoothAdapter
    private val mHandler: Handler

    private var mSecureAcceptThread: AcceptThread? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mState: Int = 0

    private var isAndroid: Boolean = false
    private var isSecure = true


    init {
        mAdapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
        mHandler = handler
    }

    @Synchronized
    fun getState(): Int {
        Log.d("TAG", "BlueService.getState()=$mState")
        return mState
    }

    @Synchronized
    private fun setState(state: Int) {
        Log.d("TAG", "BlueService.setState($state)")
        mState = state
        mHandler.obtainMessage(MESSAGE_STATE_CHANGE, state, -1).sendToTarget()
    }

    @Synchronized
    fun start(android: Boolean, secure: Boolean) {
        Log.d("TAG", "BlueService.start($android, $secure)")
        isAndroid = android
        isSecure = secure

        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        setState(STATE_LISTEN)

        if (mSecureAcceptThread == null) {
            mSecureAcceptThread = AcceptThread(isAndroid, isSecure)
            mSecureAcceptThread!!.start()
        }
    }

    @Synchronized
    fun connect(device: BluetoothDevice) {
        Log.d("TAG", "BlueService.connect()")
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread!!.cancel()
                mConnectThread = null
            }
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        mConnectThread = ConnectThread(device)
        mConnectThread!!.start()
        setState(STATE_CONNECTING)
    }

    @Synchronized
    fun connected(socket: BluetoothSocket, device: BluetoothDevice, socketType: String) {
        Log.d("TAG", "BlueService.connected()")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread = null
        }

        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread!!.start()

        val msg = mHandler.obtainMessage(MESSAGE_DEVICE_NAME)
        mHandler.sendMessage(msg)

        setState(STATE_CONNECTED)
    }

    @Synchronized
    fun stop() {
        Log.d("TAG", "BlueService.stop()")
        if (mConnectThread != null) {
            mConnectThread!!.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread!!.cancel()
            mConnectedThread = null
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread!!.cancel()
            mSecureAcceptThread!!.kill()
            mSecureAcceptThread = null
        }
        setState(STATE_NONE)
    }

    fun write(b: Int) {
        val r: ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread!!
        }
        r.write(b)
    }

    fun write(out: ByteArray) {
        val r: ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread!!
        }
        r.write(out)
    }

    fun write(out: ByteArray, off: Int, len: Int) {
        val r: ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread!!
        }
        r.write(out, off, len)
    }

    private fun connectionFailed() {
        Log.d("TAG", "BlueService.connectionFailed()")
        this@BlueService.start(isAndroid, isSecure)
    }

    private fun connectionLost() {
        Log.d("TAG", "BlueService.connectionLost()")
        this@BlueService.start(isAndroid, isSecure)
    }

    private inner class AcceptThread(isAndroid: Boolean, secure: Boolean) : Thread() {
        internal var isRunning = true
        private var mmServerSocket: BluetoothServerSocket? = null
        private val mSocketType: String? = null

        init {
            var tmp: BluetoothServerSocket? = null

            try {
                if (secure) {
                    if (isAndroid)
                        tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, UUID_ANDROID_DEVICE)
                    else
                        tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, UUID_OTHER_DEVICE)
                } else {
                    if (isAndroid)
                        tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_SECURE, UUID_ANDROID_DEVICE)
                    else
                        tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_SECURE, UUID_OTHER_DEVICE)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            mmServerSocket = tmp
        }

        override fun run() {
            Log.d(TAG, "BlueService\$AcceptThread.run()")
            name = "AcceptThread" + mSocketType
            var socket: BluetoothSocket?

            while (mState != STATE_CONNECTED && isRunning) {
                try {
                    socket = mmServerSocket!!.accept()
                } catch (e: Exception) {
                    break
                }

                //Se uma conexão foi aceita.
                if (socket != null) {
                    synchronized(this@BlueService) {
                        when (mState) {
                            STATE_LISTEN, STATE_CONNECTING -> connected(socket, socket.remoteDevice, mSocketType!!)
                            STATE_NONE, STATE_CONNECTED -> try {
                                socket.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }

                        }
                    }
                }
            }
        }

        fun cancel() {
            Log.d(TAG, "BlueService\$AcceptThread.cancel()")
            try {
                mmServerSocket!!.close()
                mmServerSocket = null
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

        fun kill() {
            Log.d(TAG, "BlueService\$AcceptThread.kill()")
            isRunning = false
        }
    }


    private inner class ConnectThread(private val mmDevice: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String? = null

        init {
            var tmp: BluetoothSocket? = null

            try {
                if (isSecure) {
                    if (isAndroid) {
                        tmp = mmDevice.createRfcommSocketToServiceRecord(UUID_ANDROID_DEVICE)
                    } else {
                        tmp = mmDevice.createRfcommSocketToServiceRecord(UUID_OTHER_DEVICE)
                    }
                } else {
                    if (isAndroid) {
                        tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(UUID_ANDROID_DEVICE)
                    } else {
                        tmp = mmDevice.createInsecureRfcommSocketToServiceRecord(UUID_OTHER_DEVICE)
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            mmSocket = tmp
        }

        override fun run() {
            Log.d(TAG, "BlueService\$ConnectThread.run()")
            mAdapter.cancelDiscovery()

            try {
                mmSocket!!.connect()
            } catch (e: IOException) {
                try {
                    mmSocket!!.close()
                } catch (e2: IOException) {
                    e2.printStackTrace()
                }

                connectionFailed()
                return
            }

            synchronized(this@BlueService) {
                mConnectThread = null
            }

            mmSocket
            mmDevice
            mSocketType
            connected(mmSocket, mmDevice, "")
        }

        fun cancel() {
            Log.d(TAG, "BlueService\$ConnectThread.cancel()")
            try {
                mmSocket!!.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket, socketType: String) : Thread() {
        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            try {
                tmpIn = mmSocket.inputStream
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
        }

        override fun run() {
            Log.d(TAG, "BlueService\$ConnectedThread.run()")
            while (true) {
                try {
                    val data = mmInStream!!.read()
                    mHandler.obtainMessage(MESSAGE_READ, data).sendToTarget()
                } catch (e: IOException) {
                    connectionLost()
                    this@BlueService.start(isAndroid, isSecure)
                    break
                }

            }
        }

        fun write(b: Int) {
            try {
                mmOutStream!!.write(b)
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, b).sendToTarget()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        @JvmOverloads
        fun write(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size) {
            try {
                mmOutStream!!.write(buffer, offset, length)
                mHandler.obtainMessage(MESSAGE_WRITE, -1, -1, buffer).sendToTarget()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        fun cancel() {
            Log.d(TAG, "BlueService\$ConnectedThread.cancel()")
            try {
                mmSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
    }

}