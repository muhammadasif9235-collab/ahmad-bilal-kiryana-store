package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        OrderEntity::class,
        LedgerEntryEntity::class,
        StockMovementEntity::class,
        RationDonationEntity::class,
        RationCardItemEntity::class,
        RationCardBillEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ShopLocalDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao

    companion object {
        private const val DB_NAME = "ahmad_bilal_kiryana_local.db"

        @Volatile
        private var INSTANCE: ShopLocalDatabase? = null

        fun getInstance(context: Context): ShopLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShopLocalDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
