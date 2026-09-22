package com.example.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.*
import com.example.domain.model.DateRangeFilter
import com.example.domain.model.ImportPreviewResult
import com.example.domain.model.ReportFilter
import com.example.ui.MoreSubScreen
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatMoney
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun MoreScreen(
    currentSubScreen: MoreSubScreen,
    businessProfile: BusinessProfileEntity?,
    shift: CashShiftEntity?,
    customers: List<CustomerEntity>,
    suppliers: List<SupplierEntity>,
    expenses: List<ExpenseEntity>,
    staffList: List<StaffEntity>,
    sales: List<SaleEntity>,
    recentAuditLogs: List<AuditLogEntity>,
    reportFilter: ReportFilter,
    importPreview: ImportPreviewResult?,
    isImporting: Boolean,
    onNavigateSub: (MoreSubScreen) -> Unit,
    onUpdateBusinessProfile: (BusinessProfileEntity) -> Unit,
    onStartShift: (Double) -> Unit,
    onCloseShift: (Double, String) -> Unit,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onPayCredit: (String, Double, String, String) -> Unit,
    onSaveSupplier: (SupplierEntity) -> Unit,
    onAddExpense: (ExpenseEntity) -> Unit,
    onSaveStaff: (StaffEntity) -> Unit,
    onParseCsv: (String) -> Unit,
    onApplyImport: () -> Unit,
    onCancelImport: () -> Unit,
    getTemplateCsv: () -> String,
    onExportCsv: suspend () -> String,
    onCreateBackupJson: suspend () -> String,
    onRestoreBackupJson: (String) -> Unit,
    onUpdateReportFilter: (ReportFilter) -> Unit,
    onClearReportFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = businessProfile?.currency ?: "KSh"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(bottom = 90.dp) // space for floating pill
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentSubScreen != MoreSubScreen.MAIN_MENU) {
                IconButton(onClick = { onNavigateSub(MoreSubScreen.MAIN_MENU) }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = CyanPrimary)
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = when (currentSubScreen) {
                    MoreSubScreen.MAIN_MENU -> "Business Management"
                    MoreSubScreen.REPORTS -> "Reports & Analytics"
                    MoreSubScreen.CUSTOMERS -> "Customers & Credit"
                    MoreSubScreen.SUPPLIERS -> "Suppliers"
                    MoreSubScreen.EXPENSES -> "Expenses"
                    MoreSubScreen.CASH_SHIFT -> "Cash Shift Management"
                    MoreSubScreen.STAFF -> "Staff & Permissions"
                    MoreSubScreen.IMPORT_EXPORT -> "Import / Export CSV"
                    MoreSubScreen.BACKUP_RESTORE -> "Backup & Restore"
                    MoreSubScreen.AUDIT_LOG -> "System Audit Log"
                    MoreSubScreen.SETTINGS -> "Business Profile & Settings"
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Slate100
            )
        }

        // Subscreen body
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentSubScreen) {
                MoreSubScreen.MAIN_MENU -> {
                    MoreMainMenu(
                        businessProfile = businessProfile,
                        onNavigate = onNavigateSub
                    )
                }
                MoreSubScreen.REPORTS -> {
                    ReportsScreen(
                        sales = sales,
                        filter = reportFilter,
                        currency = currency,
                        onUpdateFilter = onUpdateReportFilter,
                        onClearFilter = onClearReportFilter
                    )
                }
                MoreSubScreen.CUSTOMERS -> {
                    CustomersScreen(
                        customers = customers,
                        currency = currency,
                        onSaveCustomer = onSaveCustomer,
                        onPayCredit = onPayCredit
                    )
                }
                MoreSubScreen.SUPPLIERS -> {
                    SuppliersScreen(
                        suppliers = suppliers,
                        currency = currency,
                        onSaveSupplier = onSaveSupplier
                    )
                }
                MoreSubScreen.EXPENSES -> {
                    ExpensesScreen(
                        expenses = expenses,
                        currency = currency,
                        onAddExpense = onAddExpense
                    )
                }
                MoreSubScreen.CASH_SHIFT -> {
                    CashShiftScreen(
                        shift = shift,
                        currency = currency,
                        onStartShift = onStartShift,
                        onCloseShift = onCloseShift
                    )
                }
                MoreSubScreen.STAFF -> {
                    StaffScreen(
                        staffList = staffList,
                        onSaveStaff = onSaveStaff
                    )
                }
                MoreSubScreen.IMPORT_EXPORT -> {
                    ImportExportScreen(
                        importPreview = importPreview,
                        isImporting = isImporting,
                        onParseCsv = onParseCsv,
                        onApplyImport = onApplyImport,
                        onCancelImport = onCancelImport,
                        getTemplateCsv = getTemplateCsv,
                        onExportCsv = onExportCsv
                    )
                }
                MoreSubScreen.BACKUP_RESTORE -> {
                    BackupRestoreScreen(
                        onCreateBackup = onCreateBackupJson,
                        onRestoreBackup = onRestoreBackupJson
                    )
                }
                MoreSubScreen.AUDIT_LOG -> {
                    AuditLogScreen(recentAuditLogs = recentAuditLogs)
                }
                MoreSubScreen.SETTINGS -> {
                    SettingsScreen(
                        businessProfile = businessProfile ?: BusinessProfileEntity(
                            id = "default",
                            name = "RG POS Retail & Wholesale",
                            businessType = "Retail & Wholesale",
                            phone = "+254 700 000000",
                            address = "Biashara Street, Nairobi",
                            currency = "KSh"
                        ),
                        onSave = onUpdateBusinessProfile
                    )
                }
            }
        }
    }
}

@Composable
fun MoreMainMenu(
    businessProfile: BusinessProfileEntity?,
    onNavigate: (MoreSubScreen) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(title = "OPERATIONS & FINANCE")
        }

        item {
            MoreMenuCard(
                title = "Reports & Analytics",
                subtitle = "Sales summary, profits, staff, date filters & CSV export",
                icon = Icons.Default.Assessment,
                iconTint = CyanPrimary,
                onClick = { onNavigate(MoreSubScreen.REPORTS) }
            )
        }

        item {
            MoreMenuCard(
                title = "Customers & Credit",
                subtitle = "Manage accounts, wholesale/retail segments & credit repayments",
                icon = Icons.Default.People,
                iconTint = EmeraldSuccess,
                onClick = { onNavigate(MoreSubScreen.CUSTOMERS) }
            )
        }

        item {
            MoreMenuCard(
                title = "Cash Management & Shifts",
                subtitle = "Drawer opening cash, sales tracking & closing variance",
                icon = Icons.Default.Payments,
                iconTint = AmberWarning,
                onClick = { onNavigate(MoreSubScreen.CASH_SHIFT) }
            )
        }

        item {
            MoreMenuCard(
                title = "Suppliers",
                subtitle = "Supplier directory, contact information and purchase records",
                icon = Icons.Default.LocalShipping,
                iconTint = BlueInfo,
                onClick = { onNavigate(MoreSubScreen.SUPPLIERS) }
            )
        }

        item {
            MoreMenuCard(
                title = "Expenses",
                subtitle = "Record daily overheads, rent, utilities and stock transit",
                icon = Icons.Default.AccountBalanceWallet,
                iconTint = RoseError,
                onClick = { onNavigate(MoreSubScreen.EXPENSES) }
            )
        }

        item {
            SectionHeader(title = "DATA & MIGRATION")
        }

        item {
            MoreMenuCard(
                title = "Import & Export (CSV)",
                subtitle = "Batch import products with validation preview or export catalog",
                icon = Icons.Default.UploadFile,
                iconTint = CyanPrimary,
                onClick = { onNavigate(MoreSubScreen.IMPORT_EXPORT) }
            )
        }

        item {
            MoreMenuCard(
                title = "Backup & Restore",
                subtitle = "JSON system snapshot, offline storage and instant restore",
                icon = Icons.Default.Backup,
                iconTint = EmeraldSuccess,
                onClick = { onNavigate(MoreSubScreen.BACKUP_RESTORE) }
            )
        }

        item {
            MoreMenuCard(
                title = "Audit Trail & Logs",
                subtitle = "Complete tamper-evident log of stock and financial events",
                icon = Icons.Default.History,
                iconTint = VioletAccent,
                onClick = { onNavigate(MoreSubScreen.AUDIT_LOG) }
            )
        }

        item {
            SectionHeader(title = "ORGANIZATION & SETTINGS")
        }

        item {
            MoreMenuCard(
                title = "Staff & Permissions",
                subtitle = "Staff accounts, PINs and role access control",
                icon = Icons.Default.Badge,
                iconTint = CyanPrimary,
                onClick = { onNavigate(MoreSubScreen.STAFF) }
            )
        }

        item {
            MoreMenuCard(
                title = "Business Profile & Receipts",
                subtitle = "Store name, currency (${businessProfile?.currency ?: "KSh"}), tax, address & headers",
                icon = Icons.Default.Settings,
                iconTint = Slate400,
                onClick = { onNavigate(MoreSubScreen.SETTINGS) }
            )
        }
    }
}

@Composable
fun MoreMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = Slate100
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Slate700)
        }
    }
}

// 1. Reports Screen (Section 8: Management -> Reports)
@Composable
fun ReportsScreen(
    sales: List<SaleEntity>,
    filter: ReportFilter,
    currency: String,
    onUpdateFilter: (ReportFilter) -> Unit,
    onClearFilter: () -> Unit
) {
    val filteredSales = remember(sales, filter) {
        sales.filter { sale ->
            val matchStaff = filter.staffName == "All Staff" || sale.staffName.equals(filter.staffName, ignoreCase = true)
            val matchSegment = filter.customerSegment == "All Segments" || sale.customerSegment.equals(filter.customerSegment, ignoreCase = true)
            matchStaff && matchSegment
        }
    }

    val totalGrossSales = filteredSales.sumOf { it.subtotal }
    val totalDiscount = filteredSales.sumOf { it.discountAmount }
    val totalNetSales = filteredSales.sumOf { it.totalAmount }
    val totalCost = filteredSales.sumOf { it.costAmount }
    val grossProfit = totalNetSales - totalCost
    val marginPercent = if (totalNetSales > 0) (grossProfit / totalNetSales) * 100 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Date Presets row
        item {
            Text("Date Preset Range:", style = MaterialTheme.typography.labelSmall, color = Slate400)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DateRangeFilter.values().take(4).forEach { preset ->
                    FilterChip(
                        selected = filter.dateRange == preset,
                        onClick = { onUpdateFilter(filter.copy(dateRange = preset)) },
                        label = { Text(preset.label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = CyanPrimary
                        )
                    )
                }
            }
        }

        // Summary Metric Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Net Revenue",
                    value = formatMoney(totalNetSales, currency),
                    subtitle = "${filteredSales.size} sales",
                    icon = Icons.Default.TrendingUp,
                    iconTint = CyanPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Gross Profit",
                    value = formatMoney(grossProfit, currency),
                    subtitle = "Margin: %.1f%%".format(marginPercent),
                    icon = Icons.Default.MonetizationOn,
                    iconTint = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Total Cost",
                    value = formatMoney(totalCost, currency),
                    icon = Icons.Default.Receipt,
                    iconTint = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Total Discounts",
                    value = formatMoney(totalDiscount, currency),
                    icon = Icons.Default.LocalOffer,
                    iconTint = RoseError,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            SectionHeader(title = "TRANSACTIONS LIST (${filteredSales.size})")
        }

        items(filteredSales) { sale ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(sale.customerName, fontWeight = FontWeight.SemiBold, color = Slate100)
                        Text(
                            text = "${sale.saleNumber} • ${sale.staffName} • ${sale.customerSegment}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatMoney(sale.totalAmount, currency),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate100
                        )
                        Text(
                            text = "Profit: ${formatMoney(sale.grossProfit, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldSuccess
                        )
                    }
                }
            }
        }
    }
}

// 2. Customers Screen (Section 8: Management -> Customers & Credit)
@Composable
fun CustomersScreen(
    customers: List<CustomerEntity>,
    currency: String,
    onSaveCustomer: (CustomerEntity) -> Unit,
    onPayCredit: (String, Double, String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCustomerForPay by remember { mutableStateOf<CustomerEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(customers, searchQuery) {
        customers.filter {
            searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search customer...", color = Slate400, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
            ) {
                Text("+ New", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { customer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(customer.name, fontWeight = FontWeight.Bold, color = Slate100)
                            Text(
                                text = "Tel: ${customer.phone.ifBlank { "N/A" }} • Limit: ${formatMoney(customer.creditLimit, currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            StatusBadge(customer.segment, if (customer.segment == "Wholesale") BlueInfo else EmeraldSuccess)
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Balance Owed:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Slate400
                            )
                            Text(
                                text = formatMoney(customer.currentCreditBalance, currency),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (customer.currentCreditBalance > 0) AmberWarning else Slate100
                            )

                            if (customer.currentCreditBalance > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = { selectedCustomerForPay = customer },
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Slate950),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Pay Credit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Customer
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var segment by remember { mutableStateOf("Retail") }
        var limit by remember { mutableStateOf("10000") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Add Customer Account", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)

                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Customer Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Retail", "Wholesale", "VIP").forEach { seg ->
                            FilterChip(
                                selected = segment == seg,
                                onClick = { segment = seg },
                                label = { Text(seg) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = limit,
                        onValueChange = { limit = it },
                        label = { Text("Credit Limit ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSaveCustomer(
                                    CustomerEntity(
                                        id = "CUST-" + UUID.randomUUID().toString().take(8),
                                        name = name,
                                        phone = phone,
                                        segment = segment,
                                        creditLimit = limit.toDoubleOrNull() ?: 10000.0
                                    )
                                )
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                    ) {
                        Text("Save Customer", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal: Pay Credit
    if (selectedCustomerForPay != null) {
        val c = selectedCustomerForPay!!
        var payAmount by remember { mutableStateOf(c.currentCreditBalance.toString()) }
        var payMethod by remember { mutableStateOf("M-Pesa") }
        var refCode by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { selectedCustomerForPay = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, EmeraldSuccess)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Record Credit Payment", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                    Text("Customer: ${c.name} • Balance: ${formatMoney(c.currentCreditBalance, currency)}", color = Slate400, fontSize = 12.sp)

                    OutlinedTextField(
                        value = payAmount,
                        onValueChange = { payAmount = it },
                        label = { Text("Payment Amount ($currency)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Cash", "M-Pesa", "Bank").forEach { m ->
                            FilterChip(selected = payMethod == m, onClick = { payMethod = m }, label = { Text(m) })
                        }
                    }

                    OutlinedTextField(
                        value = refCode,
                        onValueChange = { refCode = it },
                        label = { Text("Reference Code") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val amt = payAmount.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onPayCredit(c.id, amt, payMethod, refCode)
                                selectedCustomerForPay = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Slate950)
                    ) {
                        Text("Confirm Payment Received", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 3. Suppliers Screen
@Composable
fun SuppliersScreen(
    suppliers: List<SupplierEntity>,
    currency: String,
    onSaveSupplier: (SupplierEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Supplier Directory", style = MaterialTheme.typography.titleMedium, color = Slate100)
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
            ) {
                Text("+ New Supplier", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(suppliers) { sup ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(sup.name, fontWeight = FontWeight.Bold, color = Slate100)
                        Text("Phone: ${sup.phone.ifBlank { "N/A" }} • Email: ${sup.email.ifBlank { "N/A" }}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        if (sup.address.isNotBlank()) {
                            Text("Address: ${sup.address}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("New Supplier", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Company *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email / Contact Person") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address / City") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSaveSupplier(
                                    SupplierEntity(
                                        id = "SUP-" + UUID.randomUUID().toString().take(8),
                                        name = name,
                                        email = email,
                                        phone = phone,
                                        address = address
                                    )
                                )
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                    ) {
                        Text("Save Supplier", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 4. Expenses Screen
@Composable
fun ExpensesScreen(
    expenses: List<ExpenseEntity>,
    currency: String,
    onAddExpense: (ExpenseEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Business Overheads", style = MaterialTheme.typography.titleMedium, color = Slate100)
                Text("Total: ${formatMoney(totalExpenses, currency)}", style = MaterialTheme.typography.bodySmall, color = RoseError)
            }
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RoseError, contentColor = Color.White)
            ) {
                Text("+ New Expense", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(expenses) { exp ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(exp.category, fontWeight = FontWeight.Bold, color = Slate100)
                            Text(exp.notes.ifBlank { "Recorded by ${exp.staffName}" }, style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                        Text(formatMoney(exp.amount, currency), fontWeight = FontWeight.Bold, color = RoseError)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var category by remember { mutableStateOf("Rent & Utilities") }
        var amount by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Record Expense", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Rent", "Transport", "Salaries", "Utilities").forEach { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat, fontSize = 11.sp) })
                        }
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount ($currency) *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Reason") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull() ?: 0.0
                            if (amt > 0) {
                                onAddExpense(
                                    ExpenseEntity(
                                        id = "EXP-" + UUID.randomUUID().toString().take(8),
                                        category = category,
                                        amount = amt,
                                        notes = notes,
                                        staffName = "Cashier"
                                    )
                                )
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RoseError, contentColor = Color.White)
                    ) {
                        Text("Record Expense", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 5. Cash Management & Shifts Screen
@Composable
fun CashShiftScreen(
    shift: CashShiftEntity?,
    currency: String,
    onStartShift: (Double) -> Unit,
    onCloseShift: (Double, String) -> Unit
) {
    var openingCashText by remember { mutableStateOf("2000") }
    var closingCashText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (shift == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, AmberWarning)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Start New Shift", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AmberWarning)
                        Text("Specify drawer opening float to track cash variance at end of shift.", style = MaterialTheme.typography.bodySmall, color = Slate400)

                        OutlinedTextField(
                            value = openingCashText,
                            onValueChange = { openingCashText = it },
                            label = { Text("Opening Cash Float ($currency)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Button(
                            onClick = {
                                val floatVal = openingCashText.toDoubleOrNull() ?: 0.0
                                onStartShift(floatVal)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                        ) {
                            Text("Open Drawer & Start Shift", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, EmeraldSuccess)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Shift Details", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldSuccess)
                            StatusBadge("Shift Live", EmeraldSuccess)
                        }

                        Text("Cashier: ${shift.staffName}", color = Slate100, fontWeight = FontWeight.SemiBold)
                        Text("Opening Cash Float: ${formatMoney(shift.openingCash, currency)}", color = Slate400)
                        Text("Expected Drawer Cash: ${formatMoney(shift.expectedCash, currency)}", color = CyanPrimary, fontWeight = FontWeight.Bold)

                        Divider(color = Slate800)

                        Text("Close Shift & Cash Audit:", style = MaterialTheme.typography.labelSmall, color = Slate200)

                        OutlinedTextField(
                            value = closingCashText,
                            onValueChange = { closingCashText = it },
                            label = { Text("Actual Cash Counted in Drawer ($currency)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Notes / Discrepancy explanation") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val counted = closingCashText.toDoubleOrNull() ?: shift.expectedCash
                                onCloseShift(counted, notesText)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseError, contentColor = Color.White)
                        ) {
                            Text("Close Shift & Lock Drawer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// 6. Staff & Permissions Screen
@Composable
fun StaffScreen(
    staffList: List<StaffEntity>,
    onSaveStaff: (StaffEntity) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Staff Accounts", style = MaterialTheme.typography.titleMedium, color = Slate100)
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
            ) {
                Text("+ New Staff", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(staffList) { staff ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(staff.name, fontWeight = FontWeight.Bold, color = Slate100)
                            Text("Username: @${staff.username}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                        }
                        StatusBadge(staff.role, if (staff.role == "OWNER") AmberWarning else CyanPrimary)
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var username by remember { mutableStateOf("") }
        var pin by remember { mutableStateOf("1234") }
        var role by remember { mutableStateOf("CASHIER") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate700)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Add Staff Member", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username *") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = pin, onValueChange = { pin = it }, label = { Text("Login PIN (4 digits)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CASHIER", "MANAGER", "ADMIN").forEach { r ->
                            FilterChip(selected = role == r, onClick = { role = r }, label = { Text(r) })
                        }
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() && username.isNotBlank()) {
                                onSaveStaff(
                                    StaffEntity(
                                        id = "STAFF-" + UUID.randomUUID().toString().take(8),
                                        name = name,
                                        username = username.lowercase(Locale.getDefault()),
                                        pin = pin,
                                        role = role
                                    )
                                )
                                showAddDialog = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                    ) {
                        Text("Create Staff Account", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// 7. Import & Export CSV Screen (Section 8: Tools -> Import / Export)
@Composable
fun ImportExportScreen(
    importPreview: ImportPreviewResult?,
    isImporting: Boolean,
    onParseCsv: (String) -> Unit,
    onApplyImport: () -> Unit,
    onCancelImport: () -> Unit,
    getTemplateCsv: () -> String,
    onExportCsv: suspend () -> String
) {
    var csvInputText by remember { mutableStateOf("") }
    var exportedCsvText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val errorsList = remember(importPreview) { importPreview?.rows?.mapNotNull { it.error } ?: emptyList() }
    val warningsList = remember(importPreview) { importPreview?.rows?.mapNotNull { it.warning } ?: emptyList() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Step 1: Template and Paste
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("1. CSV Batch Import", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)
                    Text(
                        "Paste or edit CSV rows adhering to the RG POS multi-form schema. Supports products, packaging forms, base units, and independent retail/wholesale prices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { csvInputText = getTemplateCsv() },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Load Template")
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    exportedCsvText = onExportCsv()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess)
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Current Inventory")
                        }
                    }

                    OutlinedTextField(
                        value = csvInputText,
                        onValueChange = { csvInputText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("Paste CSV data here...", color = Slate400) },
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = { onParseCsv(csvInputText) },
                        enabled = csvInputText.isNotBlank() && !isImporting,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                    ) {
                        Text(if (isImporting) "Parsing..." else "Preview Import Validation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Step 2: Validation Preview Result
        if (importPreview != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, if (importPreview.errorsCount == 0) EmeraldSuccess else AmberWarning)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Validation Summary (Batch: ${importPreview.batchId})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate100
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatusBadge("New: ${importPreview.newProductsCount}", EmeraldSuccess)
                            StatusBadge("Updated: ${importPreview.productsUpdatedCount}", CyanPrimary)
                            StatusBadge("Rows: ${importPreview.totalRows}", BlueInfo)
                            if (importPreview.errorsCount > 0) {
                                StatusBadge("Errors: ${importPreview.errorsCount}", RoseError)
                            }
                        }

                        if (errorsList.isNotEmpty()) {
                            Text("Errors found:", color = RoseError, style = MaterialTheme.typography.labelSmall)
                            errorsList.take(5).forEach { err ->
                                Text("• $err", color = RoseError, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (warningsList.isNotEmpty()) {
                            Text("Warnings:", color = AmberWarning, style = MaterialTheme.typography.labelSmall)
                            warningsList.take(5).forEach { warn ->
                                Text("• $warn", color = AmberWarning, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onCancelImport,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = onApplyImport,
                                enabled = importPreview.errorsCount == 0 && !isImporting,
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess, contentColor = Slate950)
                            ) {
                                Text(if (isImporting) "Importing..." else "Apply Import Safely", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Exported text view if clicked
        if (exportedCsvText != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, CyanPrimary)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Exported CSV String", fontWeight = FontWeight.Bold, color = Slate100)
                            IconButton(onClick = { exportedCsvText = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                            }
                        }
                        OutlinedTextField(
                            value = exportedCsvText ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }
            }
        }
    }
}

// 8. Backup & Restore Screen (Section 8: Tools -> Backup & Restore)
@Composable
fun BackupRestoreScreen(
    onCreateBackup: suspend () -> String,
    onRestoreBackup: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var backupJsonString by remember { mutableStateOf<String?>(null) }
    var restoreInputString by remember { mutableStateOf("") }
    var showSafetyRestorePrompt by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Create JSON System Snapshot", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)
                    Text("Exports your full offline catalog, customers, historical sales, staff and business settings into a single verifiable JSON payload.", style = MaterialTheme.typography.bodySmall, color = Slate400)

                    Button(
                        onClick = {
                            scope.launch {
                                backupJsonString = onCreateBackup()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate Full Backup JSON", fontWeight = FontWeight.Bold)
                    }

                    if (backupJsonString != null) {
                        OutlinedTextField(
                            value = backupJsonString ?: "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Restore from JSON Backup", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AmberWarning)
                    Text("Paste an existing RG POS JSON backup. An automatic safety snapshot will be created before applying.", style = MaterialTheme.typography.bodySmall, color = Slate400)

                    OutlinedTextField(
                        value = restoreInputString,
                        onValueChange = { restoreInputString = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Paste JSON snapshot here...", color = Slate400) }
                    )

                    Button(
                        onClick = { showSafetyRestorePrompt = true },
                        enabled = restoreInputString.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AmberWarning, contentColor = Slate950)
                    ) {
                        Text("Restore System", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSafetyRestorePrompt) {
        Dialog(onDismissRequest = { showSafetyRestorePrompt = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, RoseError)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Confirm System Restore", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = RoseError)
                    Text("This operation will import entities from the provided JSON string. Are you sure you wish to proceed?", color = Slate300)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { showSafetyRestorePrompt = false }, modifier = Modifier.weight(1f)) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                showSafetyRestorePrompt = false
                                onRestoreBackup(restoreInputString)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = RoseError, contentColor = Color.White)
                        ) {
                            Text("Confirm")
                        }
                    }
                }
            }
        }
    }
}

// 9. Audit Log Screen
@Composable
fun AuditLogScreen(recentAuditLogs: List<AuditLogEntity>) {
    val timeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(recentAuditLogs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, Slate800)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatusBadge(log.action, CyanPrimary)
                        Text(timeFormat.format(Date(log.timestamp)), style = MaterialTheme.typography.bodySmall, color = Slate400)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Action on ${log.entity}: ${log.newState ?: log.action}", style = MaterialTheme.typography.bodyMedium, color = Slate100)
                    Text("By ${log.staffName} • Ref: ${log.reference ?: "N/A"}", style = MaterialTheme.typography.labelSmall, color = Slate400)
                }
            }
        }
    }
}

// 10. Settings & Profile Screen
@Composable
fun SettingsScreen(
    businessProfile: BusinessProfileEntity,
    onSave: (BusinessProfileEntity) -> Unit
) {
    var name by remember { mutableStateOf(businessProfile.name) }
    var phone by remember { mutableStateOf(businessProfile.phone) }
    var address by remember { mutableStateOf(businessProfile.address) }
    var currency by remember { mutableStateOf(businessProfile.currency) }
    var receiptFooter by remember { mutableStateOf(businessProfile.receiptFooter) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Store Profile", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)
        }

        item {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Store / Company Name") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Telephone") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Physical Address / City") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency Symbol (e.g. KSh, USD, EUR)") }, modifier = Modifier.fillMaxWidth())
        }

        item {
            OutlinedTextField(
                value = receiptFooter,
                onValueChange = { receiptFooter = it },
                label = { Text("Receipt Footer Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    onSave(
                        businessProfile.copy(
                            name = name,
                            phone = phone,
                            address = address,
                            currency = currency,
                            receiptFooter = receiptFooter
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
            ) {
                Text("Save Profile Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}
