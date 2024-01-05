package com.gulali.gpos.database

import java.io.Serializable

data class ProductModel(
    val id: Int,
    val img: String,
    var barcode: String,
    val name: String,
    var category: String,
    val stock: Int,
    val unit: String,
    var price: Int,
    var purchase: Int,
    val created: Long,
    val updated: Long
) : Serializable

data class DateTime(
    var date: String = "",
    var time: String = "",
)

data class DataTimeLong(
    var created: Long,
    var updated: Long,
)

data class DataProduct(
    var productID: Int,
    var name: String,
    var quantity: Int = 1,
    var unit: String,
    var price: Int,
    var discountPercent: Double = 0.0,
    var discountNominal: Int,
    var totalBeforeDiscount: Int = 0,
    var totalAfterDiscount: Int = 0,
)

data class DataTransaction(
    var totalItem: Int,
    var subTotalProduct: Int,
    var discountNominal: Int,
    var discountPercent: Double,
    var taxNominal: Int,
    var taxPercent: Double,
    var adm: Int,
    var cash: Int,
    var grandTotal: Int = 0,
)

data class CountAndSum(
    val count: Int,
    val sum: Int
)

data class TransactionsModel(
    var count: CountAndSum,
    var list: List<TransactionEntity>
)

data class ParamTransactionFilter(
    var totalStart: IntObj,
    var totalEnd: IntObj,
    var dateStart: Long,
    var dateEnd: Long,
    var pIndex: IntObj,
    var pSize: Int
)

data class DataFilter(
    var t: TransactionsModel,
    var tIndex: Int,
    var tPage: Int
)

data class IntObj(
    var value: Int = 0
)

data class BoolObj(
    var value: Boolean = false
)

data class StringObj(
    var value: String = ""
)