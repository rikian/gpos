package com.gulali.gpos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gulali.gpos.R
import com.gulali.gpos.database.ProductTransaction
import com.gulali.gpos.helper.Helper

class ProductCart(
    private val helper: Helper,
    listProducts: MutableList<ProductTransaction>,
    private val context: Context,
) : RecyclerView.Adapter<ProductCart.ProductViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null
    private var products: MutableList<ProductTransaction> = listProducts

    init {
        this.products = listProducts
    }

    inner class ProductViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var pId: TextView = view.findViewById(R.id.id_product)
        var pName: TextView = view.findViewById(R.id.pname_cart)
        var pPrice: TextView = view.findViewById(R.id.pprice_cart)
        var pQty: TextView = view.findViewById(R.id.pstock_cart)
        var pPriceBeforeDiscount: TextView = view.findViewById(R.id.pp_tot_cart)
        var pPriceAfterDiscount: TextView = view.findViewById(R.id.pp_tot_cart_dis)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductCart.ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_cart, parent, false)
        val viewHolder = ProductViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition)
        }

        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(h: ProductViewHolder, position: Int) {
        h.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }

        val p = products[position]
        h.pName.text = p.pName
        h.pPrice.text = helper.intToRupiah(p.pPrice)
        h.pQty.text = p.pQty.toString()

        helper.setPriceAfterDiscount(h.pPriceBeforeDiscount, h.pPriceAfterDiscount, p.pDiscount, p.pPrice, p.pQty, false, context)
    }

    override fun getItemCount(): Int {
        return products.size
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}