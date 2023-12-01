package com.gulali.gpos

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.gulali.gpos.adapter.ProductCart
import com.gulali.gpos.adapter.ProductSearch
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.ProductTransaction
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.databinding.TransactionAddBinding
import com.gulali.gpos.helper.Helper
import java.io.IOException
import java.util.UUID

class AddTransaction: AppCompatActivity() {
    private lateinit var binding: TransactionAddBinding
    private lateinit var productSearch: ProductSearch
    private lateinit var helper: Helper
    private lateinit var scanner: GmsBarcodeScanner
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var gposRepo: Repository
    private lateinit var transactionEntity: TransactionEntity
    private var searchProductViaBarcode: Boolean = false
    private var productTransaction: MutableList<ProductTransaction> = mutableListOf()
    private lateinit var products: List<ProductModel>
    private var isDialogActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransactionAddBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            helper = Helper()
            scanner = helper.initBarcodeScanner(this)
            mediaPlayer = helper.initBeebSound(this)
            gposRepo = AdapterDb.getGposDatabase(this).repository()

            transactionEntity = TransactionEntity(
                id = helper.generatePaymentID(),
                item= 0,
                totalProduct= 0,
                discountNominal= 0,
                discountPercent= 0.0,
                taxNominal= 0,
                taxPercent= 0.0,
                adm= 0,
                cash= 0,
                createdAt= "",
                updatedAt= "",
            )
        }

        binding.payout.setOnClickListener {
            if (productTransaction.size == 0) {
                return@setOnClickListener helper.generateTOA(this, "Please insert product to cart before payout", true)
            }
            showPayment(this)
        }

        var querySearchProduct = ""
        binding.searchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentQuery = s.toString().lowercase()
                if (querySearchProduct == currentQuery) {
                    return
                }
                querySearchProduct = currentQuery
                products = gposRepo.getProductByName(querySearchProduct)
                if (products.isEmpty()) {
                    return
                }
                showAvailableProducts(products, this@AddTransaction)
            }
        })

        binding.btnScanBc.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener {
                    // add bib sound
                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        mediaPlayer?.start()
                    }
                    searchProductViaBarcode = true
                    binding.searchProduct.setText(it.rawValue)
                }
                .addOnCanceledListener {
                    // Task canceled
                }
                .addOnFailureListener {
                    Toast.makeText(applicationContext, it.message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onResume() {
        super.onResume()

        if (searchProductViaBarcode) {
            searchProductViaBarcode = false

            val productResult = gposRepo.getProductByBarcode(binding.searchProduct.text.toString())
            if (productResult == null) {
                helper.generateTOA(this, "Product not found", true)
                return
            }
            checkAvailableProduct(productResult, this)
            return
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release the MediaPlayer when the activity is destroyed
        mediaPlayer?.release()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (productTransaction.size == 0) {
            super.onBackPressed()
            return
        }

        showDialogExit(this)
    }

    private fun showPayment(ctx: Context) {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.payment_display, null)

        // text display
        val subTotal = dialogLayout.findViewById<TextView>(R.id.p_sub_total)
        val totalPriceValue = helper.intToRupiah(getCurrentTotalPriceInCart())
        subTotal.text = totalPriceValue
        val totalPayment = dialogLayout.findViewById<TextView>(R.id.p_total_payment)
        setTotalPayment(totalPayment)
        val cashReturned = dialogLayout.findViewById<TextView>(R.id.cash_returned)
        setCashReturned(cashReturned, ctx)

        val containerTotal = dialogLayout.findViewById<ConstraintLayout>(R.id.container_total)
        val btnShowDesc = dialogLayout.findViewById<Button>(R.id.btn_show_desc)

        btnShowDesc.setOnClickListener {
            val s = "Show description"
            val h = "Hide description"
            if (containerTotal.visibility == View.GONE) {
                containerTotal.visibility = View.VISIBLE
                btnShowDesc.text = h
            } else {
                containerTotal.visibility = View.GONE
                btnShowDesc.text = s
            }
        }

        val cashConsumer = dialogLayout.findViewById<EditText>(R.id.cash_consumer)
        val saveAndPrint = dialogLayout.findViewById<Button>(R.id.save_and_print)
        val savePayment = dialogLayout.findViewById<Button>(R.id.save_payment)
        val cancelPayment = dialogLayout.findViewById<Button>(R.id.cancel_payment)

        // discount section
        var needUpdateDiscountPercent = false
        var needUpdateDiscountNominal = false
        val discountNominal = dialogLayout.findViewById<EditText>(R.id.p_dis_nominal)
        val discountPercent = dialogLayout.findViewById<EditText>(R.id.p_dis_percent)
        discountPercent.addTextChangedListener(object :TextWatcher{
            var isFinish = false
            var cp = 0
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = discountPercent.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (needUpdateDiscountPercent) {
                    needUpdateDiscountPercent = false
                    return
                }
                transactionEntity.discountPercent = helper.strToDouble(s.toString())
                if (isFinish) {
                    isFinish = false
                    discountPercent.setSelection(cp)
                    // set nominal discount
                    needUpdateDiscountNominal = true
                    transactionEntity.discountNominal = getDiscountNominal(transactionEntity.totalProduct,transactionEntity.discountPercent)
                    setEditTextWithRupiahFormat(discountNominal, transactionEntity.discountNominal)
                    setCashReturned(cashReturned, ctx)
                    return setTotalPayment(totalPayment)
                }
                isFinish = true
                discountPercent.setText(s.toString())
            }
        })
        discountNominal.addTextChangedListener(object : TextWatcher {
            var cp = 0
            var isFinish = false
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = discountNominal.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    if (needUpdateDiscountNominal) {
                        needUpdateDiscountNominal = false
                        return
                    }
                    if (isFinish) {
                        isFinish = false
                        setSelectionEditText(discountNominal, cp, transactionEntity.discountNominal)
                        // set percent discount
                        needUpdateDiscountPercent = true
                        transactionEntity.discountPercent = 0.0
                        discountPercent.setText("0")
                        setCashReturned(cashReturned, ctx)
                        return setTotalPayment(totalPayment)
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (userInput == 0) {
                        isFinish = true
                        // set percent discount
                        needUpdateDiscountPercent = true
                        transactionEntity.discountPercent = 0.0
                        discountPercent.setText("0")
                        transactionEntity.discountNominal = 0
                        discountNominal.setText("0")
                        setCashReturned(cashReturned, ctx)
                        return setTotalPayment(totalPayment)
                    }
                    if (transactionEntity.discountNominal == userInput) {
                        isFinish = true
                        setEditTextWithRupiahFormat(discountNominal, transactionEntity.discountNominal)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    transactionEntity.discountNominal = userInput
                    isFinish = true
                    setEditTextWithRupiahFormat(discountNominal, transactionEntity.discountNominal)
                } catch (e: Exception) {
                    println(e.message.toString())
                }
            }
        })

        // tax section
        var needUpdateTaxPercent = false
        var needUpdateTaxNominal = false
        val taxNominal = dialogLayout.findViewById<EditText>(R.id.p_tax_nominal)
        val taxPercent = dialogLayout.findViewById<EditText>(R.id.p_tax_percent)
        taxNominal.addTextChangedListener(object : TextWatcher {
            var cp = 0
            var isFinish = false
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = taxNominal.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    if (needUpdateTaxNominal) {
                        needUpdateTaxNominal = false
                        return
                    }
                    if (isFinish) {
                        isFinish = false
                        setSelectionEditText(taxNominal, cp, transactionEntity.taxNominal)
                        // set percent tax
                        needUpdateTaxPercent = true
                        transactionEntity.taxPercent = 0.0
                        taxPercent.setText("0")
                        setCashReturned(cashReturned, ctx)
                        return setTotalPayment(totalPayment)
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (userInput == 0) {
                        isFinish = true
                        transactionEntity.taxNominal = 0
                        taxNominal.setText("")
                        // set percent tax
                        needUpdateTaxPercent = true
                        transactionEntity.taxPercent = 0.0
                        taxPercent.setText("0")
                        setCashReturned(cashReturned, ctx)
                        return setTotalPayment(totalPayment)
                    }
                    if (transactionEntity.taxNominal == userInput) {
                        isFinish = true
                        setEditTextWithRupiahFormat(taxNominal, transactionEntity.taxNominal)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    transactionEntity.taxNominal = userInput
                    isFinish = true
                    setEditTextWithRupiahFormat(taxNominal, transactionEntity.taxNominal)
                } catch (e: Exception) {
                    println(e.message.toString())
                }
            }
        })
        taxPercent.addTextChangedListener(object :TextWatcher{
            var isFinish = false
            var cp = 0
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = taxPercent.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (needUpdateTaxPercent) {
                    needUpdateTaxPercent = false
                    return
                }
                transactionEntity.taxPercent = helper.strToDouble(s.toString())
                if (isFinish) {
                    isFinish = false
                    taxPercent.setSelection(cp)
                    // set nominal tax
                    needUpdateTaxNominal = true
                    transactionEntity.taxNominal = getDiscountNominal(transactionEntity.totalProduct,transactionEntity.taxPercent)
                    setEditTextWithRupiahFormat(taxNominal, transactionEntity.taxNominal)
                    setCashReturned(cashReturned, ctx)
                    return setTotalPayment(totalPayment)
                }
                isFinish = true
                taxPercent.setText(s.toString())
            }
        })

        // adm nominal section
        val admNominal = dialogLayout.findViewById<EditText>(R.id.p_adm_nominal)
        admNominal.addTextChangedListener(object : TextWatcher {
            var cp = 0
            var isFinish = false
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = admNominal.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    if (isFinish) {
                        isFinish = false
                        setSelectionEditText(admNominal, cp, transactionEntity.adm)
                        setTotalPayment(totalPayment)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (userInput == 0) {
                        isFinish = true
                        transactionEntity.adm = 0
                        setTotalPayment(totalPayment)
                        setCashReturned(cashReturned, ctx)
                        admNominal.setText("")
                        return
                    }
                    if (transactionEntity.adm == userInput) {
                        isFinish = true
                        setEditTextWithRupiahFormat(admNominal, transactionEntity.adm)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    transactionEntity.adm = userInput
                    isFinish = true
                    setEditTextWithRupiahFormat(admNominal, transactionEntity.adm)
                } catch (e: Exception) {
                    println(e.message.toString())
                }
            }
        })

        // cash nominal section
        val containerNominal = dialogLayout.findViewById<ConstraintLayout>(R.id.type_nominal)
        val btnOpenContainerNominal = dialogLayout.findViewById<Button>(R.id.show_nominal)
        btnOpenContainerNominal.setOnClickListener {
            val s = "Show nominal"
            val h = "Hide nominal"
            if (containerNominal.visibility == View.GONE) {
                containerNominal.visibility = View.VISIBLE
                btnOpenContainerNominal.text = h
            } else {
                containerNominal.visibility = View.GONE
                btnOpenContainerNominal.text = s
            }
        }
        val btnSeRibu = dialogLayout.findViewById<Button>(R.id.btn_srb)
        val btnDuaRibu = dialogLayout.findViewById<Button>(R.id.btn_drb)
        val btnLimaRibu = dialogLayout.findViewById<Button>(R.id.btn_lrb)
        val btnSePuluhRibu = dialogLayout.findViewById<Button>(R.id.btn_sprb)
        val btnDuaPuluhRibu = dialogLayout.findViewById<Button>(R.id.btn_dprb)
        val btnLimaPuluhRibu = dialogLayout.findViewById<Button>(R.id.btn_lmrb)
        val btnSeratusRibu = dialogLayout.findViewById<Button>(R.id.btn_srbrb)
        val btnResetCash = dialogLayout.findViewById<ImageButton>(R.id.btn_reset_cash)

        btnSeRibu.setOnClickListener {addNominalToCash(ctx, 1000, cashConsumer, cashReturned)}
        btnDuaRibu.setOnClickListener {addNominalToCash(ctx, 2000, cashConsumer, cashReturned)}
        btnLimaRibu.setOnClickListener {addNominalToCash(ctx, 5000, cashConsumer, cashReturned)}
        btnSePuluhRibu.setOnClickListener {addNominalToCash(ctx, 10000, cashConsumer, cashReturned)}
        btnDuaPuluhRibu.setOnClickListener {addNominalToCash(ctx, 20000, cashConsumer, cashReturned)}
        btnLimaPuluhRibu.setOnClickListener {addNominalToCash(ctx, 50000, cashConsumer, cashReturned)}
        btnSeratusRibu.setOnClickListener {addNominalToCash(ctx, 100000, cashConsumer, cashReturned)}
        btnResetCash.setOnClickListener {
            transactionEntity.cash = 0
            setCashNominal(cashConsumer, transactionEntity.cash)
            setCashReturned(cashReturned, ctx)
        }
        cashConsumer.setRawInputType(2)
        cashConsumer.addTextChangedListener(object : TextWatcher {
            var cp = 0
            var isFinish = false
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = cashConsumer.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                try {
                    if (isFinish) {
                        isFinish = false
                        setSelectionEditText(cashConsumer, cp, transactionEntity.cash)
                        setTotalPayment(totalPayment)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (userInput == 0) {
                        isFinish = true
                        transactionEntity.cash = 0
                        setCashReturned(cashReturned, ctx)
                        setTotalPayment(totalPayment)
                        cashConsumer.setText("")
                        return
                    }
                    if (transactionEntity.cash == userInput) {
                        isFinish = true
                        setCashReturned(cashReturned, ctx)
                        setEditTextWithRupiahFormat(cashConsumer, transactionEntity.cash)
                        return
                    }
                    isFinish = true
                    transactionEntity.cash = userInput
                    setEditTextWithRupiahFormat(cashConsumer, transactionEntity.cash)
                } catch (e: Exception) {
                    println(e.message.toString())
                }
            }
        })

        val alertDialog = builder.setView(dialogLayout).show()

        savePayment.setOnClickListener {
            try {
                if (!isCorrectCashReturner()) {
                    helper.generateTOA(this, "Invalid cash nominal", true)
                    return@setOnClickListener
                }
                saveDataTransactionToDb()
                saveDataProductTransactionToDB()
                helper.generateTOA(this, "Save", true)
                alertDialog.dismiss()
                finish()
            } catch (e: Exception) {
                helper.generateTOA(applicationContext, e.message.toString(), true)
            }
        }
        saveAndPrint.setOnClickListener {
            generateStruckPayment(ctx, helper.rupiahToInt(cashConsumer.text.toString()))
//            alertDialog.dismiss()
//            finish()
        }
        cancelPayment.setOnClickListener {
            alertDialog.dismiss()
        }
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

    private fun generateStruckPayment(ctx: Context, cashConsumer: Int) {
        if (!checkPermissionBluetooth()) return

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(applicationContext, "your device not support bluetooth", Toast.LENGTH_SHORT).show()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH), 100)
                return
            }
            startActivityForResult(enableBtIntent, 1)
            return
        }

        val deviceName = gposRepo.getOwnerBluetooth()

        if (deviceName == "") {
            Intent(this, Setting::class.java).also { intent ->
                startActivity(intent)
            }
            return
        }

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
            helper.generateTOA(ctx, "Printer not found\nis printer on?", true)
            return
        }

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        val socket = device.createRfcommSocketToServiceRecord(uuid)

        try {
            socket.connect()
            // You're now connected to the printer

            var dataPrint = ""
            for (p in productTransaction) {
                dataPrint += "${p.pName}\n"
                dataPrint += generateTextForPrint(helper.intToRupiah(p.pPrice), p.pQty.toString(), helper.intToRupiah (p.pPrice * p.pQty))
            }
            dataPrint += "\n\n\n"
            dataPrint += "Total${
                String.format(
                    "%27s",
                    helper.intToRupiah(getCurrentTotalPriceInCart())
                )
            }\n"
            dataPrint += "Cash${
                String.format(
                    "%28s",
                    helper.intToRupiah(cashConsumer)
                )
            }\n"
            dataPrint += "${String.format("%32s", "------------")}\n"
            dataPrint += "Returned${
                String.format(
                    "%24s",
                    helper.intToRupiah(cashConsumer - getCurrentTotalPriceInCart())
                )
            }\n\n\n"
            dataPrint += "${String.format("%-32s", "Thank You")}\n\n"

            val outputStream = socket.outputStream
            outputStream.write(dataPrint.toByteArray())
            outputStream.flush()
            outputStream.close()
        } catch (e: IOException) {
            // Handle connection errors
            println(e.message)
        }
    }

    private fun saveDataTransactionToDb() {
        transactionEntity.createdAt = helper.getDate()
        transactionEntity.updatedAt = helper.getDate()
        gposRepo.saveTransaction(transactionEntity)
    }

    private fun saveDataProductTransactionToDB() {
        // update stock
        for (p in productTransaction) {
            val prod = gposRepo.getProductByID(p.pID)
            gposRepo.updateStockProduct(p.pID, prod.stock - p.pQty)
        }
        gposRepo.saveProductTransaction(productTransaction)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showAvailableProducts(products: List<ProductModel>, ctx: Context) {
        productSearch = ProductSearch(products, helper, applicationContext, contentResolver)
        productSearch.setOnItemClickListener(object : ProductSearch.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val p = products[position]
                checkAvailableProduct(p, ctx)
            }
        })
        binding.productSearchView.adapter = productSearch
        productSearch.notifyDataSetChanged()
    }

    private fun checkAvailableProduct(p: ProductModel, ctx: Context) {
        try {
            if (p.stock <= 0) {
                helper.generateTOA(ctx, "Stock empty!!", true)
                return
            }
            if (productTransaction.size != 0 && isProductTransactionExistInCart(p.id)) {
                helper.generateTOA(ctx, "Product already exist in the cart", true)
                return
            }
            val pCoo = ProductTransaction(
                transactionID = transactionEntity.id,
                pID = p.id,
                pName = p.name,
                pUnit= p.unit,
                pPrice = p.price,
                cAt = helper.getDate(),
                uAt = helper.getDate(),
                pQty = 1,
                pDiscount = 0.0,
            )
            showProductQTY(pCoo, p.stock, p.img, false, ctx)
        } catch (e: Exception) {
            println(e.message)
            Toast.makeText(applicationContext, "something wrong", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showProductInCart(products: MutableList<ProductTransaction>, ctx: Context) {
        val pdCard = ProductCart(helper, products, applicationContext)
        pdCard.setOnItemClickListener(object :ProductCart.OnItemClickListener{
            override fun onItemClick(position: Int) {
                try {
                    val p = products[position]
                    val pdb = gposRepo.getProductByID(p.pID)
                    showProductQTY(p, pdb.stock, pdb.img, true, ctx)
                } catch (e: Exception) {
                    helper.generateTOA(ctx, "product not found", true)
                }
            }
        })
        binding.cartProduct.adapter = pdCard
        pdCard.notifyDataSetChanged()
    }

    fun showProductQTY(prod: ProductTransaction, stock: Int, img: String, isUpdate: Boolean, ctx: Context) {
        try {
            val builder = AlertDialog.Builder(ctx)
            val inflater = layoutInflater
            val dialogLayout = inflater.inflate(R.layout.product_qty, null)
            val alertDialog = builder.setView(dialogLayout).show() // Create and show the AlertDialog

            // text display
            val displayImage  = dialogLayout.findViewById<ImageView>(R.id.product_image)
            val displayName  = dialogLayout.findViewById<TextView>(R.id.product_name)
            val displayStock = dialogLayout.findViewById<TextView>(R.id.product_stock)
            val displayUnit = dialogLayout.findViewById<TextView>(R.id.product_unit)
            val displayPrice = dialogLayout.findViewById<TextView>(R.id.product_price)

            // init display
            val uri = helper.getUriFromGallery(contentResolver, img)
            if (uri != null) {
                Glide.with(this)
                    .load(uri)
                    .into(displayImage)
            }
            displayName.text = prod.pName
            displayStock.text = stock.toString()
            displayUnit.text = prod.pUnit
            displayPrice.text = helper.intToRupiah(prod.pPrice)

            // init input
            val pdDiscount = dialogLayout.findViewById<EditText>(R.id.anp_discount)
            val priceBeforeDiscount = dialogLayout.findViewById<TextView>(R.id.anp_qty_totpr)
            val priceAfterDiscount = dialogLayout.findViewById<TextView>(R.id.tot_af_dis)
            val pdOk = dialogLayout.findViewById<Button>(R.id.btn_qty_ok)
            val pdCancel = dialogLayout.findViewById<Button>(R.id.btn_qty_cancel)
            val pdDelete = dialogLayout.findViewById<Button>(R.id.btn_del_pic)
            val pdQty = dialogLayout.findViewById<EditText>(R.id.anp_qty_tot)
            if (isUpdate) {
                pdCancel.visibility = View.GONE
                pdDelete.visibility = View.VISIBLE
                pdDelete.setOnClickListener {
                    deleteProductInCart(prod.pID)
                    showProductInCart(productTransaction, ctx)
                    binding.totpricecart.text = helper.intToRupiah(getCurrentTotalPriceInCart())
                    val totItemValue = "(${productTransaction.size} item)"
                    binding.totitem.text = totItemValue
                    alertDialog.dismiss()
                }
                pdDiscount.setText(prod.pDiscount.toString())
                pdQty.setText(prod.pQty.toString())
            } else {
                pdCancel.setOnClickListener { alertDialog.dismiss() }
            }
            val pdQtyMin = dialogLayout.findViewById<Button>(R.id.anp_qty_min)
            val pdQtyPlus = dialogLayout.findViewById<Button>(R.id.anp_qty_plus)
            helper.setPriceAfterDiscount(priceBeforeDiscount, priceAfterDiscount, prod.pDiscount, prod.pPrice, prod.pQty, true, ctx)
            pdQty.addTextChangedListener(object :TextWatcher{
                var isFinish = false
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(inpt: Editable?) {
                    val value = inpt.toString().toIntOrNull()
                    if (isFinish) {
                        isFinish = false
                        pdQty.setSelection(pdQty.text.length)
                        return
                    }
                    if (value == null || value <= 0) {
                        isFinish = true
                        helper.setPriceAfterDiscount(priceBeforeDiscount, priceAfterDiscount, prod.pDiscount, prod.pPrice, 1, true, ctx)
                        prod.pQty = 1
                        pdQty.setText("1")
                        return
                    }
                    if (value > stock) {
                        isFinish = true
                        helper.setPriceAfterDiscount(priceBeforeDiscount, priceAfterDiscount, prod.pDiscount, prod.pPrice, stock, true, ctx)
                        prod.pQty = stock
                        pdQty.setText(stock.toString())
                        return
                    }
                    prod.pQty = value
                    helper.setPriceAfterDiscount(priceBeforeDiscount, priceAfterDiscount, prod.pDiscount, prod.pPrice, value, true, ctx)
                }
            })
            pdQtyMin.setOnClickListener {
                val value = pdQty.text.toString().toIntOrNull()
                if (value == null || value - 1 <= 0) {
                    pdQty.setText("1")
                    pdQty.setSelection(pdQty.text.length)
                    prod.pQty = 1
                    helper.setPriceAfterDiscount(priceBeforeDiscount, priceAfterDiscount, prod.pDiscount, prod.pPrice, 1, true, ctx)
                    return@setOnClickListener
                }
                pdQty.setText((value - 1).toString())
                pdQty.setSelection(pdQty.text.length)
                prod.pQty = value - 1
                helper.setPriceAfterDiscount(priceBeforeDiscount, priceAfterDiscount, prod.pDiscount, prod.pPrice, value - 1, true, ctx)
            }
            pdQtyPlus.setOnClickListener {
                val value = pdQty.text.toString().toIntOrNull() ?: return@setOnClickListener
                if (value < stock) {
                    val textQtyValue = (value + 1).toString()
                    pdQty.setText(textQtyValue)
                    pdQty.setSelection(pdQty.text.length)
                    prod.pQty = value + 1
                    helper.setPriceAfterDiscount(priceBeforeDiscount, priceAfterDiscount, prod.pDiscount, prod.pPrice, value + 1, true, ctx)
                }
            }
            pdDiscount.addTextChangedListener(object :TextWatcher{
                var isFinish = false
                var cp = 0
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    cp = pdDiscount.selectionStart
                }
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    prod.pDiscount = helper.strToDouble(s.toString())
                    if (isFinish) {
                        isFinish = false
                        pdDiscount.setSelection(cp)
                        helper.setPriceAfterDiscount(priceBeforeDiscount, priceAfterDiscount, prod.pDiscount, prod.pPrice, prod.pQty, true, ctx)
                        return
                    }
                    isFinish = true
                    pdDiscount.setText(s.toString())
                }
            })
            pdOk.setOnClickListener {
                if (isUpdate) {
                    for (p in productTransaction) {
                        if (p.pID == prod.id) {
                            p.pQty = prod.pQty
                            p.pDiscount = prod.pDiscount
                            break
                        }
                    }
                } else {
                    productTransaction.add(0, prod)
                }
                showProductInCart(productTransaction, ctx)
                binding.totpricecart.text = helper.intToRupiah(getCurrentTotalPriceInCart())
                val totItemValue = "(${productTransaction.size} item)"
                binding.totitem.text = totItemValue
                alertDialog.dismiss()
            }
        } catch (e: Exception) {
            helper.generateTOA(applicationContext, "product not found", true)
        }
    }

    private fun deleteProductInCart(pID: Int):Boolean {
        return try {
            var idxProductInCart = 0
            for (p in productTransaction) {
                if (p.pID == pID) {
                    productTransaction.removeAt(idxProductInCart)
                    break
                }
                idxProductInCart += 1
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun showDialogExit(ctx: Context) {
        val builder = AlertDialog.Builder(ctx)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_exit, null)
        val btnOk = dialogLayout.findViewById<Button>(R.id.exit_yes)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.exit_no)
        val alertDialog = builder.setView(dialogLayout).show() // Create and show the AlertDialog

        btnOk.setOnClickListener {
            alertDialog.dismiss()
            finish()
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun getCurrentTotalPriceInCart(): Int {
        var result = 0
        for (p in productTransaction) {
            val totalPrice = p.pPrice * p.pQty
            val discountPercentage = p.pDiscount / 100
            val totalDiscount = discountPercentage * totalPrice
            val totalPriceAfterDiscount = totalPrice - totalDiscount
            result += totalPriceAfterDiscount.toInt()
        }

        transactionEntity.totalProduct = result
        return result
    }

    private fun generateTextForPrint(price: String, qty: String, total: String): String {
        val priceFormatted = String.format("%12s", price)
        val qtyFormatted = String.format("%4s", qty)
        val totalFormatted = String.format("%10s", total)

        return "$priceFormatted  x $qtyFormatted  $totalFormatted\n"
    }

    private fun isProductTransactionExistInCart(idProduct: Int): Boolean {
        for (p in productTransaction) {
            if (p.pID == idProduct) {
                return true
            }
        }
        return false
    }

    fun getDiscount(amount: Int, discount: Double): Int {
        return try {
            val result = (100.0 - discount) * amount
            result.toInt()
        } catch (e: Exception) {
            0
        }
    }

    fun setEditTextWithRupiahFormat(e: EditText, nominal: Int) {
        e.setText(helper.intToRupiah(nominal))
    }

    fun setSelectionEditText(edt: EditText, sS: Int, sE: Int) {
        val selection = if (sS < sE.toString().length)
            sS else helper.intToRupiah(sE).length
        edt.setSelection(selection)
    }

    private fun getTotalPayment(): Int {
        return try {
            var result = 0
            result += transactionEntity.totalProduct
            result -= transactionEntity.discountNominal
            result += transactionEntity.taxNominal
            result += transactionEntity.adm
            result
        } catch (e: Exception) {
            0
        }
    }

    fun setTotalPayment(t: TextView) {
        val totalPayment = "Rp ${helper.intToRupiah(getTotalPayment())}"
        t.text = totalPayment
    }

    fun getDiscountNominal(nominal: Int, discount: Double): Int {
        val discountPercentage = discount / 100
        val totalDiscount = discountPercentage * nominal
        return totalDiscount.toInt()
    }

    private fun setCashNominal(e: EditText, n: Int) {
        try {
            val cashStr = helper.intToRupiah(n)
            val cp = cashStr.length
            e.setText(cashStr)
            if (cp == 1) return
            e.setSelection(cashStr.length)
        } catch (e: Exception) {
            helper.generateTOA(this, "something wrong", false)
        }
    }

    private fun setCashReturned(t: TextView, ctx: Context) {
        val totalPayment = getTotalPayment()
        val cashReturned = "${helper.intToRupiah(transactionEntity.cash - totalPayment)} ,-"

        if (isCorrectCashReturner()) {
            t.text = cashReturned
            t.setTextColor(ContextCompat.getColor(ctx, R.color.black))
        } else {
            t.text = cashReturned
            t.setTextColor(ContextCompat.getColor(ctx, R.color.red))
        }
    }

    private fun isCorrectCashReturner(): Boolean {
        return transactionEntity.cash - getTotalPayment() >= 0
    }

    private fun addNominalToCash(ctx: Context, n: Int, cashNominal: EditText, cashReturned: TextView) {
        transactionEntity.cash += n
        setCashNominal(cashNominal, transactionEntity.cash)
        setCashReturned(cashReturned, ctx)
    }
}