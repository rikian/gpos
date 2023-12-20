package com.gulali.gpos.database

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "units", indices = [Index(value = ["name"], unique = true)])
data class UnitEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,
    @Embedded
    var date: DataTimeLong
)

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class CategoryEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var name: String,
    @Embedded
    var date: DataTimeLong
)

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(onDelete=CASCADE, entity=UnitEntity::class, parentColumns=["id"], childColumns=["unit"]),
        ForeignKey(onDelete=CASCADE, entity=CategoryEntity::class, parentColumns=["id"], childColumns=["category"]),
    ]
)
data class ProductEntity (
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var image: String,
    var name: String,
    var category: Int,
    var barcode: String,
    var stock: Int,
    var unit: Int,
    var purchase: Int,
    var price: Int,
    @Embedded
    var date: DataTimeLong
)

@Entity(
    tableName = "history_stock",
    foreignKeys = [
        ForeignKey(onDelete=CASCADE, entity=ProductEntity::class, parentColumns=["id"], childColumns=["pID"]),
    ]
)
data class HistoryStockEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var pID: Int,
    var inStock: Int,
    var outStock: Int,
    var currentStock: Int,
    var purchase: Int,
    val transactionID: String,
    @Embedded
    var date: DataTimeLong
)

@Entity(
    tableName = "transaction",
)
data class TransactionEntity (
    @PrimaryKey(autoGenerate = false)
    var id: String,
    @Embedded
    var dataTransaction: DataTransaction,
    @Embedded
    var date: DataTimeLong
)

@Entity(
    tableName = "product_transaction",
    foreignKeys = [
        ForeignKey(onDelete=CASCADE, entity = TransactionEntity::class, parentColumns = ["id"], childColumns = ["transactionID"]),
        ForeignKey(entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productID"]),
    ]
)
data class ProductTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val transactionID: String,
    @Embedded
    var product: DataProduct,
    @Embedded
    var date: DataTimeLong
)

// cartPayment
@Entity(
    tableName = "cart_payment"
)
data class CartPaymentEntity(
    @PrimaryKey(autoGenerate = false)
    var id: String,
    @Embedded
    var dataTransaction: DataTransaction
)

// cart
@Entity(
    tableName = "cart",
    foreignKeys = [
        ForeignKey(onDelete=CASCADE, entity = CartPaymentEntity::class, parentColumns = ["id"], childColumns = ["transactionID"]),
        ForeignKey(onDelete=CASCADE, entity = ProductEntity::class, parentColumns = ["id"], childColumns = ["productID"]),
    ]
)
data class CartEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var transactionID: String,
    @Embedded
    var product: DataProduct
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
    var taxPercent: Double,
    var taxNominal: Int,
    var adm: Int,
    var bluetoothPaired: String,
    @Embedded
    var date: DataTimeLong
)