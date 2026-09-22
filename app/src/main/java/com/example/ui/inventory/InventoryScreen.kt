package com.example.ui.inventory

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.FormPriceEntity
import com.example.data.local.entity.PackagingFormEntity
import com.example.data.local.entity.ProductEntity
import com.example.data.local.entity.StockMovementEntity
import com.example.domain.model.PackagingFormWithPrices
import com.example.domain.model.ProductWithForms
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatMoney
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun InventoryScreen(
    productsWithForms: List<ProductWithForms>,
    recentMovements: List<StockMovementEntity>,
    currency: String,
    onSaveProduct: (ProductEntity, List<PackagingFormWithPrices>) -> Unit,
    onAddStock: (String, String, Double, Double, String, String) -> Unit,
    onOpenPackage: (String, String, Double) -> Unit,
    onDeactivateProduct: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Products, 1: Stock Movements
    var searchQuery by remember { mutableStateOf("") }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var selectedProductForAddStock by remember { mutableStateOf<ProductWithForms?>(null) }
    var selectedProductForOpenPackage by remember { mutableStateOf<ProductWithForms?>(null) }
    var selectedProductForEdit by remember { mutableStateOf<ProductWithForms?>(null) }

    val filteredProducts = remember(productsWithForms, searchQuery) {
        productsWithForms.filter {
            searchQuery.isBlank() ||
                it.product.name.contains(searchQuery, ignoreCase = true) ||
                it.product.category.contains(searchQuery, ignoreCase = true) ||
                (it.product.sku?.contains(searchQuery, ignoreCase = true) == true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
            .padding(bottom = 90.dp) // Leave room for floating pill nav
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Inventory Control",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Slate100
                )
                Text(
                    text = "${productsWithForms.size} Active Products",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )
            }

            Button(
                onClick = { showAddProductDialog = true },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950),
                modifier = Modifier.testTag("inventory_add_product_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Product", fontWeight = FontWeight.SemiBold)
            }
        }

        // Tabs: Products vs Movements
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Slate900,
            contentColor = CyanPrimary,
            divider = { Divider(color = Slate800) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Product Catalog", fontWeight = FontWeight.SemiBold) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Stock Movements Audit", fontWeight = FontWeight.SemiBold) }
            )
        }

        if (selectedTab == 0) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search products, categories, SKU...", color = Slate400, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Slate400) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900
                )
            )

            // Products List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProducts) { item ->
                    InventoryProductCard(
                        productWithForms = item,
                        currency = currency,
                        onAddStockClick = { selectedProductForAddStock = item },
                        onOpenPackageClick = { selectedProductForOpenPackage = item },
                        onEditClick = { selectedProductForEdit = item }
                    )
                }
            }
        } else {
            // Stock Movements Audit List (Section 36: Every stock change creates a movement)
            val timeFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(recentMovements) { mov ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val isPositive = mov.baseQuantityDelta >= 0
                                    StatusBadge(
                                        text = mov.movementType,
                                        color = when (mov.movementType) {
                                            "ADD", "PURCHASE", "IMPORT_ADD" -> EmeraldSuccess
                                            "SALE" -> CyanPrimary
                                            "PACKAGE_OPENED" -> VioletAccent
                                            else -> AmberWarning
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = mov.reason.ifBlank { "Movement" },
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Slate100
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "By ${mov.staffName} • ${timeFormat.format(Date(mov.timestamp))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = (if (mov.baseQuantityDelta > 0) "+" else "") + "${mov.baseQuantityDelta}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (mov.baseQuantityDelta >= 0) EmeraldSuccess else RoseError
                                )
                                Text(
                                    text = "base units",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Stock (Section 38 & 39 UI fix: consistent height, padding, responsive layout)
    if (selectedProductForAddStock != null) {
        AddStockDialog(
            productWithForms = selectedProductForAddStock!!,
            currency = currency,
            onDismiss = { selectedProductForAddStock = null },
            onConfirm = { formId, qty, cost, supplier, notes ->
                onAddStock(selectedProductForAddStock!!.product.id, formId, qty, cost, supplier, notes)
                selectedProductForAddStock = null
            }
        )
    }

    // Modal: Open Package (Section 27: PACKAGE_OPENED)
    if (selectedProductForOpenPackage != null) {
        OpenPackageDialog(
            productWithForms = selectedProductForOpenPackage!!,
            onDismiss = { selectedProductForOpenPackage = null },
            onConfirm = { formId, qty ->
                onOpenPackage(selectedProductForOpenPackage!!.product.id, formId, qty)
                selectedProductForOpenPackage = null
            }
        )
    }

    // Modal: Product Setup / Edit Wizard (Section 37)
    if (showAddProductDialog || selectedProductForEdit != null) {
        ProductSetupWizardDialog(
            initialProductWithForms = selectedProductForEdit,
            currency = currency,
            onDismiss = {
                showAddProductDialog = false
                selectedProductForEdit = null
            },
            onSave = { product, forms ->
                onSaveProduct(product, forms)
                showAddProductDialog = false
                selectedProductForEdit = null
            },
            onDeactivate = { pId ->
                onDeactivateProduct(pId)
                selectedProductForEdit = null
            }
        )
    }
}

@Composable
fun InventoryProductCard(
    productWithForms: ProductWithForms,
    currency: String,
    onAddStockClick: () -> Unit,
    onOpenPackageClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val product = productWithForms.product
    val forms = productWithForms.forms
    val hasOpenableForm = forms.any { it.form.canBeOpened }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900),
        border = BorderStroke(1.dp, Slate800)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = Slate100
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${product.category} • Base Unit: ${product.baseUnit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                        if (product.sku != null) {
                            Text(
                                text = " • SKU: ${product.sku}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${product.currentStockBaseUnits} ${product.baseUnit}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (product.currentStockBaseUnits <= product.reorderLevel) AmberWarning else Slate100
                    )
                    StatusBadge(
                        text = if (product.currentStockBaseUnits <= 0) "Out of Stock" else if (product.currentStockBaseUnits <= product.reorderLevel) "Low Stock" else "In Stock",
                        color = if (product.currentStockBaseUnits <= 0) RoseError else if (product.currentStockBaseUnits <= product.reorderLevel) AmberWarning else EmeraldSuccess
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Slate800)
            Spacer(modifier = Modifier.height(8.dp))

            // Packaging Forms Table summary (Shows independent prices!)
            Text(
                text = "CONFIGURED PACKAGING FORMS & INDEPENDENT PRICES:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                color = CyanPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))

            forms.forEach { formWithPrices ->
                val f = formWithPrices.form
                val retPrice = formWithPrices.getPriceForTier("Retail")
                val whlPrice = formWithPrices.getPriceForTier("Wholesale")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "• ${f.name} (= ${f.quantityInBaseUnits} ${product.baseUnit})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate200,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "Retail: ${formatMoney(retPrice, currency)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = Slate100,
                        modifier = Modifier.weight(1.1f)
                    )
                    Text(
                        text = "Wholesale: ${formatMoney(whlPrice, currency)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = CyanPrimary,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onAddStockClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanPrimary),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.AddBox, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Stock", fontSize = 12.sp)
                }

                if (hasOpenableForm) {
                    OutlinedButton(
                        onClick = onOpenPackageClick,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = VioletAccent),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Package", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate200),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// Modal: Add Stock (Section 38 & 39 UI Fix)
@Composable
fun AddStockDialog(
    productWithForms: ProductWithForms,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (formId: String, quantity: Double, cost: Double, supplier: String, notes: String) -> Unit
) {
    val forms = productWithForms.forms
    var selectedForm by remember { mutableStateOf(forms.firstOrNull()) }
    var quantityText by remember { mutableStateOf("10") }
    var purchaseCostText by remember { mutableStateOf(selectedForm?.form?.purchaseCost?.toString() ?: "0") }
    var supplierText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("Stock replenishment") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate700)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Add Stock", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)
                        Text(productWithForms.product.name, style = MaterialTheme.typography.bodySmall, color = CyanPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Select Packaging Form:", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Slate200)
                Spacer(modifier = Modifier.height(6.dp))

                // Section 39: Form selection boxes with uniform height & alignment
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    forms.forEach { formWithPrices ->
                        val isSelected = selectedForm?.form?.id == formWithPrices.form.id
                        Surface(
                            onClick = {
                                selectedForm = formWithPrices
                                purchaseCostText = formWithPrices.form.purchaseCost.toString()
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Slate850 else Slate900,
                            border = BorderStroke(1.dp, if (isSelected) CyanPrimary else Slate800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedForm = formWithPrices }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${formWithPrices.form.name} (= ${formWithPrices.form.quantityInBaseUnits} ${productWithForms.product.baseUnit})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Slate100
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = purchaseCostText,
                        onValueChange = { purchaseCostText = it },
                        label = { Text("Unit Cost ($currency)") },
                        modifier = Modifier.weight(1.2f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = supplierText,
                    onValueChange = { supplierText = it },
                    label = { Text("Supplier Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val qty = quantityText.toDoubleOrNull() ?: 0.0
                        val cost = purchaseCostText.toDoubleOrNull() ?: 0.0
                        if (selectedForm != null && qty > 0) {
                            onConfirm(selectedForm!!.form.id, qty, cost, supplierText, notesText)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                ) {
                    Text("Confirm Stock Addition", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Modal: Open Package (Section 27: PACKAGE_OPENED)
@Composable
fun OpenPackageDialog(
    productWithForms: ProductWithForms,
    onDismiss: () -> Unit,
    onConfirm: (formId: String, quantity: Double) -> Unit
) {
    val openableForms = productWithForms.forms.filter { it.form.canBeOpened }
    var selectedForm by remember { mutableStateOf(openableForms.firstOrNull()) }
    var quantityText by remember { mutableStateOf("1") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Text(
                    text = "Break Down / Open Package",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = VioletAccent
                )
                Text(
                    text = "Convert intact packaging into loose base units without losing traceability.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate400
                )

                Spacer(modifier = Modifier.height(12.dp))

                openableForms.forEach { formWithPrices ->
                    val isSelected = selectedForm?.form?.id == formWithPrices.form.id
                    Surface(
                        onClick = { selectedForm = formWithPrices },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Slate850 else Slate900,
                        border = BorderStroke(1.dp, if (isSelected) VioletAccent else Slate800),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { selectedForm = formWithPrices })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${formWithPrices.form.name} (= ${formWithPrices.form.quantityInBaseUnits} ${productWithForms.product.baseUnit})",
                                color = Slate100
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Number of packages to open") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        if (selectedForm != null && qty > 0) {
                            onConfirm(selectedForm!!.form.id, qty)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VioletAccent, contentColor = Color.White)
                ) {
                    Text("Confirm Package Opened", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Modal: Product Setup Wizard Dialog (Section 37)
@Composable
fun ProductSetupWizardDialog(
    initialProductWithForms: ProductWithForms?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, List<PackagingFormWithPrices>) -> Unit,
    onDeactivate: (String) -> Unit
) {
    val isEdit = initialProductWithForms != null
    var name by remember { mutableStateOf(initialProductWithForms?.product?.name ?: "") }
    var category by remember { mutableStateOf(initialProductWithForms?.product?.category ?: "General") }
    var baseUnit by remember { mutableStateOf(initialProductWithForms?.product?.baseUnit ?: "Piece") }
    var sku by remember { mutableStateOf(initialProductWithForms?.product?.sku ?: "") }
    var barcode by remember { mutableStateOf(initialProductWithForms?.product?.barcode ?: "") }

    // Forms list state
    var formsList by remember {
        mutableStateOf(
            initialProductWithForms?.forms ?: listOf(
                PackagingFormWithPrices(
                    form = PackagingFormEntity(
                        id = "FORM-" + UUID.randomUUID().toString().take(8),
                        productId = initialProductWithForms?.product?.id ?: "",
                        name = "Single Piece",
                        quantityInBaseUnits = 1.0,
                        isDefaultSelling = true
                    ),
                    prices = listOf(
                        FormPriceEntity(UUID.randomUUID().toString(), "", "Retail", 0.0),
                        FormPriceEntity(UUID.randomUUID().toString(), "", "Wholesale", 0.0)
                    )
                )
            )
        )
    }

    var newFormName by remember { mutableStateOf("") }
    var newFormQty by remember { mutableStateOf("1") }
    var newFormRetailPrice by remember { mutableStateOf("0") }
    var newFormWholesalePrice by remember { mutableStateOf("0") }
    var newFormCanBeOpened by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = BorderStroke(1.dp, Slate700)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEdit) "Edit Product" else "New Product Setup",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Slate100
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = baseUnit,
                            onValueChange = { baseUnit = it },
                            label = { Text("Base Unit * (e.g. KG, Bottle)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { sku = it },
                            label = { Text("SKU / Code") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PACKAGING FORMS & INDEPENDENT PRICES",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyanPrimary
                    )
                }

                // Existing forms
                items(formsList) { formItem ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = Slate850,
                        border = BorderStroke(1.dp, Slate700)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(formItem.form.name, fontWeight = FontWeight.Bold, color = Slate100)
                                Text(
                                    text = "= ${formItem.form.quantityInBaseUnits} $baseUnit • Retail: ${formatMoney(formItem.getPriceForTier("Retail"), currency)} • Wholesale: ${formatMoney(formItem.getPriceForTier("Wholesale"), currency)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                            if (formsList.size > 1) {
                                IconButton(
                                    onClick = { formsList = formsList.filterNot { it.form.id == formItem.form.id } }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = RoseError, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                // Add Packaging Form sub-card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate950),
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Add Another Packaging Form:", style = MaterialTheme.typography.labelSmall, color = Slate200)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFormName,
                                    onValueChange = { newFormName = it },
                                    label = { Text("Form Name (e.g. Carton 18)") },
                                    modifier = Modifier.weight(1.4f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = newFormQty,
                                    onValueChange = { newFormQty = it },
                                    label = { Text("Qty In Base") },
                                    modifier = Modifier.weight(0.8f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newFormRetailPrice,
                                    onValueChange = { newFormRetailPrice = it },
                                    label = { Text("Retail Price") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                OutlinedTextField(
                                    value = newFormWholesalePrice,
                                    onValueChange = { newFormWholesalePrice = it },
                                    label = { Text("Wholesale Price") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = newFormCanBeOpened,
                                    onCheckedChange = { newFormCanBeOpened = it }
                                )
                                Text("Can be opened into loose stock (e.g. 50kg sack)", style = MaterialTheme.typography.bodySmall, color = Slate300)
                            }

                            Button(
                                onClick = {
                                    if (newFormName.isNotBlank()) {
                                        val formId = "FORM-" + UUID.randomUUID().toString().take(8)
                                        val qty = newFormQty.toDoubleOrNull() ?: 1.0
                                        val ret = newFormRetailPrice.toDoubleOrNull() ?: 0.0
                                        val whl = newFormWholesalePrice.toDoubleOrNull() ?: ret

                                        val newForm = PackagingFormWithPrices(
                                            form = PackagingFormEntity(
                                                id = formId,
                                                productId = initialProductWithForms?.product?.id ?: "",
                                                name = newFormName,
                                                quantityInBaseUnits = qty,
                                                canBeOpened = newFormCanBeOpened
                                            ),
                                            prices = listOf(
                                                FormPriceEntity(UUID.randomUUID().toString(), formId, "Retail", ret),
                                                FormPriceEntity(UUID.randomUUID().toString(), formId, "Wholesale", whl)
                                            )
                                        )
                                        formsList = formsList + newForm
                                        newFormName = ""
                                        newFormQty = "1"
                                        newFormRetailPrice = "0"
                                        newFormWholesalePrice = "0"
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanPrimary)
                            ) {
                                Text("+ Add This Form")
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val productId = initialProductWithForms?.product?.id ?: ("PRD-" + UUID.randomUUID().toString().take(8))
                                val product = ProductEntity(
                                    id = productId,
                                    name = name,
                                    category = category,
                                    baseUnit = baseUnit,
                                    sku = sku.ifBlank { null },
                                    barcode = barcode.ifBlank { null },
                                    currentStockBaseUnits = initialProductWithForms?.product?.currentStockBaseUnits ?: 0.0
                                )
                                val updatedForms = formsList.map { fwp ->
                                    val updatedForm = fwp.form.copy(productId = productId)
                                    val updatedPrices = fwp.prices.map { p -> p.copy(packagingFormId = updatedForm.id) }
                                    PackagingFormWithPrices(updatedForm, updatedPrices)
                                }
                                onSave(product, updatedForms)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                    ) {
                        Text("Save Product & Packaging", fontWeight = FontWeight.Bold)
                    }

                    if (isEdit) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = { onDeactivate(initialProductWithForms!!.product.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Deactivate Product", color = RoseError)
                        }
                    }
                }
            }
        }
    }
}
