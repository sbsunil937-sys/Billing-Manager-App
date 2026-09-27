package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_settings")
data class BusinessSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "PRD Store",
    val phone: String = "+91 98765 43210",
    val email: String = "store@prdmanager.com",
    val address: String = "123 Commercial Street, Market Area",
    val gstin: String = "29ABCDE1234F1Z5",
    val currency: String = "₹",
    val footerNotes: String = "Thank you for your business!"
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "phone" to phone,
            "email" to email,
            "address" to address,
            "gstin" to gstin,
            "currency" to currency,
            "footerNotes" to footerNotes
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): BusinessSettingsEntity {
            return BusinessSettingsEntity(
                id = 1,
                name = map["name"] as? String ?: "PRD Store",
                phone = map["phone"] as? String ?: "",
                email = map["email"] as? String ?: "",
                address = map["address"] as? String ?: "",
                gstin = map["gstin"] as? String ?: "",
                currency = map["currency"] as? String ?: "₹",
                footerNotes = map["footerNotes"] as? String ?: "Thank you for your business!"
            )
        }
    }
}
