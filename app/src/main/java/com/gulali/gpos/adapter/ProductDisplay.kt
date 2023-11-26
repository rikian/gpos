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

class ProductDisplay(
    listProducts: List<ProductModel>,
    private val helper: Helper,
    private val context: Context,
    private val cr: ContentResolver
) : RecyclerView.Adapter<ProductDisplay.ProductViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null
    private var products: List<ProductModel> = listProducts

    init {
        this.products = listProducts
    }

    inner class ProductViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var pId: TextView = view.findViewById(R.id.id_product)
        var pName: TextView = view.findViewById(R.id.pname_1)
        var pImg: ImageView = view.findViewById(R.id.pimg_1)
        var pStock: TextView = view.findViewById(R.id.pstock_1)
        var stDesc: TextView = view.findViewById(R.id.st_desc)
        var pPrice: TextView = view.findViewById(R.id.pprice_1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDisplay.ProductViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.product_item, parent, false)
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
        holder.stDesc.text = "Stock"
        holder.stDesc.textColors.defaultColor
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