package com.example.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun format(amount: Double, symbol: String = "₹"): String {
        return try {
            val format = NumberFormat.getNumberInstance(Locale("en", "IN"))
            format.minimumFractionDigits = 2
            format.maximumFractionDigits = 2
            "$symbol${format.format(amount)}"
        } catch (e: Exception) {
            String.format(Locale.US, "%s%.2f", symbol, amount)
        }
    }
}
