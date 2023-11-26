package com.gulali.gpos.database

import java.io.Serializable

data class ProductModel(
    val id: Int,
    val img: String,
    var barcode: String,
    val name: String,
    val stock: Int,
    val unit: String,
    var price: Int,
    var purchase: Int,
    val created: String,
    val updated: String
) : Serializable

data class ProductForCart(
    val id: Int,
    val name: String,
    var qty: Int,
    val unit: String,
    var price: Int,
)

data class TransactionDetail(
    var id: Int,
    var transactionID: String,
    var item: Int,
    var total: Int,
    var cash: Int,
    var products: List<ProductTransaction>
)