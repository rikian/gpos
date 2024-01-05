package com.gulali.gpos

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.gulali.gpos.adapter.ProductInCart
import com.gulali.gpos.adapter.ProductInCartDetail
import com.gulali.gpos.adapter.ProductSearch
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.DataProduct
import com.gulali.gpos.database.DataTimeLong
import com.gulali.gpos.database.DataTransaction
import com.gulali.gpos.database.HistoryStockEntity
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.ProductTransaction
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.database.TransactionEntityHistory
import com.gulali.gpos.database.repositories.Products
import com.gulali.gpos.database.repositories.Transactions
import com.gulali.gpos.databinding.TransactionDetailsBinding
import com.gulali.gpos.databinding.TransactionUpdateBinding
import com.gulali.gpos.helper.BluetoothReceiver
import com.gulali.gpos.helper.Helper
import com.gulali.gpos.helper.Permission
import com.gulali.gpos.helper.Printer
import com.gulali.gpos.service.transaction.Product
import com.gulali.gpos.service.transaction.Transaction
import java.util.UUID

class TransactionDetails : AppCompatActivity() {
    private lateinit var binding: TransactionDetailsBinding
    private lateinit var db: AdapterDb
    private lateinit var helper: Helper
    private lateinit var constant: Constant
    private lateinit var idTransaction: String
    private lateinit var trRepo: Transactions
    private lateinit var prRepo: Products
    private lateinit var transaction: List<TransactionEntity>
    private lateinit var productTransaction: List<ProductTransaction>
    private var cart: MutableList<Product> = mutableListOf()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransactionDetailsBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            helper = Helper()
            constant = Constant()
            db = AdapterDb.getGposDatabase(this)
            trRepo = db.transaction()
            prRepo = db.products()
            idTransaction = intent.getStringExtra(constant.idTransaction()) ?: ""
            if (idTransaction == "") {
                helper.generateTOA(this, "Invalid id transaction", true)
                finish()
            }
            // get detail transaction
            transaction = trRepo.getDetailById(idTransaction)
            if (transaction.size != 1) {
                finish()
                return
            }
            // get product transaction
            productTransaction = trRepo.getProducts(idTransaction)

            // passing data to view
            // passing time and id
            val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(transaction[0].date.created))
            binding.dateOrder.text = dateTime.date
            binding.orederId.text = transaction[0].id
            binding.orderTime.text = dateTime.time

            // passing data cart
            val pdCard = ProductInCartDetail(helper, productTransaction, this)
            pdCard.setOnItemClickListener(object : ProductInCartDetail.OnItemClickListener{
                override fun onItemClick(position: Int) {}
            })
            binding.cartProduct.adapter = pdCard
            pdCard.notifyDataSetChanged()

            // set data paid
            binding.pSubTotal.text = helper.intToRupiah(transaction[0].dataTransaction.subTotalProduct)
            if (transaction[0].dataTransaction.discountPercent > 0.0) {
                val textDiscPercent = "(${transaction[0].dataTransaction.discountPercent} %)"
                binding.pDisPercent.text = textDiscPercent
                binding.pDisPercent.visibility = View.VISIBLE
            }
            if (transaction[0].dataTransaction.discountNominal > 0) {
                val textDiscNominal = "- ${helper.intToRupiah(transaction[0].dataTransaction.discountNominal)}"
                binding.pDisNominal.text = textDiscNominal
            } else {
                binding.pDisNominal.text = "0"
            }

            // set data tax
            if (transaction[0].dataTransaction.taxPercent > 0.0) {
                val textTaxPercent = "(${transaction[0].dataTransaction.taxPercent} %)"
                binding.pTaxPercent.text = textTaxPercent
                binding.pTaxPercent.visibility = View.VISIBLE
            }
            if (transaction[0].dataTransaction.taxNominal > 0) {
                val textTaxNominal = helper.intToRupiah(transaction[0].dataTransaction.taxNominal)
                binding.pTaxNominal.text = textTaxNominal
            } else {
                binding.pTaxNominal.text = "0"
            }

            // set adm
            if (transaction[0].dataTransaction.adm > 0) {
                binding.pAdmNominal.text = helper.intToRupiah(transaction[0].dataTransaction.adm)
            } else {
                binding.pAdmNominal.text = "0"
            }
            val textGrandTotal = "Rp${helper.intToRupiah(transaction[0].dataTransaction.grandTotal)}"
            val textCash = "Rp${helper.intToRupiah(transaction[0].dataTransaction.cash)}"
            val textReturned = "Rp${helper.intToRupiah(transaction[0].dataTransaction.cash - transaction[0].dataTransaction.grandTotal)}"
            binding.pTotalPayment.text = textGrandTotal
            binding.pCash.text = textCash
            binding.pReturned.text = textReturned

            onBackPressedDispatcher.addCallback(
                this@TransactionDetails,
                object : OnBackPressedCallback(true) { override fun handleOnBackPressed() {
                    if (binding.mainTransactionDetails.visibility == View.GONE) {
                        binding.updated.updateDisplay.visibility = View.GONE
                        binding.mainTransactionDetails.visibility = View.VISIBLE
                    } else {
                        finish()
                    }
                } }
            )

            binding.btnTdEdit.setOnClickListener {
                binding.mainTransactionDetails.visibility = View.GONE
                binding.updated.updateDisplay.visibility = View.VISIBLE

                val cartValue: MutableList<Product> = mutableListOf()

                for (p in productTransaction) {
                    val prod = prRepo.getProductByID(p.product.productID)
                    cartValue.add(
                        Product(
                        data = p.product,
                        img = prod.img,
                        stock = prod.stock
                    )
                    )
                }

                cart = cartValue

                TransactionUpdate(
                    ParamTransactionUpdate(
                        ctx = this,
                        binding = binding.updated,
                        transactionEntity = transaction[0],
                        helper= helper,
                        layoutInflater= layoutInflater,
                        cart= cart,
                        contentResolver = contentResolver,
                        prRepo= prRepo,
                        trRepo= trRepo
                    )
                ).initUpdateTransaction()
            }
        }
    }
}

data class ParamTransactionUpdate(
    var ctx: Context,
    var binding: TransactionUpdateBinding,
    var transactionEntity: TransactionEntity,
    var helper: Helper,
    var layoutInflater: LayoutInflater,
    var cart: MutableList<Product>,
    var contentResolver: ContentResolver,
    var prRepo: Products,
    var trRepo: Transactions
)

class TransactionUpdate(private var p: ParamTransactionUpdate): BluetoothReceiver.BluetoothStateListener {
    private lateinit var ctx: Context
    private lateinit var binding: TransactionUpdateBinding
    private lateinit var dataTransaction: DataTransaction
    private lateinit var dataTransactionHistory: DataTransaction
    private lateinit var helper: Helper
    private lateinit var layoutInflater: LayoutInflater
    private var querySearchProduct = ""
    private var cart: MutableList<Product> = mutableListOf()
    private var oldCart: MutableList<HistoryStockEntity> = mutableListOf()

    // database
    private lateinit var gposRepo: Repository
    private lateinit var prRepo: Products
    private lateinit var trRepo: Transactions

    // bluetooth for print
    private var alertDialogForPrintStruck: AlertDialog? = null
    private var alertDialogForShowTurnOnBluetooth: AlertDialog? = null
    private lateinit var requestBluetoothPermission: ActivityResultLauncher<String>
    private lateinit var permission: Permission
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var printer: Printer

    private lateinit var transactionEntity: TransactionEntity
    private var productTransaction: MutableList<ProductTransaction> = mutableListOf()
    private var isTimeOutFromSocket = false

    // barcode scanning
    private var scanBarcode: Boolean = false
    private lateinit var scanner: GmsBarcodeScanner
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var contentResolver: ContentResolver

    @SuppressLint("NotifyDataSetChanged")
    fun initUpdateTransaction() {
        this.ctx = p.ctx
        this.binding= p.binding
        this.transactionEntity= p.transactionEntity
        this.helper= p.helper
        this.layoutInflater= p.layoutInflater
        this.cart = p.cart
        this.contentResolver= p.contentResolver
        this.prRepo= p.prRepo
        this.trRepo= p.trRepo

        // set empty old cart if not empty
        if (oldCart.isNotEmpty()) {
            oldCart = mutableListOf()
        }

        for (c in cart) {
            val currentStock = c.stock + c.data.quantity
            oldCart.add(HistoryStockEntity(
                pID= c.data.productID,
                inStock= c.data.quantity,
                outStock= 0,
                currentStock= currentStock,
                purchase= 0,
                transactionID= transactionEntity.id,
                date = DataTimeLong(
                    created = 0,
                    updated = 0
                )
            ))
        }

        // init data transaction for history
        dataTransaction = transactionEntity.dataTransaction
        dataTransactionHistory = transactionEntity.dataTransaction.copy()

        showProductInCart(ctx)

        // scanner barcode
        this.mediaPlayer = helper.initBeebSound(ctx)
        this.scanner = helper.initBarcodeScanner(ctx)
        this.binding.btnScanBc.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener { result ->
                    // add bib sound
                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        mediaPlayer?.start()
                    }
                    scanBarcode = true
                    binding.searchProduct.setText(result.rawValue)
                }
                .addOnCanceledListener {
                    // Task canceled
                }
                .addOnFailureListener { err ->
                    helper.generateTOA(ctx, err.message.toString(), true)
                }
        }

        binding.searchProduct.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (scanBarcode) return
                val currentQuery = s.toString().lowercase()
                if (querySearchProduct == currentQuery) {
                    return
                }
                querySearchProduct = currentQuery
                val products = prRepo.getProductByName(querySearchProduct)
                if (products.isEmpty()) {
                    return
                }
                val pr = com.gulali.gpos.service.transaction.Pr(
                    products = products,
                    idPayment = "",
                    ly = layoutInflater,
                    h = helper,
                    ctx = ctx,
                    cr = contentResolver,
                    cart = cart,
                )
                binding.productSearchView.adapter = showAvailableProducts(pr)
            }
        })

        // show payment
        binding.payout.setOnClickListener {
            if (cart.isEmpty()) {
                return@setOnClickListener helper.generateTOA(
                    ctx,
                    "Please insert product to cart before payout",
                    true
                )
            }
            showPayment(ctx)
        }
    }

    private fun getTotalPayment(): Int {
        var grandTotal = 0
        grandTotal += dataTransaction.subTotalProduct
        grandTotal -= dataTransaction.discountNominal
        grandTotal += dataTransaction.taxNominal
        grandTotal += dataTransaction.adm
        return grandTotal
    }

    fun setTotalPayment(t: TextView) {
        val grandTotal = getTotalPayment()
        val grandTotalStr = "Rp ${helper.intToRupiah(grandTotal)}"
        t.text = grandTotalStr
    }

    private fun isCorrectCashReturner(): Boolean {
        return dataTransaction.cash - getTotalPayment() >= 0
    }

    private fun setCashReturned(t: TextView, ctx: Context) {
        val totalPayment = getTotalPayment()
        val cashReturned = "${helper.intToRupiah(dataTransaction.cash - totalPayment)} ,-"

        if (isCorrectCashReturner()) {
            t.text = cashReturned
            t.setTextColor(ContextCompat.getColor(ctx, R.color.black))
        } else {
            t.text = cashReturned
            t.setTextColor(ContextCompat.getColor(ctx, R.color.red))
        }
    }

    fun getDiscountNominal(nominal: Int, discount: Double): Int {
        val discountPercentage = discount / 100
        val totalDiscount = discountPercentage * nominal
        return totalDiscount.toInt()
    }

    fun setEditTextWithRupiahFormat(e: EditText, nominal: Int) {
        e.setText(helper.intToRupiah(nominal))
    }

    fun setSelectionEditText(edt: EditText, sS: Int, sE: Int) {
        val selection = if (sS < sE.toString().length)
            sS else helper.intToRupiah(sE).length
        edt.setSelection(selection)
    }

    private fun addNominalToCash(ctx: Context, n: Int, cashNominal: EditText, cashReturned: TextView) {
        dataTransaction.cash += n
        setCashNominal(cashNominal, dataTransaction.cash)
        setCashReturned(cashReturned, ctx)
    }

    private fun setCashNominal(e: EditText, n: Int) {
        try {
            val cashStr = helper.intToRupiah(n)
            val cp = cashStr.length
            e.setText(cashStr)
            if (cp == 1) return
            e.setSelection(cashStr.length)
        } catch (e: Exception) {
            helper.generateTOA(ctx, "something wrong", false)
        }
    }

    private fun showPayment(ctx: Context) {
        val statusCart = getStatusCart()

        // initial sub total product
        dataTransaction.totalItem = statusCart.totalItem
        dataTransaction.subTotalProduct= statusCart.subTotalProduct

        // initial builder
        val builder = AlertDialog.Builder(ctx)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.payment_display, null)

        // text display
        val totalItemPayment = "(${dataTransaction.totalItem} item)"
        dialogLayout.findViewById<TextView>(R.id.total_item_payment).text = totalItemPayment

        val subTotal = dialogLayout.findViewById<TextView>(R.id.p_sub_total)
        subTotal.text = helper.intToRupiah(dataTransaction.subTotalProduct)
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
        cashConsumer.setRawInputType(2)
        if (dataTransaction.cash != 0) {
            cashConsumer.setText(helper.intToRupiah(dataTransaction.cash))
        }
        val processPayment = dialogLayout.findViewById<Button>(R.id.process_payment)
        val cancelPayment = dialogLayout.findViewById<Button>(R.id.cancel_payment)

        // discount section
        var needUpdateDiscountPercent = false
        var needUpdateDiscountNominal = false
        val discountNominal = dialogLayout.findViewById<EditText>(R.id.p_dis_nominal)
        val discountPercent = dialogLayout.findViewById<EditText>(R.id.p_dis_percent)
        if (transactionEntity.dataTransaction.discountPercent != 0.0) {
            discountPercent.setText("${transactionEntity.dataTransaction.discountPercent}")
        }
        if (transactionEntity.dataTransaction.discountNominal != 0) {
            setEditTextWithRupiahFormat(discountNominal, transactionEntity.dataTransaction.discountNominal)
        }
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
                dataTransaction.discountPercent = helper.strToDouble(s.toString())
                if (isFinish) {
                    isFinish = false
                    discountPercent.setSelection(cp)
                    // set nominal discount
                    needUpdateDiscountNominal = true
                    dataTransaction.discountNominal = getDiscountNominal(dataTransaction.subTotalProduct,dataTransaction.discountPercent)
                    setEditTextWithRupiahFormat(discountNominal, dataTransaction.discountNominal)
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
                        setSelectionEditText(discountNominal, cp, dataTransaction.discountNominal)
                        // set percent discount
                        needUpdateDiscountPercent = true
                        dataTransaction.discountPercent = 0.0
                        discountPercent.setText("0")
                        setCashReturned(cashReturned, ctx)
                        return setTotalPayment(totalPayment)
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (userInput == 0) {
                        isFinish = true
                        // set percent discount
                        needUpdateDiscountPercent = true
                        dataTransaction.discountPercent = 0.0
                        discountPercent.setText("0")
                        dataTransaction.discountNominal = 0
                        discountNominal.setText("0")
                        setCashReturned(cashReturned, ctx)
                        return setTotalPayment(totalPayment)
                    }
                    if (dataTransaction.discountNominal == userInput) {
                        isFinish = true
                        setEditTextWithRupiahFormat(discountNominal, dataTransaction.discountNominal)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    dataTransaction.discountNominal = userInput
                    isFinish = true
                    setEditTextWithRupiahFormat(discountNominal, dataTransaction.discountNominal)
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
        if (transactionEntity.dataTransaction.taxPercent != 0.0) {
            taxPercent.setText("${transactionEntity.dataTransaction.taxPercent}")
        }
        if (transactionEntity.dataTransaction.taxNominal != 0) {
            setEditTextWithRupiahFormat(taxNominal, transactionEntity.dataTransaction.taxNominal)
        }
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
                        setSelectionEditText(taxNominal, cp, dataTransaction.taxNominal)
                        // set percent tax
                        needUpdateTaxPercent = true
                        dataTransaction.taxPercent = 0.0
                        taxPercent.setText("0")
                        setCashReturned(cashReturned, ctx)
                        return setTotalPayment(totalPayment)
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (userInput == 0) {
                        isFinish = true
                        dataTransaction.taxNominal = 0
                        taxNominal.setText("")
                        // set percent tax
                        needUpdateTaxPercent = true
                        dataTransaction.taxPercent = 0.0
                        taxPercent.setText("0")
                        setCashReturned(cashReturned, ctx)
                        return setTotalPayment(totalPayment)
                    }
                    if (dataTransaction.taxNominal == userInput) {
                        isFinish = true
                        setEditTextWithRupiahFormat(taxNominal, dataTransaction.taxNominal)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    dataTransaction.taxNominal = userInput
                    isFinish = true
                    setEditTextWithRupiahFormat(taxNominal, dataTransaction.taxNominal)
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
                dataTransaction.taxPercent = helper.strToDouble(s.toString())
                if (isFinish) {
                    isFinish = false
                    taxPercent.setSelection(cp)
                    // set nominal tax
                    needUpdateTaxNominal = true
                    dataTransaction.taxNominal = getDiscountNominal(dataTransaction.subTotalProduct,dataTransaction.taxPercent)
                    setEditTextWithRupiahFormat(taxNominal, dataTransaction.taxNominal)
                    setCashReturned(cashReturned, ctx)
                    return setTotalPayment(totalPayment)
                }
                isFinish = true
                taxPercent.setText(s.toString())
            }
        })

        // adm nominal section
        val admNominal = dialogLayout.findViewById<EditText>(R.id.p_adm_nominal)
        if (transactionEntity.dataTransaction.adm != 0) {
            setEditTextWithRupiahFormat(admNominal, transactionEntity.dataTransaction.adm)
        }
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
                        setSelectionEditText(admNominal, cp, dataTransaction.adm)
                        setTotalPayment(totalPayment)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (userInput == 0) {
                        isFinish = true
                        dataTransaction.adm = 0
                        setTotalPayment(totalPayment)
                        setCashReturned(cashReturned, ctx)
                        admNominal.setText("")
                        return
                    }
                    if (dataTransaction.adm == userInput) {
                        isFinish = true
                        setEditTextWithRupiahFormat(admNominal, dataTransaction.adm)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    dataTransaction.adm = userInput
                    isFinish = true
                    setEditTextWithRupiahFormat(admNominal, dataTransaction.adm)
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
            setCashNominal(cashConsumer, 0)
            dataTransaction.cash = 0
            setCashReturned(cashReturned, ctx)
        }
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
                        setSelectionEditText(cashConsumer, cp, dataTransaction.cash)
                        setTotalPayment(totalPayment)
                        setCashReturned(cashReturned, ctx)
                        return
                    }
                    val userInput = helper.rupiahToInt(s.toString())
                    if (userInput == 0) {
                        isFinish = true
                        dataTransaction.cash = 0
                        setCashReturned(cashReturned, ctx)
                        setTotalPayment(totalPayment)
                        cashConsumer.setText("")
                        return
                    }
                    if (dataTransaction.cash == userInput) {
                        isFinish = true
                        setCashReturned(cashReturned, ctx)
                        setEditTextWithRupiahFormat(cashConsumer, dataTransaction.cash)
                        return
                    }
                    isFinish = true
                    dataTransaction.cash = userInput
                    setEditTextWithRupiahFormat(cashConsumer, dataTransaction.cash)
                } catch (e: Exception) {
                    println(e.message.toString())
                }
            }
        })

        val alertDialog = builder.setView(dialogLayout).show()

        processPayment.setOnClickListener {
            try {
                // check cash returned
                dataTransaction.grandTotal = getTotalPayment()
                if (dataTransaction.cash - dataTransaction.grandTotal < 0) {
                    helper.generateTOA(ctx, "Invalid cash nominal", true)
                    return@setOnClickListener
                }
                val currentDate = helper.getCurrentDate()
                val date = DataTimeLong(
                    created = currentDate,
                    updated = currentDate
                )

                // transactions start
                // delete product transaction
                val statusDelete = trRepo.deleteProductByID(transactionEntity.id)
                if (statusDelete != oldCart.size) {
                    helper.generateTOA(ctx, "failed delete product in transaction db", true)
                    return@setOnClickListener
                }
                // return stock product transaction to product
                for (p in oldCart) {
                    prRepo.updateStockProduct(p.pID, p.currentStock)
                    // save history stock
                    p.date = date
                    prRepo.saveHistoryStock(p)
                }

                // save new product transaction
                for (cp in cart) {
                    val pt = ProductTransaction(
                        transactionID = transactionEntity.id,
                        product = cp.data,
                        date = date
                    )

                    // save data product transaction
                    trRepo.saveProduct(pt)
                    productTransaction.add(pt)
                    // update stock
                    val currentStock = cp.stock - cp.data.quantity
                    prRepo.updateStockProduct(cp.data.productID, currentStock)
                    // save history stock
                    prRepo.saveHistoryStock(
                        HistoryStockEntity(
                            pID= cp.data.productID,
                            inStock= 0,
                            outStock= cp.data.quantity,
                            currentStock= currentStock,
                            purchase= 0,
                            transactionID= transactionEntity.id,
                            date= date
                        )
                    )
                }

                // update table transaction
                transactionEntity = TransactionEntity(
                    id = transactionEntity.id,
                    dataTransaction = dataTransaction,
                    date = transactionEntity.date
                )
                transactionEntity.date.updated = currentDate
                transactionEntity.edited = true
                trRepo.updateData(transactionEntity)

                // save history transaction updated
                trRepo.saveHistoryUpdate(TransactionEntityHistory(
                    transactionID = transactionEntity.id,
                    version = 1,
                    dataTransaction = dataTransactionHistory,
                    date = date
                ))

                alertDialog.dismiss()
//                showAlertDialogForPrintStruck()
            } catch (e: Exception) {
                isTimeOutFromSocket = true
                helper.generateTOA(ctx, e.message.toString(), true)
            }
        }

        cancelPayment.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    private fun showAlertDialogForPrintStruck() {
        // initial builder
        val builder = AlertDialog.Builder(ctx)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_payout_success, null)
        val btnYes = dialogLayout.findViewById<Button>(R.id.print_yes)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.print_no)
        builder.setView(dialogLayout)
        alertDialogForPrintStruck = builder.create()
        alertDialogForPrintStruck?.setCanceledOnTouchOutside(false)
        alertDialogForPrintStruck?.setOnCancelListener{
            alertDialogForPrintStruck?.dismiss()
        }

        btnYes.setOnClickListener {
            printStruct()
        }

        btnCancel.setOnClickListener {
            alertDialogForPrintStruck?.dismiss()
        }

        alertDialogForPrintStruck?.show()
    }

    private fun printStruct() {
        // check bluetooth permission
        if (!permission.checkBluetoothPermission()) {
            return
        }
        bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter ?: return
        if (!bluetoothAdapter.isEnabled) {
            showDialogTurnOnBluetooth(ctx)
            return
        }
        val owner = gposRepo.getOwner()[0]
        if (owner.bluetoothPaired == "") {
            Intent(ctx, BluetoothSetting::class.java).also { btSetting ->
                ctx.startActivity(btSetting)
            }
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != permission.granted) {
                requestBluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT)
                return
            }
        }
        var isValidPairedDevice = false
        var deviceAddress = ""
        val pairedDevices = bluetoothAdapter.bondedDevices
        for (device in pairedDevices) {
            if (device.name != owner.bluetoothPaired) continue

            // Pair with the printer using the deviceAddress
            // You can use this deviceAddress to establish a connection later
            deviceAddress = device.address.toString()
            isValidPairedDevice = true
            break
        }

        if (!isValidPairedDevice || deviceAddress == "") {
            return helper.generateTOA(ctx, "Cannot print struck because some data paired device not valid\n\nPlease make sure you have paired bluetooth device.", true)
        }

        val device = bluetoothAdapter.getRemoteDevice(deviceAddress)
        val uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        val socket = device.createRfcommSocketToServiceRecord(uuid)
        try {
            socket.connect()
            val oS = socket.outputStream
            val dataPrint = printer.generateStruckPayment(transactionEntity, owner, productTransaction)
            for (d in dataPrint) {
                oS.write(d)
            }
            oS.flush()
            oS.close()
            alertDialogForPrintStruck?.dismiss()
        } catch (e: Exception) {
            helper.generateTOA(ctx, "Socket timeout!!!\n\ncan not connect to device. is printer on?", true)
            socket.close()
        }
    }

    private fun showDialogTurnOnBluetooth(ctx: Context) {
        val builder = AlertDialog.Builder(ctx)
        val inflater = layoutInflater
        val dialogLayout = inflater.inflate(R.layout.dialog_open_bluetooth, null)
        val btnOk = dialogLayout.findViewById<Button>(R.id.dbt_yes)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.dbt_no)
        builder.setView(dialogLayout)
        alertDialogForShowTurnOnBluetooth = builder.create() // Create the AlertDialog
        alertDialogForShowTurnOnBluetooth?.setCanceledOnTouchOutside(false)
        alertDialogForShowTurnOnBluetooth?.setOnCancelListener{
            alertDialogForShowTurnOnBluetooth?.dismiss()
        }
        btnOk.setOnClickListener {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            ctx.startActivity(intent)
            alertDialogForShowTurnOnBluetooth?.dismiss() // Dismiss the dialog after starting the Bluetooth settings activity
        }
        btnCancel.setOnClickListener {
            alertDialogForShowTurnOnBluetooth?.dismiss()
        }
        alertDialogForShowTurnOnBluetooth?.show() // Show the AlertDialog after configuring it
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showAvailableProducts(pr: com.gulali.gpos.service.transaction.Pr): ProductSearch {
        val productSearch = ProductSearch(pr.products, pr.h, pr.ctx, pr.cr)
        productSearch.setOnItemClickListener(object : ProductSearch.OnItemClickListener {
            override fun onItemClick(position: Int) {
                // check stock
                val product = pr.products[position]
                checkAvailableProduct(
                    product,
                    pr.h,
                    pr.ctx,
                    pr.cart
                )
            }
        })
        productSearch.notifyDataSetChanged()
        return productSearch
    }

    private fun checkAvailableProduct(
        product: ProductModel,
        helper: Helper,
        context: Context,
        cart: List<Product>
    ) {
        if (product.stock <= 0) {
            return helper.generateTOA(context, "Stock empty", true)
        }
        // check product exist or not in the cart
        for (c in cart) {
            if (c.data.productID == product.id) {
                return helper.generateTOA(context, "Product exist in the cart", true)
            }
        }
        val p = Product(
            data = DataProduct(
                productID = product.id,
                name = product.name,
                quantity = 1,
                unit = product.unit,
                price = product.price,
                discountPercent = 0.0,
                discountNominal = 0,
                totalBeforeDiscount = 0,
                totalAfterDiscount = product.price
            ),
            img = product.img,
            stock = product.stock
        )

        showDialogForInputProductQty(
            helper,
            p,
            false,
            context,
            0
        )
    }

    private fun showDialogForInputProductQty(
        helper: Helper,
        product: Product,
        isUpdate: Boolean,
        ctx: Context,
        position: Int
    ) {
        // create default product variable for display
        val productID = product.data.productID
        val name = product.data.name
        val img = product.img
        val stock = product.stock
        val unit = product.data.unit
        val price = product.data.price

        // create default product variable for update later
        var quantity = 1
        var discountPercent = 0.0
        var discountNominal: Int
        var totalBeforeDiscount: Int
        var totalAfterDiscount: Int

        // update product variable if 'update' == true
        if (isUpdate) {
            quantity = product.data.quantity
            discountPercent = product.data.discountPercent
            discountNominal = product.data.discountNominal
            totalBeforeDiscount = product.data.totalBeforeDiscount
            totalAfterDiscount = product.data.totalAfterDiscount
        }

        // show dialog for input quantity
        val builder = AlertDialog.Builder(ctx)
        val dialogLayout = layoutInflater.inflate(R.layout.product_qty, null)
        val alertDialog = builder.setView(dialogLayout).show() // Create and show the AlertDialog

        // text and button display
        val displayImage = dialogLayout.findViewById<ImageView>(R.id.product_image)
        val displayName = dialogLayout.findViewById<TextView>(R.id.product_name)
        val displayStock = dialogLayout.findViewById<TextView>(R.id.product_stock)
        val displayUnit = dialogLayout.findViewById<TextView>(R.id.product_unit)
        val displayPrice = dialogLayout.findViewById<TextView>(R.id.product_price)
        val priceBeforeDiscount = dialogLayout.findViewById<TextView>(R.id.anp_qty_totpr)
        val priceAfterDiscount = dialogLayout.findViewById<TextView>(R.id.tot_af_dis)
        val pdOk = dialogLayout.findViewById<Button>(R.id.btn_qty_ok)
        val pdCancel = dialogLayout.findViewById<Button>(R.id.btn_qty_cancel)
        val pdDelete = dialogLayout.findViewById<Button>(R.id.btn_del_pic)

        // initial input
        val pdQtyMin = dialogLayout.findViewById<Button>(R.id.anp_qty_min)
        val pdQtyPlus = dialogLayout.findViewById<Button>(R.id.anp_qty_plus)
        val pdQty = dialogLayout.findViewById<EditText>(R.id.anp_qty_tot)
        val pdDiscount = dialogLayout.findViewById<EditText>(R.id.anp_discount)

        // initial image and text display
        val uri = helper.getUriFromGallery(ctx.contentResolver, img)
        if (uri != null) {
            Glide.with(ctx)
                .load(uri)
                .into(displayImage)
        }
        displayName.text = name
        displayStock.text = stock.toString()
        displayUnit.text = unit
        displayPrice.text = helper.intToRupiah(price)

        // initial total price
        helper.setPriceAfterDiscount(
            priceBeforeDiscount,
            priceAfterDiscount,
            discountPercent,
            price,
            quantity,
            true,
            ctx
        )

        // initial total price
        if (isUpdate) {
            pdDiscount.setText(discountPercent.toString())
            pdQty.setText(quantity.toString())
            pdCancel.visibility = View.GONE
            pdDelete.visibility = View.VISIBLE
            pdDelete.setOnClickListener {
                cart.removeAt(position)
                showProductInCart(
                    ctx,
                )
                val statusCart = getStatusCart()
                binding.totpricecart.text = helper.intToRupiah(statusCart.subTotalProduct)
                val totItemValue = "(${statusCart.totalItem} item)"
                binding.totitem.text = totItemValue
                alertDialog.dismiss()
            }
        } else {
            pdCancel.setOnClickListener { alertDialog.dismiss() }
        }

        // set listener quantity
        pdQty.addTextChangedListener(object : TextWatcher {
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
                    helper.setPriceAfterDiscount(
                        priceBeforeDiscount,
                        priceAfterDiscount,
                        discountPercent,
                        price,
                        1,
                        true,
                        ctx
                    )
                    quantity = 1
                    pdQty.setText("1")
                    return
                }
                if (value > stock) {
                    isFinish = true
                    helper.setPriceAfterDiscount(
                        priceBeforeDiscount,
                        priceAfterDiscount,
                        discountPercent,
                        price,
                        stock,
                        true,
                        ctx
                    )
                    quantity = stock
                    pdQty.setText(stock.toString())
                    return
                }

                quantity = value
                helper.setPriceAfterDiscount(
                    priceBeforeDiscount,
                    priceAfterDiscount,
                    discountPercent,
                    price,
                    value,
                    true,
                    ctx
                )
            }
        })
        pdQtyMin.setOnClickListener {
            val value = pdQty.text.toString().toIntOrNull()
            if (value == null || value - 1 <= 0) {
                pdQty.setText("1")
                pdQty.setSelection(pdQty.text.length)
                quantity = 1
                helper.setPriceAfterDiscount(
                    priceBeforeDiscount,
                    priceAfterDiscount,
                    discountPercent,
                    price,
                    1,
                    true,
                    ctx
                )
                return@setOnClickListener
            }

            pdQty.setText((value - 1).toString())
            pdQty.setSelection(pdQty.text.length)
            quantity = value - 1
            helper.setPriceAfterDiscount(
                priceBeforeDiscount,
                priceAfterDiscount,
                discountPercent,
                price,
                value - 1,
                true,
                ctx
            )
        }
        pdQtyPlus.setOnClickListener {
            val value = pdQty.text.toString().toIntOrNull() ?: return@setOnClickListener
            if (value < stock) {
                val textQtyValue = (value + 1).toString()
                pdQty.setText(textQtyValue)
                pdQty.setSelection(pdQty.text.length)
                quantity = value + 1
                helper.setPriceAfterDiscount(
                    priceBeforeDiscount,
                    priceAfterDiscount,
                    discountPercent,
                    price,
                    value + 1,
                    true,
                    ctx
                )
            }
        }

        // set listener discount
        pdDiscount.addTextChangedListener(object : TextWatcher {
            var isFinish = false
            var cp = 0
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = pdDiscount.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                discountPercent = helper.strToDouble(s.toString())
                if (isFinish) {
                    isFinish = false
                    pdDiscount.setSelection(cp)
                    helper.setPriceAfterDiscount(
                        priceBeforeDiscount,
                        priceAfterDiscount,
                        discountPercent,
                        price,
                        quantity,
                        true,
                        ctx
                    )
                    return
                }
                isFinish = true
                pdDiscount.setText(s.toString())
            }
        })

        // set listener for button ok
        pdOk.setOnClickListener {
            // set total after and before discount
            discountNominal = helper.getDiscountNominal(discountPercent, price, quantity)
            totalBeforeDiscount = price * quantity
            totalAfterDiscount = totalBeforeDiscount - discountNominal

            if (isUpdate) {
                cart[position].data.quantity = quantity
                cart[position].data.discountPercent = discountPercent
                cart[position].data.discountNominal = discountNominal
                cart[position].data.totalBeforeDiscount = totalBeforeDiscount
                cart[position].data.totalAfterDiscount = totalAfterDiscount
            } else {
                cart.add(0, Product(
                    DataProduct(
                        productID,
                        name,
                        quantity,
                        unit,
                        price,
                        discountPercent,
                        discountNominal,
                        totalBeforeDiscount,
                        totalAfterDiscount
                    ),
                    img,
                    stock
                )
                )
            }

            showProductInCart(ctx)
            alertDialog.dismiss()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showProductInCart(ctx: Context) {
        val pdCard = ProductInCart(helper, cart, ctx)
        pdCard.setOnItemClickListener(object : ProductInCart.OnItemClickListener{
            override fun onItemClick(position: Int) {
                try {
                    val c = cart[position]
                    showDialogForInputProductQty(helper, c, true, ctx, position)
                } catch (e: Exception) {
                    helper.generateTOA(ctx, "product not found", true)
                }
            }
        })
        binding.cartProduct.adapter = pdCard
        pdCard.notifyDataSetChanged()
        val statusCart = getStatusCart()
        binding.totpricecart.text = helper.intToRupiah(statusCart.subTotalProduct)
        val totItemValue = "(${statusCart.totalItem} item)"
        binding.totitem.text = totItemValue
    }

    private fun getStatusCart(): com.gulali.gpos.service.transaction.StatusCart {
        val result = com.gulali.gpos.service.transaction.StatusCart(
            subTotalProduct = 0,
            totalItem = 0,
        )
        for (c in cart) {
            result.totalItem += 1
            result.subTotalProduct += c.data.totalAfterDiscount
        }

        return  result
    }

    override fun onBluetoothConnected() {}

    override fun onBluetoothDisconnected() {
        if (alertDialogForPrintStruck != null) {
            showDialogTurnOnBluetooth(ctx)
        }
    }

    override fun onBluetoothActionError(action: String) {}

    override fun onBluetoothStateError(state: String) {}
}

//                val result = true // Set this based on whether data has changed or not
//                val resultIntent = Intent().apply {
//                    // Set any data to be returned, if needed
//                }
//                setResult(if (result) Activity.RESULT_OK else Activity.RESULT_CANCELED, resultIntent)