package com.dagorik.bluetooth

import android.bluetooth.BluetoothDevice

interface OnClickListener {
    fun onClick(bluetoothDeviceModel: BluetoothDevice)
}