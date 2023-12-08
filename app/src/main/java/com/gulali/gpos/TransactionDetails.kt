package com.gulali.gpos

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.gulali.gpos.adapter.TransactionItem
import com.gulali.gpos.constant.Constant
import com.gulali.gpos.database.AdapterDb
import com.gulali.gpos.database.ProductTransaction
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.databinding.TransactionDetailsBinding
import com.gulali.gpos.helper.Helper

class TransactionDetails : AppCompatActivity() {
    private lateinit var binding: TransactionDetailsBinding
    private lateinit var helper: Helper
    private lateinit var constant: Constant
    private lateinit var idTransaction: String
    private lateinit var gposRepo: Repository
    private lateinit var transaction: TransactionEntity
    private lateinit var productTransaction: List<ProductTransaction>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransactionDetailsBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            helper = Helper()
            constant = Constant()
            gposRepo = AdapterDb.getGposDatabase(applicationContext).repository()
            idTransaction = intent.getStringExtra(constant.idTransaction()) ?: ""
            transaction = gposRepo.getTransactionById(idTransaction)
            productTransaction = gposRepo.getProductTransactionByTransactionID(transaction.id)
            showProductInCart(productTransaction, this)
            generateDetailPayment(this, binding, transaction, productTransaction, helper)

            binding.tdtPrint.setOnClickListener{
                Intent(this, Print::class.java).also { print ->
                    print.putExtra(constant.idTransaction(), idTransaction)
                    startActivity(print)
                }
            }
        }
    }

    private fun generateDetailPayment(ctx: Context, b: TransactionDetailsBinding, t: TransactionEntity, pt: List<ProductTransaction>, h: Helper) {
        if (pt.isEmpty()) finish()
        if (t == null) finish()

        val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(t.createdAt))
        val dateStr = "${dateTime.date} (${dateTime.time})"
        b.orederId.text = t.id
        b.dateOrder.text = dateStr

        if (t.discountPercent > 0.0) {
            val disPercent = "(${t.discountPercent} %)"
            b.pDisPercent.visibility = View.VISIBLE
            b.pDisPercent.text = disPercent
        }

        if (t.taxPercent > 0.0) {
            val taxPercent = "(${t.taxPercent} %)"
            b.pTaxPercent.visibility = View.VISIBLE
            b.pTaxPercent.text = taxPercent
        }

        val totalPayment = helper.getTotalPayment(t)
        val totalPaymentStr = "Rp${helper.intToRupiah(totalPayment)}"
        val disNominal = "- ${helper.intToRupiah(t.discountNominal)}"
        val totalItemPayment = "(${t.item} item)"
        val cash = "Rp${helper.intToRupiah(t.cash)}"
        val cashReturned = "Rp${helper.intToRupiah(t.cash - totalPayment)}"
        b.pSubTotal.text = helper.intToRupiah(t.totalProduct)
        b.pDisNominal.text = disNominal
        b.pTaxNominal.text = helper.intToRupiah(t.taxNominal)
        b.pAdmNominal.text = helper.intToRupiah(t.adm)
        b.totalItemPayment.text = totalItemPayment
        b.pTotalPayment.text = totalPaymentStr
        b.pCash.text = cash
        b.pReturned.text = cashReturned
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showProductInCart(products: List<ProductTransaction>, ctx: Context) {
        val pdCard = TransactionItem(helper, products, applicationContext)
        pdCard.setOnItemClickListener(object : TransactionItem.OnItemClickListener{
            override fun onItemClick(position: Int) {}
        })
        binding.cartProduct.adapter = pdCard
        pdCard.notifyDataSetChanged()
    }
}