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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DailyCashEntry
import com.example.data.DailyCashType
import com.example.data.DailyPaymentSource
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.pick
import com.example.services.ReceiptPrintHelper
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class RoznamchaPeriod(val urduLabel: String, val englishLabel: String) {
    TODAY("آج", "Today"),
    YESTERDAY("کل", "Yesterday"),
    THIS_WEEK("اس ہفتے", "This Week"),
    THIS_MONTH("اس مہینے", "This Month"),
    ALL("تمام تاریخیں", "All History")
}

/**
 * Keeps the dedicated Daily Roznamcha / Manual Cashbook completely separate from
 * auto-synced itemized POS & Billing receipts.
 */
fun isManualRoznamchaEntry(entry: DailyCashEntry): Boolean {
    val title = entry.categoryOrTitle.trim()
    if (title.startsWith("بل نقد وصولی #") || title.startsWith("POS کارڈ بل #")) {
        return false
    }
    return true
}

@Composable
fun DailyRoznamchaScreen(
    language: AppLanguage,
    settings: ShopSettings,
    allCashEntries: List<DailyCashEntry>,
    onSaveEntry: (
        existingId: String?,
        entryType: DailyCashType,
        paymentSource: DailyPaymentSource,
        title: String,
        amount: Double,
        referenceId: String,
        note: String,
        entryDateMs: Long
    ) -> Unit,
    onDeleteEntry: (String) -> Unit
) {
    val context = LocalContext.current
    var selectedPeriod by remember { mutableStateOf(RoznamchaPeriod.TODAY) }
    var searchQuery by remember { mutableStateOf("") }
    var dialogEntryType by remember { mutableStateOf<DailyCashType?>(null) }
    var editingEntry by remember { mutableStateOf<DailyCashEntry?>(null) }

    val manualEntries = remember(allCashEntries) {
        allCashEntries.filter(::isManualRoznamchaEntry)
    }

    val startOfTodayMs = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val startOfYesterdayMs = startOfTodayMs - 86_400_000L
    val startOfWeekMs = startOfTodayMs - (6L * 86_400_000L)
    val startOfMonthMs = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.US) }

    // Always calculate Today's live manual metrics for the top hero summary
    val todayEntries = remember(manualEntries, startOfTodayMs) {
        manualEntries.filter { it.entryDateMs >= startOfTodayMs }
    }
    val todayCashIn = remember(todayEntries) {
        todayEntries.filter { it.entryTypeEnum() == DailyCashType.CASH_IN }.sumOf { it.amount }
    }
    val todayCashOut = remember(todayEntries) {
        todayEntries.filter { it.entryTypeEnum() == DailyCashType.CASH_OUT }.sumOf { it.amount }
    }
    val todayNetBalance = todayCashIn - todayCashOut

    // Filtered entries by active period + date/note search
    val filteredEntries = remember(manualEntries, selectedPeriod, searchQuery) {
        val byPeriod = when (selectedPeriod) {
            RoznamchaPeriod.TODAY -> manualEntries.filter { it.entryDateMs >= startOfTodayMs }
            RoznamchaPeriod.YESTERDAY -> manualEntries.filter {
                it.entryDateMs in startOfYesterdayMs until startOfTodayMs
            }
            RoznamchaPeriod.THIS_WEEK -> manualEntries.filter { it.entryDateMs >= startOfWeekMs }
            RoznamchaPeriod.THIS_MONTH -> manualEntries.filter { it.entryDateMs >= startOfMonthMs }
            RoznamchaPeriod.ALL -> manualEntries
        }
        val q = searchQuery.trim()
        if (q.isBlank()) {
            byPeriod
        } else {
            byPeriod.filter { entry ->
                val dateStr = dateFormatter.format(Date(entry.entryDateMs))
                entry.categoryOrTitle.contains(q, ignoreCase = true) ||
                    entry.note.contains(q, ignoreCase = true) ||
                    dateStr.contains(q, ignoreCase = true) ||
                    entry.amount.toLong().toString().contains(q)
            }
        }
    }

    val periodIn = remember(filteredEntries) {
        filteredEntries.filter { it.entryTypeEnum() == DailyCashType.CASH_IN }.sumOf { it.amount }
    }
    val periodOut = remember(filteredEntries) {
        filteredEntries.filter { it.entryTypeEnum() == DailyCashType.CASH_OUT }.sumOf { it.amount }
    }
    val periodNet = periodIn - periodOut

    // Group entries day-by-day for clean date-wise history
    val groupedByDate = remember(filteredEntries) {
        filteredEntries.groupBy { dateFormatter.format(Date(it.entryDateMs)) }
    }

    val periodLabel = language.pick(selectedPeriod.urduLabel, selectedPeriod.englishLabel)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 740.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Hero Daily Summary Banner (Today's Manual Cash In / Out / Net Balance)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, GoldShimmer.copy(alpha = 0.7f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(EmeraldPrimaryDark, EmeraldPrimary, EmeraldBright)
                                )
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = GoldShimmer,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = language.pick(
                                        "روزنامچہ — دستی سیل اور اخراجات",
                                        "Daily Roznamcha — Manual Cashbook"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = language.pick(
                                        "آئٹمائزڈ POS بلز سے مکمل الگ • روزانہ دستی آمدن اور اخراجات کا ریکارڈ",
                                        "Standalone manual Cash In / Cash Out ledger separate from POS bills"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Today's 3-column KPI strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RoznamchaHeroMetric(
                                label = language.pick("آج کی دستی سیل", "Today Cash In"),
                                value = formatPkr(todayCashIn, language),
                                accent = Color(0xFF86EFAC),
                                modifier = Modifier.weight(1f)
                            )
                            RoznamchaHeroMetric(
                                label = language.pick("آج کے اخراجات", "Today Cash Out"),
                                value = formatPkr(todayCashOut, language),
                                accent = Color(0xFFFCA5A5),
                                modifier = Modifier.weight(1f)
                            )
                            RoznamchaHeroMetric(
                                label = language.pick("آج کی خالص بچت", "Net Cash Today"),
                                value = formatPkr(todayNetBalance, language),
                                accent = GoldShimmer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Two Big Prominent Buttons: "+ کیش اِن" & "- کیش آؤٹ"
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            editingEntry = null
                            dialogEntryType = DailyCashType.CASH_IN
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfitGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roznamcha_cash_in_button")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(
                                    text = language.pick("کیش اِن (Cash In)", "+ Cash In"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = language.pick("دستی سیل / آمدن", "Manual Sale / Income"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = {
                            editingEntry = null
                            dialogEntryType = DailyCashType.CASH_OUT
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UdharCrimson,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 14.dp, horizontal = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roznamcha_cash_out_button")
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(20.dp))
                                Text(
                                    text = language.pick("کیش آؤٹ (Cash Out)", "- Cash Out"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = language.pick("اخراجات / مال خریداری", "Expense / Purchase"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 3. One-Tap WhatsApp Text & PDF Statement Sharing
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ReceiptPrintHelper.shareRoznamchaStatementOnWhatsAppText(
                                context = context,
                                periodLabel = periodLabel,
                                entries = filteredEntries,
                                totalIn = periodIn,
                                totalOut = periodOut,
                                netBalance = periodNet,
                                language = language,
                                settings = settings
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roznamcha_share_whatsapp_text")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("واٹس ایپ اسٹیٹمنٹ", "WhatsApp Text"), maxLines = 1)
                    }

                    FilledTonalButton(
                        onClick = {
                            ReceiptPrintHelper.shareRoznamchaStatementPdfOnWhatsApp(
                                context = context,
                                periodLabel = periodLabel,
                                entries = filteredEntries,
                                totalIn = periodIn,
                                totalOut = periodOut,
                                netBalance = periodNet,
                                language = language,
                                settings = settings
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("roznamcha_share_whatsapp_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("روزنامچہ PDF شیئر", "Share PDF"), maxLines = 1)
                    }
                }
            }

            // 4. Period Filter Chips + Date / Note Search
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        RoznamchaPeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = { selectedPeriod = period },
                                label = { Text(language.pick(period.urduLabel, period.englishLabel)) },
                                modifier = Modifier.testTag("roznamcha_filter_${period.name.lowercase()}")
                            )
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                language.pick(
                                    "تاریخ (مثلاً 23/11/2026)، تفصیل یا رقم تلاش کریں...",
                                    "Search by date (dd/MM/yyyy), note, or amount..."
                                )
                            )
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("roznamcha_search_input")
                    )
                }
            }

            // 5. Selected Period Subtotal Bar (when viewing non-Today filters)
            if (selectedPeriod != RoznamchaPeriod.TODAY || searchQuery.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${language.pick("کل اِن", "In")}: +${formatPkr(periodIn, language)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ProfitGreen
                            )
                            Text(
                                text = "${language.pick("کل آؤٹ", "Out")}: -${formatPkr(periodOut, language)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = UdharCrimson
                            )
                            Text(
                                text = "${language.pick("بچت", "Net")}: ${formatPkr(periodNet, language)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 6. Date-Wise Grouped Roznamcha Entries
            if (groupedByDate.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = language.pick(
                                    "اس مدت میں کوئی دستی روزنامچہ انٹری نہیں۔ اوپر '+ کیش اِن' یا '- کیش آؤٹ' دبائیں۔",
                                    "No manual cashbook entries for this period. Tap '+ Cash In' or '- Cash Out' above."
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                groupedByDate.forEach { (dateKey, dayList) ->
                    val dayIn = dayList.filter { it.entryTypeEnum() == DailyCashType.CASH_IN }.sumOf { it.amount }
                    val dayOut = dayList.filter { it.entryTypeEnum() == DailyCashType.CASH_OUT }.sumOf { it.amount }
                    val dayNet = dayIn - dayOut

                    item(key = "header_$dateKey") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = dateKey,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "+${dayIn.toLong()}  |  -${dayOut.toLong()}  |  ${language.pick("بچت", "Net")}: ${dayNet.toLong()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dayNet >= 0.0) ProfitGreen else UdharCrimson
                            )
                        }
                    }

                    items(dayList, key = { it.id }) { entry ->
                        val isIn = entry.entryTypeEnum() == DailyCashType.CASH_IN
                        val modeLabel = when (entry.paymentSourceEnum()) {
                            DailyPaymentSource.CASH -> language.pick("نقد", "Cash")
                            DailyPaymentSource.ONLINE -> language.pick("آن لائن", "Online")
                            DailyPaymentSource.HBL_QR -> language.pick("بینک QR", "Bank QR")
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingEntry = entry
                                    dialogEntryType = entry.entryTypeEnum()
                                },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.dp,
                                if (isIn) ProfitGreen.copy(alpha = 0.28f) else UdharCrimson.copy(alpha = 0.28f)
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isIn) ProfitGreen.copy(alpha = 0.14f) else UdharCrimson.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isIn) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                tint = if (isIn) ProfitGreen else UdharCrimson,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.categoryOrTitle.ifBlank {
                                                if (isIn) language.pick("دستی سیل / آمدن", "Manual Cash In")
                                                else language.pick("اخراجات / خریداری", "Cash Out Expense")
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$modeLabel • ${timeFormatter.format(Date(entry.entryDateMs))}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (entry.note.isNotBlank()) {
                                            Text(
                                                text = entry.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = (if (isIn) "+" else "-") + formatPkr(entry.amount, language),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isIn) ProfitGreen else UdharCrimson
                                        )
                                        Text(
                                            text = if (isIn) language.pick("کیش اِن", "Cash In")
                                            else language.pick("کیش آؤٹ", "Cash Out"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isIn) ProfitGreen else UdharCrimson
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            editingEntry = entry
                                            dialogEntryType = entry.entryTypeEnum()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = language.pick("ایڈٹ کریں", "Edit"),
                                            tint = MaterialTheme.colorScheme.primary,
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

    // Add / Edit Roznamcha Entry Dialog
    val activeType = dialogEntryType
    if (activeType != null) {
        RoznamchaEntryDialog(
            language = language,
            initialType = activeType,
            existingEntry = editingEntry,
            onSave = { entryType, source, title, amount, note, entryDateMs ->
                onSaveEntry(
                    editingEntry?.id,
                    entryType,
                    source,
                    title,
                    amount,
                    editingEntry?.referenceId?.ifBlank { "ROZ" } ?: "ROZ",
                    note,
                    entryDateMs
                )
                dialogEntryType = null
                editingEntry = null
            },
            onDelete = editingEntry?.let { entry ->
                {
                    onDeleteEntry(entry.id)
                    dialogEntryType = null
                    editingEntry = null
                }
            },
            onDismiss = {
                dialogEntryType = null
                editingEntry = null
            }
        )
    }
}

@Composable
private fun RoznamchaHeroMetric(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.88f),
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

@Composable
private fun RoznamchaEntryDialog(
    language: AppLanguage,
    initialType: DailyCashType,
    existingEntry: DailyCashEntry?,
    onSave: (
        entryType: DailyCashType,
        paymentSource: DailyPaymentSource,
        title: String,
        amount: Double,
        note: String,
        entryDateMs: Long
    ) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var entryType by remember(existingEntry?.id, initialType) { mutableStateOf(existingEntry?.entryTypeEnum() ?: initialType) }
    var paymentSource by remember(existingEntry?.id) {
        mutableStateOf(existingEntry?.paymentSourceEnum() ?: DailyPaymentSource.CASH)
    }
    var amountStr by remember(existingEntry?.id) {
        mutableStateOf(
            existingEntry?.let {
                if (it.amount % 1.0 == 0.0) it.amount.toLong().toString() else it.amount.toString()
            } ?: ""
        )
    }
    var titleInput by remember(existingEntry?.id, initialType) {
        mutableStateOf(
            existingEntry?.categoryOrTitle ?: if (initialType == DailyCashType.CASH_IN) {
                language.pick("آج کی دستی سیل", "Manual Counter Sale")
            } else {
                language.pick("دکان کا خرچ / سامان خریداری", "Shop Expense / Stock Purchase")
            }
        )
    }
    var noteInput by remember(existingEntry?.id) { mutableStateOf(existingEntry?.note ?: "") }

    val dateFmt = remember { SimpleDateFormat("dd/MM/yyyy", Locale.US) }
    var selectedDateMs by remember(existingEntry?.id) {
        mutableStateOf(existingEntry?.entryDateMs ?: System.currentTimeMillis())
    }
    var customDateStr by remember(existingEntry?.id) {
        mutableStateOf(dateFmt.format(Date(existingEntry?.entryDateMs ?: System.currentTimeMillis())))
    }

    val isCashIn = entryType == DailyCashType.CASH_IN

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingEntry != null) {
                    language.pick("روزنامچہ انٹری ایڈٹ کریں", "Edit Roznamcha Entry")
                } else if (isCashIn) {
                    language.pick("+ کیش اِن (دستی سیل / آمدن)", "+ Cash In (Manual Sale / Income)")
                } else {
                    language.pick("- کیش آؤٹ (اخراجات / مال خریداری)", "- Cash Out (Expense / Purchase)")
                },
                color = if (isCashIn) ProfitGreen else UdharCrimson,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCashIn,
                        onClick = { entryType = DailyCashType.CASH_IN },
                        label = { Text(language.pick("+ کیش اِن", "+ Cash In")) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isCashIn,
                        onClick = { entryType = DailyCashType.CASH_OUT },
                        label = { Text(language.pick("- کیش آؤٹ", "- Cash Out")) },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(language.pick("رقم (روپے PKR)*", "Amount (PKR)*")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roznamcha_amount_input")
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(language.pick("عنوان / مد (مثلاً دستی سیل، بجلی بل، چینی خریداری)*", "Title / Description*")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("roznamcha_title_input")
                )

                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text(language.pick("تفصیلی نوٹ (اختیاری)", "Detailed Note (optional)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = language.pick("ادائیگی کا ذریعہ:", "Payment Mode:"),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = paymentSource == DailyPaymentSource.CASH,
                        onClick = { paymentSource = DailyPaymentSource.CASH },
                        label = { Text(language.pick("نقد", "Cash")) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentSource == DailyPaymentSource.ONLINE,
                        onClick = { paymentSource = DailyPaymentSource.ONLINE },
                        label = { Text(language.pick("آن لائن", "Online")) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = paymentSource == DailyPaymentSource.HBL_QR,
                        onClick = { paymentSource = DailyPaymentSource.HBL_QR },
                        label = { Text(language.pick("بینک QR", "Bank QR")) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Quick Date Preset (Today / Yesterday / Custom dd/MM/yyyy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = {
                            val now = System.currentTimeMillis()
                            selectedDateMs = now
                            customDateStr = dateFmt.format(Date(now))
                        },
                        label = { Text(language.pick("آج", "Today"), style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = false,
                        onClick = {
                            val yesterday = System.currentTimeMillis() - 86_400_000L
                            selectedDateMs = yesterday
                            customDateStr = dateFmt.format(Date(yesterday))
                        },
                        label = { Text(language.pick("کل", "Yesterday"), style = MaterialTheme.typography.labelSmall) }
                    )
                    OutlinedTextField(
                        value = customDateStr,
                        onValueChange = { raw ->
                            customDateStr = raw
                            runCatching {
                                dateFmt.isLenient = false
                                val parsed = dateFmt.parse(raw.trim())
                                if (parsed != null) selectedDateMs = parsed.time + 43_200_000L
                            }
                        },
                        label = { Text(language.pick("تاریخ", "Date")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (onDelete != null) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = UdharCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("roznamcha_delete_entry_button")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("یہ انٹری ڈیلیٹ کریں", "Delete This Entry"))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        val finalTitle = titleInput.trim().ifBlank {
                            if (isCashIn) language.pick("دستی سیل / آمدن", "Manual Cash In")
                            else language.pick("اخراجات / خریداری", "Manual Cash Out")
                        }
                        onSave(entryType, paymentSource, finalTitle, amt, noteInput.trim(), selectedDateMs)
                    }
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCashIn) ProfitGreen else UdharCrimson
                ),
                modifier = Modifier.testTag("roznamcha_save_entry_button")
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
