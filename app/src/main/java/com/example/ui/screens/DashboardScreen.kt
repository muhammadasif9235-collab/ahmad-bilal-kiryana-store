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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
    // Live ticking clock & date (updates every second)
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

    var orderToDelete by remember { mutableStateOf<Order?>(null) }
    val pendingDelete = orderToDelete
    if (pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { orderToDelete = null },
            title = {
                Text(
                    text = language.pick("بل مستقل ڈیلیٹ کریں؟", "Delete Bill Permanently?"),
                    fontWeight = FontWeight.Bold,
                    color = UdharCrimson
                )
            },
            text = {
                Text(
                    text = language.pick(
                        "کیا آپ واقعی بل نمبر ${pendingDelete.billNumber} (${pendingDelete.customerName}) کو مستقل طور پر ڈیلیٹ کرنا چاہتے ہیں؟",
                        "Are you sure you want to permanently delete Bill #${pendingDelete.billNumber} (${pendingDelete.customerName})?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteOrder(pendingDelete)
                        orderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UdharCrimson)
                ) {
                    Text(language.pick("جی ہاں، ڈیلیٹ کریں", "Yes, Delete"), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { orderToDelete = null }) {
                    Text(language.pick("منسوخ", "Cancel"))
                }
            }
        )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        // Top Live Date & Ticking Clock Bar + Rate Sync Button
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
                                    Text(
                                        text = "•",
                                        color = GoldShimmer,
                                        style = MaterialTheme.typography.labelSmall
                                    )
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

                        // Store Emblem + Dynamic Store Name & Address
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(listOf(GoldShimmer, SaffronSecondary)),
                                        shape = CircleShape
                                    )
                                    .padding(2.dp)
                            ) {
                                ShopLogoBadge(
                                    resId = R.drawable.img_shop_logo,
                                    contentDescription = displayShopTitle,
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
                                if (displayShopAddress.isNotBlank()) {
                                    Text(
                                        text = displayShopAddress,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GoldShimmer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = language.pick(
                                            "پروفیشنل POS • کھاتہ • روزنامچہ • اسٹاک منیجر",
                                            "Smart POS • Khata Ledger • Daily Cashbook • Stock"
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.88f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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

            // 2a. Standalone Daily Roznamcha & Customer WhatsApp + GPS Order Cards
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
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(19.dp)
                                    )
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${language.pick("آج کی بچت", "Today Net")}: ${formatPkr(todayManualCashNet, language)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (todayManualCashNet >= 0.0) ProfitGreen else UdharCrimson,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                    Icon(
                                        imageVector = Icons.Default.LocalShipping,
                                        contentDescription = null,
                                        tint = SaffronSecondary,
                                        modifier = Modifier.size(19.dp)
                                    )
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
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = language.pick("لوکیشن کے ساتھ آرڈر", "Live Maps Delivery"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = SaffronSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2b. POS / Maryam Nawaz Ration Card Hero Feature Card
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

            // 2c. Ration Charity Fund Featured Card
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

            // 3. KPI Metric Grid (2x2)
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
                            icon = Icons.Default.PointOSale,
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
                        // 4. Enhanced 7-Day Sales Analytics Graph with Weekly Total & Daily Values
            item {
                val weekTotal = remember(analytics.dailySalesLast7Days) {
                    analytics.dailySalesLast7Days.sumOf { it.second }
                }
                val weekAvg = remember(weekTotal, analytics.dailySalesLast7Days) {
                    val count = analytics.dailySalesLast7Days.size.coerceAtLeast(1)
                    weekTotal / count
                }
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
                            if (analytics.pendingOrderCount > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(50.dp)
                                ) {
                                    Text(
                                        text = language.pick(
                                            "${analytics.pendingOrderCount} زیرِ التواء",
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
                                    text = language.pick(
                                        "کم اسٹاک الرٹ (${lowStockProducts.size})",
                                        "Low Stock Alert (${lowStockProducts.size})"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = WarningAmber
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = onOpenLowStockReport,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = UdharCrimson,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.testTag("dashboard_low_stock_pdf_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            text = language.pick("رپورٹ PDF", "PDF Report"),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = onGoToStock,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = language.pick("اسٹاک", "Stock"),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
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
                                IconButton(
                                    onClick = { onEditOrder(order) },
                                    modifier = Modifier.testTag("dashboard_edit_order_${order.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = language.pick("بل ایڈٹ کریں", "Edit Bill"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { orderToDelete = order },
                                    modifier = Modifier.testTag("dashboard_delete_order_${order.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = language.pick("بل ڈیلیٹ کریں", "Delete Bill"),
                                        tint = UdharCrimson,
                                        modifier = Modifier.size(18.dp)
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
        // Amount labels above each bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

                            
