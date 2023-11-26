package com.gulali.gpos.adapter

import android.content.ContentResolver
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.gulali.gpos.R
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.helper.Helper

class ProductSearch(
    listProducts: List<ProductModel>,
    private val helper: Helper,
    private val context: Context,
    private val cr: ContentResolver
) : RecyclerView.Adapter<ProductSearch.ProductViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null
    private var products: List<ProductModel> = listProducts

    init {
        this.products = listProducts
    }

    inner class ProductViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var pId: TextView = view.findViewById(R.id.ps_id)
        var pName: TextView = view.findViewById(R.id.ps_name)
        var pImg: ImageView = view.findViewById(R.id.ps_image)
        var pStock: TextView = view.findViewById(R.id.ps_stock)
        var pPrice: TextView = view.findViewById(R.id.ps_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductSearch.ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_search, parent, false)
        val viewHolder = ProductViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition)
        }

        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }
        val product = products[position]
        holder.pId.text = product.id.toString()
        holder.pName.text = product.name
        holder.pStock.text = product.stock.toString()
        holder.pPrice.text = helper.intToRupiah(product.price)
        val uri = helper.getUriFromGallery(cr, product.img)
        if (uri != null) {
            Glide.with(context)
                .load(uri)
                .into(holder.pImg)
        }
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