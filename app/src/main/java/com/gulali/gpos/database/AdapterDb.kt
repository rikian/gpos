package com.gulali.gpos.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        OwnerEntity::class,
        ProductEntity::class,
        UnitEntity::class,
        TransactionEntity::class,
        ProductTransaction::class,
        CategoryEntity::class,
        HistoryStockEntity::class,
        CartEntity::class,
        CartPaymentEntity::class],
    version = 8.1.toInt()
)
abstract class AdapterDb: RoomDatabase() {
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