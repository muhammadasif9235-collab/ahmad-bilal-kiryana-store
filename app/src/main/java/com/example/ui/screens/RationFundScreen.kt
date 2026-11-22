package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DonationEntryType
import com.example.data.RationDonation
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.pick
import com.example.services.ReceiptFormatter
import com.example.services.ReceiptPrintHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object RationBankDetails {
    const val BANK_NAME = "HBL (Habib Bank Limited)"
    const val ACCOUNT_TITLE = "Muhammad Asif"
    const val ACCOUNT_NUMBER = "16857900668003"
    const val IBAN = "PK08HABB0016857900668003"
    const val BRANCH_NAME = "MOTA, (TANDA)"
    const val QR_PAYLOAD = "IBAN:PK08HABB0016857900668003|ACC:16857900668003|NAME:Muhammad Asif|BRANCH:MOTA (TANDA)"
}

@Composable
fun RationFundScreen(
    language: AppLanguage,
    donations: List<RationDonation>,
    onRecordEntry: (DonationEntryType, String, String, Double, String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var activeDialogType by remember { mutableStateOf<DonationEntryType?>(null) }

    val totalDonated = remember(donations) {
        donations.filter { it.entryTypeEnum() == DonationEntryType.DONATION_IN }.sumOf { it.amount }
    }
    val totalDistributed = remember(donations) {
        donations.filter { it.entryTypeEnum() == DonationEntryType.RATION_OUT }.sumOf { it.amount }
    }
    val availableBalance = (totalDonated - totalDistributed).coerceAtLeast(0.0)
    val familiesHelped = remember(donations) {
        donations.count { it.entryTypeEnum() == DonationEntryType.RATION_OUT }
    }

    val qrBitmap by produceState<Bitmap?>(initialValue = null, key1 = RationBankDetails.QR_PAYLOAD) {
        value = withContext(Dispatchers.Default) {
            generateHblQrBitmap(RationBankDetails.QR_PAYLOAD, 384)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Charity Appeal Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF0A4D3C), Color(0xFF006A4E), Color(0xFF0F5132))
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = SaffronSecondary,
                                    shape = CircleShape,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.VolunteerActivism,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = language.pick(
                                            "راشن امداد فنڈ (مستحق خاندانوں کی مدد)",
                                            "Ration Charity Fund (Help Deserving Families)"
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = language.pick(
                                            "احمد بلال کریانہ سٹور ٹانڈہ • ضلع گجرات",
                                            "Ahmad Bilal Kiryana Store, Tanda • District Gujrat"
                                        ),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color(0xFFFDE68A)
                                    )
                                }
                            }

                            Text(
                                text = language.pick(
                                    "مخیر حضرات غریب اور مستحق خاندانوں کے راشن کے لیے دکاندار کے تصدیق شدہ HBL اکاؤنٹ میں براہِ راست عطیہ جمع کروا سکتے ہیں۔ ہر روپے اور تقسیم شدہ راشن کا مکمل ریکارڈ یہاں محفوظ رہتا ہے۔",
                                    "Philanthropists can donate directly into the shopkeeper's verified HBL account to provide ration packs to deserving families. Every rupee donated and ration pack distributed is transparently logged here."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // 2. Fund Transparency Summary Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FundStatTile(
                        label = language.pick("موجودہ فنڈ", "Available Fund"),
                        value = formatPkr(availableBalance, language),
                        color = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    FundStatTile(
                        label = language.pick("کل عطیات", "Total Donated"),
                        value = formatPkr(totalDonated, language),
                        color = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    FundStatTile(
                        label = language.pick("مستحق خاندان", "Families Helped"),
                        value = "$familiesHelped",
                        color = SaffronSecondary,
                        modifier = Modifier.weight(0.85f)
                    )
                }
            }

            // 3. Verified HBL Account & QR Code Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = Color(0xFF007A5E),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = "HBL • Habib Bank Limited",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp)
                            )
                        }

                        // Scannable QR Code with centered HBL emblem
                        val qr = qrBitmap
                        if (qr != null) {
                            Box(
                                modifier = Modifier
                                    .size(210.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White)
                                    .border(2.dp, Color(0xFF007A5E), RoundedCornerShape(16.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qr.asImageBitmap(),
                                    contentDescription = language.pick(
                                        "HBL راشن ڈونیشن کیو آر کوڈ",
                                        "HBL Ration Donation QR Code"
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = Color(0xFF007A5E),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .border(2.dp, Color.White, CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "HBL",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = RationBankDetails.ACCOUNT_TITLE,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        HorizontalDivider()

                        BankCopyRow(
                            label = language.pick("اکاؤنٹ نمبر", "Account Number"),
                            value = RationBankDetails.ACCOUNT_NUMBER,
                            testTag = "copy_hbl_account_button",
                            onCopy = {
                                clipboard.setText(AnnotatedString(RationBankDetails.ACCOUNT_NUMBER))
                                onShowMessage(
                                    language.pick(
                                        "اکاؤنٹ نمبر کاپی ہو گیا: ${RationBankDetails.ACCOUNT_NUMBER}",
                                        "Account Number copied: ${RationBankDetails.ACCOUNT_NUMBER}"
                                    )
                                )
                            }
                        )

                        BankCopyRow(
                            label = "IBAN",
                            value = RationBankDetails.IBAN,
                            testTag = "copy_hbl_iban_button",
                            onCopy = {
                                clipboard.setText(AnnotatedString(RationBankDetails.IBAN))
                                onShowMessage(
                                    language.pick(
                                        "IBAN کاپی ہو گیا: ${RationBankDetails.IBAN}",
                                        "IBAN copied: ${RationBankDetails.IBAN}"
                                    )
                                )
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.pick("برانچ کا نام", "Branch Name"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = RationBankDetails.BRANCH_NAME,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Share Appeal Button (WhatsApp / SMS)
                        Button(
                            onClick = {
                                val appeal = buildDonationAppealMessage(language)
                                ReceiptPrintHelper.shareText(
                                    context = context,
                                    text = appeal,
                                    title = language.pick("راشن امداد اکاؤنٹ شیئر کریں", "Share Ration Donation Account")
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A5E)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("share_donation_account_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                language.pick(
                                    "مخیر حضرات کو اکاؤنٹ تفصیل شیئر کریں (واٹس ایپ)",
                                    "Share Bank Details with Donors (WhatsApp)"
                                )
                            )
                        }
                    }
                }
            }

            // 4. Suggested Gujrat Ration Packs
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = language.pick("راشن پیکج گائیڈ (گجرات ریٹس)", "Suggested Ration Packs (Gujrat Rates)"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = language.pick(
                                "• چھوٹا راشن پیک (3,500 روپے): آٹا 10 کلو، گھی 1 کلو، چینی 2 کلو، دال 1 کلو، چائے، نمک",
                                "• Small Family Pack (Rs. 3,500): Atta 10kg, Ghee 1kg, Sugar 2kg, Daal 1kg, Tea, Salt"
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = language.pick(
                                "• مکمل خاندانی راشن پیک (6,500 روپے): آٹا 20 کلو، گھی 2 کلو، چینی 3 کلو، چاول 3 کلو، دالیں 2 کلو، چائے، مصالحے",
                                "• Full Family Pack (Rs. 6,500): Atta 20kg, Ghee 2kg, Sugar 3kg, Rice 3kg, Pulses 2kg, Tea, Spices"
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // 5. Action Buttons: Record Donation / Record Ration Distribution
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { activeDialogType = DonationEntryType.DONATION_IN },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("record_donation_in_button")
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("عطیہ درج کریں", "Add Donation"))
                    }
                    FilledTonalButton(
                        onClick = { activeDialogType = DonationEntryType.RATION_OUT },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("record_ration_out_button")
                    ) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("راشن تقسیم", "Give Ration"))
                    }
                }
            }

            // 6. Transparent Charity Ledger History
            item {
                Text(
                    text = language.pick("راشن فنڈ کا مکمل ریکارڈ", "Ration Fund Transparency Log"),
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (donations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = language.pick(
                                "ابھی کوئی عطیہ یا راشن تقسیم درج نہیں ہوئی۔ اوپر بٹن سے پہلا اندراج کریں۔",
                                "No donations or ration distributions logged yet. Use the buttons above to record."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        )
                    }
                }
            } else {
                items(donations, key = { it.id }) { entry ->
                    val isDonation = entry.entryTypeEnum() == DonationEntryType.DONATION_IN
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.personName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isDonation) {
                                        language.pick("عطیہ موصول", "Donation Received")
                                    } else {
                                        language.pick("راشن تقسیم (مستحق خاندان)", "Ration Distributed")
                                    } + " • " + ReceiptFormatter.formatDateTime(
                                        entry.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (entry.rationSummary.isNotBlank()) {
                                    Text(
                                        text = entry.rationSummary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (entry.paymentRef.isNotBlank()) {
                                    Text(
                                        text = entry.paymentRef,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = (if (isDonation) "+" else "-") + formatPkr(entry.amount, language),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDonation) ProfitGreen else SaffronSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (activeDialogType != null) {
        RationEntryDialog(
            language = language,
            entryType = activeDialogType!!,
            onConfirm = { name, phone, amount, summary, ref ->
                onRecordEntry(activeDialogType!!, name, phone, amount, summary, ref)
                activeDialogType = null
            },
            onDismiss = { activeDialogType = null }
        )
    }
}

@Composable
private fun FundStatTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BankCopyRow(
    label: String,
    value: String,
    testTag: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(
            onClick = onCopy,
            modifier = Modifier.testTag(testTag)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun RationEntryDialog(
    language: AppLanguage,
    entryType: DonationEntryType,
    onConfirm: (String, String, Double, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDonation = entryType == DonationEntryType.DONATION_IN
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf(if (isDonation) "6500" else "3500") }
    var summary by remember {
        mutableStateOf(
            if (isDonation) "" else language.pick(
                "آٹا 20 کلو، گھی 2 کلو، چینی 3 کلو، چاول، دالیں",
                "Atta 20kg, Ghee 2kg, Sugar 3kg, Rice, Pulses"
            )
        )
    }
    var ref by remember { mutableStateOf(if (isDonation) "HBL Transfer (${RationBankDetails.ACCOUNT_NUMBER})" else "Shop Counter") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isDonation) language.pick("راشن عطیہ درج کریں", "Record Ration Donation")
                else language.pick("مستحق خاندان کو راشن تقسیم", "Record Ration Distribution")
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            if (isDonation) language.pick("مخیر کا نام (یا گمنام)", "Donor Name (or Anonymous)")
                            else language.pick("مستحق خاندان / وصول کنندہ کا نام", "Deserving Family / Recipient Name")
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text(language.pick("رقم / راشن مالیت (روپے)*", "Amount / Ration Value (PKR)*")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("3500", "6500", "10000").forEach { preset ->
                        OutlinedButton(
                            onClick = { amountStr = preset },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(preset)
                        }
                    }
                }
                OutlinedTextField(
                    value = summary,
                    onValueChange = { summary = it },
                    label = { Text(language.pick("راشن تفصیل / نوٹ", "Ration Items / Note")) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text(language.pick("حوالہ (HBL / نقد)", "Reference (HBL / Cash)")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(language.pick("فون نمبر (اختیاری)", "Phone (optional)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onConfirm(name, phone, amt, summary, ref)
                },
                enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0
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

private fun buildDonationAppealMessage(language: AppLanguage): String {
    return if (language == AppLanguage.URDU) {
        """
        🌿 *راشن امداد فنڈ — احمد بلال کریانہ سٹور ٹانڈہ (ضلع گجرات)* 🌿
        غریب اور مستحق خاندانوں کو راشن فراہم کرنے کے لیے مخیر حضرات دکاندار کے تصدیق شدہ HBL اکاؤنٹ میں تعاون کر سکتے ہیں:

        🏦 بینک: ${RationBankDetails.BANK_NAME}
        👤 نام: ${RationBankDetails.ACCOUNT_TITLE}
        🔢 اکاؤنٹ نمبر: ${RationBankDetails.ACCOUNT_NUMBER}
        💳 IBAN: ${RationBankDetails.IBAN}
        📍 برانچ: ${RationBankDetails.BRANCH_NAME}

        جزاک اللہ خیراً!
        """.trimIndent()
    } else {
        """
        *Ration Charity Fund — Ahmad Bilal Kiryana Store, Tanda (District Gujrat)*
        Help deserving & needy families with monthly groceries by donating to our verified HBL account:

        Bank: ${RationBankDetails.BANK_NAME}
        Account Title: ${RationBankDetails.ACCOUNT_TITLE}
        Account Number: ${RationBankDetails.ACCOUNT_NUMBER}
        IBAN: ${RationBankDetails.IBAN}
        Branch: ${RationBankDetails.BRANCH_NAME}

        JazakAllah Khair!
        """.trimIndent()
    }
}

private fun generateHblQrBitmap(content: String, sizePx: Int): Bitmap? {
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
            EncodeHintType.MARGIN to 1
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }
    } catch (_: Exception) {
        null
    }
}
