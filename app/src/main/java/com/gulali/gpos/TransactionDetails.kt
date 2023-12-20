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
import com.gulali.gpos.database.CartEntity
import com.gulali.gpos.database.CartPaymentEntity
import com.gulali.gpos.database.DataTimeLong
import com.gulali.gpos.database.ProductTransaction
import com.gulali.gpos.database.Repository
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.databinding.TransactionDetailsBinding
import com.gulali.gpos.helper.Helper
import com.gulali.gpos.repository.CartRepo
import com.gulali.gpos.repository.CartRepoParam

class TransactionDetails : AppCompatActivity() {
    private lateinit var binding: TransactionDetailsBinding
    private lateinit var helper: Helper
    private lateinit var constant: Constant
    private lateinit var idTransaction: String
    private lateinit var gposRepo: Repository
    private lateinit var transaction: TransactionEntity
    private lateinit var productTransaction: List<ProductTransaction>
    private lateinit var cartRepo: CartRepo
    private var isFromPayment = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TransactionDetailsBinding.inflate(layoutInflater).also {
            binding = it
            setContentView(binding.root)
            helper = Helper()
            constant = Constant()
            val db = AdapterDb.getGposDatabase(this)
            gposRepo = db.repository()
            cartRepo = CartRepo(CartRepoParam(
                db,
                helper,
                this
            ))
            isFromPayment = intent.getBooleanExtra(constant.fromPayment(), false)
            idTransaction = intent.getStringExtra(constant.idTransaction()) ?: ""

            if (isFromPayment) {
                binding.tdtPrint.setOnClickListener {
                    val dt = cartRepo.getCartPayment(idTransaction)
                    val cp = cartRepo.getProductsInCart(idTransaction)
                    if (dt == null) {
                        helper.generateTOA(this, "Data transaction not found", true)
                        return@setOnClickListener
                    }
                    val dateAndTime = helper.getCurrentDate()
                    val dateTime = DataTimeLong(
                        created = dateAndTime,
                        updated = dateAndTime
                    )
                    gposRepo.saveTransaction(TransactionEntity(
                        id = dt.id,
                        dataTransaction = dt.dataTransaction,
                        date = dateTime
                    ))
                    val listProduct: MutableList<ProductTransaction> = mutableListOf()
                    for (ct in cp) {
                        listProduct.add(
                            ProductTransaction(
                                transactionID = dt.id,
                                product = ct.product,
                                date = dateTime
                            )
                        )
                    }
                    gposRepo.saveProductTransaction(listProduct)
                    cartRepo.truncateCartPayment()
                    val intent = Intent(this, Index::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
            } else {
                transaction = gposRepo.getTransactionById(idTransaction)[0]
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
    }

    private fun generateDetailPayment(
        ctx: Context,
        b: TransactionDetailsBinding,
        t: TransactionEntity,
        pt: List<ProductTransaction>,
        h: Helper
    ) {
        if (pt.isEmpty()) finish()
        if (t == null) finish()

        val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(t.date.created))
        val dateStr = "${dateTime.date} (${dateTime.time})"
        b.orederId.text = t.id
        b.dateOrder.text = dateStr

        if (t.dataTransaction.discountPercent > 0.0) {
            val disPercent = "(${t.dataTransaction.discountPercent} %)"
            b.pDisPercent.visibility = View.VISIBLE
            b.pDisPercent.text = disPercent
        }

        if (t.dataTransaction.taxPercent > 0.0) {
            val taxPercent = "(${t.dataTransaction.taxPercent} %)"
            b.pTaxPercent.visibility = View.VISIBLE
            b.pTaxPercent.text = taxPercent
        }

        val totalPayment = helper.getTotalPayment(t)
        val totalPaymentStr = "Rp${helper.intToRupiah(totalPayment)}"
        val disNominal = "- ${helper.intToRupiah(t.dataTransaction.discountNominal)}"
        val totalItemPayment = "(${t.dataTransaction.totalItem} item)"
        val cash = "Rp${helper.intToRupiah(t.dataTransaction.cash)}"
        val cashReturned = "Rp${helper.intToRupiah(t.dataTransaction.cash - totalPayment)}"
        b.pSubTotal.text = helper.intToRupiah(t.dataTransaction.subTotalProduct)
        b.pDisNominal.text = disNominal
        b.pTaxNominal.text = helper.intToRupiah(t.dataTransaction.taxNominal)
        b.pAdmNominal.text = helper.intToRupiah(t.dataTransaction.adm)
        b.totalItemPayment.text = totalItemPayment
        b.pTotalPayment.text = totalPaymentStr
        b.pCash.text = cash
        b.pReturned.text = cashReturned
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showProductInCart(products: List<ProductTransaction>, ctx: Context) {
        val pdCard = TransactionItem(helper, products, ctx)
        pdCard.setOnItemClickListener(object : TransactionItem.OnItemClickListener{
            override fun onItemClick(position: Int) {}
        })
        binding.cartProduct.adapter = pdCard
        pdCard.notifyDataSetChanged()
    }
}