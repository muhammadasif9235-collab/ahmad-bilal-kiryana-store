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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.Order
import com.example.data.OrderStatus
import com.example.data.Product
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.ui.components.AsyncBannerImage
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.ShopAnalytics

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
    onOpenReceipt: (Order) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 740.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Royal Emerald & Gold Marquee Banner — "احمد بلال کریانہ سٹور ٹانڈہ" in bold display typography
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(198.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .border(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(listOf(GoldShimmer, SaffronSecondary, GoldShimmer)),
                            shape = RoundedCornerShape(26.dp)
                        )
                ) {
                    AsyncBannerImage(
                        resId = R.drawable.img_kiryana_banner_1795314088201,
                        contentDescription = language.pick("احمد بلال کریانہ سٹور ٹانڈہ", "Ahmad Bilal Kiryana Store, Tanda"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xE8042B1E),
                                        Color(0xCC084C36),
                                        Color(0xDD052B1E)
                                    )
                                )
                            )
                            .drawBehind {
                                val filigree = Color(0x30FDE68A)
                                drawCircle(
                                    color = filigree,
                                    radius = size.height * 0.62f,
                                    center = Offset(size.width * 0.06f, size.height * 0.12f),
                                    style = Stroke(width = 2.5f)
                                )
                                drawCircle(
                                    color = filigree,
                                    radius = size.height * 0.48f,
                                    center = Offset(size.width * 0.94f, size.height * 0.88f),
                                    style = Stroke(width = 2f)
                                )
                            }
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Badge & Gujrat Rate Sync Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SaffronSecondary,
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = language.pick("ٹانڈہ • ضلع گجرات", "Tanda • District Gujrat"),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
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

                        // Bold Prominent Centerpiece Title
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = language.pick("احمد بلال کریانہ سٹور ٹانڈہ", "Ahmad Bilal Kiryana Store, Tanda"),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = if (language == AppLanguage.URDU) 27.sp else 22.sp,
                                    lineHeight = if (language == AppLanguage.URDU) 38.sp else 28.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.Transparent, GoldShimmer, Color.Transparent)
                                        )
                                    )
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = language.pick(
                                    "آج کی فروخت، منافع، اسٹاک، راشن کارڈ اور ادھار کھاتہ",
                                    "Daily Sales, Profit, Inventory, Ration Card & Udhar Khata"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldShimmer.copy(alpha = 0.92f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
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
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_new_bill_button"),
                            onClick = onGoToBilling
                        )
                        QuickActionTile(
                            icon = Icons.Default.QrCodeScanner,
                            label = language.pick("بارکوڈ", "Scan"),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_scan_button"),
                            onClick = onOpenBarcodeScanner
                        )
                        QuickActionTile(
                            icon = Icons.Default.Inventory2,
                            label = language.pick("اسٹاک", "Stock"),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_stock_button"),
                            onClick = onGoToStock
                        )
                        QuickActionTile(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = language.pick("ادھار", "Khata"),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_khata_button"),
                            onClick = onGoToKhata
                        )
                    }
                }
            }

            // 2b. Maryam Nawaz Ration Card (Punjab Govt NFC Rs. 3,000) Hero Feature Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenMaryamRationCard)
                        .testTag("dashboard_maryam_ration_card"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, GoldShimmer.copy(alpha = 0.8f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(EmeraldPrimaryDark, EmeraldPrimary, EmeraldBright)
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(SaffronSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = language.pick(
                                                "مریم نواز راشن کارڈ (NFC)",
                                                "Maryam Nawaz Ration Card"
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Nfc,
                                            contentDescription = null,
                                            tint = GoldShimmer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = language.pick(
                                            "حکومت پنجاب 3,000 روپے راشن • $rationCardsServedCount بل محفوظ",
                                            "Govt of Punjab Rs. 3,000 Subsidy • $rationCardsServedCount served"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldShimmer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = language.pick("اسکین / بل", "Scan / Bill"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2c. Ration Charity Fund Featured Card (Muhammad Asif HBL Account)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenRationFund)
                        .testTag("dashboard_ration_fund_card"),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, SaffronSecondary.copy(alpha = 0.45f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SaffronSecondary.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolunteerActivism,
                                    contentDescription = null,
                                    tint = SaffronSecondary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = language.pick(
                                        "راشن امداد فنڈ (مستحق خاندانوں کی مدد)",
                                        "Ration Charity Fund (Donate for Needy)"
                                    ),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = language.pick(
                                        "HBL: Muhammad Asif • MOTA (TANDA) • QR کوڈ",
                                        "HBL: Muhammad Asif • MOTA (TANDA) • QR & IBAN"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = language.pick("مدد کریں", "Donate"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // 3. KPI Metric Grid (2x2) with crisp heritage framing
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = language.pick("کل ادھار وصولی", "Total Udhar Due"),
                            value = formatPkr(analytics.totalUdharReceivable, language),
                            subtitle = language.pick("کسٹمر کھاتہ", "Customer Ledger"),
                            icon = Icons.Default.AccountBalanceWallet,
                            accentColor = UdharCrimson,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onGoToKhata)
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
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = onGoToStock)
                        )
                    }
                }
            }

            // 4. 7-Day Sales Mini Bar Chart
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.pick("گزشتہ 7 دنوں کی فروخت", "Last 7 Days Sales"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (analytics.pendingOrderCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = language.pick(
                                            "${analytics.pendingOrderCount} زیرِ التواء آرڈرز",
                                            "${analytics.pendingOrderCount} Pending"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
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
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language.pick("کم اسٹاک الرٹ", "Low Stock Alert"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                                FilledTonalButton(onClick = onGoToStock) {
                                    Text(language.pick("اسٹاک بھریں", "Restock"))
                                }
                            }
                            lowStockProducts.take(4).forEach { prod ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.URDU) prod.nameUrdu else prod.nameEnglish,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${formatQty(prod.stockQty)} ${unitLabel(prod.unit, language)}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = UdharCrimson
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Recent Bills & Receipts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.pick("حالیہ بل اور رسیدیں", "Recent Bills & Receipts"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = language.pick("سب دیکھیں", "See all"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onGoToBilling)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (recentOrders.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = language.pick(
                                    "ابھی کوئی بل نہیں بنا۔ پہلا بل بنانے کے لیے 'نیا بل' دبائیں۔",
                                    "No bills yet. Tap 'New Bill' to create your first Urdu/English receipt."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recentOrders.take(5).size) { idx ->
                    val order = recentOrders[idx]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenReceipt(order) },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = order.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${order.billNumber} • ${order.items.size} ${language.pick("اشیاء", "items")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatPkr(order.totalAmount, language),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (order.statusEnum() == OrderStatus.PENDING) {
                                        language.pick("زیرِ التواء", "Pending")
                                    } else if (order.remainingUdhar > 0) {
                                        language.pick("ادھار", "Udhar")
                                    } else {
                                        language.pick("نقد", "Cash")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (order.remainingUdhar > 0) UdharCrimson else ProfitGreen
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
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .background(accentColor)
            )
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp)
        ) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            buckets.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
