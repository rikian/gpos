package com.gulali.gpos.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gulali.gpos.database.repositories.Products
import com.gulali.gpos.database.repositories.Transactions

@Database(
    entities = [
        OwnerEntity::class,
        ProductEntity::class,
        UnitEntity::class,
        TransactionEntity::class,
        ProductTransaction::class,
        CategoryEntity::class,
        HistoryStockEntity::class,
        TransactionEntityHistory::class,
        TransactionEntityProductHistory::class,
        CartEntity::class,
        CartPaymentEntity::class],
    version = 10.1.toInt()
)
abstract class AdapterDb: RoomDatabase() {
    abstract fun products(): Products
    abstract fun transaction(): Transactions
    abstract fun repository(): Repository
    abstract fun ownerDao(): OwnerDao
    abstract fun cartDao(): CartDao

    companion object {
        @Volatile
        private var INSTANCE: AdapterDb? = null

        fun getGposDatabase(ctx: Context): AdapterDb {
            val tmplInstant = INSTANCE
            if (tmplInstant != null) {
                return tmplInstant
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(ctx, AdapterDb::class.java, "gpos")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}