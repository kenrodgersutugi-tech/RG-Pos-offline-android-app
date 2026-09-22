package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.AppDatabase
import com.example.data.repository.PosRepository
import com.example.ui.MainViewModel
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.FloatingPillNavBar
import com.example.ui.components.ReceiptDialog
import com.example.ui.home.HomeScreen
import com.example.ui.inventory.InventoryScreen
import com.example.ui.more.MoreScreen
import com.example.ui.pos.PosScreen
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PosRepository(database)

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MainViewModel(repository) as T
                        }
                    }
                )

                PosApp(viewModel)
            }
        }
    }
}

@Composable
fun PosApp(viewModel: MainViewModel) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val moreSubScreen by viewModel.moreSubScreen.collectAsStateWithLifecycle()
    val businessProfile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val currentShift by viewModel.currentOpenShift.collectAsStateWithLifecycle()
    val dashboardStats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val productsWithForms by viewModel.productsWithForms.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val heldSales by viewModel.heldSales.collectAsStateWithLifecycle()
    val completedSale by viewModel.completedSale.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val staffList by viewModel.staffList.collectAsStateWithLifecycle()
    val recentStockMovements by viewModel.recentStockMovements.collectAsStateWithLifecycle()
    val recentAuditLogs by viewModel.recentAuditLogs.collectAsStateWithLifecycle()
    val allSales by viewModel.allSales.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val importPreview by viewModel.importPreview.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val reportFilter by viewModel.reportFilter.collectAsStateWithLifecycle()

    val posSearchQuery by viewModel.posSearchQuery.collectAsStateWithLifecycle()
    val posCategory by viewModel.posCategory.collectAsStateWithLifecycle()
    val posPriceTier by viewModel.posPriceTier.collectAsStateWithLifecycle()
    val posSelectedCustomer by viewModel.posSelectedCustomer.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showStartShiftDialog by remember { mutableStateOf(false) }

    // Show snackbars automatically when ViewModel posts messages
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    val currency = businessProfile?.currency ?: "KSh"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Slate950,
        contentWindowInsets = WindowInsets.statusBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            // Floating Pill Navigation Bar (Section 7)
            FloatingPillNavBar(
                currentDestination = currentDestination,
                onNavigate = { viewModel.navigateTo(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate950)
        ) {
            when (currentDestination) {
                BottomNavDestination.HOME -> {
                    HomeScreen(
                        stats = dashboardStats,
                        businessProfile = businessProfile,
                        shift = currentShift,
                        recentSales = allSales,
                        onNavigate = { viewModel.navigateTo(it) },
                        onStartShift = { showStartShiftDialog = true }
                    )
                }
                BottomNavDestination.POS -> {
                    PosScreen(
                        productsWithForms = productsWithForms,
                        categories = categories,
                        cartItems = cartItems,
                        customers = customers,
                        selectedCustomer = posSelectedCustomer,
                        priceTier = posPriceTier,
                        searchQuery = posSearchQuery,
                        selectedCategory = posCategory,
                        heldSalesCount = heldSales.size,
                        currency = currency,
                        onSearchChange = { viewModel.setPosSearch(it) },
                        onCategoryChange = { viewModel.setPosCategory(it) },
                        onPriceTierChange = { viewModel.setPosPriceTier(it) },
                        onSelectCustomer = { viewModel.setPosCustomer(it) },
                        onAddToCart = { productWithForms, form, qty ->
                            viewModel.addToCart(productWithForms.product, form, qty)
                        },
                        onUpdateQuantity = { pId, fId, qty -> viewModel.updateCartItemQuantity(pId, fId, qty) },
                        onUpdatePrice = { pId, fId, price -> viewModel.updateCartItemPrice(pId, fId, price) },
                        onRemoveItem = { pId, fId -> viewModel.removeCartItem(pId, fId) },
                        onClearCart = { viewModel.clearCart() },
                        onHoldSale = { viewModel.holdCurrentSale() },
                        onResumeSaleClick = { viewModel.resumeSale(0) },
                        onCompleteCheckout = { split -> viewModel.completeCheckout(split) }
                    )
                }
                BottomNavDestination.INVENTORY -> {
                    InventoryScreen(
                        productsWithForms = productsWithForms,
                        recentMovements = recentStockMovements,
                        currency = currency,
                        onSaveProduct = { p, forms -> viewModel.saveProduct(p, forms) },
                        onAddStock = { pId, fId, qty, cost, sup, notes ->
                            viewModel.addStock(pId, fId, qty, cost, sup, notes)
                        },
                        onOpenPackage = { pId, fId, qty -> viewModel.openPackage(pId, fId, qty) },
                        onDeactivateProduct = { pId -> viewModel.deactivateProduct(pId) }
                    )
                }
                BottomNavDestination.MORE -> {
                    MoreScreen(
                        currentSubScreen = moreSubScreen,
                        businessProfile = businessProfile,
                        shift = currentShift,
                        customers = customers,
                        suppliers = suppliers,
                        expenses = expenses,
                        staffList = staffList,
                        sales = allSales,
                        recentAuditLogs = recentAuditLogs,
                        reportFilter = reportFilter,
                        importPreview = importPreview,
                        isImporting = isImporting,
                        onNavigateSub = { viewModel.navigateMoreSubScreen(it) },
                        onUpdateBusinessProfile = { viewModel.updateBusinessProfile(it) },
                        onStartShift = { floatVal -> viewModel.startShift(floatVal) },
                        onCloseShift = { counted, notes -> viewModel.closeShift(counted, notes) },
                        onSaveCustomer = { viewModel.saveCustomer(it) },
                        onPayCredit = { cId, amt, method, ref -> viewModel.payCredit(cId, amt, method, ref) },
                        onSaveSupplier = { viewModel.saveSupplier(it) },
                        onAddExpense = { viewModel.addExpense(it) },
                        onSaveStaff = { viewModel.saveStaff(it) },
                        onParseCsv = { csv -> viewModel.parseCsvForPreview(csv) },
                        onApplyImport = { viewModel.applyImportPreview() },
                        onCancelImport = { viewModel.cancelImportPreview() },
                        getTemplateCsv = { viewModel.getImportTemplateCsv() },
                        onExportCsv = { viewModel.exportInventoryCsv() },
                        onCreateBackupJson = { viewModel.createBackupJson() },
                        onRestoreBackupJson = { json -> viewModel.restoreBackupJson(json) },
                        onUpdateReportFilter = { viewModel.updateReportFilter(it) },
                        onClearReportFilter = { viewModel.clearReportFilter() }
                    )
                }
            }
        }
    }

    // Modal: Thermal Receipt Dialog (Section 17, 18)
    if (completedSale != null) {
        val (sale, items, payments) = completedSale!!
        ReceiptDialog(
            sale = sale,
            items = items,
            payments = payments,
            businessProfile = businessProfile ?: com.example.data.local.entity.BusinessProfileEntity(
                id = "default",
                name = "RG POS Retail & Wholesale",
                businessType = "Retail & Wholesale",
                phone = "+254 700 000000",
                address = "Biashara Street, Nairobi",
                currency = "KSh"
            ),
            onDismiss = { viewModel.dismissReceipt() },
            onPrintSuccess = {
                viewModel.showSnackbar("Receipt printed / shared successfully!")
            }
        )
    }

    // Modal: Start Shift Dialog
    if (showStartShiftDialog) {
        var openingCashText by remember { mutableStateOf("2000") }

        Dialog(onDismissRequest = { showStartShiftDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = androidx.compose.foundation.BorderStroke(1.dp, CyanPrimary)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Start Cash Shift",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate100
                    )
                    Text(
                        text = "Enter opening cash drawer balance to track discrepancies.",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.Slate400
                    )

                    OutlinedTextField(
                        value = openingCashText,
                        onValueChange = { openingCashText = it },
                        label = { Text("Opening Cash ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Button(
                        onClick = {
                            val opening = openingCashText.toDoubleOrNull() ?: 0.0
                            viewModel.startShift(opening)
                            showStartShiftDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                    ) {
                        Text("Start Shift", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

