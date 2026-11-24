package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import com.example.i18n.label
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.ui.components.PosLowStockWarningBanner
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

private const val WEIGHT_STEP_KG = 0.125

private data class QuickWeightPreset(
    val weight: Double,
    val labelKgUrdu: String,
    val labelKgEnglish: String,
    val labelLiterUrdu: String,
    val labelLiterEnglish: String
) {
    fun labelFor(unit: String, language: AppLanguage): String {
        val isLiter = unit.equals("liter", ignoreCase = true) || unit.equals("l", ignoreCase = true)
        return if (isLiter) language.pick(labelLiterUrdu, labelLiterEnglish)
        else language.pick(labelKgUrdu, labelKgEnglish)
    }
}

private val QUICK_WEIGHT_PRESETS = listOf(
    QuickWeightPreset(0.125, "125g (ادھ پاؤ)", "125g", "125ml (ادھ پاؤ)", "125ml"),
    QuickWeightPreset(0.25, "250g (ایک پاؤ)", "250g", "250ml (ایک پاؤ)", "250ml"),
    QuickWeightPreset(0.5, "500g (آدھا کلو)", "500g", "500ml (آدھا لیٹر)", "500ml"),
    QuickWeightPreset(0.75, "750g (تین پاؤ)", "750g", "750ml (تین پاؤ)", "750ml"),
    QuickWeightPreset(1.0, "1kg", "1kg", "1L", "1L"),
    QuickWeightPreset(1.25, "1.25kg", "1.25kg", "1.25L", "1.25L"),
    QuickWeightPreset(1.5, "1.5kg", "1.5kg", "1.5L", "1.5L")
)

private val QUICK_RUPEE_AMOUNTS = listOf(50, 100, 150, 200, 250, 300, 500, 1000)

private fun Product.isWeightBased(): Boolean =
    unit.equals("kg", ignoreCase = true) ||
        unit.equals("liter", ignoreCase = true) ||
        unit.equals("l", ignoreCase = true)

private fun roundMoney(value: Double): Double = round(value * 100.0) / 100.0

private fun formatExactQty(qty: Double): String {
    val rounded = round(qty * 1000.0) / 1000.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
    else String.format(Locale.US, "%.3f", rounded).trimEnd('0').trimEnd('.')
}

private fun formatGramsSubLabel(qty: Double, unit: String): String? {
    val isKg = unit.equals("kg", ignoreCase = true)
    val isLiter = unit.equals("liter", ignoreCase = true) || unit.equals("l", ignoreCase = true)
    if (!isKg && !isLiter) return null
    val subVal = round(qty * 1000.0).toLong()
    val suffix = if (isKg) "g" else "ml"
    return if (subVal < 1000L) "$subVal$suffix" else "${formatExactQty(qty)}${if (isKg) "kg" else "L"}"
}

private fun isWalkInName(name: String): Boolean {
    val clean = name.trim()
    return clean.isBlank() ||
        clean.equals("نقد کسٹمر", ignoreCase = true) ||
        clean.equals("Walk-in Customer", ignoreCase = true)
}

private fun packPhoneIntoNote(phone: String, rawNote: String): String {
    val cleanPhone = phone.trim()
    return if (cleanPhone.isBlank()) rawNote else "\u001FCP:$cleanPhone\u001F$rawNote"
}

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
    onCancelPendingOrder: (String) -> Unit,
    manualCustomerPhone: String = "",
    onManualCustomerPhoneChange: ((String) -> Unit)? = null,
    onEditOrder: (Order) -> Unit = {},
    onDeleteOrder: (Order) -> Unit = {},
    onOpenLowStockReport: () -> Unit = {}
) {
    var activeTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    var showOnlyLowStock by remember { mutableStateOf(false) }
    var expandLowStockBanner by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var customWeightProduct by remember { mutableStateOf<Product?>(null) }
    var rupeeCalcProduct by remember { mutableStateOf<Product?>(null) }

    val lowStockProducts = remember(products) { products.filter { it.isLowStock() } }

    val filteredProducts = remember(products, searchQuery, selectedCategory, showOnlyLowStock) {
        products.filter { prod ->
            val matchesLow = !showOnlyLowStock || prod.isLowStock()
            val matchesCat = selectedCategory == null || prod.categoryEnum() == selectedCategory
            val q = searchQuery.trim()
            val matchesText = q.isBlank() ||
                prod.nameUrdu.contains(q, ignoreCase = true) ||
                prod.nameEnglish.contains(q, ignoreCase = true) ||
                prod.barcode.contains(q, ignoreCase = true)
            matchesLow && matchesCat && matchesText
        }
    }

    val cartSubtotal = remember(cartItems) { roundMoney(cartItems.sumOf { roundMoney(it.lineTotal) }) }
    val cartMap = remember(cartItems) { cartItems.associateBy { it.product.id } }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                SegmentedButton(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("billing_new_bill_tab")
                ) { Text(language.pick("نیا بل / آرڈر", "New Bill / Order"), style = MaterialTheme.typography.labelMedium) }
                SegmentedButton(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("billing_history_tab")
                ) {
                    val pendingCount = orders.count { it.statusEnum() == OrderStatus.PENDING }
                    Text(
                        if (pendingCount > 0) language.pick("آرڈرز و بل ($pendingCount)", "Orders ($pendingCount)")
                        else language.pick("آرڈرز اور بلز", "Orders & Bills"),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
                        if (activeTab == 0) {
                // 1. ULTRA-SLIM COLLAPSIBLE LOW-STOCK BAR (Keeps 75% screen open for items)
                if (lowStockProducts.isNotEmpty()) {
                    Surface(
                        color = UdharCrimson.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, UdharCrimson.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .clickable { expandLowStockBanner = !expandLowStockBanner }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = UdharCrimson, modifier = Modifier.size(15.dp))
                                Text(
                                    text = language.pick("کم اسٹاک: ${lowStockProducts.size} اشیاء", "Low Stock: ${lowStockProducts.size} items"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = UdharCrimson,
                                    maxLines = 1
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (expandLowStockBanner) language.pick("بند کریں", "Hide") else language.pick("تفصیل", "View"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = UdharCrimson
                                )
                                Icon(
                                    imageVector = if (expandLowStockBanner) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    tint = UdharCrimson,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (expandLowStockBanner) {
                        PosLowStockWarningBanner(
                            language = language,
                            lowStockProducts = lowStockProducts,
                            onOpenLowStockReport = onOpenLowStockReport,
                            showOnlyLowStockActive = showOnlyLowStock,
                            onToggleLowStockFilter = { showOnlyLowStock = !showOnlyLowStock },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                // 2. SINGLE COMPACT ROW: SEARCH + BARCODE + CUSTOMER / KHATA PICKER
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                language.pick("تلاش (آٹا، چینی)...", "Search item..."),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.35f).testTag("billing_search_input")
                    )

                    FilledIconButton(
                        onClick = onOpenScanner,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(46.dp).testTag("billing_barcode_scan_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(20.dp))
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        val activeCustLabel = when {
                            selectedCustomer != null -> selectedCustomer.name
                            !isWalkInName(manualCustomerName) -> manualCustomerName.trim()
                            else -> language.pick("کسٹمر / کھاتہ", "Customer")
                        }
                        FilledTonalButton(
                            onClick = { showCustomerDropdown = true },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                            modifier = Modifier.fillMaxWidth().height(46.dp).testTag("bill_customer_name_input")
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = activeCustLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

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
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Text(cust.name, fontWeight = FontWeight.Medium)
                                            if (cust.balance > 0) {
                                                Text("(${formatPkr(cust.balance, language)})", color = UdharCrimson, style = MaterialTheme.typography.labelSmall)
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
                }

                // 3. COMPACT CATEGORY CHIPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(language.pick("تمام اشیاء", "All"), style = MaterialTheme.typography.labelSmall) }
                    )
                    ProductCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                            label = { Text(cat.label(language), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // 4. EXPANDED ITEMS LIST (5-6 items visible at once)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = if (cartItems.isNotEmpty()) 88.dp else 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { prod ->
                        val inCart = cartMap[prod.id]
                        val currentQty = inCart?.quantity ?: 0.0
                        val step = if (prod.isWeightBased()) WEIGHT_STEP_KG else 1.0
                        BillingProductRow(
                            product = prod,
                            language = language,
                            cartQty = currentQty,
                            lineTotal = inCart?.lineTotal ?: 0.0,
                            onIncrement = {
                                if (currentQty <= 0.0) onAddProductToCart(prod, 1.0)
                                else onAddProductToCart(prod, step)
                            },
                            onDecrement = {
                                val next = currentQty - step
                                onSetCartItemQuantity(prod.id, if (next <= 0.0005) 0.0 else next)
                            },
                            onSetQuickWeight = { weight ->
                                if (currentQty <= 0.0) onAddProductToCart(prod, weight)
                                else onSetCartItemQuantity(prod.id, weight)
                            },
                            onOpenCustomWeight = { customWeightProduct = prod },
                            onOpenRupeeCalculator = { rupeeCalcProduct = prod }
                        )
                    }
                }

                if (cartItems.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${cartItems.size} ${language.pick("اشیاء منتخب", "items in bill")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = formatPkr(cartSubtotal, language),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onClearCart, modifier = Modifier.testTag("clear_cart_button")) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = Color.White)
                                }
                                Button(
                                    onClick = { showCheckoutDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("open_checkout_button")
                                ) {
                                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(language.pick("بل بنائیں", "Checkout"))
                                }
                            }
                        }
                    }
                }
            } else {
                OrdersHistoryList(
                    language = language,
                    orders = orders,
                    onOpenReceipt = onOpenReceipt,
                    onEditOrder = onEditOrder,
                    onDeleteOrder = onDeleteOrder,
                    onCompletePendingOrder = onCompletePendingOrder,
                    onCancelPendingOrder = onCancelPendingOrder
                )
            }
        }
    }
    
