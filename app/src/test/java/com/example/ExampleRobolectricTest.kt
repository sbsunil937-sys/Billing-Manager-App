package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BillEntity
import com.example.data.model.BillItem
import com.example.data.model.BusinessSettingsEntity
import com.example.data.model.CustomerEntity
import com.example.data.model.ProductEntity
import com.example.util.CurrencyFormatter
import com.example.util.JsonBackupHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PRD Bills", appName)
    }

    @Test
    fun `bill item amount calculation and json serialization`() {
        val item = BillItem(name = "Thermal Receipt Paper", qty = 4.0, rate = 250.0)
        assertEquals(1000.0, item.amount, 0.001)

        val itemsList = listOf(item, BillItem(name = "POS Scanner", qty = 1.0, rate = 3200.0))
        val jsonString = BillItem.toJsonArrayString(itemsList)
        val parsedList = BillItem.parseList(jsonString)

        assertEquals(2, parsedList.size)
        assertEquals("Thermal Receipt Paper", parsedList[0].name)
        assertEquals(4.0, parsedList[0].qty, 0.001)
        assertEquals(250.0, parsedList[0].rate, 0.001)
        assertEquals(1000.0, parsedList[0].amount, 0.001)
        assertEquals(3200.0, parsedList[1].amount, 0.001)
    }

    @Test
    fun `currency formatter Indian rupee test`() {
        val formatted = CurrencyFormatter.format(12450.50, "₹")
        assertTrue(formatted.contains("₹"))
        assertTrue(formatted.contains("12,450.50") || formatted.contains("12450.50"))
    }

    @Test
    fun `create bill state calculations with gst and discount`() {
        val items = listOf(
            BillItem(name = "Product A", qty = 2.0, rate = 100.0), // 200
            BillItem(name = "Product B", qty = 1.0, rate = 300.0)  // 300
        )
        val state = com.example.ui.viewmodel.CreateBillState(
            items = items,
            gstPercent = 18.0,
            discount = 50.0
        )
        // subtotal = 500
        assertEquals(500.0, state.subtotal, 0.001)
        // discounted base = 500 - 50 = 450
        // gst = 450 * 18% = 81.0
        assertEquals(81.0, state.gstAmount, 0.001)
        // grand total = 450 + 81 = 531.0
        assertEquals(531.0, state.grandTotal, 0.001)
    }

    @Test
    fun `main activity launches and composes successfully`() {
        val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
        controller.setup()
        assertNotNull(controller.get())
    }

    @Test
    fun `backup data export and import test`() {
        val bill = BillEntity(
            id = "test-bill-1",
            no = "INV-0099",
            date = "2026-09-27",
            customerName = "Sunil Kumar",
            status = "Paid",
            subtotal = 5000.0,
            gstPercent = 18.0,
            gstAmount = 900.0,
            total = 5900.0
        )
        val customer = CustomerEntity(id = "cust-1", name = "Sunil Kumar", phone = "+91 94481 55667")
        val product = ProductEntity(id = "prod-1", name = "Scanner", sku = "SCN01", price = 4500.0, gstPercent = 18.0)
        val settings = BusinessSettingsEntity(name = "PRD Store Test", currency = "₹")

        val json = JsonBackupHelper.exportToJson(listOf(bill), listOf(customer), listOf(product), settings)
        assertNotNull(json)

        val importResult = JsonBackupHelper.importFromJson(json)
        assertTrue(importResult.isSuccess)

        val restored = importResult.getOrThrow()
        assertEquals(1, restored.bills.size)
        assertEquals("INV-0099", restored.bills[0].no)
        assertEquals(5900.0, restored.bills[0].total, 0.001)
        assertEquals(1, restored.customers.size)
        assertEquals("Sunil Kumar", restored.customers[0].name)
        assertEquals(1, restored.products.size)
        assertEquals("PRD Store Test", restored.settings.name)
    }
}
