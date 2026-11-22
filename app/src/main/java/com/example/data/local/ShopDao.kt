package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {

    // --- PRODUCTS ---
    @Query("SELECT * FROM products ORDER BY nameUrdu ASC")
    fun observeProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsOnce(): List<ProductEntity>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun countProducts(): Int

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProduct(entity: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProducts(entities: List<ProductEntity>)

    @Query("UPDATE products SET stockQty = :stockQty, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun updateProductStock(id: String, stockQty: Double, updatedAtMs: Long)

    @Query("UPDATE products SET stockQty = :stockQty, costPrice = :costPrice, sellingPrice = :sellingPrice, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun updateProductStockAndPrices(id: String, stockQty: Double, costPrice: Double, sellingPrice: Double, updatedAtMs: Long)

    @Query("UPDATE products SET costPrice = :costPrice, sellingPrice = :sellingPrice, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun updateProductPrices(id: String, costPrice: Double, sellingPrice: Double, updatedAtMs: Long)

    @Query("DELETE FROM products WHERE id = :id")
    suspend fun deleteProductById(id: String)

    // --- CUSTOMERS ---
    @Query("SELECT * FROM customers ORDER BY balance DESC, name ASC")
    fun observeCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersOnce(): List<CustomerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomer(entity: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCustomers(entities: List<CustomerEntity>)

    @Query("UPDATE customers SET balance = :balance, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun updateCustomerBalance(id: String, balance: Double, updatedAtMs: Long)

    // --- ORDERS ---
    @Query("SELECT * FROM orders ORDER BY createdAtMs DESC")
    fun observeOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders")
    suspend fun getAllOrdersOnce(): List<OrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(entity: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrders(entities: List<OrderEntity>)

    @Query("UPDATE orders SET status = :status, paymentMethod = :paymentMethod, paidAmount = :paidAmount, remainingUdhar = :remainingUdhar, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun updateOrderCompletion(id: String, status: String, paymentMethod: String, paidAmount: Double, remainingUdhar: Double, updatedAtMs: Long)

    @Query("UPDATE orders SET status = :status, updatedAtMs = :updatedAtMs WHERE id = :id")
    suspend fun updateOrderStatus(id: String, status: String, updatedAtMs: Long)

    // --- LEDGER ENTRIES ---
    @Query("SELECT * FROM ledger_entries ORDER BY createdAtMs DESC")
    fun observeLedgerEntries(): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entries")
    suspend fun getAllLedgerEntriesOnce(): List<LedgerEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(entity: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLedgerEntries(entities: List<LedgerEntryEntity>)

    // --- STOCK MOVEMENTS ---
    @Query("SELECT * FROM stock_movements ORDER BY createdAtMs DESC")
    fun observeStockMovements(): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements")
    suspend fun getAllStockMovementsOnce(): List<StockMovementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(entity: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStockMovements(entities: List<StockMovementEntity>)

    // --- RATION DONATIONS ---
    @Query("SELECT * FROM ration_donations ORDER BY createdAtMs DESC")
    fun observeDonations(): Flow<List<RationDonationEntity>>

    @Query("SELECT * FROM ration_donations")
    suspend fun getAllDonationsOnce(): List<RationDonationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDonation(entity: RationDonationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDonations(entities: List<RationDonationEntity>)

    // --- MARYAM NAWAZ RATION CARD ITEMS ---
    @Query("SELECT * FROM ration_card_items ORDER BY sortOrder ASC, nameUrdu ASC")
    fun observeRationCardItems(): Flow<List<RationCardItemEntity>>

    @Query("SELECT * FROM ration_card_items")
    suspend fun getAllRationCardItemsOnce(): List<RationCardItemEntity>

    @Query("SELECT COUNT(*) FROM ration_card_items")
    suspend fun countRationCardItems(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRationCardItem(entity: RationCardItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRationCardItems(entities: List<RationCardItemEntity>)

    @Query("DELETE FROM ration_card_items WHERE id = :id")
    suspend fun deleteRationCardItemById(id: String)

    // --- MARYAM NAWAZ RATION CARD BILLS ---
    @Query("SELECT * FROM ration_card_bills ORDER BY createdAtMs DESC")
    fun observeRationCardBills(): Flow<List<RationCardBillEntity>>

    @Query("SELECT * FROM ration_card_bills")
    suspend fun getAllRationCardBillsOnce(): List<RationCardBillEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRationCardBill(entity: RationCardBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRationCardBills(entities: List<RationCardBillEntity>)
}
