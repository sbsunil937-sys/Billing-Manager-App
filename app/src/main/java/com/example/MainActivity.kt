package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.AuthState
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.ui.screens.AuthDialog
import com.example.ui.screens.BillsScreen
import com.example.ui.screens.CreateBillScreen
import com.example.ui.screens.CustomerDialog
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InvoicePreviewDialog
import com.example.ui.screens.ProductDialog
import com.example.ui.screens.ProductsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandPrimaryVariant
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.BillingViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: BillingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            val context = LocalContext.current
            LaunchedEffect(Unit) {
                viewModel.toastEvent.collect { msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                MainContent(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { isDarkTheme = !isDarkTheme }
                )
            }
        }
    }
}

data class NavTabItem(
    val tab: AppTab,
    val label: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent(
    viewModel: BillingViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val previewBill by viewModel.previewBill.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val authState by viewModel.authState.collectAsState()

    var customerDialogTarget by remember { mutableStateOf<CustomerEntity?>(null) }
    var showCustomerDialog by remember { mutableStateOf(false) }

    var productDialogTarget by remember { mutableStateOf<ProductEntity?>(null) }
    var showProductDialog by remember { mutableStateOf(false) }

    var showAuthDialog by remember { mutableStateOf(false) }

    // Intercept back press to return to Dashboard if on another tab
    if (currentTab != AppTab.DASHBOARD) {
        BackHandler {
            viewModel.navigateTo(AppTab.DASHBOARD)
        }
    }

    val pendingCount = bills.count { it.status == "Pending" || it.status == "Overdue" }

    val navTabs = listOf(
        NavTabItem(AppTab.DASHBOARD, "Home", Icons.Default.Home),
        NavTabItem(AppTab.BILLS, "Bills", Icons.Default.Receipt, badgeCount = pendingCount),
        NavTabItem(AppTab.CREATE_BILL, "Create", Icons.Default.AddCircle),
        NavTabItem(AppTab.CUSTOMERS, "Customers", Icons.Default.People),
        NavTabItem(AppTab.PRODUCTS, "Products", Icons.Default.Inventory),
        NavTabItem(AppTab.REPORTS, "Reports", Icons.Default.Assessment),
        NavTabItem(AppTab.SETTINGS, "Settings", Icons.Default.Settings)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(BrandPrimary, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "₹",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PRD Bills",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentTab.title,
                                fontSize = 11.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    // Theme toggle button
                    IconButton(
                        onClick = onToggleDarkTheme,
                        modifier = Modifier.testTag("theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                            contentDescription = "Toggle theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Cloud Sync / Profile Icon
                    IconButton(
                        onClick = { showAuthDialog = true },
                        modifier = Modifier.testTag("cloud_auth_btn")
                    ) {
                        Icon(
                            imageVector = when (authState) {
                                is AuthState.Authenticated -> Icons.Default.CloudDone
                                else -> Icons.Default.CloudSync
                            },
                            contentDescription = "Cloud Status",
                            tint = when (authState) {
                                is AuthState.Authenticated -> Color(0xFF16A34A)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                windowInsets = WindowInsets.safeDrawing
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                navTabs.forEach { item ->
                    val isSelected = currentTab == item.tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.navigateTo(item.tab) },
                        icon = {
                            if (item.badgeCount > 0) {
                                BadgedBox(badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("${item.badgeCount}")
                                    }
                                }) {
                                    Icon(item.icon, contentDescription = item.label)
                                }
                            } else {
                                Icon(item.icon, contentDescription = item.label)
                            }
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            indicatorColor = BrandPrimary,
                            selectedTextColor = BrandPrimary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("nav_tab_${item.label.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToTab = { viewModel.navigateTo(it) },
                    onOpenCustomerDialog = {
                        customerDialogTarget = null
                        showCustomerDialog = true
                    },
                    onOpenProductDialog = {
                        productDialogTarget = null
                        showProductDialog = true
                    },
                    onOpenAuthDialog = { showAuthDialog = true }
                )
                AppTab.BILLS -> BillsScreen(
                    viewModel = viewModel,
                    onCreateNewBill = {
                        viewModel.initNewBillForm()
                        viewModel.navigateTo(AppTab.CREATE_BILL)
                    }
                )
                AppTab.CREATE_BILL -> CreateBillScreen(
                    viewModel = viewModel,
                    onOpenCustomerDialog = {
                        customerDialogTarget = null
                        showCustomerDialog = true
                    }
                )
                AppTab.CUSTOMERS -> CustomersScreen(
                    viewModel = viewModel,
                    onOpenCustomerDialog = { customer ->
                        customerDialogTarget = customer
                        showCustomerDialog = true
                    }
                )
                AppTab.PRODUCTS -> ProductsScreen(
                    viewModel = viewModel,
                    onOpenProductDialog = { product ->
                        productDialogTarget = product
                        showProductDialog = true
                    }
                )
                AppTab.REPORTS -> ReportsScreen(viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(
                    viewModel = viewModel,
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = onToggleDarkTheme,
                    onOpenAuthDialog = { showAuthDialog = true }
                )
            }
        }
    }

    // Invoice Preview Dialog
    previewBill?.let { bill ->
        InvoicePreviewDialog(
            bill = bill,
            settings = settings,
            onDismiss = { viewModel.dismissInvoicePreview() },
            onEdit = { b -> viewModel.populateBillForEdit(b) }
        )
    }

    // Customer Dialog
    if (showCustomerDialog) {
        CustomerDialog(
            initialCustomer = customerDialogTarget,
            onDismiss = { showCustomerDialog = false },
            onSave = { id, name, phone, email, address ->
                viewModel.saveCustomer(id, name, phone, email, address)
            }
        )
    }

    // Product Dialog
    if (showProductDialog) {
        ProductDialog(
            initialProduct = productDialogTarget,
            currency = settings.currency,
            onDismiss = { showProductDialog = false },
            onSave = { id, name, sku, price, gst ->
                viewModel.saveProduct(id, name, sku, price, gst)
            }
        )
    }

    // Auth Dialog
    if (showAuthDialog) {
        AuthDialog(
            viewModel = viewModel,
            onDismiss = { showAuthDialog = false }
        )
    }
}
