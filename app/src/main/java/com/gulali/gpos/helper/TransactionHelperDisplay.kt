package com.gulali.gpos.helper

import android.view.View
import com.gulali.gpos.databinding.TransactionDisplayBinding

class TransactionHelperDisplay {
    fun setPages(currentPage: Int, totalPage: Int, tdBinding: TransactionDisplayBinding) {
        val currentPageStr = if (currentPage <= 0) "1" else "${currentPage + 1}"
        val totalPageStr = totalPage.toString()
        tdBinding.t65Up1.text = currentPageStr
        tdBinding.t65Up2.text = totalPageStr
        tdBinding.t65Down1.text = currentPageStr
        tdBinding.t65Down2.text = totalPageStr

        // set default
        tdBinding.textViewNoMoreItems.visibility = View.GONE
        tdBinding.cpDownContainer.visibility = View.GONE

        // cek if total page equal to 1
        if (totalPage == 1) {
            tdBinding.textViewNoMoreItems.visibility = View.VISIBLE
            return
        }

        // check if total page greater than 1
        if (totalPage > 1) {
            tdBinding.cpDownContainer.visibility = View.VISIBLE
        }

        // check if last of total page
        if (currentPage + 1 >= totalPage) {
            tdBinding.textViewNoMoreItems.visibility = View.VISIBLE
        } else {
            tdBinding.textViewNoMoreItems.visibility = View.GONE
        }
    }

    fun setHeaderTransaction(
        tdBinding: TransactionDisplayBinding,
        totalTransaction: Int,
        totalAmount: Int,
        helper: Helper
    ) {
        val amountStr = "Rp ${helper.intToRupiah(totalAmount)}"
        tdBinding.disTotTr.text = totalTransaction.toString()
        tdBinding.disTotAmaount.text = amountStr
    }
}