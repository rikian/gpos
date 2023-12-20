package com.gulali.gpos.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentResolver
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.gulali.gpos.R
import com.gulali.gpos.adapter.ProductInCart
import com.gulali.gpos.adapter.ProductSearch
import com.gulali.gpos.database.CartEntity
import com.gulali.gpos.database.DataProduct
import com.gulali.gpos.database.DataTimeLong
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.databinding.TransactionAddBinding
import com.gulali.gpos.repository.CartRepo

class Dialog(
    private var activity: Activity,
    private var cr: CartRepo,
    private var contentResolver: ContentResolver,
    private var helper: Helper,
    private val layoutInflater: LayoutInflater,
    private val binding: TransactionAddBinding
) {
    fun showDialogExit(inflater: LayoutInflater) {
        val builder = AlertDialog.Builder(activity)
        val dialogLayout = inflater.inflate(R.layout.dialog_exit, null)
        val btnOk = dialogLayout.findViewById<Button>(R.id.exit_yes)
        val btnCancel = dialogLayout.findViewById<Button>(R.id.exit_no)
        val alertDialog = builder.setView(dialogLayout).show() // Create and show the AlertDialog

        btnOk.setOnClickListener {
            alertDialog.dismiss()
            cr.truncateCartPayment()
            activity.finish()
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun showAvailableProducts(products: List<ProductModel>, idPayment: String): ProductSearch {
        val productSearch = ProductSearch(products, this.helper, this.activity, contentResolver)
        productSearch.setOnItemClickListener(object : ProductSearch.OnItemClickListener {
            override fun onItemClick(position: Int) {
                checkExistenceProduct(products[position], idPayment)
            }
        })
        productSearch.notifyDataSetChanged()
        return productSearch
    }

    private fun checkExistenceProduct(p: ProductModel, idPayment: String) {
        // check stock product
        if (p.stock <= 0) {
            helper.generateTOA(
                this.activity,
                "Stock empty!!",
                true
            )
            return
        }
        // check product exist or not in table cart
        val isProductExistInCart = this.cr.getProductInCart(idPayment, p.id)
        if (isProductExistInCart != null) return helper.generateTOA(
            this.activity,
            "Product exist in cart",
            true
        )
        showDialogInputProductQty(p, null, idPayment, false, this.activity)
    }

    private fun showDialogInputProductQty(
        p: ProductModel,
        pc: CartEntity?,
        transactionID: String,
        isUpdate: Boolean,
        ctx: Context
    ) {
        var pro = DataProduct(
            productID= p.id,
            name= p.name,
            quantity= 1,
            unit = p.unit,
            price= p.price,
            discountPercent= 0.0,
            discountNominal= 0,
            totalBeforeDiscount= 0,
            totalAfterDiscount= 0
        )
        val prod: CartEntity = pc
            ?: CartEntity(
                transactionID = transactionID,
                product = pro
            )

        // show dialog for input quantity
        val builder = AlertDialog.Builder(ctx)
        val inflater = this.layoutInflater
        val dialogLayout = inflater.inflate(R.layout.product_qty, null)
        val alertDialog = builder.setView(dialogLayout).show() // Create and show the AlertDialog

        // text display
        val displayImage  = dialogLayout.findViewById<ImageView>(R.id.product_image)
        val displayName  = dialogLayout.findViewById<TextView>(R.id.product_name)
        val displayStock = dialogLayout.findViewById<TextView>(R.id.product_stock)
        val displayUnit = dialogLayout.findViewById<TextView>(R.id.product_unit)
        val displayPrice = dialogLayout.findViewById<TextView>(R.id.product_price)

        // init display
        val uri = this.helper.getUriFromGallery(this.contentResolver, p.img)
        if (uri != null) {
            Glide.with(this.activity)
                .load(uri)
                .into(displayImage)
        }
        displayName.text = p.name
        displayStock.text = p.stock.toString()
        displayUnit.text = p.unit
        displayPrice.text = this.helper.intToRupiah(p.price)

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
                if (cr.deleteProductInCart(transactionID, p.id) != 1) {
                    return@setOnClickListener helper.generateTOA(
                        ctx,
                        "failed delete product in cart",
                        true
                    )
                }
//                showProductInCart(p, transactionID, ctx)
                binding.totpricecart.text = helper.intToRupiah(cr.getCurrentTotalPriceInCart(transactionID))
                val totItemValue = "(${cr.countProductInCart(transactionID)} item)"
                binding.totitem.text = totItemValue
                alertDialog.dismiss()
            }
            pdDiscount.setText(prod.product.discountPercent.toString())
            pdQty.setText(prod.product.quantity.toString())
        } else {
            pdCancel.setOnClickListener { alertDialog.dismiss() }
        }

        val pdQtyMin = dialogLayout.findViewById<Button>(R.id.anp_qty_min)
        val pdQtyPlus = dialogLayout.findViewById<Button>(R.id.anp_qty_plus)
        this.helper.setPriceAfterDiscount(
            priceBeforeDiscount,
            priceAfterDiscount,
            prod.product.discountPercent,
            prod.product.price,
            prod.product.quantity,
            true,
            this.activity
        )
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
                        prod.product.discountPercent,
                        prod.product.price,
                        1,
                        true,
                        ctx
                    )
                    prod.product.quantity = 1
                    pdQty.setText("1")
                    return
                }
                if (value > p.stock) {
                    isFinish = true
                    helper.setPriceAfterDiscount(
                        priceBeforeDiscount,
                        priceAfterDiscount,
                        prod.product.discountPercent,
                        prod.product.price,
                        p.stock,
                        true,
                        ctx
                    )
                    prod.product.quantity = p.stock
                    pdQty.setText(p.stock.toString())
                    return
                }
                prod.product.quantity = value
                helper.setPriceAfterDiscount(
                    priceBeforeDiscount,
                    priceAfterDiscount,
                    prod.product.discountPercent,
                    prod.product.price,
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
                prod.product.quantity = 1
                helper.setPriceAfterDiscount(
                    priceBeforeDiscount,
                    priceAfterDiscount,
                    prod.product.discountPercent,
                    prod.product.price,
                    1,
                    true,
                    ctx
                )
                return@setOnClickListener
            }
            pdQty.setText((value - 1).toString())
            pdQty.setSelection(pdQty.text.length)
            prod.product.quantity = value - 1
            helper.setPriceAfterDiscount(
                priceBeforeDiscount,
                priceAfterDiscount,
                prod.product.discountPercent,
                prod.product.price,
                value - 1,
                true,
                ctx
            )
        }
        pdQtyPlus.setOnClickListener {
            val value = pdQty.text.toString().toIntOrNull() ?: return@setOnClickListener
            if (value < p.stock) {
                val textQtyValue = (value + 1).toString()
                pdQty.setText(textQtyValue)
                pdQty.setSelection(pdQty.text.length)
                prod.product.quantity = value + 1
                helper.setPriceAfterDiscount(
                    priceBeforeDiscount,
                    priceAfterDiscount,
                    prod.product.discountPercent,
                    prod.product.price,
                    value + 1,
                    true,
                    ctx
                )
            }
        }
        pdDiscount.addTextChangedListener(object : TextWatcher {
            var isFinish = false
            var cp = 0
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                cp = pdDiscount.selectionStart
            }
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(s: Editable?) {
                prod.product.discountPercent = helper.strToDouble(s.toString())
                if (isFinish) {
                    isFinish = false
                    pdDiscount.setSelection(cp)
                    helper.setPriceAfterDiscount(
                        priceBeforeDiscount,
                        priceAfterDiscount,
                        prod.product.discountPercent,
                        prod.product.price,
                        prod.product.quantity,
                        true,
                        ctx
                    )
                    return
                }
                isFinish = true
                pdDiscount.setText(s.toString())
            }
        })
        pdOk.setOnClickListener {
            // check discount
            val totalPrice = prod.product.price * prod.product.quantity
            val discountPrice = helper.getDiscountNominal(
                totalPrice,
                prod.product.discountPercent
            )
            prod.product.totalAfterDiscount = totalPrice - discountPrice

            if (isUpdate) {
                val isUpdateSuccess = this.cr.updateProductInCart(prod)
                if (isUpdateSuccess > 1) {
                    helper.generateTOA(
                        ctx,
                        "failed update product in cart",
                        true
                    )
                }
            } else {
//                this.cr.saveProductInCart(prod)
            }
//            this.showProductInCart(p, transactionID, ctx)
            binding.totpricecart.text = helper.intToRupiah(cr.getCurrentTotalPriceInCart(transactionID))
            val totItemValue = "(${cr.countProductInCart(transactionID)} item)"
            binding.totitem.text = totItemValue
            alertDialog.dismiss()
        }
    }

//    @SuppressLint("NotifyDataSetChanged")
//    fun showProductInCart(
//        p: ProductModel,
//        transactionID: String,
//        ctx: Context
//    ) {
//        val productsInCart = this.cr.getProductsInCart(transactionID)
//        val pdCard = ProductInCart(helper, productsInCart, activity)
//        pdCard.setOnItemClickListener(object : ProductInCart.OnItemClickListener{
//            override fun onItemClick(position: Int) {
//                try {
//                    val pc = productsInCart[position]
//                    showDialogInputProductQty(p, pc, transactionID, true, ctx)
//                } catch (e: Exception) {
//                    helper.generateTOA(ctx, "product not found", true)
//                }
//            }
//        })
//        binding.cartProduct.adapter = pdCard
//        pdCard.notifyDataSetChanged()
//    }
}