package com.example.util

import com.example.data.model.BillEntity
import com.example.data.model.BusinessSettingsEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val bills: List<BillEntity>,
    val customers: List<CustomerEntity>,
    val products: List<ProductEntity>,
    val settings: BusinessSettingsEntity
)

object JsonBackupHelper {

    fun exportToJson(
        bills: List<BillEntity>,
        customers: List<CustomerEntity>,
        products: List<ProductEntity>,
        settings: BusinessSettingsEntity
    ): String {
        val root = JSONObject()

        val billsArray = JSONArray()
        bills.forEach { b ->
            billsArray.put(JSONObject(b.toFirestoreMap()))
        }
        root.put("bills", billsArray)

        val custArray = JSONArray()
        customers.forEach { c ->
            custArray.put(JSONObject(c.toFirestoreMap()))
        }
        root.put("customers", custArray)

        val prodArray = JSONArray()
        products.forEach { p ->
            prodArray.put(JSONObject(p.toFirestoreMap()))
        }
        root.put("products", prodArray)

        root.put("settings", JSONObject(settings.toFirestoreMap()))
        root.put("exportedAt", System.currentTimeMillis())
        root.put("version", 1)

        return root.toString(2)
    }

    fun importFromJson(jsonString: String): Result<BackupData> {
        return try {
            val root = JSONObject(jsonString)

            val billsList = mutableListOf<BillEntity>()
            if (root.has("bills")) {
                val billsArray = root.getJSONArray("bills")
                for (i in 0 until billsArray.length()) {
                    val obj = billsArray.getJSONObject(i)
                    val map = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = obj.get(key)
                    }
                    billsList.add(BillEntity.fromFirestoreMap(map))
                }
            }

            val customersList = mutableListOf<CustomerEntity>()
            if (root.has("customers")) {
                val custArray = root.getJSONArray("customers")
                for (i in 0 until custArray.length()) {
                    val obj = custArray.getJSONObject(i)
                    val map = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = obj.get(key)
                    }
                    customersList.add(CustomerEntity.fromFirestoreMap(map))
                }
            }

            val productsList = mutableListOf<ProductEntity>()
            if (root.has("products")) {
                val prodArray = root.getJSONArray("products")
                for (i in 0 until prodArray.length()) {
                    val obj = prodArray.getJSONObject(i)
                    val map = mutableMapOf<String, Any?>()
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = obj.get(key)
                    }
                    productsList.add(ProductEntity.fromFirestoreMap(map))
                }
            }

            val settings = if (root.has("settings")) {
                val obj = root.getJSONObject("settings")
                val map = mutableMapOf<String, Any?>()
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    map[key] = obj.get(key)
                }
                BusinessSettingsEntity.fromFirestoreMap(map)
            } else {
                BusinessSettingsEntity()
            }

            Result.success(BackupData(billsList, customersList, productsList, settings))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
