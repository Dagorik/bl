package com.dagorik.bluetooth.bluedroid

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ListView
import android.widget.ProgressBar
import androidx.annotation.NonNull
import com.dagorik.bluetooth.R

class BlueDiscoveryDialog(@param:NonNull private val mActivity: Activity, private val mBluetooth: BlueDago) :
    AlertDialog(mActivity, false, null), BlueDago.ConnectionListener, BlueDago.DiscoveryListener {

    var mView: View? = null

    init {

        mView = LayoutInflater.from(mActivity).inflate(R.layout.dialog_bluetooth_discovery, null, false)
        getWindow().setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setView(mView)

        mBluetooth.addConnectionListener(this)
        mBluetooth.addDiscoveryListener(this)

        (mView!!.findViewById(R.id.device_list_view) as ListView).adapter = mBluetooth.getAdapter()
        (mView!!.findViewById(R.id.device_list_view) as ListView).dividerHeight = 0
        (mView!!.findViewById(R.id.device_list_view) as ListView).onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                if (!mBluetooth.isConnected()) {
                    val device = view.tag as Device
                    mBluetooth.connect(device)
                }
            }

        mView!!.findViewById<Button>(R.id.scan_button)
            .setOnClickListener(View.OnClickListener { mBluetooth.doDiscovery(mActivity) })

        mView!!.findViewById<Button>(R.id.cancel_button).setOnClickListener(View.OnClickListener {
            mBluetooth.cancelDiscovery()
            dismiss()
        })

        mView!!.findViewById<Button>(R.id.ok_button).setOnClickListener(View.OnClickListener {
            mBluetooth.cancelDiscovery()
            dismiss()
        })
    }

    override fun dismiss() {
        mBluetooth.removeDiscoveryListener(this)
        mBluetooth.removeConnectionListener(this)
        super.dismiss()
    }

    override fun onDeviceConnecting() {}

    override fun onDeviceConnected() {
        dismiss()
    }

    override fun onDeviceDisconnected() {}

    override fun onDeviceConnectionFailed() {}

    override fun onDiscoveryStarted() {
        mView!!.findViewById<ProgressBar>(R.id.progress).setVisibility(View.VISIBLE)
    }

    override fun onDiscoveryFinished() {
        mView!!.findViewById<ProgressBar>(R.id.progress).setVisibility(View.INVISIBLE)
    }

    override fun onNoDevicesFound() {}

    override fun onDeviceFound(device: Device) {}

    override fun onDiscoveryFailed() {}
}