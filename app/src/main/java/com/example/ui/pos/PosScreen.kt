package com.example.ui.pos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.local.entity.CustomerEntity
import com.example.domain.model.CartItem
import com.example.domain.model.PackagingFormWithPrices
import com.example.domain.model.PaymentSplit
import com.example.domain.model.ProductWithForms
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatMoney
import com.example.ui.theme.*

@Composable
fun PosScreen(
    productsWithForms: List<ProductWithForms>,
    categories: List<String>,
    cartItems: List<CartItem>,
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    priceTier: String,
    searchQuery: String,
    selectedCategory: String,
    heldSalesCount: Int,
    currency: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriceTierChange: (String) -> Unit,
    onSelectCustomer: (CustomerEntity?) -> Unit,
    onAddToCart: (ProductWithForms, PackagingFormWithPrices, Double) -> Unit,
    onUpdateQuantity: (String, String, Double) -> Unit,
    onUpdatePrice: (String, String, Double) -> Unit,
    onRemoveItem: (String, String) -> Unit,
    onClearCart: () -> Unit,
    onHoldSale: () -> Unit,
    onResumeSaleClick: () -> Unit,
    onCompleteCheckout: (PaymentSplit) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProductForFormSelection by remember { mutableStateOf<ProductWithForms?>(null) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showCartBottomSheetOnPhone by remember { mutableStateOf(false) }

    val filteredProducts = remember(productsWithForms, searchQuery, selectedCategory) {
        productsWithForms.filter { item ->
            val matchQuery = searchQuery.isBlank() ||
                item.product.name.contains(searchQuery, ignoreCase = true) ||
                (item.product.sku?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.product.barcode?.contains(searchQuery, ignoreCase = true) == true)
            val matchCategory = selectedCategory == "All" || item.product.category.equals(selectedCategory, ignoreCase = true)
            matchQuery && matchCategory
        }
    }

    val cartTotal = remember(cartItems) { cartItems.sumOf { it.total } }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950)
    ) {
        val isTablet = maxWidth >= 680.dp

        if (isTablet) {
            // Tablet layout (Section 82: Catalog on Left, Cart on Right)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp)
            ) {
                Box(modifier = Modifier.weight(1.3f)) {
                    ProductCatalogPane(
                        products = filteredProducts,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        priceTier = priceTier,
                        currency = currency,
                        onSearchChange = onSearchChange,
                        onCategoryChange = onCategoryChange,
                        onPriceTierChange = onPriceTierChange,
                        onProductClick = { selectedProductForFormSelection = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Slate900)
                ) {
                    CartPane(
                        cartItems = cartItems,
                        selectedCustomer = selectedCustomer,
                        currency = currency,
                        cartTotal = cartTotal,
                        heldSalesCount = heldSalesCount,
                        onSelectCustomerClick = { showCustomerPicker = true },
                        onUpdateQuantity = onUpdateQuantity,
                        onUpdatePrice = onUpdatePrice,
                        onRemoveItem = onRemoveItem,
                        onClearCart = onClearCart,
                        onHoldSale = onHoldSale,
                        onResumeSale = onResumeSaleClick,
                        onCheckoutClick = { showCheckoutDialog = true }
                    )
                }
            }
        } else {
            // Phone layout (Catalog with bottom floating summary button)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp)
            ) {
                // Header status & customer selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Customer Chip
                    Surface(
                        onClick = { showCustomerPicker = true },
                        shape = RoundedCornerShape(8.dp),
                        color = Slate900,
                        border = BorderStroke(1.dp, if (selectedCustomer != null) CyanPrimary else Slate700)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (selectedCustomer != null) CyanPrimary else Slate400,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedCustomer?.name ?: "Walk-in Customer",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selectedCustomer != null) Slate100 else Slate400
                            )
                        }
                    }

                    // Price Tier Selector
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Retail", "Wholesale").forEach { tier ->
                            FilterChip(
                                selected = priceTier == tier,
                                onClick = { onPriceTierChange(tier) },
                                label = { Text(tier, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                                    selectedLabelColor = CyanPrimary
                                )
                            )
                        }
                    }
                }

                // Catalog Pane
                Box(modifier = Modifier.weight(1f)) {
                    ProductCatalogPane(
                        products = filteredProducts,
                        categories = categories,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        priceTier = priceTier,
                        currency = currency,
                        onSearchChange = onSearchChange,
                        onCategoryChange = onCategoryChange,
                        onPriceTierChange = onPriceTierChange,
                        onProductClick = { selectedProductForFormSelection = it }
                    )
                }

                // Phone bottom cart action bar
                if (cartItems.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Slate850,
                        border = BorderStroke(1.dp, CyanPrimary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.clickable { showCartBottomSheetOnPhone = true }
                            ) {
                                Text(
                                    text = "${cartItems.sumOf { it.quantity.toInt() }} items in Cart",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CyanPrimary
                                )
                                Text(
                                    text = formatMoney(cartTotal, currency),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Slate100
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { showCartBottomSheetOnPhone = true },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("View Cart", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = { showCheckoutDialog = true },
                                    modifier = Modifier.testTag("pos_phone_pay_button"),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text("Pay", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Packaging Form Selector (Section 20-34: Product -> Base Unit -> Packaging Forms -> Independent Prices)
    if (selectedProductForFormSelection != null) {
        PackagingFormSelectorDialog(
            productWithForms = selectedProductForFormSelection!!,
            priceTier = priceTier,
            currency = currency,
            onDismiss = { selectedProductForFormSelection = null },
            onFormSelected = { formWithPrices, qty ->
                onAddToCart(selectedProductForFormSelection!!, formWithPrices, qty)
                selectedProductForFormSelection = null
            }
        )
    }

    // Modal: Customer Selector
    if (showCustomerPicker) {
        CustomerPickerDialog(
            customers = customers,
            selectedCustomer = selectedCustomer,
            onDismiss = { showCustomerPicker = false },
            onSelect = {
                onSelectCustomer(it)
                showCustomerPicker = false
            }
        )
    }

    // Modal: Phone Cart Bottom Sheet / Full Dialog
    if (showCartBottomSheetOnPhone) {
        Dialog(onDismissRequest = { showCartBottomSheetOnPhone = false }) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(16.dp),
                color = Slate900,
                border = BorderStroke(1.dp, Slate700)
            ) {
                CartPane(
                    cartItems = cartItems,
                    selectedCustomer = selectedCustomer,
                    currency = currency,
                    cartTotal = cartTotal,
                    heldSalesCount = heldSalesCount,
                    onSelectCustomerClick = {
                        showCartBottomSheetOnPhone = false
                        showCustomerPicker = true
                    },
                    onUpdateQuantity = onUpdateQuantity,
                    onUpdatePrice = onUpdatePrice,
                    onRemoveItem = onRemoveItem,
                    onClearCart = onClearCart,
                    onHoldSale = {
                        onHoldSale()
                        showCartBottomSheetOnPhone = false
                    },
                    onResumeSale = {
                        onResumeSaleClick()
                        showCartBottomSheetOnPhone = false
                    },
                    onCheckoutClick = {
                        showCartBottomSheetOnPhone = false
                        showCheckoutDialog = true
                    }
                )
            }
        }
    }

    // Modal: Payment Dialog (Cash / M-Pesa / Credit / Partial Validation)
    if (showCheckoutDialog) {
        PaymentCheckoutDialog(
            totalAmount = cartTotal,
            selectedCustomer = selectedCustomer,
            currency = currency,
            onDismiss = { showCheckoutDialog = false },
            onCompleteSale = { paymentSplit ->
                showCheckoutDialog = false
                onCompleteCheckout(paymentSplit)
            }
        )
    }
}

@Composable
fun ProductCatalogPane(
    products: List<ProductWithForms>,
    categories: List<String>,
    selectedCategory: String,
    searchQuery: String,
    priceTier: String,
    currency: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onPriceTierChange: (String) -> Unit,
    onProductClick: (ProductWithForms) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Search & Barcode Scan row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pos_search_input"),
                placeholder = { Text("Search name, SKU, barcode...", color = Slate400, fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Slate400, modifier = Modifier.size(20.dp))
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanPrimary,
                    unfocusedBorderColor = Slate700,
                    focusedContainerColor = Slate900,
                    unfocusedContainerColor = Slate900
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Categories Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedCategory == "All",
                    onClick = { onCategoryChange("All") },
                    label = { Text("All Products", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanPrimary
                    )
                )
            }
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = CyanPrimary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Product Cards Grid/List
        if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Slate700, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No products match your search", color = Slate400)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(products) { item ->
                    ProductPosCard(
                        productWithForms = item,
                        priceTier = priceTier,
                        currency = currency,
                        onClick = { onProductClick(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductPosCard(
    productWithForms: ProductWithForms,
    priceTier: String,
    currency: String,
    onClick: () -> Unit
) {
    val product = productWithForms.product
    val forms = productWithForms.forms

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("pos_product_${product.id}"),
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
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = Slate100
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Base: ${product.baseUnit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${forms.size} packaging forms",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyanPrimary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val defaultForm = forms.find { it.form.isDefaultSelling } ?: forms.firstOrNull()
                val price = defaultForm?.getPriceForTier(priceTier) ?: 0.0

                Text(
                    text = formatMoney(price, currency),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = Slate100
                )

                if (product.currentStockBaseUnits <= 0) {
                    StatusBadge("Out of Stock", RoseError)
                } else if (product.currentStockBaseUnits <= product.reorderLevel) {
                    StatusBadge("Low Stock (${product.currentStockBaseUnits.toInt()})", AmberWarning)
                } else {
                    StatusBadge("Stock: ${product.currentStockBaseUnits.toInt()} ${product.baseUnit}", EmeraldSuccess)
                }
            }
        }
    }
}

// Modal: Packaging Form Selector (Sections 20-34)
@Composable
fun PackagingFormSelectorDialog(
    productWithForms: ProductWithForms,
    priceTier: String,
    currency: String,
    onDismiss: () -> Unit,
    onFormSelected: (PackagingFormWithPrices, Double) -> Unit
) {
    val product = productWithForms.product
    val forms = productWithForms.forms
    var selectedForm by remember { mutableStateOf(forms.firstOrNull()) }
    var quantityText by remember { mutableStateOf("1") }

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
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate100
                        )
                        Text(
                            text = "Base Stock Unit: ${product.baseUnit} • Available: ${product.currentStockBaseUnits}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate400
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "SELECT SELLING FORM ($priceTier Tier):",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = CyanPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Forms list (Section 39 UI fix: consistent height, padding, responsive layout)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(forms) { formWithPrices ->
                        val form = formWithPrices.form
                        val isSelected = selectedForm?.form?.id == form.id
                        val formPrice = formWithPrices.getPriceForTier(priceTier)

                        Surface(
                            onClick = { selectedForm = formWithPrices },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Slate850 else Slate900,
                            border = BorderStroke(1.dp, if (isSelected) CyanPrimary else Slate800),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedForm = formWithPrices },
                                        colors = RadioButtonDefaults.colors(selectedColor = CyanPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = form.name,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            color = Slate100
                                        )
                                        Text(
                                            text = "= ${form.quantityInBaseUnits} ${product.baseUnit}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Slate400
                                        )
                                    }
                                }

                                Text(
                                    text = formatMoney(formPrice, currency),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) CyanPrimary else Slate200
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quantity selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Quantity:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Slate200
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val current = quantityText.toDoubleOrNull() ?: 1.0
                                if (current > 1) quantityText = (current - 1).toInt().toString()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate800)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = Slate100)
                        }

                        OutlinedTextField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            modifier = Modifier
                                .width(70.dp)
                                .padding(horizontal = 6.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanPrimary,
                                unfocusedBorderColor = Slate700
                            )
                        )

                        IconButton(
                            onClick = {
                                val current = quantityText.toDoubleOrNull() ?: 1.0
                                quantityText = (current + 1).toInt().toString()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate800)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Slate100)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add to Cart button
                Button(
                    onClick = {
                        val qty = quantityText.toDoubleOrNull() ?: 1.0
                        if (selectedForm != null && qty > 0) {
                            onFormSelected(selectedForm!!, qty)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_add_to_cart_confirm"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                ) {
                    Text("Add to Cart", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Cart Pane (Right pane on tablet, or full screen modal on phone)
@Composable
fun CartPane(
    cartItems: List<CartItem>,
    selectedCustomer: CustomerEntity?,
    currency: String,
    cartTotal: Double,
    heldSalesCount: Int,
    onSelectCustomerClick: () -> Unit,
    onUpdateQuantity: (String, String, Double) -> Unit,
    onUpdatePrice: (String, String, Double) -> Unit,
    onRemoveItem: (String, String) -> Unit,
    onClearCart: () -> Unit,
    onHoldSale: () -> Unit,
    onResumeSale: () -> Unit,
    onCheckoutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Customer row & Hold buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                onClick = onSelectCustomerClick,
                shape = RoundedCornerShape(8.dp),
                color = Slate850,
                border = BorderStroke(1.dp, if (selectedCustomer != null) CyanPrimary else Slate700)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (selectedCustomer != null) CyanPrimary else Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = selectedCustomer?.name ?: "Walk-in Customer",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = if (selectedCustomer != null) Slate100 else Slate400
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (heldSalesCount > 0) {
                    OutlinedButton(
                        onClick = onResumeSale,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning)
                    ) {
                        Text("Resume ($heldSalesCount)", fontSize = 11.sp)
                    }
                }

                if (cartItems.isNotEmpty()) {
                    IconButton(onClick = onHoldSale, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Pause, contentDescription = "Hold Sale", tint = CyanPrimary, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onClearCart, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = RoseError, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = Slate800)
        Spacer(modifier = Modifier.height(8.dp))

        // Cart Items List
        if (cartItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Slate700, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cart is empty", style = MaterialTheme.typography.bodyMedium, color = Slate400)
                    Text("Tap products from catalog to add", style = MaterialTheme.typography.bodySmall, color = Slate700)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate850),
                        border = BorderStroke(1.dp, Slate800)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = Slate100
                                    )
                                    Text(
                                        text = "Form: ${item.formName} @ ${formatMoney(item.unitPrice, currency)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyanPrimary
                                    )
                                }

                                Text(
                                    text = formatMoney(item.total, currency),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Slate100
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quantity controls & Remove
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateQuantity(item.productId, item.formId, item.quantity - 1) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate800)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, tint = Slate200, modifier = Modifier.size(14.dp))
                                    }

                                    Text(
                                        text = "%.0f".format(item.quantity),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Slate100,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    )

                                    IconButton(
                                        onClick = { onUpdateQuantity(item.productId, item.formId, item.quantity + 1) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Slate800)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, tint = Slate200, modifier = Modifier.size(14.dp))
                                    }
                                }

                                IconButton(
                                    onClick = { onRemoveItem(item.productId, item.formId) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Slate400, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Summary & Pay Button
        if (cartItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Slate800)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Due", style = MaterialTheme.typography.titleMedium, color = Slate400)
                Text(
                    text = formatMoney(cartTotal, currency),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Slate100
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onCheckoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("pos_checkout_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
            ) {
                Text("Complete Sale", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// Modal: Payment Checkout Dialog (Section 17: Cash, M-Pesa, Credit, Partial Split Validation)
@Composable
fun PaymentCheckoutDialog(
    totalAmount: Double,
    selectedCustomer: CustomerEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onCompleteSale: (PaymentSplit) -> Unit
) {
    var cashInput by remember { mutableStateOf(totalAmount.toString()) }
    var mpesaInput by remember { mutableStateOf("0") }
    var mpesaRef by remember { mutableStateOf("") }
    var creditInput by remember { mutableStateOf("0") }

    val cashVal = cashInput.toDoubleOrNull() ?: 0.0
    val mpesaVal = mpesaInput.toDoubleOrNull() ?: 0.0
    val creditVal = creditInput.toDoubleOrNull() ?: 0.0
    val totalPaid = cashVal + mpesaVal + creditVal
    val difference = totalAmount - totalPaid
    val isBalanced = kotlin.math.abs(difference) <= 0.05

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
                    Text(
                        text = "Take Payment",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Slate100
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                // Total Box
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = Slate850,
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL AMOUNT DUE", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        Text(
                            text = formatMoney(totalAmount, currency),
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyanPrimary
                        )
                    }
                }

                // Quick Mode Buttons (All Cash / All M-Pesa / All Credit)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            cashInput = totalAmount.toString()
                            mpesaInput = "0"
                            creditInput = "0"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate100)
                    ) {
                        Text("Cash", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            cashInput = "0"
                            mpesaInput = totalAmount.toString()
                            creditInput = "0"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldSuccess)
                    ) {
                        Text("M-Pesa", fontSize = 12.sp)
                    }
                    if (selectedCustomer != null) {
                        OutlinedButton(
                            onClick = {
                                cashInput = "0"
                                mpesaInput = "0"
                                creditInput = totalAmount.toString()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberWarning)
                        ) {
                            Text("Credit", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Payment Inputs (Section 17: Partial Split validation)
                OutlinedTextField(
                    value = cashInput,
                    onValueChange = { cashInput = it },
                    label = { Text("Cash Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = mpesaInput,
                        onValueChange = { mpesaInput = it },
                        label = { Text("M-Pesa Amount") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = mpesaRef,
                        onValueChange = { mpesaRef = it },
                        label = { Text("Ref Code") },
                        modifier = Modifier.weight(0.9f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                if (selectedCustomer != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = creditInput,
                        onValueChange = { creditInput = it },
                        label = { Text("Credit (${selectedCustomer.name})") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Validation balance status
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBalanced) EmeraldDark.copy(alpha = 0.4f) else RoseError.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, if (isBalanced) EmeraldSuccess else RoseError)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isBalanced) "Payment Balanced" else if (difference > 0) "Remaining: ${formatMoney(difference, currency)}" else "Overpaid: ${formatMoney(-difference, currency)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isBalanced) EmeraldSuccess else RoseError
                        )
                        Text(
                            text = "Total Paid: ${formatMoney(totalPaid, currency)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Slate100
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isBalanced) {
                            val split = PaymentSplit(
                                cashAmount = cashVal,
                                mpesaAmount = mpesaVal,
                                mpesaReference = mpesaRef,
                                creditAmount = creditVal,
                                customerId = selectedCustomer?.id,
                                customerName = selectedCustomer?.name ?: "Walk-in Customer"
                            )
                            onCompleteSale(split)
                        }
                    },
                    enabled = isBalanced,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pos_confirm_payment_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                ) {
                    Text("Confirm Payment & Print", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// Modal: Customer Picker
@Composable
fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    selectedCustomer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSelect: (CustomerEntity?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(customers, query) {
        customers.filter {
            query.isBlank() || it.name.contains(query, ignoreCase = true) || it.phone.contains(query)
        }
    }

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
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Customer", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Slate100)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                    }
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search name or phone...", color = Slate400) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Walk-in option
                Surface(
                    onClick = { onSelect(null) },
                    shape = RoundedCornerShape(8.dp),
                    color = if (selectedCustomer == null) Slate800 else Slate850,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonOutline, contentDescription = null, tint = CyanPrimary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Walk-in Customer (Default)", fontWeight = FontWeight.SemiBold, color = Slate100)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { c ->
                        Surface(
                            onClick = { onSelect(c) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedCustomer?.id == c.id) Slate800 else Slate850,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(c.name, fontWeight = FontWeight.SemiBold, color = Slate100)
                                    Text(c.phone.ifBlank { "No phone" }, style = MaterialTheme.typography.bodySmall, color = Slate400)
                                }
                                StatusBadge(c.segment, if (c.segment == "Wholesale") BlueInfo else CyanPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
