package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.DonationEntryType
import com.example.data.DonationStatus
import com.example.data.RationDonation
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.pick
import com.example.services.RaastEmvQrHelper
import com.example.services.ReceiptFormatter
import com.example.services.ReceiptPrintHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Permanently hardcoded and locked central charity donation account for راشن امداد فنڈ.
 * Intentionally immutable and decoupled from ShopSettings so no shopkeeper can edit or change it.
 */
object RationBankDetails {
    const val BANK_NAME = "HBL (Habib Bank Limited)"
    const val ACCOUNT_TITLE = "Muhammad Asif"
    const val ACCOUNT_NUMBER = "16857900668003"
    const val IBAN = "PK08HABB0016857900668003"
    const val BRANCH_NAME = "MOTA, (TANDA)"

    val OFFICIAL_HBL_QR_PAYLOAD: String by lazy {
        RaastEmvQrHelper.buildRaastEmvQrPayload(
            iban = IBAN,
            accountNumber = ACCOUNT_NUMBER,
            merchantName = ACCOUNT_TITLE,
            merchantCity = "Tanda Gujrat",
            amountPkr = null,
            billReference = "RATION FUND"
        )
    }
}

@Composable
fun RationFundScreen(
    language: AppLanguage,
    donations: List<RationDonation>,
    onRecordEntry: (
        entryType: DonationEntryType,
        personName: String,
        phone: String,
        amount: Double,
        rationSummary: String,
        paymentRef: String,
        senderAccount: String,
        transactionId: String,
        receiptImagePath: String
    ) -> Unit,
    onUpdateDonationStatus: (donationId: String, newStatus: DonationStatus) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var activeDialogType by remember { mutableStateOf<DonationEntryType?>(null) }
    var previewReceiptPath by remember { mutableStateOf<String?>(null) }

    // Pending HBL transfer submissions awaiting shop owner verification
    val pendingDonations = remember(donations) {
        donations.filter {
            it.entryTypeEnum() == DonationEntryType.DONATION_IN &&
                it.statusEnum() == DonationStatus.PENDING
        }
    }

    // Verified & historical ledger (never count PENDING or REJECTED in official fund!)
    val verifiedAndLedgerEntries = remember(donations) {
        donations.filter {
            it.entryTypeEnum() == DonationEntryType.RATION_OUT ||
                it.statusEnum() != DonationStatus.PENDING
        }
    }

    val totalDonated = remember(donations) {
        donations.filter {
            it.entryTypeEnum() == DonationEntryType.DONATION_IN &&
                it.statusEnum() == DonationStatus.VERIFIED
        }.sumOf { it.amount }
    }
    val totalDistributed = remember(donations) {
        donations.filter { it.entryTypeEnum() == DonationEntryType.RATION_OUT }.sumOf { it.amount }
    }
    val availableBalance = (totalDonated - totalDistributed).coerceAtLeast(0.0)
    val familiesHelped = remember(donations) {
        donations.count { it.entryTypeEnum() == DonationEntryType.RATION_OUT }
    }

    val qrBitmap by produceState<Bitmap?>(initialValue = null, key1 = RationBankDetails.OFFICIAL_HBL_QR_PAYLOAD) {
        value = withContext(Dispatchers.Default) {
            generateHblQrBitmap(RationBankDetails.OFFICIAL_HBL_QR_PAYLOAD, 512)
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
                                            "راشن امداد فنڈ (HBL تصدیق شدہ)",
                                            "Ration Charity Fund (Verified HBL)"
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
                                    "پہلے نیچے دیے گئے HBL اکاؤنٹ میں رقم بھیجیں، پھر اپنا اکاؤنٹ نمبر اور ٹرانزیکشن آئی ڈی (TID) درج کریں۔ دکاندار کی HBL تصدیق کے بعد ہی رقم فنڈ میں شامل ہوگی۔",
                                    "First transfer directly to our HBL account below, then submit your Sender Account & Transaction ID (TID). Donations are added to the official fund only after the shop owner verifies receipt in HBL."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }
            }

            // 2. Fund Transparency Summary Row (Counts ONLY VERIFIED Donations!)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FundStatTile(
                        label = language.pick("موجودہ تصدیق شدہ فنڈ", "Verified Fund"),
                        value = formatPkr(availableBalance, language),
                        color = EmeraldPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    FundStatTile(
                        label = language.pick("کل تصدیق شدہ عطیات", "Verified Donated"),
                        value = formatPkr(totalDonated, language),
                        color = ProfitGreen,
                        modifier = Modifier.weight(1f)
                    )
                    FundStatTile(
                        label = language.pick("مستحق خاندان", "Families"),
                        value = "$familiesHelped",
                        color = SaffronSecondary,
                        modifier = Modifier.weight(0.8f)
                    )
                }
            }

            // 3. Existing Verified HBL Account & QR Code Card + One-Tap Copy Account Number
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color(0xFF007A5E).copy(alpha = 0.35f)),
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
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = RationBankDetails.BANK_NAME,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White
                                )
                            }
                        }

                        Surface(
                            color = Color(0xFF007A5E).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = language.pick(
                                    "مستقل مرکزی امداد اکاؤنٹ • ناقابلِ ترمیم (Official HBL RAAST QR)",
                                    "Locked Central Charity Account • Read-Only (Official HBL RAAST QR)"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007A5E),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        // Scannable QR Code with centered HBL emblem
                        val qr = qrBitmap
                        if (qr != null) {
                            Box(
                                modifier = Modifier
                                    .size(196.dp)
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
                                        .size(42.dp)
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.pick("بینک", "Bank"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = RationBankDetails.BANK_NAME,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF007A5E)
                            )
                        }

                        BankCopyRow(
                            label = language.pick("اکاؤنٹ نمبر (HBL)", "HBL Account Number"),
                            value = RationBankDetails.ACCOUNT_NUMBER,
                            testTag = "copy_hbl_account_icon_button",
                            onCopy = {
                                clipboard.setText(AnnotatedString(RationBankDetails.ACCOUNT_NUMBER))
                                onShowMessage(
                                    language.pick(
                                        "HBL اکاؤنٹ نمبر کاپی ہو گیا: ${RationBankDetails.ACCOUNT_NUMBER}",
                                        "HBL Account Number copied: ${RationBankDetails.ACCOUNT_NUMBER}"
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

                        // Prominent One-Tap Copy Account Number + Share Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    clipboard.setText(AnnotatedString(RationBankDetails.ACCOUNT_NUMBER))
                                    onShowMessage(
                                        language.pick(
                                            "اکاؤنٹ نمبر کاپی ہو گیا: ${RationBankDetails.ACCOUNT_NUMBER}",
                                            "Account Number copied: ${RationBankDetails.ACCOUNT_NUMBER}"
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007A5E)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1.15f)
                                    .testTag("copy_hbl_account_button")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    language.pick("اکاؤنٹ نمبر کاپی کریں", "Copy Account Number"),
                                    maxLines = 1
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    val appeal = buildDonationAppealMessage(language)
                                    ReceiptPrintHelper.shareText(
                                        context = context,
                                        text = appeal,
                                        title = language.pick("راشن امداد اکاؤنٹ شیئر کریں", "Share Ration Donation Account")
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(0.85f)
                                    .testTag("share_donation_account_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.size(5.dp))
                                Text(language.pick("واٹس ایپ شیئر", "Share"), maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 4. Action Buttons: Submit HBL Donation Proof / Record Ration Distribution
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
                        Text(language.pick("HBL عطیہ جمع کریں", "Submit Donation"))
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

            // 5. Shop Owner Pending Verification Queue (تصدیق طلب عطیات)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = language.pick(
                                "تصدیق طلب عطیات (${pendingDonations.size})",
                                "Pending Verification (${pendingDonations.size})"
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (pendingDonations.isNotEmpty()) {
                        Surface(
                            color = WarningAmber.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text(
                                text = language.pick("فنڈ میں شامل نہیں", "Not in Fund Yet"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = WarningAmber,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (pendingDonations.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ProfitGreen,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = language.pick(
                                    "کوئی عطیہ تصدیق کا منتظر نہیں۔ نئے HBL عطیات پہلے یہاں تصدیق کے لیے آئیں گے۔",
                                    "No donations waiting for verification. New HBL donations will appear here for owner review before entering the fund."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(pendingDonations, key = { "pending_${it.id}" }) { entry ->
                    PendingDonationVerificationCard(
                        language = language,
                        entry = entry,
                        onPreviewReceipt = { path -> previewReceiptPath = path },
                        onApprove = { onUpdateDonationStatus(entry.id, DonationStatus.VERIFIED) },
                        onReject = { onUpdateDonationStatus(entry.id, DonationStatus.REJECTED) }
                    )
                }
            }

            // 6. Suggested Gujrat Ration Packs
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

            // 7. Verified & Historical Charity Ledger
            item {
                Text(
                    text = language.pick("تصدیق شدہ راشن فنڈ ریکارڈ", "Verified Charity Fund Log"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (verifiedAndLedgerEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = language.pick(
                                "ابھی کوئی تصدیق شدہ عطیہ یا راشن تقسیم درج نہیں ہوئی۔",
                                "No verified donations or ration distributions logged yet."
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
                items(verifiedAndLedgerEntries, key = { it.id }) { entry ->
                    val isDonation = entry.entryTypeEnum() == DonationEntryType.DONATION_IN
                    val isRejected = isDonation && entry.statusEnum() == DonationStatus.REJECTED
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isRejected -> UdharCrimson.copy(alpha = 0.35f)
                                isDonation -> ProfitGreen.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            }
                        ),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = entry.personName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (isDonation) {
                                            Surface(
                                                color = if (isRejected) UdharCrimson.copy(alpha = 0.14f) else ProfitGreen.copy(alpha = 0.14f),
                                                shape = RoundedCornerShape(50.dp)
                                            ) {
                                                Text(
                                                    text = if (isRejected) {
                                                        language.pick("مسترد", "Rejected")
                                                    } else {
                                                        language.pick("تصدیق شدہ", "Verified")
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isRejected) UdharCrimson else ProfitGreen,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = if (isDonation) {
                                            if (isRejected) language.pick("مسترد شدہ عطیہ (فنڈ میں شامل نہیں)", "Rejected Donation (Not counted)")
                                            else language.pick("تصدیق شدہ HBL عطیہ", "Verified HBL Donation")
                                        } else {
                                            language.pick("راشن تقسیم (مستحق خاندان)", "Ration Distributed")
                                        } + " • " + ReceiptFormatter.formatDateTime(
                                            entry.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (entry.senderAccount.isNotBlank()) {
                                        Text(
                                            text = "${language.pick("بھیجنے والا اکاؤنٹ", "Sender")}: ${entry.senderAccount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (entry.transactionId.isNotBlank()) {
                                        Text(
                                            text = "TID: ${entry.transactionId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (entry.rationSummary.isNotBlank()) {
                                        Text(
                                            text = entry.rationSummary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Text(
                                    text = when {
                                        isRejected -> formatPkr(entry.amount, language)
                                        isDonation -> "+" + formatPkr(entry.amount, language)
                                        else -> "-" + formatPkr(entry.amount, language)
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isRejected -> MaterialTheme.colorScheme.onSurfaceVariant
                                        isDonation -> ProfitGreen
                                        else -> SaffronSecondary
                                    }
                                )
                            }

                            if (entry.receiptImagePath.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { previewReceiptPath = entry.receiptImagePath },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text(
                                        text = language.pick("رسید کی تصویر دیکھیں", "View Receipt Screenshot"),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
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
            onConfirm = { name, phone, amount, summary, ref, senderAccount, tid, receiptPath ->
                onRecordEntry(
                    activeDialogType!!,
                    name,
                    phone,
                    amount,
                    summary,
                    ref,
                    senderAccount,
                    tid,
                    receiptPath
                )
                activeDialogType = null
            },
            onDismiss = { activeDialogType = null }
        )
    }

    if (previewReceiptPath != null) {
        ReceiptImagePreviewDialog(
            language = language,
            filePath = previewReceiptPath!!,
            onDismiss = { previewReceiptPath = null }
        )
    }
}

@Composable
private fun PendingDonationVerificationCard(
    language: AppLanguage,
    entry: RationDonation,
    onPreviewReceipt: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val receiptThumb by produceState<Bitmap?>(initialValue = null, key1 = entry.receiptImagePath) {
        value = if (entry.receiptImagePath.isNotBlank()) {
            withContext(Dispatchers.IO) { decodeSampledBitmap(entry.receiptImagePath, 240) }
        } else null
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pending_donation_card_${entry.id}"),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.5.dp, WarningAmber.copy(alpha = 0.7f)),
        colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.06f))
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Surface(
                        color = WarningAmber.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = language.pick("تصدیق باقی • PENDING", "PENDING • تصدیق باقی"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = entry.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.phone.isNotBlank()) {
                        Text(
                            text = "${language.pick("فون", "Phone")}: ${entry.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = formatPkr(entry.amount, language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${language.pick("بھیجنے والے کا اکاؤنٹ / نام", "Sender Account / Title")}: ${entry.senderAccount}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${language.pick("ٹرانزیکشن آئی ڈی (TID)", "Transaction ID (TID)")}: ${entry.transactionId}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = ReceiptFormatter.formatDateTime(
                            entry.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (receiptThumb != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { onPreviewReceipt(entry.receiptImagePath) }
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = receiptThumb!!.asImageBitmap(),
                        contentDescription = language.pick("ادائیگی کی رسید", "Payment Receipt Screenshot"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = language.pick("رسید کی تصویر منسلک ہے", "Payment Receipt Attached"),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = language.pick("بڑی تصویر دیکھنے کے لیے ٹیپ کریں", "Tap to view full receipt screenshot"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Owner Action Buttons: Verify & Approve / Reject
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = ProfitGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.35f)
                        .testTag("verify_donation_${entry.id}")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(
                        text = language.pick("رقم موصول ہو گئی - تصدیق کریں", "Verify & Approve"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onReject,
                    border = BorderStroke(1.dp, UdharCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.65f)
                        .testTag("reject_donation_${entry.id}")
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = null,
                        tint = UdharCrimson,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = language.pick("مسترد کریں", "Reject"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = UdharCrimson
                    )
                }
            }
        }
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
    onConfirm: (
        name: String,
        phone: String,
        amount: Double,
        summary: String,
        ref: String,
        senderAccount: String,
        tid: String,
        receiptPath: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDonation = entryType == DonationEntryType.DONATION_IN

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf(if (isDonation) "" else "3500") }
    var senderAccount by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var receiptImagePath by remember { mutableStateOf("") }
    var summary by remember {
        mutableStateOf(
            if (isDonation) "" else language.pick(
                "آٹا 20 کلو، گھی 2 کلو، چینی 3 کلو، چاول، دالیں",
                "Atta 20kg, Ghee 2kg, Sugar 3kg, Rice, Pulses"
            )
        )
    }
    var ref by remember { mutableStateOf(if (isDonation) "HBL Transfer (${RationBankDetails.ACCOUNT_NUMBER})" else "Shop Counter") }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Google Play-compliant zero-permission Android Photo Picker for optional receipt screenshot
    val pickReceiptLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    copyUriToInternalReceiptFile(context, uri)
                }
                if (savedPath != null) {
                    receiptImagePath = savedPath
                }
            }
        }
    }

    val receiptPreview by produceState<Bitmap?>(initialValue = null, key1 = receiptImagePath) {
        value = if (receiptImagePath.isNotBlank()) {
            withContext(Dispatchers.IO) { decodeSampledBitmap(receiptImagePath, 300) }
        } else null
    }

    val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
    val isValidDonation = !isDonation || (
        name.isNotBlank() &&
            phone.isNotBlank() &&
            parsedAmount > 0.0 &&
            senderAccount.isNotBlank() &&
            transactionId.isNotBlank()
        )
    val isValidDistribution = isDonation || (parsedAmount > 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isDonation) language.pick("HBL عطیہ ثبوت جمع کروائیں", "Submit HBL Transfer Proof")
                else language.pick("مستحق خاندان کو راشن تقسیم", "Record Ration Distribution"),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isDonation) {
                    Surface(
                        color = WarningAmber.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = language.pick(
                                "پہلے HBL اکاؤنٹ (${RationBankDetails.ACCOUNT_NUMBER}) میں رقم بھیجیں۔ یہ عطیہ 'تصدیق باقی' کے طور پر محفوظ ہوگا اور دکاندار کی HBL تصدیق کے بعد فنڈ میں شامل ہوگا۔",
                                "Transfer to HBL (${RationBankDetails.ACCOUNT_NUMBER}) first. This entry is saved as PENDING and added to the Charity Fund only after the owner verifies it in HBL."
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        validationError = null
                    },
                    label = {
                        Text(
                            if (isDonation) language.pick("عطیہ دہندہ کا مکمل نام*", "Donor Full Name*")
                            else language.pick("مستحق خاندان / وصول کنندہ کا نام", "Deserving Family / Recipient Name")
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donation_donor_name_input")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        validationError = null
                    },
                    label = {
                        Text(
                            if (isDonation) language.pick("فون نمبر*", "Phone Number*")
                            else language.pick("فون نمبر (اختیاری)", "Phone (optional)")
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donation_phone_input")
                )

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it.filter { c -> c.isDigit() || c == '.' }
                        validationError = null
                    },
                    label = {
                        Text(
                            if (isDonation) language.pick("بھیجی گئی رقم (Rs.)*", "Amount Sent (Rs.)*")
                            else language.pick("راشن مالیت (Rs.)*", "Ration Value (Rs.)*")
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("donation_amount_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("3500", "6500", "10000").forEach { preset ->
                        OutlinedButton(
                            onClick = { amountStr = preset },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rs. $preset", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (isDonation) {
                    OutlinedTextField(
                        value = senderAccount,
                        onValueChange = {
                            senderAccount = it
                            validationError = null
                        },
                        label = {
                            Text(
                                language.pick(
                                    "بھیجنے والے کا اکاؤنٹ نمبر / نام*",
                                    "Sender Account Number / Title*"
                                )
                            )
                        },
                        placeholder = { Text(language.pick("مثال: HBL / JazzCash / EasyPaisa", "e.g. HBL / JazzCash / Title")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donation_sender_account_input")
                    )

                    OutlinedTextField(
                        value = transactionId,
                        onValueChange = {
                            transactionId = it
                            validationError = null
                        },
                        label = {
                            Text(
                                language.pick(
                                    "ٹرانزیکشن آئی ڈی (TID)*",
                                    "Transaction ID / TID*"
                                )
                            )
                        },
                        placeholder = { Text(language.pick("بینک یا ایپ کا TID نمبر درج کریں", "Enter bank / app TID number")) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("donation_tid_input")
                    )

                    // Optional Payment Receipt Screenshot via Zero-Permission Photo Picker
                    OutlinedButton(
                        onClick = {
                            pickReceiptLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pick_donation_receipt_button")
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            if (receiptImagePath.isBlank()) {
                                language.pick("رسید کی تصویر منسلک کریں (اختیاری)", "Attach Receipt Screenshot (Optional)")
                            } else {
                                language.pick("رسید کی تصویر تبدیل کریں ✓", "Change Receipt Screenshot ✓")
                            }
                        )
                    }

                    if (receiptPreview != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = receiptPreview!!.asImageBitmap(),
                                contentDescription = language.pick("منتخب رسید", "Selected receipt screenshot"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Text(
                                text = language.pick("رسید منسلک ہو گئی", "Receipt attached"),
                                style = MaterialTheme.typography.labelMedium,
                                color = ProfitGreen,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { receiptImagePath = "" }) {
                                Text(language.pick("ہٹائیں", "Remove"), color = UdharCrimson)
                            }
                        }
                    }
                } else {
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
                        label = { Text(language.pick("حوالہ (دکان کاؤنٹر)", "Reference (Shop Counter)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = UdharCrimson
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isDonation) {
                        if (name.isBlank() || phone.isBlank() || parsedAmount <= 0.0 ||
                            senderAccount.isBlank() || transactionId.isBlank()
                        ) {
                            validationError = language.pick(
                                "براہ کرم نام، فون، رقم، بھیجنے والے کا اکاؤنٹ اور ٹرانزیکشن آئی ڈی (TID) لازمی درج کریں۔",
                                "Please enter Donor Name, Phone, Amount, Sender Account, and Transaction ID (TID)."
                            )
                            return@Button
                        }
                    }
                    if (parsedAmount > 0.0) {
                        onConfirm(
                            name.trim(),
                            phone.trim(),
                            parsedAmount,
                            summary.trim(),
                            if (isDonation) "HBL TID: ${transactionId.trim()}" else ref.trim(),
                            senderAccount.trim(),
                            transactionId.trim(),
                            receiptImagePath
                        )
                    }
                },
                enabled = isValidDonation && isValidDistribution,
                modifier = Modifier.testTag("submit_donation_confirm_button")
            ) {
                Text(
                    if (isDonation) language.pick("تصدیق کے لیے جمع کروائیں", "Submit for Verification")
                    else language.pick("محفوظ کریں", "Save")
                )
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
private fun ReceiptImagePreviewDialog(
    language: AppLanguage,
    filePath: String,
    onDismiss: () -> Unit
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = filePath) {
        value = withContext(Dispatchers.IO) { decodeSampledBitmap(filePath, 1080) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("ادائیگی کی رسید", "Payment Receipt Screenshot")) },
        text = {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = language.pick("ادائیگی کی رسید", "Payment Receipt Screenshot"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Text(
                    text = language.pick("رسید کی تصویر دستیاب نہیں۔", "Receipt screenshot file unavailable."),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(language.pick("بند کریں", "Close"))
            }
        }
    )
}

private fun copyUriToInternalReceiptFile(context: Context, uri: Uri): String? {
    return try {
        val dir = File(context.filesDir, "donation_receipts")
        if (!dir.exists()) dir.mkdirs()
        val destFile = File(dir, "receipt_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        if (destFile.exists() && destFile.length() > 0L) destFile.absolutePath else null
    } catch (_: Exception) {
        null
    }
}

private fun decodeSampledBitmap(path: String, reqMaxPx: Int): Bitmap? {
    return try {
        val file = File(path)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        val maxDim = maxOf(bounds.outWidth, bounds.outHeight)
        var sampleSize = 1
        while (maxDim / sampleSize > reqMaxPx) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        BitmapFactory.decodeFile(path, opts)
    } catch (_: Exception) {
        null
    }
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

        رقم بھیجنے کے بعد ٹرانزیکشن آئی ڈی (TID) لازمی بھیجیں۔
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

        Please share your Transaction ID (TID) after transferring for verification.
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
