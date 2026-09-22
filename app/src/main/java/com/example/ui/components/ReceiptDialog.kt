package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.BusinessProfileEntity
import com.example.data.local.entity.SaleEntity
import com.example.data.local.entity.SaleItemEntity
import com.example.data.local.entity.SalePaymentEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReceiptDialog(
    sale: SaleEntity,
    items: List<SaleItemEntity>,
    payments: List<SalePaymentEntity>,
    businessProfile: BusinessProfileEntity,
    onDismiss: () -> Unit,
    onPrintSuccess: () -> Unit = {}
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(sale.timestamp))

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Slate900),
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header with success badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sale Completed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Slate100
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Thermal receipt paper container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFAFAFA))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Business Branding
                        Text(
                            text = businessProfile.name.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.Black,
                            textAlign = TextAlign.Center
                        )
                        if (businessProfile.address.isNotBlank()) {
                            Text(
                                text = businessProfile.address,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center
                            )
                        }
                        Text(
                            text = "Tel: ${businessProfile.phone}",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color(0xFF475569),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "--------------------------------",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Receipt metadata
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Receipt: ${sale.saleNumber}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = sale.customerSegment,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF0F766E)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Date: $formattedDate",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF475569)
                            )
                            Text(
                                text = "Cashier: ${sale.staffName}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color(0xFF475569)
                            )
                        }
                        if (sale.customerName.isNotBlank() && sale.customerName != "Walk-in Customer") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Customer: ${sale.customerName}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "--------------------------------",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Column headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ITEM & FORM",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Black,
                                modifier = Modifier.weight(1.8f)
                            )
                            Text(
                                text = "QTY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Black,
                                modifier = Modifier.weight(0.7f),
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "TOTAL",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Black,
                                modifier = Modifier.weight(1.1f),
                                textAlign = TextAlign.End
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Items list (Section 20-30: shows product and packaging form clearly!)
                        items.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.productName,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color.Black,
                                        modifier = Modifier.weight(1.8f)
                                    )
                                    Text(
                                        text = "%.0f".format(item.quantity),
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                        color = Color.Black,
                                        modifier = Modifier.weight(0.7f),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = formatMoney(item.total, businessProfile.currency),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color = Color.Black,
                                        modifier = Modifier.weight(1.1f),
                                        textAlign = TextAlign.End
                                    )
                                }
                                Text(
                                    text = "  Form: ${item.formName} @ ${formatMoney(item.unitPrice, businessProfile.currency)}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "--------------------------------",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Totals
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Black
                            )
                            Text(
                                text = formatMoney(sale.subtotal, businessProfile.currency),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color.Black
                            )
                        }

                        if (sale.discountAmount > 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Discount",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFFDC2626)
                                )
                                Text(
                                    text = "-${formatMoney(sale.discountAmount, businessProfile.currency)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "TOTAL DUE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Black
                            )
                            Text(
                                text = formatMoney(sale.totalAmount, businessProfile.currency),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = Color.Black
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PAYMENT BREAKDOWN",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF475569)
                        )

                        // Payments split (Section 17)
                        payments.forEach { payment ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val refText = if (payment.reference.isNotBlank()) " (${payment.reference})" else ""
                                Text(
                                    text = "${payment.paymentMethod}$refText",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color.Black
                                )
                                Text(
                                    text = formatMoney(payment.amount, businessProfile.currency),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    ),
                                    color = Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = businessProfile.receiptFooter,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Powered by RG POS — Offline",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons (Print / Share / Done)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onPrintSuccess() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("receipt_print_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanPrimary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("receipt_done_button"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanPrimary, contentColor = Slate950)
                    ) {
                        Text("Next Sale", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
