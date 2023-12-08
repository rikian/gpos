package com.gulali.gpos.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.gulali.gpos.R
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.helper.Helper

class TransactionDisplay(
    private val helper: Helper,
    transactionItems: List<TransactionEntity>,
    private val context: Context,
) : RecyclerView.Adapter<TransactionDisplay.PDViewHolder>() {
    private var itemClickListener: OnItemClickListener? = null
    private var transaction: List<TransactionEntity> = transactionItems

    init {
        this.transaction = transactionItems
    }

    inner class PDViewHolder(view: View): RecyclerView.ViewHolder(view) {
        var tNumber: TextView = view.findViewById(R.id.transaction_number)
        var tID: TextView = view.findViewById(R.id.id_payment)
        var tItem: TextView = view.findViewById(R.id.transaction_item)
        var tTotal: TextView = view.findViewById(R.id.total_payment)
        var tCreate: TextView = view.findViewById(R.id.payment_created)
        var isNew: TextView = view.findViewById(R.id.is_new)
        val payTime: TextView = view.findViewById(R.id.pay_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionDisplay.PDViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.transaction_item, parent, false)
        val viewHolder = PDViewHolder(view)
        viewHolder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(viewHolder.absoluteAdapterPosition)
        }

        return PDViewHolder(view)
    }

    override fun onBindViewHolder(holder: PDViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemClickListener?.onItemClick(position)
        }

        if (position == 0) {
            holder.isNew.visibility = View.VISIBLE
        }

        val tr = transaction[position]
        val num = position + 1
        val item = "(${tr.item} item)"
        val dateTime = helper.formatSpecificDate(helper.unixTimestampToDate(tr.createdAt))

        holder.tNumber.text = num.toString()
        holder.tID.text = tr.id
        holder.tItem.text = item
        holder.tTotal.text = helper.intToRupiah(helper.getTotalPayment(tr))
        holder.tCreate.text = dateTime.date
        holder.payTime.text = dateTime.time

//        // Check if the current item is the last one
//        val isLastItem = position == itemCount - 1
//
//        // Adjust bottom margin for the last item
//        val params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
//        if (isLastItem) {
//            params.bottomMargin = 50.dpToPx(context) // Convert dp to pixels
//        } else {
//            params.bottomMargin = 0
//        }
    }

    private fun Int.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }

    override fun getItemCount(): Int {
        return transaction.size
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        itemClickListener = listener
    }
}