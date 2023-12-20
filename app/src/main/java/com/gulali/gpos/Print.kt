package com.gulali.gpos

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.database.ProductTransaction
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.databinding.PrintBinding
import com.gulali.gpos.helper.BluetoothReceiver
import com.gulali.gpos.helper.Helper
import com.gulali.gpos.helper.Permission
import com.gulali.gpos.helper.Printer
import java.util.UUID

class Print: AppCompatActivity(), BluetoothReceiver.BluetoothStateListener {
    private lateinit var binding: PrintBinding
    private lateinit var helper: Helper
    private lateinit var printer: Printer
    private lateinit var constant: Constant
    private lateinit var permission: Permission
    private lateinit var gposRepo: Repository
    private lateinit var owner: OwnerEntity
    private lateinit var bluetoothReceiver: BluetoothReceiver
    private lateinit var bluetoothFilter: IntentFilter
    private var alertDialog: AlertDialog? = null
    private lateinit var requestBluetoothPermission: ActivityResultLauncher<String>
    private var idTransaction: String = ""
    private lateinit var transaction: TransactionEntity
    private lateinit var productTransaction: List<ProductTransaction>
    private lateinit var textForPrint: String
    private lateinit var bluetoothManager: BluetoothManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PrintBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)

            // Initialize the ActivityResultLauncher
            requestBluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                return@registerForActivityResult if (isGranted) runBluetoothOperation() else finish()
            }

            binding.stEditBuetooth.setOnClickListener{
                Intent(this, BluetoothSetting::class.java).also { btSetting ->
                    startActivity(btSetting)
                }
            }

            binding.btnPrintTransaction.setOnClickListener {
                if (owner.bluetoothPaired == "") return@setOnClickListener
                val bluetoothAdapter = bluetoothManager.adapter
                if (bluetoothAdapter == null) finish()
                if (!bluetoothAdapter.isEnabled) showDialogTurnOnBluetooth(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ActivityCompat.checkSelfPermission(this@Print, Manifest.permission.BLUETOOTH_CONNECT) != permission.granted) {
                        requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        return@setOnClickListener
                    }
                }
                var deviceAddress = ""
                val pairedDevices = bluetoothAdapter.bondedDevices
                for (device in pairedDevices) {
                    if (device.name == owner.bluetoothPaired) {
                        // Pair with the printer using the deviceAddress
                        // You can use this deviceAddress to establish a connection later
                        deviceAddress = device.address.toString()
                        break
                    }
                }
                if (deviceAddress == "") {
                    return@setOnClickListener helper.generateTOA(this@Print, "Printer not found\nis printer on?", true)
                }
                val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
                val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
                val socket = device.createRfcommSocketToServiceRecord(uuid)

                try {
                    socket.connect()
                    val oS = socket.outputStream
                    val dataPrint = printer.generateStruckPayment(transaction, owner, productTransaction)
                    for (d in dataPrint) {
                        oS.write(d)
                    }
                    oS.flush()
                    oS.close()
                } catch (e: Exception) {
                    helper.generateTOA(this, "can not connect to device. is printer on?", true)
                    socket.close()
                }
            }

            helper = Helper()
            permission = Permission(this@Print, requestBluetoothPermission)
            gposRepo = AdapterDb.getGposDatabase(this).repository()
            constant = Constant()
            printer = Printer(helper)

            bluetoothReceiver = BluetoothReceiver(this)
            bluetoothFilter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            registerReceiver(bluetoothReceiver, bluetoothFilter)
        }
    }

    override fun onResume() {
        super.onResume()
        runBluetoothOperation()
        owner = gposRepo.getOwner()[0]
        binding.pairedName.text = owner.bluetoothPaired
        idTransaction = intent.getStringExtra(constant.idTransaction()) ?: ""
        if (idTransaction == "") return finish()
        transaction = gposRepo.getTransactionById(idTransaction)[0]
        productTransaction = gposRepo.getProductTransactionByTransactionID(transaction.id)
        textForPrint = generateStruckPayment(productTransaction, transaction)
        binding.textPrint.text = textForPrint
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothReceiver)
    }

    private fun runBluetoothOperation() {
        // Check if Bluetooth permission is already granted
        if (!permission.checkBluetoothPermission()) return
        bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) finish()
        if (!bluetoothAdapter.isEnabled) showDialogTurnOnBluetooth(this)
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

    private fun generateStruckPayment(pt: List<ProductTransaction>, t: TransactionEntity): String {
        var dataPrint = ""
        for (p in pt) {
            dataPrint += "${p.product.name}\n"
            dataPrint += generateQtyProduct(
                helper.intToRupiah(p.product.price),
                p.product.quantity.toString(),
                helper.intToRupiah (p.product.price * p.product.quantity)
            )
        }
        dataPrint += "\n\n\n"
        dataPrint += "Total${
            String.format(
                "%27s",
                helper.intToRupiah(helper.getTotalPayment(t))
            )
        }\n"
        dataPrint += "Cash${
            String.format(
                "%28s",
                helper.intToRupiah(t.dataTransaction.cash)
            )
        }\n"
        dataPrint += "${String.format("%32s", "------------")}\n"
        dataPrint += "Returned${
            String.format(
                "%24s",
                helper.intToRupiah(t.dataTransaction.cash - helper.getTotalPayment(t))
            )
        }\n\n\n"
        dataPrint += "${String.format("%-32s", "Thank You")}\n\n"

        return dataPrint
    }

    private fun generateQtyProduct(price: String, qty: String, total: String): String {
        val priceFormatted = String.format("%12s", price)
        val qtyFormatted = String.format("%4s", qty)
        val totalFormatted = String.format("%10s", total)

        return "$priceFormatted  x $qtyFormatted  $totalFormatted\n"
    }
}