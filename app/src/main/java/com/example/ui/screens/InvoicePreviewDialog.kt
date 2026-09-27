package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.BillEntity
import com.example.data.model.BusinessSettingsEntity
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StatusOverdueBg
import com.example.ui.theme.StatusOverdueText
import com.example.ui.theme.StatusPaidBg
import com.example.ui.theme.StatusPaidText
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingText
import com.example.util.CurrencyFormatter
import com.example.util.PdfPrintHelper

@Composable
fun InvoicePreviewDialog(
    bill: BillEntity,
    settings: BusinessSettingsEntity,
    onDismiss: () -> Unit,
    onEdit: (BillEntity) -> Unit
) {
    val context = LocalContext.current
    val items = bill.getItems()
    val currency = settings.currency

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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .testTag("invoice_preview_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Receipt Preview",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_preview_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close preview")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Invoice Sheet
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .background(Color(0xFFFCFDFF), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    // Business & Receipt Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settings.name.ifBlank { "PRD Store" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandPrimary
                            )
                            if (settings.address.isNotBlank()) {
                                Text(
                                    text = settings.address,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 15.sp
                                )
                            }
                            if (settings.phone.isNotBlank()) {
                                Text(
                                    text = "Phone: ${settings.phone}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            if (settings.gstin.isNotBlank()) {
                                Text(
                                    text = "GSTIN: ${settings.gstin}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TAX INVOICE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B)
                            )
                            Text(
                                text = bill.no,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Date: ${bill.date}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(statusBg, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Billed To Box
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "BILLED TO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = bill.customerName.ifBlank { "Walk-in Customer" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                if (bill.customerPhone.isNotBlank()) {
                                    Text(
                                        text = bill.customerPhone,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                if (bill.customerAddress.isNotBlank()) {
                                    Text(
                                        text = bill.customerAddress,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "PAYMENT METHOD",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = bill.paymentMethod,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Items Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(text = "#", modifier = Modifier.width(24.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Text(text = "Item", modifier = Modifier.weight(1f), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                        Text(text = "Qty", modifier = Modifier.width(36.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xFF475569))
                        Text(text = "Rate", modifier = Modifier.width(60.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = Color(0xFF475569))
                        Text(text = "Total", modifier = Modifier.width(70.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = Color(0xFF475569))
                    }

                    // Item rows
                    items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${index + 1}", modifier = Modifier.width(24.dp), fontSize = 12.sp, color = Color(0xFF64748B))
                            Text(text = item.name, modifier = Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            val qtyText = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString()
                            Text(text = qtyText, modifier = Modifier.width(36.dp), fontSize = 12.sp, textAlign = TextAlign.Center, color = Color(0xFF0F172A))
                            Text(text = CurrencyFormatter.format(item.rate, currency), modifier = Modifier.width(60.dp), fontSize = 12.sp, textAlign = TextAlign.End, color = Color(0xFF475569))
                            Text(text = CurrencyFormatter.format(item.amount, currency), modifier = Modifier.width(70.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, color = Color(0xFF0F172A))
                        }
                        if (index < items.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                        }
                    }

                    HorizontalDivider(color = Color(0xFFCBD5E1), modifier = Modifier.padding(vertical = 8.dp))

                    // Totals
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.width(240.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subtotal:", fontSize = 13.sp, color = Color(0xFF64748B))
                            Text(text = CurrencyFormatter.format(bill.subtotal, currency), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                        }

                        if (bill.gstPercent > 0 || bill.gstAmount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.width(240.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "GST (${bill.gstPercent}%):", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(text = CurrencyFormatter.format(bill.gstAmount, currency), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A))
                            }
                        }

                        if (bill.discount > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.width(240.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Discount:", fontSize = 13.sp, color = Color(0xFF64748B))
                                Text(text = "- ${CurrencyFormatter.format(bill.discount, currency)}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFF0F172A), thickness = 1.5.dp, modifier = Modifier.width(240.dp))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.width(240.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Grand Total:", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                            Text(text = CurrencyFormatter.format(bill.total, currency), fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = BrandPrimary)
                        }
                    }

                    // Notes & Footer
                    if (bill.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Note: ${bill.notes}",
                                fontSize = 11.sp,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = settings.footerNotes.ifBlank { "Thank you for your business!" },
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons: Print PDF & Share & Edit
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            PdfPrintHelper.printInvoice(context, bill, settings)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("print_invoice_btn")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Print / PDF", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            PdfPrintHelper.shareReceiptAsText(context, bill, settings)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_invoice_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Share", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onEdit(bill)
                        },
                        modifier = Modifier.testTag("edit_from_preview_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
