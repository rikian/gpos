package com.gulali.gpos.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.gulali.gpos.database.HistoryStockEntity
import com.gulali.gpos.database.ProductEntity
import com.gulali.gpos.database.ProductModel

@Dao
interface Products {
    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.category AS category, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.created AS created, a.updated AS updated, b.name AS unit, c.name AS category FROM products AS a INNER JOIN units AS b ON a.unit = b.id INNER JOIN categories AS c ON a.category = c.id"
    )
    fun getProducts(): List<ProductModel>

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.created AS created, a.updated AS updated, b.name AS unit, c.name AS category FROM products AS a INNER JOIN units AS b ON a.unit = b.id INNER JOIN categories AS c ON a.category = c.id WHERE a.name LIKE '%' || :query || '%'"
    )
    fun getProductByName(query: String): List<ProductModel>

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.created AS created, a.updated AS updated, b.name AS unit, c.name AS category FROM products AS a INNER JOIN units AS b ON a.unit = b.id INNER JOIN categories AS c ON a.category = c.id WHERE a.id = :query"
    )
    fun getProductByID(query: Int): ProductModel

    @Query(
        "SELECT a.id AS id, a.image AS img, a.barcode as barcode, a.name AS name, a.stock AS stock, a.price AS price, a.purchase AS purchase, a.created AS created, a.updated AS updated, b.name AS unit, c.name AS category FROM products AS a INNER JOIN units AS b ON a.unit = b.id INNER JOIN categories AS c ON a.category = c.id WHERE a.barcode = :query"
    )
    fun getProductByBarcode(query: String): ProductModel?

    @Insert
    fun insertProduct(data: ProductEntity): Long

    @Update
    fun updateProduct(data: ProductEntity)

    @Query("UPDATE products SET stock=:stock WHERE id=:id")
    fun updateStockProduct(id: Int, stock: Int)

    @Insert
    fun saveHistoryStock(data: HistoryStockEntity)
}