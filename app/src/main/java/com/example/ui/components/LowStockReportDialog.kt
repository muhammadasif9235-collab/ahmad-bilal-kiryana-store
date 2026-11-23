package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Product
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.services.ReceiptPrintHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber

private val QUICK_THRESHOLD_PRESETS = listOf(2, 5, 10, 15, 20, 50)

/**
 * High-visibility visual Low Stock Warning Banner displayed on POS / Billing screens
 * whenever one or more products fall at or below their configured minimum stock threshold.
 */
@Composable
fun PosLowStockWarningBanner(
    language: AppLanguage,
    lowStockProducts: List<Product>,
    onOpenLowStockReport: () -> Unit,
    modifier: Modifier = Modifier,
    showOnlyLowStockActive: Boolean = false,
    onToggleLowStockFilter: (() -> Unit)? = null
) {
    AnimatedVisibility(
        visible = lowStockProducts.isNotEmpty(),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val sortedUrgent = remember(lowStockProducts) {
            lowStockProducts.sortedWith(
                compareBy<Product> { if (it.isOutOfStock()) 0 else 1 }
                    .thenBy { if (it.lowStockThreshold > 0.0) it.stockQty / it.lowStockThreshold else 0.0 }
            )
        }
        val outCount = remember(sortedUrgent) { sortedUrgent.count { it.isOutOfStock() } }
        val isCritical = outCount > 0
        val borderColor = if (isCritical) UdharCrimson else WarningAmber
        val accentTint = if (isCritical) Color(0xFFFEF2F2) else Color(0xFFFFFBEB)
        val badgeColor = if (isCritical) UdharCrimson else WarningAmber

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenLowStockReport() }
                .testTag("pos_low_stock_warning_banner"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.3.dp, borderColor.copy(alpha = 0.75f)),
            colors = CardDefaults.cardColors(containerColor = accentTint)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(badgeColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = language.pick("کم اسٹاک وارننگ", "Low Stock Warning"),
                                tint = badgeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCritical) {
                                    language.pick(
                                        "کم اسٹاک وارننگ: ${sortedUrgent.size} اشیاء ($outCount ختم!)",
                                        "Low Stock Alert: ${sortedUrgent.size} items ($outCount out!)"
                                    )
                                } else {
                                    language.pick(
                                        "کم اسٹاک وارننگ: ${sortedUrgent.size} اشیاء حد سے کم ہیں",
                                        "Low Stock Warning: ${sortedUrgent.size} items below min limit"
                                    )
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = badgeColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = language.pick(
                                    "ری اسٹاک کے لیے PDF رپورٹ بنائیں یا اشیاء دیکھیں",
                                    "Tap to generate Low Stock Report PDF for easy replenishment"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF57534E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Compact scrollable preview pills of urgent low-stock items
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    sortedUrgent.take(6).forEach { prod ->
                        val pillColor = if (prod.isOutOfStock()) UdharCrimson else WarningAmber
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, pillColor.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "${prod.nameUrdu.ifBlank { prod.nameEnglish }}: ${formatQty(prod.stockQty)}/${formatQty(prod.lowStockThreshold)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = pillColor,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    if (sortedUrgent.size > 6) {
                        Surface(
                            color = badgeColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "+${sortedUrgent.size - 6}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Quick action row: Filter Low Stock + Generate Low Stock Report PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onToggleLowStockFilter != null) {
                        OutlinedButton(
                            onClick = onToggleLowStockFilter,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, badgeColor),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (showOnlyLowStockActive) badgeColor else Color.White,
                                contentColor = if (showOnlyLowStockActive) Color.White else badgeColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pos_low_stock_filter_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = if (showOnlyLowStockActive) {
                                    language.pick("تمام اشیاء دکھائیں", "Show All")
                                } else {
                                    language.pick("صرف کم اسٹاک", "Filter Low")
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Button(
                        onClick = onOpenLowStockReport,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = badgeColor,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1.25f)
                            .testTag("pos_generate_low_stock_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(5.dp))
                        Text(
                            text = language.pick("کم اسٹاک PDF رپورٹ", "Low Stock Report PDF"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Full Low Stock Replenishment Report Dialog allowing shopkeepers to inspect all low-stock items,
 * adjust per-product thresholds, quick-restock items, and generate/share/print the A4 PDF report.
 */
@Composable
fun LowStockReportDialog(
    language: AppLanguage,
    settings: ShopSettings,
    lowStockProducts: List<Product>,
    onUpdateThreshold: (Product, Double) -> Unit,
    onQuickRestock: ((Product, Double) -> Unit)? = null,
    onReportGeneratedMessage: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var editingThresholdProduct by remember { mutableStateOf<Product?>(null) }

    val sortedLowStock = remember(lowStockProducts) {
        lowStockProducts.sortedWith(
            compareBy<Product> { if (it.isOutOfStock()) 0 else 1 }
                .thenBy { if (it.lowStockThreshold > 0.0) it.stockQty / it.lowStockThreshold else 0.0 }
        )
    }
    val outCount = remember(sortedLowStock) { sortedLowStock.count { it.isOutOfStock() } }
    val totalEstCost = remember(sortedLowStock) { sortedLowStock.sumOf { it.estimatedRestockCost() } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .widthIn(max = 600.dp)
                .testTag("low_stock_report_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header with Emerald + Crimson Alert Accent
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    if (outCount > 0) Color(0xFF991B1B) else Color(0xFFB45309),
                                    EmeraldPrimary
                                )
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = language.pick("کم اسٹاک ری اسٹاک رپورٹ", "Low Stock Report"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = language.pick(
                                        "آسان مال خریداری اور منڈی آرڈر کے لیے PDF رپورٹ",
                                        "Replenishment checklist & A4 PDF export"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_low_stock_report_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = language.pick("بند کریں", "Close"),
                                tint = Color.White
                            )
                        }
                    }
                }

                // KPI Summary Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportKpiPill(
                        label = language.pick("کم اسٹاک", "Low Stock"),
                        value = "${sortedLowStock.size}",
                        accent = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiPill(
                        label = language.pick("ختم شدہ", "Out of Stock"),
                        value = "$outCount",
                        accent = UdharCrimson,
                        modifier = Modifier.weight(1f)
                    )
                    ReportKpiPill(
                        label = language.pick("اندازاً لاگت", "Est. Cost"),
                        value = formatPkr(totalEstCost, language),
                        accent = EmeraldPrimary,
                        modifier = Modifier.weight(1.35f)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Product List or Healthy Stock Empty State
                if (sortedLowStock.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = language.pick(
                                "تمام اشیاء کا اسٹاک محفوظ حد سے اوپر ہے!",
                                "All products are safely above their minimum threshold!"
                            ),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = ProfitGreen
                        )
                        Text(
                            text = language.pick(
                                "کسی بھی پروڈکٹ کی کم از کم حد تبدیل کرنے کے لیے اسٹاک ٹیب میں جائیں۔",
                                "You can adjust per-product minimum thresholds anytime in the Stock tab."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 360.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedLowStock, key = { it.id }) { prod ->
                            val isOut = prod.isOutOfStock()
                            val accent = if (isOut) UdharCrimson else WarningAmber
                            val unitTxt = unitLabel(prod.unit, language)
                            val ratio = if (prod.lowStockThreshold > 0.0) {
                                (prod.stockQty / prod.lowStockThreshold).toFloat().coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                            val suggestedQty = prod.suggestedRestockQty()

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOut) {
                                        UdharCrimson.copy(alpha = 0.05f)
                                    } else {
                                        WarningAmber.copy(alpha = 0.06f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = prod.nameUrdu.ifBlank { prod.nameEnglish },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (prod.nameEnglish.isNotBlank() && prod.nameEnglish != prod.nameUrdu) {
                                                Text(
                                                    text = prod.nameEnglish,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        Surface(
                                            color = accent,
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Text(
                                                text = if (isOut) {
                                                    language.pick("اسٹاک ختم", "OUT")
                                                } else {
                                                    language.pick("کم اسٹاک", "LOW")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        color = accent,
                                        trackColor = accent.copy(alpha = 0.16f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${language.pick("موجودہ", "Now")}: ${formatQty(prod.stockQty)} $unitTxt  •  ${language.pick("کم حد", "Min")}: ${formatQty(prod.lowStockThreshold)} $unitTxt",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accent
                                        )
                                        Text(
                                            text = "${language.pick("آرڈر", "Order")}: +${formatQty(suggestedQty)} (${formatPkr(prod.estimatedRestockCost(), language)})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { editingThresholdProduct = prod },
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("report_edit_threshold_${prod.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Tune,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.size(4.dp))
                                            Text(
                                                text = language.pick("حد تبدیل کریں", "Edit Min Limit"),
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 1
                                            )
                                        }
                                        if (onQuickRestock != null) {
                                            FilledTonalButton(
                                                onClick = { onQuickRestock(prod, suggestedQty) },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("report_quick_restock_${prod.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AddBox,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(Modifier.size(4.dp))
                                                Text(
                                                    text = "+${formatQty(suggestedQty)} ${language.pick("ری اسٹاک", "Restock")}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Footer PDF Generation & Sharing Actions
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val generated = ReceiptPrintHelper.shareLowStockReportPdf(
                                context = context,
                                lowStockProducts = sortedLowStock,
                                language = language,
                                settings = settings,
                                preferWhatsApp = false
                            )
                            if (generated != null) {
                                onReportGeneratedMessage?.invoke(
                                    language.pick(
                                        "کم اسٹاک رپورٹ PDF تیار ہو گئی: ${generated.name}",
                                        "Low Stock Report PDF generated: ${generated.name}"
                                    )
                                )
                            }
                        },
                        enabled = sortedLowStock.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("share_low_stock_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = language.pick(
                                "کم اسٹاک رپورٹ PDF بنائیں اور شیئر کریں",
                                "Generate & Share Low Stock Report PDF"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                ReceiptPrintHelper.openOrPrintLowStockReportPdf(
                                    context = context,
                                    lowStockProducts = sortedLowStock,
                                    language = language,
                                    settings = settings
                                )
                            },
                            enabled = sortedLowStock.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_print_low_stock_pdf_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(5.dp))
                            Text(
                                text = language.pick("PDF کھولیں / پرنٹ", "Open / Print PDF"),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                ReceiptPrintHelper.shareLowStockReportOnWhatsAppText(
                                    context = context,
                                    lowStockProducts = sortedLowStock,
                                    language = language,
                                    settings = settings
                                )
                            },
                            enabled = sortedLowStock.isNotEmpty(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_low_stock_whatsapp_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(5.dp))
                            Text(
                                text = language.pick("واٹس ایپ لسٹ", "WhatsApp List"),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    val targetForThreshold = editingThresholdProduct
    if (targetForThreshold != null) {
        QuickThresholdEditDialog(
            language = language,
            product = targetForThreshold,
            onSaveThreshold = { newThreshold ->
                onUpdateThreshold(targetForThreshold, newThreshold)
                editingThresholdProduct = null
            },
            onDismiss = { editingThresholdProduct = null }
        )
    }
}

@Composable
private fun ReportKpiPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accent.copy(alpha = 0.09f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Fast, dedicated dialog for defining a product's Minimum Stock Threshold.
 */
@Composable
fun QuickThresholdEditDialog(
    language: AppLanguage,
    product: Product,
    onSaveThreshold: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var thresholdStr by remember(product.id) { mutableStateOf(formatQty(product.lowStockThreshold)) }
    val parsedThreshold = (thresholdStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val unitTxt = unitLabel(product.unit, language)
    val willTriggerWarning = (parsedThreshold > 0.0 && product.stockQty <= parsedThreshold) || product.stockQty <= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = WarningAmber
                )
                Column {
                    Text(
                        text = language.pick("کم از کم اسٹاک حد مقرر کریں", "Set Minimum Stock Threshold"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = product.nameUrdu.ifBlank { product.nameEnglish },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                        Text(
                            text = language.pick("موجودہ اسٹاک:", "Current Stock:"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${formatQty(product.stockQty)} $unitTxt",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (willTriggerWarning) UdharCrimson else ProfitGreen
                        )
                    }
                }

                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = { raw -> thresholdStr = raw.filter { c -> c.isDigit() || c == '.' } },
                    label = {
                        Text(
                            language.pick(
                                "کم از کم حد ($unitTxt)",
                                "Minimum Stock Threshold ($unitTxt)"
                            )
                        )
                    },
                    supportingText = {
                        Text(
                            language.pick(
                                "اس مقدار یا اس سے کم ہونے پر POS/بلنگ پر وارننگ آئے گی",
                                "Triggers visual POS/Billing alert & Low Stock Report PDF"
                            )
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_threshold_input")
                )

                // Quick preset chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QUICK_THRESHOLD_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = parsedThreshold == preset.toDouble(),
                            onClick = { thresholdStr = preset.toString() },
                            label = { Text("$preset $unitTxt") },
                            modifier = Modifier.testTag("threshold_preset_$preset")
                        )
                    }
                }

                Surface(
                    color = if (willTriggerWarning) {
                        WarningAmber.copy(alpha = 0.14f)
                    } else {
                        ProfitGreen.copy(alpha = 0.12f)
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (willTriggerWarning) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (willTriggerWarning) WarningAmber else ProfitGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (willTriggerWarning) {
                                language.pick(
                                    "اس حد کے مطابق یہ آئٹم ابھی کم اسٹاک وارننگ اور PDF رپورٹ میں شامل ہو گا۔",
                                    "At this threshold, this product will trigger a Low Stock warning immediately."
                                )
                            } else {
                                language.pick(
                                    "اسٹاک محفوظ حد سے اوپر ہے۔ جب ${formatQty(parsedThreshold)} $unitTxt تک آئے گا تو وارننگ آئے گی۔",
                                    "Stock is healthy. Alert triggers once quantity drops to ${formatQty(parsedThreshold)} $unitTxt."
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveThreshold(parsedThreshold) },
                modifier = Modifier.testTag("quick_threshold_save_button")
            ) {
                Text(language.pick("حد محفوظ کریں", "Save Threshold"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("منسوخ", "Cancel"))
            }
        }
    )
}
