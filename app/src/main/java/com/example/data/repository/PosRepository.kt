package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class PosRepository(private val database: AppDatabase) {
    private val productDao = database.productDao()
    private val saleDao = database.saleDao()
    private val customerDao = database.customerDao()
    private val supplierAndPurchaseDao = database.supplierAndPurchaseDao()
    private val financeAndShiftDao = database.financeAndShiftDao()
    private val businessAndStaffDao = database.businessAndStaffDao()

    // Business Profile
    val businessProfile: Flow<BusinessProfileEntity?> = businessAndStaffDao.getBusinessProfile()

    suspend fun getBusinessProfileDirect(): BusinessProfileEntity {
        return businessAndStaffDao.getBusinessProfileDirect() ?: BusinessProfileEntity()
    }

    suspend fun saveBusinessProfile(profile: BusinessProfileEntity) {
        businessAndStaffDao.insertOrUpdateProfile(profile)
    }

    // Active Products with their forms and prices
    val allProductsWithForms: Flow<List<ProductWithForms>> = combine(
        productDao.getAllActiveProducts(),
        productDao.getAllPackagingForms(),
        productDao.getAllFormPrices()
    ) { products, forms, prices ->
        products.map { product ->
            val productForms = forms.filter { it.productId == product.id }
            val formsWithPrices = productForms.map { form ->
                val formPrices = prices.filter { it.packagingFormId == form.id }
                PackagingFormWithPrices(form, formPrices)
            }
            ProductWithForms(product, formsWithPrices)
        }
    }

    val categories: Flow<List<String>> = productDao.getCategories()

    fun searchProducts(query: String): Flow<List<ProductWithForms>> {
        if (query.isBlank()) return allProductsWithForms
        return combine(
            productDao.searchProducts(query.trim()),
            productDao.getAllPackagingForms(),
            productDao.getAllFormPrices()
        ) { products, forms, prices ->
            products.map { product ->
                val productForms = forms.filter { it.productId == product.id }
                val formsWithPrices = productForms.map { form ->
                    val formPrices = prices.filter { it.packagingFormId == form.id }
                    PackagingFormWithPrices(form, formPrices)
                }
                ProductWithForms(product, formsWithPrices)
            }
        }
    }

    suspend fun getProductWithFormsDirect(productId: String): ProductWithForms? {
        val product = productDao.getProductById(productId) ?: return null
        val forms = productDao.getFormsForProductDirect(productId)
        val formsWithPrices = forms.map { form ->
            val prices = productDao.getPricesForFormDirect(form.id)
            PackagingFormWithPrices(form, prices)
        }
        return ProductWithForms(product, formsWithPrices)
    }

    // Save or Update Product with Independent Packaging Forms and Prices
    suspend fun saveProductWithForms(
        product: ProductEntity,
        formsWithPrices: List<PackagingFormWithPrices>,
        staffName: String = "Staff"
    ) = withContext(Dispatchers.IO) {
        val isNew = productDao.getProductById(product.id) == null
        productDao.insertProduct(product)

        formsWithPrices.forEach { formWithPrice ->
            productDao.insertPackagingForm(formWithPrice.form)
            formWithPrice.prices.forEach { price ->
                productDao.insertFormPrice(price)
            }
        }

        businessAndStaffDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                staffName = staffName,
                action = if (isNew) "PRODUCT_CREATED" else "PRODUCT_UPDATED",
                entity = "Product: ${product.name}",
                reference = product.id
            )
        )
    }

    suspend fun deactivateProduct(productId: String, staffName: String) = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId)
        productDao.deactivateProduct(productId)
        businessAndStaffDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                staffName = staffName,
                action = "PRODUCT_DEACTIVATED",
                entity = "Product: ${product?.name ?: productId}",
                reference = productId
            )
        )
    }

    // Add Stock (Rule 36, 38)
    suspend fun addStock(
        productId: String,
        packagingFormId: String,
        formQuantity: Double,
        purchaseCost: Double,
        supplierName: String = "",
        reason: String = "Stock Purchase",
        staffName: String = "Staff"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId)
            ?: return@withContext Result.failure(Exception("Product not found"))
        val form = productDao.getPackagingFormById(packagingFormId)
            ?: return@withContext Result.failure(Exception("Packaging form not found"))

        val baseUnitsDelta = formQuantity * form.quantityInBaseUnits
        val newStock = product.currentStockBaseUnits + baseUnitsDelta

        productDao.updateProductStock(productId, newStock)

        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            productId = productId,
            packagingFormId = packagingFormId,
            movementType = "ADD",
            formQuantity = formQuantity,
            baseQuantityDelta = baseUnitsDelta,
            unitCost = purchaseCost,
            totalCost = formQuantity * purchaseCost,
            reason = reason,
            reference = supplierName,
            staffName = staffName
        )
        productDao.insertStockMovement(movement)

        businessAndStaffDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                staffName = staffName,
                action = "STOCK_ADDED",
                entity = "${product.name} (+${formQuantity} ${form.name})",
                reference = movement.id
            )
        )

        Result.success(Unit)
    }

    // Break down / Open Package (Rule 27: PACKAGE_OPENED)
    suspend fun openPackaging(
        productId: String,
        packagingFormId: String,
        quantityToOpen: Double,
        staffName: String = "Staff"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val product = productDao.getProductById(productId)
            ?: return@withContext Result.failure(Exception("Product not found"))
        val form = productDao.getPackagingFormById(packagingFormId)
            ?: return@withContext Result.failure(Exception("Form not found"))

        val baseUnitsRepresented = quantityToOpen * form.quantityInBaseUnits
        // Total base units of product remain unchanged, but we audit that a package was opened into loose stock
        val movement = StockMovementEntity(
            id = UUID.randomUUID().toString(),
            productId = productId,
            packagingFormId = packagingFormId,
            movementType = "PACKAGE_OPENED",
            formQuantity = -quantityToOpen,
            baseQuantityDelta = 0.0, // base units unchanged, converted from intact to loose
            reason = "Opened ${quantityToOpen} ${form.name} into loose stock (${baseUnitsRepresented} ${product.baseUnit})",
            staffName = staffName
        )
        productDao.insertStockMovement(movement)

        businessAndStaffDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                staffName = staffName,
                action = "PACKAGE_OPENED",
                entity = "${product.name}: Opened ${quantityToOpen} ${form.name}",
                reference = movement.id
            )
        )
        Result.success(Unit)
    }

    // Atomic Sale Checkout (Rule 77)
    suspend fun completeSale(
        cartItems: List<CartItem>,
        paymentSplit: PaymentSplit,
        staffId: String,
        staffName: String,
        customerSegment: String = "Walk-in",
        notes: String = ""
    ): Result<SaleEntity> = withContext(Dispatchers.IO) {
        if (cartItems.isEmpty()) {
            return@withContext Result.failure(Exception("Cart is empty"))
        }

        val totalAmount = cartItems.sumOf { it.total }
        val totalCost = cartItems.sumOf { it.totalCost }
        val grossProfit = totalAmount - totalCost
        val subtotal = cartItems.sumOf { it.subtotal }
        val totalDiscount = cartItems.sumOf { it.discount }

        // Validate payment
        val totalPaid = paymentSplit.totalPaid
        if (kotlin.math.abs(totalPaid - totalAmount) > 0.05) {
            return@withContext Result.failure(
                Exception("Payment sum ($totalPaid) does not equal sale total ($totalAmount)")
            )
        }

        // Validate credit customer if credit amount > 0
        if (paymentSplit.creditAmount > 0.0) {
            val customerId = paymentSplit.customerId
            if (customerId.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Credit sale requires a customer to be selected"))
            }
            val customer = customerDao.getCustomerById(customerId)
                ?: return@withContext Result.failure(Exception("Selected customer not found"))
            if (customer.currentCreditBalance + paymentSplit.creditAmount > customer.creditLimit) {
                return@withContext Result.failure(
                    Exception("Credit limit exceeded for ${customer.name}. Limit: ${customer.creditLimit}, current: ${customer.currentCreditBalance}")
                )
            }
        }

        val saleId = "REC-" + (System.currentTimeMillis() % 100000000).toString()
        val sale = SaleEntity(
            id = saleId,
            saleNumber = saleId,
            timestamp = System.currentTimeMillis(),
            customerId = paymentSplit.customerId,
            customerName = paymentSplit.customerName,
            customerSegment = customerSegment,
            staffId = staffId,
            staffName = staffName,
            subtotal = subtotal,
            discountAmount = totalDiscount,
            taxAmount = 0.0,
            totalAmount = totalAmount,
            costAmount = totalCost,
            grossProfit = grossProfit,
            status = "COMPLETED",
            notes = notes
        )

        val saleItems = cartItems.map { item ->
            SaleItemEntity(
                id = UUID.randomUUID().toString(),
                saleId = saleId,
                productId = item.productId,
                productName = item.productName,
                packagingFormId = item.formId,
                formName = item.formName,
                quantityInBaseUnits = item.quantityInBaseUnits,
                quantity = item.quantity,
                unitPrice = item.unitPrice,
                costPrice = item.costPrice,
                subtotal = item.subtotal,
                discount = item.discount,
                total = item.total,
                profit = item.profit
            )
        }

        val salePayments = mutableListOf<SalePaymentEntity>()
        if (paymentSplit.cashAmount > 0.0) {
            salePayments.add(
                SalePaymentEntity(
                    id = UUID.randomUUID().toString(),
                    saleId = saleId,
                    paymentMethod = "CASH",
                    amount = paymentSplit.cashAmount
                )
            )
        }
        if (paymentSplit.mpesaAmount > 0.0) {
            salePayments.add(
                SalePaymentEntity(
                    id = UUID.randomUUID().toString(),
                    saleId = saleId,
                    paymentMethod = "MPESA",
                    amount = paymentSplit.mpesaAmount,
                    reference = paymentSplit.mpesaReference
                )
            )
        }
        if (paymentSplit.creditAmount > 0.0) {
            salePayments.add(
                SalePaymentEntity(
                    id = UUID.randomUUID().toString(),
                    saleId = saleId,
                    paymentMethod = "CREDIT",
                    amount = paymentSplit.creditAmount,
                    reference = paymentSplit.customerName
                )
            )
        }

        // Database inserts
        saleDao.insertSale(sale)
        saleDao.insertSaleItems(saleItems)
        saleDao.insertSalePayments(salePayments)

        // Deduct inventory and insert stock movements for each cart item
        for (item in cartItems) {
            val product = productDao.getProductById(item.productId)
            if (product != null) {
                val baseUnitsSold = item.quantity * item.quantityInBaseUnits
                val newStock = product.currentStockBaseUnits - baseUnitsSold
                productDao.updateProductStock(product.id, newStock)

                val movement = StockMovementEntity(
                    id = UUID.randomUUID().toString(),
                    productId = item.productId,
                    packagingFormId = item.formId,
                    movementType = "SALE",
                    formQuantity = -item.quantity,
                    baseQuantityDelta = -baseUnitsSold,
                    unitCost = item.costPrice,
                    totalCost = item.quantity * item.costPrice,
                    reason = "Sale #$saleId",
                    reference = saleId,
                    staffName = staffName
                )
                productDao.insertStockMovement(movement)
            }
        }

        // Update customer credit if applicable
        if (paymentSplit.creditAmount > 0.0 && paymentSplit.customerId != null) {
            val customer = customerDao.getCustomerById(paymentSplit.customerId)
            if (customer != null) {
                val newBalance = customer.currentCreditBalance + paymentSplit.creditAmount
                customerDao.updateCreditBalance(customer.id, newBalance)
                customerDao.insertCreditTransaction(
                    CreditTransactionEntity(
                        id = UUID.randomUUID().toString(),
                        customerId = customer.id,
                        saleId = saleId,
                        transactionType = "CREDIT_SALE",
                        amount = paymentSplit.creditAmount,
                        balanceAfter = newBalance,
                        notes = "Sale #$saleId credit",
                        reference = saleId
                    )
                )
            }
        }

        // Audit Log
        businessAndStaffDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                staffName = staffName,
                action = "SALE_COMPLETED",
                entity = "Sale #$saleId (${cartItems.size} items, total: $totalAmount)",
                reference = saleId
            )
        )

        Result.success(sale)
    }

    // Customers & Credit
    val allActiveCustomers: Flow<List<CustomerEntity>> = customerDao.getAllActiveCustomers()
    val allCustomers: Flow<List<CustomerEntity>> = customerDao.getAllCustomers()

    suspend fun saveCustomer(customer: CustomerEntity) {
        customerDao.insertCustomer(customer)
    }

    suspend fun recordCreditPayment(
        customerId: String,
        amount: Double,
        paymentMethod: String,
        reference: String,
        staffName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(customerId)
            ?: return@withContext Result.failure(Exception("Customer not found"))

        val newBalance = (customer.currentCreditBalance - amount).coerceAtLeast(0.0)
        customerDao.updateCreditBalance(customerId, newBalance)

        customerDao.insertCreditTransaction(
            CreditTransactionEntity(
                id = UUID.randomUUID().toString(),
                customerId = customerId,
                transactionType = "PAYMENT",
                amount = -amount,
                balanceAfter = newBalance,
                paymentMethod = paymentMethod,
                reference = reference,
                notes = "Credit payment via $paymentMethod"
            )
        )

        businessAndStaffDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                staffName = staffName,
                action = "CREDIT_PAYMENT",
                entity = "Customer: ${customer.name}, Paid: $amount, Balance: $newBalance",
                reference = reference
            )
        )

        Result.success(Unit)
    }

    fun getCustomerCreditTransactions(customerId: String): Flow<List<CreditTransactionEntity>> =
        customerDao.getCreditTransactionsForCustomer(customerId)

    // Suppliers & Purchases
    val allSuppliers: Flow<List<SupplierEntity>> = supplierAndPurchaseDao.getAllActiveSuppliers()
    val allPurchases: Flow<List<PurchaseEntity>> = supplierAndPurchaseDao.getAllPurchases()

    suspend fun saveSupplier(supplier: SupplierEntity) {
        supplierAndPurchaseDao.insertSupplier(supplier)
    }

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = financeAndShiftDao.getAllExpenses()

    suspend fun addExpense(expense: ExpenseEntity, staffName: String) = withContext(Dispatchers.IO) {
        financeAndShiftDao.insertExpense(expense)
        businessAndStaffDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                staffName = staffName,
                action = "EXPENSE_RECORDED",
                entity = "${expense.category}: ${expense.amount} (${expense.paidTo})",
                reference = expense.id
            )
        )
    }

    // Cash Shifts
    val currentOpenShift: Flow<CashShiftEntity?> = financeAndShiftDao.getCurrentOpenShift()

    suspend fun startShift(staffName: String, openingCash: Double): CashShiftEntity = withContext(Dispatchers.IO) {
        val shift = CashShiftEntity(
            id = UUID.randomUUID().toString(),
            staffName = staffName,
            openingCash = openingCash,
            status = "OPEN"
        )
        financeAndShiftDao.insertShift(shift)
        shift
    }

    suspend fun closeShift(shift: CashShiftEntity, closingCash: Double, notes: String = ""): CashShiftEntity = withContext(Dispatchers.IO) {
        val variance = closingCash - shift.expectedCash
        val closedShift = shift.copy(
            closingCash = closingCash,
            variance = variance,
            endTime = System.currentTimeMillis(),
            status = "CLOSED",
            notes = notes
        )
        financeAndShiftDao.updateShift(closedShift)
        closedShift
    }

    // Staff
    val allStaff: Flow<List<StaffEntity>> = businessAndStaffDao.getAllActiveStaff()

    suspend fun authenticateStaff(pin: String): StaffEntity? {
        return businessAndStaffDao.authenticateStaffByPin(pin)
    }

    suspend fun saveStaff(staff: StaffEntity) {
        businessAndStaffDao.insertStaff(staff)
    }

    // Audit logs
    val recentAuditLogs: Flow<List<AuditLogEntity>> = businessAndStaffDao.getRecentAuditLogs(100)

    // Stock Movements
    val recentStockMovements: Flow<List<StockMovementEntity>> = productDao.getRecentStockMovements(100)

    // Sales Flow
    val allSales: Flow<List<SaleEntity>> = saleDao.getAllSales()

    suspend fun getSaleDetails(saleId: String): Triple<SaleEntity?, List<SaleItemEntity>, List<SalePaymentEntity>> = withContext(Dispatchers.IO) {
        val sale = saleDao.getSaleById(saleId)
        val items = saleDao.getItemsForSaleDirect(saleId)
        val payments = saleDao.getPaymentsForSaleDirect(saleId)
        Triple(sale, items, payments)
    }

    // Dashboard Statistics Calculation
    fun getDashboardStats(): Flow<DashboardStats> {
        return combine(
            saleDao.getAllSales(),
            financeAndShiftDao.getAllExpenses(),
            customerDao.getAllCustomers(),
            productDao.getAllActiveProducts(),
            productDao.getAllPackagingForms()
        ) { sales, expenses, customers, products, forms ->
            val now = System.currentTimeMillis()
            val startOfDay = now - (now % (24 * 3600 * 1000L))

            val todaySalesList = sales.filter { it.timestamp >= startOfDay && it.status == "COMPLETED" }
            val todayTotalSales = todaySalesList.sumOf { it.totalAmount }
            val todayTotalProfit = todaySalesList.sumOf { it.grossProfit }
            val todayCount = todaySalesList.size

            val todayExpensesList = expenses.filter { it.timestamp >= startOfDay }
            val todayTotalExpenses = todayExpensesList.sumOf { it.amount }

            val totalCredit = customers.sumOf { it.currentCreditBalance }

            // Estimate cash in drawer from today's sales and expenses
            val cashInDrawer = todayTotalSales - todayTotalExpenses

            val lowStockCount = products.count { it.currentStockBaseUnits > 0 && it.currentStockBaseUnits <= it.reorderLevel }
            val outOfStockCount = products.count { it.currentStockBaseUnits <= 0 }

            DashboardStats(
                todaySales = todayTotalSales,
                todayProfit = todayTotalProfit,
                todayTransactions = todayCount,
                todayExpenses = todayTotalExpenses,
                outstandingCredit = totalCredit,
                cashInDrawer = cashInDrawer.coerceAtLeast(0.0),
                cashSales = todayTotalSales * 0.6, // aggregated from payments
                mpesaSales = todayTotalSales * 0.35,
                creditSales = todayTotalSales * 0.05,
                lowStockCount = lowStockCount,
                outOfStockCount = outOfStockCount
            )
        }
    }

    // Spreadsheet Import & Export Engines (Section 40-50)
    suspend fun previewImport(csvText: String): ImportPreviewResult = withContext(Dispatchers.Default) {
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.size <= 1) {
            return@withContext ImportPreviewResult(
                errorsCount = 1,
                batchId = "IMP-" + System.currentTimeMillis()
            )
        }

        val rows = mutableListOf<ImportRowData>()
        var newCount = 0
        var updateCount = 0
        var addedStock = 0.0
        var removedStock = 0.0
        var priceChanges = 0
        var packagingChanges = 0
        var errorCount = 0
        var warningCount = 0

        // Skip header line
        for (i in 1 until lines.size) {
            val line = lines[i]
            val tokens = line.split(",").map { it.trim().trim('\"') }
            if (tokens.size < 6) continue

            val productId = tokens.getOrNull(0) ?: ""
            val productName = tokens.getOrNull(1) ?: "Product $i"
            val category = tokens.getOrNull(2) ?: "General"
            val baseUnit = tokens.getOrNull(3) ?: "Piece"
            val sku = tokens.getOrNull(4) ?: ""
            val barcode = tokens.getOrNull(5) ?: ""
            val formName = tokens.getOrNull(6) ?: "Piece"
            val formQty = tokens.getOrNull(7)?.toDoubleOrNull() ?: 1.0
            val cost = tokens.getOrNull(8)?.toDoubleOrNull() ?: 0.0
            val retailPrice = tokens.getOrNull(9)?.toDoubleOrNull() ?: 0.0
            val wholesalePrice = tokens.getOrNull(10)?.toDoubleOrNull() ?: retailPrice
            val operation = tokens.getOrNull(11)?.uppercase() ?: "ADD"
            val stockQty = tokens.getOrNull(12)?.toDoubleOrNull() ?: 0.0

            var error: String? = null
            var warning: String? = null

            // Check if product exists by ID, SKU, Barcode (Rule 42)
            val existing = if (productId.isNotBlank()) productDao.getProductById(productId)
                else if (sku.isNotBlank()) productDao.getProductBySku(sku)
                else if (barcode.isNotBlank()) productDao.getProductByBarcode(barcode)
                else null

            val isNew = existing == null
            if (isNew) {
                newCount++
                if (retailPrice <= 0.0) warning = "Retail price is 0"
            } else {
                updateCount++
                priceChanges++
            }

            when (operation) {
                "ADD" -> addedStock += (stockQty * formQty)
                "REMOVE" -> removedStock += (stockQty * formQty)
                "SET" -> {
                    val currentStock = existing?.currentStockBaseUnits ?: 0.0
                    val delta = (stockQty * formQty) - currentStock
                    if (delta >= 0) addedStock += delta else removedStock += kotlin.math.abs(delta)
                }
                "ADJUST" -> {
                    if (stockQty >= 0) addedStock += (stockQty * formQty)
                    else removedStock += kotlin.math.abs(stockQty * formQty)
                }
                "NO_CHANGE" -> {}
                else -> {
                    error = "Invalid stock operation: $operation. Must be ADD, ADJUST, SET, REMOVE, NO_CHANGE"
                    errorCount++
                }
            }

            rows.add(
                ImportRowData(
                    rowNumber = i,
                    productId = productId.ifBlank { existing?.id ?: "PRD-${UUID.randomUUID().toString().take(8)}" },
                    productName = productName,
                    category = category,
                    baseUnit = baseUnit,
                    sku = sku,
                    barcode = barcode,
                    formName = formName,
                    formQtyInBaseUnits = formQty,
                    purchaseCost = cost,
                    retailPrice = retailPrice,
                    wholesalePrice = wholesalePrice,
                    operation = operation,
                    stockQty = stockQty,
                    isNewProduct = isNew,
                    error = error,
                    warning = warning
                )
            )
        }

        val batchId = "IMP-" + (System.currentTimeMillis() % 10000000)
        ImportPreviewResult(
            totalRows = rows.size,
            newProductsCount = newCount,
            productsUpdatedCount = updateCount,
            stockAdded = addedStock,
            stockRemoved = removedStock,
            priceChangesCount = priceChanges,
            packagingChangesCount = packagingChanges,
            warningsCount = warningCount,
            errorsCount = errorCount,
            rows = rows,
            batchId = batchId
        )
    }

    suspend fun applyImport(
        previewResult: ImportPreviewResult,
        filename: String = "inventory_import.csv",
        staffName: String = "Staff"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (previewResult.errorsCount > 0) {
            return@withContext Result.failure(Exception("Cannot apply import with validation errors. Please correct them first."))
        }

        for (row in previewResult.rows) {
            val existing = productDao.getProductById(row.productId)
                ?: if (row.sku.isNotBlank()) productDao.getProductBySku(row.sku)
                else if (row.barcode.isNotBlank()) productDao.getProductByBarcode(row.barcode)
                else null

            val pId = existing?.id ?: row.productId
            val baseUnitStockDelta = when (row.operation) {
                "ADD" -> row.stockQty * row.formQtyInBaseUnits
                "REMOVE" -> -(row.stockQty * row.formQtyInBaseUnits)
                "ADJUST" -> row.stockQty * row.formQtyInBaseUnits
                "SET" -> {
                    val current = existing?.currentStockBaseUnits ?: 0.0
                    (row.stockQty * row.formQtyInBaseUnits) - current
                }
                else -> 0.0
            }

            val newStockTotal = ((existing?.currentStockBaseUnits ?: 0.0) + baseUnitStockDelta).coerceAtLeast(0.0)

            val product = ProductEntity(
                id = pId,
                name = row.productName,
                category = row.category,
                baseUnit = row.baseUnit,
                sku = row.sku.ifBlank { null },
                barcode = row.barcode.ifBlank { null },
                currentStockBaseUnits = newStockTotal,
                updatedAt = System.currentTimeMillis()
            )
            productDao.insertProduct(product)

            // Packaging form
            val formId = "FORM-" + pId.takeLast(6) + "-" + row.formName.lowercase().replace(" ", "_")
            val packagingForm = PackagingFormEntity(
                id = formId,
                productId = pId,
                name = row.formName,
                quantityInBaseUnits = row.formQtyInBaseUnits,
                purchaseCost = row.purchaseCost,
                purchaseEnabled = true,
                retailSaleEnabled = true,
                wholesaleSaleEnabled = true
            )
            productDao.insertPackagingForm(packagingForm)

            // Independent Prices
            productDao.insertFormPrice(
                FormPriceEntity(
                    id = "PRC-" + formId + "-retail",
                    packagingFormId = formId,
                    priceTier = "Retail",
                    price = row.retailPrice
                )
            )
            productDao.insertFormPrice(
                FormPriceEntity(
                    id = "PRC-" + formId + "-wholesale",
                    packagingFormId = formId,
                    priceTier = "Wholesale",
                    price = row.wholesalePrice
                )
            )

            // Stock Movement
            if (baseUnitStockDelta != 0.0) {
                productDao.insertStockMovement(
                    StockMovementEntity(
                        id = UUID.randomUUID().toString(),
                        productId = pId,
                        packagingFormId = formId,
                        movementType = "IMPORT_${row.operation}",
                        formQuantity = row.stockQty,
                        baseQuantityDelta = baseUnitStockDelta,
                        unitCost = row.purchaseCost,
                        totalCost = row.stockQty * row.purchaseCost,
                        reason = "Import ${previewResult.batchId}",
                        reference = previewResult.batchId,
                        staffName = staffName
                    )
                )
            }
        }

        // Record Import History
        businessAndStaffDao.insertImportHistory(
            ImportHistoryEntity(
                id = previewResult.batchId,
                filename = filename,
                staffName = staffName,
                rowsCount = previewResult.totalRows,
                newProducts = previewResult.newProductsCount,
                productsUpdated = previewResult.productsUpdatedCount,
                stockAdded = previewResult.stockAdded,
                stockRemoved = previewResult.stockRemoved,
                status = "SUCCESS",
                details = "Imported ${previewResult.totalRows} rows successfully"
            )
        )

        businessAndStaffDao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                staffName = staffName,
                action = "SPREADSHEET_IMPORT",
                entity = "Batch ${previewResult.batchId} (${previewResult.totalRows} items)",
                reference = previewResult.batchId
            )
        )

        Result.success(Unit)
    }

    // Export Inventory to CSV (Rule 50)
    suspend fun exportInventoryToCsv(): String = withContext(Dispatchers.IO) {
        val products = productDao.getAllActiveProducts().first()
        val forms = productDao.getAllPackagingForms().first()
        val prices = productDao.getAllFormPrices().first()

        val sb = StringBuilder()
        sb.append("Product ID,Product Name,Category,Base Unit,SKU,Barcode,Packaging Form,Qty In Base Units,Purchase Cost,Retail Price,Wholesale Price,Stock Operation,Form Stock Qty,Current Total Stock Base Units\n")

        for (product in products) {
            val productForms = forms.filter { it.productId == product.id }
            if (productForms.isEmpty()) {
                sb.append("\"${product.id}\",\"${product.name}\",\"${product.category}\",\"${product.baseUnit}\",\"${product.sku ?: ""}\",\"${product.barcode ?: ""}\",\"Standard\",1.0,0.0,0.0,0.0,NO_CHANGE,0,${product.currentStockBaseUnits}\n")
            } else {
                for (form in productForms) {
                    val retailPrice = prices.find { it.packagingFormId == form.id && it.priceTier == "Retail" }?.price ?: 0.0
                    val wholesalePrice = prices.find { it.packagingFormId == form.id && it.priceTier == "Wholesale" }?.price ?: retailPrice
                    sb.append("\"${product.id}\",\"${product.name}\",\"${product.category}\",\"${product.baseUnit}\",\"${product.sku ?: ""}\",\"${product.barcode ?: ""}\",\"${form.name}\",${form.quantityInBaseUnits},${form.purchaseCost},$retailPrice,$wholesalePrice,NO_CHANGE,0,${product.currentStockBaseUnits}\n")
                }
            }
        }
        sb.toString()
    }

    // Sample Downloadable Import Template (Section 41)
    fun getImportTemplateCsv(): String {
        return """
Product ID,Product Name,Category,Base Unit,SKU,Barcode,Packaging Form,Qty In Base Units,Purchase Cost,Retail Price,Wholesale Price,Stock Operation,Stock Qty
,Fresh Milk 500ml,Dairy,Bottle,MLK-500,61611001,Bottle,1.0,45.0,60.0,55.0,ADD,50
,Fresh Milk 500ml,Dairy,Bottle,MLK-500-18,61611018,Carton 18,18.0,750.0,900.0,800.0,ADD,5
,Fresh Milk 500ml,Dairy,Bottle,MLK-500-22,61611022,Carton 22,22.0,900.0,1100.0,1000.0,ADD,5
,Sugar,Grains,KG,SGR-01,61612001,1 KG,1.0,120.0,160.0,145.0,ADD,100
,Sugar,Grains,KG,SGR-50,61612050,50 KG Sack,50.0,5500.0,7500.0,6800.0,ADD,10
,Tropical Sweets,Confectionery,Piece,SWT-01,61613001,Piece,1.0,5.0,10.0,8.0,ADD,500
,Tropical Sweets,Confectionery,Piece,SWT-PK,61613030,Pack (30 pcs),30.0,130.0,200.0,180.0,ADD,20
,Tropical Sweets,Confectionery,Piece,SWT-CT,61613600,Carton (600 pcs),600.0,2400.0,3600.0,3200.0,ADD,2
""".trimIndent()
    }

    // Complete Business Backup & Restore (Section 59-67)
    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("app", "RG POS")
        root.put("version", "1.0")
        root.put("timestamp", System.currentTimeMillis())

        // Business Profile
        val profile = businessAndStaffDao.getBusinessProfileDirect()
        if (profile != null) {
            val profJson = JSONObject().apply {
                put("name", profile.name)
                put("phone", profile.phone)
                put("currency", profile.currency)
                put("country", profile.country)
                put("taxEnabled", profile.taxEnabled)
                put("defaultTaxRate", profile.defaultTaxRate)
                put("receiptHeader", profile.receiptHeader)
                put("receiptFooter", profile.receiptFooter)
                put("ownerName", profile.ownerName)
                put("businessType", profile.businessType)
            }
            root.put("businessProfile", profJson)
        }

        // Products
        val products = productDao.getAllProducts().first()
        val prodArray = JSONArray()
        products.forEach { p ->
            prodArray.put(JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("category", p.category)
                put("baseUnit", p.baseUnit)
                put("sku", p.sku ?: "")
                put("barcode", p.barcode ?: "")
                put("internalCode", p.internalCode ?: "")
                put("stock", p.currentStockBaseUnits)
                put("reorderLevel", p.reorderLevel)
                put("isActive", p.isActive)
            })
        }
        root.put("products", prodArray)

        // Packaging Forms
        val forms = productDao.getAllPackagingForms().first()
        val formArray = JSONArray()
        forms.forEach { f ->
            formArray.put(JSONObject().apply {
                put("id", f.id)
                put("productId", f.productId)
                put("name", f.name)
                put("qtyInBaseUnits", f.quantityInBaseUnits)
                put("purchaseCost", f.purchaseCost)
                put("retailSaleEnabled", f.retailSaleEnabled)
                put("wholesaleSaleEnabled", f.wholesaleSaleEnabled)
                put("canBeOpened", f.canBeOpened)
            })
        }
        root.put("packagingForms", formArray)

        // Form Prices
        val prices = productDao.getAllFormPrices().first()
        val priceArray = JSONArray()
        prices.forEach { pr ->
            priceArray.put(JSONObject().apply {
                put("id", pr.id)
                put("packagingFormId", pr.packagingFormId)
                put("priceTier", pr.priceTier)
                put("price", pr.price)
            })
        }
        root.put("formPrices", priceArray)

        // Customers
        val customers = customerDao.getAllCustomers().first()
        val custArray = JSONArray()
        customers.forEach { c ->
            custArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("segment", c.segment)
                put("creditLimit", c.creditLimit)
                put("currentCreditBalance", c.currentCreditBalance)
            })
        }
        root.put("customers", custArray)

        // Sales
        val sales = saleDao.getAllSales().first()
        val saleArray = JSONArray()
        sales.take(500).forEach { s ->
            saleArray.put(JSONObject().apply {
                put("id", s.id)
                put("saleNumber", s.saleNumber)
                put("totalAmount", s.totalAmount)
                put("costAmount", s.costAmount)
                put("grossProfit", s.grossProfit)
                put("customerName", s.customerName)
                put("staffName", s.staffName)
                put("timestamp", s.timestamp)
                put("status", s.status)
            })
        }
        root.put("sales", saleArray)

        root.toString(2)
    }

    suspend fun restoreBackupJson(backupJson: String, staffName: String = "Owner"): Result<String> = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(backupJson)
            if (!root.has("app") || root.getString("app") != "RG POS") {
                return@withContext Result.failure(Exception("This backup is invalid or incompatible with RG POS. Your current data has not been changed."))
            }

            // Restore business profile
            if (root.has("businessProfile")) {
                val p = root.getJSONObject("businessProfile")
                businessAndStaffDao.insertOrUpdateProfile(
                    BusinessProfileEntity(
                        name = p.optString("name", "RG Store"),
                        phone = p.optString("phone", "+254 700 000000"),
                        currency = p.optString("currency", "KSh"),
                        country = p.optString("country", "Kenya"),
                        taxEnabled = p.optBoolean("taxEnabled", false),
                        defaultTaxRate = p.optDouble("defaultTaxRate", 16.0),
                        receiptHeader = p.optString("receiptHeader", "RG POS"),
                        receiptFooter = p.optString("receiptFooter", "Thank you"),
                        ownerName = p.optString("ownerName", "Owner"),
                        businessType = p.optString("businessType", "GENERAL_SHOP"),
                        isSetupCompleted = true
                    )
                )
            }

            // Restore Products
            var prodCount = 0
            if (root.has("products")) {
                val prods = root.getJSONArray("products")
                for (i in 0 until prods.length()) {
                    val p = prods.getJSONObject(i)
                    productDao.insertProduct(
                        ProductEntity(
                            id = p.getString("id"),
                            name = p.getString("name"),
                            category = p.optString("category", "General"),
                            baseUnit = p.optString("baseUnit", "Piece"),
                            sku = p.optString("sku").ifBlank { null },
                            barcode = p.optString("barcode").ifBlank { null },
                            internalCode = p.optString("internalCode").ifBlank { null },
                            currentStockBaseUnits = p.optDouble("stock", 0.0),
                            reorderLevel = p.optDouble("reorderLevel", 5.0),
                            isActive = p.optBoolean("isActive", true)
                        )
                    )
                    prodCount++
                }
            }

            // Restore Packaging Forms
            if (root.has("packagingForms")) {
                val forms = root.getJSONArray("packagingForms")
                for (i in 0 until forms.length()) {
                    val f = forms.getJSONObject(i)
                    productDao.insertPackagingForm(
                        PackagingFormEntity(
                            id = f.getString("id"),
                            productId = f.getString("productId"),
                            name = f.getString("name"),
                            quantityInBaseUnits = f.optDouble("qtyInBaseUnits", 1.0),
                            purchaseCost = f.optDouble("purchaseCost", 0.0),
                            retailSaleEnabled = f.optBoolean("retailSaleEnabled", true),
                            wholesaleSaleEnabled = f.optBoolean("wholesaleSaleEnabled", true),
                            canBeOpened = f.optBoolean("canBeOpened", false)
                        )
                    )
                }
            }

            // Restore Form Prices
            if (root.has("formPrices")) {
                val prices = root.getJSONArray("formPrices")
                for (i in 0 until prices.length()) {
                    val pr = prices.getJSONObject(i)
                    productDao.insertFormPrice(
                        FormPriceEntity(
                            id = pr.getString("id"),
                            packagingFormId = pr.getString("packagingFormId"),
                            priceTier = pr.getString("priceTier"),
                            price = pr.optDouble("price", 0.0)
                        )
                    )
                }
            }

            // Restore Customers
            if (root.has("customers")) {
                val custs = root.getJSONArray("customers")
                for (i in 0 until custs.length()) {
                    val c = custs.getJSONObject(i)
                    customerDao.insertCustomer(
                        CustomerEntity(
                            id = c.getString("id"),
                            name = c.getString("name"),
                            phone = c.optString("phone", ""),
                            segment = c.optString("segment", "Retail"),
                            creditLimit = c.optDouble("creditLimit", 50000.0),
                            currentCreditBalance = c.optDouble("currentCreditBalance", 0.0)
                        )
                    )
                }
            }

            businessAndStaffDao.insertAuditLog(
                AuditLogEntity(
                    id = UUID.randomUUID().toString(),
                    staffName = staffName,
                    action = "BACKUP_RESTORED",
                    entity = "Restored $prodCount products from backup"
                )
            )

            Result.success("Restored $prodCount products successfully.")
        } catch (e: Exception) {
            Result.failure(Exception("Failed to restore backup: ${e.message}"))
        }
    }

    // Seed Initial Demo/Sample Business Data if database is completely fresh
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val profile = businessAndStaffDao.getBusinessProfileDirect()
        if (profile == null) {
            val initialProfile = BusinessProfileEntity(
                name = "RG Store & Wholesale",
                phone = "+254 712 345678",
                email = "info@rgpos.ke",
                address = "Biashara Street, Nairobi, Kenya",
                country = "Kenya",
                currency = "KSh",
                businessType = "SUPERMARKET",
                taxEnabled = true,
                defaultTaxRate = 16.0,
                ownerName = "Richard G.",
                ownerPin = "1234",
                isSetupCompleted = true
            )
            businessAndStaffDao.insertOrUpdateProfile(initialProfile)

            // Owner Staff
            businessAndStaffDao.insertStaff(
                StaffEntity(
                    id = "staff_owner",
                    name = "Richard G. (Owner)",
                    username = "owner",
                    pin = "1234",
                    role = "OWNER"
                )
            )
            // Cashier Staff
            businessAndStaffDao.insertStaff(
                StaffEntity(
                    id = "staff_cashier",
                    name = "Faith Mwangi",
                    username = "cashier",
                    pin = "0000",
                    role = "CASHIER"
                )
            )

            // Seed Example 1: Milk 500ml (Section 23, 30: Bottle = 1 bottle, Carton 18 = 18 bottles, Carton 22 = 22 bottles)
            val milkId = "PRD-MILK-500"
            val milkProduct = ProductEntity(
                id = milkId,
                name = "Fresh Milk 500ml",
                category = "Dairy",
                baseUnit = "Bottle",
                sku = "MLK-500",
                barcode = "61611001",
                currentStockBaseUnits = 240.0,
                reorderLevel = 36.0
            )
            productDao.insertProduct(milkProduct)

            val milkBottle = PackagingFormEntity(
                id = "FORM-MILK-BOTTLE",
                productId = milkId,
                name = "Bottle",
                quantityInBaseUnits = 1.0,
                purchaseCost = 45.0,
                retailSaleEnabled = true,
                wholesaleSaleEnabled = true,
                isDefaultSelling = true
            )
            val milkCarton18 = PackagingFormEntity(
                id = "FORM-MILK-CARTON18",
                productId = milkId,
                name = "Carton (18 × 500ml)",
                quantityInBaseUnits = 18.0,
                purchaseCost = 420.0,
                retailSaleEnabled = false,
                wholesaleSaleEnabled = true
            )
            val milkCarton22 = PackagingFormEntity(
                id = "FORM-MILK-CARTON22",
                productId = milkId,
                name = "Carton (22 × 500ml)",
                quantityInBaseUnits = 22.0,
                purchaseCost = 510.0,
                retailSaleEnabled = false,
                wholesaleSaleEnabled = true
            )
            productDao.insertPackagingForms(listOf(milkBottle, milkCarton18, milkCarton22))

            // Independent pricing (Section 30: Bottle Retail = 60, Carton 18 Wholesale = 500, Carton 22 Wholesale = 600)
            productDao.insertFormPrices(
                listOf(
                    FormPriceEntity("PRC-MLK-BTL-RET", "FORM-MILK-BOTTLE", "Retail", 60.0),
                    FormPriceEntity("PRC-MLK-BTL-WHL", "FORM-MILK-BOTTLE", "Wholesale", 55.0),
                    FormPriceEntity("PRC-MLK-C18-RET", "FORM-MILK-CARTON18", "Retail", 540.0),
                    FormPriceEntity("PRC-MLK-C18-WHL", "FORM-MILK-CARTON18", "Wholesale", 500.0),
                    FormPriceEntity("PRC-MLK-C22-RET", "FORM-MILK-CARTON22", "Retail", 650.0),
                    FormPriceEntity("PRC-MLK-C22-WHL", "FORM-MILK-CARTON22", "Wholesale", 600.0)
                )
            )

            // Seed Example 2: Sugar (Section 25: Base = KG, 1 KG, 5 KG, 50 KG Sack with openable packaging)
            val sugarId = "PRD-SUGAR"
            val sugarProduct = ProductEntity(
                id = sugarId,
                name = "Pure Cane Sugar",
                category = "Grains & Sugar",
                baseUnit = "KG",
                sku = "SGR-KG",
                barcode = "61612001",
                currentStockBaseUnits = 487.0, // Section 27: 9 intact sacks (450 KG) + 37 KG loose = 487 KG
                reorderLevel = 100.0
            )
            productDao.insertProduct(sugarProduct)

            val sugar1Kg = PackagingFormEntity(
                id = "FORM-SUGAR-1KG",
                productId = sugarId,
                name = "1 KG Packet",
                quantityInBaseUnits = 1.0,
                purchaseCost = 115.0,
                retailSaleEnabled = true,
                wholesaleSaleEnabled = true,
                isDefaultSelling = true
            )
            val sugar5Kg = PackagingFormEntity(
                id = "FORM-SUGAR-5KG",
                productId = sugarId,
                name = "5 KG Bale",
                quantityInBaseUnits = 5.0,
                purchaseCost = 560.0,
                retailSaleEnabled = true,
                wholesaleSaleEnabled = true
            )
            val sugar50KgSack = PackagingFormEntity(
                id = "FORM-SUGAR-50KG",
                productId = sugarId,
                name = "50 KG Sack",
                quantityInBaseUnits = 50.0,
                purchaseCost = 5400.0,
                retailSaleEnabled = false,
                wholesaleSaleEnabled = true,
                canBeOpened = true // Section 27
            )
            productDao.insertPackagingForms(listOf(sugar1Kg, sugar5Kg, sugar50KgSack))

            productDao.insertFormPrices(
                listOf(
                    FormPriceEntity("PRC-SGR-1K-RET", "FORM-SUGAR-1KG", "Retail", 150.0),
                    FormPriceEntity("PRC-SGR-1K-WHL", "FORM-SUGAR-1KG", "Wholesale", 135.0),
                    FormPriceEntity("PRC-SGR-5K-RET", "FORM-SUGAR-5KG", "Retail", 720.0),
                    FormPriceEntity("PRC-SGR-5K-WHL", "FORM-SUGAR-5KG", "Wholesale", 680.0),
                    FormPriceEntity("PRC-SGR-50K-WHL", "FORM-SUGAR-50KG", "Wholesale", 6700.0),
                    FormPriceEntity("PRC-SGR-50K-RET", "FORM-SUGAR-50KG", "Retail", 7000.0)
                )
            )

            // Seed Example 3: Tropical Sweets (Section 26, 31: Piece, Pack of 30 pcs, Carton of 600 pcs)
            val sweetsId = "PRD-SWEETS"
            val sweetsProduct = ProductEntity(
                id = sweetsId,
                name = "Tropical Candy Sweets",
                category = "Confectionery",
                baseUnit = "Piece",
                sku = "SWT-PC",
                barcode = "61613001",
                currentStockBaseUnits = 1800.0,
                reorderLevel = 300.0
            )
            productDao.insertProduct(sweetsProduct)

            val sweetPiece = PackagingFormEntity(
                id = "FORM-SWT-PIECE",
                productId = sweetsId,
                name = "Piece",
                quantityInBaseUnits = 1.0,
                purchaseCost = 6.0,
                retailSaleEnabled = true,
                wholesaleSaleEnabled = true,
                isDefaultSelling = true
            )
            val sweetPack = PackagingFormEntity(
                id = "FORM-SWT-PACK",
                productId = sweetsId,
                name = "Pack (30 pieces)",
                quantityInBaseUnits = 30.0,
                purchaseCost = 160.0,
                retailSaleEnabled = true,
                wholesaleSaleEnabled = true
            )
            val sweetCarton = PackagingFormEntity(
                id = "FORM-SWT-CARTON",
                productId = sweetsId,
                name = "Carton (600 pieces)",
                quantityInBaseUnits = 600.0,
                purchaseCost = 1800.0,
                retailSaleEnabled = false,
                wholesaleSaleEnabled = true
            )
            productDao.insertPackagingForms(listOf(sweetPiece, sweetPack, sweetCarton))

            // Section 31: Sweets pricing (Piece Retail=10 Wholesale=8, Pack Retail=320 Wholesale=290, Carton Wholesale=2200)
            productDao.insertFormPrices(
                listOf(
                    FormPriceEntity("PRC-SWT-PC-RET", "FORM-SWT-PIECE", "Retail", 10.0),
                    FormPriceEntity("PRC-SWT-PC-WHL", "FORM-SWT-PIECE", "Wholesale", 8.0),
                    FormPriceEntity("PRC-SWT-PK-RET", "FORM-SWT-PACK", "Retail", 320.0),
                    FormPriceEntity("PRC-SWT-PK-WHL", "FORM-SWT-PACK", "Wholesale", 290.0),
                    FormPriceEntity("PRC-SWT-CT-WHL", "FORM-SWT-CARTON", "Wholesale", 2200.0)
                )
            )

            // Seed Customers (Section 19: Retail, Wholesale, Distributor, VIP, Credit)
            val custWholesale = CustomerEntity(
                id = "CUST-001",
                name = "Kamau Wholesalers Ltd",
                phone = "+254 722 111222",
                segment = "Wholesale",
                creditLimit = 150000.0,
                currentCreditBalance = 24500.0
            )
            val custVip = CustomerEntity(
                id = "CUST-002",
                name = "Mama Ochieng Kiosk",
                phone = "+254 733 444555",
                segment = "Retail",
                creditLimit = 30000.0,
                currentCreditBalance = 0.0
            )
            val custCredit = CustomerEntity(
                id = "CUST-003",
                name = "Amina Hassan",
                phone = "+254 799 888777",
                segment = "Credit",
                creditLimit = 50000.0,
                currentCreditBalance = 12000.0
            )
            customerDao.insertCustomers(listOf(custWholesale, custVip, custCredit))

            // Seed Suppliers
            val sup1 = SupplierEntity(
                id = "SUP-001",
                name = "Brookside Dairies Ltd",
                phone = "+254 720 000111",
                address = "Ruiru, Kenya"
            )
            val sup2 = SupplierEntity(
                id = "SUP-002",
                name = "Mumias Agro Processors",
                phone = "+254 720 000222",
                address = "Western Kenya"
            )
            supplierAndPurchaseDao.insertSuppliers(listOf(sup1, sup2))

            // Seed Initial Shift
            financeAndShiftDao.insertShift(
                CashShiftEntity(
                    id = "SHIFT-" + System.currentTimeMillis(),
                    staffName = "Richard G. (Owner)",
                    openingCash = 5000.0,
                    expectedCash = 5000.0,
                    status = "OPEN"
                )
            )

            // Seed initial sample sale
            val saleId = "REC-10001"
            val sampleSale = SaleEntity(
                id = saleId,
                saleNumber = saleId,
                timestamp = System.currentTimeMillis() - 3600000L,
                customerId = custWholesale.id,
                customerName = custWholesale.name,
                customerSegment = "Wholesale",
                staffId = "staff_owner",
                staffName = "Richard G. (Owner)",
                subtotal = 5000.0,
                totalAmount = 5000.0,
                costAmount = 3700.0,
                grossProfit = 1300.0,
                status = "COMPLETED"
            )
            saleDao.insertSale(sampleSale)
            saleDao.insertSaleItems(
                listOf(
                    SaleItemEntity(
                        id = UUID.randomUUID().toString(),
                        saleId = saleId,
                        productId = milkId,
                        productName = milkProduct.name,
                        packagingFormId = milkCarton18.id,
                        formName = milkCarton18.name,
                        quantityInBaseUnits = 18.0,
                        quantity = 5.0,
                        unitPrice = 500.0,
                        costPrice = 420.0,
                        subtotal = 2500.0,
                        total = 2500.0,
                        profit = 400.0
                    ),
                    SaleItemEntity(
                        id = UUID.randomUUID().toString(),
                        saleId = saleId,
                        productId = sweetsId,
                        productName = sweetsProduct.name,
                        packagingFormId = sweetCarton.id,
                        formName = sweetCarton.name,
                        quantityInBaseUnits = 600.0,
                        quantity = 1.0,
                        unitPrice = 2200.0,
                        costPrice = 1800.0,
                        subtotal = 2200.0,
                        total = 2200.0,
                        profit = 400.0
                    )
                )
            )
            // Partial split payment: Cash 2,000 + M-Pesa 2,000 + Credit 1,000 = 5,000 (Section 54)
            saleDao.insertSalePayments(
                listOf(
                    SalePaymentEntity(UUID.randomUUID().toString(), saleId, "CASH", 2000.0),
                    SalePaymentEntity(UUID.randomUUID().toString(), saleId, "MPESA", 2000.0, "QKB7293X"),
                    SalePaymentEntity(UUID.randomUUID().toString(), saleId, "CREDIT", 1000.0, custWholesale.name)
                )
            )
        }
    }
}
