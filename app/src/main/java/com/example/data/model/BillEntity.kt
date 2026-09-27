package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val no: String = "",
    val date: String = "",
    val customerId: String = "",
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val status: String = "Paid", // Paid, Pending, Overdue
    val paymentMethod: String = "Cash", // Cash, UPI, Card, Bank Transfer, Other
    val itemsJson: String = "[]",
    val subtotal: Double = 0.0,
    val gstPercent: Double = 0.0,
    val gstAmount: Double = 0.0,
    val discount: Double = 0.0,
    val total: Double = 0.0,
    val notes: String = "Thank you for your business",
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    fun getItems(): List<BillItem> = BillItem.parseList(itemsJson)

    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "no" to no,
            "date" to date,
            "customerId" to customerId,
            "customerName" to customerName,
            "customerPhone" to customerPhone,
            "customerEmail" to customerEmail,
            "customerAddress" to customerAddress,
            "status" to status,
            "paymentMethod" to paymentMethod,
            "itemsJson" to itemsJson,
            "subtotal" to subtotal,
            "gstPercent" to gstPercent,
            "gstAmount" to gstAmount,
            "discount" to discount,
            "total" to total,
            "notes" to notes,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): BillEntity {
            return BillEntity(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                no = map["no"] as? String ?: "",
                date = map["date"] as? String ?: "",
                customerId = map["customerId"] as? String ?: "",
                customerName = map["customerName"] as? String ?: "Walk-in Customer",
                customerPhone = map["customerPhone"] as? String ?: "",
                customerEmail = map["customerEmail"] as? String ?: "",
                customerAddress = map["customerAddress"] as? String ?: "",
                status = map["status"] as? String ?: "Paid",
                paymentMethod = map["paymentMethod"] as? String ?: "Cash",
                itemsJson = map["itemsJson"] as? String ?: "[]",
                subtotal = (map["subtotal"] as? Number)?.toDouble() ?: 0.0,
                gstPercent = (map["gstPercent"] as? Number)?.toDouble() ?: 0.0,
                gstAmount = (map["gstAmount"] as? Number)?.toDouble() ?: 0.0,
                discount = (map["discount"] as? Number)?.toDouble() ?: 0.0,
                total = (map["total"] as? Number)?.toDouble() ?: 0.0,
                notes = map["notes"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isSynced = true
            )
        }
    }
}
