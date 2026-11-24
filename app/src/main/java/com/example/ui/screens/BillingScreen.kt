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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.material3.FilterChipDefaults
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
    QuickWeightPreset(0.125, "125g (ادھ پاؤ)", "125g (ادھ پاؤ)", "125ml (ادھ پاؤ)", "125ml (ادھ پاؤ)"),
    QuickWeightPreset(0.25, "250g (ایک پاؤ)", "250g (ایک پاؤ)", "250ml (ایک پاؤ)", "250ml (ایک پاؤ)"),
    QuickWeightPreset(0.5, "500g (آدھا کلو)", "500g (آدھا کلو)", "500ml (آدھا لیٹر)", "500ml (آدھا لیٹر)"),
    QuickWeightPreset(0.75, "750g (تین پاؤ)", "750g (تین پاؤ)", "750ml (تین پاؤ)", "750ml (تین پاؤ)"),
    QuickWeightPreset(1.0, "1kg", "1kg", "1L", "1L"),
    QuickWeightPreset(1.25, "1.25kg (سوا کلو)", "1.25kg (سوا کلو)", "1.25L (سوا لیٹر)", "1.25L (سوا لیٹر)"),
    QuickWeightPreset(1.5, "1.5kg (ڈیڑھ کلو)", "1.5kg (ڈیڑھ کلو)", "1.5L (ڈیڑھ لیٹر)", "1.5L (ڈیڑھ لیٹر)")
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
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("billing_new_bill_tab")
                ) { Text(language.pick("نیا بل / آرڈر", "New Bill / Order")) }
                SegmentedButton(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("billing_history_tab")
                ) {
                    val pendingCount = orders.count { it.statusEnum() == OrderStatus.PENDING }
                    Text(
                        if (pendingCount > 0) language.pick("آرڈرز و بل ($pendingCount)", "Orders & Bills ($pendingCount)")
                        else language.pick("آرڈرز اور بلز", "Orders & Bills")
                    )
                }
            }
                        if (activeTab == 0) {
                PosLowStockWarningBanner(
                    language = language,
                    lowStockProducts = lowStockProducts,
                    onOpenLowStockReport = onOpenLowStockReport,
                    showOnlyLowStockActive = showOnlyLowStock,
                    onToggleLowStockFilter = { showOnlyLowStock = !showOnlyLowStock },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                )

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = manualCustomerName,
                        onValueChange = onManualCustomerNameChange,
                        label = { Text(language.pick("کسٹمر کا نام (اردو یا انگلش)", "Customer Name (for Bill)")) },
                        placeholder = { Text(language.pick("نقد کسٹمر یا کھاتہ منتخب کریں", "Walk-in or pick from Khata")) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        trailingIcon = {
                            TextButton(onClick = { showCustomerDropdown = true }) {
                                Text(language.pick("کھاتہ", "Khata"))
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("bill_customer_name_input")
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

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(language.pick("اردو یا انگلش میں تلاش کریں (آٹا، چینی، گھی)...", "Search Urdu or English (Atta, Sugar, Ghee)..."))
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).testTag("billing_search_input")
                    )
                    FilledIconButton(
                        onClick = onOpenScanner,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.size(52.dp).testTag("billing_barcode_scan_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = language.pick("بارکوڈ اسکین کریں", "Scan Barcode"))
                    }
                }

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

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = if (cartItems.isNotEmpty()) 96.dp else 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                        shadowElevation = 8.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
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
                                IconButton(onClick = onClearCart, modifier = Modifier.testTag("clear_cart_button")) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = language.pick("خالی کریں", "Clear"), tint = Color.White)
                                }
                                Button(
                                    onClick = { showCheckoutDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(14.dp),
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

    val weightTarget = customWeightProduct
    if (weightTarget != null) {
        val currentCartQty = cartMap[weightTarget.id]?.quantity ?: 0.0
        CustomWeightDialog(
            product = weightTarget,
            language = language,
            currentCartQty = currentCartQty,
            onApplyWeight = { newWeight ->
                if (currentCartQty <= 0.0) onAddProductToCart(weightTarget, newWeight)
                else onSetCartItemQuantity(weightTarget.id, newWeight)
                customWeightProduct = null
            },
            onSwitchToRupeeCalc = {
                customWeightProduct = null
                rupeeCalcProduct = weightTarget
            },
            onDismiss = { customWeightProduct = null }
        )
    }

    val rupeeTarget = rupeeCalcProduct
    if (rupeeTarget != null) {
        val currentCartQty = cartMap[rupeeTarget.id]?.quantity ?: 0.0
        BuyByRupeeAmountDialog(
            product = rupeeTarget,
            language = language,
            currentCartQty = currentCartQty,
            onAddCalculatedWeight = { exactWeight, replaceExisting ->
                if (replaceExisting && currentCartQty > 0.0) onSetCartItemQuantity(rupeeTarget.id, exactWeight)
                else onAddProductToCart(rupeeTarget, exactWeight)
                rupeeCalcProduct = null
            },
            onDismiss = { rupeeCalcProduct = null }
        )
    }
        if (showCheckoutDialog) {
        CheckoutDialog(
            language = language,
            cartItems = cartItems,
            customers = customers,
            selectedCustomer = selectedCustomer,
            initialCustomerName = manualCustomerName,
            initialCustomerPhone = manualCustomerPhone,
            subtotal = cartSubtotal,
            isBusy = isBusy,
            onConfirm = { method, discount, paid, isPending, note, pickedCustomer, newCustName, newCustPhone ->
                showCheckoutDialog = false
                val isCreditFlow = method == PaymentMethod.UDHAR || method == PaymentMethod.PARTIAL
                val finalNote = if (isCreditFlow && pickedCustomer != null) {
                    onSelectCustomer(pickedCustomer)
                    note
                } else if (isCreditFlow && newCustName.isNotBlank()) {
                    onSelectCustomer(null)
                    onManualCustomerNameChange(newCustName.trim())
                    if (onManualCustomerPhoneChange != null) {
                        onManualCustomerPhoneChange(newCustPhone.trim())
                        note
                    } else {
                        packPhoneIntoNote(newCustPhone.trim(), note)
                    }
                } else {
                    note
                }
                onCheckout(method, discount, paid, isPending, finalNote)
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
    lineTotal: Double,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onSetQuickWeight: (Double) -> Unit,
    onOpenCustomWeight: () -> Unit,
    onOpenRupeeCalculator: () -> Unit
) {
    val isWeight = product.isWeightBased()
    val isOutOfStock = product.isOutOfStock()
    val isLowStock = product.isLowStock()
    val projectedAfterCart = (product.stockQty - cartQty).coerceAtLeast(0.0)
    val willDropBelowThreshold = cartQty > 0.0 &&
        !isLowStock &&
        product.lowStockThreshold > 0.0 &&
        projectedAfterCart <= product.lowStockThreshold

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = when {
            isOutOfStock -> BorderStroke(1.5.dp, UdharCrimson.copy(alpha = 0.85f))
            isLowStock -> BorderStroke(1.4.dp, WarningAmber.copy(alpha = 0.85f))
            willDropBelowThreshold -> BorderStroke(1.3.dp, WarningAmber.copy(alpha = 0.75f))
            else -> null
        },
        colors = CardDefaults.cardColors(
            containerColor = when {
                cartQty > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                isOutOfStock -> UdharCrimson.copy(alpha = 0.06f)
                isLowStock -> WarningAmber.copy(alpha = 0.07f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = product.nameUrdu.ifBlank { product.nameEnglish },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isLowStock) {
                            Surface(
                                color = if (isOutOfStock) UdharCrimson else WarningAmber,
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier.testTag("low_stock_badge_${product.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = if (isOutOfStock) language.pick("ختم!", "OUT") else language.pick("کم اسٹاک", "LOW"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                    if (product.nameEnglish.isNotBlank() && product.nameEnglish != product.nameUrdu) {
                        Text(
                            text = product.nameEnglish,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${formatPkr(product.sellingPrice, language)} / ${unitLabel(product.unit, language)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "• ${language.pick("اسٹاک", "Stock")}: ${formatExactQty(product.stockQty)} / ${language.pick("حد", "Min")}: ${formatExactQty(product.lowStockThreshold)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                isOutOfStock -> UdharCrimson
                                isLowStock -> WarningAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }

                if (cartQty > 0) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDecrement,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("decrement_${product.id}")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenCustomWeight() }
                                    .testTag("qty_badge_${product.id}")
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${formatExactQty(cartQty)} ${unitLabel(product.unit, language)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    val subGrams = formatGramsSubLabel(cartQty, product.unit)
                                    if (subGrams != null && cartQty < 1.0) {
                                        Text(subGrams, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            IconButton(
                                onClick = onIncrement,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .testTag("increment_${product.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Text(
                            text = "= ${formatPkr(roundMoney(lineTotal), language)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                    }
                } else {
                    FilledTonalButton(
                        onClick = onIncrement,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_product_${product.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(2.dp))
                        Text(if (isWeight) language.pick("1 کلو", "1 ${unitLabel(product.unit, language)}") else language.pick("شامل", "Add"))
                    }
                }
            }
                        if (willDropBelowThreshold || (cartQty > 0.0 && cartQty > product.stockQty)) {
                Surface(
                    color = if (cartQty > product.stockQty) UdharCrimson.copy(alpha = 0.12f) else WarningAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = if (cartQty > product.stockQty) UdharCrimson else WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (cartQty > product.stockQty) {
                                language.pick(
                                    "توجہ: بل کی مقدار موجودہ اسٹاک (${formatExactQty(product.stockQty)}) سے زیادہ ہے!",
                                    "Alert: Bill quantity exceeds current stock (${formatExactQty(product.stockQty)})!"
                                )
                            } else {
                                language.pick(
                                    "اس بل کے بعد اسٹاک (${formatExactQty(projectedAfterCart)}) کم از کم حد (${formatExactQty(product.lowStockThreshold)}) سے نیچے آ جائے گا",
                                    "After this bill, remaining stock (${formatExactQty(projectedAfterCart)}) drops below min limit (${formatExactQty(product.lowStockThreshold)})"
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (cartQty > product.stockQty) UdharCrimson else WarningAmber
                        )
                    }
                }
            }

            if (isWeight) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QUICK_WEIGHT_PRESETS.forEach { preset ->
                        val isSelected = cartQty > 0.0 && abs(cartQty - preset.weight) < 0.001
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSetQuickWeight(preset.weight) },
                            label = {
                                Text(
                                    text = preset.labelFor(product.unit, language),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            modifier = Modifier.testTag("quick_wt_${product.id}_${preset.weight}")
                        )
                    }
                    FilterChip(
                        selected = false,
                        onClick = onOpenCustomWeight,
                        leadingIcon = { Icon(Icons.Default.Scale, contentDescription = null, modifier = Modifier.size(15.dp)) },
                        label = {
                            Text(
                                text = language.pick("کسٹم وزن", "Custom Wt"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier.testTag("custom_wt_chip_${product.id}")
                    )
                }

                if (product.sellingPrice > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onOpenRupeeCalculator() }
                                .testTag("buy_by_rupee_${product.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    text = language.pick("رقم کے حساب سے دیں (مثلاً 50 یا 100 روپے)", "Buy by Rupee Amount (PKR → Weight)"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomWeightDialog(
    product: Product,
    language: AppLanguage,
    currentCartQty: Double,
    onApplyWeight: (Double) -> Unit,
    onSwitchToRupeeCalc: () -> Unit,
    onDismiss: () -> Unit
) {
    val isWeight = product.isWeightBased()
    val isKg = product.unit.equals("kg", ignoreCase = true)
    var inputInGrams by remember { mutableStateOf(false) }
    var qtyText by remember { mutableStateOf(if (currentCartQty > 0.0) formatExactQty(currentCartQty) else "") }

    val parsedInput = qtyText.toDoubleOrNull() ?: 0.0
    val finalWeight = if (isWeight && inputInGrams) parsedInput / 1000.0 else parsedInput
    val validWeight = finalWeight >= 0.001
    val previewTotal = roundMoney(finalWeight * product.sellingPrice)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Scale, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (isWeight) language.pick("کسٹم وزن درج کریں", "Enter Custom Weight")
                    else language.pick("مقدار درج کریں", "Enter Quantity"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${product.nameUrdu.ifBlank { product.nameEnglish }} • ${formatPkr(product.sellingPrice, language)} /${unitLabel(product.unit, language)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (isWeight) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !inputInGrams,
                            onClick = {
                                if (inputInGrams) {
                                    val gVal = qtyText.toDoubleOrNull()
                                    qtyText = if (gVal != null && gVal > 0) formatExactQty(gVal / 1000.0) else ""
                                    inputInGrams = false
                                }
                            },
                            label = { Text(if (isKg) language.pick("کلوگرام (kg)", "Kilograms (kg)") else language.pick("لیٹر (L)", "Liters (L)")) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = inputInGrams,
                            onClick = {
                                if (!inputInGrams) {
                                    val kgVal = qtyText.toDoubleOrNull()
                                    qtyText = if (kgVal != null && kgVal > 0) round(kgVal * 1000.0).toLong().toString() else ""
                                    inputInGrams = true
                                }
                            },
                            label = { Text(if (isKg) language.pick("گرام (g)", "Grams (g)") else language.pick("ملی لیٹر (ml)", "Milliliters (ml)")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { raw -> qtyText = raw.filter { it.isDigit() || it == '.' } },
                    label = {
                        Text(
                            when {
                                !isWeight -> language.pick("مقدار (${unitLabel(product.unit, language)})", "Quantity (${unitLabel(product.unit, language)})")
                                inputInGrams && isKg -> language.pick("وزن گرام میں (مثلاً 350)", "Weight in Grams (e.g. 350)")
                                inputInGrams -> language.pick("مقدار ملی لیٹر میں (مثلاً 350)", "Volume in ml (e.g. 350)")
                                isKg -> language.pick("وزن کلو میں (مثلاً 0.375 یا 2.5)", "Weight in kg (e.g. 0.375 or 2.5)")
                                else -> language.pick("مقدار لیٹر میں (مثلاً 0.75)", "Volume in L (e.g. 0.75)")
                            }
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("custom_weight_input")
                )

                if (validWeight) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = language.pick("بل کی رقم:", "Bill Amount:"), style = MaterialTheme.typography.bodySmall)
                            Text(text = formatPkr(previewTotal, language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ProfitGreen)
                        }
                    }
                }

                if (isWeight && product.sellingPrice > 0.0) {
                    TextButton(onClick = onSwitchToRupeeCalc, modifier = Modifier.align(Alignment.End)) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("رقم کے حساب سے دیں", "Switch to Buy by Rs. Amount"))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApplyWeight(finalWeight) },
                enabled = validWeight,
                modifier = Modifier.testTag("confirm_custom_weight_button")
            ) { Text(language.pick("بل میں شامل کریں", "Set Weight")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("منسوخ", "Cancel")) } }
    )
}
@Composable
private fun BuyByRupeeAmountDialog(
    product: Product,
    language: AppLanguage,
    currentCartQty: Double,
    onAddCalculatedWeight: (exactWeight: Double, replaceExisting: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    val amountPkr = amountStr.toDoubleOrNull() ?: 0.0
    val ratePerUnit = product.sellingPrice
    val isValid = amountPkr > 0.0 && ratePerUnit > 0.0
    val exactWeight = if (isValid) (amountPkr / ratePerUnit).coerceAtLeast(0.001) else 0.0
    val isKg = product.unit.equals("kg", ignoreCase = true)
    val gramsOrMl = round(exactWeight * 1000.0).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("رقم کے حساب سے دیں", "Buy by Rupee Amount"), fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${product.nameUrdu.ifBlank { product.nameEnglish }} • ${formatPkr(ratePerUnit, language)} / ${unitLabel(product.unit, language)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_RUPEE_AMOUNTS.forEach { presetAmt ->
                        FilterChip(
                            selected = abs(amountPkr - presetAmt.toDouble()) < 0.01,
                            onClick = { amountStr = presetAmt.toString() },
                            label = { Text(if (language == AppLanguage.URDU) "روپے $presetAmt" else "Rs. $presetAmt", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { raw -> amountStr = raw.filter { it.isDigit() || it == '.' } },
                    label = { Text(language.pick("مطلوبہ رقم درج کریں (روپے)", "Enter PKR Amount")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rupee_amount_input")
                )
                if (isValid) {
                    Text(
                        text = "${language.pick("صحیح وزن:", "Weight:")} ${formatExactQty(exactWeight)} ${unitLabel(product.unit, language)} ($gramsOrMl${if (isKg) "g" else "ml"})",
                        fontWeight = FontWeight.Bold,
                        color = ProfitGreen
                    )
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { onAddCalculatedWeight(exactWeight, false) },
                    enabled = isValid,
                    modifier = Modifier.fillMaxWidth().testTag("confirm_rupee_amount_add_button")
                ) { Text(language.pick("بل میں شامل کریں", "Add to Bill")) }
                if (currentCartQty > 0.0) {
                    OutlinedButton(
                        onClick = { onAddCalculatedWeight(exactWeight, true) },
                        enabled = isValid,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(language.pick("صرف یہی رقم رکھیں", "Replace Quantity")) }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("منسوخ", "Cancel")) } }
    )
}

@Composable
private fun CheckoutDialog(
    language: AppLanguage,
    cartItems: List<CartItem>,
    customers: List<Customer>,
    selectedCustomer: Customer?,
    initialCustomerName: String,
    initialCustomerPhone: String,
    subtotal: Double,
    isBusy: Boolean,
    onConfirm: (PaymentMethod, Double, Double, Boolean, String, Customer?, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var discountStr by remember { mutableStateOf("0") }
    var paidStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val initialHasCustomTypedName = selectedCustomer == null && !isWalkInName(initialCustomerName)
    var createNewKhataCustomer by remember {
        mutableStateOf(selectedCustomer == null && (customers.isEmpty() || initialHasCustomTypedName))
    }
    var pickedKhataCustomer by remember { mutableStateOf(selectedCustomer) }
    var newCustomerName by remember { mutableStateOf(if (initialHasCustomTypedName) initialCustomerName.trim() else "") }
    var newCustomerPhone by remember { mutableStateOf(if (selectedCustomer == null) initialCustomerPhone.trim() else "") }

    val discount = roundMoney(discountStr.toDoubleOrNull()?.coerceIn(0.0, subtotal) ?: 0.0)
    val grandTotal = roundMoney((subtotal - discount).coerceAtLeast(0.0))
    val paidAmt = when (paymentMethod) {
        PaymentMethod.CASH -> grandTotal
        PaymentMethod.UDHAR -> 0.0
        PaymentMethod.PARTIAL -> roundMoney(paidStr.toDoubleOrNull()?.coerceIn(0.0, grandTotal) ?: 0.0)
    }
    val udharAmt = roundMoney((grandTotal - paidAmt).coerceAtLeast(0.0))
    val isCreditPayment = paymentMethod == PaymentMethod.UDHAR || paymentMethod == PaymentMethod.PARTIAL
    val needsKhataProfile = isCreditPayment && udharAmt > 0.0
    val hasValidCreditCustomer = when {
        !needsKhataProfile -> true
        createNewKhataCustomer -> !isWalkInName(newCustomerName)
        else -> pickedKhataCustomer != null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("بل فائنل کریں اور رسید بنائیں", "Finalize Bill & Generate Receipt")) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { discountStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(language.pick("رعایت (روپے)", "Discount (PKR)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PaymentMethod.entries.forEach { method ->
                        val chipLabel = when (method) {
                            PaymentMethod.CASH -> language.pick("نقد", "Cash")
                            PaymentMethod.UDHAR -> language.pick("ادھار", "Credit")
                            PaymentMethod.PARTIAL -> language.pick("جزوی", "Partial")
                        }
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(chipLabel, maxLines = 1) },
                            modifier = Modifier.weight(1f).testTag("checkout_payment_${method.name.lowercase()}")
                        )
                    }
                }
                if (paymentMethod == PaymentMethod.PARTIAL) {
                    OutlinedTextField(
                        value = paidStr,
                        onValueChange = { paidStr = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        label = { Text(language.pick("نقد وصول شدہ رقم", "Cash Paid Now")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("checkout_partial_paid_input")
                    )
                }
                if (isCreditPayment) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !createNewKhataCustomer,
                            onClick = { createNewKhataCustomer = false },
                            label = { Text(language.pick("موجودہ کھاتہ", "Existing")) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = createNewKhataCustomer,
                            onClick = { createNewKhataCustomer = true },
                            label = { Text(language.pick("نیا کسٹمر", "New")) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (!createNewKhataCustomer) {
                        Column(modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            customers.forEach { cust ->
                                val isSel = pickedKhataCustomer?.id == cust.id
                                Surface(
                                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().clickable { pickedKhataCustomer = cust }
                                ) {
                                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(cust.name, fontWeight = FontWeight.Bold)
                                        Text(formatPkr(cust.balance, language), color = UdharCrimson)
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(value = newCustomerName, onValueChange = { newCustomerName = it }, label = { Text(language.pick("کسٹمر کا نام*", "Customer Name*")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = newCustomerPhone, onValueChange = { newCustomerPhone = it }, label = { Text(language.pick("فون نمبر", "Phone")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    }
                }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text(language.pick("نوٹ", "Note")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(language.pick("کل رقم:", "Grand Total:"), fontWeight = FontWeight.Bold)
                    Text(formatPkr(grandTotal, language), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        onConfirm(
                            paymentMethod, discount, paidAmt, false, note,
                            if (!createNewKhataCustomer) pickedKhataCustomer else null,
                            if (createNewKhataCustomer) newCustomerName.trim() else "",
                            if (createNewKhataCustomer) newCustomerPhone.trim() else ""
                        )
                    },
                    enabled = !isBusy && hasValidCreditCustomer,
                    modifier = Modifier.fillMaxWidth().testTag("confirm_complete_bill_button")
                ) { Text(language.pick("بل مکمل کریں (خودکار رسید)", "Complete Bill & Auto Receipt")) }
                OutlinedButton(
                    onClick = {
                        onConfirm(
                            paymentMethod, discount, 0.0, true, note,
                            if (!createNewKhataCustomer) pickedKhataCustomer else null,
                            if (createNewKhataCustomer) newCustomerName.trim() else "",
                            if (createNewKhataCustomer) newCustomerPhone.trim() else ""
                        )
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(language.pick("زیرِ التواء آرڈر محفوظ کریں", "Save Pending Order")) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("منسوخ", "Cancel")) } }
    )
}

@Composable
private fun OrdersHistoryList(
    language: AppLanguage,
    orders: List<Order>,
    onOpenReceipt: (Order) -> Unit,
    onEditOrder: (Order) -> Unit,
    onDeleteOrder: (Order) -> Unit,
    onCompletePendingOrder: (Order, PaymentMethod) -> Unit,
    onCancelPendingOrder: (String) -> Unit
) {
    var orderToDelete by remember { mutableStateOf<Order?>(null) }
    val pendingDelete = orderToDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = { Text(language.pick("بل مستقل ڈیلیٹ کریں؟", "Delete Bill Permanently?"), fontWeight = FontWeight.Bold, color = UdharCrimson) },
            text = {
                Text(
                    language.pick(
                        "کیا آپ واقعی بل نمبر ${pendingDelete.billNumber} (${pendingDelete.customerName}) کو مستقل طور پر ڈیلیٹ کرنا چاہتے ہیں؟",
                        "Are you sure you want to permanently delete Bill #${pendingDelete.billNumber} (${pendingDelete.customerName})?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = { onDeleteOrder(pendingDelete); orderToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = UdharCrimson)
                ) { Text(language.pick("جی ہاں، ڈیلیٹ کریں", "Yes, Delete"), color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { orderToDelete = null }) { Text(language.pick("منسوخ", "Cancel")) } }
        )
    }

    if (orders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(language.pick("ابھی کوئی آرڈر یا بل موجود نہیں", "No orders or bills recorded yet"))
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
                modifier = Modifier.fillMaxWidth().clickable { onOpenReceipt(order) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(order.customerName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${order.billNumber} • ${order.items.size} ${language.pick("اشیاء", "items")}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(formatPkr(order.totalAmount, language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = { onEditOrder(order) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("ایڈٹ", "Edit"))
                        }
                        FilledTonalButton(onClick = { onOpenReceipt(order) }, modifier = Modifier.weight(1.2f)) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("رسید / WA", "Receipt"))
                        }
                        IconButton(onClick = { orderToDelete = order }, modifier = Modifier.testTag("billing_delete_order_${order.id}")) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = language.pick("ڈیلیٹ", "Delete"), tint = UdharCrimson)
                        }
                    }
                    if (status == OrderStatus.PENDING) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onCompletePendingOrder(order, PaymentMethod.CASH) }, modifier = Modifier.weight(1f)) {
                                Text(language.pick("نقد مکمل", "Complete Cash"))
                            }
                            IconButton(onClick = { onCancelPendingOrder(order.id) }) {
                                Icon(Icons.Default.Cancel, contentDescription = null, tint = UdharCrimson)
                            }
                        }
                    }
                }
            }
        }
    }
}
