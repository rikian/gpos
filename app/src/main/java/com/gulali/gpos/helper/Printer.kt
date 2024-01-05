package com.gulali.gpos.helper

import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.database.ProductTransaction
import com.gulali.gpos.database.TransactionEntity

class Printer(private val h: Helper) {
    private val reset = byteArrayOf(0x1b, 0x40)
//    private val fontX1 = byteArrayOf(0x1b,0x21, 0x00)
    private val fontX2 = byteArrayOf(0x1b,0x21, 0x10)
//    private val bold = byteArrayOf(0x1b,0x21, 0x30)
    private val cut = byteArrayOf(0x1D, 0x56, 66, 0x00)
    private val centerText = byteArrayOf(0x1b, 0x61, 0x01)
//    private val rightText = byteArrayOf(0x1b, 0x61, 0x02)

    fun generateStruckDisplay(t: TransactionEntity, o: OwnerEntity, p: List<ProductTransaction>): List<ByteArray> {
        val dateTime = this.h.formatSpecificDate(this.h.unixTimestampToDate(t.date.created))
        val result = mutableListOf<ByteArray>()
        result.add(reset)
        result.add(fontX2)
        result.add(centerText)
        result.add("SHOP\n".toByteArray())
        result.add("${o.owner}\n".toByteArray())
        result.add(reset)
        result.add("${dateTime.date}\n".toByteArray())
        result.add("id ${t.id}\n".toByteArray())
        result.add("time ${dateTime.time}\n".toByteArray())
        result.add("--------------------------------\n".toByteArray())
        result.add(this.generateProductListForPrint(p).toByteArray())
        result.add("--------------------------------\n".toByteArray())
        result.add("${this.generateSubTotalProduct(t)}\n".toByteArray())
        result.add("${this.generateDiscountPayment(t)}\n".toByteArray())
        result.add("${this.generateTaxPayment(t)}\n".toByteArray())
        result.add("${this.generateAdmPayment(t)}\n".toByteArray())
        result.add("--------------------------------\n".toByteArray())
        result.add(fontX2)
        result.add(this.generateGrandTotal(t).toByteArray())
        result.add(reset)
        result.add("${this.generateCashPayment(t)}\n".toByteArray())
        result.add("${this.generateCashReturnedPayment(t)}\n".toByteArray())
        result.add("================================\n".toByteArray())
        result.add(centerText)
        result.add("This receipt is valid\n".toByteArray())
        result.add("proof of payment\n".toByteArray())
        result.add("from the shop ${o.owner}.\n".toByteArray())
        result.add("For further information, please call ${o.phone}, or visit the shop address at ${o.address}\n".toByteArray())
        result.add("\nthank you for visiting\n\n".toByteArray())
        result.add("---\n".toByteArray())
        result.add(reset)
        result.add(cut)

        return result
    }

    fun generateStruckPayment(t: TransactionEntity, o: OwnerEntity, p: List<ProductTransaction>): List<ByteArray> {
        val dateTime = this.h.formatSpecificDate(this.h.unixTimestampToDate(t.date.created))
        val result = mutableListOf<ByteArray>()
        result.add(reset)
        result.add(fontX2)
        result.add(centerText)
        result.add("SHOP\n".toByteArray())
        result.add("${o.owner}\n".toByteArray())
        result.add(reset)
        result.add("${dateTime.date}\n".toByteArray())
        result.add("id ${t.id}\n".toByteArray())
        result.add("time ${dateTime.time}\n".toByteArray())
        result.add("--------------------------------\n".toByteArray())
        result.add(this.generateProductListForPrint(p).toByteArray())
        result.add("--------------------------------\n".toByteArray())
        result.add("${this.generateSubTotalProduct(t)}\n".toByteArray())
        result.add("${this.generateDiscountPayment(t)}\n".toByteArray())
        result.add("${this.generateTaxPayment(t)}\n".toByteArray())
        result.add("${this.generateAdmPayment(t)}\n".toByteArray())
        result.add("--------------------------------\n".toByteArray())
        result.add(fontX2)
        result.add(this.generateGrandTotal(t).toByteArray())
        result.add(reset)
        result.add("${this.generateCashPayment(t)}\n".toByteArray())
        result.add("${this.generateCashReturnedPayment(t)}\n".toByteArray())
        result.add("================================\n".toByteArray())
        result.add(centerText)
        result.add("This receipt is valid\n".toByteArray())
        result.add("proof of payment\n".toByteArray())
        result.add("from the shop ${o.owner}.\n".toByteArray())
        result.add("For further information, please call ${o.phone}, or visit the shop address at ${o.address}\n".toByteArray())
        result.add("\nthank you for visiting\n\n".toByteArray())
        result.add("---\n".toByteArray())
        result.add(reset)
        result.add(cut)

        return result
    }
    private fun generateProductListForPrint(pt: List<ProductTransaction>): String {
        var result = ""
        for (p in pt) {
            result += this.generateProductName(p.product.name)
            if (p.product.discountPercent > 0.0) {
                result += this.generateProductQty(
                    this.h.intToRupiah(p.product.price),
                    p.product.quantity.toString(),
                    ""
                )
                val totalPrice = p.product.price * p.product.quantity
                val discountPercentage = p.product.discountPercent / 100
                val totalDiscount = discountPercentage * totalPrice
                val totalPriceAfterDiscount = totalPrice - totalDiscount
                // passing to result
                result += String.format("%18s", "disc (${p.product.discountPercent}%)")
                result += String.format("%14s", h.intToRupiah(totalPriceAfterDiscount.toInt()))
            } else {
                result += this.generateProductQty(
                    this.h.intToRupiah(p.product.price),
                    p.product.quantity.toString(),
                    this.h.intToRupiah (
                        p.product.price * p.product.quantity
                    )
                )
            }
            result += "\n"
        }
        return result
    }
    private fun generateProductName(product: String): String {
        val result = StringBuilder()
        val lineLength = 28

        for (i in product.indices step lineLength) {
            val endIndex = i + lineLength
            val line = if (endIndex <= product.length) {
                product.substring(i, endIndex)
            } else {
                product.substring(i)
            }
            result.append("$line\n")
        }

        return result.toString()
    }
    private fun generateProductQty(price: String, qty: String, total: String): String {
        val priceFormatted = String.format("%12s", price)
        val qtyFormatted = String.format("%4s", qty)
        val totalFormatted = String.format("%10s", total)

        return "$priceFormatted  x $qtyFormatted  $totalFormatted"
    }
    private fun generateSubTotalProduct(t: TransactionEntity): String {
        var textFormatted = "Sub total products"
        textFormatted += "\n"
        val t1 = String.format("%-15s", "(${t.dataTransaction.totalItem}) item")
        val t2 = String.format("%17s", this.h.intToRupiah(t.dataTransaction.subTotalProduct))
        textFormatted += t1
        textFormatted += t2
        return textFormatted
    }
    private fun generateDiscountPayment(t: TransactionEntity): String {
        var textFormatted = ""
        textFormatted += String.format("%-18s", "Discount payment")
        if (t.dataTransaction.discountPercent > 0.0) {
            textFormatted += "\n"
            textFormatted += String.format("%-10s", "(${t.dataTransaction.discountPercent}%)")
            textFormatted += String.format("%22s", "- ${this.h.intToRupiah(t.dataTransaction.discountNominal)}")
        } else {
            textFormatted += if (t.dataTransaction.discountNominal == 0) {
                String.format("%14s", "0")
            } else {
                String.format("%14s", "- ${this.h.intToRupiah(t.dataTransaction.discountNominal)}")
            }
        }
        return textFormatted
    }
    private fun generateTaxPayment(t: TransactionEntity): String {
        var textFormatted = ""
        textFormatted += String.format("%-18s", "Tax")
        if (t.dataTransaction.taxPercent > 0.0) {
            textFormatted += "\n"
            textFormatted += String.format("%-10s", "(${t.dataTransaction.taxPercent}%)")
            textFormatted += String.format("%22s", this.h.intToRupiah(t.dataTransaction.taxNominal))
        } else {
            textFormatted += if (t.dataTransaction.taxNominal == 0) {
                String.format("%14s", "0")
            } else {
                String.format("%14s", this.h.intToRupiah(t.dataTransaction.taxNominal))
            }
        }
        return textFormatted
    }
    private fun generateAdmPayment(t: TransactionEntity): String {
        var textFormatted = ""
        textFormatted += String.format("%-18s", "Adm")
        textFormatted += String.format("%14s", this.h.intToRupiah(t.dataTransaction.adm))
        return textFormatted
    }
    private fun generateCashPayment(t: TransactionEntity): String {
        var textFormatted = ""
        textFormatted += String.format("%-20s", "Cash")
        textFormatted += String.format("%12s", this.h.intToRupiah(t.dataTransaction.cash))
        return textFormatted
    }
    private fun generateCashReturnedPayment(t: TransactionEntity): String {
        var totalPayment = 0
        totalPayment += t.dataTransaction.subTotalProduct
        totalPayment += t.dataTransaction.taxNominal
        totalPayment += t.dataTransaction.adm
        totalPayment -= t.dataTransaction.discountNominal

        var textFormatted = ""
        textFormatted += String.format("%-20s", "Returned")
        textFormatted += String.format("%12s", this.h.intToRupiah(t.dataTransaction.cash - totalPayment))
        return textFormatted
    }
    private fun generateGrandTotal(t: TransactionEntity): String {
        val totalPrice = "Rp${this.h.intToRupiah(this.h.getTotalPayment(t))}"
        return "Total${String.format("%27s", totalPrice)}\n\n"
    }
}