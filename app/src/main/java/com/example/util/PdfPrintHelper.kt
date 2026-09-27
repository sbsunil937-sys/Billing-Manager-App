package com.example.util

import android.content.Context
import android.content.Intent
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.model.BillEntity
import com.example.data.model.BusinessSettingsEntity

object PdfPrintHelper {

    fun generateInvoiceHtml(bill: BillEntity, settings: BusinessSettingsEntity): String {
        val currency = settings.currency
        val items = bill.getItems()

        val itemsHtml = StringBuilder()
        items.forEachIndexed { index, item ->
            itemsHtml.append("""
                <tr>
                    <td style="padding:10px;border-bottom:1px solid #e5e7eb;text-align:center;">${index + 1}</td>
                    <td style="padding:10px;border-bottom:1px solid #e5e7eb;"><b>${escapeHtml(item.name)}</b></td>
                    <td style="padding:10px;border-bottom:1px solid #e5e7eb;text-align:center;">${if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString()}</td>
                    <td style="padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;">${CurrencyFormatter.format(item.rate, currency)}</td>
                    <td style="padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;"><b>${CurrencyFormatter.format(item.amount, currency)}</b></td>
                </tr>
            """.trimIndent())
        }

        val statusColor = when (bill.status) {
            "Paid" -> "#166534"
            "Pending" -> "#92400e"
            else -> "#991b1b"
        }
        val statusBg = when (bill.status) {
            "Paid" -> "#dcfce7"
            "Pending" -> "#fef3c7"
            else -> "#fee2e2"
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Receipt ${escapeHtml(bill.no)}</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif;
                        color: #1e293b;
                        margin: 0;
                        padding: 24px;
                        background: #ffffff;
                    }
                    .header {
                        display: flex;
                        justify-content: space-between;
                        border-bottom: 2px solid #0f172a;
                        padding-bottom: 18px;
                        margin-bottom: 24px;
                    }
                    .biz-title {
                        font-size: 24px;
                        font-weight: 800;
                        color: #4f46e5;
                        margin: 0 0 6px 0;
                    }
                    .biz-info {
                        font-size: 13px;
                        color: #64748b;
                        line-height: 1.5;
                        margin: 0;
                    }
                    .inv-title {
                        text-align: right;
                    }
                    .inv-badge {
                        font-size: 22px;
                        font-weight: 800;
                        color: #0f172a;
                        margin: 0 0 4px 0;
                    }
                    .bill-to-box {
                        background: #f8fafc;
                        border: 1px solid #e2e8f0;
                        border-radius: 12px;
                        padding: 16px;
                        margin-bottom: 24px;
                        display: flex;
                        justify-content: space-between;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 24px;
                    }
                    th {
                        background: #f1f5f9;
                        padding: 10px;
                        font-size: 12px;
                        text-transform: uppercase;
                        letter-spacing: 0.05em;
                        color: #475569;
                    }
                    .totals {
                        width: 320px;
                        margin-left: auto;
                        border-top: 1px solid #e2e8f0;
                        padding-top: 12px;
                    }
                    .totals-row {
                        display: flex;
                        justify-content: space-between;
                        padding: 6px 0;
                        font-size: 14px;
                    }
                    .grand-total {
                        font-size: 18px;
                        font-weight: 800;
                        color: #4f46e5;
                        border-top: 2px solid #0f172a;
                        padding-top: 10px;
                        margin-top: 8px;
                    }
                    .badge {
                        display: inline-block;
                        padding: 4px 10px;
                        border-radius: 999px;
                        font-size: 12px;
                        font-weight: 700;
                        background: $statusBg;
                        color: $statusColor;
                    }
                    .footer {
                        margin-top: 40px;
                        padding-top: 16px;
                        border-top: 1px dashed #cbd5e1;
                        text-align: center;
                        font-size: 13px;
                        color: #64748b;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div>
                        <div class="biz-title">${escapeHtml(settings.name.ifBlank { "PRD Store" })}</div>
                        <p class="biz-info">
                            ${if (settings.address.isNotBlank()) escapeHtml(settings.address) + "<br>" else ""}
                            ${if (settings.phone.isNotBlank()) "Phone: " + escapeHtml(settings.phone) + "<br>" else ""}
                            ${if (settings.email.isNotBlank()) "Email: " + escapeHtml(settings.email) + "<br>" else ""}
                            ${if (settings.gstin.isNotBlank()) "<b>GSTIN:</b> " + escapeHtml(settings.gstin) else ""}
                        </p>
                    </div>
                    <div class="inv-title">
                        <div class="inv-badge">TAX INVOICE</div>
                        <div style="font-size:16px;font-weight:700;">${escapeHtml(bill.no)}</div>
                        <div style="font-size:13px;color:#64748b;margin-top:4px;">Date: ${escapeHtml(bill.date)}</div>
                        <div style="margin-top:8px;"><span class="badge">${bill.status.uppercase()}</span></div>
                    </div>
                </div>

                <div class="bill-to-box">
                    <div>
                        <div style="font-size:11px;text-transform:uppercase;color:#64748b;font-weight:700;margin-bottom:4px;">Billed To:</div>
                        <div style="font-size:16px;font-weight:700;">${escapeHtml(bill.customerName.ifBlank { "Walk-in Customer" })}</div>
                        ${if (bill.customerPhone.isNotBlank()) "<div style='font-size:13px;color:#64748b;'>Phone: " + escapeHtml(bill.customerPhone) + "</div>" else ""}
                        ${if (bill.customerEmail.isNotBlank()) "<div style='font-size:13px;color:#64748b;'>Email: " + escapeHtml(bill.customerEmail) + "</div>" else ""}
                        ${if (bill.customerAddress.isNotBlank()) "<div style='font-size:13px;color:#64748b;'>" + escapeHtml(bill.customerAddress) + "</div>" else ""}
                    </div>
                    <div style="text-align:right;">
                        <div style="font-size:11px;text-transform:uppercase;color:#64748b;font-weight:700;margin-bottom:4px;">Payment Method:</div>
                        <div style="font-size:14px;font-weight:600;">${escapeHtml(bill.paymentMethod)}</div>
                    </div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th style="width:40px;text-align:center;">#</th>
                            <th style="text-align:left;">Item & Description</th>
                            <th style="width:80px;text-align:center;">Qty</th>
                            <th style="width:110px;text-align:right;">Rate</th>
                            <th style="width:120px;text-align:right;">Amount</th>
                        </tr>
                    </thead>
                    <tbody>
                        $itemsHtml
                    </tbody>
                </table>

                <div class="totals">
                    <div class="totals-row">
                        <span style="color:#64748b;">Subtotal:</span>
                        <b>${CurrencyFormatter.format(bill.subtotal, currency)}</b>
                    </div>
                    ${if (bill.gstPercent > 0 || bill.gstAmount > 0) """
                    <div class="totals-row">
                        <span style="color:#64748b;">GST (${bill.gstPercent}%):</span>
                        <b>${CurrencyFormatter.format(bill.gstAmount, currency)}</b>
                    </div>
                    """ else ""}
                    ${if (bill.discount > 0) """
                    <div class="totals-row">
                        <span style="color:#64748b;">Discount:</span>
                        <b style="color:#dc2626;">- ${CurrencyFormatter.format(bill.discount, currency)}</b>
                    </div>
                    """ else ""}
                    <div class="totals-row grand-total">
                        <span>Grand Total:</span>
                        <span>${CurrencyFormatter.format(bill.total, currency)}</span>
                    </div>
                </div>

                ${if (bill.notes.isNotBlank()) """
                <div style="margin-top:24px;padding:12px;background:#f8fafc;border-radius:8px;font-size:13px;color:#475569;">
                    <b>Notes:</b> ${escapeHtml(bill.notes)}
                </div>
                """ else ""}

                <div class="footer">
                    <div>${escapeHtml(settings.footerNotes.ifBlank { "Thank you for your business!" })}</div>
                    <div style="font-size:11px;margin-top:4px;color:#94a3b8;">Generated via PRD Receipt & Bill Manager</div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    fun printInvoice(context: Context, bill: BillEntity, settings: BusinessSettingsEntity) {
        val html = generateInvoiceHtml(bill, settings)
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                val printAdapter = webView.createPrintDocumentAdapter("Receipt_${bill.no}")
                printManager?.print("Receipt_${bill.no}", printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun shareReceiptAsText(context: Context, bill: BillEntity, settings: BusinessSettingsEntity) {
        val currency = settings.currency
        val sb = StringBuilder()
        sb.appendLine("🧾 *RECEIPT / BILL*")
        sb.appendLine("*${settings.name.ifBlank { "PRD Store" }}*")
        if (settings.phone.isNotBlank()) sb.appendLine("📞 ${settings.phone}")
        if (settings.gstin.isNotBlank()) sb.appendLine("GSTIN: ${settings.gstin}")
        sb.appendLine("--------------------------------")
        sb.appendLine("Bill No: ${bill.no}")
        sb.appendLine("Date: ${bill.date}")
        sb.appendLine("Customer: ${bill.customerName}")
        sb.appendLine("Payment: ${bill.paymentMethod} (${bill.status})")
        sb.appendLine("--------------------------------")
        sb.appendLine("*Items:*")
        bill.getItems().forEachIndexed { index, item ->
            val qtyStr = if (item.qty % 1.0 == 0.0) item.qty.toInt().toString() else item.qty.toString()
            sb.appendLine("${index + 1}. ${item.name}")
            sb.appendLine("   $qtyStr x ${CurrencyFormatter.format(item.rate, currency)} = ${CurrencyFormatter.format(item.amount, currency)}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("Subtotal: ${CurrencyFormatter.format(bill.subtotal, currency)}")
        if (bill.gstPercent > 0) sb.appendLine("GST (${bill.gstPercent}%): ${CurrencyFormatter.format(bill.gstAmount, currency)}")
        if (bill.discount > 0) sb.appendLine("Discount: -${CurrencyFormatter.format(bill.discount, currency)}")
        sb.appendLine("*Grand Total: ${CurrencyFormatter.format(bill.total, currency)}*")
        if (bill.notes.isNotBlank()) sb.appendLine("\nNote: ${bill.notes}")
        sb.appendLine("\n${settings.footerNotes}")

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, sb.toString())
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Receipt ${bill.no}"))
    }

    private fun escapeHtml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
}
