package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.PurchaseEntity
import com.example.data.local.entity.PurchaseItemEntity
import com.example.data.local.entity.SupplierEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SupplierAndPurchaseDao {
    // Suppliers
    @Query("SELECT * FROM suppliers WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    fun getAllSuppliers(): Flow<List<SupplierEntity>>

    @Query("SELECT * FROM suppliers WHERE id = :id LIMIT 1")
    suspend fun getSupplierById(id: String): SupplierEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSupplier(supplier: SupplierEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuppliers(suppliers: List<SupplierEntity>)

    @Update
    suspend fun updateSupplier(supplier: SupplierEntity)

    // Purchases
    @Query("SELECT * FROM purchases ORDER BY timestamp DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    @Query("SELECT * FROM purchases WHERE id = :id LIMIT 1")
    suspend fun getPurchaseById(id: String): PurchaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    fun getItemsForPurchase(purchaseId: String): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    suspend fun getItemsForPurchaseDirect(purchaseId: String): List<PurchaseItemEntity>

    @Query("SELECT * FROM purchase_items")
    suspend fun getAllPurchaseItemsDirect(): List<PurchaseItemEntity>
}
