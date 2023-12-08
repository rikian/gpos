package com.gulali.gpos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gulali.gpos.R

class BluetoothListAdapter(
    private val context: Context,
    private val items: List<String>
): RecyclerView.Adapter<BluetoothListAdapter.BluetoothViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null
    private var listBluetooth: List<String> = items

    init {
        this.listBluetooth = items
    }

    inner class BluetoothViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var pDevice: TextView = itemView.findViewById(R.id.device_paired)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_paired_divices, parent, false)
        val viewHolder = BluetoothViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition)
        }
        return BluetoothViewHolder(view)
    }

    override fun onBindViewHolder(holder: BluetoothViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }

        holder.pDevice.text = items[position]

    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}