package com.gulali.gpos.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface Repository {
    // unit
    @Query("SELECT * FROM units")
    fun getUnits(): List<UnitEntity>

    @Query("SELECT * FROM units WHERE id = :unitId")
    fun getUnit(unitId: Int): UnitEntity?

    @Insert
    fun insertUnit(data: UnitEntity): Long

    // Product
    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.createdAt AS created, a.updatedAt AS updated, b.name AS unit FROM products AS a INNER JOIN units AS b ON a.unit = b.id"
    )
    fun getProducts(): List<ProductModel>

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.createdAt AS created, a.updatedAt AS updated, b.name AS unit FROM products AS a INNER JOIN units AS b ON a.unit = b.id WHERE a.name LIKE '%' || :query || '%'"
    )
    fun getProductByName(query: String): List<ProductModel>

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.createdAt AS created, a.updatedAt AS updated, b.name AS unit FROM products AS a INNER JOIN units AS b ON a.unit = b.id WHERE a.id = :query"
    )
    fun getProductByID(query: Int): ProductModel

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.createdAt AS created, a.updatedAt AS updated, b.name AS unit FROM products AS a INNER JOIN units AS b ON a.unit = b.id WHERE a.barcode = :query"
    )
    fun getProductByBarcode(query: String): ProductModel?

    @Insert
    fun insertProduct(data: ProductEntity)

    @Query("UPDATE products SET stock=:stock WHERE id=:id")
    fun updateProduct(id: Int, stock: Int)

    // transaction
    @Insert
    fun saveTransaction(data: TransactionEntity)

    @Query("SELECT * FROM `transaction`")
    fun getTransaction(): List<TransactionEntity>

//    @Query()
//    fun getTransactionDetail(idTransaction: String): TransactionDetail

    // product transaction
    @Insert
    fun saveProductTransaction(data: List<ProductTransaction>)

    @Query("SELECT * FROM product_transaction")
    fun getProductTransaction(): List<ProductTransaction>

    @Query("SELECT * FROM product_transaction WHERE transactionID = :query")
    fun getProductTransactionByTransactionID(query: String): List<ProductTransaction>

    @Query("SELECT * FROM product_transaction WHERE pID = :query")
    fun getProductTransactionByProductID(query: Int): List<ProductTransaction>

    // owner
    @Insert
    fun createOwner(data: OwnerEntity)

    @Query("Update owner SET bluetoothPaired=:name WHERE id='001'")
    fun updateBluetooth(name: String)

    @Query("SELECT bluetoothPaired FROM owner WHERE id='001'")
    fun getOwnerBluetooth(): String
}