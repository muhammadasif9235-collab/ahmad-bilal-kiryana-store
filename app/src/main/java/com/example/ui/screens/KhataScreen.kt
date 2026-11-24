package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Customer
import com.example.data.LedgerEntry
import com.example.data.LedgerEntryType
import com.example.data.Order
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.pick
import com.example.services.ReceiptFormatter
import com.example.services.ReceiptPrintHelper
import com.example.ui.components.LiveCalculatorAmountField
import com.example.ui.components.LiveCalculatorEngine
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber

private fun buildUdharReminderMessage(customer: Customer, language: AppLanguage): String {
    val balText = formatPkr(customer.balance, language)
    return if (language == AppLanguage.URDU) {
        "السلام علیکم ${customer.name}!\n" +
            "یاد دہانی: کھاتے میں آپ کے ذمے بقایا ادھار رقم $balText واجب الادا ہے۔\n" +
            "براہِ کرم اپنی سہولت کے مطابق ادائیگی کر دیں۔ جزاک اللہ!"
    } else {
        "Assalam-o-Alaikum ${customer.name}!\n" +
            "Friendly Reminder: Your pending Khata balance is $balText.\n" +
            "Kindly arrange payment at your earliest convenience. Thank you!"
    }
}

@Composable
fun KhataScreen(
    language: AppLanguage,
    customers: List<Customer>,
    ledgerEntries: List<LedgerEntry>,
    orders: List<Order> = emptyList(),
    onAddCustomer: (String, String, String, Double) -> Unit,
    onRecordTransaction: (Customer, LedgerEntryType, Double, String) -> Unit,
    onOpenEntryReceipt: (LedgerEntry) -> Unit = {},
    onEditOrder: (Order) -> Unit = {},
    onUpdateTransaction: (LedgerEntry, LedgerEntryType, Double, String) -> Unit = { _, _, _, _ -> },
    onDeleteTransaction: (LedgerEntry) -> Unit = {},
    onDeleteCustomer: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var selectedCustomerId by remember { mutableStateOf<String?>(null) }

    val selectedCustomer = remember(customers, selectedCustomerId) {
        customers.firstOrNull { it.id == selectedCustomerId }
    }

    val totalReceivable = remember(customers) {
        customers.filter { it.balance > 0 }.sumOf { it.balance }
    }

    val filteredCustomers = remember(customers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) customers
        else customers.filter {
            it.name.contains(q, true) || it.phone.contains(q, true) || it.address.contains(q, true)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = language.pick("کل بقایا ادھار (وصول طلب)", "Total Udhar Receivable"),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = formatPkr(totalReceivable, language),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                    }
                    Surface(
                        color = Color.White.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${customers.size}",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = language.pick("کسٹمرز", "Customers"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(language.pick("کسٹمر کا نام یا فون نمبر تلاش کریں...", "Search customer name or phone...")) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("khata_search_input")
            )

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = language.pick(
                                "ابھی کوئی کسٹمر کھاتہ نہیں ہے۔ نیا کھاتہ کھولنے کے لیے + بٹن دبائیں۔",
                                "No customers yet. Tap + to create an Udhar Khata account."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { cust ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedCustomerId = cust.id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cust.name.firstOrNull()?.uppercase() ?: "?",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = cust.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (cust.phone.isNotBlank() || cust.address.isNotBlank()) {
                                            Text(
                                                text = listOf(cust.phone, cust.address)
                                                    .filter { it.isNotBlank() }
                                                    .joinToString(" • "),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (cust.balance > 0) {
                                        IconButton(
                                            onClick = {
                                                val msg = buildUdharReminderMessage(cust, language)
                                                ReceiptPrintHelper.sendViaWhatsApp(context, cust.phone, msg)
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.NotificationsActive,
                                                contentDescription = language.pick("واٹس ایپ ادھار یاد دہانی", "WhatsApp Reminder"),
                                                tint = WarningAmber,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatPkr(cust.balance, language),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (cust.balance > 0) UdharCrimson else ProfitGreen
                                        )
                                        Text(
                                            text = if (cust.balance > 0) {
                                                language.pick("ادھار باقی", "Due")
                                            } else {
                                                language.pick("حساب صاف", "Clear")
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (cust.balance > 0) UdharCrimson else ProfitGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddCustomerDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("add_customer_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = language.pick("نیا کسٹمر", "Add Customer"))
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            language = language,
            onConfirm = { name, phone, address, opening ->
                onAddCustomer(name, phone, address, opening)
                showAddCustomerDialog = false
            },
            onDismiss = { showAddCustomerDialog = false }
        )
    }

    if (selectedCustomer != null) {
        val custEntries = remember(ledgerEntries, selectedCustomer.id) {
            ledgerEntries.filter { it.customerId == selectedCustomer.id }
        }
        CustomerLedgerDetailDialog(
            language = language,
            customer = selectedCustomer,
            entries = custEntries,
            ordersById = remember(orders) { orders.associateBy { it.id } },
            onRecordTransaction = { type, amount, note ->
                onRecordTransaction(selectedCustomer, type, amount, note)
            },
            onOpenEntryReceipt = onOpenEntryReceipt,
            onEditOrder = onEditOrder,
            onUpdateTransaction = onUpdateTransaction,
            onDeleteTransaction = onDeleteTransaction,
            onDeleteCustomer = {
                onDeleteCustomer(selectedCustomer.id)
                selectedCustomerId = null
            },
            onDismiss = { selectedCustomerId = null }
        )
    }
}
@Composable
private fun AddCustomerDialog(
    language: AppLanguage,
    onConfirm: (String, String, String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var openingExpr by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.pick("نیا کسٹمر ادھار کھاتہ", "New Customer Udhar Account"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(language.pick("کسٹمر کا نام*", "Customer Name*")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(language.pick("موبائل نمبر", "Phone Number")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(language.pick("پتہ / محلہ", "Address / Mohalla")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LiveCalculatorAmountField(
                    expression = openingExpr,
                    onExpressionChange = { openingExpr = it },
                    language = language,
                    label = language.pick("سابقہ بقایا ادھار (روپے)", "Opening Udhar Balance (PKR)"),
                    onDone = {
                        if (name.isNotBlank()) {
                            onConfirm(name, phone, address, LiveCalculatorEngine.evaluate(openingExpr))
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(language.pick("منسوخ", "Cancel"))
                    }
                    Button(
                        onClick = { onConfirm(name, phone, address, LiveCalculatorEngine.evaluate(openingExpr)) },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(language.pick("محفوظ کریں", "Save"))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerLedgerDetailDialog(
    language: AppLanguage,
    customer: Customer,
    entries: List<LedgerEntry>,
    ordersById: Map<String, Order>,
    onRecordTransaction: (LedgerEntryType, Double, String) -> Unit,
    onOpenEntryReceipt: (LedgerEntry) -> Unit,
    onEditOrder: (Order) -> Unit,
    onUpdateTransaction: (LedgerEntry, LedgerEntryType, Double, String) -> Unit,
    onDeleteTransaction: (LedgerEntry) -> Unit,
    onDeleteCustomer: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var pendingEntryType by remember { mutableStateOf<LedgerEntryType?>(null) }
    var editingLedgerEntry by remember { mutableStateOf<LedgerEntry?>(null) }
    var deletingEntryConfirm by remember { mutableStateOf<LedgerEntry?>(null) }
    var confirmDeleteCustomer by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 680.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(customer.name, style = MaterialTheme.typography.titleLarge)
                        if (customer.phone.isNotBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(
                                    customer.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (customer.balance > 0) {
                            IconButton(
                                onClick = {
                                    val msg = buildUdharReminderMessage(customer, language)
                                    ReceiptPrintHelper.sendViaWhatsApp(context, customer.phone, msg)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = language.pick("واٹس ایپ ادھار یاد دہانی", "WhatsApp Reminder"),
                                    tint = WarningAmber
                                )
                            }
                        }
                        IconButton(onClick = { confirmDeleteCustomer = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = language.pick("پورا کھاتہ ڈیلیٹ کریں", "Delete Customer Account"),
                                tint = UdharCrimson
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = language.pick("بند کریں", "Close"))
                        }
                    }
                }

                Surface(
                    color = if (customer.balance > 0) UdharCrimson.copy(alpha = 0.1f) else ProfitGreen.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.pick("موجودہ بقایا ادھار:", "Current Udhar Balance:"),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = formatPkr(customer.balance, language),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (customer.balance > 0) UdharCrimson else ProfitGreen
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { pendingEntryType = LedgerEntryType.CREDIT },
                        colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("khata_receive_payment_button")
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("وصولی (+)", "Receive"))
                    }
                    OutlinedButton(
                        onClick = { pendingEntryType = LedgerEntryType.DEBIT },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("khata_add_udhar_button")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = UdharCrimson, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("ادھار دیں", "Add Udhar"), color = UdharCrimson)
                    }
                }

                if (customer.balance > 0) {
                    OutlinedButton(
                        onClick = {
                            val msg = buildUdharReminderMessage(customer, language)
                            ReceiptPrintHelper.sendViaWhatsApp(context, customer.phone, msg)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningAmber),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("واٹس ایپ پر ادھار یاد دہانی بھیجیں", "Send WhatsApp Udhar Reminder"))
                    }
                }

                HorizontalDivider()

                Text(
                    text = language.pick("لین دین کی تفصیل", "Transaction History"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                if (entries.isEmpty()) {
                    Text(
                        text = language.pick("ابھی کوئی لین دین درج نہیں", "No transactions recorded yet"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(entries, key = { it.id }) { entry ->
                            val isCredit = entry.entryTypeEnum() == LedgerEntryType.CREDIT
                            val linkedOrder = if (entry.orderId.isNotBlank()) ordersById[entry.orderId] else null
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onOpenEntryReceipt(entry) }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isCredit) language.pick("نقد وصولی", "Cash Received")
                                        else if (linkedOrder != null) language.pick("ادھار بل #${linkedOrder.billNumber}", "Credit Bill #${linkedOrder.billNumber}")
                                        else language.pick("ادھار دیا", "Udhar Given"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isCredit) ProfitGreen else UdharCrimson
                                    )
                                    Text(
                                        text = ReceiptFormatter.formatDateTime(entry.createdAt?.toDate()?.time ?: System.currentTimeMillis()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (entry.note.isNotBlank()) {
                                        Text(entry.note, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = (if (isCredit) "-" else "+") + formatPkr(entry.amount, language),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCredit) ProfitGreen else UdharCrimson
                                        )
                                        Text(
                                            text = "${language.pick("بقایا", "Bal")}: ${entry.balanceAfter.toLong()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (linkedOrder != null) {
                                                onEditOrder(linkedOrder)
                                            } else {
                                                editingLedgerEntry = entry
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = language.pick("ایڈٹ کریں", "Edit"),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { deletingEntryConfirm = entry },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = language.pick("انٹری ڈیلیٹ کریں", "Delete Entry"),
                                            tint = UdharCrimson,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { confirmDeleteCustomer = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UdharCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(language.pick("اس کسٹمر کا پورا کھاتہ ڈیلیٹ کریں", "Delete Entire Customer Khata"))
                }
            }
        }
    }
        if (confirmDeleteCustomer) {
        AlertDialog(
            onDismissRequest = { confirmDeleteCustomer = false },
            title = { Text(language.pick("کھاتہ مستقل ڈیلیٹ کریں؟", "Delete Customer Khata?"), color = UdharCrimson) },
            text = {
                Text(
                    language.pick(
                        "کیا آپ واقعی «${customer.name}» کا پورا کھاتہ اور اس کا تمام لین دین مستقل طور پر ڈیلیٹ کرنا چاہتے ہیں؟",
                        "Are you sure you want to permanently delete ${customer.name}'s entire Khata and all its entries?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteCustomer = false
                        onDeleteCustomer()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UdharCrimson)
                ) {
                    Text(language.pick("ہاں، ڈیلیٹ کریں", "Yes, Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteCustomer = false }) {
                    Text(language.pick("منسوخ", "Cancel"))
                }
            }
        )
    }

    val entryToDelete = deletingEntryConfirm
    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { deletingEntryConfirm = null },
            title = { Text(language.pick("انٹری ڈیلیٹ کریں؟", "Delete This Entry?"), color = UdharCrimson) },
            text = {
                Text(
                    language.pick(
                        "کیا آپ واقعی ${formatPkr(entryToDelete.amount, language)} کی یہ انٹری ڈیلیٹ کرنا چاہتے ہیں؟ کسٹمر کا بقایا بیلنس خودکار اپڈیٹ ہو جائے گا۔",
                        "Delete this entry of ${formatPkr(entryToDelete.amount, language)}? Remaining balance will automatically recalculate."
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(entryToDelete)
                        deletingEntryConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UdharCrimson)
                ) {
                    Text(language.pick("ڈیلیٹ کریں", "Delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEntryConfirm = null }) {
                    Text(language.pick("منسوخ", "Cancel"))
                }
            }
        )
    }

    // DigiKhata-Style Live Calculator Dialog for Receive Payment / Give Udhar
    if (pendingEntryType != null) {
        var amountExpr by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        val isCredit = pendingEntryType == LedgerEntryType.CREDIT
        val accent = if (isCredit) ProfitGreen else UdharCrimson

        Dialog(
            onDismissRequest = { pendingEntryType = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isCredit) language.pick("نقد وصولی درج کریں", "Record Cash Payment")
                            else language.pick("ادھار رقم درج کریں", "Record Udhar Debit"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                        IconButton(onClick = { pendingEntryType = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(language.pick("تفصیل / آئٹمز نوٹ (اختیاری)", "Details / Note (optional)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LiveCalculatorAmountField(
                        expression = amountExpr,
                        onExpressionChange = { amountExpr = it },
                        language = language,
                        accentColor = accent,
                        onDone = {
                            val amt = LiveCalculatorEngine.evaluate(amountExpr)
                            if (amt > 0.0) {
                                onRecordTransaction(pendingEntryType!!, amt, note)
                                pendingEntryType = null
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pendingEntryType = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(language.pick("منسوخ", "Cancel"))
                        }
                        Button(
                            onClick = {
                                val amt = LiveCalculatorEngine.evaluate(amountExpr)
                                if (amt > 0.0) {
                                    onRecordTransaction(pendingEntryType!!, amt, note)
                                    pendingEntryType = null
                                }
                            },
                            enabled = LiveCalculatorEngine.evaluate(amountExpr) > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(language.pick("محفوظ کریں", "Save"))
                        }
                    }
                }
            }
        }
    }

    // DigiKhata-Style Live Calculator Dialog for Editing Existing Khata Entry
    val editTarget = editingLedgerEntry
    if (editTarget != null) {
        var editType by remember(editTarget.id) { mutableStateOf(editTarget.entryTypeEnum()) }
        var editAmountExpr by remember(editTarget.id) {
            mutableStateOf(
                if (editTarget.amount % 1.0 == 0.0) editTarget.amount.toLong().toString()
                else editTarget.amount.toString()
            )
        }
        var editNote by remember(editTarget.id) { mutableStateOf(editTarget.note) }
        val editAccent = if (editType == LedgerEntryType.CREDIT) ProfitGreen else UdharCrimson

        Dialog(
            onDismissRequest = { editingLedgerEntry = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = language.pick("کھاتہ انٹری ایڈٹ کریں", "Edit Khata Entry"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { editingLedgerEntry = null }) {
                            Icon(Icons.Default.Close, contentDescription = null)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = editType == LedgerEntryType.DEBIT,
                            onClick = { editType = LedgerEntryType.DEBIT },
                            label = { Text(language.pick("ادھار (+)", "Udhar (+)")) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = editType == LedgerEntryType.CREDIT,
                            onClick = { editType = LedgerEntryType.CREDIT },
                            label = { Text(language.pick("وصولی (-)", "Received (-)")) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text(language.pick("تفصیل", "Note")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    LiveCalculatorAmountField(
                        expression = editAmountExpr,
                        onExpressionChange = { editAmountExpr = it },
                        language = language,
                        accentColor = editAccent,
                        onDone = {
                            val amt = LiveCalculatorEngine.evaluate(editAmountExpr)
                            if (amt > 0.0) {
                                onUpdateTransaction(editTarget, editType, amt, editNote)
                                editingLedgerEntry = null
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDeleteTransaction(editTarget)
                                editingLedgerEntry = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = UdharCrimson),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("ڈیلیٹ", "Delete"))
                        }
                        Button(
                            onClick = {
                                val amt = LiveCalculatorEngine.evaluate(editAmountExpr)
                                if (amt > 0.0) {
                                    onUpdateTransaction(editTarget, editType, amt, editNote)
                                    editingLedgerEntry = null
                                }
                            },
                            enabled = LiveCalculatorEngine.evaluate(editAmountExpr) > 0.0,
                            colors = ButtonDefaults.buttonColors(containerColor = editAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(language.pick("اپڈیٹ کریں", "Update"))
                        }
                    }
                }
            }
        }
    }
}
