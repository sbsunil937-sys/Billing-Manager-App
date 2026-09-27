package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusOverdueText
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingText
import com.example.ui.viewmodel.BillingViewModel
import com.example.util.CurrencyFormatter
import com.example.util.PdfPrintHelper

@Composable
fun BillsScreen(
    viewModel: BillingViewModel,
    onCreateNewBill: () -> Unit
) {
    val context = LocalContext.current
    val filteredBills by viewModel.filteredBills.collectAsState()
    val searchQuery by viewModel.billSearchQuery.collectAsState()
    val statusFilter by viewModel.billStatusFilter.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var billToDelete by remember { mutableStateOf<BillEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNewBill,
                containerColor = BrandPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(bottom = 60.dp)
                    .testTag("fab_create_bill")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create new bill")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .testTag("bills_screen")
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.billSearchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bill_search_input"),
                placeholder = { Text("Search bill no, customer name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.billSearchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Status filter chips (All, Paid, Pending, Overdue)
            val filters = listOf("All", "Paid", "Pending", "Overdue")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters) { filter ->
                    val isSelected = statusFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.billStatusFilter.value = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("filter_chip_$filter")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredBills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "🧾", fontSize = 42.sp)
                        Text(
                            text = if (searchQuery.isNotBlank() || statusFilter != "All") "No matching bills found" else "No bills created yet",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (searchQuery.isNotBlank() || statusFilter != "All") "Try adjusting your search or filters" else "Tap '+' to create your first receipt",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (searchQuery.isNotBlank() || statusFilter != "All") {
                            TextButton(onClick = {
                                viewModel.billSearchQuery.value = ""
                                viewModel.billStatusFilter.value = "All"
                            }) {
                                Text("Reset Filters")
                            }
                        } else {
                            Button(
                                onClick = onCreateNewBill,
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Create Bill")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredBills, key = { it.id }) { bill ->
                        BillCardItem(
                            bill = bill,
                            currency = settings.currency,
                            onPreview = { viewModel.showInvoicePreview(bill) },
                            onEdit = { viewModel.populateBillForEdit(bill) },
                            onPrint = { PdfPrintHelper.printInvoice(context, bill, settings) },
                            onShare = { PdfPrintHelper.shareReceiptAsText(context, bill, settings) },
                            onUpdateStatus = { newStatus -> viewModel.updateBillStatus(bill, newStatus) },
                            onDelete = { billToDelete = bill }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    billToDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text("Delete Bill ${b.no}?") },
            text = { Text("Are you sure you want to delete this bill for ${b.customerName}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBill(b.id)
                        billToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BillCardItem(
    bill: BillEntity,
    currency: String,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onPrint: () -> Unit,
    onShare: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

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

    val items = bill.getItems()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPreview() }
            .testTag("bill_card_${bill.no}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = bill.no,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = bill.date,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(statusBg, CircleShape)
                            .padding(horizontal = 9.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = bill.status,
                            color = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("bill_menu_btn_${bill.no}")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More actions")
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("View & Print Receipt") },
                                onClick = {
                                    menuExpanded = false
                                    onPreview()
                                },
                                leadingIcon = { Icon(Icons.Default.Print, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Edit Bill") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Receipt Text") },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (bill.status == "Paid") "Mark as Pending" else "Mark as Paid") },
                                onClick = {
                                    menuExpanded = false
                                    onUpdateStatus(if (bill.status == "Paid") "Pending" else "Paid")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Bill", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.customerName.ifBlank { "Walk-in Customer" },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${items.size} item${if (items.size != 1) "s" else ""} • ${bill.paymentMethod}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = CurrencyFormatter.format(bill.total, currency),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BrandPrimary
                )
            }
        }
    }
}
