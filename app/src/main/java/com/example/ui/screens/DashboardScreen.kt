package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Order
import com.example.data.Product
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.ui.components.ShopLogoBadge
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.ShopAnalytics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    language: AppLanguage,
    analytics: ShopAnalytics,
    recentOrders: List<Order>,
    lowStockProducts: List<Product>,
    rationCardsServedCount: Int,
    onGoToBilling: () -> Unit,
    onOpenBarcodeScanner: () -> Unit,
    onGoToStock: () -> Unit,
    onGoToKhata: () -> Unit,
    onOpenMaryamRationCard: () -> Unit,
    onOpenRationFund: () -> Unit,
    onSyncGujratRates: () -> Unit,
    onOpenReceipt: (Order) -> Unit,
    todayManualCashNet: Double = 0.0,
    onOpenRoznamcha: () -> Unit = {},
    onOpenCustomerOrder: () -> Unit = {},
    onEditOrder: (Order) -> Unit = {},
    onDeleteOrder: (Order) -> Unit = {},
    onOpenLowStockReport: () -> Unit = {},
    settings: ShopSettings = ShopSettings()
) {
    val context = LocalContext.current
    val customPhotoPath = remember { getSavedShopPhotoPath(context) }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }
    val liveTimeText = remember(nowMillis) {
        SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(nowMillis))
    }
    val liveDateText = remember(nowMillis) {
        SimpleDateFormat("EEE, dd MMM yyyy", Locale.US).format(Date(nowMillis))
    }

    val displayShopTitle = if (language == AppLanguage.URDU) {
        settings.storeNameUrdu.ifBlank { "کریانہ اینڈ جنرل سٹور" }
    } else {
        settings.storeNameEnglish.ifBlank { "Kiryana & General Store" }
    }
    val displayShopAddress = if (language == AppLanguage.URDU) {
        settings.shopAddressUrdu
    } else {
        settings.shopAddressEnglish
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 740.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Modern Executive Fintech Header with Live Clock & Dynamic Shop Title
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.2.dp, GoldShimmer.copy(alpha = 0.7f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldPrimaryDark, EmeraldPrimary, EmeraldBright)
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.22f),
                                shape = RoundedCornerShape(50.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.24f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = GoldShimmer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = liveDateText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                    Text(text = "•", color = GoldShimmer, style = MaterialTheme.typography.labelSmall)
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccessTime,
                                            contentDescription = null,
                                            tint = Color(0xFF86EFAC),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = liveTimeText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF86EFAC)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50.dp),
                                border = BorderStroke(1.dp, GoldShimmer.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50.dp))
                                    .clickable(onClick = onSyncGujratRates)
                                    .testTag("dashboard_rate_sync_chip")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        tint = GoldShimmer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = language.pick("ریٹ اپڈیٹ", "Update Rates"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(listOf(GoldShimmer, SaffronSecondary)),
                                        shape = CircleShape
                                    )
                            ) {
                                ShopLogoBadge(
                                    resId = R.drawable.img_shop_logo,
                                    contentDescription = displayShopTitle,
                                    customPhotoPath = customPhotoPath,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = displayShopTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = if (language == AppLanguage.URDU) 22.sp else 19.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = displayShopAddress.ifBlank {
                                        language.pick(
                                            "پروفیشنل POS • کھاتہ • روزنامچہ • اسٹاک منیجر",
                                            "Smart POS • Khata Ledger • Daily Cashbook • Stock"
                                        )
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldShimmer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
                        // 2. Elevated Quick Action Dock
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Default.AddShoppingCart,
                            label = language.pick("نیا بل", "New Bill"),
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White,
                            modifier = Modifier.weight(1f).testTag("quick_new_bill_button"),
                            onClick = onGoToBilling
                        )
                        QuickActionTile(
                            icon = Icons.Default.QrCodeScanner,
                            label = language.pick("بارکوڈ", "Scan"),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f).testTag("quick_scan_button"),
                            onClick = onOpenBarcodeScanner
                        )
                        QuickActionTile(
                            icon = Icons.Default.Inventory2,
                            label = language.pick("اسٹاک", "Stock"),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f).testTag("quick_stock_button"),
                            onClick = onGoToStock
                        )
                        QuickActionTile(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = language.pick("ادھار", "Khata"),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.weight(1f).testTag("quick_khata_button"),
                            onClick = onGoToKhata
                        )
                    }
                }
            }

            // 2a. Daily Roznamcha & Customer WhatsApp Order Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenRoznamcha)
                            .testTag("dashboard_roznamcha_card"),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.45f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(ProfitGreen.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(19.dp))
                                }
                                Text(
                                    text = language.pick("روزنامچہ", "Roznamcha"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = language.pick("دستی سیل و اخراجات", "Manual Cash In/Out"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = "${language.pick("آج کی بچت", "Today Net")}: ${formatPkr(todayManualCashNet, language)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (todayManualCashNet >= 0.0) ProfitGreen else UdharCrimson,
                                maxLines = 1
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenCustomerOrder)
                            .testTag("dashboard_customer_order_card"),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, SaffronSecondary.copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(SaffronSecondary.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = SaffronSecondary, modifier = Modifier.size(19.dp))
                                }
                                Text(
                                    text = language.pick("سامان آرڈر", "Order Now"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = language.pick("واٹس ایپ + آٹو GPS", "WhatsApp + Auto-GPS"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                            Text(
                                text = language.pick("لوکیشن کے ساتھ آرڈر", "Live Maps Delivery"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SaffronSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2b. Compact Bills & Ration Charity Fund Summary Shortcut Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onGoToBilling),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = language.pick("تمام بل و رسیدیں", "All Bills & Receipts"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = language.pick("${recentOrders.size} بل محفوظ", "${recentOrders.size} bills saved"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenRationFund)
                            .testTag("dashboard_ration_fund_card"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, SaffronSecondary.copy(alpha = 0.45f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = SaffronSecondary, modifier = Modifier.size(22.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = language.pick("راشن امداد فنڈ", "Ration Charity"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = language.pick("مستحق خاندانوں کی مدد", "HBL Charity Fund"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 3. KPI Metric Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(
                            title = language.pick("آج کی فروخت", "Today's Sales"),
                            value = formatPkr(analytics.todaySales, language),
                            subtitle = language.pick(
                                "کل: ${formatPkr(analytics.totalSalesAllTime, AppLanguage.URDU)}",
                                "All-time: ${formatPkr(analytics.totalSalesAllTime, AppLanguage.ENGLISH)}"
                            ),
                            icon = Icons.Default.PointOfSale,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        KpiCard(
                            title = language.pick("آج کا منافع", "Today's Profit"),
                            value = formatPkr(analytics.todayProfit, language),
                            subtitle = language.pick(
                                "کل منافع: ${formatPkr(analytics.totalProfitAllTime, AppLanguage.URDU)}",
                                "Total: ${formatPkr(analytics.totalProfitAllTime, AppLanguage.ENGLISH)}"
                            ),
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            accentColor = ProfitGreen,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        KpiCard(
                            title = language.pick("کل ادھار وصولی", "Total Udhar Due"),
                            value = formatPkr(analytics.totalUdharReceivable, language),
                            subtitle = language.pick("کسٹمر کھاتہ", "Customer Ledger"),
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = UdharCrimson,
                            modifier = Modifier.weight(1f).clickable(onClick = onGoToKhata)
                        )
                        KpiCard(
                            title = language.pick("اسٹاک مالیت", "Stock Value"),
                            value = formatPkr(analytics.totalInventoryValue, language),
                            subtitle = language.pick(
                                "کم اسٹاک: ${analytics.lowStockCount} اشیاء",
                                "Low stock: ${analytics.lowStockCount} items"
                            ),
                            icon = Icons.Default.WarningAmber,
                            accentColor = if (analytics.lowStockCount > 0) WarningAmber else EmeraldPrimary,
                            modifier = Modifier.weight(1f).clickable(onClick = onGoToStock)
                        )
                    }
                }
            }

            // 4. 7-Day Sales Analytics Graph
            item {
                val weekTotal = remember(analytics.dailySalesLast7Days) { analytics.dailySalesLast7Days.sumOf { it.second } }
                val weekAvg = remember(weekTotal, analytics.dailySalesLast7Days) {
                    weekTotal / analytics.dailySalesLast7Days.size.coerceAtLeast(1)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = language.pick("گزشتہ 7 دنوں کی فروخت کا گراف", "Last 7 Days Sales Analytics"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = language.pick(
                                        "ہفتہ وار کل: ${formatPkr(weekTotal, AppLanguage.URDU)} • یومیہ اوسط: ${formatPkr(weekAvg, AppLanguage.URDU)}",
                                        "7-Day Total: ${formatPkr(weekTotal, AppLanguage.ENGLISH)} • Avg/Day: ${formatPkr(weekAvg, AppLanguage.ENGLISH)}"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        SalesBarChart(
                            buckets = analytics.dailySalesLast7Days,
                            barColor = MaterialTheme.colorScheme.primary,
                            highlightColor = SaffronSecondary
                        )
                    }
                }
            }

            // 5. Low Stock Alerts (if any)
            if (lowStockProducts.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language.pick("کم اسٹاک الرٹ (${lowStockProducts.size})", "Low Stock Alert (${lowStockProducts.size})"),
                                    style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = onOpenLowStockReport,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = UdharCrimson, contentColor = Color.White),
                                        modifier = Modifier.testTag("dashboard_low_stock_pdf_button")
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text(language.pick("رپورٹ PDF", "PDF Report"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                    FilledTonalButton(
                                        onClick = onGoToStock,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(language.pick("اسٹاک", "Stock"), style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            lowStockProducts.take(4).forEach { prod ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(
                                        text = if (language == AppLanguage.URDU) prod.nameUrdu else prod.nameEnglish,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${formatQty(prod.stockQty)} / ${formatQty(prod.lowStockThreshold)} ${unitLabel(prod.unit, language)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (prod.isOutOfStock()) UdharCrimson else WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.height(74.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth().height(3.5.dp).background(accentColor))
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                    }
                }
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = accentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SalesBarChart(
    buckets: List<Pair<String, Double>>,
    barColor: Color,
    highlightColor: Color
) {
    val maxVal = (buckets.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(100.0)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            buckets.forEachIndexed { index, (_, amount) ->
                val shortAmt = when {
                    amount >= 1000.0 -> String.format(Locale.US, "%.1fk", amount / 1000.0)
                    amount > 0.0 -> "${amount.toLong()}"
                    else -> "0"
                }
                Text(
                    text = shortAmt,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (index == buckets.lastIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (index == buckets.lastIndex) highlightColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(104.dp)) {
            val count = buckets.size.coerceAtLeast(1)
            val gap = 14f
            val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(12f)
            buckets.forEachIndexed { index, pair ->
                val ratio = (pair.second / maxVal).toFloat().coerceIn(0.08f, 1f)
                val barHeight = size.height * ratio
                val x = index * (barWidth + gap)
                val y = size.height - barHeight
                drawRoundRect(
                    color = if (index == buckets.lastIndex) highlightColor else barColor.copy(alpha = 0.82f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            buckets.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
