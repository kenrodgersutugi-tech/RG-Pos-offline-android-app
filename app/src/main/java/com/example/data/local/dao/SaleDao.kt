package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SalePaymentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalePayments(payments: List<SalePaymentEntity>)

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :saleId LIMIT 1")
    suspend fun getSaleById(saleId: String): SaleEntity?

    @Query("SELECT * FROM sales WHERE saleNumber = :saleNumber LIMIT 1")
    suspend fun getSaleByNumber(saleNumber: String): SaleEntity?

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    fun getItemsForSale(saleId: String): Flow<List<SaleItemEntity>>

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getItemsForSaleDirect(saleId: String): List<SaleItemEntity>

    @Query("SELECT * FROM sale_payments WHERE saleId = :saleId")
    fun getPaymentsForSale(saleId: String): Flow<List<SalePaymentEntity>>

    @Query("SELECT * FROM sale_payments WHERE saleId = :saleId")
    suspend fun getPaymentsForSaleDirect(saleId: String): List<SalePaymentEntity>

    @Query("SELECT * FROM sales WHERE status = 'HELD' ORDER BY timestamp DESC")
    fun getHeldSales(): Flow<List<SaleEntity>>

    @Query("UPDATE sales SET status = :status WHERE id = :saleId")
    suspend fun updateSaleStatus(saleId: String, status: String)

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getSalesByDateRange(startTime: Long, endTime: Long): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getSalesByDateRangeDirect(startTime: Long, endTime: Long): List<SaleEntity>

    @Query("SELECT * FROM sale_items")
    suspend fun getAllSaleItemsDirect(): List<SaleItemEntity>

    @Query("SELECT * FROM sale_payments")
    suspend fun getAllSalePaymentsDirect(): List<SalePaymentEntity>

    @Query("SELECT COUNT(*) FROM sales WHERE status = 'COMPLETED'")
    suspend fun getCompletedSalesCount(): Int
}
