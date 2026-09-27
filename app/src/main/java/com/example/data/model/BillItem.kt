package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class BillItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val qty: Double = 1.0,
    val rate: Double = 0.0
) {
    val amount: Double
        get() = qty * rate

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("qty", qty)
            put("rate", rate)
        }
    }

    companion object {
        fun fromJson(obj: JSONObject): BillItem {
            return BillItem(
                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                name = obj.optString("name", ""),
                qty = obj.optDouble("qty", 1.0),
                rate = obj.optDouble("rate", 0.0)
            )
        }

        fun parseList(jsonString: String): List<BillItem> {
            if (jsonString.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonString)
                val list = mutableListOf<BillItem>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun toJsonArrayString(items: List<BillItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}
