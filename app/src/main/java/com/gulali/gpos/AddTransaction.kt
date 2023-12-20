package com.gulali.gpos

//import android.app.AlertDialog
//import android.content.Context
//import android.media.MediaPlayer
//import android.os.Bundle
//import android.text.Editable
//import android.text.TextWatcher
//import android.view.View
//import android.widget.Button
//import android.widget.EditText
//import android.widget.ImageButton
//import android.widget.TextView
//import android.widget.Toast
//import androidx.activity.OnBackPressedCallback
//import androidx.appcompat.app.AppCompatActivity
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.core.content.ContextCompat
//import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
//import com.gulali.gpos.adapter.ProductSearch
//import com.gulali.gpos.database.AdapterDb
//import com.gulali.gpos.database.CartEntity
//import com.gulali.gpos.database.CartPaymentEntity
//import com.gulali.gpos.database.HistoryStockEntity
//import com.gulali.gpos.database.ProductModel
//import com.gulali.gpos.database.ProductTransaction
//import com.gulali.gpos.database.Repository
//import com.gulali.gpos.database.TransactionEntity
//import com.gulali.gpos.databinding.TransactionAddBinding
//import com.gulali.gpos.helper.Dialog
//import com.gulali.gpos.helper.Helper
//import com.gulali.gpos.repository.CartRepo
//import com.gulali.gpos.repository.CartRepoParam
//
//class AddTransaction: AppCompatActivity() {
//    private lateinit var binding: TransactionAddBinding
//    private lateinit var productSearch: ProductSearch
//    private lateinit var helper: Helper
//    private lateinit var scanner: GmsBarcodeScanner
//    private var mediaPlayer: MediaPlayer? = null
//    private lateinit var gposRepo: Repository
//    private lateinit var transactionEntity: TransactionEntity
//    private var searchProductViaBarcode: Boolean = false
//    private var productTransaction: MutableList<ProductTransaction> = mutableListOf()
//    private lateinit var products: List<ProductModel>
//
//    private lateinit var cartPaymentEntity: CartPaymentEntity
//    private lateinit var cartRepoParam: CartRepoParam
//    private lateinit var cartRepo: CartRepo
//    private lateinit var dialog: Dialog
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        TransactionAddBinding.inflate(layoutInflater).also {
//            binding = it
//            setContentView(binding.root)
//            helper = Helper()
//            scanner = helper.initBarcodeScanner(this)
//            mediaPlayer = helper.initBeebSound(this)
//            gposRepo = AdapterDb.getGposDatabase(this).repository()
//            cartRepoParam = CartRepoParam(
//                db = AdapterDb.getGposDatabase(this),
//                helper = helper,
//                ctx = this
//            )
//            cartRepo = CartRepo(cartRepoParam)
//            dialog = Dialog(
//                this@AddTransaction,
//                cartRepo,
//                contentResolver,
//                helper,
//                layoutInflater,
//                binding
//            )
//            transactionEntity = TransactionEntity(
//                id = helper.generatePaymentID(),
//                totalItem= 0,
//                subTotalProduct = 0,
//                discountNominal= 0,
//                discountPercent= 0.0,
//                taxNominal= 0,
//                taxPercent= 0.0,
//                adm= 0,
//                cash= 0,
//                createdAt= 0,
//                updatedAt= 0,
//            )
//        }
//
//        binding.btnScanBc.setOnClickListener {
//            scanner.startScan()
//                .addOnSuccessListener {
//                    // add bib sound
//                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
//                        mediaPlayer?.start()
//                    }
//                    searchProductViaBarcode = true
//                    binding.searchProduct.setText(it.rawValue)
//                }
//                .addOnCanceledListener {
//                    // Task canceled
//                }
//                .addOnFailureListener {
//                    Toast.makeText(applicationContext, it.message, Toast.LENGTH_SHORT).show()
//                }
//        }
//
//        // initial cart payment entity
//        if (!initCartPaymentEntity(cartRepo)) {
//            // truncate table cart payment
//            finish()
//            return
//        }
//
//        onBackPressedDispatcher.addCallback(
//            this@AddTransaction,
//            object :OnBackPressedCallback(true) { override fun handleOnBackPressed() {
//                dialog.showDialogExit(layoutInflater)
//            }
//        })
//
//        var querySearchProduct = ""
//        binding.searchProduct.addTextChangedListener(object : TextWatcher {
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                val currentQuery = s.toString().lowercase()
//                if (querySearchProduct == currentQuery) {
//                    return
//                }
//                querySearchProduct = currentQuery
//                products = gposRepo.getProductByName(querySearchProduct)
//                if (products.isEmpty()) {
//                    return
//                }
//                binding.productSearchView.adapter = dialog.showAvailableProducts(
//                    products,
//                    cartPaymentEntity.id
//                )
//            }
//        })
//
//        binding.payout.setOnClickListener {
//            if (cartRepo.countProductInCart(cartPaymentEntity.id) == 0) {
//                return@setOnClickListener helper.generateTOA(this, "Please insert product to cart before payout", true)
//            }
//            showPayment(this, cartRepo, cartPaymentEntity.id)
//        }
//    }
//
//    private fun initCartPaymentEntity(cartRepo: CartRepo): Boolean {
//        cartPaymentEntity = CartPaymentEntity(
//            id = helper.generatePaymentID(),
//            totalItem= 0,
//            subTotalProduct = 0,
//            discountNominal= 0,
//            discountPercent= 0.0,
//            taxNominal= 0,
//            taxPercent= 0.0,
//            adm= 0,
//            cash= 0,
//            createdAt= 0,
//        )
//        return cartRepo.saveCartPayment(cartPaymentEntity)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        // Release the MediaPlayer when the activity is destroyed
//        mediaPlayer?.release()
//    }
//
//    private fun showPayment(ctx: Context, cartRepo: CartRepo, idPayment: String) {
//        val dataPayment: CartPaymentEntity? = cartRepo.getCartPayment(idPayment)
//        if (dataPayment == null) {
//            helper.generateTOA(ctx, "data payment not found", true)
//            finish()
//            return
//        }
//        val dataProducts: List<CartEntity> = cartRepo.getProductsInCart(idPayment)
//        if (dataProducts.isEmpty()) {
//            helper.generateTOA(ctx, "Please insert product to cart before payout", true)
//            return
//        }
//
//        // initial payment
//        dataPayment.totalItem = dataProducts.size
//        for (dtp in dataProducts) {
//            dataPayment.subTotalProduct += dtp.ptAfterDiscount
//        }
//
//        val builder = AlertDialog.Builder(this)
//        val inflater = layoutInflater
//        val dialogLayout = inflater.inflate(R.layout.payment_display, null)
//
//        // text display
//        val totalItemPayment = "(${dataPayment.totalItem} item)"
//        dialogLayout.findViewById<TextView>(R.id.total_item_payment).text = totalItemPayment
//
//        val subTotal = dialogLayout.findViewById<TextView>(R.id.p_sub_total)
//        subTotal.text = helper.intToRupiah(dataPayment.subTotalProduct)
//        val totalPayment = dialogLayout.findViewById<TextView>(R.id.p_total_payment)
//        setTotalPayment(totalPayment, dataPayment)
//        val cashReturned = dialogLayout.findViewById<TextView>(R.id.cash_returned)
//        setCashReturned(cashReturned, ctx)
//
//        val containerTotal = dialogLayout.findViewById<ConstraintLayout>(R.id.container_total)
//        val btnShowDesc = dialogLayout.findViewById<Button>(R.id.btn_show_desc)
//
//        btnShowDesc.setOnClickListener {
//            val s = "Show description"
//            val h = "Hide description"
//            if (containerTotal.visibility == View.GONE) {
//                containerTotal.visibility = View.VISIBLE
//                btnShowDesc.text = h
//            } else {
//                containerTotal.visibility = View.GONE
//                btnShowDesc.text = s
//            }
//        }
//
//        val cashConsumer = dialogLayout.findViewById<EditText>(R.id.cash_consumer)
//        val saveAndPrint = dialogLayout.findViewById<Button>(R.id.save_and_print)
//        val savePayment = dialogLayout.findViewById<Button>(R.id.save_payment)
//        val cancelPayment = dialogLayout.findViewById<Button>(R.id.cancel_payment)
//
//        // discount section
//        var needUpdateDiscountPercent = false
//        var needUpdateDiscountNominal = false
//        val discountNominal = dialogLayout.findViewById<EditText>(R.id.p_dis_nominal)
//        val discountPercent = dialogLayout.findViewById<EditText>(R.id.p_dis_percent)
//        discountPercent.addTextChangedListener(object :TextWatcher{
//            var isFinish = false
//            var cp = 0
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                cp = discountPercent.selectionStart
//            }
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                if (needUpdateDiscountPercent) {
//                    needUpdateDiscountPercent = false
//                    return
//                }
//                transactionEntity.discountPercent = helper.strToDouble(s.toString())
//                if (isFinish) {
//                    isFinish = false
//                    discountPercent.setSelection(cp)
//                    // set nominal discount
//                    needUpdateDiscountNominal = true
//                    transactionEntity.discountNominal = getDiscountNominal(transactionEntity.subTotalProduct,transactionEntity.discountPercent)
//                    setEditTextWithRupiahFormat(discountNominal, transactionEntity.discountNominal)
//                    setCashReturned(cashReturned, ctx)
//                    return setTotalPayment(totalPayment, dataPayment)
//                }
//                isFinish = true
//                discountPercent.setText(s.toString())
//            }
//        })
//        discountNominal.addTextChangedListener(object : TextWatcher {
//            var cp = 0
//            var isFinish = false
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                cp = discountNominal.selectionStart
//            }
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                try {
//                    if (needUpdateDiscountNominal) {
//                        needUpdateDiscountNominal = false
//                        return
//                    }
//                    if (isFinish) {
//                        isFinish = false
//                        setSelectionEditText(discountNominal, cp, transactionEntity.discountNominal)
//                        // set percent discount
//                        needUpdateDiscountPercent = true
//                        transactionEntity.discountPercent = 0.0
//                        discountPercent.setText("0")
//                        setCashReturned(cashReturned, ctx)
//                        return setTotalPayment(totalPayment, dataPayment)
//                    }
//                    val userInput = helper.rupiahToInt(s.toString())
//                    if (userInput == 0) {
//                        isFinish = true
//                        // set percent discount
//                        needUpdateDiscountPercent = true
//                        transactionEntity.discountPercent = 0.0
//                        discountPercent.setText("0")
//                        transactionEntity.discountNominal = 0
//                        discountNominal.setText("0")
//                        setCashReturned(cashReturned, ctx)
//                        return setTotalPayment(totalPayment, dataPayment)
//                    }
//                    if (transactionEntity.discountNominal == userInput) {
//                        isFinish = true
//                        setEditTextWithRupiahFormat(discountNominal, transactionEntity.discountNominal)
//                        setCashReturned(cashReturned, ctx)
//                        return
//                    }
//                    transactionEntity.discountNominal = userInput
//                    isFinish = true
//                    setEditTextWithRupiahFormat(discountNominal, transactionEntity.discountNominal)
//                } catch (e: Exception) {
//                    println(e.message.toString())
//                }
//            }
//        })
//
//        // tax section
//        var needUpdateTaxPercent = false
//        var needUpdateTaxNominal = false
//        val taxNominal = dialogLayout.findViewById<EditText>(R.id.p_tax_nominal)
//        val taxPercent = dialogLayout.findViewById<EditText>(R.id.p_tax_percent)
//        taxNominal.addTextChangedListener(object : TextWatcher {
//            var cp = 0
//            var isFinish = false
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                cp = taxNominal.selectionStart
//            }
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                try {
//                    if (needUpdateTaxNominal) {
//                        needUpdateTaxNominal = false
//                        return
//                    }
//                    if (isFinish) {
//                        isFinish = false
//                        setSelectionEditText(taxNominal, cp, transactionEntity.taxNominal)
//                        // set percent tax
//                        needUpdateTaxPercent = true
//                        transactionEntity.taxPercent = 0.0
//                        taxPercent.setText("0")
//                        setCashReturned(cashReturned, ctx)
//                        return setTotalPayment(totalPayment, dataPayment)
//                    }
//                    val userInput = helper.rupiahToInt(s.toString())
//                    if (userInput == 0) {
//                        isFinish = true
//                        transactionEntity.taxNominal = 0
//                        taxNominal.setText("")
//                        // set percent tax
//                        needUpdateTaxPercent = true
//                        transactionEntity.taxPercent = 0.0
//                        taxPercent.setText("0")
//                        setCashReturned(cashReturned, ctx)
//                        return setTotalPayment(totalPayment, dataPayment)
//                    }
//                    if (transactionEntity.taxNominal == userInput) {
//                        isFinish = true
//                        setEditTextWithRupiahFormat(taxNominal, transactionEntity.taxNominal)
//                        setCashReturned(cashReturned, ctx)
//                        return
//                    }
//                    transactionEntity.taxNominal = userInput
//                    isFinish = true
//                    setEditTextWithRupiahFormat(taxNominal, transactionEntity.taxNominal)
//                } catch (e: Exception) {
//                    println(e.message.toString())
//                }
//            }
//        })
//        taxPercent.addTextChangedListener(object :TextWatcher{
//            var isFinish = false
//            var cp = 0
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                cp = taxPercent.selectionStart
//            }
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                if (needUpdateTaxPercent) {
//                    needUpdateTaxPercent = false
//                    return
//                }
//                transactionEntity.taxPercent = helper.strToDouble(s.toString())
//                if (isFinish) {
//                    isFinish = false
//                    taxPercent.setSelection(cp)
//                    // set nominal tax
//                    needUpdateTaxNominal = true
//                    transactionEntity.taxNominal = getDiscountNominal(transactionEntity.subTotalProduct,transactionEntity.taxPercent)
//                    setEditTextWithRupiahFormat(taxNominal, transactionEntity.taxNominal)
//                    setCashReturned(cashReturned, ctx)
//                    return setTotalPayment(totalPayment, dataPayment)
//                }
//                isFinish = true
//                taxPercent.setText(s.toString())
//            }
//        })
//
//        // adm nominal section
//        val admNominal = dialogLayout.findViewById<EditText>(R.id.p_adm_nominal)
//        admNominal.addTextChangedListener(object : TextWatcher {
//            var cp = 0
//            var isFinish = false
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                cp = admNominal.selectionStart
//            }
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                try {
//                    if (isFinish) {
//                        isFinish = false
//                        setSelectionEditText(admNominal, cp, transactionEntity.adm)
//                        setTotalPayment(totalPayment, dataPayment)
//                        setCashReturned(cashReturned, ctx)
//                        return
//                    }
//                    val userInput = helper.rupiahToInt(s.toString())
//                    if (userInput == 0) {
//                        isFinish = true
//                        transactionEntity.adm = 0
//                        setTotalPayment(totalPayment, dataPayment)
//                        setCashReturned(cashReturned, ctx)
//                        admNominal.setText("")
//                        return
//                    }
//                    if (transactionEntity.adm == userInput) {
//                        isFinish = true
//                        setEditTextWithRupiahFormat(admNominal, transactionEntity.adm)
//                        setCashReturned(cashReturned, ctx)
//                        return
//                    }
//                    transactionEntity.adm = userInput
//                    isFinish = true
//                    setEditTextWithRupiahFormat(admNominal, transactionEntity.adm)
//                } catch (e: Exception) {
//                    println(e.message.toString())
//                }
//            }
//        })
//
//        // cash nominal section
//        val containerNominal = dialogLayout.findViewById<ConstraintLayout>(R.id.type_nominal)
//        val btnOpenContainerNominal = dialogLayout.findViewById<Button>(R.id.show_nominal)
//        btnOpenContainerNominal.setOnClickListener {
//            val s = "Show nominal"
//            val h = "Hide nominal"
//            if (containerNominal.visibility == View.GONE) {
//                containerNominal.visibility = View.VISIBLE
//                btnOpenContainerNominal.text = h
//            } else {
//                containerNominal.visibility = View.GONE
//                btnOpenContainerNominal.text = s
//            }
//        }
//        val btnSeRibu = dialogLayout.findViewById<Button>(R.id.btn_srb)
//        val btnDuaRibu = dialogLayout.findViewById<Button>(R.id.btn_drb)
//        val btnLimaRibu = dialogLayout.findViewById<Button>(R.id.btn_lrb)
//        val btnSePuluhRibu = dialogLayout.findViewById<Button>(R.id.btn_sprb)
//        val btnDuaPuluhRibu = dialogLayout.findViewById<Button>(R.id.btn_dprb)
//        val btnLimaPuluhRibu = dialogLayout.findViewById<Button>(R.id.btn_lmrb)
//        val btnSeratusRibu = dialogLayout.findViewById<Button>(R.id.btn_srbrb)
//        val btnResetCash = dialogLayout.findViewById<ImageButton>(R.id.btn_reset_cash)
//
//        btnSeRibu.setOnClickListener {addNominalToCash(ctx, 1000, cashConsumer, cashReturned)}
//        btnDuaRibu.setOnClickListener {addNominalToCash(ctx, 2000, cashConsumer, cashReturned)}
//        btnLimaRibu.setOnClickListener {addNominalToCash(ctx, 5000, cashConsumer, cashReturned)}
//        btnSePuluhRibu.setOnClickListener {addNominalToCash(ctx, 10000, cashConsumer, cashReturned)}
//        btnDuaPuluhRibu.setOnClickListener {addNominalToCash(ctx, 20000, cashConsumer, cashReturned)}
//        btnLimaPuluhRibu.setOnClickListener {addNominalToCash(ctx, 50000, cashConsumer, cashReturned)}
//        btnSeratusRibu.setOnClickListener {addNominalToCash(ctx, 100000, cashConsumer, cashReturned)}
//        btnResetCash.setOnClickListener {
//            transactionEntity.cash = 0
//            setCashNominal(cashConsumer, transactionEntity.cash)
//            setCashReturned(cashReturned, ctx)
//        }
//        cashConsumer.setRawInputType(2)
//        cashConsumer.addTextChangedListener(object : TextWatcher {
//            var cp = 0
//            var isFinish = false
//            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
//                cp = cashConsumer.selectionStart
//            }
//            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
//            override fun afterTextChanged(s: Editable?) {
//                try {
//                    if (isFinish) {
//                        isFinish = false
//                        setSelectionEditText(cashConsumer, cp, transactionEntity.cash)
//                        setTotalPayment(totalPayment, dataPayment)
//                        setCashReturned(cashReturned, ctx)
//                        return
//                    }
//                    val userInput = helper.rupiahToInt(s.toString())
//                    if (userInput == 0) {
//                        isFinish = true
//                        transactionEntity.cash = 0
//                        setCashReturned(cashReturned, ctx)
//                        setTotalPayment(totalPayment, dataPayment)
//                        cashConsumer.setText("")
//                        return
//                    }
//                    if (transactionEntity.cash == userInput) {
//                        isFinish = true
//                        setCashReturned(cashReturned, ctx)
//                        setEditTextWithRupiahFormat(cashConsumer, transactionEntity.cash)
//                        return
//                    }
//                    isFinish = true
//                    transactionEntity.cash = userInput
//                    setEditTextWithRupiahFormat(cashConsumer, transactionEntity.cash)
//                } catch (e: Exception) {
//                    println(e.message.toString())
//                }
//            }
//        })
//
//        val alertDialog = builder.setView(dialogLayout).show()
//
//        savePayment.setOnClickListener {
//            try {
//                if (!isCorrectCashReturner()) {
//                    helper.generateTOA(this, "Invalid cash nominal", true)
//                    return@setOnClickListener
//                }
//                saveDataTransactionToDb()
//                saveDataProductTransactionToDB()
//                helper.generateTOA(this, "Save", true)
//                alertDialog.dismiss()
//                finish()
//            } catch (e: Exception) {
//                helper.generateTOA(applicationContext, e.message.toString(), true)
//            }
//        }
//
//        saveAndPrint.setOnClickListener {
//            alertDialog.dismiss()
//            finish()
//        }
//
//        cancelPayment.setOnClickListener {
//            alertDialog.dismiss()
//        }
//    }
//
//    private fun saveDataTransactionToDb() {
//        transactionEntity.grandTotal = 0
//        transactionEntity.grandTotal += transactionEntity.subTotalProduct
//        transactionEntity.grandTotal -= transactionEntity.discountNominal
//        transactionEntity.grandTotal += transactionEntity.taxNominal
//        transactionEntity.grandTotal += transactionEntity.adm
//        transactionEntity.createdAt = helper.getCurrentDate()
//        transactionEntity.updatedAt = helper.getCurrentDate()
//        gposRepo.saveTransaction(transactionEntity)
//    }
//
//    private fun saveDataProductTransactionToDB() {
//        // update stock
//        for (p in productTransaction) {
//            val prod = gposRepo.getProductByID(p.pID)
//            val historyStockEntity = HistoryStockEntity(
//                pID= p.pID,
//                inStock= 0,
//                outStock= p.pQty,
//                currentStock= prod.stock - p.pQty,
//                purchase= 0,
//                transactionID= transactionEntity.id,
//                createdAt= transactionEntity.createdAt,
//            )
//            gposRepo.saveHistoryStock(historyStockEntity)
//            gposRepo.updateStockProduct(p.pID, prod.stock - p.pQty)
//        }
//        gposRepo.saveProductTransaction(productTransaction)
//    }
//
//    private fun getCurrentTotalPriceInCart(): Int {
//        var result = 0
//        for (p in productTransaction) {
//            val totalPrice = p.pPrice * p.pQty
//            val discountPercentage = p.pDiscount / 100
//            val totalDiscount = discountPercentage * totalPrice
//            val totalPriceAfterDiscount = totalPrice - totalDiscount
//            result += totalPriceAfterDiscount.toInt()
//        }
//
//        transactionEntity.subTotalProduct = result
//        return result
//    }
//
//    fun setEditTextWithRupiahFormat(e: EditText, nominal: Int) {
//        e.setText(helper.intToRupiah(nominal))
//    }
//
//    fun setSelectionEditText(edt: EditText, sS: Int, sE: Int) {
//        val selection = if (sS < sE.toString().length)
//            sS else helper.intToRupiah(sE).length
//        edt.setSelection(selection)
//    }
//
//    private fun getTotalPayment(t: TransactionEntity): Int {
//        return try {
//            var result = 0
//            result += t.subTotalProduct
//            result -= t.discountNominal
//            result += t.taxNominal
//            result += t.adm
//            result
//        } catch (e: Exception) {
//            0
//        }
//    }
//
//    fun setTotalPayment(t: TextView, cpe: CartPaymentEntity) {
//        var grandTotal = 0
//        grandTotal += cpe.subTotalProduct
//        grandTotal -= cpe.discountNominal
//        grandTotal += cpe.taxNominal
//        grandTotal += cpe.adm
//        val grandTotalStr = "Rp ${helper.intToRupiah(grandTotal)}"
//        t.text = grandTotalStr
//    }
//
//    fun getDiscountNominal(nominal: Int, discount: Double): Int {
//        val discountPercentage = discount / 100
//        val totalDiscount = discountPercentage * nominal
//        return totalDiscount.toInt()
//    }
//
//    private fun setCashNominal(e: EditText, n: Int) {
//        try {
//            val cashStr = helper.intToRupiah(n)
//            val cp = cashStr.length
//            e.setText(cashStr)
//            if (cp == 1) return
//            e.setSelection(cashStr.length)
//        } catch (e: Exception) {
//            helper.generateTOA(this, "something wrong", false)
//        }
//    }
//
//    private fun setCashReturned(t: TextView, ctx: Context) {
//        val totalPayment = getTotalPayment(transactionEntity)
//        val cashReturned = "${helper.intToRupiah(transactionEntity.cash - totalPayment)} ,-"
//
//        if (isCorrectCashReturner()) {
//            t.text = cashReturned
//            t.setTextColor(ContextCompat.getColor(ctx, R.color.black))
//        } else {
//            t.text = cashReturned
//            t.setTextColor(ContextCompat.getColor(ctx, R.color.red))
//        }
//    }
//
//    private fun isCorrectCashReturner(): Boolean {
//        return transactionEntity.cash - getTotalPayment(transactionEntity) >= 0
//    }
//
//    private fun addNominalToCash(ctx: Context, n: Int, cashNominal: EditText, cashReturned: TextView) {
//        transactionEntity.cash += n
//        setCashNominal(cashNominal, transactionEntity.cash)
//        setCashReturned(cashReturned, ctx)
//    }
//}