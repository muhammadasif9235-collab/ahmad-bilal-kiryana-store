package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        CustomerEntity::class,
        OrderEntity::class,
        LedgerEntryEntity::class,
        StockMovementEntity::class,
        RationDonationEntity::class,
        RationCardItemEntity::class,
        RationCardBillEntity::class,
        DailyCashEntryEntity::class,
        WholesalerEntity::class,
        WholesalerLedgerEntryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class ShopLocalDatabase : RoomDatabase() {
    abstract fun shopDao(): ShopDao

    companion object {
        private const val DB_NAME = "ahmad_bilal_kiryana_local.db"

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE ration_card_bills ADD COLUMN feeDeduction REAL NOT NULL DEFAULT 100.0"
                )
                db.execSQL(
                    "ALTER TABLE ration_card_bills ADD COLUMN netSubsidyLimit REAL NOT NULL DEFAULT 2900.0"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_cash_entries` (
                        `id` TEXT NOT NULL,
                        `entryType` TEXT NOT NULL,
                        `paymentSource` TEXT NOT NULL,
                        `categoryOrTitle` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `referenceId` TEXT NOT NULL,
                        `note` TEXT NOT NULL,
                        `entryDateMs` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wholesalers` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `cityOrMarket` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `totalPurchased` REAL NOT NULL,
                        `totalPaid` REAL NOT NULL,
                        `payableBalance` REAL NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        `updatedAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `wholesaler_ledger_entries` (
                        `id` TEXT NOT NULL,
                        `wholesalerId` TEXT NOT NULL,
                        `wholesalerName` TEXT NOT NULL,
                        `entryType` TEXT NOT NULL,
                        `invoiceNumber` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `paymentSource` TEXT NOT NULL,
                        `referenceId` TEXT NOT NULL,
                        `amount` REAL NOT NULL,
                        `balanceAfter` REAL NOT NULL,
                        `entryDateMs` INTEGER NOT NULL,
                        `createdAtMs` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: ShopLocalDatabase? = null

        fun getInstance(context: Context): ShopLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ShopLocalDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
