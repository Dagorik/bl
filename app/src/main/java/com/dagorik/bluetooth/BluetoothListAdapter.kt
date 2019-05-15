package com.dagorik.bluetooth

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_bluetooth_device.view.*
import java.util.ArrayList


class BluetoothListAdapter(private var item: ArrayList<BluetoothDevice>, val clickListener: OnClickListener) :
    RecyclerView.Adapter<BluetoothListAdapter.ViewHolder>() {


    companion object {
        var mClickListener: OnClickListener? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bluetooth_device, parent, false)
        return ViewHolder(view as ConstraintLayout)
    }

    override fun getItemCount() = item.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        mClickListener = clickListener
        holder.view.apply {
            device_name.text = item[position].name
            macAddress.text = item[position].address
        }

        holder.view.setOnClickListener {
            mClickListener?.onClick(item[position])
        }

    }


    class ViewHolder(val view: ConstraintLayout) : RecyclerView.ViewHolder(view)
}
