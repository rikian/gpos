package com.gulali.gpos.service.transaction

import android.content.ContentResolver
import android.content.Context
import android.view.LayoutInflater
import com.gulali.gpos.database.DataProduct
import com.gulali.gpos.database.ProductModel
import com.gulali.gpos.helper.Helper

data class StatusCart(
    var subTotalProduct: Int,
    var totalItem: Int
)

data class Pr(
    var products: List<ProductModel>,
    var idPayment: String,
    var ly: LayoutInflater,
    var h: Helper,
    var ctx: Context,
    var cr: ContentResolver,
    var cart: List<Product>
)

data class Product(
    var data: DataProduct,
    var img: String,
    var stock: Int
)
