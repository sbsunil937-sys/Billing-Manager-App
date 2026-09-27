package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.AuthState
import com.example.data.firebase.FirebaseAuthManager
import com.example.data.firebase.FirestoreManager
import com.example.data.local.AppDatabase
import com.example.data.model.BillEntity
import com.example.data.model.BillItem
import com.example.data.model.BusinessSettingsEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.data.repository.BillingRepository
import com.example.util.JsonBackupHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class AppTab(val title: String) {
    DASHBOARD("Dashboard"),
    BILLS("Bills & Receipts"),
    CREATE_BILL("Create Bill"),
    CUSTOMERS("Customers"),
    PRODUCTS("Products"),
    REPORTS("Reports"),
    SETTINGS("Settings")
}

data class CreateBillState(
    val editId: String? = null,
    val billNo: String = "",
    val date: String = "",
    val customerId: String = "",
    val customerName: String = "Walk-in Customer",
    val customerPhone: String = "",
    val customerEmail: String = "",
    val customerAddress: String = "",
    val paymentStatus: String = "Paid",
    val paymentMethod: String = "Cash",
    val items: List<BillItem> = listOf(BillItem(name = "", qty = 1.0, rate = 0.0)),
    val gstPercent: Double = 0.0,
    val discount: Double = 0.0,
    val notes: String = "Thank you for your business"
) {
    val subtotal: Double
        get() = items.sumOf { it.amount }

    val gstAmount: Double
        get() {
            val base = maxOf(0.0, subtotal - discount)
            return (base * gstPercent) / 100.0
        }

    val grandTotal: Double
        get() = maxOf(0.0, subtotal - discount + gstAmount)
}

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    val authManager = FirebaseAuthManager(application)
    val firestoreManager = FirestoreManager()
    val repository = BillingRepository(database, authManager, firestoreManager, viewModelScope)

    // Current Navigation Tab
    private val _currentTab = MutableStateFlow(AppTab.DASHBOARD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    // Bill for Invoice Preview Dialog / Modal
    private val _previewBill = MutableStateFlow<BillEntity?>(null)
    val previewBill: StateFlow<BillEntity?> = _previewBill.asStateFlow()

    // Transient message (Snackbar / Toast)
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    // Syncing indicator
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Data streams from repository
    val bills: StateFlow<List<BillEntity>> = repository.billsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customers: StateFlow<List<CustomerEntity>> = repository.customersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val products: StateFlow<List<ProductEntity>> = repository.productsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<BusinessSettingsEntity> = repository.settingsFlow
        .combine(MutableStateFlow(BusinessSettingsEntity())) { s, defaultVal -> s ?: defaultVal }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BusinessSettingsEntity())

    val authState: StateFlow<AuthState> = authManager.authState

    // Search and filter for Bills tab
    val billSearchQuery = MutableStateFlow("")
    val billStatusFilter = MutableStateFlow("All")

    val filteredBills: StateFlow<List<BillEntity>> = combine(
        bills,
        billSearchQuery,
        billStatusFilter
    ) { list, query, filter ->
        list.filter { bill ->
            val matchesQuery = query.isBlank() ||
                    bill.no.contains(query, ignoreCase = true) ||
                    bill.customerName.contains(query, ignoreCase = true) ||
                    bill.date.contains(query, ignoreCase = true)
            val matchesFilter = filter == "All" || bill.status.equals(filter, ignoreCase = true)
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Create / Edit Bill Form State
    private val _billForm = MutableStateFlow(CreateBillState())
    val billForm: StateFlow<CreateBillState> = _billForm.asStateFlow()

    init {
        initNewBillForm()
    }

    fun navigateTo(tab: AppTab) {
        _currentTab.value = tab
    }

    fun showInvoicePreview(bill: BillEntity) {
        _previewBill.value = bill
    }

    fun dismissInvoicePreview() {
        _previewBill.value = null
    }

    fun emitToast(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }

    // Bill Form Methods
    fun initNewBillForm() {
        viewModelScope.launch {
            val nextNo = repository.getNextBillNumber()
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            _billForm.value = CreateBillState(
                billNo = nextNo,
                date = todayStr,
                items = listOf(BillItem(name = "", qty = 1.0, rate = 0.0))
            )
        }
    }

    fun populateBillForEdit(bill: BillEntity) {
        val items = bill.getItems().ifEmpty { listOf(BillItem(name = "", qty = 1.0, rate = 0.0)) }
        _billForm.value = CreateBillState(
            editId = bill.id,
            billNo = bill.no,
            date = bill.date,
            customerId = bill.customerId,
            customerName = bill.customerName,
            customerPhone = bill.customerPhone,
            customerEmail = bill.customerEmail,
            customerAddress = bill.customerAddress,
            paymentStatus = bill.status,
            paymentMethod = bill.paymentMethod,
            items = items,
            gstPercent = bill.gstPercent,
            discount = bill.discount,
            notes = bill.notes
        )
        _currentTab.value = AppTab.CREATE_BILL
    }

    fun updateBillForm(update: (CreateBillState) -> CreateBillState) {
        _billForm.value = update(_billForm.value)
    }

    fun selectCustomerForBill(customer: CustomerEntity?) {
        if (customer != null) {
            _billForm.value = _billForm.value.copy(
                customerId = customer.id,
                customerName = customer.name,
                customerPhone = customer.phone,
                customerEmail = customer.email,
                customerAddress = customer.address
            )
        } else {
            _billForm.value = _billForm.value.copy(
                customerId = "",
                customerName = "Walk-in Customer",
                customerPhone = "",
                customerEmail = "",
                customerAddress = ""
            )
        }
    }

    fun addBillItem(initial: BillItem = BillItem(name = "", qty = 1.0, rate = 0.0)) {
        val current = _billForm.value.items.toMutableList()
        current.add(initial)
        _billForm.value = _billForm.value.copy(items = current)
    }

    fun updateBillItem(index: Int, updated: BillItem) {
        val current = _billForm.value.items.toMutableList()
        if (index in current.indices) {
            current[index] = updated
            _billForm.value = _billForm.value.copy(items = current)
        }
    }

    fun removeBillItem(index: Int) {
        val current = _billForm.value.items.toMutableList()
        if (current.size > 1 && index in current.indices) {
            current.removeAt(index)
            _billForm.value = _billForm.value.copy(items = current)
        } else if (current.size == 1 && index == 0) {
            // keep at least 1 empty row
            _billForm.value = _billForm.value.copy(items = listOf(BillItem(name = "", qty = 1.0, rate = 0.0)))
        }
    }

    fun selectProductForItem(index: Int, product: ProductEntity) {
        val current = _billForm.value.items.toMutableList()
        if (index in current.indices) {
            val item = current[index].copy(
                name = product.name,
                rate = product.price
            )
            current[index] = item
            // Also suggest GST if current GST is 0
            val newGst = if (_billForm.value.gstPercent == 0.0 && product.gstPercent > 0.0) product.gstPercent else _billForm.value.gstPercent
            _billForm.value = _billForm.value.copy(items = current, gstPercent = newGst)
        }
    }

    fun saveCurrentBill(): Boolean {
        val form = _billForm.value
        val validItems = form.items.filter { it.name.isNotBlank() && it.qty > 0 }
        if (validItems.isEmpty()) {
            emitToast("Please add at least one valid item with name and quantity")
            return false
        }
        if (form.billNo.isBlank()) {
            emitToast("Bill number cannot be blank")
            return false
        }

        val billId = form.editId ?: UUID.randomUUID().toString()
        val bill = BillEntity(
            id = billId,
            no = form.billNo.trim(),
            date = form.date.ifBlank { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) },
            customerId = form.customerId,
            customerName = form.customerName.ifBlank { "Walk-in Customer" },
            customerPhone = form.customerPhone,
            customerEmail = form.customerEmail,
            customerAddress = form.customerAddress,
            status = form.paymentStatus,
            paymentMethod = form.paymentMethod,
            itemsJson = BillItem.toJsonArrayString(validItems),
            subtotal = form.subtotal,
            gstPercent = form.gstPercent,
            gstAmount = form.gstAmount,
            discount = form.discount,
            total = form.grandTotal,
            notes = form.notes,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            repository.saveBill(bill)
            emitToast("Bill ${bill.no} saved successfully")
            initNewBillForm()
            _currentTab.value = AppTab.BILLS
        }
        return true
    }

    fun updateBillStatus(bill: BillEntity, newStatus: String) {
        viewModelScope.launch {
            repository.saveBill(bill.copy(status = newStatus))
            emitToast("Bill ${bill.no} marked as $newStatus")
        }
    }

    fun deleteBill(billId: String) {
        viewModelScope.launch {
            repository.deleteBill(billId)
            emitToast("Bill deleted")
        }
    }

    // Customers CRUD
    fun saveCustomer(id: String?, name: String, phone: String, email: String, address: String) {
        if (name.isBlank()) {
            emitToast("Customer name is required")
            return
        }
        viewModelScope.launch {
            val customer = CustomerEntity(
                id = id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                address = address.trim()
            )
            repository.saveCustomer(customer)
            emitToast("Customer ${customer.name} saved")
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            emitToast("Customer deleted")
        }
    }

    // Products CRUD
    fun saveProduct(id: String?, name: String, sku: String, price: Double, gstPercent: Double) {
        if (name.isBlank()) {
            emitToast("Product name is required")
            return
        }
        viewModelScope.launch {
            val product = ProductEntity(
                id = id ?: UUID.randomUUID().toString(),
                name = name.trim(),
                sku = sku.trim(),
                price = price,
                gstPercent = gstPercent
            )
            repository.saveProduct(product)
            emitToast("Product ${product.name} saved")
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
            emitToast("Product deleted")
        }
    }

    // Settings
    fun saveSettings(settings: BusinessSettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(settings)
            emitToast("Business settings saved")
        }
    }

    // Cloud Sync & Auth
    fun signInWithGoogle(webClientId: String? = null) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = authManager.signInWithGoogle(webClientId)
            _isSyncing.value = false
            if (result.isSuccess) {
                emitToast("Signed in as ${result.getOrNull()?.displayName ?: "Google User"}")
                repository.syncAllNow()
            } else {
                emitToast("Sign-in note: ${result.exceptionOrNull()?.message ?: "Failed"}. Try Guest sign-in.")
            }
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = authManager.signInAnonymously()
            _isSyncing.value = false
            if (result.isSuccess) {
                emitToast("Guest Cloud Sync Activated")
                repository.syncAllNow()
            } else {
                emitToast("Guest sign-in error: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun signInWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = authManager.signInWithEmail(email, pass)
            _isSyncing.value = false
            if (result.isSuccess) {
                emitToast("Signed in as ${result.getOrNull()?.email}")
                repository.syncAllNow()
            } else {
                emitToast("Sign-in failed: ${result.exceptionOrNull()?.message ?: "Check credentials"}")
            }
        }
    }

    fun signUpWithEmail(email: String, pass: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = authManager.signUpWithEmail(email, pass)
            _isSyncing.value = false
            if (result.isSuccess) {
                emitToast("Account created for ${result.getOrNull()?.email}")
                repository.syncAllNow()
            } else {
                emitToast("Sign-up failed: ${result.exceptionOrNull()?.message ?: "Try again"}")
            }
        }
    }

    fun signOut() {
        authManager.signOut()
        emitToast("Signed out")
    }

    fun triggerSync() {
        viewModelScope.launch {
            _isSyncing.value = true
            val success = repository.syncAllNow()
            _isSyncing.value = false
            if (success) {
                emitToast("Sync completed with Firestore")
            } else {
                emitToast("Sync: Offline or not signed in. Data saved locally.")
            }
        }
    }

    // Sample data & reset
    fun loadSampleData() {
        viewModelScope.launch {
            repository.loadSampleStoreData()
            initNewBillForm()
            emitToast("Sample bills, customers, and products loaded!")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            initNewBillForm()
            emitToast("All store data cleared")
        }
    }

    fun importBackup(jsonString: String) {
        val result = JsonBackupHelper.importFromJson(jsonString)
        if (result.isSuccess) {
            val data = result.getOrThrow()
            viewModelScope.launch(Dispatchers.IO) {
                for (b in data.bills) repository.saveBill(b)
                for (c in data.customers) repository.saveCustomer(c)
                for (p in data.products) repository.saveProduct(p)
                repository.saveSettings(data.settings)
                initNewBillForm()
                emitToast("Backup restored successfully!")
            }
        } else {
            emitToast("Invalid backup JSON format")
        }
    }

    fun getExportBackupJson(): String {
        return JsonBackupHelper.exportToJson(
            bills.value,
            customers.value,
            products.value,
            settings.value
        )
    }
}
