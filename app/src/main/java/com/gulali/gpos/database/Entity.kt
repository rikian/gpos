package com.gulali.gpos.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "units", indices = [Index(value = ["name"], unique = true)])
data class UnitEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,
    var createdAt: String,
    var updatedAt: String,
)

@Entity(
    tableName = "products",
    foreignKeys = [ForeignKey(entity = UnitEntity::class, parentColumns = ["id"], childColumns = ["unit"])]
)
data class ProductEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var image: String,
    var name: String,
    var barcode: String,
    var stock: Int,
    var unit: Int,
    var purchase: Int,
    var price: Int,
    var createdAt: String,
    var updatedAt: String,
)

@Entity(
    tableName = "transaction",
)
data class TransactionEntity (
    @PrimaryKey(autoGenerate = false)
    var id: String,
    var item: Int,
    var totalProduct: Int,
    var discountNominal: Int,
    var discountPercent: Double,
    var taxNominal: Int,
    var taxPercent: Double,
    var adm: Int,
    var cash: Int,
    var createdAt: String,
    var updatedAt: String,
)

@Entity(
    tableName = "product_transaction",
    foreignKeys = [
        ForeignKey(entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionID"]),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["pID"]),
    ]
)
data class ProductTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionID: String,
    var pID: Int,
    var pName: String,
    var pQty: Int = 1,
    var pUnit: String,
    var pPrice: Int,
    var pDiscount: Double = 0.0,
    var cAt: String,
    var uAt: String,
)

@Entity(
    tableName = "owner"
)
data class OwnerEntity(
    @PrimaryKey(autoGenerate = false)
    var id: String,
    var shop: String,
    var owner: String,
    var address: String,
    var phone: String,
    var discountPercent: Double,
    var discountNominal: Int,
    var adm: Int,
    var bluetoothPaired: String,
    var createdAt: String,
    var updatedAt: String,
)