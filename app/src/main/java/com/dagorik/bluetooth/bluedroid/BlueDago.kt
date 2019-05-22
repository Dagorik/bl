package com.dagorik.bluetooth.bluedroid

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.nsd.NsdManager
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.dagorik.bluetooth.R
import java.util.ArrayList

class BlueDago(
    private var context: Context, private var device: ConnectionDevice, private var  type: ConnectionSecure
) {



    val REQUEST_COARSE_LOCATION_PERMISSIONS = 0x00BB

    private val TAG = "TAG"
    private var mContext: Context
    private val mAdapter = BlueDagoAdapter()
    private var mConnectionDevice: ConnectionDevice?
    private var mConnectionSecure: ConnectionSecure
    private val mDevices = ArrayList<Device>(32)
    private val discoveryListener = ArrayList<DiscoveryListener>()
    private val dataReceivedListener = ArrayList<DataReceivedListener>()
    private val connectionListener = ArrayList<ConnectionListener>()


    private var mBtAdapter: BluetoothAdapter?
    private var mBtService: BlueService? = null
    private var mCurrentDevice: Device? = null
    private var isServiceRunning = false
    private var isConnecting = false
    private var isConnected = false


    init {
        mContext = context
        mConnectionDevice = device
        mConnectionSecure = type
        mBtAdapter = BluetoothAdapter.getDefaultAdapter()
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d(TAG, "BlueDroid.mReceiver.onReceive()")
            val action = intent.action

            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED == action) {
                mCurrentDevice = null
                mDevices.clear()
                mAdapter.notifyDataSetChanged()
                fireOnDiscoveryStarted()
            } else if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceClass = device.bluetoothClass.deviceClass
                val newDevice = Device(device.name ?: "Sin nombre", device.address, false, deviceClass)
                mDevices.add(newDevice)
                mAdapter.notifyDataSetChanged()
                fireOnDeviceFound(newDevice)
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                mContext.unregisterReceiver(this)
                fireOnDiscoveryFinished()
                if (mDevices.size == 0) {
                    fireOnNoDevicesFound()
                }
            }
        }
    }


    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            Log.d(TAG, "BlueDroid.mHandler.handleMessage(" + msg.what + ")")
            when (msg.what) {
                BlueService.MESSAGE_WRITE -> {
                }
                BlueService.MESSAGE_READ -> {
                    val data = (msg.obj as Int).toByte()
                    fireOnDataReceived(data)
                }
                BlueService.MESSAGE_DEVICE_NAME -> {
                    fireOnDeviceConnected()
                    isConnected = true
                }
                BlueService.MESSAGE_STATE_CHANGE -> {
                    if (isConnected && msg.arg1 != BlueService.STATE_CONNECTED) {
                        isConnected = false
                        fireOnDeviceDisconnected()
                        mCurrentDevice = null
                    }
                    if (!isConnecting && msg.arg1 == BlueService.STATE_CONNECTING) {
                        isConnecting = true
                        fireOnDeviceConnecting()
                    } else if (isConnecting) {
                        isConnecting = false
                        if (msg.arg1 != BlueService.STATE_CONNECTED) {
                            fireOnDeviceConnectionFailed()
                            mCurrentDevice = null
                        }
                    }
                }
            }
        }
    }



    fun getAdapter(): BaseAdapter {
        return mAdapter
    }


    fun getDevices(): List<Device> {
        return mDevices
    }


    fun isAvailable(): Boolean {
        Log.d(TAG, "BlueDroid.isAvailable()")
        try {
            return mBtAdapter != null
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

    }


    fun getCurrentDevice(): Device? {
        return mCurrentDevice
    }


    fun isServiceAvailable(): Boolean {
        Log.d(TAG, "BlueDroid.isServiceAvailable()")
        return mBtService != null
    }


    fun isEnabled(): Boolean {
        Log.d(TAG, "BlueDroid.isEnabled()")
        Log.d(TAG, mBtAdapter!!.address)
        return mBtAdapter!!.address != null && mBtAdapter!!.isEnabled
    }


    fun isServiceRunning(): Boolean {
        return isServiceRunning
    }


    fun startDiscovery(): Boolean {
        Log.d(TAG, "BlueDroid.startDiscovery()")
        return mBtAdapter!!.startDiscovery()
    }


    fun isDiscovering(): Boolean {
        Log.d(TAG, "BlueDroid.isDiscovering()")
        return mBtAdapter!!.isDiscovering
    }


    fun cancelDiscovery(): Boolean {
        Log.d(TAG, "BlueDroid.cancelDiscovery()")
        return mBtAdapter!!.cancelDiscovery()
    }


    fun getBluetoothAdapter(): BluetoothAdapter? {
        Log.d(TAG, "BlueDroid.getBluetoothAdapter()")
        return mBtAdapter
    }

    private fun setupService() {
        Log.d(TAG, "BlueDroid.setupService()")
        mBtService = BlueService(mHandler)
    }

    private fun startService() {
        Log.d(TAG, "BlueDroid.startService()")
        if (isServiceAvailable()) {
            if (mBtService!!.getState() === BlueService.STATE_NONE) {
                isServiceRunning = true
                mBtService!!.start(
                    mConnectionDevice === ConnectionDevice.ANDROID,
                    mConnectionSecure === ConnectionSecure.SECURE
                )
            }
        }
    }


    fun stop() {
        Log.d(TAG, "BlueDroid.stop()")
        mCurrentDevice = null
        if (isServiceAvailable()) {
            isServiceRunning = false
            mBtService!!.stop()
        }

        Handler().postDelayed({
            if (isServiceAvailable()) {
                isServiceRunning = false
                mBtService!!.stop()
            }
        }, 500)
    }


    fun connect(device: Device?) {
        if (device != null) {
            mCurrentDevice = device
            connect(device.getAddress()!!)
        }
    }

    private fun connect(address: String) {
        Log.d(TAG, "BlueDroid.connect($address)")
        if (isConnecting) {
            return
        }
        if (!isServiceAvailable()) {
            setupService()
        }

        startService()

        if (BluetoothAdapter.checkBluetoothAddress(address)) {
            val device = mBtAdapter!!.getRemoteDevice(address)
            mBtService!!.connect(device)
        }
    }


    fun isConnected(): Boolean {
        Log.d(TAG, "BlueDroid.isConnected()")
        return isConnected && mConnectionDevice != null
    }


    fun isConnecting(): Boolean {
        Log.d(TAG, "BlueDroid.isConnecting()")
        return isConnecting
    }


    fun enable() {
        Log.d(TAG, "BlueDroid.enable()")
        mBtAdapter!!.enable()
    }


    fun disconnect() {
        Log.d(TAG, "BlueDroid.disconnect()")
        mCurrentDevice = null

        if (isServiceAvailable()) {
            isServiceRunning = false
            mBtService!!.stop()
            if (mBtService!!.getState() === BlueService.STATE_NONE) {
                isServiceRunning = true
                mBtService!!.start(
                    mConnectionDevice === ConnectionDevice.ANDROID,
                    mConnectionSecure === ConnectionSecure.SECURE
                )
            }
        }
    }

    fun getState(): Int {
        return mBtService!!.getState()
    }


    fun send(data: ByteArray, lbt: LineBreakType) {
        send(data, 0, data.size, lbt)
    }


    fun send(data: ByteArray, off: Int, len: Int, lbt: LineBreakType) {
        if (lbt.value === LineBreakType.NONE.value) {
            send(data, off, len)
        } else {
            val tmp = ByteArray(len + 2)
            System.arraycopy(data, off, tmp, 0, len)
            tmp[tmp.size - 1] = 0x0D // CR
            tmp[tmp.size - 2] = 0x0A // LF
            if (lbt.value === LineBreakType.LF.value) {
                tmp[tmp.size - 1] = 0x0A // LF
                send(tmp, 0, len + 1)
            } else if (lbt.value === LineBreakType.CR.value) {
                send(tmp, 0, len + 1)
            } else if (lbt.value === LineBreakType.CRLF.value) {
                send(tmp)
            }
        }
    }


    fun send(data: ByteArray, off: Int, len: Int) {
        mBtService!!.write(data, off, len)
    }


    fun send(data: ByteArray) {
        mBtService!!.write(data)
    }


    fun send(b: Int) {
        mBtService!!.write(b)
    }

    fun checkDiscoveryPermissionRequest(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_COARSE_LOCATION_PERMISSIONS -> {
                if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    doDiscovery(null)
                } else {
                    fireOnDiscoveryFailed()
                }

                return
            }
        }
    }


    fun doDiscovery(activity: Activity?) {
        Log.d(TAG, "BlueDroid.doDiscovery()")

        val hasPermission = ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (hasPermission != PackageManager.PERMISSION_GRANTED) {
            if (activity != null) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                    REQUEST_COARSE_LOCATION_PERMISSIONS
                )
            }

            return
        }

        if (isDiscovering()) {
            mContext.unregisterReceiver(mReceiver)
            cancelDiscovery()
        }

        mContext.registerReceiver(mReceiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        mContext.registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED))
        mContext.registerReceiver(mReceiver, IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED))

        startDiscovery()
    }

    protected fun fireOnDiscoveryStarted() {
        for (listener in discoveryListener) listener.onDiscoveryStarted()
    }

    protected fun fireOnDiscoveryFinished() {
        for (listener in discoveryListener) listener.onDiscoveryFinished()
    }

    protected fun fireOnNoDevicesFound() {
        for (listener in discoveryListener) listener.onNoDevicesFound()
    }

    protected fun fireOnDeviceFound(dev: Device) {
        for (listener in discoveryListener) listener.onDeviceFound(dev)
    }

    protected fun fireOnDiscoveryFailed() {
        for (listener in discoveryListener) listener.onDiscoveryFailed()
    }

    fun addDiscoveryListener(listener: DiscoveryListener) {
        if (!discoveryListener.contains(listener)) {
            discoveryListener.add(listener)
        }
    }

    fun removeDiscoveryListener(listener: DiscoveryListener) {
        discoveryListener.remove(listener)
    }

    fun clearDiscoveryListener() {
        discoveryListener.clear()
    }

    protected fun fireOnDataReceived(data: Byte) {
        for (listener in dataReceivedListener) listener.onDataReceived(data)
    }

    fun addDataReceivedListener(listener: DataReceivedListener) {
        if (!dataReceivedListener.contains(listener)) {
            dataReceivedListener.add(listener)
        }
    }

    fun removeDataReceivedListener(listener: DataReceivedListener) {
        dataReceivedListener.remove(listener)
    }

    fun clearDataReceivedListener() {
        dataReceivedListener.clear()
    }

    protected fun fireOnDeviceConnecting() {
        for (listener in connectionListener) listener.onDeviceConnecting()
    }

    protected fun fireOnDeviceConnected() {
        for (listener in connectionListener) listener.onDeviceConnected()
    }

    protected fun fireOnDeviceDisconnected() {
        for (listener in connectionListener) listener.onDeviceDisconnected()
    }

    protected fun fireOnDeviceConnectionFailed() {
        for (listener in connectionListener) listener.onDeviceConnectionFailed()
    }

    fun addConnectionListener(listener: ConnectionListener) {
        if (!connectionListener.contains(listener)) {
            connectionListener.add(listener)
        }
    }

    fun removeConnectionListener(listener: ConnectionListener) {
        connectionListener.remove(listener)
    }

    fun clearConnectionListener() {
        connectionListener.clear()
    }

    interface DiscoveryListener {
        fun onDiscoveryStarted()

        fun onDiscoveryFinished()

        fun onNoDevicesFound()

        fun onDeviceFound(device: Device)

        fun onDiscoveryFailed()
    }

    interface DataReceivedListener {
        fun onDataReceived(data: Byte)
    }

    interface ConnectionListener {
        fun onDeviceConnecting()

        fun onDeviceConnected()

        fun onDeviceDisconnected()

        fun onDeviceConnectionFailed()
    }


    private inner class BlueDagoAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return getDevices().size
        }

        override fun getItem(position: Int): Any {
            return getDevices().get(position)
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, v: View?, parent: ViewGroup): View {
            var v = v
            if (v == null) {
                v = LayoutInflater.from(mContext).inflate(R.layout.device_item, parent, false)
            }

            val device = getDevices().get(position)

            v!!.tag = device
            (v.findViewById(R.id.bt_device_icon) as ImageView).setImageResource(device.getDeviceClassIcon())
            (v.findViewById(R.id.bt_device_name) as TextView).setText(device.getName())
            (v.findViewById(R.id.bt_device_address) as TextView).setText(device.getAddress())

            return v
        }
    }
}

