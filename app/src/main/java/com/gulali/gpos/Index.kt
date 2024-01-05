package com.gulali.gpos

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.gulali.gpos.adapter.ProductDisplay
import com.gulali.gpos.adapter.TransactionDisplay
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.BoolObj
import com.gulali.gpos.database.CountAndSum
import com.gulali.gpos.database.IntObj
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.database.ParamTransactionFilter
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.StringObj
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.database.TransactionsModel
import com.gulali.gpos.database.repositories.Transactions
import com.gulali.gpos.databinding.IndexBinding
import com.gulali.gpos.databinding.ProductDisplayBinding
import com.gulali.gpos.databinding.SettingBinding
import com.gulali.gpos.databinding.TransactionDisplayBinding
import com.gulali.gpos.helper.Helper
import com.gulali.gpos.helper.TransactionHelperDisplay
import com.gulali.gpos.service.transaction.Transaction
import java.util.Calendar
import kotlin.math.ceil

data class TransactionDisplay(
    var isBarActive: BoolObj,
    var isFilterActive: BoolObj,
    var querySearchTransaction: StringObj,
    var data: TransactionDisplayData,
    var dataFilter: ParamTransactionFilter
)

data class TransactionDisplayData(
    var index: IntObj,
    var totalPage: IntObj,
    var details: TransactionsModel
)

class Index: AppCompatActivity() {
    private lateinit var binding: IndexBinding
    private lateinit var tdBinding: TransactionDisplayBinding
    private lateinit var pdBinding: ProductDisplayBinding
    private lateinit var stBinding: SettingBinding
    private lateinit var gposRepo: Repository
    private lateinit var displayProductAdapter: ProductDisplay
    private lateinit var displayTransactionAdapter: TransactionDisplay
    private lateinit var linearLayoutManagerProduct: LinearLayoutManager
    private lateinit var dataProduct: List<ProductModel>
    private lateinit var helper: Helper
    private lateinit var constant: Constant
    private lateinit var scanner: GmsBarcodeScanner
    private var mediaPlayer: MediaPlayer? = null
    private var doubleBackToExitPressedOnce = false
    private lateinit var dataOwner: List<OwnerEntity>
    private lateinit var transactionHelperDisplay: TransactionHelperDisplay

    // transaction
    private lateinit var transactionNew: ActivityResultLauncher<String>
    private lateinit var transactionDetailsContract: ActivityResultLauncher<String>
    private lateinit var trRepo: Transactions

    // data transaction
    private lateinit var trDisplay: com.gulali.gpos.TransactionDisplay

    private var isSearchActive = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IndexBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            pdBinding = binding.productsDisplay
            tdBinding = binding.transactionDisplay
            stBinding = binding.settingsDisplay
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val insetsCompat = toWindowInsetsCompat(insets, view)
                val isImeVisible = insetsCompat.isVisible(WindowInsetsCompat.Type.ime())
                // below line, do the necessary stuff:
                binding.bottomNavigationView.visibility =
                    if (isImeVisible) View.GONE else View.VISIBLE
                view.onApplyWindowInsets(insets)
            }

            gposRepo = AdapterDb.getGposDatabase(this).repository()
            trRepo = AdapterDb.getGposDatabase(this).transaction()
            helper = Helper()
            transactionHelperDisplay = TransactionHelperDisplay()
            constant = Constant()
            scanner = helper.initBarcodeScanner(this)
            val moduleInstallRequest =
                ModuleInstallRequest.newBuilder()
                    .addApi(scanner) //Add the scanner client to the module install request
                    .build()
            val moduleInstallClient = ModuleInstall.getClient(this)
            moduleInstallClient.installModules(moduleInstallRequest)
                .addOnFailureListener { ex ->
                    helper.generateTOA(
                        this,
                        ex.message.toString(),
                        true
                    )
                }
            mediaPlayer = helper.initBeebSound(this)
            linearLayoutManagerProduct = LinearLayoutManager(this)
            dataProduct = gposRepo.getProducts()
            dataOwner = gposRepo.getOwner()

            // index section
            initIndex(this)

            // transaction section
            trDisplay = TransactionDisplay(
                isBarActive = BoolObj(
                    value = false
                ),
                isFilterActive = BoolObj(
                    value = false
                ),
                querySearchTransaction = StringObj(
                    value = ""
                ),
                data = TransactionDisplayData(
                    index = IntObj(
                        value = 0
                    ),
                    totalPage = IntObj(
                        value = 0
                    ),
                    details = TransactionsModel(
                        count = CountAndSum(
                            count = 0,
                            sum = 0
                        ),
                        list = mutableListOf()
                    )
                ),
                dataFilter = ParamTransactionFilter(
                    totalStart= IntObj(
                        value = 0
                    ),
                    totalEnd= IntObj(
                        value = 0
                    ),
                    dateStart= 0L,
                    dateEnd= 0L,
                    pIndex= IntObj(
                        value = 0
                    ),
                    pSize= 10,
                )
            )
            initTransaction(this, tdBinding, helper, trDisplay)

            // product section
            initProduct()
        }
    }

    override fun onResume() {
        super.onResume()
        pdBinding.switchBarProduct.setOnCheckedChangeListener { _, b ->
            if (b) {
                pdBinding.statusProduct.visibility = View.GONE
            } else {
                pdBinding.statusProduct.visibility = View.VISIBLE
            }
        }

        if (pdBinding.productsDisplay.visibility == View.VISIBLE) {
            dataProduct = gposRepo.getProducts()
            pdBinding.totalStock.text = getTotalStock().toString()
            pdBinding.productsTotal.text = dataProduct.size.toString()
            val dtBarcode = pdBinding.inpBarcode.text.toString()
            if (dtBarcode != "") {
                val products = helper.filterProductByBarcode(dtBarcode, dataProduct)
                if (products.isNullOrEmpty()) {
                    helper.generateTOA(applicationContext, "product not found", true)
                    return
                }
                displayProducts(pdBinding.productList, products, applicationContext, contentResolver)
                pdBinding.inpBarcode.setSelection(dtBarcode.length)
                return
            }

            displayProducts(pdBinding.productList, dataProduct, this, contentResolver)
            return
        }
    }

    override fun onStart() {
        super.onStart()
//        if (dataOwner.isEmpty()) {
//            Intent(this, Registration::class.java).also { reg -> startActivity(reg) }
//            finish()
//        }
    }

    private fun initTransaction(ctx: Context, tdBinding: TransactionDisplayBinding, helper: Helper, trDisplay: com.gulali.gpos.TransactionDisplay) {
        transactionDetailsContract = registerForActivityResult(TransactionDetailsContract()) { isDataChanged ->
            if (isDataChanged) {
                helper.generateTOA(ctx, "data is changed...", true)
                // Data has been changed, update the label or perform any other action
                // For example: displayTransactionAdapter.updateItemLabel(position)
                return@registerForActivityResult
            }

            helper.generateTOA(ctx, "data not changed...", true)
        }

        transactionNew = registerForActivityResult(TransactionNew()) { isDataChanged ->
            if (!isDataChanged) return@registerForActivityResult
            newDisplayTransaction(ctx, false, tdBinding, helper)
        }

        // init header bar
        tdBinding.switchBar.setOnCheckedChangeListener { _, b ->
            if (b) {
                tdBinding.statusPurchase.visibility = View.GONE
            } else {
                tdBinding.statusPurchase.visibility = View.VISIBLE
            }
        }

        // set display transaction
        newDisplayTransaction(ctx, false, tdBinding, helper)

        // set btn transaction
        tdBinding.btnNTransaction.setOnClickListener {
            trDisplay.isFilterActive.value = false
            transactionNew.launch("ok")
        }

        // set clear field search transaction
        tdBinding.clearSearchTr.setOnClickListener {
            val currentQuery = tdBinding.seacrhTr.text.toString().trim()
            if (currentQuery == "") return@setOnClickListener
            trDisplay.querySearchTransaction.value = ""
            tdBinding.seacrhTr.setText("")
        }

        // set form input search transaction
        tdBinding.seacrhTr.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val currentQuery = s.toString().trim()
                if (currentQuery == "") {
                    newDisplayTransaction(ctx, false, tdBinding, helper)
                    return
                }
                if (trDisplay.querySearchTransaction.value == currentQuery) return
                trDisplay.querySearchTransaction.value = currentQuery
                val dtSearch = trRepo.getDataById(trDisplay.querySearchTransaction.value)
                transactionHelperDisplay.setPages(
                    currentPage = 0,
                    totalPage = 1,
                    tdBinding = tdBinding
                )
                displayTransaction(tdBinding.transactionList, dtSearch, ctx, true)
            }
        })

        // set dialog filter transaction
        tdBinding.filterTransaction.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogFilterTransaction = inflater.inflate(R.layout.transaction_filter,null)

            val btnApply: Button = dialogFilterTransaction.findViewById(R.id.btn_ftr_appy)
            val btnCancel: Button = dialogFilterTransaction.findViewById(R.id.btn_ftr_cancel)
            val btnReset: Button = dialogFilterTransaction.findViewById(R.id.btn_ftr_reset)

            // initial filter by grand total
            val totalStart: EditText = dialogFilterTransaction.findViewById(R.id.f_tot_start)
            val totalEnd: EditText = dialogFilterTransaction.findViewById(R.id.f_tot_end)
            totalStart.setRawInputType(2)
            totalEnd.setRawInputType(2)

            if (trDisplay.dataFilter.totalStart.value > 0) {
                totalStart.setText(helper.intToRupiah(trDisplay.dataFilter.totalStart.value))
            }

            if (trDisplay.dataFilter.totalEnd.value > 0) {
                if (trDisplay.dataFilter.totalEnd.value != 999999999) {
                    totalEnd.setText(helper.intToRupiah(trDisplay.dataFilter.totalEnd.value))
                }
            }

            totalStart.addTextChangedListener(
                SetRupiahToEditText(
                    helper,
                    totalStart,
                    trDisplay.dataFilter.totalStart
                )
            )

            totalEnd.addTextChangedListener(
                SetRupiahToEditText(
                    helper,
                    totalEnd,
                    trDisplay.dataFilter.totalEnd
                )
            )

            val dateStart = dialogFilterTransaction.findViewById<EditText>(R.id.f_date_start)
            val dateEnd = dialogFilterTransaction.findViewById<EditText>(R.id.f_date_end)
            dateStart.setOnClickListener{filterDate(dateStart, this)}
            dateEnd.setOnClickListener{filterDate(dateEnd, this)}

            val alertDialog = builder.setView(dialogFilterTransaction).show()

            btnApply.setOnClickListener apply@{
                trDisplay.dataFilter.dateStart = helper.parseDateStrToUnix(dateStart.text.toString())
                trDisplay.dataFilter.dateEnd = helper.parseToEndDateUnix(dateEnd.text.toString())

                // validation
                if (trDisplay.dataFilter.totalStart.value == 0 && trDisplay.dataFilter.totalEnd.value == 0 && trDisplay.dataFilter.dateStart == 0L && trDisplay.dataFilter.dateEnd == 0L) {
                    helper.generateTOA(this, "Invalid value", true)
                    return@apply
                }

                if (trDisplay.dataFilter.totalStart.value != 0 && trDisplay.dataFilter.totalEnd.value != 0) {
                    if (trDisplay.dataFilter.totalStart.value >= trDisplay.dataFilter.totalEnd.value) {
                        helper.generateTOA(ctx, "Total start must be less than to total end", true)
                        return@apply
                    }
                }

                if (trDisplay.dataFilter.dateStart != 0L && trDisplay.dataFilter.dateEnd != 0L) {
                    if (trDisplay.dataFilter.dateStart >= trDisplay.dataFilter.dateEnd) {
                        helper.generateTOA(ctx, "Date start must be less than to date end", true)
                        return@apply
                    }
                }

                if (trDisplay.dataFilter.totalEnd.value <= 0) {
                    trDisplay.dataFilter.totalEnd.value = 999999999
                }

                if (trDisplay.dataFilter.dateEnd <= 0L) {
                    trDisplay.dataFilter.dateEnd = helper.getCurrentEndDate()
                }

                // set index param to zero
                trDisplay.dataFilter.pIndex.value = 0
                trDisplay.data.details = trRepo.getDataFilterWithCount(trDisplay.dataFilter)

                // set default data index
                trDisplay.data.index.value = 0
                trDisplay.data.totalPage.value = getTotalPage(trDisplay.data.details.count.count, 10)
                if (trDisplay.data.totalPage.value == 0) trDisplay.data.totalPage.value = 1

                // display transaction
                displayTransaction(
                    tList = tdBinding.transactionList,
                    tItems = trDisplay.data.details.list,
                    ctx = ctx,
                    isSearch = true
                )

                transactionHelperDisplay.setPages(
                    currentPage = trDisplay.data.index.value,
                    totalPage = trDisplay.data.totalPage.value,
                    tdBinding = tdBinding
                )

                transactionHelperDisplay.setHeaderTransaction(
                    tdBinding = tdBinding,
                    totalTransaction = trDisplay.data.details.count.count,
                    totalAmount = trDisplay.data.details.count.sum,
                    helper = helper
                )

                trDisplay.isFilterActive.value = true
                alertDialog.dismiss()
            }

            btnCancel.setOnClickListener {
                alertDialog.dismiss()
            }

            btnReset.setOnClickListener {
                newDisplayTransaction(ctx, false, tdBinding, helper)
                alertDialog.dismiss()
            }
        }

        tdBinding.pgNextTr.setOnClickListener {
            if (trDisplay.data.index.value + 1 >= trDisplay.data.totalPage.value) {
                return@setOnClickListener
            }
            trDisplay.data.index.value += 1
            showFilter(it, trDisplay)
        }

        tdBinding.pgPrevTr.setOnClickListener {
            if (trDisplay.data.index.value - 1 < 0) {
                return@setOnClickListener
            }
            trDisplay.data.index.value -= 1
            showFilter(it, trDisplay)
        }

        tdBinding.trBtnJumpTo.setOnClickListener { view ->
            val target = helper.strToInt(tdBinding.trJumpTo.text.toString())
            if (target <= 0 || target > trDisplay.data.totalPage.value) return@setOnClickListener
            trDisplay.data.index.value = target - 1
            showFilter(view, trDisplay)
        }

        tdBinding.refreshTransaction.setOnRefreshListener {
            newDisplayTransaction(ctx, false, tdBinding, helper)
            tdBinding.refreshTransaction.isRefreshing = false
        }
    }

    private fun showFilter(view: View, trDisplay: com.gulali.gpos.TransactionDisplay) {
        if (trDisplay.isFilterActive.value) {
            // insert index to parameter
            trDisplay.dataFilter.pIndex.value = trDisplay.data.index.value
            trDisplay.data.details.list = trRepo.getDataFilter(
                totalStart = trDisplay.dataFilter.totalStart.value,
                totalEnd = trDisplay.dataFilter.totalEnd.value,
                dateStart = trDisplay.dataFilter.dateStart,
                dateEnd = trDisplay.dataFilter.dateEnd,
                pIndex = trDisplay.data.index.value,
                pSize = 10
            )
        } else {
            trDisplay.data.details.list = trRepo.getData(
                index = trDisplay.data.index.value,
                limit = 10
            )
        }

        transactionHelperDisplay.setPages(
            currentPage = trDisplay.data.index.value,
            totalPage = trDisplay.data.totalPage.value,
            tdBinding = tdBinding
        )

        displayTransaction(
            tdBinding.transactionList,
            trDisplay.data.details.list,
            this,
            true
        )

        // hide keyboard
        hideKeyboard(view)
    }

    private fun newDisplayTransaction(
        ctx: Context,
        isSearch: Boolean,
        tdBinding: TransactionDisplayBinding,
        helper: Helper,
    ) {
        // set default current page
        trDisplay.data.details = trRepo.getDataWithCount(
            index = 0,
            limit = 10
        )

        // set new total page
        trDisplay.data.index.value = 0
        trDisplay.data.totalPage.value = getTotalPage(trDisplay.data.details.count.count, 10)
        if (trDisplay.data.totalPage.value == 0) trDisplay.data.totalPage.value = 1

        // display transaction
        displayTransaction(
            tList = tdBinding.transactionList,
            tItems = trDisplay.data.details.list,
            ctx = ctx,
            isSearch = isSearch
        )

        transactionHelperDisplay.setPages(
            currentPage = trDisplay.data.index.value,
            totalPage = trDisplay.data.totalPage.value,
            tdBinding = tdBinding
        )
        transactionHelperDisplay.setHeaderTransaction(
            tdBinding = tdBinding,
            totalTransaction = trDisplay.data.details.count.count,
            totalAmount = trDisplay.data.details.count.sum,
            helper = helper
        )
        trDisplay.isFilterActive.value = false
    }

    private fun initIndex(ctx: Context) {
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.transaction -> {
                    stBinding.settingDisplay.visibility = View.GONE
                    pdBinding.productsDisplay.visibility = View.GONE
                    tdBinding.transactionDisplay.visibility = View.VISIBLE
                }
                R.id.products -> {
                    stBinding.settingDisplay.visibility = View.GONE
                    pdBinding.productsDisplay.visibility = View.VISIBLE
                    tdBinding.transactionDisplay.visibility = View.GONE
                    dataProduct = gposRepo.getProducts()
                    displayProducts(pdBinding.productList, dataProduct, ctx, contentResolver)
                    pdBinding.totalStock.text = getTotalStock().toString()
                }
                R.id.settings -> {
                    stBinding.settingDisplay.visibility = View.VISIBLE
                    pdBinding.productsDisplay.visibility = View.GONE
                    tdBinding.transactionDisplay.visibility = View.GONE
                    displaySetting(ctx, stBinding, dataOwner[0])
                }
                else -> {
                    return@setOnItemSelectedListener false
                }
            }
            return@setOnItemSelectedListener true
        }
    }

    private fun displaySetting(ctx: Context, sb: SettingBinding, oe: OwnerEntity) {
        sb.regOwner.setText(oe.owner)
        sb.regShopName.setText(oe.shop)
        sb.regAddress.setText(oe.address)
        sb.regPhone.setText(oe.phone)

        sb.updatePerin.setOnClickListener {
            val owner = sb.regOwner.text.toString().trim()
            if (owner == "") {
                helper.generateTOA(this, "owner cannot be empty", true)
                return@setOnClickListener
            }
            val shopName = sb.regShopName.text.toString().trim()
            if (shopName == "") {
                helper.generateTOA(this, "shop name cannot be empty", true)
                return@setOnClickListener
            }
            val regAddress = sb.regAddress.text.toString().trim()
            if (regAddress == "") {
                helper.generateTOA(this, "address cannot be empty", true)
                return@setOnClickListener
            }
            val regPhone = sb.regPhone.text.toString()
            if (regPhone == "") {
                helper.generateTOA(this, "phone number cannot be empty", true)
                return@setOnClickListener
            }
            oe.owner = owner
            oe.shop = shopName
            oe.address = regAddress
            oe.phone = regPhone
            gposRepo.updateOwner(oe)
        }

        sb.btnCloseAccount.setOnClickListener {
            val builder = AlertDialog.Builder(ctx)
            val inflater = layoutInflater
            val dialogLayout = inflater.inflate(R.layout.dialog_close_account, null)
            val alertDialog = builder.setView(dialogLayout).show() // Create and show the AlertDialog
            dialogLayout.findViewById<Button>(R.id.close_no).setOnClickListener {
                alertDialog.dismiss()
            }
            dialogLayout.findViewById<Button>(R.id.close_yes).setOnClickListener {
                try {
                    gposRepo.truncateOwnerTable()
                    gposRepo.truncateTransactionTable()
                    gposRepo.truncateUnitTable()
                    gposRepo.truncateCategoriesTable()

                    val intent = Intent(ctx, Registration::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    println(e.message)
                }
            }
        }
    }

    private fun filterDate(e: EditText, ctx: Context) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            ctx,
            { _: DatePicker, pYear: Int, pMonth: Int, dayOfMonth: Int ->
                // Update the EditText with the selected date
                val selectedDate = "$dayOfMonth/${pMonth + 1}/$pYear"
                e.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    private fun initProduct() {
        displayProducts(pdBinding.productList, dataProduct, this, contentResolver)
        pdBinding.btnAddProduct.setOnClickListener {
            Intent(this, AddProduct::class.java).also {
                startActivity(it)
            }
        }
        pdBinding.btnScanProduct.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener {
                    // add bib sound
                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        mediaPlayer?.start()
                    }

                    val dtBarcode = it.rawValue ?: return@addOnSuccessListener
                    pdBinding.inpBarcode.setText(dtBarcode)
                }
                .addOnCanceledListener {
                    // Task canceled
                }
                .addOnFailureListener {
                    Toast.makeText(applicationContext, it.message, Toast.LENGTH_SHORT).show()
                }
        }
        pdBinding.inpBarcode.addTextChangedListener(object : TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(query: Editable?) {
                if (query == null) return
                val products = helper.filterProductByName(query.toString(), dataProduct)
                displayProducts(pdBinding.productList, products, applicationContext, contentResolver)
            }
        })
        pdBinding.productsTotal.text = dataProduct.size.toString()

        pdBinding.filterProduct.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogFilterTransaction = inflater.inflate(R.layout.product_filter,null)
            val alertDialog = builder.setView(dialogFilterTransaction)
//            val dateStart = dialogFilterTransaction.findViewById<EditText>(R.id.f_date_start)
//            val dateEnd = dialogFilterTransaction.findViewById<EditText>(R.id.f_date_end)
//            dateStart.setOnClickListener{filterDate(dateStart, this)}
//            dateEnd.setOnClickListener{filterDate(dateEnd, this)}
//            dialogFilterTransaction.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
        }
    }

    private fun getTotalStock(): Int {
        var result = 0
        for (p in dataProduct) {
            result += p.stock
        }

        return result
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000) // Change this value to adjust the time threshold
    }

    @SuppressLint("NotifyDataSetChanged")
    fun displayProducts(pList: RecyclerView, products: List<ProductModel>, ctx: Context, cr: ContentResolver) {
        displayProductAdapter = ProductDisplay(products, helper, ctx, cr)
        displayProductAdapter.setOnItemClickListener(object : ProductDisplay.OnItemClickListener{
            override fun onItemClick(position: Int) {
                try {
                    val product = products[position]
                    Intent(ctx, ProductView::class.java).also {
                        it.putExtra(constant.needUpdate(), true)
                        it.putExtra(constant.product(), product.id)
                        startActivity(it)
                    }
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, e.message.toString(), Toast.LENGTH_SHORT).show()
                }
            }
        })
        pList.adapter = displayProductAdapter
        displayProductAdapter.notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun displayTransaction(
        tList: RecyclerView,
        tItems: List<TransactionEntity>,
        ctx: Context,
        isSearch: Boolean
    ) {
        tdBinding.transactionList.visibility = View.GONE
        tdBinding.nsvTransaction.smoothScrollTo(0,0)
        tdBinding.transactionList.scrollToPosition(0)
        tdBinding.shimmerLayout.visibility = View.VISIBLE
        tdBinding.shimmerLayout.startShimmer()
        displayTransactionAdapter = TransactionDisplay(helper, tItems, ctx, isSearch)
        displayTransactionAdapter.setOnItemClickListener(object : TransactionDisplay.OnItemClickListener{
            override fun onItemClick(position: Int) {
                if (isSearch) {
                    isSearchActive = true
                }
                val tr = tItems[position]
                transactionDetailsContract.launch(tr.id)
            }
        })
        tList.adapter = displayTransactionAdapter
        displayTransactionAdapter.notifyDataSetChanged()
        tdBinding.transactionList.postDelayed({
            // Stop the shimmer effect after the data is updated
            tdBinding.shimmerLayout.stopShimmer()
            tdBinding.shimmerLayout.visibility = View.GONE
            tdBinding.transactionList.visibility = View.VISIBLE
        }, 1000)
    }

    private fun hideKeyboard(view: View) {
        try {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        } catch (e: Exception) {
            helper.generateTOA(this, e.message.toString(), true)
        }
    }

    private fun getTotalPage(totalPage: Int, pageSize: Int): Int {
        return ceil(totalPage.toDouble() / pageSize).toInt()
    }
}

class SetRupiahToEditText(private var helper: Helper, private var editText: EditText, private var nominal: IntObj): TextWatcher {
    private var cp = 0
    private var isFinish = false
    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        cp = editText.selectionStart
    }
    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
    override fun afterTextChanged(s: Editable?) {
        try {
            if (isFinish) {
                isFinish = false
                helper.setSelectionEditText(editText, cp, nominal.value)
                return
            }
            val userInput = helper.rupiahToInt(s.toString())
            if (userInput == 0) {
                isFinish = true
                nominal.value = 0
                editText.setText("")
                return
            }
            if (nominal.value == userInput) {
                isFinish = true
                helper.setEditTextWithRupiahFormat(editText, nominal.value)
                return
            }
            nominal.value = userInput
            isFinish = true
            helper.setEditTextWithRupiahFormat(editText, nominal.value)
        } catch (e: Exception) {
            println(e.message.toString())
        }
    }
}

class TransactionDetailsContract : ActivityResultContract<String, Boolean>() {
    private var constant = Constant()

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, TransactionDetails::class.java).apply {
            putExtra(constant.idTransaction(), input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        // Here you can parse the result from TransactionDetails activity
        // For example, if the result code is RESULT_OK, return true indicating the data has changed
        return resultCode == Activity.RESULT_OK
    }
}

class TransactionNew : ActivityResultContract<String, Boolean>() {
    private var constant = Constant()

    override fun createIntent(context: Context, input: String): Intent {
        return Intent(context, Transaction::class.java).apply {
            putExtra("new_transaction", input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        // Here you can parse the result from TransactionDetails activity
        // For example, if the result code is RESULT_OK, return true indicating the data has changed
        return resultCode == Activity.RESULT_OK
    }
}
