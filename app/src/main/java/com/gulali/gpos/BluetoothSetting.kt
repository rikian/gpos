package com.gulali.gpos

import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.gulali.gpos.adapter.BluetoothListAdapter
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.database.Repository
import com.gulali.gpos.databinding.BluetoothSettingsBinding
import com.gulali.gpos.helper.BluetoothReceiver
import com.gulali.gpos.helper.Helper
import com.gulali.gpos.helper.Permission

class BluetoothSetting : AppCompatActivity(), BluetoothReceiver.BluetoothStateListener {
    private lateinit var binding: BluetoothSettingsBinding
    private lateinit var bluetoothListAdapter: BluetoothListAdapter
    private lateinit var helper: Helper
    private lateinit var permission: Permission
    private lateinit var gposRepo: Repository
    private lateinit var ownerEntity: OwnerEntity
    private lateinit var requestBluetoothPermission: ActivityResultLauncher<String>
    private lateinit var bluetoothReceiver: BluetoothReceiver
    private var alertDialog: AlertDialog? = null
    private var pairedChooser: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BluetoothSettingsBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)

            // Initialize the ActivityResultLauncher
            requestBluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission())
            { isGranted: Boolean -> return@registerForActivityResult if (isGranted) runBluetoothOperation() else finish() }

            binding.addPairedDevice.setOnClickListener { openBluetoothSetting() }

            helper = Helper()
            permission = Permission(this@BluetoothSetting, requestBluetoothPermission)
            gposRepo = AdapterDb.getGposDatabase(this).repository()
            ownerEntity = gposRepo.getOwner()[0]
            binding.printerName.text = ownerEntity.bluetoothPaired
            binding.setUpdate.setOnClickListener {
                if (pairedChooser == "") {
                    return@setOnClickListener helper.generateTOA(this@BluetoothSetting, "Please select one of paired devices in list before update", true)
                }
                gposRepo.updateBluetooth(pairedChooser)
                helper.generateTOA(this@BluetoothSetting, "Updated", true)
                finish()
            }
            bluetoothReceiver = BluetoothReceiver(this)
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(bluetoothReceiver, filter)
        }
    }

    override fun onResume() {
        super.onResume()
        runBluetoothOperation()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    private fun runBluetoothOperation() {
        // Check if Bluetooth permission is already granted
        if (!permission.checkBluetoothPermission()) return
        val bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) finish()
        if (!bluetoothAdapter.isEnabled) showDialogTurnOnBluetooth(this)
        val pairedDeviceArr: MutableList<String> = mutableListOf()
        val pairedDevices = bluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            pairedDeviceArr.add(device.name)
        }
        showListPairedDevice(this, pairedDeviceArr)
    }

    private fun openBluetoothSetting() {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
        this.startActivity(intent)
    }

    private fun showDialogTurnOnBluetooth(ctx: Context) {
        val builder = AlertDialog.Builder(ctx)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_open_bluetooth, null)
        val btnOk = dialogLayout.findViewById<Button>(R.id.dbt_yes)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.dbt_no)
        builder.setView(dialogLayout)
        alertDialog = builder.create() // Create the AlertDialog
        alertDialog?.setCanceledOnTouchOutside(false)
        alertDialog?.setOnCancelListener{
            alertDialog?.dismiss()
            finish()
        }
        btnOk.setOnClickListener {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            this.startActivity(intent)
            alertDialog?.dismiss() // Dismiss the dialog after starting the Bluetooth settings activity
        }
        btnCancel.setOnClickListener {
            alertDialog?.dismiss()
            finish()
        }
        alertDialog?.show() // Show the AlertDialog after configuring it
    }

    override fun onBluetoothConnected() {
        if (alertDialog != null && alertDialog!!.isShowing) {
            alertDialog?.dismiss()
        }
    }

    override fun onBluetoothDisconnected() {
        showDialogTurnOnBluetooth(this)
    }

    override fun onBluetoothActionError(action: String) {}

    override fun onBluetoothStateError(state: String) {}

    private fun showListPairedDevice(ctx: Context, item: List<String>) {
        bluetoothListAdapter = BluetoothListAdapter(ctx, item)
        bluetoothListAdapter.setOnItemClickListener(object : BluetoothListAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val bluetoothName = item[position]
                binding.printerName.text = bluetoothName
                pairedChooser = bluetoothName
                return
            }
        })
        binding.recyclerView.adapter = bluetoothListAdapter
    }
}