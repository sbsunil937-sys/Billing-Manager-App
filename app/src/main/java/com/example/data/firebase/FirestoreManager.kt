package com.example.data.firebase

import android.util.Log
import com.example.data.model.BillEntity
import com.example.data.model.BusinessSettingsEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreManager {
    private val tag = "FirestoreManager"
    private var firestore: FirebaseFirestore? = null
    var isConnected: Boolean = false
        private set

    init {
        try {
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
            firestore = db
            isConnected = true
        } catch (e: Exception) {
            Log.w(tag, "Firestore initialization warning: ${e.message}")
            isConnected = false
        }
    }

    // Bills
    suspend fun saveBill(userId: String, bill: BillEntity): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("users")
                .document(userId)
                .collection("bills")
                .document(bill.id)
                .set(bill.toFirestoreMap(), SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to save bill to Firestore: ${e.message}")
            false
        }
    }

    suspend fun deleteBill(userId: String, billId: String): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("users")
                .document(userId)
                .collection("bills")
                .document(billId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete bill from Firestore: ${e.message}")
            false
        }
    }

    fun observeBills(userId: String): Flow<List<BillEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection("users")
                .document(userId)
                .collection("bills")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(tag, "Observe bills error: ${error.message}")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val bills = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { BillEntity.fromFirestoreMap(it) }
                        }
                        trySend(bills)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Listen bills failed: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    // Customers
    suspend fun saveCustomer(userId: String, customer: CustomerEntity): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("users")
                .document(userId)
                .collection("customers")
                .document(customer.id)
                .set(customer.toFirestoreMap(), SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to save customer to Firestore: ${e.message}")
            false
        }
    }

    suspend fun deleteCustomer(userId: String, customerId: String): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("users")
                .document(userId)
                .collection("customers")
                .document(customerId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete customer: ${e.message}")
            false
        }
    }

    fun observeCustomers(userId: String): Flow<List<CustomerEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection("users")
                .document(userId)
                .collection("customers")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val customers = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { CustomerEntity.fromFirestoreMap(it) }
                        }
                        trySend(customers)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Observe customers error: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    // Products
    suspend fun saveProduct(userId: String, product: ProductEntity): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("users")
                .document(userId)
                .collection("products")
                .document(product.id)
                .set(product.toFirestoreMap(), SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to save product: ${e.message}")
            false
        }
    }

    suspend fun deleteProduct(userId: String, productId: String): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("users")
                .document(userId)
                .collection("products")
                .document(productId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to delete product: ${e.message}")
            false
        }
    }

    fun observeProducts(userId: String): Flow<List<ProductEntity>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection("users")
                .document(userId)
                .collection("products")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val products = snapshot.documents.mapNotNull { doc ->
                            doc.data?.let { ProductEntity.fromFirestoreMap(it) }
                        }
                        trySend(products)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Observe products error: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }

    // Settings
    suspend fun saveSettings(userId: String, settings: BusinessSettingsEntity): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("users")
                .document(userId)
                .collection("settings")
                .document("business")
                .set(settings.toFirestoreMap(), SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w(tag, "Failed to save settings: ${e.message}")
            false
        }
    }

    fun observeSettings(userId: String): Flow<BusinessSettingsEntity?> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        var registration: ListenerRegistration? = null
        try {
            registration = db.collection("users")
                .document(userId)
                .collection("settings")
                .document("business")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val settings = snapshot.data?.let { BusinessSettingsEntity.fromFirestoreMap(it) }
                        trySend(settings)
                    }
                }
        } catch (e: Exception) {
            Log.w(tag, "Observe settings error: ${e.message}")
        }

        awaitClose {
            registration?.remove()
        }
    }
}
