package com.gulali.gpos

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.gulali.gpos.adapter.ProductDisplay
import com.gulali.gpos.adapter.TransactionDisplay
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.databinding.IndexBinding
import com.gulali.gpos.databinding.ProductDisplayBinding
import com.gulali.gpos.databinding.TransactionDisplayBinding
import com.gulali.gpos.helper.Helper

class Index: AppCompatActivity() {
    private lateinit var binding: IndexBinding
    private lateinit var pdBinding: ProductDisplayBinding
    private lateinit var tdBinding: TransactionDisplayBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        IndexBinding.inflate(layoutInflater).also {
            binding = it
            pdBinding = binding.productsDisplay
            tdBinding = binding.transactionDisplay
            setContentView(binding.root)
            helper = Helper()
            constant = Constant()
            scanner = helper.initBarcodeScanner(this)
            mediaPlayer = helper.initBeebSound(this)
            linearLayoutManagerProduct = LinearLayoutManager(this)
            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
            dataProduct = gposRepo.getProducts()
            dataTransaction = gposRepo.getTransaction()

            // index section
            initIndex()

            // transaction section
            initTransaction()

            // product section
            initProduct()
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

    private fun initIndex() {
        binding.bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.transaction -> {
                    tdBinding.transactionDisplay.visibility = View.VISIBLE
                    pdBinding.productsDisplay.visibility = View.GONE
                }
                R.id.products -> {
                    tdBinding.transactionDisplay.visibility = View.GONE
                    pdBinding.productsDisplay.visibility = View.VISIBLE
                    dataProduct = gposRepo.getProducts()
                    displayProducts(pdBinding.productList, dataProduct, this, contentResolver)
                }
                R.id.settings -> {
                    Intent(this, Setting::class.java).also { intent ->
                        startActivity(intent)
                    }
                }
                else -> {
                    return@setOnItemSelectedListener false
                }
            }
            return@setOnItemSelectedListener true
        }
    }

    private fun initTransaction() {
        displayTransaction(tdBinding.transactionList, dataTransaction, this)
        tdBinding.btnNTransaction.setOnClickListener {
            Intent(this, AddTransaction::class.java).also {
                startActivity(it)
            }
        }
    }

    private fun initProduct() {
        displayProducts(pdBinding.productList, dataProduct, this, contentResolver)
        pdBinding.btnAddProduct.setOnClickListener {
            Intent(this, AddProduct::class.java).also {
                startActivity(it)
            }
        }
        pdBinding.btnScanBc2.setOnClickListener {
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
            override fun onItemClick(position: Int) {}
        })
        tList.adapter = displayTransactionAdapter
        displayTransactionAdapter.notifyDataSetChanged()
    }
}