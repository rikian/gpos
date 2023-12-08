package com.gulali.gpos

import android.annotation.SuppressLint
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
import android.widget.Button
import android.widget.DatePicker
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.gulali.gpos.adapter.ProductDisplay
import com.gulali.gpos.adapter.TransactionDisplay
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.databinding.IndexBinding
import com.gulali.gpos.databinding.ProductDisplayBinding
import com.gulali.gpos.databinding.SettingBinding
import com.gulali.gpos.databinding.TransactionDisplayBinding
import com.gulali.gpos.helper.Helper
import java.util.Calendar

class Index: AppCompatActivity() {
    private lateinit var binding: IndexBinding
    private lateinit var pdBinding: ProductDisplayBinding
    private lateinit var tdBinding: TransactionDisplayBinding
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
    private lateinit var dataTransaction: List<TransactionEntity>
    private var doubleBackToExitPressedOnce = false
    private lateinit var dataOwner: List<OwnerEntity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IndexBinding.inflate(layoutInflater).also {
            binding = it
            pdBinding = binding.productsDisplay
            tdBinding = binding.transactionDisplay
            stBinding = binding.settingsDisplay
            setContentView(binding.root)
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val insetsCompat = toWindowInsetsCompat(insets, view)
                val isImeVisible = insetsCompat.isVisible(WindowInsetsCompat.Type.ime())
                // below line, do the necessary stuff:
                binding.bottomNavigationView.visibility = if (isImeVisible) View.GONE else View.VISIBLE
                view.onApplyWindowInsets(insets)
            }

            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
            helper = Helper()
            constant = Constant()
            scanner = helper.initBarcodeScanner(this)
            mediaPlayer = helper.initBeebSound(this)
            linearLayoutManagerProduct = LinearLayoutManager(this)
            dataProduct = gposRepo.getProducts()
            dataTransaction = gposRepo.getTransaction()
            dataOwner = gposRepo.getOwner()

            // index section
            initIndex(this)

            // transaction section
            initTransaction()

            // product section
            initProduct()
        }
    }

    override fun onStart() {
        super.onStart()
        if (dataOwner.isEmpty()) {
            Intent(this, Registration::class.java).also { reg -> startActivity(reg) }
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        if (tdBinding.transactionDisplay.visibility == View.VISIBLE) {
            displayTransaction(tdBinding.transactionList, gposRepo.getTransaction(), this)
            return
        }

        if (pdBinding.productsDisplay.visibility == View.VISIBLE) {
            dataProduct = gposRepo.getProducts()
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

    private fun initTransaction() {
        displayTransaction(tdBinding.transactionList, dataTransaction, this)
        tdBinding.btnNTransaction.setOnClickListener {
            Intent(this, AddTransaction::class.java).also {
                startActivity(it)
            }
        }

        tdBinding.btnScanTransaction.setOnClickListener {
            scanner.startScan()
                .addOnSuccessListener {
                    // add bib sound
                    if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
                        mediaPlayer?.start()
                    }

                    val dtBarcode = it.rawValue ?: return@addOnSuccessListener
                    tdBinding.inpBarcode.setText(dtBarcode)
                }
                .addOnCanceledListener {
                    // Task canceled
                }
                .addOnFailureListener {
                    Toast.makeText(applicationContext, it.message, Toast.LENGTH_SHORT).show()
                }
        }

        tdBinding.filterTransaction.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inflater = layoutInflater
            val dialogFilterTransaction = inflater.inflate(R.layout.transaction_filter,null)
            val alertDialog = builder.setView(dialogFilterTransaction)
            val dateStart = dialogFilterTransaction.findViewById<EditText>(R.id.f_date_start)
            val dateEnd = dialogFilterTransaction.findViewById<EditText>(R.id.f_date_end)
            dateStart.setOnClickListener{filterDate(dateStart, this)}
            dateEnd.setOnClickListener{filterDate(dateEnd, this)}
            dialogFilterTransaction.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
        }
    }

    private fun filterDate(e: EditText, ctx: Context) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(ctx, { _: DatePicker, pYear: Int, pMonth: Int, dayOfMonth: Int ->
            // Update the EditText with the selected date
            val selectedDate = "$dayOfMonth/${pMonth + 1}/$pYear"
            e.setText(selectedDate)
        },
            year, month, day
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
            dialogFilterTransaction.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()
        }
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
                    Intent(ctx, AddProduct::class.java).also {
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
    fun displayTransaction(tList: RecyclerView, tItems: List<TransactionEntity>, ctx: Context) {
        displayTransactionAdapter = TransactionDisplay(helper, tItems, ctx)
        displayTransactionAdapter.setOnItemClickListener(object : TransactionDisplay.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val tr = tItems[position]
                Intent(ctx, TransactionDetails::class.java).also {
                    it.putExtra(constant.idTransaction(), tr.id)
                    startActivity(it)
                }
            }
        })
        tList.adapter = displayTransactionAdapter
        displayTransactionAdapter.notifyDataSetChanged()
    }
}