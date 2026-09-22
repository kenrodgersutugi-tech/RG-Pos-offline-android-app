package com.example.domain.model

import com.example.data.local.entity.FormPriceEntity
import com.example.data.local.entity.PackagingFormEntity
import com.example.data.local.entity.ProductEntity

data class ProductWithForms(
    val product: ProductEntity,
    val forms: List<PackagingFormWithPrices>
)

data class PackagingFormWithPrices(
    val form: PackagingFormEntity,
    val prices: List<FormPriceEntity>
) {
    fun getPriceForTier(tier: String): Double {
        return prices.find { it.priceTier.equals(tier, ignoreCase = true) }?.price
            ?: prices.find { it.priceTier.equals("Retail", ignoreCase = true) }?.price
            ?: 0.0
    }
}

data class CartItem(
    val productId: String,
    val formId: String,
    val productName: String,
    val formName: String,
    val quantityInBaseUnits: Double,
    val quantity: Double, // Number of units of this form
    val unitPrice: Double, // Actual transaction selling price
    val costPrice: Double, // Purchase cost
    val discount: Double = 0.0
) {
    val subtotal: Double get() = quantity * unitPrice
    val total: Double get() = (subtotal - discount).coerceAtLeast(0.0)
    val totalCost: Double get() = quantity * costPrice
    val profit: Double get() = total - totalCost
    val totalBaseUnits: Double get() = quantity * quantityInBaseUnits
}

data class PaymentSplit(
    val cashAmount: Double = 0.0,
    val mpesaAmount: Double = 0.0,
    val mpesaReference: String = "",
    val creditAmount: Double = 0.0,
    val customerId: String? = null,
    val customerName: String = "Walk-in Customer"
) {
    val totalPaid: Double get() = cashAmount + mpesaAmount + creditAmount
}

data class DashboardStats(
    val todaySales: Double = 0.0,
    val todayProfit: Double = 0.0,
    val todayTransactions: Int = 0,
    val todayExpenses: Double = 0.0,
    val outstandingCredit: Double = 0.0,
    val cashInDrawer: Double = 0.0,
    val mpesaSales: Double = 0.0,
    val cashSales: Double = 0.0,
    val creditSales: Double = 0.0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val topSellingProducts: List<TopProductItem> = emptyList()
)

data class TopProductItem(
    val productName: String,
    val formName: String,
    val totalQuantitySold: Double,
    val totalRevenue: Double
)

enum class DateRangeFilter(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    LAST_WEEK("Last Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

data class ReportFilter(
    val dateRange: DateRangeFilter = DateRangeFilter.TODAY,
    val customStartTimestamp: Long? = null,
    val customEndTimestamp: Long? = null,
    val staffName: String = "All Staff",
    val paymentMethod: String = "All", // All, CASH, MPESA, CREDIT, PARTIAL
    val customerSegment: String = "All Segments"
)

data class ImportRowData(
    val rowNumber: Int,
    val productId: String = "",
    val productName: String = "",
    val category: String = "General",
    val baseUnit: String = "Piece",
    val sku: String = "",
    val barcode: String = "",
    val internalCode: String = "",
    val formName: String = "Standard",
    val formQtyInBaseUnits: Double = 1.0,
    val purchaseCost: Double = 0.0,
    val retailPrice: Double = 0.0,
    val wholesalePrice: Double = 0.0,
    val operation: String = "ADD", // ADD, ADJUST, SET, REMOVE, NO_CHANGE
    val stockQty: Double = 0.0,
    val supplier: String = "",
    val isNewProduct: Boolean = false,
    val error: String? = null,
    val warning: String? = null
)

data class ImportPreviewResult(
    val totalRows: Int = 0,
    val newProductsCount: Int = 0,
    val productsUpdatedCount: Int = 0,
    val stockAdded: Double = 0.0,
    val stockRemoved: Double = 0.0,
    val priceChangesCount: Int = 0,
    val packagingChangesCount: Int = 0,
    val warningsCount: Int = 0,
    val errorsCount: Int = 0,
    val rows: List<ImportRowData> = emptyList(),
    val batchId: String = ""
)
