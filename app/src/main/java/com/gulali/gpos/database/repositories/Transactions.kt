package com.gulali.gpos.database.repositories

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.gulali.gpos.database.CountAndSum
import com.gulali.gpos.database.OwnerEntity
import com.gulali.gpos.database.ParamTransactionFilter
import com.gulali.gpos.database.ProductTransaction
import com.gulali.gpos.database.TransactionEntity
import com.gulali.gpos.database.TransactionEntityHistory
import com.gulali.gpos.database.TransactionsModel

@Dao
interface Transactions {
    @Insert
    fun saveData(data: TransactionEntity)

    @Update
    fun updateData(data: TransactionEntity): Int

    @Query("SELECT COUNT(grandTotal) AS count, SUM(grandTotal) AS sum FROM `transaction`")
    fun getDataCountAndSum(): CountAndSum

    @Query("SELECT * FROM `transaction` ORDER BY created DESC LIMIT :limit OFFSET :index * :limit")
    fun getData(index: Int, limit: Int): List<TransactionEntity>

    @Transaction
    fun getDataWithCount(index: Int, limit: Int): TransactionsModel {
        val count = getDataCountAndSum()
        val transactions = getData(index, limit)
        return TransactionsModel(count, transactions)
    }

    @Query("SELECT COUNT(*) as count, SUM(grandTotal) as sum FROM `transaction` WHERE grandTotal >= :totalStart AND grandTotal <= :totalEnd AND created >= :dateStart AND created <= :dateEnd")
    fun getDataFilterCountAndSum(
        totalStart: Int,
        totalEnd: Int,
        dateStart: Long,
        dateEnd: Long
    ): CountAndSum

    @Query("SELECT * FROM `transaction` WHERE grandTotal >= :totalStart AND grandTotal <= :totalEnd AND created >= :dateStart AND created <= :dateEnd ORDER BY grandTotal ASC LIMIT :pSize OFFSET :pIndex * :pSize")
    fun getDataFilter(
        totalStart: Int,
        totalEnd: Int,
        dateStart: Long,
        dateEnd: Long,
        pIndex: Int,
        pSize: Int
    ): List<TransactionEntity>

    @Transaction
    fun getDataFilterWithCount(p: ParamTransactionFilter): TransactionsModel {
        val count = getDataFilterCountAndSum(p.totalStart.value, p.totalEnd.value, p.dateStart, p.dateEnd)
        val transactions = getDataFilter(
            p.totalStart.value,
            p.totalEnd.value,
            p.dateStart,
            p.dateEnd,
            p.pIndex.value,
            p.pSize,
        )
        return TransactionsModel(count, transactions)
    }

    @Insert()
    fun saveHistoryUpdate(data: TransactionEntityHistory)

    // search transaction
    @Query("SELECT * FROM `transaction` WHERE id LIKE '%' || :idTransaction || '%' ORDER BY created DESC LIMIT 5")
    fun getDataById(idTransaction: String): List<TransactionEntity>

    // detail
    @Query("SELECT * FROM `transaction` WHERE id=:idTransaction")
    fun getDetailById(idTransaction: String): List<TransactionEntity>

    // product transaction
    @Insert
    fun saveProducts(data: List<ProductTransaction>)

    @Insert
    fun saveProduct(data: ProductTransaction)

    @Query("SELECT * FROM product_transaction WHERE transactionID = :query")
    fun getProducts(query: String): List<ProductTransaction>

    @Query("Delete FROM product_transaction WHERE transactionID=:idTransaction")
    fun deleteProductByID(idTransaction: String): Int
}