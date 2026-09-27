package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val sku: String = "",
    val price: Double = 0.0,
    val gstPercent: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "id" to id,
            "name" to name,
            "sku" to sku,
            "price" to price,
            "gstPercent" to gstPercent,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromFirestoreMap(map: Map<String, Any?>): ProductEntity {
            return ProductEntity(
                id = map["id"] as? String ?: UUID.randomUUID().toString(),
                name = map["name"] as? String ?: "",
                sku = map["sku"] as? String ?: "",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                gstPercent = (map["gstPercent"] as? Number)?.toDouble() ?: 0.0,
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                isSynced = true
            )
        }
    }
}
