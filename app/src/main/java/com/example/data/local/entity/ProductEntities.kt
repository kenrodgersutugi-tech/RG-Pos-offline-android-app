package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["sku"], unique = false),
        Index(value = ["barcode"], unique = false),
        Index(value = ["internalCode"], unique = false),
        Index(value = ["category"])
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String, // e.g. PRD-xxxx
    val name: String,
    val category: String = "General",
    val baseUnit: String = "Piece", // KG, Bottle, Piece, Metre, Tin, Litre, etc.
    val sku: String? = null,
    val barcode: String? = null,
    val internalCode: String? = null,
    val productType: String = "STANDARD", // STANDARD, VARIABLE, SERVICE, BATCH_EXPIRY
    val currentStockBaseUnits: Double = 0.0,
    val reorderLevel: Double = 5.0,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "packaging_forms",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["barcode"], unique = false),
        Index(value = ["sku"], unique = false)
    ]
)
data class PackagingFormEntity(
    @PrimaryKey val id: String, // e.g. FORM-xxxx
    val productId: String,
    val name: String, // e.g. "Bottle", "Carton (18 pcs)", "Carton (22 pcs)", "50 KG Sack", "1 KG"
    val quantityInBaseUnits: Double, // e.g. 1.0, 18.0, 22.0, 50.0
    val barcode: String? = null,
    val sku: String? = null,
    val purchaseCost: Double = 0.0,
    val purchaseEnabled: Boolean = true,
    val retailSaleEnabled: Boolean = true,
    val wholesaleSaleEnabled: Boolean = true,
    val distributorSaleEnabled: Boolean = false,
    val canBeOpened: Boolean = false, // If true, can break down into loose base units (e.g. sack to kg)
    val isDefaultSelling: Boolean = false,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "form_prices",
    indices = [
        Index(value = ["packagingFormId"]),
        Index(value = ["packagingFormId", "priceTier"], unique = true)
    ]
)
data class FormPriceEntity(
    @PrimaryKey val id: String, // e.g. PRC-xxxx
    val packagingFormId: String,
    val priceTier: String, // "Retail", "Wholesale", "Distributor", "VIP", "Custom"
    val price: Double
)

@Entity(
    tableName = "stock_movements",
    indices = [
        Index(value = ["productId"]),
        Index(value = ["movementType"]),
        Index(value = ["timestamp"])
    ]
)
data class StockMovementEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val packagingFormId: String? = null,
    val movementType: String, // PURCHASE, SALE, RETURN, ADJUSTMENT, SET, ADD, REMOVE, TRANSFER, DAMAGE, EXPIRY, STOCK_COUNT, PACKAGE_OPENED, IMPORT_ADD, IMPORT_REMOVE, IMPORT_ADJUST, IMPORT_SET
    val formQuantity: Double, // Quantity in the form (e.g. 2 cartons)
    val baseQuantityDelta: Double, // Delta in base units (+36 or -36)
    val unitCost: Double = 0.0,
    val totalCost: Double = 0.0,
    val reason: String = "",
    val reference: String = "", // Sale number, PO number, or import ID
    val staffName: String = "Staff",
    val timestamp: Long = System.currentTimeMillis()
)
