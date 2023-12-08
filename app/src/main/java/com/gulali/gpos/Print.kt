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
import androidx.core.content.ContextCompat
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
            requestBluetoothPermission = registerForActivityResult(ActivityResultContracts.RequestPermission())
            { isGranted: Boolean -> return@registerForActivityResult if (isGranted) runBluetoothOperation() else finish() }

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

                val totalPrice = "Rp${helper.intToRupiah(helper.getTotalPayment(transaction))}"
                var totalAmount = ""
                totalAmount += "Total${
                    String.format(
                        "%27s",
                        totalPrice
                    )
                }\n\n"

                var dataPrint = ""
                dataPrint += "\n"
                for (p in productTransaction) {
                    dataPrint += generateProductNameForPrint(p.pName)
                    dataPrint += generateQtyProduct(helper.intToRupiah(p.pPrice), p.pQty.toString(), helper.intToRupiah (p.pPrice * p.pQty))
                }
                dataPrint += "\n"

                val reset = byteArrayOf(0x1b, 0x40)
                val fontX2 = byteArrayOf(0x1b,0x21, 0x10)
                val characterSpacing = byteArrayOf(0x1b, 0x20, 0x09)
                val font1x = byteArrayOf(0x1b,0x21, 0x00)
                val bold = byteArrayOf(0x1b,0x21, 0x30)
                val cut = byteArrayOf(0x1D, 0x56, 66, 0x00)
                val centerText = byteArrayOf(0x1b, 0x61, 0x01)
                val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(transaction.createdAt))
                val rightText = byteArrayOf(0x1b, 0x61, 0x02)
                var subTotalProduct = ""
                subTotalProduct += "Sub total products${
                    String.format(
                        "%14s",
                        helper.intToRupiah(transaction.totalProduct)
                    )
                }\n"
                var discount = "Discount"
                try {
                    socket.connect()
                    val outputStream = socket.outputStream
                    outputStream.write(reset)
                    outputStream.write(fontX2)
                    outputStream.write(centerText)
                    outputStream.write("TOKO\n".toByteArray())
                    outputStream.write("${owner.owner}\n".toByteArray())
                    outputStream.write(reset)
                    outputStream.write("${dateTime.date}\n".toByteArray())
                    outputStream.write("id ${transaction.id}\n".toByteArray())
                    outputStream.write("time ${dateTime.time}\n".toByteArray())
                    outputStream.write("--------------------------------\n".toByteArray())
                    outputStream.write(generateListProductForPrint(productTransaction).toByteArray())
                    outputStream.write("--------------------------------\n".toByteArray())
                    outputStream.write("${generateSubTotalProduct(transaction)}\n".toByteArray())
                    outputStream.write("${generateDiscountPayment(transaction)}\n".toByteArray())
                    outputStream.write("${generateTaxPayment(transaction)}\n".toByteArray())
                    outputStream.write("${generateAdmPayment(transaction)}\n".toByteArray())
                    outputStream.write("--------------------------------\n".toByteArray())
                    outputStream.write(fontX2)
                    outputStream.write(totalAmount.toByteArray())
                    outputStream.write(reset)
                    outputStream.write("${generateCashPayment(transaction)}\n".toByteArray())
                    outputStream.write("${generateCashReturnedPayment(transaction)}\n".toByteArray())
                    outputStream.write("================================\n".toByteArray())
                    outputStream.write(centerText)
                    outputStream.write("This receipt is valid\n".toByteArray())
                    outputStream.write("proof of payment\n".toByteArray())
                    outputStream.write("from the shop ${owner.owner}.\n".toByteArray())
                    outputStream.write("For further information, please call ${owner.phone}, or visit the shop address at ${owner.address}\n".toByteArray())
                    outputStream.write("\nthank you for visiting\n\n".toByteArray())
                    outputStream.write("---\n".toByteArray())
                    outputStream.write(reset)
                    outputStream.write(cut)
                    outputStream.flush()
                    outputStream.close()
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
        transaction = gposRepo.getTransactionById(idTransaction)
        productTransaction = gposRepo.getProductTransactionByTransactionID(transaction.id)
        textForPrint = generateStruckPayment(productTransaction, transaction, owner)
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

    private fun generateStruckPayment(pt: List<ProductTransaction>, t: TransactionEntity, owner: OwnerEntity): String {
        var dataPrint = ""
        for (p in pt) {
            dataPrint += "${p.pName}\n"
            dataPrint += generateQtyProduct(helper.intToRupiah(p.pPrice), p.pQty.toString(), helper.intToRupiah (p.pPrice * p.pQty))
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
                helper.intToRupiah(t.cash)
            )
        }\n"
        dataPrint += "${String.format("%32s", "------------")}\n"
        dataPrint += "Returned${
            String.format(
                "%24s",
                helper.intToRupiah(t.cash - helper.getTotalPayment(t))
            )
        }\n\n\n"
        dataPrint += "${String.format("%-32s", "Thank You")}\n\n"

        return dataPrint
    }

    private fun generateProductNameForPrint(product: String): String {
        val result = StringBuilder()
        val lineLength = 28

        for (i in product.indices step lineLength) {
            val endIndex = i + lineLength
            val line = if (endIndex <= product.length) {
                product.substring(i, endIndex)
            } else {
                product.substring(i)
            }
            result.append("$line\n")
        }

        return result.toString()
    }

    private fun generateListProductForPrint(pt: List<ProductTransaction>): String {
        var result = ""
        for (p in pt) {
            result += generateProductNameForPrint(p.pName)
            if (p.pDiscount > 0.0) {
                result += generateQtyProduct(helper.intToRupiah(p.pPrice), p.pQty.toString(), "")
                val totalPrice = p.pPrice * p.pQty
                val discountPercentage = p.pDiscount / 100
                val totalDiscount = discountPercentage * totalPrice
                val totalPriceAfterDiscount = totalPrice - totalDiscount
                // format string after discount
                val disStr = String.format("%18s", "disc (${p.pDiscount}%)")
                val totStr = String.format("%12s", helper.intToRupiah(totalPriceAfterDiscount.toInt()))
                // passing to result
                result += "$disStr  $totStr"
                result += "\n"
            } else {
                result += generateQtyProduct(helper.intToRupiah(p.pPrice), p.pQty.toString(), helper.intToRupiah (p.pPrice * p.pQty))
            }
        }
        return result
    }

    private fun generateQtyProduct(price: String, qty: String, total: String): String {
        val priceFormatted = String.format("%12s", price)
        val qtyFormatted = String.format("%4s", qty)
        val totalFormatted = String.format("%10s", total)

        return "$priceFormatted  x $qtyFormatted  $totalFormatted\n"
    }

    private fun generateSubTotalProduct(t: TransactionEntity): String {
        var textFormatted = "Sub total products"
        textFormatted += "\n"
        val t1 = String.format("%-15s", "(${t.item}) item")
        val t2 = String.format("%17s", helper.intToRupiah(transaction.totalProduct))
        textFormatted += t1
        textFormatted += t2
        return textFormatted
    }

    private fun generateDiscountPayment(t: TransactionEntity): String {
        var textFormatted = ""
        textFormatted += String.format("%-18s", "Discount payment")
        if (t.discountPercent > 0.0) {
            textFormatted += "\n"
            textFormatted += String.format("%-10s", "(${t.discountPercent}%)")
            textFormatted += String.format("%22s", "- ${helper.intToRupiah(t.discountNominal)}")
        } else {
            textFormatted += if (t.discountNominal == 0) {
                String.format("%14s", "0")
            } else {
                String.format("%14s", "- ${helper.intToRupiah(t.discountNominal)}")
            }
        }
        return textFormatted
    }

    private fun generateTaxPayment(t: TransactionEntity): String {
        var textFormatted = ""
        textFormatted += String.format("%-18s", "Tax")
        if (t.taxPercent > 0.0) {
            textFormatted += "\n"
            textFormatted += String.format("%-10s", "(${t.taxPercent}%)")
            textFormatted += String.format("%22s", helper.intToRupiah(t.taxNominal))
        } else {
            textFormatted += if (t.taxNominal == 0) {
                String.format("%14s", "0")
            } else {
                String.format("%14s", helper.intToRupiah(t.taxNominal))
            }
        }
        return textFormatted
    }

    private fun generateAdmPayment(t: TransactionEntity): String {
        var textFormatted = ""
        textFormatted += String.format("%-18s", "Adm")
        textFormatted += String.format("%14s", helper.intToRupiah(t.adm))
        return textFormatted
    }
    private fun generateCashPayment(t: TransactionEntity): String {
        var textFormatted = ""
        textFormatted += String.format("%-20s", "Cash")
        textFormatted += String.format("%12s", helper.intToRupiah(t.cash))
        return textFormatted
    }
    private fun generateCashReturnedPayment(t: TransactionEntity): String {
        var totalPayment = 0
        totalPayment += t.totalProduct
        totalPayment += t.taxNominal
        totalPayment += t.adm
        totalPayment -= t.discountNominal

        var textFormatted = ""
        textFormatted += String.format("%-20s", "Returned")
        textFormatted += String.format("%12s", helper.intToRupiah(t.cash - totalPayment))
        return textFormatted
    }
}