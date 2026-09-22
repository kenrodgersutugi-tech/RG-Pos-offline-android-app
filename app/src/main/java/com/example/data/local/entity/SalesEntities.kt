package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["customerId"]),
        Index(value = ["staffId"]),
        Index(value = ["status"])
    ]
)
data class SaleEntity(
    @PrimaryKey val id: String, // e.g. REC-10001
    val saleNumber: String,
    val timestamp: Long = System.currentTimeMillis(),
    val customerId: String? = null,
    val customerName: String = "Walk-in Customer",
    val customerSegment: String = "Walk-in",
    val staffId: String = "staff_1",
    val staffName: String = "Cashier",
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val costAmount: Double = 0.0,
    val grossProfit: Double = 0.0,
    val status: String = "COMPLETED", // COMPLETED, HELD, REFUNDED, CANCELLED
    val notes: String = ""
)

@Entity(
    tableName = "sale_items",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["productId"])
    ]
)
data class SaleItemEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val productId: String,
    val productName: String,
    val packagingFormId: String,
    val formName: String,
    val quantityInBaseUnits: Double,
    val quantity: Double, // Number of forms sold (e.g. 2 cartons)
    val unitPrice: Double, // Actual transaction price per form
    val costPrice: Double = 0.0, // Purchase cost per form
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val profit: Double = 0.0
)

@Entity(
    tableName = "sale_payments",
    indices = [
        Index(value = ["saleId"]),
        Index(value = ["paymentMethod"])
    ]
)
data class SalePaymentEntity(
    @PrimaryKey val id: String,
    val saleId: String,
    val paymentMethod: String, // CASH, MPESA, CREDIT
    val amount: Double,
    val reference: String = "", // e.g. M-Pesa transaction code
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cash_shifts",
    indices = [Index(value = ["status"])]
)
data class CashShiftEntity(
    @PrimaryKey val id: String,
    val staffName: String,
    val openingCash: Double = 0.0,
    val closingCash: Double? = null,
    val expectedCash: Double = 0.0,
    val variance: Double? = null,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val status: String = "OPEN", // OPEN, CLOSED
    val notes: String = ""
)

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["category"]),
        Index(value = ["timestamp"])
    ]
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    val category: String, // Rent, Utilities, Transport, Salaries, Supplies, Other
    val amount: Double,
    val paidTo: String = "",
    val paymentMethod: String = "CASH", // CASH, MPESA
    val reference: String = "",
    val staffName: String = "Staff",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
