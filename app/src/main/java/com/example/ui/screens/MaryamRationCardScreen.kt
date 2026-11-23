package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.VolunteerActivism
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.data.MaryamRationCatalogData
import com.example.data.Product
import com.example.data.RationCardBill
import com.example.data.RationCardItem
import com.example.data.RationCartItem
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.services.NfcHardwareStatus
import com.example.services.NfcRationCardReader
import com.example.services.ReceiptFormatter
import com.example.services.ScannedNfcCard
import com.example.ui.components.PosLowStockWarningBanner
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class RationCardTab { BILL, ITEMS, HISTORY }

private val QUICK_SERVICE_FEE_PRESETS = listOf(0, 50, 70, 80, 100)

@Composable
fun MaryamRationCardScreen(
    language: AppLanguage,
    rationItems: List<RationCardItem>,
    rationBills: List<RationCardBill>,
    scannedNfcCard: ScannedNfcCard?,
    isBusy: Boolean,
    onClearScannedNfc: () -> Unit,
    onSaveRationItem: (String?, String, String, String, Double, Int) -> Unit,
    onDeleteRationItem: (String) -> Unit,
    onCheckoutRationBill: (
        cartItems: List<RationCartItem>,
        cardUid: String,
        cardNumber: String,
        beneficiaryName: String,
        cnic: String,
        phone: String,
        subsidyLimit: Double,
        feeDeduction: Double,
        alreadyUsedThisMonth: Double,
        scannedViaNfc: Boolean,
        note: String,
        onSuccessClear: () -> Unit
    ) -> Unit,
    onOpenReceipt: (RationCardBill) -> Unit,
    onOpenCharityFund: () -> Unit,
    lowStockProducts: List<Product> = emptyList(),
    onOpenLowStockReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val nfcStatus = remember(context) {
        try {
            NfcRationCardReader.checkHardwareStatus(context)
        } catch (_: Throwable) {
            NfcHardwareStatus.NOT_SUPPORTED
        }
    }
    val currentMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }

    var activeTab by remember { mutableStateOf(RationCardTab.BILL) }

    // Beneficiary & POS/NFC Card Form State
    var cardUid by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var beneficiaryName by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var subsidyLimitStr by remember { mutableStateOf("3000") }
    var feeDeductionStr by remember { mutableStateOf("100") }
    var note by remember { mutableStateOf("") }
    var scannedViaNfc by remember { mutableStateOf(false) }

    // POS Bill Cart State
    var cartItems by remember { mutableStateOf<List<RationCartItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var catalogSearchQuery by remember { mutableStateOf("") }
    var historySearch by remember { mutableStateOf("") }

    // Item Editor Dialogs
    var editingItem by remember { mutableStateOf<RationCardItem?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingCartPriceItem by remember { mutableStateOf<RationCartItem?>(null) }
    var showProgramInfoDialog by remember { mutableStateOf(false) }

    // Automatically populate fields when a real POS / Ration Card is tapped via NFC!
    LaunchedEffect(scannedNfcCard?.scannedAtMs) {
        val card = scannedNfcCard ?: return@LaunchedEffect
        cardUid = card.uidHex
        scannedViaNfc = true
        if (card.ndefText.isNotBlank() && cardNumber.isBlank()) {
            cardNumber = card.ndefText.take(60)
        } else if (card.suggestedCardNumber.isNotBlank() && cardNumber.isBlank()) {
            cardNumber = card.suggestedCardNumber.take(60)
        }
        // Auto-fill returning beneficiary details if this NFC UID was previously served
        val prevBill = rationBills.firstOrNull {
            it.cardUid.equals(card.uidHex, ignoreCase = true)
        }
        if (prevBill != null) {
            if (beneficiaryName.isBlank()) beneficiaryName = prevBill.beneficiaryName
            if (cnic.isBlank()) cnic = prevBill.cnic
            if (phone.isBlank()) phone = prevBill.phone
            if (cardNumber.isBlank()) cardNumber = prevBill.cardNumber
        }
        activeTab = RationCardTab.BILL
    }

    // Track how much of this card's net monthly allowance was already used this month
    val alreadyUsedThisMonth = remember(cardUid, cnic, cardNumber, rationBills, currentMonthKey) {
        val keyUid = cardUid.trim()
        val keyCnic = cnic.trim()
        val keyCard = cardNumber.trim()
        if (keyUid.isBlank() && keyCnic.isBlank() && keyCard.isBlank()) {
            0.0
        } else {
            rationBills.filter { bill ->
                bill.monthKey == currentMonthKey && (
                    (keyUid.isNotBlank() && bill.cardUid.equals(keyUid, ignoreCase = true)) ||
                        (keyCnic.isNotBlank() && bill.cnic.equals(keyCnic, ignoreCase = true)) ||
                        (keyCard.isNotBlank() && bill.cardNumber.equals(keyCard, ignoreCase = true))
                    )
            }.sumOf { it.subsidyUsed }
        }
    }

    // Manual-mode helper: detect returning beneficiary by typed CNIC or Card #
    val matchedManualBeneficiary = remember(cnic, cardNumber, beneficiaryName, rationBills) {
        val keyCnic = cnic.trim()
        val keyCard = cardNumber.trim()
        if (beneficiaryName.isNotBlank() || (keyCnic.length < 5 && keyCard.length < 4)) {
            null
        } else {
            rationBills.firstOrNull { bill ->
                (keyCnic.length >= 5 && bill.cnic.equals(keyCnic, ignoreCase = true)) ||
                    (keyCard.length >= 4 && bill.cardNumber.equals(keyCard, ignoreCase = true))
            }
        }
    }

    val subsidyLimit = (subsidyLimitStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 1_000_000.0)
    val feeDeduction = (feeDeductionStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, subsidyLimit)
    val netRationValue = (subsidyLimit - feeDeduction).coerceAtLeast(0.0)
    val availableAllowance = (netRationValue - alreadyUsedThisMonth).coerceAtLeast(0.0)
    val cartTotal = cartItems.sumOf { it.lineTotal }
    val subsidyDeducted = cartTotal.coerceAtMost(availableAllowance)
    val extraCashNeeded = (cartTotal - subsidyDeducted).coerceAtLeast(0.0)
    val remainingAfterBill = (availableAllowance - subsidyDeducted).coerceAtLeast(0.0)

    val filteredItems = remember(rationItems, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isBlank()) rationItems
        else rationItems.filter {
            it.nameUrdu.lowercase().contains(q) || it.nameEnglish.lowercase().contains(q)
        }
    }

    val filteredCatalogItems = remember(rationItems, catalogSearchQuery) {
        val q = catalogSearchQuery.trim().lowercase()
        if (q.isBlank()) rationItems
        else rationItems.filter {
            it.nameUrdu.lowercase().contains(q) || it.nameEnglish.lowercase().contains(q)
        }
    }

    val filteredHistory = remember(rationBills, historySearch) {
        val q = historySearch.trim().lowercase()
        if (q.isBlank()) rationBills
        else rationBills.filter {
            it.beneficiaryName.lowercase().contains(q) ||
                it.cnic.lowercase().contains(q) ||
                it.cardNumber.lowercase().contains(q) ||
                it.cardUid.lowercase().contains(q) ||
                it.billNumber.lowercase().contains(q)
        }
    }

    fun addOrIncrementCart(item: RationCardItem, delta: Double = 1.0) {
        val existingIdx = cartItems.indexOfFirst { it.item.id == item.id }
        cartItems = if (existingIdx >= 0) {
            val mut = cartItems.toMutableList()
            val cur = mut[existingIdx]
            val nextQty = cur.quantity + delta
            if (nextQty <= 0.0) mut.removeAt(existingIdx)
            else mut[existingIdx] = cur.copy(quantity = nextQty)
            mut
        } else if (delta > 0.0) {
            cartItems + RationCartItem(item = item, quantity = delta)
        } else {
            cartItems
        }
    }

    fun loadStandard3000Pack() {
        val preset = MaryamRationCatalogData.standard3000PackQuantities
        val byId = rationItems.associateBy { it.id }
        val built = preset.mapNotNull { (docId, qty) ->
            byId[docId]?.let { RationCartItem(item = it, quantity = qty) }
        }
        cartItems = if (built.isNotEmpty()) built else rationItems.take(5).map { RationCartItem(it, 1.0) }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (lowStockProducts.isNotEmpty()) {
                item {
                    PosLowStockWarningBanner(
                        language = language,
                        lowStockProducts = lowStockProducts,
                        onOpenLowStockReport = onOpenLowStockReport,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 1. Royal Punjab Government Maryam Nawaz Ration Card Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, GoldShimmer.copy(alpha = 0.7f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(EmeraldPrimaryDark, EmeraldPrimary, Color(0xFF0E6E4E))
                                )
                            )
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = SaffronSecondary,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = language.pick(
                                                "POS / پی او ایس کارڈ (NFC)",
                                                "POS / پی او ایس Card (NFC)"
                                            ),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = language.pick(
                                                "کوٹہ ${subsidyLimit.toLong()} - فیس ${feeDeduction.toLong()} = خالص راشن ${netRationValue.toLong()} روپے • ٹانڈہ",
                                                "Quota Rs. ${subsidyLimit.toLong()} - Fee Rs. ${feeDeduction.toLong()} = Net Rs. ${netRationValue.toLong()} • Tanda"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldShimmer
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showProgramInfoDialog = true },
                                    modifier = Modifier.testTag("maryam_ration_info_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = language.pick("POS معلومات", "POS Info"),
                                        tint = GoldShimmer
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text(
                                            text = language.pick("کل POS بلز", "POS Bills"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = "${rationBills.size}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text(
                                            text = language.pick("کل POS راشن", "Total POS Given"),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = formatPkr(rationBills.sumOf { it.subsidyUsed }, language),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldShimmer
                                        )
                                    }
                                }
                                Surface(
                                    color = SaffronSecondary,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onOpenCharityFund)
                                        .testTag("open_hbl_charity_from_ration")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolunteerActivism,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = language.pick("امداد فنڈ", "Donate"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Sub-Navigation Tabs
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val tabs = listOf(
                        RationCardTab.BILL to language.pick("نیا POS بل", "New POS Bill"),
                        RationCardTab.ITEMS to language.pick("اشیاء و ریٹس", "Items & Rates"),
                        RationCardTab.HISTORY to language.pick("ریکارڈ (${rationBills.size})", "History (${rationBills.size})")
                    )
                    tabs.forEachIndexed { idx, (tab, title) ->
                        SegmentedButton(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                            modifier = Modifier.testTag("ration_tab_${tab.name.lowercase()}")
                        ) {
                            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            when (activeTab) {
                RationCardTab.BILL -> {
                    // 3A. NFC Tap Card Scanner / Graceful Manual Mode Banner
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(
                                1.5.dp,
                                when {
                                    scannedViaNfc && cardUid.isNotBlank() -> ProfitGreen
                                    nfcStatus == NfcHardwareStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                }
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    scannedViaNfc && cardUid.isNotBlank() -> ProfitGreen.copy(alpha = 0.08f)
                                    nfcStatus == NfcHardwareStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)
                                    else -> MaterialTheme.colorScheme.surface
                                }
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        scannedViaNfc && cardUid.isNotBlank() -> ProfitGreen
                                                        nfcStatus == NfcHardwareStatus.READY -> EmeraldPrimary
                                                        nfcStatus == NfcHardwareStatus.DISABLED -> WarningAmber
                                                        else -> MaterialTheme.colorScheme.secondary
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when {
                                                    scannedViaNfc && cardUid.isNotBlank() -> Icons.Default.CheckCircle
                                                    nfcStatus == NfcHardwareStatus.NOT_SUPPORTED -> Icons.Default.CreditCard
                                                    else -> Icons.Default.Nfc
                                                },
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = when {
                                                    scannedViaNfc && cardUid.isNotBlank() -> language.pick(
                                                        "POS کارڈ NFC سے اسکین ہو گیا!",
                                                        "POS Card Scanned via NFC!"
                                                    )
                                                    nfcStatus == NfcHardwareStatus.READY -> language.pick(
                                                        "NFC تیار ہے — POS کارڈ فون کے پیچھے ٹچ کریں",
                                                        "NFC Ready — Tap POS Card on back of phone"
                                                    )
                                                    nfcStatus == NfcHardwareStatus.DISABLED -> language.pick(
                                                        "فون کا NFC بند ہے — آن کریں یا دستی اندراج کریں",
                                                        "Phone NFC is OFF — Enable or use manual entry below"
                                                    )
                                                    else -> language.pick(
                                                        "دستی POS کارڈ موڈ فعال (بغیر NFC)",
                                                        "Manual POS Card Mode Active (No NFC Required)"
                                                    )
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (scannedViaNfc && cardUid.isNotBlank()) ProfitGreen else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (cardUid.isNotBlank()) {
                                                Text(
                                                    text = "NFC UID: $cardUid",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = ProfitGreen,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            } else if (nfcStatus == NfcHardwareStatus.NOT_SUPPORTED) {
                                                Text(
                                                    text = language.pick(
                                                        "اس ڈیوائس میں NFC نہیں — کارڈ نمبر، شناختی کارڈ یا نام نیچے دستی درج کر کے مکمل POS بل اور رسید بنائیں۔",
                                                        "NFC hardware unavailable on this device — type Card #, CNIC, or Name below for full POS billing & receipts."
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (nfcStatus == NfcHardwareStatus.DISABLED) {
                                        FilledTonalButton(
                                            onClick = {
                                                try {
                                                    context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                                                } catch (_: Throwable) {
                                                    // Ignore gracefully if NFC settings screen is unavailable
                                                }
                                            }
                                        ) {
                                            Text(language.pick("NFC آن کریں", "Enable NFC"))
                                        }
                                    } else if (cardUid.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                cardUid = ""
                                                scannedViaNfc = false
                                                onClearScannedNfc()
                                            }
                                        ) {
                                            Text(language.pick("صاف کریں", "Clear"))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3B. Beneficiary, Manual Card Entry & Live Quota / Fee Deduction Form
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = language.pick("POS / پی او ایس کارڈ اور صارف کی تفصیل", "POS Card & Beneficiary Details"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                OutlinedTextField(
                                    value = beneficiaryName,
                                    onValueChange = { beneficiaryName = it },
                                    label = { Text(language.pick("صارف کا نام*", "Beneficiary Name*")) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("ration_beneficiary_name_input")
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = cardNumber,
                                        onValueChange = { cardNumber = it },
                                        label = { Text(language.pick("POS کارڈ نمبر / BOP", "POS Card # / BOP")) },
                                        placeholder = { Text("BOP-XXXX") },
                                        singleLine = true,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pos_card_number_input")
                                    )
                                    OutlinedTextField(
                                        value = cnic,
                                        onValueChange = { cnic = it },
                                        label = { Text(language.pick("شناختی کارڈ (CNIC)", "CNIC Number")) },
                                        placeholder = { Text("34201-XXXXXXX-X") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("ration_cnic_input")
                                    )
                                }
                                OutlinedTextField(
                                    value = phone,
                                    onValueChange = { phone = it },
                                    label = { Text(language.pick("موبائل نمبر", "Phone Number")) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (matchedManualBeneficiary != null) {
                                    FilledTonalButton(
                                        onClick = {
                                            beneficiaryName = matchedManualBeneficiary.beneficiaryName
                                            if (cnic.isBlank()) cnic = matchedManualBeneficiary.cnic
                                            if (phone.isBlank()) phone = matchedManualBeneficiary.phone
                                            if (cardNumber.isBlank()) cardNumber = matchedManualBeneficiary.cardNumber
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            language.pick(
                                                "سابقہ ریکارڈ ملا: ${matchedManualBeneficiary.beneficiaryName} (خودکار بھریں)",
                                                "Returning Card Found: ${matchedManualBeneficiary.beneficiaryName} (Auto-Fill)"
                                            )
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                                // Live Editable Total Quota & Service Fee Deduction Controls
                                Text(
                                    text = language.pick(
                                        "کل کوٹہ اور سروس فیس کٹوتی (فوری حساب)",
                                        "Total Quota & Service Fee Deduction (Live Calculation)"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = subsidyLimitStr,
                                        onValueChange = { raw ->
                                            subsidyLimitStr = raw.filter { c -> c.isDigit() || c == '.' }
                                        },
                                        label = { Text(language.pick("کل کوٹہ (روپے)", "Total Quota (Rs.)")) },
                                        placeholder = { Text("3000") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pos_quota_input")
                                    )
                                    OutlinedTextField(
                                        value = feeDeductionStr,
                                        onValueChange = { raw ->
                                            feeDeductionStr = raw.filter { c -> c.isDigit() || c == '.' }
                                        },
                                        label = { Text(language.pick("سروس فیس (روپے)", "Service Fee (Rs.)")) },
                                        placeholder = { Text("100") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("pos_fee_custom_input")
                                    )
                                }

                                // Quick Service Fee Chips: 0, 50, 70, 80, 100
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    QUICK_SERVICE_FEE_PRESETS.forEach { presetFee ->
                                        val selected = feeDeductionStr.isNotBlank() &&
                                            feeDeduction.toLong() == presetFee.toLong()
                                        FilterChip(
                                            selected = selected,
                                            onClick = { feeDeductionStr = presetFee.toString() },
                                            label = {
                                                Text(
                                                    text = if (presetFee == 0) {
                                                        language.pick("0 (فری)", "Rs. 0")
                                                    } else {
                                                        language.pick("فیس $presetFee", "Rs. $presetFee")
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            },
                                            modifier = Modifier.testTag("pos_fee_chip_$presetFee")
                                        )
                                    }
                                }

                                // Live Net Ration Value Breakdown Box
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = language.pick("خالص راشن ویلیو (کوٹہ − فیس)", "Net Ration Value (Quota − Fee)"),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = "${formatPkr(subsidyLimit, language)} − ${formatPkr(feeDeduction, language)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = formatPkr(netRationValue, language),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = ProfitGreen,
                                            modifier = Modifier.testTag("pos_net_ration_value")
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3C. Live POS Net Ration Quota Meter & Cart Summary
                    item {
                        val progress = if (netRationValue > 0.0) {
                            ((alreadyUsedThisMonth + subsidyDeducted) / netRationValue).toFloat().coerceIn(0f, 1f)
                        } else 0f
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
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
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = language.pick(
                                                "POS / پی او ایس خالص راشن میٹر ($currentMonthKey)",
                                                "POS Net Ration Meter ($currentMonthKey)"
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = language.pick(
                                                "کل کوٹہ: ${subsidyLimit.toLong()} • فیس: -${feeDeduction.toLong()} • خالص: ${netRationValue.toLong()}",
                                                "Quota: ${subsidyLimit.toLong()} • Fee: -${feeDeduction.toLong()} • Net: ${netRationValue.toLong()}"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = formatPkr(cartTotal, language),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progress },
                                    color = if (extraCashNeeded > 0) SaffronSecondary else EmeraldPrimary,
                                    trackColor = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50.dp))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = language.pick("POS کارڈ سے کٹوتی", "From POS Card"),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = formatPkr(subsidyDeducted, language),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = language.pick("خالص بقایا بیلنس", "Net Remaining"),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = formatPkr(remainingAfterBill, language),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = EmeraldPrimary
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = language.pick("اضافی نقد", "Extra Cash"),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Text(
                                            text = formatPkr(extraCashNeeded, language),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (extraCashNeeded > 0) UdharCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (alreadyUsedThisMonth > 0.0) {
                                    Text(
                                        text = language.pick(
                                            "نوٹ: اس کارڈ سے رواں ماہ پہلے ہی ${formatPkr(alreadyUsedThisMonth, AppLanguage.URDU)} استعمال ہو چکے ہیں۔",
                                            "Note: ${formatPkr(alreadyUsedThisMonth, AppLanguage.ENGLISH)} already used on this card this month."
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarningAmber
                                    )
                                }

                                // Quick Standard Pack + Clear Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { loadStandard3000Pack() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("load_3000_pack_button")
                                    ) {
                                        Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            language.pick("اسٹینڈرڈ POS پیک لوڈ کریں", "Load Standard POS Pack"),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (cartItems.isNotEmpty()) {
                                        OutlinedButton(onClick = { cartItems = emptyList() }) {
                                            Text(language.pick("خالی کریں", "Clear"))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3D. Selected Cart Items (with inline manual price edit)
                    if (cartItems.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = language.pick(
                                            "منتخب راشن اشیاء (${cartItems.size}) — قیمت تبدیل کرنے کے لیے پینسل دبائیں",
                                            "Selected Ration Items (${cartItems.size}) — Tap pencil to override price"
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    cartItems.forEach { ci ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = ci.item.nameUrdu.ifBlank { ci.item.nameEnglish },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                if (ci.item.nameEnglish.isNotBlank()) {
                                                    Text(
                                                        text = ci.item.nameEnglish,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "${formatPkr(ci.effectivePrice, language)} / ${unitLabel(ci.item.unit, language)} = ${formatPkr(ci.lineTotal, language)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(onClick = { editingCartPriceItem = ci }) {
                                                    Icon(
                                                        Icons.Default.Edit,
                                                        contentDescription = language.pick("قیمت تبدیل کریں", "Edit price"),
                                                        tint = SaffronSecondary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(onClick = { addOrIncrementCart(ci.item, -1.0) }) {
                                                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp))
                                                }
                                                Text(
                                                    text = formatQty(ci.quantity),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                IconButton(onClick = { addOrIncrementCart(ci.item, 1.0) }) {
                                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                    }

                                    OutlinedTextField(
                                        value = note,
                                        onValueChange = { note = it },
                                        label = { Text(language.pick("نوٹ (اختیاری)", "Note (optional)")) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Button(
                                        onClick = {
                                            onCheckoutRationBill(
                                                cartItems,
                                                cardUid,
                                                cardNumber,
                                                beneficiaryName,
                                                cnic,
                                                phone,
                                                subsidyLimit,
                                                feeDeduction,
                                                alreadyUsedThisMonth,
                                                scannedViaNfc,
                                                note
                                            ) {
                                                cartItems = emptyList()
                                                beneficiaryName = ""
                                                cnic = ""
                                                phone = ""
                                                cardNumber = ""
                                                cardUid = ""
                                                note = ""
                                                scannedViaNfc = false
                                                onClearScannedNfc()
                                            }
                                        },
                                        enabled = !isBusy && cartItems.isNotEmpty(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp)
                                            .testTag("save_ration_card_bill_button")
                                    ) {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                        Spacer(Modifier.size(8.dp))
                                        Text(
                                            text = language.pick(
                                                "POS / پی او ایس بل محفوظ کریں اور رسید بنائیں",
                                                "Save POS Bill & Print Receipt"
                                            ),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3E. Quick-Tap Eligible Ration Commodities List
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = {
                                Text(
                                    language.pick(
                                        "اردو یا انگلش میں راشن تلاش کریں (آٹا، چینی، گھی)...",
                                        "Search Urdu or English (Atta, Sugar, Ghee)..."
                                    )
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    items(filteredItems, key = { it.id }) { item ->
                        val inCartQty = cartItems.firstOrNull { it.item.id == item.id }?.quantity ?: 0.0
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { addOrIncrementCart(item, 1.0) },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (inCartQty > 0) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            ),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    // Prominent Urdu name always primary
                                    Text(
                                        text = item.nameUrdu.ifBlank { item.nameEnglish },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (item.nameEnglish.isNotBlank()) {
                                        Text(
                                            text = item.nameEnglish,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${formatPkr(item.price, language)} / ${unitLabel(item.unit, language)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { editingItem = item },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("ration_quick_edit_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.PriceChange,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(Modifier.size(3.dp))
                                        Text(
                                            text = language.pick("ریٹ تبدیل", "Edit Price"),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (inCartQty > 0) {
                                        Surface(
                                            color = EmeraldPrimary,
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Text(
                                                text = formatQty(inCartQty),
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        FilledTonalButton(
                                            onClick = { addOrIncrementCart(item, 1.0) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Text(language.pick("شامل", "Add"))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                RationCardTab.ITEMS -> {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showAddItemDialog = true },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_maryam_ration_item_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(language.pick("نئی راشن چیز شامل کریں", "Add New Ration Item"))
                            }
                            OutlinedTextField(
                                value = catalogSearchQuery,
                                onValueChange = { catalogSearchQuery = it },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                placeholder = {
                                    Text(
                                        language.pick(
                                            "اردو یا انگلش میں راشن تلاش کریں...",
                                            "Search ration items in Urdu or English..."
                                        )
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ration_catalog_search_input")
                            )
                        }
                    }
                    items(filteredCatalogItems, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = item.nameUrdu.ifBlank { item.nameEnglish },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${item.nameEnglish} • ${unitLabel(item.unit, language)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = formatPkr(item.price, language),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = { editingItem = item },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("ration_catalog_edit_${item.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.PriceChange,
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
                                    IconButton(onClick = { onDeleteRationItem(item.id) }) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = language.pick("حذف", "Delete"),
                                            tint = UdharCrimson
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                RationCardTab.HISTORY -> {
                    item {
                        OutlinedTextField(
                            value = historySearch,
                            onValueChange = { historySearch = it },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            placeholder = { Text(language.pick("نام، شناختی کارڈ یا NFC UID سے تلاش کریں...", "Search by Name, CNIC or NFC UID...")) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (filteredHistory.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
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
                                        Icons.Default.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = language.pick(
                                            "ابھی تک کوئی POS / پی او ایس کارڈ بل درج نہیں ہوا۔",
                                            "No POS / پی او ایس card bills recorded yet."
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredHistory, key = { it.id }) { bill ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenReceipt(bill) },
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
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = bill.beneficiaryName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (bill.scannedViaNfc) {
                                                Surface(
                                                    color = ProfitGreen.copy(alpha = 0.14f),
                                                    shape = RoundedCornerShape(50.dp)
                                                ) {
                                                    Text(
                                                        text = "NFC",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = ProfitGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = buildString {
                                                append(bill.billNumber)
                                                if (bill.cnic.isNotBlank()) append(" • ${bill.cnic}")
                                                append(" • ${bill.monthKey}")
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = language.pick(
                                                "کوٹہ ${bill.subsidyLimit.toLong()} − فیس ${bill.feeDeduction.toLong()} = خالص ${bill.netSubsidyLimit.toLong()}",
                                                "Quota ${bill.subsidyLimit.toLong()} − Fee ${bill.feeDeduction.toLong()} = Net ${bill.netSubsidyLimit.toLong()}"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = ReceiptFormatter.formatDateTime(
                                                bill.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatPkr(bill.totalAmount, language),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = language.pick(
                                                "POS کارڈ: ${bill.subsidyUsed.toLong()}",
                                                "POS Card: ${bill.subsidyUsed.toLong()}"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ProfitGreen
                                        )
                                        if (bill.cashTopUp > 0) {
                                            Text(
                                                text = language.pick(
                                                    "نقد: ${bill.cashTopUp.toLong()}",
                                                    "Cash: ${bill.cashTopUp.toLong()}"
                                                ),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SaffronSecondary
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
    }

    // Add / Edit Ration Card Item Dialog
    if (showAddItemDialog || editingItem != null) {
        RationItemEditorDialog(
            language = language,
            existing = editingItem,
            nextSortOrder = rationItems.size + 1,
            onSave = { id, ur, en, unit, price, order ->
                onSaveRationItem(id, ur, en, unit, price, order)
                showAddItemDialog = false
                editingItem = null
            },
            onDismiss = {
                showAddItemDialog = false
                editingItem = null
            }
        )
    }

    // Inline Cart Line Price Override Dialog
    val priceOverrideTarget = editingCartPriceItem
    if (priceOverrideTarget != null) {
        var newRateStr by remember(priceOverrideTarget) {
            mutableStateOf(priceOverrideTarget.effectivePrice.toLong().toString())
        }
        AlertDialog(
            onDismissRequest = { editingCartPriceItem = null },
            title = { Text(language.pick("آئٹم کی قیمت تبدیل کریں", "Edit Item Price for Bill")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (language == AppLanguage.URDU) priceOverrideTarget.item.nameUrdu else priceOverrideTarget.item.nameEnglish,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = newRateStr,
                        onValueChange = { newRateStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(language.pick("نئی قیمت (روپے)", "New Price (PKR)")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = newRateStr.toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            cartItems = cartItems.map {
                                if (it.item.id == priceOverrideTarget.item.id) it.copy(customUnitPrice = parsed) else it
                            }
                        }
                        editingCartPriceItem = null
                    }
                ) {
                    Text(language.pick("لاگو کریں", "Apply"))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCartPriceItem = null }) {
                    Text(language.pick("منسوخ", "Cancel"))
                }
            }
        )
    }

    // Program Info Dialog (POS / پی او ایس Card)
    if (showProgramInfoDialog) {
        AlertDialog(
            onDismissRequest = { showProgramInfoDialog = false },
            title = { Text(language.pick("POS / پی او ایس کارڈ معلومات", "POS / پی او ایس Card Info")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = language.pick(
                            "• POS / پی او ایس کارڈ کے تحت کل کوٹہ (ڈیفالٹ 3,000 روپے) اور سروس فیس کٹوتی (ڈیفالٹ 100 روپے) کو آپ فوری تبدیل کر سکتے ہیں — خالص راشن (کوٹہ − فیس) خودکار شمار ہو گا۔\n" +
                                "• اہل بنیادی اشیاء: آٹا، بناسپتی گھی، کوکنگ آئل، چینی، دالیں، چاول، چائے، بیسن، نمک اور مصالحہ جات۔\n" +
                                "• NFC والے فون پر کارڈ ٹچ کر کے فوری اسکین کریں، اور بغیر NFC والے فون پر کارڈ نمبر / شناختی کارڈ دستی درج کر کے مکمل POS بل اور رسید بنائیں۔\n" +
                                "• سرکاری ہیلپ لائن: 1105",
                            "• Under POS / پی او ایس Card billing, Total Quota (default Rs. 3,000) and Service Fee Deduction (default Rs. 100) are editable in real time — Net Ration Value (Quota − Fee) updates automatically for totals & receipts.\n" +
                                "• Core eligible items: Atta (Flour), Ghee/Cooking Oil, Sugar, Pulses (Daal), Rice, Tea, Besan, Salt & Spices.\n" +
                                "• Tap smart cards on NFC-enabled phones, or enter Card # / CNIC manually on non-NFC devices without any errors.\n" +
                                "• Official Helpline: 1105"
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showProgramInfoDialog = false }) {
                    Text(language.pick("سمجھ گیا", "Got it"))
                }
            }
        )
    }
}

@Composable
private fun RationItemEditorDialog(
    language: AppLanguage,
    existing: RationCardItem?,
    nextSortOrder: Int,
    onSave: (String?, String, String, String, Double, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var nameUrdu by remember(existing) { mutableStateOf(existing?.nameUrdu ?: "") }
    var nameEnglish by remember(existing) { mutableStateOf(existing?.nameEnglish ?: "") }
    var unit by remember(existing) { mutableStateOf(existing?.unit ?: "kg") }
    var priceStr by remember(existing) { mutableStateOf(existing?.price?.toLong()?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existing == null) language.pick("نئی راشن چیز شامل کریں", "Add Ration Item")
                else language.pick("راشن چیز اور قیمت ایڈیٹ کریں", "Edit Ration Item & Price")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nameUrdu,
                    onValueChange = { nameUrdu = it },
                    label = { Text("اردو نام (Urdu Name)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameEnglish,
                    onValueChange = { nameEnglish = it },
                    label = { Text("انگلش نام (English Name)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(language.pick("قیمت (Rs.)*", "Price (Rs.)*")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text(language.pick("یونٹ", "Unit")) },
                        placeholder = { Text("kg / bag / pack") },
                        singleLine = true,
                        modifier = Modifier.weight(0.9f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = priceStr.toDoubleOrNull() ?: 0.0
                    if (nameUrdu.isNotBlank()) {
                        onSave(
                            existing?.id,
                            nameUrdu,
                            nameEnglish,
                            unit,
                            parsedPrice,
                            existing?.sortOrder ?: nextSortOrder
                        )
                    }
                },
                enabled = nameUrdu.isNotBlank() && (priceStr.toDoubleOrNull() != null)
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
