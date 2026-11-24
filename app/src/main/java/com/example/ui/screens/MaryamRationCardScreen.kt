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
import androidx.compose.material.icons.filled.Contactless
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

private enum class PosCardMode { MARYAM_RATION, GENERAL_CARD }
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

    var posMode by remember { mutableStateOf(PosCardMode.MARYAM_RATION) }
    var activeTab by remember { mutableStateOf(RationCardTab.BILL) }

    // Beneficiary & Ration Card Form State
    var cardUid by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var beneficiaryName by remember { mutableStateOf("") }
    var cnic by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var subsidyLimitStr by remember { mutableStateOf("3000") }
    var feeDeductionStr by remember { mutableStateOf("100") }
    var note by remember { mutableStateOf("") }
    var scannedViaNfc by remember { mutableStateOf(false) }

    // General Card Payment State (for standard debit/bank NFC card swipe/tap)
    var generalCustomerName by remember { mutableStateOf("") }
    var generalCardDigits by remember { mutableStateOf("") }
    var generalAmountStr by remember { mutableStateOf("") }
    var generalReferenceNote by remember { mutableStateOf("") }

    // POS Bill Cart State
    var cartItems by remember { mutableStateOf<List<RationCartItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var catalogSearchQuery by remember { mutableStateOf("") }
    var historySearch by remember { mutableStateOf("") }

    var editingItem by remember { mutableStateOf<RationCardItem?>(null) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    var editingCartPriceItem by remember { mutableStateOf<RationCartItem?>(null) }
    var showProgramInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(scannedNfcCard?.scannedAtMs) {
        val card = scannedNfcCard ?: return@LaunchedEffect
        cardUid = card.uidHex
        scannedViaNfc = true

        val detectedNum = when {
            card.maskedPan.isNotBlank() -> card.maskedPan
            card.ndefText.isNotBlank() -> card.ndefText.take(60)
            card.suggestedCardNumber.isNotBlank() -> card.suggestedCardNumber.take(60)
            else -> ""
        }
        if (detectedNum.isNotBlank()) {
            cardNumber = detectedNum
            generalCardDigits = detectedNum
        }

        val prevBill = rationBills.firstOrNull {
            it.cardUid.equals(card.uidHex, ignoreCase = true)
        }
        if (prevBill != null) {
            if (beneficiaryName.isBlank()) beneficiaryName = prevBill.beneficiaryName
            if (cnic.isBlank()) cnic = prevBill.cnic
            if (phone.isBlank()) phone = prevBill.phone
        }
        activeTab = RationCardTab.BILL
    }
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

            // 1. TWO DISTINCT POS MODE BOXES (Maryam Nawaz Ration Card vs General Card Payment Scanner)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isRationSelected = posMode == PosCardMode.MARYAM_RATION
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { posMode = PosCardMode.MARYAM_RATION }
                            .testTag("pos_mode_maryam_card"),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            width = if (isRationSelected) 2.dp else 1.dp,
                            color = if (isRationSelected) GoldShimmer else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isRationSelected) EmeraldPrimary else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (isRationSelected) SaffronSecondary else EmeraldPrimary.copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = if (isRationSelected) Color.White else EmeraldPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = if (isRationSelected) GoldShimmer else EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = language.pick("مریم نواز راشن کارڈ", "Maryam Ration Card"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isRationSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = language.pick("3,000 سبسڈی • فیس 100", "Rs. 3,000 Quota • Fee 100"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isRationSelected) GoldShimmer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    val isGeneralSelected = posMode == PosCardMode.GENERAL_CARD
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { posMode = PosCardMode.GENERAL_CARD }
                            .testTag("pos_mode_general_card"),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            width = if (isGeneralSelected) 2.dp else 1.dp,
                            color = if (isGeneralSelected) SaffronSecondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isGeneralSelected) EmeraldPrimaryDark else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = if (isGeneralSelected) GoldShimmer else SaffronSecondary.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Contactless,
                                            contentDescription = null,
                                            tint = if (isGeneralSelected) EmeraldPrimaryDark else SaffronSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = if (isGeneralSelected) GoldShimmer else SaffronSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = language.pick("عام کارڈ پیمنٹ سکینر", "General Card Payment"),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isGeneralSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = language.pick("ڈیبٹ / بینک کارڈ وصولی", "Bank / ATM NFC Payment"),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGeneralSelected) GoldShimmer else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
                        if (posMode == PosCardMode.GENERAL_CARD) {
                // GENERAL BANK / ATM / NFC CARD PAYMENT SCANNER SCREEN
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.2.dp, SaffronSecondary.copy(alpha = 0.6f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(if (scannedViaNfc && cardUid.isNotBlank()) ProfitGreen else SaffronSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Contactless, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = language.pick("عام بینک / ڈیبٹ کارڈ پیمنٹ", "General Bank / Debit Card Payment"),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (cardUid.isNotBlank()) "NFC UID: $cardUid"
                                        else language.pick("کارڈ فون کے پیچھے ٹچ کریں یا تفصیل درج کریں", "Tap customer card on back of phone or enter details"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (cardUid.isNotBlank()) ProfitGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = generalAmountStr,
                                onValueChange = { generalAmountStr = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text(language.pick("وصول کی جانے والی رقم (روپے)*", "Payment Amount (PKR)*")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = generalCardDigits,
                                onValueChange = { generalCardDigits = it },
                                label = { Text(language.pick("کارڈ نمبر / آخری 4 ہندسے", "Card Number / Last 4 Digits")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = generalCustomerName,
                                onValueChange = { generalCustomerName = it },
                                label = { Text(language.pick("کسٹمر کا نام (اختیاری)", "Customer Name (Optional)")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = generalReferenceNote,
                                onValueChange = { generalReferenceNote = it },
                                label = { Text(language.pick("بینک / ٹرانزیکشن تفصیل", "Bank / Transaction Reference")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val amt = generalAmountStr.toDoubleOrNull() ?: 0.0
                                    if (amt > 0.0) {
                                        val payItem = RationCardItem(
                                            id = "general_card_pos",
                                            nameUrdu = "عام کارڈ پیمنٹ (${generalCardDigits.ifBlank { "NFC" }})",
                                            nameEnglish = "Card Payment (${generalCardDigits.ifBlank { "NFC" }})",
                                            unit = "tx",
                                            price = amt,
                                            sortOrder = 0
                                        )
                                        onCheckoutRationBill(
                                            listOf(RationCartItem(item = payItem, quantity = 1.0)),
                                            cardUid,
                                            generalCardDigits.ifBlank { "CARD-POS" },
                                            generalCustomerName.ifBlank { language.pick("کارڈ کسٹمر", "Card Customer") },
                                            "",
                                            "",
                                            0.0,
                                            0.0,
                                            0.0,
                                            scannedViaNfc,
                                            generalReferenceNote.ifBlank { "General Card POS Payment" }
                                        ) {
                                            generalAmountStr = ""
                                            generalCardDigits = ""
                                            generalCustomerName = ""
                                            generalReferenceNote = ""
                                            cardUid = ""
                                            scannedViaNfc = false
                                            onClearScannedNfc()
                                        }
                                    }
                                },
                                enabled = !isBusy && (generalAmountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier.fillMaxWidth().height(52.dp)
                            ) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text(
                                    text = language.pick("کارڈ پیمنٹ محفوظ کریں اور رسید بنائیں", "Complete Card Payment & Print Receipt"),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            } else {
                // MARYAM NAWAZ RATION CARD POS MODE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
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
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = language.pick("مریم نواز راشن کارڈ POS", "Maryam Nawaz Ration Card POS"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = language.pick(
                                                "کوٹہ ${subsidyLimit.toLong()} - فیس ${feeDeduction.toLong()} = خالص راشن ${netRationValue.toLong()} روپے",
                                                "Quota Rs. ${subsidyLimit.toLong()} - Fee Rs. ${feeDeduction.toLong()} = Net Rs. ${netRationValue.toLong()}"
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldShimmer
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = onOpenCharityFund) {
                                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = GoldShimmer)
                                        }
                                        IconButton(onClick = { showProgramInfoDialog = true }) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = GoldShimmer)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Sub-Navigation Tabs
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
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (scannedViaNfc && cardUid.isNotBlank()) ProfitGreen else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                ),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
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
                                                .clip(CircleShape)
                                                .background(if (scannedViaNfc && cardUid.isNotBlank()) ProfitGreen else EmeraldPrimary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (scannedViaNfc && cardUid.isNotBlank()) Icons.Default.CheckCircle else Icons.Default.Nfc,
                                                contentDescription = null,
                                                tint = Color.White
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = if (scannedViaNfc && cardUid.isNotBlank()) language.pick("کارڈ NFC سے اسکین ہو گیا!", "Card Scanned via NFC!")
                                                else language.pick("راشن کارڈ فون کے پیچھے ٹچ کریں یا دستی لکھیں", "Tap Ration Card on phone or enter manually"),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (cardUid.isNotBlank()) {
                                                Text("NFC UID: $cardUid", style = MaterialTheme.typography.labelSmall, color = ProfitGreen)
                                            }
                                        }
                                    }
                                    if (nfcStatus == NfcHardwareStatus.DISABLED) {
                                        FilledTonalButton(onClick = {
                                            runCatching { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) }
                                        }) {
                                            Text(language.pick("NFC آن", "Enable NFC"))
                                        }
                                    } else if (cardUid.isNotBlank()) {
                                        TextButton(onClick = {
                                            cardUid = ""
                                            scannedViaNfc = false
                                            onClearScannedNfc()
                                        }) {
                                            Text(language.pick("صاف کریں", "Clear"))
                                        }
                                    }
                                }
                            }
                        }
                                                item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = beneficiaryName,
                                        onValueChange = { beneficiaryName = it },
                                        label = { Text(language.pick("صارف کا نام*", "Beneficiary Name*")) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("ration_beneficiary_name_input")
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = cardNumber,
                                            onValueChange = { cardNumber = it },
                                            label = { Text(language.pick("کارڈ نمبر / BOP", "Card # / BOP")) },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f).testTag("pos_card_number_input")
                                        )
                                        OutlinedTextField(
                                            value = cnic,
                                            onValueChange = { cnic = it },
                                            label = { Text(language.pick("شناختی کارڈ (CNIC)", "CNIC")) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f).testTag("ration_cnic_input")
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
                                            Text(language.pick("سابقہ ریکارڈ ملا: ${matchedManualBeneficiary.beneficiaryName}", "Found: ${matchedManualBeneficiary.beneficiaryName}"))
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = subsidyLimitStr,
                                            onValueChange = { subsidyLimitStr = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text(language.pick("کل کوٹہ", "Quota")) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f).testTag("pos_quota_input")
                                        )
                                        OutlinedTextField(
                                            value = feeDeductionStr,
                                            onValueChange = { feeDeductionStr = it.filter { c -> c.isDigit() || c == '.' } },
                                            label = { Text(language.pick("سروس فیس", "Fee")) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.weight(1f).testTag("pos_fee_custom_input")
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        QUICK_SERVICE_FEE_PRESETS.forEach { presetFee ->
                                            FilterChip(
                                                selected = feeDeductionStr.isNotBlank() && feeDeduction.toLong() == presetFee.toLong(),
                                                onClick = { feeDeductionStr = presetFee.toString() },
                                                label = { Text(if (presetFee == 0) "Rs. 0" else "Rs. $presetFee", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(language.pick("خالص راشن ویلیو:", "Net Ration Value:"), fontWeight = FontWeight.Bold)
                                        Text(formatPkr(netRationValue, language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = ProfitGreen)
                                    }
                                }
                            }
                        }

                        item {
                            val progress = if (netRationValue > 0.0) ((alreadyUsedThisMonth + subsidyDeducted) / netRationValue).toFloat().coerceIn(0f, 1f) else 0f
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(language.pick("بل ٹوٹل: ${formatPkr(cartTotal, language)}", "Bill Total: ${formatPkr(cartTotal, language)}"), fontWeight = FontWeight.Bold)
                                        Text(language.pick("بقایا: ${formatPkr(remainingAfterBill, language)}", "Left: ${formatPkr(remainingAfterBill, language)}"), color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                                    }
                                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(50.dp)))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilledTonalButton(onClick = { loadStandard3000Pack() }, modifier = Modifier.weight(1f)) {
                                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(4.dp))
                                            Text(language.pick("اسٹینڈرڈ پیک لوڈ کریں", "Load Standard Pack"), maxLines = 1)
                                        }
                                        if (cartItems.isNotEmpty()) {
                                            OutlinedButton(onClick = { cartItems = emptyList() }) { Text(language.pick("خالی", "Clear")) }
                                        }
                                    }
                                }
                            }
                        }

                        if (cartItems.isNotEmpty()) {
                            item {
                                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        cartItems.forEach { ci ->
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(ci.item.nameUrdu.ifBlank { ci.item.nameEnglish }, fontWeight = FontWeight.Bold)
                                                    Text("${formatPkr(ci.effectivePrice, language)} x ${formatQty(ci.quantity)} = ${formatPkr(ci.lineTotal, language)}", style = MaterialTheme.typography.labelSmall)
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(onClick = { editingCartPriceItem = ci }) { Icon(Icons.Default.Edit, contentDescription = null, tint = SaffronSecondary, modifier = Modifier.size(18.dp)) }
                                                    IconButton(onClick = { addOrIncrementCart(ci.item, -1.0) }) { Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                                    Text(formatQty(ci.quantity), fontWeight = FontWeight.Bold)
                                                    IconButton(onClick = { addOrIncrementCart(ci.item, 1.0) }) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                                }
                                            }
                                            HorizontalDivider()
                                        }
                                        Button(
                                            onClick = {
                                                onCheckoutRationBill(
                                                    cartItems, cardUid, cardNumber, beneficiaryName, cnic, phone,
                                                    subsidyLimit, feeDeduction, alreadyUsedThisMonth, scannedViaNfc, note
                                                ) {
                                                    cartItems = emptyList(); beneficiaryName = ""; cnic = ""; phone = ""
                                                    cardNumber = ""; cardUid = ""; note = ""; scannedViaNfc = false; onClearScannedNfc()
                                                }
                                            },
                                            enabled = !isBusy,
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                            modifier = Modifier.fillMaxWidth().height(50.dp)
                                        ) {
                                            Icon(Icons.Default.ReceiptLong, contentDescription = null)
                                            Spacer(Modifier.size(6.dp))
                                            Text(language.pick("راشن کارڈ بل محفوظ کریں", "Save Ration Card Bill"), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                placeholder = { Text(language.pick("راشن تلاش کریں...", "Search ration items...")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        items(filteredItems, key = { it.id }) { item ->
                            val inCartQty = cartItems.firstOrNull { it.item.id == item.id }?.quantity ?: 0.0
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { addOrIncrementCart(item, 1.0) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (inCartQty > 0) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.nameUrdu.ifBlank { item.nameEnglish }, fontWeight = FontWeight.Bold)
                                        Text("${formatPkr(item.price, language)} / ${unitLabel(item.unit, language)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { editingItem = item }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                                            Icon(Icons.Default.PriceChange, contentDescription = null, modifier = Modifier.size(15.dp))
                                        }
                                        if (inCartQty > 0) {
                                            Surface(color = EmeraldPrimary, shape = RoundedCornerShape(50.dp)) {
                                                Text(formatQty(inCartQty), color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                            }
                                        } else {
                                            FilledTonalButton(onClick = { addOrIncrementCart(item, 1.0) }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
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
                            Button(onClick = { showAddItemDialog = true }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.size(6.dp))
                                Text(language.pick("نئی راشن چیز شامل کریں", "Add New Ration Item"))
                            }
                        }
                        items(filteredCatalogItems, key = { it.id }) { item ->
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.nameUrdu.ifBlank { item.nameEnglish }, fontWeight = FontWeight.Bold)
                                        Text("${formatPkr(item.price, language)} / ${unitLabel(item.unit, language)}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Row {
                                        IconButton(onClick = { editingItem = item }) { Icon(Icons.Default.PriceChange, contentDescription = null) }
                                        IconButton(onClick = { onDeleteRationItem(item.id) }) { Icon(Icons.Default.Delete, contentDescription = null, tint = UdharCrimson) }
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
                                placeholder = { Text(language.pick("نام یا کارڈ سے تلاش کریں...", "Search history...")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        items(filteredHistory, key = { it.id }) { bill ->
                            Card(modifier = Modifier.fillMaxWidth().clickable { onOpenReceipt(bill) }, shape = RoundedCornerShape(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(bill.beneficiaryName, fontWeight = FontWeight.Bold)
                                        Text("${bill.billNumber} • ${ReceiptFormatter.formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Text(formatPkr(bill.totalAmount, language), fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
            onDismiss = { showAddItemDialog = false; editingItem = null }
        )
    }

    val priceTarget = editingCartPriceItem
    if (priceTarget != null) {
        var newRateStr by remember(priceTarget) { mutableStateOf(priceTarget.effectivePrice.toLong().toString()) }
        AlertDialog(
            onDismissRequest = { editingCartPriceItem = null },
            title = { Text(language.pick("قیمت تبدیل کریں", "Edit Price")) },
            text = {
                OutlinedTextField(
                    value = newRateStr,
                    onValueChange = { newRateStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(language.pick("نئی قیمت (روپے)", "New Price (PKR)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    newRateStr.toDoubleOrNull()?.let { parsed ->
                        cartItems = cartItems.map { if (it.item.id == priceTarget.item.id) it.copy(customUnitPrice = parsed) else it }
                    }
                    editingCartPriceItem = null
                }) { Text(language.pick("لاگو کریں", "Apply")) }
            },
            dismissButton = { TextButton(onClick = { editingCartPriceItem = null }) { Text(language.pick("منسوخ", "Cancel")) } }
        )
    }

    if (showProgramInfoDialog) {
        AlertDialog(
            onDismissRequest = { showProgramInfoDialog = false },
            title = { Text(language.pick("POS کارڈ معلومات", "POS Card Info")) },
            text = { Text(language.pick("مریم نواز راشن کارڈ اور عام بینک/NFC کارڈ پیمنٹ کے لیے ہیلپ لائن: 1105", "Help line for Ration & General Card POS: 1105")) },
            confirmButton = { Button(onClick = { showProgramInfoDialog = false }) { Text(language.pick("ٹھیک ہے", "OK")) } }
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
        title = { Text(if (existing == null) language.pick("نئی چیز شامل کریں", "Add Item") else language.pick("ریٹ ایڈیٹ کریں", "Edit Rate")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = nameUrdu, onValueChange = { nameUrdu = it }, label = { Text("اردو نام*") }, singleLine = true)
                OutlinedTextField(value = nameEnglish, onValueChange = { nameEnglish = it }, label = { Text("انگلش نام") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = priceStr, onValueChange = { priceStr = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("قیمت*") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("یونٹ") }, singleLine = true, modifier = Modifier.weight(0.9f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = priceStr.toDoubleOrNull() ?: 0.0
                    if (nameUrdu.isNotBlank()) onSave(existing?.id, nameUrdu, nameEnglish, unit, parsed, existing?.sortOrder ?: nextSortOrder)
                },
                enabled = nameUrdu.isNotBlank() && (priceStr.toDoubleOrNull() != null)
            ) { Text(language.pick("محفوظ کریں", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(language.pick("منسوخ", "Cancel")) } }
    )
}
