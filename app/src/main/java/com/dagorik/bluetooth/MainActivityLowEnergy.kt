package com.dagorik.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.robotpajamas.blueteeth.BlueteethDevice
import com.robotpajamas.blueteeth.BlueteethManager
import androidx.core.app.ActivityCompat
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog


class MainActivityLowEnergy : AppCompatActivity(), OnClickListener {

    private lateinit var viewAdapter: RecyclerView.Adapter<*>

    private var itemList = ArrayList<BlueteethDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        viewAdapter = BluetoothListAdapter(itemList,this)


        recycler_view.apply {
            layoutManager = LinearLayoutManager(this@MainActivityLowEnergy)
            adapter = viewAdapter
        }

        BlueteethManager.with(this).scanForPeripherals(20000) { blueteethDevices ->
            // Scan completed, iterate through received devices and log their name/mac address
            Log.e("All device",blueteethDevices.toString())
            for (device in blueteethDevices) {
                if (!TextUtils.isEmpty(device.bluetoothDevice.name)) {
                    Log.e("Encontrado", device.name + " " + device.macAddress)
                    itemList.add(device)
                    viewAdapter.notifyDataSetChanged()
                } else {
                    Log.e("EncontradoN", device.macAddress)
                }
            }
        }

    }

    override fun onClick(bluetoothDeviceModel: BluetoothDevice) {
        Log.e("CLICK",bluetoothDeviceModel.name)

//        bluetoothDeviceModel.connect(true) { isConnected ->
//            Log.e("Connecy","Exitoo" + isConnected)
//
//            bluetoothDeviceModel.discoverServices{ response ->
//                Log.e("DISCOVER",response.toString())
//            }
//        }


    }

}
