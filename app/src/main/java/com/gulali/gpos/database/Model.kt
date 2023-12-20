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