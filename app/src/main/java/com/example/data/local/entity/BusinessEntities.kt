package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_profile")
data class BusinessProfileEntity(
    @PrimaryKey val id: String = "primary_business",
    val name: String = "RG Store",
    val phone: String = "+254 700 000000",
    val email: String = "info@rgpos.local",
    val address: String = "Nairobi, Kenya",
    val country: String = "Kenya",
    val currency: String = "KSh",
    val businessType: String = "GENERAL_SHOP", // GENERAL_SHOP, SUPERMARKET, WHOLESALE, HARDWARE, BEAUTY, PHARMACY, RESTAURANT
    val taxEnabled: Boolean = false,
    val defaultTaxRate: Double = 16.0,
    val receiptHeader: String = "RG POS — Sell. Track. Grow. Offline.",
    val receiptFooter: String = "Thank you for your business! Please come again.",
    val ownerName: String = "Admin Owner",
    val ownerPin: String = "1234",
    val activePriceTiers: String = "Retail,Wholesale,Distributor,VIP",
    val costCalculationMethod: String = "LAST_PURCHASE", // WEIGHTED_AVERAGE, FIFO, LAST_PURCHASE
    val isSetupCompleted: Boolean = false,
    val enabledModules: String = "POS,INVENTORY,REPORTS,CUSTOMERS,PURCHASES,EXPENSES,STAFF,BACKUP"
)

@Entity(tableName = "staff")
data class StaffEntity(
    @PrimaryKey val id: String,
    val name: String,
    val username: String,
    val pin: String,
    val role: String, // OWNER, ADMIN, MANAGER, CASHIER, INVENTORY_MANAGER
    val canChangePrices: Boolean = true,
    val canGiveDiscounts: Boolean = true,
    val canRefund: Boolean = true,
    val canAdjustStock: Boolean = true,
    val canImportExport: Boolean = true,
    val canViewReports: Boolean = true,
    val canManageCredit: Boolean = true,
    val canManageStaff: Boolean = true,
    val canManageSettings: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val staffName: String,
    val action: String, // SALE_COMPLETED, PRICE_CHANGED, STOCK_ADJUSTED, IMPORT_APPLIED, REFUND, BACKUP_CREATED, RESTORE
    val entity: String,
    val previousState: String? = null,
    val newState: String? = null,
    val reference: String? = null
)

@Entity(tableName = "import_history")
data class ImportHistoryEntity(
    @PrimaryKey val id: String, // e.g. IMP-20260922-001
    val timestamp: Long = System.currentTimeMillis(),
    val filename: String,
    val staffName: String,
    val rowsCount: Int,
    val newProducts: Int,
    val productsUpdated: Int,
    val stockAdded: Double,
    val stockRemoved: Double,
    val status: String, // SUCCESS, REVERSED, FAILED
    val details: String = ""
)
