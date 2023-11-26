package com.gulali.gpos

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.gulali.gpos.adapter.BluetoothListAdapter
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.database.Repository
import com.gulali.gpos.databinding.SettingBinding

class Setting : AppCompatActivity() {
    private lateinit var binding: SettingBinding
    private lateinit var bluetoothListAdapter: BluetoothListAdapter
    private lateinit var gposRepo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            gposRepo = AdapterDb.getGposDatabase(this).repository()
        }

        binding.btngetpired.setOnClickListener {
            if (!checkPermissionBluetooth()) {
                return@setOnClickListener
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
                    return@setOnClickListener
                }
                startActivityForResult(enableBtIntent, 1)
                return@setOnClickListener
            }

            // open bluetooth for paired
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            this.startActivity(intent)
        }

        binding.setSave.setOnClickListener {
            val data = OwnerEntity(
                id = "001",
                shop = "Untung Melulu",
                owner = "Sanusi",
                address= "JL. Terus Belok Kiri" ,
                phone= "0812345678",
                discountPercent= 0.0,
                discountNominal= 1000,
                adm= 1000,
                bluetoothPaired = "",
                createdAt= "",
                updatedAt= "",
            )
            gposRepo.createOwner(data)
        }

        binding.setUpdate.setOnClickListener {  }
    }

    override fun onResume() {
        super.onResume()
        val pairedDevice = getPairedDevice()
        if (pairedDevice.size == 0) {
            Toast.makeText(applicationContext, "device paired not found", Toast.LENGTH_SHORT).show()
            return
        }

        showListPairedDevice(pairedDevice)
    }

    private fun getPairedDevice(): MutableList<String> {
        val pairedDevice: MutableList<String> = mutableListOf()

        if (!checkPermissionBluetooth()) {
            return pairedDevice
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(applicationContext, "your device not support bluetooth", Toast.LENGTH_SHORT).show()
            return pairedDevice
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 100)
                return pairedDevice
            }
            startActivityForResult(enableBtIntent, 1)
            return pairedDevice
        }

        val pairedDevices = bluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            pairedDevice.add(device.name)
        }
        return pairedDevice
    }

    private fun showListPairedDevice(item: List<String>) {
        bluetoothListAdapter = BluetoothListAdapter(item)
        binding.recyclerView.adapter = bluetoothListAdapter
    }

    private fun checkPermissionBluetooth(): Boolean {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 100)
                false
            } else {
                false
            }
        }

        return true
    }
}