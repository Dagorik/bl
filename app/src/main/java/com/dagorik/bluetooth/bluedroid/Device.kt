package com.dagorik.bluetooth.bluedroid

import android.bluetooth.BluetoothClass
import android.util.Log
import com.dagorik.bluetooth.R

class Device(
    private var mName: String = "",
    private var mAddress: String? = null,
    private var isPaired: Boolean = false,
    private var mDeviceClass: Int = 0
) {

    fun getName(): String {
        return mName
    }

    fun getAddress(): String? {
        return mAddress
    }

    fun isPaired(): Boolean {
        return isPaired
    }

    fun getDeviceClass(): Int {
        return mDeviceClass
    }

    fun getDeviceClassIcon(): Int {
        Log.d("TAG", "Device.getDeviceClass() = " + getDeviceClass())

        val deviceClass = getDeviceClass()
        val deviceClassMasked = deviceClass and 0x1F00

        return if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES) {
            R.drawable.headphone
        } else if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_MICROPHONE) {
            R.drawable.microphone
        } else if (deviceClassMasked == BluetoothClass.Device.Major.COMPUTER) {
            R.drawable.computer
        } else if (deviceClassMasked == BluetoothClass.Device.Major.PHONE) {
            R.drawable.cell_phone
        } else if (deviceClassMasked == BluetoothClass.Device.Major.HEALTH) {
            R.drawable.heart
        } else {
            R.drawable.bluetooth
        }
    }

    override fun hashCode(): Int {
        return mAddress?.hashCode() ?: super.hashCode()
    }

    override fun equals(o: Any?): Boolean {
        return o is Device && getAddress() === o.getAddress()
    }
}