package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.*
import com.example.data.repository.PosRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class MoreSubScreen {
    MAIN_MENU,
    REPORTS,
    CUSTOMERS,
    SUPPLIERS,
    EXPENSES,
    CASH_SHIFT,
    STAFF,
    IMPORT_EXPORT,
    BACKUP_RESTORE,
    AUDIT_LOG,
    SETTINGS
}

class MainViewModel(private val repository: PosRepository) : ViewModel() {

    // Navigation
    private val _currentDestination = MutableStateFlow(com.example.ui.components.BottomNavDestination.HOME)
    val currentDestination: StateFlow<com.example.ui.components.BottomNavDestination> = _currentDestination.asStateFlow()

    private val _moreSubScreen = MutableStateFlow(MoreSubScreen.MAIN_MENU)
    val moreSubScreen: StateFlow<MoreSubScreen> = _moreSubScreen.asStateFlow()

    // Business Profile
    val businessProfile: StateFlow<BusinessProfileEntity?> = repository.businessProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Staff User
    private val _currentStaff = MutableStateFlow<StaffEntity?>(null)
    val currentStaff: StateFlow<StaffEntity?> = _currentStaff.asStateFlow()

    // Products & Categories
    val productsWithForms: StateFlow<List<ProductWithForms>> = repository.allProductsWithForms
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard Stats
    val dashboardStats: StateFlow<DashboardStats> = repository.getDashboardStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // Current Open Shift
    val currentOpenShift: StateFlow<CashShiftEntity?> = repository.currentOpenShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // POS State
    private val _posSearchQuery = MutableStateFlow("")
    val posSearchQuery: StateFlow<String> = _posSearchQuery.asStateFlow()

    private val _posCategory = MutableStateFlow("All")
    val posCategory: StateFlow<String> = _posCategory.asStateFlow()

    private val _posPriceTier = MutableStateFlow("Retail")
    val posPriceTier: StateFlow<String> = _posPriceTier.asStateFlow()

    private val _posSelectedCustomer = MutableStateFlow<CustomerEntity?>(null)
    val posSelectedCustomer: StateFlow<CustomerEntity?> = _posSelectedCustomer.asStateFlow()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _heldSales = MutableStateFlow<List<Pair<String, List<CartItem>>>>(emptyList())
    val heldSales: StateFlow<List<Pair<String, List<CartItem>>>> = _heldSales.asStateFlow()

    // Active Receipt Modal
    private val _completedSale = MutableStateFlow<Triple<SaleEntity, List<SaleItemEntity>, List<SalePaymentEntity>>?>(null)
    val completedSale: StateFlow<Triple<SaleEntity, List<SaleItemEntity>, List<SalePaymentEntity>>?> = _completedSale.asStateFlow()

    // Customers, Suppliers, Expenses, Staff
    val customers: StateFlow<List<CustomerEntity>> = repository.allCustomers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val suppliers: StateFlow<List<SupplierEntity>> = repository.allSuppliers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val staffList: StateFlow<List<StaffEntity>> = repository.allStaff
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentAuditLogs: StateFlow<List<AuditLogEntity>> = repository.recentAuditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentStockMovements: StateFlow<List<StockMovementEntity>> = repository.recentStockMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSales: StateFlow<List<SaleEntity>> = repository.allSales
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Feedback messages
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Import Preview State
    private val _importPreview = MutableStateFlow<ImportPreviewResult?>(null)
    val importPreview: StateFlow<ImportPreviewResult?> = _importPreview.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Reports Filter State
    private val _reportFilter = MutableStateFlow(ReportFilter())
    val reportFilter: StateFlow<ReportFilter> = _reportFilter.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            // Set current staff to owner initially if not set
            val all = repository.allStaff.first()
            if (all.isNotEmpty()) {
                _currentStaff.value = all.first()
            }
        }
    }

    fun navigateTo(destination: com.example.ui.components.BottomNavDestination) {
        _currentDestination.value = destination
    }

    fun navigateMoreSubScreen(subScreen: MoreSubScreen) {
        _moreSubScreen.value = subScreen
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun setStaff(staff: StaffEntity) {
        _currentStaff.value = staff
    }

    // POS Methods
    fun setPosSearch(query: String) {
        _posSearchQuery.value = query
    }

    fun setPosCategory(category: String) {
        _posCategory.value = category
    }

    fun setPosPriceTier(tier: String) {
        _posPriceTier.value = tier
    }

    fun setPosCustomer(customer: CustomerEntity?) {
        _posSelectedCustomer.value = customer
        if (customer != null && customer.segment.equals("Wholesale", ignoreCase = true)) {
            _posPriceTier.value = "Wholesale"
        }
    }

    fun addToCart(product: ProductEntity, formWithPrices: PackagingFormWithPrices, quantity: Double = 1.0) {
        val form = formWithPrices.form
        val unitPrice = formWithPrices.getPriceForTier(_posPriceTier.value)
        val currentList = _cartItems.value.toMutableList()

        val existingIndex = currentList.indexOfFirst { it.productId == product.id && it.formId == form.id }
        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        } else {
            currentList.add(
                CartItem(
                    productId = product.id,
                    formId = form.id,
                    productName = product.name,
                    formName = form.name,
                    quantityInBaseUnits = form.quantityInBaseUnits,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    costPrice = form.purchaseCost,
                    discount = 0.0
                )
            )
        }
        _cartItems.value = currentList
    }

    fun updateCartItemQuantity(productId: String, formId: String, newQuantity: Double) {
        if (newQuantity <= 0) {
            removeCartItem(productId, formId)
            return
        }
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId && it.formId == formId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(quantity = newQuantity)
            _cartItems.value = currentList
        }
    }

    fun updateCartItemPrice(productId: String, formId: String, newPrice: Double) {
        val currentList = _cartItems.value.toMutableList()
        val index = currentList.indexOfFirst { it.productId == productId && it.formId == formId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(unitPrice = newPrice)
            _cartItems.value = currentList
        }
    }

    fun removeCartItem(productId: String, formId: String) {
        _cartItems.value = _cartItems.value.filterNot { it.productId == productId && it.formId == formId }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _posSelectedCustomer.value = null
    }

    fun holdCurrentSale() {
        if (_cartItems.value.isEmpty()) return
        val current = _cartItems.value
        val label = "Held #" + (_heldSales.value.size + 1) + " - " + (_posSelectedCustomer.value?.name ?: "Walk-in")
        _heldSales.value = _heldSales.value + (label to current)
        _cartItems.value = emptyList()
        _posSelectedCustomer.value = null
        showSnackbar("Sale held successfully")
    }

    fun resumeSale(index: Int) {
        val held = _heldSales.value
        if (index in held.indices) {
            val item = held[index]
            _cartItems.value = item.second
            _heldSales.value = held.filterIndexed { i, _ -> i != index }
            showSnackbar("Sale resumed")
        }
    }

    fun completeCheckout(paymentSplit: PaymentSplit, notes: String = "") {
        viewModelScope.launch {
            val staff = _currentStaff.value ?: StaffEntity("staff_default", "Cashier", "cashier", "0000", "CASHIER")
            val segment = _posSelectedCustomer.value?.segment ?: "Walk-in"

            val result = repository.completeSale(
                cartItems = _cartItems.value,
                paymentSplit = paymentSplit,
                staffId = staff.id,
                staffName = staff.name,
                customerSegment = segment,
                notes = notes
            )

            result.onSuccess { sale ->
                val details = repository.getSaleDetails(sale.id)
                if (details.first != null) {
                    _completedSale.value = Triple(details.first!!, details.second, details.third)
                }
                _cartItems.value = emptyList()
                _posSelectedCustomer.value = null
                showSnackbar("Sale ${sale.saleNumber} completed!")
            }.onFailure { error ->
                showSnackbar("Error: ${error.message}")
            }
        }
    }

    fun dismissReceipt() {
        _completedSale.value = null
    }

    // Inventory operations
    fun addStock(
        productId: String,
        packagingFormId: String,
        quantity: Double,
        cost: Double,
        supplierName: String = "",
        reason: String = "Stock Purchase"
    ) {
        viewModelScope.launch {
            val staffName = _currentStaff.value?.name ?: "Staff"
            val result = repository.addStock(productId, packagingFormId, quantity, cost, supplierName, reason, staffName)
            result.onSuccess {
                showSnackbar("Stock added successfully!")
            }.onFailure {
                showSnackbar("Error adding stock: ${it.message}")
            }
        }
    }

    fun openPackage(productId: String, packagingFormId: String, quantity: Double) {
        viewModelScope.launch {
            val staffName = _currentStaff.value?.name ?: "Staff"
            val result = repository.openPackaging(productId, packagingFormId, quantity, staffName)
            result.onSuccess {
                showSnackbar("Package opened into loose stock successfully!")
            }.onFailure {
                showSnackbar("Error: ${it.message}")
            }
        }
    }

    fun saveProduct(product: ProductEntity, formsWithPrices: List<PackagingFormWithPrices>) {
        viewModelScope.launch {
            val staffName = _currentStaff.value?.name ?: "Staff"
            repository.saveProductWithForms(product, formsWithPrices, staffName)
            showSnackbar("Product saved: ${product.name}")
        }
    }

    fun deactivateProduct(productId: String) {
        viewModelScope.launch {
            val staffName = _currentStaff.value?.name ?: "Staff"
            repository.deactivateProduct(productId, staffName)
            showSnackbar("Product deactivated")
        }
    }

    // Customer operations
    fun saveCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showSnackbar("Customer saved: ${customer.name}")
        }
    }

    fun payCredit(customerId: String, amount: Double, method: String, reference: String) {
        viewModelScope.launch {
            val staffName = _currentStaff.value?.name ?: "Staff"
            val result = repository.recordCreditPayment(customerId, amount, method, reference, staffName)
            result.onSuccess {
                showSnackbar("Credit payment of KSh $amount recorded!")
            }.onFailure {
                showSnackbar("Error: ${it.message}")
            }
        }
    }

    // Supplier operations
    fun saveSupplier(supplier: SupplierEntity) {
        viewModelScope.launch {
            repository.saveSupplier(supplier)
            showSnackbar("Supplier saved: ${supplier.name}")
        }
    }

    // Expense operations
    fun addExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            val staffName = _currentStaff.value?.name ?: "Staff"
            repository.addExpense(expense, staffName)
            showSnackbar("Expense of ${expense.amount} recorded")
        }
    }

    // Shift operations
    fun startShift(openingCash: Double) {
        viewModelScope.launch {
            val staffName = _currentStaff.value?.name ?: "Staff"
            repository.startShift(staffName, openingCash)
            showSnackbar("Shift started with KSh $openingCash")
        }
    }

    fun closeShift(closingCash: Double, notes: String = "") {
        viewModelScope.launch {
            val current = currentOpenShift.value
            if (current != null) {
                repository.closeShift(current, closingCash, notes)
                showSnackbar("Shift closed. Variance: KSh ${closingCash - current.expectedCash}")
            }
        }
    }

    // Staff operations
    fun saveStaff(staff: StaffEntity) {
        viewModelScope.launch {
            repository.saveStaff(staff)
            showSnackbar("Staff member saved: ${staff.name}")
        }
    }

    // Business Profile update
    fun updateBusinessProfile(profile: BusinessProfileEntity) {
        viewModelScope.launch {
            repository.saveBusinessProfile(profile)
            showSnackbar("Business profile updated!")
        }
    }

    // Import / Export
    fun parseCsvForPreview(csvText: String) {
        viewModelScope.launch {
            _isImporting.value = true
            val preview = repository.previewImport(csvText)
            _importPreview.value = preview
            _isImporting.value = false
        }
    }

    fun applyImportPreview() {
        val preview = _importPreview.value ?: return
        viewModelScope.launch {
            _isImporting.value = true
            val staffName = _currentStaff.value?.name ?: "Staff"
            val result = repository.applyImport(preview, staffName = staffName)
            _isImporting.value = false
            result.onSuccess {
                showSnackbar("Import applied: ${preview.totalRows} items processed!")
                _importPreview.value = null
            }.onFailure {
                showSnackbar("Import error: ${it.message}")
            }
        }
    }

    fun cancelImportPreview() {
        _importPreview.value = null
    }

    fun getImportTemplateCsv(): String {
        return repository.getImportTemplateCsv()
    }

    suspend fun exportInventoryCsv(): String {
        return repository.exportInventoryToCsv()
    }

    // Backup & Restore
    suspend fun createBackupJson(): String {
        return repository.createBackupJson()
    }

    fun restoreBackupJson(jsonString: String) {
        viewModelScope.launch {
            val staffName = _currentStaff.value?.name ?: "Owner"
            val result = repository.restoreBackupJson(jsonString, staffName)
            result.onSuccess { msg ->
                showSnackbar(msg)
            }.onFailure { err ->
                showSnackbar(err.message ?: "Restore failed")
            }
        }
    }

    // Reports filter
    fun updateReportFilter(newFilter: ReportFilter) {
        _reportFilter.value = newFilter
    }

    fun clearReportFilter() {
        _reportFilter.value = ReportFilter()
    }
}
