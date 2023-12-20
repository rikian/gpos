package com.gulali.gpos

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.DataTimeLong
import com.gulali.gpos.database.HistoryStockEntity
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.database.Repository
import com.gulali.gpos.databinding.DialogUpdateStockBinding
import com.gulali.gpos.helper.Helper

class UpdateStock : AppCompatActivity() {
    private val constant: Constant = Constant()
    private var idProduct: Int = -1
    private lateinit var binding: DialogUpdateStockBinding
    private lateinit var helper: Helper
    private lateinit var gposRepo: Repository
    private lateinit var productModel: ProductModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DialogUpdateStockBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            idProduct = intent.getIntExtra(constant.product(), -1)
            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
            helper = Helper()
            binding.increase.isChecked = true
            binding.stockOk.setOnClickListener {
                updateStock(helper, productModel, binding, this@UpdateStock)
            }
            binding.stockCancel.setOnClickListener { finish() }
            binding.anpQtyTot.setRawInputType(2)
            binding.anpEdtPurchase.setRawInputType(2)
            helper.initialSockListener(binding.anpQtyMin, binding.anpQtyPlus, binding.anpQtyTot)
            binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
                when (checkedId) {
                    R.id.increase -> {
                        val textInc = "+ ${binding.anpQtyTot.text}"
                        binding.issetto.text = textInc
                    }
                    R.id.decrease -> {
                        val textDesc = "- ${binding.anpQtyTot.text}"
                        binding.issetto.text = textDesc
                    }
                }
            }
            binding.anpQtyTot.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(p0: Editable?) {
                    if (binding.increase.isChecked) {
                        val textInc = "+ ${binding.anpQtyTot.text}"
                        binding.issetto.text = textInc
                    } else {
                        val textDesc = "- ${binding.anpQtyTot.text}"
                        binding.issetto.text = textDesc
                    }
                    if (p0 != null) {
                        binding.anpQtyTot.setSelection(p0.length)
                    }
                }
            })
            var purchase = 0
            binding.anpEdtPurchase.addTextChangedListener(object : TextWatcher {
                var cp = 0
                var isFinish = false

                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                    cp = binding.anpEdtPurchase.selectionStart
                }

                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    try {
                        if (isFinish) {
                            isFinish = false
                            val sp3 = if (cp < purchase.toString().length)
                                cp else helper.intToRupiah(purchase).length
                            binding.anpEdtPurchase.setSelection(sp3)
                            return
                        }
                        val userInput = helper.rupiahToInt(s.toString())
                        if (purchase == userInput) {
                            isFinish = true
                            binding.anpEdtPurchase.setText(helper.intToRupiah(purchase))
                            return
                        }
                        purchase = userInput
                        isFinish = true
                        binding.anpEdtPurchase.setText(helper.intToRupiah(purchase))
                    } catch (e: Exception) {
                        println(e.message.toString())
                    }
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if (idProduct == -1) return finish()
        productModel = gposRepo.getProductByID(idProduct)
        binding.crStock.text = productModel.stock.toString()
    }

    private fun updateStock(h: Helper, pm: ProductModel, b: DialogUpdateStockBinding, ctx: Context) {
        val inpStock = h.strToInt(b.anpQtyTot.text.toString())
        if (inpStock == 0) {
            return h.generateTOA(
                ctx,
                "Stock cannot be empty",
                true
            )
        }
        val isIncStock = b.increase.isChecked
        val presentPurchase = h.rupiahToInt(b.anpEdtPurchase.text.toString())
        if (isIncStock && presentPurchase <= 0) {
            return h.generateTOA(ctx, "Purchase cannot be empty", true)
        }
        val dateTimeLong = DataTimeLong(
            created = helper.getCurrentDate(),
            updated = helper.getCurrentDate()
        )
        val historyStockEntity = HistoryStockEntity(
            pID = pm.id,
            inStock = 0,
            outStock = 0,
            currentStock = 0,
            purchase = 0,
            transactionID = "",
            date = dateTimeLong
        )
        // in stock
        if (isIncStock) {
            historyStockEntity.pID = pm.id
            historyStockEntity.inStock = inpStock
            historyStockEntity.outStock = 0
            historyStockEntity.currentStock = pm.stock + inpStock
            historyStockEntity.purchase = presentPurchase
            gposRepo.updateStockProduct(historyStockEntity.pID, pm.stock + inpStock)
            gposRepo.saveHistoryStock(historyStockEntity)
        } else {
            if (pm.stock - inpStock < 0) {
                return h.generateTOA(
                    ctx,
                    "Invalid current stock",
                    true
                )
            }
            historyStockEntity.inStock = 0
            historyStockEntity.outStock = inpStock
            historyStockEntity.currentStock = pm.stock - inpStock
            historyStockEntity.purchase = 0
            gposRepo.updateStockProduct(historyStockEntity.pID, pm.stock - inpStock)
            gposRepo.saveHistoryStock(historyStockEntity)
        }

        helper.generateTOA(
            ctx,
            "Success update stock",
            true
        )

        finish()
    }
}