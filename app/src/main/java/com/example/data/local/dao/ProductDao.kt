package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.FormPriceEntity
import com.example.data.local.entity.PackagingFormEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE sku = :sku LIMIT 1")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcode(barcode: String): ProductEntity?

    @Query("SELECT * FROM products WHERE internalCode = :code LIMIT 1")
    suspend fun getProductByInternalCode(code: String): ProductEntity?

    @Query("SELECT DISTINCT category FROM products WHERE isActive = 1 ORDER BY category ASC")
    fun getCategories(): Flow<List<String>>

    @Query("""
        SELECT * FROM products 
        WHERE isActive = 1 AND (
            name LIKE '%' || :query || '%' OR 
            sku LIKE '%' || :query || '%' OR 
            barcode LIKE '%' || :query || '%' OR 
            internalCode LIKE '%' || :query || '%' OR
            category LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
    """)
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Query("UPDATE products SET currentStockBaseUnits = :newStock, updatedAt = :timestamp WHERE id = :productId")
    suspend fun updateProductStock(productId: String, newStock: Double, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET isActive = 0 WHERE id = :productId")
    suspend fun deactivateProduct(productId: String)

    // Packaging Forms
    @Query("SELECT * FROM packaging_forms WHERE productId = :productId ORDER BY sortOrder ASC, quantityInBaseUnits ASC")
    fun getFormsForProduct(productId: String): Flow<List<PackagingFormEntity>>

    @Query("SELECT * FROM packaging_forms WHERE productId = :productId ORDER BY sortOrder ASC, quantityInBaseUnits ASC")
    suspend fun getFormsForProductDirect(productId: String): List<PackagingFormEntity>

    @Query("SELECT * FROM packaging_forms")
    fun getAllPackagingForms(): Flow<List<PackagingFormEntity>>

    @Query("SELECT * FROM packaging_forms WHERE id = :formId LIMIT 1")
    suspend fun getPackagingFormById(formId: String): PackagingFormEntity?

    @Query("SELECT * FROM packaging_forms WHERE barcode = :barcode LIMIT 1")
    suspend fun getPackagingFormByBarcode(barcode: String): PackagingFormEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackagingForm(form: PackagingFormEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackagingForms(forms: List<PackagingFormEntity>)

    @Delete
    suspend fun deletePackagingForm(form: PackagingFormEntity)

    // Form Prices
    @Query("SELECT * FROM form_prices WHERE packagingFormId = :formId")
    fun getPricesForForm(formId: String): Flow<List<FormPriceEntity>>

    @Query("SELECT * FROM form_prices WHERE packagingFormId = :formId")
    suspend fun getPricesForFormDirect(formId: String): List<FormPriceEntity>

    @Query("SELECT * FROM form_prices")
    fun getAllFormPrices(): Flow<List<FormPriceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormPrice(price: FormPriceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFormPrices(prices: List<FormPriceEntity>)

    // Stock Movements
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovements(movements: List<StockMovementEntity>)

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY timestamp DESC")
    fun getMovementsForProduct(productId: String): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentStockMovements(limit: Int = 100): Flow<List<StockMovementEntity>>

    @Query("SELECT * FROM stock_movements WHERE reference = :reference")
    suspend fun getMovementsByReference(reference: String): List<StockMovementEntity>
}
