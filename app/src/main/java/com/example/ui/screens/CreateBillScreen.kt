package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BillEntity
import com.example.data.model.BillItem
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.viewmodel.BillingViewModel
import com.example.util.CurrencyFormatter
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateBillScreen(
    viewModel: BillingViewModel,
    onOpenCustomerDialog: () -> Unit
) {
    val context = LocalContext.current
    val form by viewModel.billForm.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var statusDropdownExpanded by remember { mutableStateOf(false) }
    var methodDropdownExpanded by remember { mutableStateOf(false) }

    // Date picker dialog
    val calendar = Calendar.getInstance()
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                viewModel.updateBillForm { it.copy(date = dateStr) }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("create_bill_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (form.editId != null) "Edit Bill" else "Create New Bill",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { viewModel.initNewBillForm() },
                    modifier = Modifier.testTag("reset_bill_btn")
                ) {
                    Text("Reset Form", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Bill Header Info Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bill No & Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = form.billNo,
                            onValueChange = { viewModel.updateBillForm { f -> f.copy(billNo = it) } },
                            label = { Text("Bill / Receipt No.") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_no_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = form.date,
                            onValueChange = { viewModel.updateBillForm { f -> f.copy(date = it) } },
                            label = { Text("Date") },
                            trailingIcon = {
                                IconButton(onClick = { datePickerDialog.show() }) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = "Pick date")
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_date_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // Customer Selection
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = form.customerName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer") },
                            trailingIcon = {
                                IconButton(onClick = { customerDropdownExpanded = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select customer")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_select_field"),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { customerDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = customerDropdownExpanded,
                            onDismissRequest = { customerDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Walk-in Customer (Default)") },
                                onClick = {
                                    customerDropdownExpanded = false
                                    viewModel.selectCustomerForBill(null)
                                }
                            )
                            HorizontalDivider()
                            customers.forEach { c ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(c.name, fontWeight = FontWeight.SemiBold)
                                            if (c.phone.isNotBlank()) {
                                                Text(c.phone, fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }
                                    },
                                    onClick = {
                                        customerDropdownExpanded = false
                                        viewModel.selectCustomerForBill(c)
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = BrandPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("+ Add New Customer", color = BrandPrimary, fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    customerDropdownExpanded = false
                                    onOpenCustomerDialog()
                                }
                            )
                        }
                    }

                    // Payment Status & Method Dropdowns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = form.paymentStatus,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status") },
                                trailingIcon = {
                                    IconButton(onClick = { statusDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_status_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { statusDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = statusDropdownExpanded,
                                onDismissRequest = { statusDropdownExpanded = false }
                            ) {
                                listOf("Paid", "Pending", "Overdue").forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text(status) },
                                        onClick = {
                                            statusDropdownExpanded = false
                                            viewModel.updateBillForm { it.copy(paymentStatus = status) }
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = form.paymentMethod,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Method") },
                                trailingIcon = {
                                    IconButton(onClick = { methodDropdownExpanded = true }) {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("payment_method_input"),
                                shape = RoundedCornerShape(10.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { methodDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = methodDropdownExpanded,
                                onDismissRequest = { methodDropdownExpanded = false }
                            ) {
                                listOf("Cash", "UPI", "Card", "Bank Transfer", "Other").forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method) },
                                        onClick = {
                                            methodDropdownExpanded = false
                                            viewModel.updateBillForm { it.copy(paymentMethod = method) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bill Items Header & Shortcut Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Items & Products (${form.items.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = { viewModel.addBillItem() },
                        modifier = Modifier.testTag("add_item_top_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", color = BrandPrimary, fontWeight = FontWeight.Bold)
                    }
                }

                if (products.isNotEmpty()) {
                    Text(
                        text = "Quick add from products catalogue:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        products.take(6).forEach { product ->
                            SuggestionChip(
                                onClick = {
                                    val lastIndex = form.items.indexOfLast { it.name.isBlank() }
                                    if (lastIndex >= 0) {
                                        viewModel.selectProductForItem(lastIndex, product)
                                    } else {
                                        viewModel.addBillItem(BillItem(name = product.name, qty = 1.0, rate = product.price))
                                    }
                                },
                                label = { Text(product.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Bill Items List
        itemsIndexed(form.items) { index, item ->
            ItemRowCard(
                index = index,
                item = item,
                currency = settings.currency,
                onUpdate = { updated -> viewModel.updateBillItem(index, updated) },
                onRemove = { viewModel.removeBillItem(index) }
            )
        }

        item {
            OutlinedButton(
                onClick = { viewModel.addBillItem() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_item_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("+ Add Item")
            }
        }

        // Tax / GST & Discount & Notes Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Taxes & Discount", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = if (form.gstPercent == 0.0) "" else form.gstPercent.toString(),
                            onValueChange = {
                                val v = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateBillForm { f -> f.copy(gstPercent = v) }
                            },
                            label = { Text("Tax / GST %") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("gst_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = if (form.discount == 0.0) "" else form.discount.toString(),
                            onValueChange = {
                                val v = it.toDoubleOrNull() ?: 0.0
                                viewModel.updateBillForm { f -> f.copy(discount = v) }
                            },
                            label = { Text("Discount (${settings.currency})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("discount_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    // GST Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GST Presets:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(0.0, 5.0, 12.0, 18.0, 28.0).forEach { rate ->
                            FilterChip(
                                selected = form.gstPercent == rate,
                                onClick = { viewModel.updateBillForm { it.copy(gstPercent = rate) } },
                                label = { Text("${rate.toInt()}%", fontSize = 10.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = form.notes,
                        onValueChange = { viewModel.updateBillForm { f -> f.copy(notes = it) } },
                        label = { Text("Receipt Notes / Terms") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bill_notes_input"),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }

        // Summary Calculation Card (Matching PRD Summary)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            CurrencyFormatter.format(form.subtotal, settings.currency),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (form.gstPercent > 0 || form.gstAmount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GST (${form.gstPercent}%)", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                CurrencyFormatter.format(form.gstAmount, settings.currency),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (form.discount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Discount", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                "- ${CurrencyFormatter.format(form.discount, settings.currency)}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Grand Total",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            CurrencyFormatter.format(form.grandTotal, settings.currency),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPrimary
                        )
                    }
                }
            }
        }

        // Action Buttons: Preview & Save
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val validItems = form.items.filter { it.name.isNotBlank() && it.qty > 0 }
                        if (validItems.isEmpty()) {
                            viewModel.emitToast("Add at least one item before previewing")
                        } else {
                            val tempBill = BillEntity(
                                id = form.editId ?: "temp_preview",
                                no = form.billNo.ifBlank { "INV-PREVIEW" },
                                date = form.date,
                                customerName = form.customerName,
                                customerPhone = form.customerPhone,
                                customerEmail = form.customerEmail,
                                customerAddress = form.customerAddress,
                                status = form.paymentStatus,
                                paymentMethod = form.paymentMethod,
                                itemsJson = BillItem.toJsonArrayString(validItems),
                                subtotal = form.subtotal,
                                gstPercent = form.gstPercent,
                                gstAmount = form.gstAmount,
                                discount = form.discount,
                                total = form.grandTotal,
                                notes = form.notes
                            )
                            viewModel.showInvoicePreview(tempBill)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("preview_bill_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview")
                }

                Button(
                    onClick = { viewModel.saveCurrentBill() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("save_bill_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Bill", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(90.dp))
        }
    }
}

@Composable
fun ItemRowCard(
    index: Int,
    item: BillItem,
    currency: String,
    onUpdate: (BillItem) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    placeholder = { Text("Product / Item name") },
                    label = { Text("Item ${index + 1}") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_name_$index"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("remove_item_$index")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove item", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = if (item.qty == 0.0) "" else if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString(),
                    onValueChange = {
                        val v = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(qty = v))
                    },
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(0.8f)
                        .testTag("item_qty_$index"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = if (item.rate == 0.0) "" else item.rate.toString(),
                    onValueChange = {
                        val v = it.toDoubleOrNull() ?: 0.0
                        onUpdate(item.copy(rate = v))
                    },
                    label = { Text("Rate") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("item_rate_$index"),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text("Amount", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        CurrencyFormatter.format(item.amount, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandPrimary
                    )
                }
            }
        }
    }
}
