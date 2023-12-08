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

    @Query("DELETE FROM units")
    fun truncateUnitTable()

    // category
    @Query("SELECT * FROM categories")
    fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :catId")
    fun getCategory(catId: Int): CategoryEntity?

    @Insert
    fun insertCategory(data: CategoryEntity): Long

    @Query("DELETE FROM categories")
    fun truncateCategoriesTable()

    // Product
    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.category AS category, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.createdAt AS created, a.updatedAt AS updated, b.name AS unit, c.name AS category FROM products AS a INNER JOIN units AS b ON a.unit = b.id INNER JOIN categories AS c ON a.category = c.id"
    )
    fun getProducts(): List<ProductModel>

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.createdAt AS created, a.updatedAt AS updated, b.name AS unit, c.name AS category FROM products AS a INNER JOIN units AS b ON a.unit = b.id INNER JOIN categories AS c ON a.category = c.id WHERE a.name LIKE '%' || :query || '%'"
    )
    fun getProductByName(query: String): List<ProductModel>

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.createdAt AS created, a.updatedAt AS updated, b.name AS unit, c.name AS category FROM products AS a INNER JOIN units AS b ON a.unit = b.id INNER JOIN categories AS c ON a.category = c.id WHERE a.id = :query"
    )
    fun getProductByID(query: Int): ProductModel

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.createdAt AS created, a.updatedAt AS updated, b.name AS unit, c.name AS category FROM products AS a INNER JOIN units AS b ON a.unit = b.id INNER JOIN categories AS c ON a.category = c.id WHERE a.barcode = :query"
    )
    fun getProductByBarcode(query: String): ProductModel?

    @Insert
    fun insertProduct(data: ProductEntity)

    @Update
    fun updateProduct(data: ProductEntity)

    @Query("UPDATE products SET stock=:stock WHERE id=:id")
    fun updateStockProduct(id: Int, stock: Int)

    @Query("DELETE FROM products")
    fun truncateProductsTable()

    // transaction
    @Insert
    fun saveTransaction(data: TransactionEntity)

    @Query("SELECT * FROM `transaction` ORDER BY createdAt DESC")
    fun getTransaction(): List<TransactionEntity>

    @Query("SELECT * FROM `transaction` WHERE id=:idTransaction LIMIT 1")
    fun getTransactionById(idTransaction: String): TransactionEntity

    @Query("DELETE FROM `transaction`")
    fun truncateTransactionTable()

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

    @Query("DELETE FROM product_transaction")
    fun truncateProductTransactionTable()

    // owner
    @Insert
    fun createOwner(data: OwnerEntity)

    @Update
    fun updateOwner(data: OwnerEntity)

    @Query("SELECT * FROM owner")
    fun getOwner(): List<OwnerEntity>

    @Query("Update owner SET bluetoothPaired=:name WHERE id='001'")
    fun updateBluetooth(name: String)

    @Query("SELECT bluetoothPaired FROM owner WHERE id='001'")
    fun getOwnerBluetooth(): String

    @Query("DELETE FROM owner")
    fun truncateOwnerTable()
}