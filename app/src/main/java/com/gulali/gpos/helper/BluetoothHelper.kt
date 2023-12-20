package com.gulali.gpos.helper

import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import com.gulali.gpos.R

class BluetoothHelper(
    private var ctx: Context,
    private var permission: Permission,
    private var bluetoothManager: BluetoothManager,
    private var layoutInflater: LayoutInflater,
    private var alertDialog: AlertDialog? = null
) {
    private fun runBluetoothOperation() {
        // Check if Bluetooth permission is already granted
        if (!permission.checkBluetoothPermission()) return
        bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter ?: return
        if (!bluetoothAdapter.isEnabled) showDialogTurnOnBluetooth(ctx)
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
        }
        btnOk.setOnClickListener {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            ctx.startActivity(intent)
            alertDialog?.dismiss() // Dismiss the dialog after starting the Bluetooth settings activity
        }
        btnCancel.setOnClickListener {
            alertDialog?.dismiss()
        }
        alertDialog?.show() // Show the AlertDialog after configuring it
    }
}