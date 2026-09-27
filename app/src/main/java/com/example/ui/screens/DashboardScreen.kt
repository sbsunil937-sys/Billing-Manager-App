package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.firebase.AuthState
import com.example.data.model.BillEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
import com.example.ui.theme.BrandSecondary
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusOverdueText
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingText
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.BillingViewModel
import com.example.util.CurrencyFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: BillingViewModel,
    onNavigateToTab: (AppTab) -> Unit,
    onOpenCustomerDialog: () -> Unit,
    onOpenProductDialog: () -> Unit,
    onOpenAuthDialog: () -> Unit
) {
    val bills by viewModel.bills.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val authState by viewModel.authState.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val totalSales = bills.sumOf { it.total }
    val paidSales = bills.filter { it.status == "Paid" }.sumOf { it.total }
    val pendingSales = bills.filter { it.status != "Paid" }.sumOf { it.total }
    val totalBillsCount = bills.size

    val recentBills = bills.sortedByDescending { it.createdAt }.take(6)
    val todayFormatted = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Business Welcome Banner
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings.name.ifBlank { "PRD Store" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandPrimary
                            )
                            Text(
                                text = todayFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Cloud status pill
                        Surface(
                            shape = CircleShape,
                            color = when (authState) {
                                is AuthState.Authenticated -> Color(0xFFDCFCE7)
                                else -> Color(0xFFF1F5F9)
                            },
                            modifier = Modifier
                                .clickable { onOpenAuthDialog() }
                                .testTag("cloud_status_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        strokeWidth = 2.dp,
                                        color = BrandPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = when (authState) {
                                            is AuthState.Authenticated -> Icons.Default.CloudDone
                                            else -> Icons.Default.CloudSync
                                        },
                                        contentDescription = null,
                                        tint = when (authState) {
                                            is AuthState.Authenticated -> Color(0xFF16A34A)
                                            else -> Color(0xFF64748B)
                                        },
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = when (val a = authState) {
                                        is AuthState.Authenticated -> if (a.isAnonymous) "Guest Sync" else "Cloud Active"
                                        else -> "Connect Cloud"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (authState) {
                                        is AuthState.Authenticated -> Color(0xFF166534)
                                        else -> Color(0xFF475569)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hero Graphic Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrandPrimary, BrandPrimaryVariant)
                                )
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_dashboard_hero_1790496035713),
                            contentDescription = "Billing management banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = 0.85f
                        )

                        // Overlay Text
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color(0xCC0B1220))
                                    )
                                )
                                .padding(14.dp),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "Smart POS & Receipt Billing",
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Fast GST calculations, PDF invoices & Firestore sync",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 11.sp
                                    )
                                }

                                Button(
                                    onClick = { onNavigateToTab(AppTab.CREATE_BILL) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("hero_new_bill_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "New Bill", color = BrandPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Stats Cards Grid (4 KPI Cards matching the PRD)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Total Sales",
                        value = CurrencyFormatter.format(totalSales, settings.currency),
                        icon = Icons.Default.Receipt,
                        iconBg = Color(0xFFEEF2FF),
                        iconTint = BrandPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Paid",
                        value = CurrencyFormatter.format(paidSales, settings.currency),
                        icon = Icons.Default.CheckCircle,
                        iconBg = Color(0xFFDCFCE7),
                        iconTint = Color(0xFF16A34A),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Pending / Overdue",
                        value = CurrencyFormatter.format(pendingSales, settings.currency),
                        icon = Icons.Default.HourglassBottom,
                        iconBg = Color(0xFFFEF3C7),
                        iconTint = Color(0xFFD97706),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Bills",
                        value = totalBillsCount.toString(),
                        icon = Icons.Default.Inventory,
                        iconBg = Color(0xFFF3E8FF),
                        iconTint = Color(0xFF7C3AED),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onNavigateToTab(AppTab.CREATE_BILL) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("quick_new_bill_btn")
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Bill", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenCustomerDialog,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("quick_add_customer_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Customer", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenProductDialog,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("quick_add_product_btn")
                        ) {
                            Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Product", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.triggerSync() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("quick_sync_btn")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Cloud", fontSize = 12.sp)
                        }

                        if (bills.isEmpty()) {
                            Button(
                                onClick = { viewModel.loadSampleData() },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("load_sample_data_btn")
                            ) {
                                Text("Load Sample Store Data", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Recent Bills Section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Bills",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { onNavigateToTab(AppTab.BILLS) },
                    modifier = Modifier.testTag("view_all_bills_btn")
                ) {
                    Text("View all", color = BrandPrimary, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (recentBills.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🧾", fontSize = 36.sp)
                        Text(
                            text = "No bills created yet",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Create your first bill or load sample store data to see receipts, reports and calculations in action.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onNavigateToTab(AppTab.CREATE_BILL) },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                            ) {
                                Text("Create Bill")
                            }
                            OutlinedButton(
                                onClick = { viewModel.loadSampleData() }
                            ) {
                                Text("Load Sample Data")
                            }
                        }
                    }
                }
            }
        } else {
            items(recentBills) { bill ->
                RecentBillItem(
                    bill = bill,
                    currency = settings.currency,
                    onClick = { viewModel.showInvoicePreview(bill) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bar
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun RecentBillItem(
    bill: BillEntity,
    currency: String,
    onClick: () -> Unit
) {
    val statusBg = when (bill.status) {
        "Paid" -> StatusPaidBg
        "Pending" -> StatusPendingBg
        else -> StatusOverdueBg
    }
    val statusText = when (bill.status) {
        "Paid" -> StatusPaidText
        "Pending" -> StatusPendingText
        else -> StatusOverdueText
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("recent_bill_${bill.no}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bill.no,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = bill.customerName.ifBlank { "Walk-in Customer" },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = bill.date,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = CurrencyFormatter.format(bill.total, currency),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = BrandPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .background(statusBg, CircleShape)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = bill.status,
                        color = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
