package com.gulali.gpos

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.gulali.gpos.databinding.PrintBinding
import java.io.IOException
import java.util.UUID

class Printer: AppCompatActivity() {
    private lateinit var binding: PrintBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrintBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
        }

        binding.btnPrint.setOnClickListener {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 100)
                    } else {
                        return@setOnClickListener
                    }
                }

                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter == null) {
                    // Device does not support Bluetooth
                    Toast.makeText(applicationContext, "your device not support bluetooth", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (!bluetoothAdapter.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 100)
                    }
                    startActivityForResult(enableBtIntent, 1)
                    return@setOnClickListener
                }

                val deviceName = "RPP02"
                var deviceAddress = ""
                val pairedDevices = bluetoothAdapter.bondedDevices
                for (device in pairedDevices) {
                    if (device.name == deviceName) {
                        deviceAddress = device.address.toString()
                        // Pair with the printer using the deviceAddress
                        // You can use this deviceAddress to establish a connection later
                        break
                    }
                }

                if (deviceAddress == "") {
                    return@setOnClickListener
                }

                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                val socket = device.createRfcommSocketToServiceRecord(uuid)

                socket.connect()
                // You're now connected to the printer

                val outputStream = socket.outputStream
                val data = "${binding.editForPrint.text}".toByteArray()
                outputStream.write(data)
                outputStream.flush()
                outputStream.close()
            } catch (e: IOException) {
                // Handle connection errors
                println(e.message)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {

        }
    }
}