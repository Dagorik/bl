package com.dagorik.bluetooth.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

abstract class BluetoothDeviceFounderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        val action = intent?.action

        if (action == BluetoothDevice.ACTION_FOUND) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            getFoundDevices(device)
        }
    }

    abstract fun getFoundDevices(device: BluetoothDevice)
}