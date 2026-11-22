package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.CartItem
import com.example.data.Customer
import com.example.data.Order
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.Product
import com.example.data.ProductCategory
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.label
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber

@Composable
fun BillingScreen(
    language: AppLanguage,
    products: List<Product>,
    customers: List<Customer>,
    orders: List<Order>,
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    manualCustomerName: String,
    isBusy: Boolean,
    onSelectCustomer: (Customer?) -> Unit,
    onManualCustomerNameChange: (String) -> Unit,
    onAddProductToCart: (Product, Double) -> Unit,
    onSetCartItemQuantity: (String, Double) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (PaymentMethod, Double, Double, Boolean, String) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenReceipt: (Order) -> Unit,
    onCompletePendingOrder: (Order, PaymentMethod) -> Unit,
    onCancelPendingOrder: (String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = New Bill, 1 = Orders & Bills History
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomerDropdown by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { prod ->
            val matchesCat = selectedCategory == null || prod.categoryEnum() == selectedCategory
            val q = searchQuery.trim()
            val matchesText = q.isBlank() ||
                prod.nameUrdu.contains(q, ignoreCase = true) ||
                prod.nameEnglish.contains(q, ignoreCase = true) ||
                prod.barcode.contains(q, ignoreCase = true)
            matchesCat && matchesText
        }
    }

    val cartSubtotal = remember(cartItems) { cartItems.sumOf { it.lineTotal } }
    val cartMap = remember(cartItems) { cartItems.associateBy { it.product.id } }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            // Mode Switcher: New Bill vs Orders & Bills
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("billing_new_bill_tab")
                ) {
                    Text(language.pick("نیا بل / آرڈر", "New Bill / Order"))
                }
                SegmentedButton(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("billing_history_tab")
                ) {
                    val pendingCount = orders.count { it.statusEnum() == OrderStatus.PENDING }
                    val label = if (pendingCount > 0) {
                        language.pick("آرڈرز و بل ($pendingCount)", "Orders & Bills ($pendingCount)")
                    } else {
                        language.pick("آرڈرز اور بلز", "Orders & Bills")
                    }
                    Text(label)
                }
            }

            if (activeTab == 0) {
                // Customer Name Selector (Urdu RTL / English LTR)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    OutlinedTextField(
                        value = manualCustomerName,
                        onValueChange = onManualCustomerNameChange,
                        label = {
                            Text(language.pick("کسٹمر کا نام (اردو یا انگلش)", "Customer Name (for Bill)"))
                        },
                        placeholder = {
                            Text(language.pick("نقد کسٹمر یا کھاتہ منتخب کریں", "Walk-in or pick from Khata"))
                        },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingIcon = {
                            TextButton(onClick = { showCustomerDropdown = true }) {
                                Text(language.pick("کھاتہ", "Khata"))
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bill_customer_name_input")
                    )

                    DropdownMenu(
                        expanded = showCustomerDropdown,
                        onDismissRequest = { showCustomerDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(language.pick("نقد کسٹمر (بغیر کھاتہ)", "Walk-in Cash Customer")) },
                            onClick = {
                                onSelectCustomer(null)
                                onManualCustomerNameChange(language.pick("نقد کسٹمر", "Walk-in Customer"))
                                showCustomerDropdown = false
                            }
                        )
                        customers.forEach { cust ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cust.name, fontWeight = FontWeight.Medium)
                                        if (cust.balance > 0) {
                                            Text(
                                                text = "(${formatPkr(cust.balance, language)})",
                                                color = UdharCrimson,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSelectCustomer(cust)
                                    showCustomerDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Search + Barcode Scanner Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(language.pick("آٹا، چینی، گھی، چاول تلاش کریں...", "Search Atta, Sugar, Ghee..."))
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("billing_search_input")
                    )
                    FilledIconButton(
                        onClick = onOpenScanner,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("billing_barcode_scan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = language.pick("بارکوڈ اسکین کریں", "Scan Barcode")
                        )
                    }
                }

                // Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(language.pick("تمام اشیاء", "All")) }
                    )
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.label(language)) }
                        )
                    }
                }

                // Product List for Fast POS Selection
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = if (cartItems.isNotEmpty()) 96.dp else 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        val inCart = cartMap[prod.id]
                        BillingProductRow(
                            product = prod,
                            language = language,
                            cartQty = inCart?.quantity ?: 0.0,
                            onIncrement = { onAddProductToCart(prod, 1.0) },
                            onAddHalf = { onAddProductToCart(prod, 0.5) },
                            onDecrement = {
                                val next = (inCart?.quantity ?: 0.0) - 1.0
                                onSetCartItemQuantity(prod.id, next)
                            }
                        )
                    }
                }

                // Sticky Bottom Cart Bar
                if (cartItems.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${cartItems.size} ${language.pick("اشیاء منتخب", "items in bill")}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = formatPkr(cartSubtotal, language),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onClearCart,
                                    modifier = Modifier.testTag("clear_cart_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = language.pick("خالی کریں", "Clear"),
                                        tint = Color.White
                                    )
                                }
                                Button(
                                    onClick = { showCheckoutDialog = true },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.testTag("open_checkout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCartCheckout,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.size(6.dp))
                                    Text(language.pick("بل بنائیں", "Checkout"))
                                }
                            }
                        }
                    }
                }
            } else {
                // Orders & Completed Bills History
                OrdersHistoryList(
                    language = language,
                    orders = orders,
                    onOpenReceipt = onOpenReceipt,
                    onCompletePendingOrder = onCompletePendingOrder,
                    onCancelPendingOrder = onCancelPendingOrder
                )
            }
        }
    }

    if (showCheckoutDialog) {
        CheckoutDialog(
            language = language,
            cartItems = cartItems,
            selectedCustomer = selectedCustomer,
            customerName = manualCustomerName.ifBlank { language.pick("نقد کسٹمر", "Walk-in Customer") },
            subtotal = cartSubtotal,
            isBusy = isBusy,
            onConfirm = { method, discount, paid, isPending, note ->
                showCheckoutDialog = false
                onCheckout(method, discount, paid, isPending, note)
            },
            onDismiss = { showCheckoutDialog = false }
        )
    }
}

@Composable
private fun BillingProductRow(
    product: Product,
    language: AppLanguage,
    cartQty: Double,
    onIncrement: () -> Unit,
    onAddHalf: () -> Unit,
    onDecrement: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (cartQty > 0) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.URDU) product.nameUrdu else product.nameEnglish,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatPkr(product.sellingPrice, language)} / ${unitLabel(product.unit, language)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• ${language.pick("اسٹاک", "Stock")}: ${formatQty(product.stockQty)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (product.isLowStock()) UdharCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (cartQty > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDecrement,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = language.pick("کم کریں", "Decrease"),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = formatQty(cartQty),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onIncrement,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = language.pick("بڑھائیں", "Increase"),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (product.unit.equals("kg", ignoreCase = true)) {
                        OutlinedButton(
                            onClick = onAddHalf,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(language.pick("آدھا", "0.5"))
                        }
                    }
                    FilledTonalButton(
                        onClick = onIncrement,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(2.dp))
                        Text(language.pick("شامل", "Add"))
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutDialog(
    language: AppLanguage,
    cartItems: List<CartItem>,
    selectedCustomer: Customer?,
    customerName: String,
    subtotal: Double,
    isBusy: Boolean,
    onConfirm: (PaymentMethod, Double, Double, Boolean, String) -> Unit,
    onDismiss: () -> Unit
) {
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var discountStr by remember { mutableStateOf("0") }
    var paidStr by remember { mutableStateOf(subtotal.toLong().toString()) }
    var note by remember { mutableStateOf("") }

    val discount = discountStr.toDoubleOrNull()?.coerceIn(0.0, subtotal) ?: 0.0
    val grandTotal = (subtotal - discount).coerceAtLeast(0.0)
    val paidAmt = when (paymentMethod) {
        PaymentMethod.CASH -> grandTotal
        PaymentMethod.UDHAR -> 0.0
        PaymentMethod.PARTIAL -> paidStr.toDoubleOrNull()?.coerceIn(0.0, grandTotal) ?: 0.0
    }
    val udharAmt = (grandTotal - paidAmt).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(language.pick("بل فائنل کریں اور رسید بنائیں", "Finalize Bill & Generate Receipt"))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${language.pick("کسٹمر", "Customer")}: $customerName (${cartItems.size} ${language.pick("اشیاء", "items")})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { discountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(language.pick("رعایت (روپے)", "Discount (PKR)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = language.pick("ادائیگی کا طریقہ:", "Payment Method:"),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PaymentMethod.entries.forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method.label(language), maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (paymentMethod == PaymentMethod.PARTIAL) {
                    OutlinedTextField(
                        value = paidStr,
                        onValueChange = { paidStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(language.pick("نقد وصول شدہ رقم", "Cash Paid Now (PKR)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if ((paymentMethod == PaymentMethod.UDHAR || paymentMethod == PaymentMethod.PARTIAL) && selectedCustomer == null) {
                    Text(
                        text = language.pick(
                            "نوٹ: ادھار کھاتہ میں درج کرنے کے لیے کسٹمر لسٹ سے کھاتہ منتخب کریں۔",
                            "Note: Select a saved customer from Khata to record Udhar."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = UdharCrimson
                    )
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(language.pick("نوٹ (اختیاری)", "Order Note (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(language.pick("کل رقم:", "Grand Total:"), fontWeight = FontWeight.Bold)
                    Text(
                        formatPkr(grandTotal, language),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (udharAmt > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(language.pick("ادھار کھاتہ میں:", "Added to Udhar:"), color = UdharCrimson)
                        Text(formatPkr(udharAmt, language), color = UdharCrimson, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onConfirm(paymentMethod, discount, paidAmt, false, note) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_complete_bill_button")
                ) {
                    Text(language.pick("بل مکمل کریں (خودکار رسید)", "Complete Bill & Auto Receipt"))
                }
                OutlinedButton(
                    onClick = { onConfirm(paymentMethod, discount, 0.0, true, note) },
                    enabled = !isBusy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_pending_order_button")
                ) {
                    Text(language.pick("زیرِ التواء آرڈر محفوظ کریں", "Save as Pending Order"))
                }
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
private fun OrdersHistoryList(
    language: AppLanguage,
    orders: List<Order>,
    onOpenReceipt: (Order) -> Unit,
    onCompletePendingOrder: (Order, PaymentMethod) -> Unit,
    onCancelPendingOrder: (String) -> Unit
) {
    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(44.dp)
                )
                Text(
                    text = language.pick("ابھی کوئی آرڈر یا بل موجود نہیں", "No orders or bills recorded yet"),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(orders, key = { it.id }) { order ->
            val status = order.statusEnum()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenReceipt(order) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = order.customerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${order.billNumber} • ${order.items.size} ${language.pick("اشیاء", "items")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            color = when (status) {
                                OrderStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                                OrderStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                                OrderStatus.CANCELLED -> MaterialTheme.colorScheme.errorContainer
                            },
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = status.label(language),
                                style = MaterialTheme.typography.labelSmall,
                                color = when (status) {
                                    OrderStatus.PENDING -> WarningAmber
                                    OrderStatus.COMPLETED -> ProfitGreen
                                    OrderStatus.CANCELLED -> UdharCrimson
                                },
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${language.pick("منافع", "Profit")}: ${formatPkr(order.profitAmount, language)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = ProfitGreen
                        )
                        Text(
                            text = formatPkr(order.totalAmount, language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (status == OrderStatus.PENDING) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { onCompletePendingOrder(order, PaymentMethod.CASH) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(language.pick("نقد بل مکمل", "Complete Cash"))
                            }
                            if (order.customerId.isNotBlank()) {
                                FilledTonalButton(
                                    onClick = { onCompletePendingOrder(order, PaymentMethod.UDHAR) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(language.pick("ادھار مکمل", "Complete Udhar"))
                                }
                            }
                            IconButton(onClick = { onCancelPendingOrder(order.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = language.pick("منسوخ", "Cancel"),
                                    tint = UdharCrimson
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
