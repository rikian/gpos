package com.gulali.gpos.helper

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothReceiver(private val listener: BluetoothStateListener) : BroadcastReceiver() {
    interface BluetoothStateListener {
        fun onBluetoothConnected()
        fun onBluetoothDisconnected()
        fun onBluetoothActionError(action: String)
        fun onBluetoothStateError(state: String)
    }
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        when (val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
            BluetoothAdapter.STATE_TURNING_ON -> {
                // Bluetooth is connected
                listener.onBluetoothConnected()
            }
            BluetoothAdapter.STATE_TURNING_OFF -> {
                // Bluetooth is disconnected
                listener.onBluetoothDisconnected()
            }
            else -> {
                // There is something wrong with bluetooth state
                val stateMessage = getStateMessage(state)
                listener.onBluetoothStateError(stateMessage)
            }
        }
//        when (action) {
//            BluetoothAdapter.ACTION_STATE_CHANGED -> {
//
//            }
//            else -> {
//                // There is something wrong with bluetooth action
//                listener.onBluetoothActionError(action)
//            }
//        }
    }

    private fun getStateMessage(state: Int): String {
        return when (state) {
            BluetoothAdapter.STATE_TURNING_ON -> "Bluetooth is turning on"
            BluetoothAdapter.STATE_TURNING_OFF -> "Bluetooth is turning off"
            BluetoothAdapter.STATE_ON -> "Bluetooth is on"
            BluetoothAdapter.STATE_OFF -> "Bluetooth is off"
            else -> "Unknown Bluetooth state"
        }
    }
}