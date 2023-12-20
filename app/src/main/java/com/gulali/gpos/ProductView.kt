package com.gulali.gpos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.Repository
import com.gulali.gpos.databinding.ProductViewBinding
import com.gulali.gpos.helper.Helper

class ProductView: AppCompatActivity() {
    private val constant: Constant = Constant()
    private lateinit var binding: ProductViewBinding
    private lateinit var helper: Helper
    private lateinit var gposRepo: Repository
    private lateinit var productModel: ProductModel
    private var idProduct: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProductViewBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            idProduct = intent.getIntExtra(constant.product(), -1)
            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
            helper = Helper()
            binding.viewUpd.setOnClickListener{_ ->
                Intent(this@ProductView, AddProduct::class.java).also { next ->
                    next.putExtra(constant.needUpdate(), true)
                    next.putExtra(constant.product(), idProduct)
                    startActivity(next)
                }
            }
            binding.viewAddStock.setOnClickListener {
                Intent(this@ProductView, UpdateStock::class.java).also { next ->
                    next.putExtra(constant.needUpdate(), true)
                    next.putExtra(constant.product(), idProduct)
                    startActivity(next)
                }
            }
            binding.btnHstory.setOnClickListener {
                Intent(this@ProductView, HistoryStock::class.java).also { next ->
                    next.putExtra(constant.needUpdate(), true)
                    next.putExtra(constant.product(), idProduct)
                    startActivity(next)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (idProduct == -1) return finish()
        productModel = gposRepo.getProductByID(idProduct)
        val uri = helper.getUriFromGallery(contentResolver, productModel.img)
        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .into(binding.imageView2)
        }
        binding.pvName.text = productModel.name
        binding.pvPrice.text = helper.intToRupiah(productModel.price)
        binding.pvStock.text = productModel.stock.toString()
    }
}