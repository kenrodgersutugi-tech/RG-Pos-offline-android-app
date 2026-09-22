package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customers",
    indices = [
        Index(value = ["phone"], unique = false),
        Index(value = ["segment"])
    ]
)
data class CustomerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val segment: String = "Retail", // Retail, Wholesale, Distributor, VIP, Credit
    val creditLimit: Double = 50000.0,
    val currentCreditBalance: Double = 0.0,
    val isActive: Boolean = true,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "credit_transactions",
    indices = [
        Index(value = ["customerId"]),
        Index(value = ["timestamp"])
    ]
)
data class CreditTransactionEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val saleId: String? = null,
    val transactionType: String, // CREDIT_SALE, PAYMENT, ADJUSTMENT
    val amount: Double,
    val balanceAfter: Double,
    val notes: String = "",
    val paymentMethod: String? = null, // CASH, MPESA (when paying down credit)
    val reference: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "suppliers",
    indices = [Index(value = ["name"])]
)
data class SupplierEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val currentBalance: Double = 0.0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchases",
    indices = [
        Index(value = ["supplierId"]),
        Index(value = ["timestamp"])
    ]
)
data class PurchaseEntity(
    @PrimaryKey val id: String, // e.g. PO-1001
    val purchaseNumber: String,
    val supplierId: String,
    val supplierName: String,
    val totalAmount: Double,
    val paidAmount: Double,
    val status: String = "RECEIVED", // RECEIVED, PENDING, RETURNED
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "purchase_items",
    indices = [
        Index(value = ["purchaseId"]),
        Index(value = ["productId"])
    ]
)
data class PurchaseItemEntity(
    @PrimaryKey val id: String,
    val purchaseId: String,
    val productId: String,
    val productName: String,
    val packagingFormId: String,
    val formName: String,
    val quantity: Double,
    val unitCost: Double,
    val totalCost: Double
)
