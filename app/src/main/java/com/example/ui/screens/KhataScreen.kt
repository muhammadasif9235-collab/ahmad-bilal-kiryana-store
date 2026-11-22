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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.pick
import com.example.services.ReceiptFormatter
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson

@Composable
fun KhataScreen(
    language: AppLanguage,
    customers: List<Customer>,
    ledgerEntries: List<LedgerEntry>,
    onAddCustomer: (String, String, String, Double) -> Unit,
    onRecordTransaction: (Customer, LedgerEntryType, Double, String) -> Unit
) {
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
            // Total Udhar Receivable Banner
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

            // Search Box
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
            onRecordTransaction = { type, amount, note ->
                onRecordTransaction(selectedCustomer, type, amount, note)
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
    var openingStr by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("نیا کسٹمر ادھار کھاتہ", "New Customer Udhar Account")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = { Text(language.pick("پتہ / محلہ (گجرات)", "Address / Mohalla (Gujrat)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = openingStr,
                    onValueChange = { openingStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(language.pick("سابقہ بقایا ادھار (روپے)", "Opening Udhar Balance (PKR)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, phone, address, openingStr.toDoubleOrNull() ?: 0.0) },
                enabled = name.isNotBlank()
            ) {
                Text(language.pick("محفوظ کریں", "Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("منسوخ", "Cancel"))
            }
        }
    )
}

@Composable
private fun CustomerLedgerDetailDialog(
    language: AppLanguage,
    customer: Customer,
    entries: List<LedgerEntry>,
    onRecordTransaction: (LedgerEntryType, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingEntryType by remember { mutableStateOf<LedgerEntryType?>(null) }

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
                    .heightIn(max = 650.dp)
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
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = language.pick("بند کریں", "Close"))
                    }
                }

                // Current Balance Strip
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

                // Receive Payment / Give Udhar Buttons
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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isCredit) language.pick("نقد وصولی", "Cash Received") else language.pick("ادھار دیا", "Udhar Given"),
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
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingEntryType != null) {
        var amountStr by remember { mutableStateOf("") }
        var note by remember { mutableStateOf("") }
        val isCredit = pendingEntryType == LedgerEntryType.CREDIT
        AlertDialog(
            onDismissRequest = { pendingEntryType = null },
            title = {
                Text(
                    if (isCredit) language.pick("نقد وصولی درج کریں", "Record Cash Payment")
                    else language.pick("ادھار رقم درج کریں", "Record Udhar Debit")
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(language.pick("رقم (روپے)*", "Amount (PKR)*")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(language.pick("تفصیل (اختیاری)", "Note (optional)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onRecordTransaction(pendingEntryType!!, amt, note)
                            pendingEntryType = null
                        }
                    },
                    enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
                ) {
                    Text(language.pick("محفوظ کریں", "Save"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingEntryType = null }) {
                    Text(language.pick("منسوخ", "Cancel"))
                }
            }
        )
    }
}
