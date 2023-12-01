package com.gulali.gpos

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
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
    private lateinit var ownerEntity: OwnerEntity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SettingBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            gposRepo = AdapterDb.getGposDatabase(this).repository()
            ownerEntity = gposRepo.getOwner()[0]
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

        binding.setUpdate.setOnClickListener {
            val bluetoothPaired = binding.printerName.text.toString()
            gposRepo.updateBluetooth(bluetoothPaired)
            Toast.makeText(applicationContext, "updated bluetooth", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val pairedDevice = getPairedDevice()
        if (pairedDevice.size == 0) {
            Toast.makeText(applicationContext, "device paired not found", Toast.LENGTH_SHORT).show()
            return
        }

        showListPairedDevice(this, pairedDevice)
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

    private fun showListPairedDevice(ctx: Context, item: List<String>) {
        bluetoothListAdapter = BluetoothListAdapter(ctx, item)
        bluetoothListAdapter.setOnItemClickListener(object : BluetoothListAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val bluetoothName = item[position]
                binding.printerName.text = bluetoothName
                return
            }
        })
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