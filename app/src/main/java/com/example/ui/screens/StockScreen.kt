package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.GujratCatalogData
import com.example.data.Product
import com.example.data.ProductCategory
import com.example.data.StockMovement
import com.example.data.StockMovementType
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.label
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.services.ReceiptFormatter
import com.example.ui.components.QuickThresholdEditDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.ShopAnalytics

@Composable
fun StockScreen(
    language: AppLanguage,
    products: List<Product>,
    movements: List<StockMovement>,
    analytics: ShopAnalytics,
    defaultMarginPercent: Double,
    onSaveProduct: (
        existingId: String?,
        nameUrdu: String,
        nameEnglish: String,
        category: ProductCategory,
        unit: String,
        costPrice: Double,
        sellingPrice: Double,
        marginPercent: Double,
        autoPriceEnabled: Boolean,
        stockQty: Double,
        lowStockThreshold: Double,
        barcode: String
    ) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onRecordStockMovement: (Product, StockMovementType, Double, Double, String) -> Unit,
    onSyncGujratMarketRates: () -> Unit,
    onOpenLowStockReport: () -> Unit = {}
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0 = Inventory, 1 = Stock In/Out, 2 = Profit Report
    var searchQuery by remember { mutableStateOf("") }
    var showOnlyLowStock by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var quickPriceProduct by remember { mutableStateOf<Product?>(null) }
    var quickThresholdProduct by remember { mutableStateOf<Product?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var movementProduct by remember { mutableStateOf<Product?>(null) }

    val lowStockProducts = remember(products) { products.filter { it.isLowStock() } }

    val filteredProducts = remember(products, searchQuery, showOnlyLowStock) {
        val q = searchQuery.trim()
        products.filter { prod ->
            val matchesLow = !showOnlyLowStock || prod.isLowStock()
            val matchesQuery = q.isBlank() ||
                prod.nameUrdu.contains(q, true) ||
                prod.nameEnglish.contains(q, true) ||
                prod.barcode.contains(q, true)
            matchesLow && matchesQuery
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
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(0, 3),
                    modifier = Modifier.testTag("stock_catalog_tab")
                ) {
                    Text(language.pick("اشیاء و ریٹ", "Items"), maxLines = 1)
                }
                SegmentedButton(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(1, 3),
                    modifier = Modifier.testTag("stock_movements_tab")
                ) {
                    Text(language.pick("ان / آؤٹ", "In / Out"), maxLines = 1)
                }
                SegmentedButton(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    shape = SegmentedButtonDefaults.itemShape(2, 3),
                    modifier = Modifier.testTag("stock_profit_tab")
                ) {
                    Text(language.pick("منافع", "Profit"), maxLines = 1)
                }
            }

            when (activeTab) {
                0 -> {
                    // Search (Urdu & English) + Auto Gujrat Rate Update Bar
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
                                Text(
                                    language.pick(
                                        "اردو یا انگلش نام تلاش کریں...",
                                        "Search Urdu or English name..."
                                    )
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stock_search_input")
                        )
                        FilledTonalButton(
                            onClick = onSyncGujratMarketRates,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("gujrat_auto_price_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("آٹو ریٹ", "Auto Rate"))
                        }
                    }

                    // Filter Row: All vs Low Stock + One-Tap Low Stock Report PDF
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = !showOnlyLowStock,
                            onClick = { showOnlyLowStock = false },
                            label = {
                                Text(language.pick("تمام (${products.size})", "All (${products.size})"))
                            },
                            modifier = Modifier.testTag("stock_filter_all")
                        )
                        FilterChip(
                            selected = showOnlyLowStock,
                            onClick = { showOnlyLowStock = !showOnlyLowStock },
                            leadingIcon = if (lowStockProducts.isNotEmpty()) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = if (showOnlyLowStock) MaterialTheme.colorScheme.onSecondaryContainer else UdharCrimson,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    language.pick(
                                        "کم اسٹاک (${lowStockProducts.size})",
                                        "Low (${lowStockProducts.size})"
                                    ),
                                    fontWeight = if (lowStockProducts.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("stock_filter_low")
                        )
                        Spacer(Modifier.weight(1f))
                        FilledTonalButton(
                            onClick = onOpenLowStockReport,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("stock_low_stock_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = if (lowStockProducts.isNotEmpty()) UdharCrimson else EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = language.pick("کم اسٹاک PDF", "Low Stock PDF"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { prod ->
                            val isLow = prod.isLowStock()
                            val isOut = prod.isOutOfStock()
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { editingProduct = prod },
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    if (isLow) 1.4.dp else 1.dp,
                                    when {
                                        isOut -> UdharCrimson.copy(alpha = 0.8f)
                                        isLow -> WarningAmber.copy(alpha = 0.8f)
                                        prod.autoPriceEnabled -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                        else -> SaffronSecondary.copy(alpha = 0.55f)
                                    }
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = when {
                                        isOut -> UdharCrimson.copy(alpha = 0.04f)
                                        isLow -> WarningAmber.copy(alpha = 0.05f)
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            // Always display prominent Urdu name as primary title
                                            Text(
                                                text = prod.nameUrdu.ifBlank { prod.nameEnglish },
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (prod.nameEnglish.isNotBlank()) {
                                                Text(
                                                    text = prod.nameEnglish,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        // Clear Auto Price vs Manual Price Badge
                                        Surface(
                                            color = if (prod.autoPriceEnabled) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                SaffronSecondary.copy(alpha = 0.16f)
                                            },
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Text(
                                                text = if (prod.autoPriceEnabled) {
                                                    language.pick("آٹو ریٹ", "Auto Price")
                                                } else {
                                                    language.pick("مینوئل ریٹ", "Manual Price")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (prod.autoPriceEnabled) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    SaffronSecondary
                                                },
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "${formatPkr(prod.sellingPrice, language)} / ${unitLabel(prod.unit, language)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${language.pick("خرید", "Cost")}: Rs. ${prod.costPrice.toLong()} • ${language.pick("منافع", "Profit")}: +Rs. ${prod.profitPerUnit().toLong()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${language.pick("اسٹاک", "Stock")}: ${formatQty(prod.stockQty)} ${unitLabel(prod.unit, language)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        isOut -> UdharCrimson
                                                        isLow -> WarningAmber
                                                        else -> ProfitGreen
                                                    }
                                                )
                                                if (isLow) {
                                                    Surface(
                                                        color = if (isOut) UdharCrimson else WarningAmber,
                                                        shape = RoundedCornerShape(50.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isOut) {
                                                                language.pick("ختم", "OUT")
                                                            } else {
                                                                language.pick("کم اسٹاک", "LOW")
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            // Tappable Minimum Stock Threshold chip per product
                                            Surface(
                                                color = if (isLow) {
                                                    WarningAmber.copy(alpha = 0.14f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    if (isLow) WarningAmber.copy(alpha = 0.6f)
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                                ),
                                                modifier = Modifier
                                                    .padding(top = 2.dp)
                                                    .clickable { quickThresholdProduct = prod }
                                                    .testTag("edit_threshold_chip_${prod.id}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Tune,
                                                        contentDescription = null,
                                                        tint = if (isLow) WarningAmber else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                    Text(
                                                        text = "${language.pick("کم از کم حد", "Min Threshold")}: ${formatQty(prod.lowStockThreshold)} ${unitLabel(prod.unit, language)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (isLow) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilledTonalButton(
                                                onClick = { quickPriceProduct = prod },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("quick_edit_price_${prod.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PriceChange,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(Modifier.size(4.dp))
                                                Text(
                                                    text = language.pick("ریٹ تبدیل کریں", "Edit Price"),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            IconButton(
                                                onClick = { movementProduct = prod },
                                                modifier = Modifier.testTag("stock_inout_${prod.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SwapVert,
                                                    contentDescription = language.pick("اسٹاک ان/آؤٹ", "Stock In/Out"),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { editingProduct = prod },
                                                modifier = Modifier.testTag("edit_product_${prod.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = language.pick("مکمل ترمیم", "Edit Item"),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Stock In / Stock Out Audit Log
                    if (movements.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = language.pick(
                                    "ابھی کوئی اسٹاک ان/آؤٹ ریکارڈ نہیں۔ کسی آئٹم کے سامنے ↕ بٹن دبائیں۔",
                                    "No Stock In/Out records yet. Tap ↕ on any item to log stock."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(movements, key = { it.id }) { mov ->
                                val isIn = mov.movementTypeEnum() == StockMovementType.STOCK_IN
                                val isSale = mov.movementTypeEnum() == StockMovementType.SALE
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
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
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isIn) ProfitGreen else if (isSale) MaterialTheme.colorScheme.primary else UdharCrimson
                                            )
                                            Column {
                                                Text(
                                                    text = mov.productNameUrdu.ifBlank { mov.productNameEnglish },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (mov.productNameEnglish.isNotBlank()) {
                                                    Text(
                                                        text = mov.productNameEnglish,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = "${mov.movementTypeEnum().label(language)} • ${
                                                        ReceiptFormatter.formatDateTime(mov.createdAt?.toDate()?.time ?: System.currentTimeMillis())
                                                    }",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                if (mov.note.isNotBlank()) {
                                                    Text(
                                                        text = mov.note,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = (if (isIn) "+" else "-") + formatQty(mov.quantity),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIn) ProfitGreen else UdharCrimson
                                            )
                                            if (mov.profitDelta != 0.0) {
                                                Text(
                                                    text = "${language.pick("منافع", "Profit")}: ${formatPkr(mov.profitDelta, language)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (mov.profitDelta >= 0) ProfitGreen else UdharCrimson
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Detailed Profit & Inventory Analytics Report
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = language.pick("کل خالص منافع", "Total Net Profit"),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = formatPkr(analytics.totalProfitAllTime, language),
                                        style = MaterialTheme.typography.displaySmall,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${language.pick("آج کا منافع", "Today's Profit")}: ${formatPkr(analytics.todayProfit, language)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFFDE68A)
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatMiniCard(
                                    title = language.pick("کل فروخت", "Total Sales"),
                                    value = formatPkr(analytics.totalSalesAllTime, language),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                StatMiniCard(
                                    title = language.pick("موجودہ اسٹاک مالیت", "Inventory Value"),
                                    value = formatPkr(analytics.totalInventoryValue, language),
                                    color = WarningAmber,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Text(
                                text = language.pick("فی آئٹم منافع کی تفصیل", "Per-Item Profit Margin Breakdown"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(products.sortedByDescending { it.profitPerUnit() }.take(15), key = { it.id }) { prod ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.nameUrdu.ifBlank { prod.nameEnglish },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (prod.nameEnglish.isNotBlank()) {
                                            Text(
                                                text = prod.nameEnglish,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${language.pick("خرید", "Buy")}: Rs. ${prod.costPrice.toLong()} → ${language.pick("فروخت", "Sell")}: Rs. ${prod.sellingPrice.toLong()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "+${formatPkr(prod.profitPerUnit(), language)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = ProfitGreen,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activeTab == 0) {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(18.dp)
                    .testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = language.pick("نیا آئٹم شامل کریں", "Add Item"))
            }
        }
    }

    if (quickPriceProduct != null) {
        val target = quickPriceProduct!!
        QuickPriceEditDialog(
            language = language,
            product = target,
            defaultMargin = defaultMarginPercent,
            onSavePrice = { newCost, newSell, newMargin, newAuto ->
                onSaveProduct(
                    target.id,
                    target.nameUrdu,
                    target.nameEnglish,
                    target.categoryEnum(),
                    target.unit,
                    newCost,
                    newSell,
                    newMargin,
                    newAuto,
                    target.stockQty,
                    target.lowStockThreshold,
                    target.barcode
                )
                quickPriceProduct = null
            },
            onDismiss = { quickPriceProduct = null }
        )
    }

    if (quickThresholdProduct != null) {
        val target = quickThresholdProduct!!
        QuickThresholdEditDialog(
            language = language,
            product = target,
            onSaveThreshold = { newThreshold ->
                onSaveProduct(
                    target.id,
                    target.nameUrdu,
                    target.nameEnglish,
                    target.categoryEnum(),
                    target.unit,
                    target.costPrice,
                    target.sellingPrice,
                    target.marginPercent,
                    target.autoPriceEnabled,
                    target.stockQty,
                    newThreshold,
                    target.barcode
                )
                quickThresholdProduct = null
            },
            onDismiss = { quickThresholdProduct = null }
        )
    }

    if (showAddProductDialog || editingProduct != null) {
        ProductEditorDialog(
            language = language,
            existing = editingProduct,
            defaultMargin = defaultMarginPercent,
            onSave = { id, ur, en, cat, unit, cost, sell, margin, auto, stock, low, barcode ->
                onSaveProduct(id, ur, en, cat, unit, cost, sell, margin, auto, stock, low, barcode)
                showAddProductDialog = false
                editingProduct = null
            },
            onDelete = { id ->
                onDeleteProduct(id)
                editingProduct = null
            },
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            }
        )
    }

    if (movementProduct != null) {
        StockMovementDialog(
            language = language,
            product = movementProduct!!,
            onConfirm = { type, qty, newCost, note ->
                onRecordStockMovement(movementProduct!!, type, qty, newCost, note)
                movementProduct = null
            },
            onDismiss = { movementProduct = null }
        )
    }
}

@Composable
private fun StatMiniCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color)
        }
    }
}

@Composable
private fun QuickPriceEditDialog(
    language: AppLanguage,
    product: Product,
    defaultMargin: Double,
    onSavePrice: (Double, Double, Double, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var costStr by remember(product.id) { mutableStateOf(product.costPrice.toLong().toString()) }
    var marginStr by remember(product.id) { mutableStateOf(product.marginPercent.toString()) }
    var autoPrice by remember(product.id) { mutableStateOf(product.autoPriceEnabled) }
    var sellStr by remember(product.id) { mutableStateOf(product.sellingPrice.toLong().toString()) }

    val parsedCost = costStr.toDoubleOrNull() ?: 0.0
    val parsedMargin = marginStr.toDoubleOrNull() ?: defaultMargin
    val autoPreviewPrice = GujratCatalogData.calculateAutoSellingPrice(parsedCost, parsedMargin)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = language.pick("ریٹ تبدیل کریں", "Quick Edit Price"),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.nameUrdu.ifBlank { product.nameEnglish } +
                        if (product.nameEnglish.isNotBlank()) " (${product.nameEnglish})" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Auto Price vs Manual Price Toggle
                Surface(
                    color = if (autoPrice) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    else SaffronSecondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (autoPrice) {
                                    language.pick("آٹو ریٹ (Auto Price)", "Auto Price (آٹو ریٹ)")
                                } else {
                                    language.pick("مینوئل ریٹ (Manual Price)", "Manual Price (مینوئل ریٹ)")
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (autoPrice) MaterialTheme.colorScheme.primary else SaffronSecondary
                            )
                            Text(
                                text = if (autoPrice) {
                                    language.pick(
                                        "خرید ریٹ اور منافع % سے قیمت خودکار نکلے گی",
                                        "Selling price auto-calculated from cost + margin %"
                                    )
                                } else {
                                    language.pick(
                                        "آپ کی درج کردہ قیمت محفوظ رہے گی — خودکار تبدیل نہیں ہوگی",
                                        "Your entered selling price is locked — never overwritten"
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoPrice,
                            onCheckedChange = { enabled ->
                                autoPrice = enabled
                                if (enabled) {
                                    sellStr = GujratCatalogData
                                        .calculateAutoSellingPrice(parsedCost, parsedMargin)
                                        .toLong()
                                        .toString()
                                }
                            },
                            modifier = Modifier.testTag("quick_price_auto_switch")
                        )
                    }
                }

                // Always-editable Selling Price: typing manually automatically switches to Manual Price mode
                OutlinedTextField(
                    value = sellStr,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { c -> c.isDigit() || c == '.' }
                        sellStr = cleaned
                        if (autoPrice) {
                            autoPrice = false
                        }
                    },
                    label = {
                        Text(
                            language.pick(
                                "فروخت ریٹ (Rs.) — ٹائپ کریں تو مینوئل ہو جائے گا",
                                "Selling Price (Rs.) — Typing switches to Manual"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_selling_price_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { raw ->
                            val cleaned = raw.filter { c -> c.isDigit() || c == '.' }
                            costStr = cleaned
                            if (autoPrice) {
                                val nextCost = cleaned.toDoubleOrNull() ?: 0.0
                                sellStr = GujratCatalogData
                                    .calculateAutoSellingPrice(nextCost, parsedMargin)
                                    .toLong()
                                    .toString()
                            }
                        },
                        label = { Text(language.pick("خرید ریٹ (Rs.)", "Cost (Rs.)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = marginStr,
                        onValueChange = { raw ->
                            val cleaned = raw.filter { c -> c.isDigit() || c == '.' }
                            marginStr = cleaned
                            if (autoPrice) {
                                val nextMargin = cleaned.toDoubleOrNull() ?: defaultMargin
                                sellStr = GujratCatalogData
                                    .calculateAutoSellingPrice(parsedCost, nextMargin)
                                    .toLong()
                                    .toString()
                            }
                        },
                        label = { Text(language.pick("منافع %", "Margin %")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = autoPrice,
                        modifier = Modifier.weight(0.9f)
                    )
                }

                val previewProfit = ((sellStr.toDoubleOrNull() ?: autoPreviewPrice) - parsedCost).coerceAtLeast(0.0)
                Text(
                    text = "${language.pick("فی یونٹ منافع", "Profit per unit")}: +${formatPkr(previewProfit, language)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ProfitGreen
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSell = if (autoPrice) autoPreviewPrice else (sellStr.toDoubleOrNull() ?: parsedCost)
                    onSavePrice(parsedCost, finalSell, parsedMargin, autoPrice)
                },
                enabled = (sellStr.toDoubleOrNull() ?: 0.0) > 0.0 || (autoPrice && parsedCost > 0.0),
                modifier = Modifier.testTag("quick_price_save_button")
            ) {
                Text(language.pick("ریٹ محفوظ کریں", "Save Price"))
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
private fun ProductEditorDialog(
    language: AppLanguage,
    existing: Product?,
    defaultMargin: Double,
    onSave: (String?, String, String, ProductCategory, String, Double, Double, Double, Boolean, Double, Double, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nameUrdu by remember { mutableStateOf(existing?.nameUrdu ?: "") }
    var nameEnglish by remember { mutableStateOf(existing?.nameEnglish ?: "") }
    var category by remember { mutableStateOf(existing?.categoryEnum() ?: ProductCategory.OTHER) }
    var unit by remember { mutableStateOf(existing?.unit ?: "kg") }
    var costStr by remember { mutableStateOf(existing?.costPrice?.toLong()?.toString() ?: "") }
    var marginStr by remember { mutableStateOf((existing?.marginPercent ?: defaultMargin).toString()) }
    var autoPrice by remember { mutableStateOf(existing?.autoPriceEnabled ?: true) }
    var sellStr by remember { mutableStateOf(existing?.sellingPrice?.toLong()?.toString() ?: "") }
    var stockStr by remember { mutableStateOf(existing?.stockQty?.let { formatQty(it) } ?: "10") }
    var lowStr by remember { mutableStateOf(existing?.lowStockThreshold?.let { formatQty(it) } ?: "5") }
    var barcode by remember { mutableStateOf(existing?.barcode ?: "") }

    val parsedCost = costStr.toDoubleOrNull() ?: 0.0
    val parsedMargin = marginStr.toDoubleOrNull() ?: defaultMargin
    val autoPreviewPrice = GujratCatalogData.calculateAutoSellingPrice(parsedCost, parsedMargin)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) language.pick("نیا کریانہ آئٹم شامل کریں", "Add New Kiryana Item")
                else language.pick("آئٹم اور قیمت میں ترمیم", "Edit Item & Price")
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = nameUrdu,
                    onValueChange = { nameUrdu = it },
                    label = { Text("اردو نام (Urdu Name)*") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_urdu_name_input")
                )
                OutlinedTextField(
                    value = nameEnglish,
                    onValueChange = { nameEnglish = it },
                    label = { Text("انگلش نام (English Name)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_english_name_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { raw ->
                            val cleaned = raw.filter { c -> c.isDigit() || c == '.' }
                            costStr = cleaned
                            if (autoPrice) {
                                val nextCost = cleaned.toDoubleOrNull() ?: 0.0
                                sellStr = GujratCatalogData
                                    .calculateAutoSellingPrice(nextCost, parsedMargin)
                                    .toLong()
                                    .toString()
                            }
                        },
                        label = { Text(language.pick("خرید ریٹ (Rs.)", "Cost Price (Rs.)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(language.pick("یونٹ (kg/pack)", "Unit")) },
                        singleLine = true,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                // Clear Auto Price (آٹو ریٹ) vs Manual Price (مینوئل ریٹ) Toggle
                Surface(
                    color = if (autoPrice) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else SaffronSecondary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (autoPrice) {
                                    language.pick("آٹو ریٹ (Auto Price)", "Auto Price (آٹو ریٹ)")
                                } else {
                                    language.pick("مینوئل ریٹ (Manual Price)", "Manual Price (مینوئل ریٹ)")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (autoPrice) MaterialTheme.colorScheme.primary else SaffronSecondary
                            )
                            Text(
                                text = if (autoPrice) {
                                    language.pick(
                                        "خرید ریٹ اور منافع % کے مطابق خودکار قیمت",
                                        "Auto-calculated from cost + margin %"
                                    )
                                } else {
                                    language.pick(
                                        "مینوئل ریٹ فعال — ایپ قیمت خودکار تبدیل نہیں کرے گی",
                                        "Manual Price locked — app will never overwrite it"
                                    )
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoPrice,
                            onCheckedChange = { enabled ->
                                autoPrice = enabled
                                if (enabled) {
                                    sellStr = GujratCatalogData
                                        .calculateAutoSellingPrice(parsedCost, parsedMargin)
                                        .toLong()
                                        .toString()
                                }
                            },
                            modifier = Modifier.testTag("product_auto_price_switch")
                        )
                    }
                }

                // Always-editable Selling Price — typing automatically flips to Manual Price mode
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = sellStr,
                        onValueChange = { raw ->
                            val cleaned = raw.filter { c -> c.isDigit() || c == '.' }
                            sellStr = cleaned
                            if (autoPrice) {
                                autoPrice = false
                            }
                        },
                        label = {
                            Text(
                                if (autoPrice) language.pick("فروخت ریٹ (Rs.)", "Selling Price (Rs.)")
                                else language.pick("مینوئل فروخت ریٹ (Rs.)", "Manual Selling Price (Rs.)")
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("product_selling_price_input")
                    )
                    OutlinedTextField(
                        value = marginStr,
                        onValueChange = { raw ->
                            val cleaned = raw.filter { c -> c.isDigit() || c == '.' }
                            marginStr = cleaned
                            if (autoPrice) {
                                val nextMargin = cleaned.toDoubleOrNull() ?: defaultMargin
                                sellStr = GujratCatalogData
                                    .calculateAutoSellingPrice(parsedCost, nextMargin)
                                    .toLong()
                                    .toString()
                            }
                        },
                        label = { Text(language.pick("منافع %", "Margin %")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        enabled = autoPrice,
                        modifier = Modifier.weight(0.8f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(language.pick("موجودہ اسٹاک", "Stock Qty")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lowStr,
                        onValueChange = { lowStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(language.pick("کم از کم حد (Min Limit)", "Min Stock Threshold")) },
                        supportingText = {
                            Text(
                                language.pick(
                                    "کم ہونے پر POS وارننگ اور PDF",
                                    "Triggers POS alert & PDF"
                                ),
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_low_threshold_input")
                    )
                }

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text(language.pick("بارکوڈ نمبر (اختیاری)", "Barcode (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (existing != null) {
                    OutlinedButton(
                        onClick = { onDelete(existing.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(language.pick("آئٹم حذف کریں", "Delete Item"), color = UdharCrimson)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalSell = if (autoPrice) autoPreviewPrice else (sellStr.toDoubleOrNull() ?: parsedCost)
                    onSave(
                        existing?.id,
                        nameUrdu,
                        nameEnglish,
                        category,
                        unit,
                        parsedCost,
                        finalSell,
                        parsedMargin,
                        autoPrice,
                        stockStr.toDoubleOrNull() ?: 0.0,
                        lowStr.toDoubleOrNull() ?: 5.0,
                        barcode
                    )
                },
                enabled = nameUrdu.isNotBlank() || nameEnglish.isNotBlank()
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
private fun StockMovementDialog(
    language: AppLanguage,
    product: Product,
    onConfirm: (StockMovementType, Double, Double, String) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(StockMovementType.STOCK_IN) }
    var qtyStr by remember { mutableStateOf("10") }
    var costStr by remember { mutableStateOf(product.costPrice.toLong().toString()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("اسٹاک ان / آؤٹ اندراج", "Record Stock In / Out")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = product.nameUrdu.ifBlank { product.nameEnglish },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (product.nameEnglish.isNotBlank()) {
                    Text(
                        text = product.nameEnglish,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == StockMovementType.STOCK_IN,
                        onClick = { type = StockMovementType.STOCK_IN },
                        label = { Text(language.pick("اسٹاک ان (+)", "Stock In (+)")) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == StockMovementType.STOCK_OUT,
                        onClick = { type = StockMovementType.STOCK_OUT },
                        label = { Text(language.pick("اسٹاک آؤٹ (-)", "Stock Out (-)")) },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(language.pick("مقدار (${unitLabel(product.unit, language)})", "Quantity (${product.unit})")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == StockMovementType.STOCK_IN) {
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(language.pick("نیا خرید ریٹ (Rs.)", "New Purchase Rate (Rs.)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(language.pick("تفصیل / منڈی بل نمبر", "Note / Mandi Ref")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val cost = costStr.toDoubleOrNull() ?: product.costPrice
                    if (qty > 0) onConfirm(type, qty, cost, note)
                },
                enabled = (qtyStr.toDoubleOrNull() ?: 0.0) > 0.0
            ) {
                Text(language.pick("محفوظ کریں", "Confirm"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("منسوخ", "Cancel"))
            }
        }
    )
}
