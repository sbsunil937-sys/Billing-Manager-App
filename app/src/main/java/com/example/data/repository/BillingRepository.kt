package com.example.data.repository

import com.example.data.firebase.FirebaseAuthManager
import com.example.data.firebase.FirestoreManager
import com.example.data.local.AppDatabase
import com.example.data.model.BillEntity
import com.example.data.model.BillItem
import com.example.data.model.BusinessSettingsEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class BillingRepository(
    private val database: AppDatabase,
    val authManager: FirebaseAuthManager,
    val firestoreManager: FirestoreManager,
    private val coroutineScope: CoroutineScope
) {
    private val billDao = database.billDao()
    private val customerDao = database.customerDao()
    private val productDao = database.productDao()
    private val settingsDao = database.settingsDao()

    val billsFlow: Flow<List<BillEntity>> = billDao.getAllBills().distinctUntilChanged()
    val customersFlow: Flow<List<CustomerEntity>> = customerDao.getAllCustomers().distinctUntilChanged()
    val productsFlow: Flow<List<ProductEntity>> = productDao.getAllProducts().distinctUntilChanged()
    val settingsFlow: Flow<BusinessSettingsEntity?> = settingsDao.getSettings().distinctUntilChanged()

    init {
        // Initialize default settings if missing
        coroutineScope.launch(Dispatchers.IO) {
            val existing = settingsDao.getSettingsOnce()
            if (existing == null) {
                settingsDao.saveSettings(BusinessSettingsEntity())
            }
        }

        // Listen for Firestore updates if user is authenticated
        coroutineScope.launch(Dispatchers.IO) {
            authManager.authState.collect { state ->
                val userId = authManager.getCurrentUserId()
                if (userId != null) {
                    syncWithFirestore(userId)
                }
            }
        }
    }

    private fun syncWithFirestore(userId: String) {
        coroutineScope.launch(Dispatchers.IO) {
            // Listen to remote bills
            firestoreManager.observeBills(userId).collect { remoteBills ->
                if (remoteBills.isNotEmpty()) {
                    billDao.insertAll(remoteBills)
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            // Listen to remote customers
            firestoreManager.observeCustomers(userId).collect { remoteCustomers ->
                if (remoteCustomers.isNotEmpty()) {
                    customerDao.insertAll(remoteCustomers)
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            // Listen to remote products
            firestoreManager.observeProducts(userId).collect { remoteProducts ->
                if (remoteProducts.isNotEmpty()) {
                    productDao.insertAll(remoteProducts)
                }
            }
        }

        coroutineScope.launch(Dispatchers.IO) {
            // Listen to remote settings
            firestoreManager.observeSettings(userId).collect { remoteSettings ->
                if (remoteSettings != null) {
                    settingsDao.saveSettings(remoteSettings)
                }
            }
        }
    }

    suspend fun getNextBillNumber(): String = withContext(Dispatchers.IO) {
        val count = billDao.getCount()
        "INV-" + String.format(Locale.US, "%04d", count + 1)
    }

    // Bills
    suspend fun saveBill(bill: BillEntity) = withContext(Dispatchers.IO) {
        billDao.insertOrUpdate(bill)
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            val success = firestoreManager.saveBill(userId, bill)
            if (success) {
                billDao.insertOrUpdate(bill.copy(isSynced = true))
            }
        }
    }

    suspend fun deleteBill(billId: String) = withContext(Dispatchers.IO) {
        billDao.deleteById(billId)
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            firestoreManager.deleteBill(userId, billId)
        }
    }

    // Customers
    suspend fun saveCustomer(customer: CustomerEntity) = withContext(Dispatchers.IO) {
        customerDao.insertOrUpdate(customer)
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            val success = firestoreManager.saveCustomer(userId, customer)
            if (success) {
                customerDao.insertOrUpdate(customer.copy(isSynced = true))
            }
        }
    }

    suspend fun deleteCustomer(customerId: String) = withContext(Dispatchers.IO) {
        customerDao.deleteById(customerId)
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            firestoreManager.deleteCustomer(userId, customerId)
        }
    }

    // Products
    suspend fun saveProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        productDao.insertOrUpdate(product)
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            val success = firestoreManager.saveProduct(userId, product)
            if (success) {
                productDao.insertOrUpdate(product.copy(isSynced = true))
            }
        }
    }

    suspend fun deleteProduct(productId: String) = withContext(Dispatchers.IO) {
        productDao.deleteById(productId)
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            firestoreManager.deleteProduct(userId, productId)
        }
    }

    // Settings
    suspend fun saveSettings(settings: BusinessSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(settings)
        val userId = authManager.getCurrentUserId()
        if (userId != null) {
            firestoreManager.saveSettings(userId, settings)
        }
    }

    suspend fun syncAllNow(): Boolean = withContext(Dispatchers.IO) {
        val userId = authManager.getCurrentUserId() ?: return@withContext false
        try {
            val currentBills = billsFlow.firstOrNull() ?: emptyList()
            for (b in currentBills) {
                firestoreManager.saveBill(userId, b)
            }
            val currentCustomers = customersFlow.firstOrNull() ?: emptyList()
            for (c in currentCustomers) {
                firestoreManager.saveCustomer(userId, c)
            }
            val currentProducts = productsFlow.firstOrNull() ?: emptyList()
            for (p in currentProducts) {
                firestoreManager.saveProduct(userId, p)
            }
            val currentSettings = settingsDao.getSettingsOnce()
            if (currentSettings != null) {
                firestoreManager.saveSettings(userId, currentSettings)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        billDao.deleteAll()
        customerDao.deleteAll()
        productDao.deleteAll()
    }

    suspend fun loadSampleStoreData() = withContext(Dispatchers.IO) {
        val c1 = CustomerEntity(name = "Rajesh Sharma", phone = "+91 98200 12345", email = "rajesh.sharma@example.com", address = "Flat 402, Lotus Tower, Mumbai")
        val c2 = CustomerEntity(name = "Priya Patel", phone = "+91 98450 67890", email = "priya.patel@example.com", address = "Sector 14, Gandhinagar")
        val c3 = CustomerEntity(name = "Amitabh Verma", phone = "+91 99112 33445", email = "verma.corp@example.com", address = "Plot 88, Okhla Phase 3, New Delhi")
        val c4 = CustomerEntity(name = "Sunil Kumar", phone = "+91 94481 55667", email = "sbsunil937@gmail.com", address = "Brigade Road, Bengaluru")
        customerDao.insertAll(listOf(c1, c2, c3, c4))

        val p1 = ProductEntity(name = "Wireless Barcode Scanner", sku = "ELEC-WBS01", price = 3499.0, gstPercent = 18.0)
        val p2 = ProductEntity(name = "Thermal Receipt Paper (Pack of 10)", sku = "STAT-TRP10", price = 650.0, gstPercent = 12.0)
        val p3 = ProductEntity(name = "POS Touch Terminal", sku = "HW-POST01", price = 24990.0, gstPercent = 18.0)
        val p4 = ProductEntity(name = "Cash Drawer 4-Bill/8-Coin", sku = "HW-CD04", price = 2200.0, gstPercent = 18.0)
        val p5 = ProductEntity(name = "Annual Cloud POS Software License", sku = "SW-POS-1Y", price = 5999.0, gstPercent = 18.0)
        val p6 = ProductEntity(name = "Premium Thermal Printer 80mm", sku = "HW-TP80", price = 4850.0, gstPercent = 18.0)
        productDao.insertAll(listOf(p1, p2, p3, p4, p5, p6))

        val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = df.format(Date())
        val yesterdayStr = df.format(Date(System.currentTimeMillis() - 86400000L))
        val pastStr = df.format(Date(System.currentTimeMillis() - 86400000L * 4))

        val b1Items = listOf(
            BillItem(name = p1.name, qty = 2.0, rate = p1.price),
            BillItem(name = p2.name, qty = 3.0, rate = p2.price)
        )
        val b1Sub = b1Items.sumOf { it.amount }
        val b1Gst = (b1Sub - 200.0) * 0.18
        val b1Total = b1Sub - 200.0 + b1Gst
        val b1 = BillEntity(
            no = "INV-0001",
            date = todayStr,
            customerId = c1.id,
            customerName = c1.name,
            customerPhone = c1.phone,
            customerEmail = c1.email,
            customerAddress = c1.address,
            status = "Paid",
            paymentMethod = "UPI",
            itemsJson = BillItem.toJsonArrayString(b1Items),
            subtotal = b1Sub,
            gstPercent = 18.0,
            gstAmount = b1Gst,
            discount = 200.0,
            total = b1Total,
            notes = "Paid via GPay. Thank you for shopping with PRD Store!"
        )

        val b2Items = listOf(
            BillItem(name = p3.name, qty = 1.0, rate = p3.price),
            BillItem(name = p6.name, qty = 1.0, rate = p6.price)
        )
        val b2Sub = b2Items.sumOf { it.amount }
        val b2Gst = b2Sub * 0.18
        val b2Total = b2Sub + b2Gst
        val b2 = BillEntity(
            no = "INV-0002",
            date = yesterdayStr,
            customerId = c2.id,
            customerName = c2.name,
            customerPhone = c2.phone,
            customerEmail = c2.email,
            customerAddress = c2.address,
            status = "Pending",
            paymentMethod = "Bank Transfer",
            itemsJson = BillItem.toJsonArrayString(b2Items),
            subtotal = b2Sub,
            gstPercent = 18.0,
            gstAmount = b2Gst,
            discount = 0.0,
            total = b2Total,
            notes = "Net 15 days payment terms. NEFT details sent."
        )

        val b3Items = listOf(
            BillItem(name = p5.name, qty = 1.0, rate = p5.price)
        )
        val b3Sub = b3Items.sumOf { it.amount }
        val b3Gst = b3Sub * 0.18
        val b3Total = b3Sub + b3Gst
        val b3 = BillEntity(
            no = "INV-0003",
            date = pastStr,
            customerId = c3.id,
            customerName = c3.name,
            customerPhone = c3.phone,
            customerEmail = c3.email,
            customerAddress = c3.address,
            status = "Overdue",
            paymentMethod = "Other",
            itemsJson = BillItem.toJsonArrayString(b3Items),
            subtotal = b3Sub,
            gstPercent = 18.0,
            gstAmount = b3Gst,
            discount = 0.0,
            total = b3Total,
            notes = "Overdue invoice. Reminder email dispatched."
        )

        val b4Items = listOf(
            BillItem(name = "Assorted Store Supplies", qty = 1.0, rate = 1450.0)
        )
        val b4Sub = 1450.0
        val b4 = BillEntity(
            no = "INV-0004",
            date = todayStr,
            customerId = "",
            customerName = "Walk-in Customer",
            status = "Paid",
            paymentMethod = "Cash",
            itemsJson = BillItem.toJsonArrayString(b4Items),
            subtotal = b4Sub,
            gstPercent = 0.0,
            gstAmount = 0.0,
            discount = 50.0,
            total = 1400.0,
            notes = "Cash counter sale"
        )

        billDao.insertAll(listOf(b1, b2, b3, b4))
    }
}
