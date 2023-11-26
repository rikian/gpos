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
        ProductTransaction::class],
    version = 1.1.toInt()
)
abstract class AdapterDb: RoomDatabase() {
    abstract fun repository(): Repository
    companion object {
        @Volatile
        private var INSTANCE: AdapterDb? = null

        fun getGposDatabase(context: Context): AdapterDb {
            val tmplInstant = INSTANCE
            if (tmplInstant != null) {
                return tmplInstant
            }

            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AdapterDb::class.java,
                    "gpos"
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}