package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.BusinessProfileEntity
import com.example.data.local.entity.CashShiftEntity
import com.example.data.local.entity.SaleEntity
import com.example.domain.model.DashboardStats
import com.example.ui.components.BottomNavDestination
import com.example.ui.components.MetricCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.formatMoney
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    stats: DashboardStats,
    businessProfile: BusinessProfileEntity?,
    shift: CashShiftEntity?,
    recentSales: List<SaleEntity>,
    onNavigate: (BottomNavDestination) -> Unit,
    onStartShift: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currency = businessProfile?.currency ?: "KSh"
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Slate950),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp,
            bottom = 100.dp // Leave clear space for floating pill navigation
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top App Bar / Business Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = businessProfile?.name ?: "RG POS",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Slate100
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(CyanPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "OFFLINE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp
                                ),
                                color = CyanPrimary
                            )
                        }
                    }
                    Text(
                        text = "Sell. Track. Grow. Offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Slate400
                    )
                }

                // Quick POS action
                Button(
                    onClick = { onNavigate(BottomNavDestination.POS) },
                    modifier = Modifier.testTag("home_pos_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanPrimary,
                        contentColor = Slate950
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PointOfSale,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open POS", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Shift Status Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate900),
                border = BorderStroke(1.dp, if (shift != null) Slate700 else AmberDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (shift != null) EmeraldSuccess else AmberWarning)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (shift != null) "Shift Active (${shift.staffName})" else "No Active Shift",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Slate100
                            )
                            Text(
                                text = if (shift != null) "Opening: ${formatMoney(shift.openingCash, currency)}" else "Start a shift to track cash variance",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate400
                            )
                        }
                    }

                    if (shift == null) {
                        Button(
                            onClick = onStartShift,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanPrimary),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Start Shift", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Primary Metrics Row (Section 15: Today's sales, profit, transactions)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Today's Sales",
                    value = formatMoney(stats.todaySales, currency),
                    subtitle = "${stats.todayTransactions} transactions",
                    icon = Icons.Default.TrendingUp,
                    iconTint = CyanPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Gross Profit",
                    value = formatMoney(stats.todayProfit, currency),
                    subtitle = "Real cost calculation",
                    icon = Icons.Default.MonetizationOn,
                    iconTint = EmeraldSuccess,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Secondary Metrics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Cash In Drawer",
                    value = formatMoney(stats.cashInDrawer, currency),
                    subtitle = "Net cash position",
                    icon = Icons.Default.Payments,
                    iconTint = Slate200,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Customer Credit",
                    value = formatMoney(stats.outstandingCredit, currency),
                    subtitle = "Outstanding owed",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconTint = AmberWarning,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Inventory Alerts Banner (Low Stock & Out of Stock)
        if (stats.lowStockCount > 0 || stats.outOfStockCount > 0) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, if (stats.outOfStockCount > 0) RoseError.copy(alpha = 0.5f) else AmberWarning.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (stats.outOfStockCount > 0) RoseError else AmberWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Inventory Notice",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Slate100
                                )
                                Text(
                                    text = "${stats.outOfStockCount} out of stock, ${stats.lowStockCount} low stock items",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }

                        TextButton(onClick = { onNavigate(BottomNavDestination.INVENTORY) }) {
                            Text("Review", color = CyanPrimary)
                        }
                    }
                }
            }
        }

        // Recent Sales List Header
        item {
            SectionHeader(
                title = "RECENT TRANSACTIONS",
                actionText = "Open POS",
                onActionClick = { onNavigate(BottomNavDestination.POS) }
            )
        }

        if (recentSales.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    border = BorderStroke(1.dp, Slate800)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Slate700,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No transactions yet today",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate400
                            )
                        }
                    }
                }
            }
        } else {
            items(recentSales.take(5)) { sale ->
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CyanPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = CyanPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = sale.customerName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Slate100
                                )
                                Text(
                                    text = "${sale.saleNumber} • ${timeFormat.format(Date(sale.timestamp))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate400
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = formatMoney(sale.totalAmount, currency),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Slate100
                            )
                            StatusBadge(
                                text = sale.customerSegment,
                                color = if (sale.customerSegment == "Wholesale") BlueInfo else EmeraldSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}
