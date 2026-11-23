package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.RationCardBill
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.services.ReceiptFormatter
import com.example.services.ReceiptPrintHelper
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary

@Composable
fun RationCardReceiptDialog(
    bill: RationCardBill,
    language: AppLanguage,
    settings: ShopSettings,
    onBluetoothPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = ReceiptFormatter.formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())
    val netLimit = if (bill.netSubsidyLimit > 0.0) bill.netSubsidyLimit else (bill.subsidyLimit - bill.feeDeduction).coerceAtLeast(0.0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 710.dp)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ProfitGreen,
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = language.pick("POS / پی او ایس رسید", "POS Card Receipt"),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_ration_receipt_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = language.pick("بند کریں", "Close"))
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = language.pick(
                                settings.storeNameUrdu,
                                settings.storeNameEnglish
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Surface(
                            color = SaffronSecondary,
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = language.pick(
                                    "POS / پی او ایس کارڈ • کل ${bill.subsidyLimit.toLong()} | فیس ${bill.feeDeduction.toLong()} | خالص ${netLimit.toLong()}",
                                    "POS Card • Quota ${bill.subsidyLimit.toLong()} | Fee ${bill.feeDeduction.toLong()} | Net ${netLimit.toLong()}"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = language.pick(
                                settings.shopAddressUrdu,
                                settings.shopAddressEnglish
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (settings.shopPhone.isNotBlank()) {
                            Text(
                                text = settings.shopPhone,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        MetaRow(language.pick("رسید نمبر:", "Receipt #:"), "${bill.billNumber} (${bill.monthKey})")
                        MetaRow(language.pick("تاریخ:", "Date:"), dateStr)
                        MetaRow(language.pick("صارف کا نام:", "Customer:"), bill.beneficiaryName, bold = true)
                        if (bill.phone.isNotBlank()) MetaRow(language.pick("موبائل:", "Phone:"), bill.phone)
                        if (bill.cnic.isNotBlank()) MetaRow(language.pick("شناختی کارڈ:", "CNIC:"), bill.cnic)
                        if (bill.cardNumber.isNotBlank()) MetaRow(language.pick("کارڈ نمبر:", "Card #:"), bill.cardNumber)
                        if (bill.cardUid.isNotBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Nfc,
                                        contentDescription = null,
                                        tint = ProfitGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "NFC UID:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = bill.cardUid,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = language.pick("اشیاء", "Item"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(2.2f)
                            )
                            Text(
                                text = language.pick("مقدار", "Qty"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = language.pick("رقم", "Total"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.2f)
                            )
                        }

                        bill.items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(2.2f)) {
                                    Text(
                                        text = if (language == AppLanguage.URDU) item.nameUrdu else item.nameEnglish,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "@ ${item.unitPrice.toLong()} / ${unitLabel(item.unit, language)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = formatQty(item.quantity),
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${item.lineTotal.toLong()}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1.2f)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        }

                        MetaRow(
                            language.pick("کل کارڈ کوٹہ:", "Total Quota:"),
                            formatPkr(bill.subsidyLimit, language)
                        )
                        MetaRow(
                            language.pick("سروس فیس کٹوتی:", "Service Fee:"),
                            "-${formatPkr(bill.feeDeduction, language)}"
                        )
                        MetaRow(
                            language.pick("خالص راشن ویلیو:", "Net Ration Value:"),
                            formatPkr(netLimit, language),
                            bold = true
                        )
                        MetaRow(
                            language.pick("کل POS بل:", "Total POS Bill:"),
                            formatPkr(bill.totalAmount, language),
                            bold = true
                        )
                        MetaRow(
                            language.pick("کارڈ سے کٹوتی:", "POS Card Deducted:"),
                            formatPkr(bill.subsidyUsed, language),
                            bold = true,
                            valueColor = ProfitGreen
                        )
                        if (bill.cashTopUp > 0.0) {
                            MetaRow(
                                language.pick("اضافی نقد ادائیگی:", "Extra Cash Paid:"),
                                formatPkr(bill.cashTopUp, language),
                                bold = true,
                                valueColor = SaffronSecondary
                            )
                        }
                        MetaRow(
                            language.pick("بقایا خالص بیلنس:", "Remaining Net Balance:"),
                            formatPkr(bill.remainingSubsidy, language)
                        )
                    }
                }

                // Row 1: One-tap WhatsApp Text & WhatsApp PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ReceiptPrintHelper.shareRationCardOnWhatsAppText(
                                context = context,
                                bill = bill,
                                language = language,
                                settings = settings
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ration_receipt_whatsapp_text")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("واٹس ایپ ٹیکسٹ", "WhatsApp Text"), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            ReceiptPrintHelper.shareRationCardPdfOnWhatsApp(
                                context = context,
                                bill = bill,
                                language = language,
                                settings = settings
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ration_receipt_whatsapp_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("واٹس ایپ PDF", "WhatsApp PDF"), maxLines = 1)
                    }
                }

                // Row 2: 58mm ESC/POS Bluetooth Thermal Print & System Print / Save PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onBluetoothPrint,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ration_receipt_bt_print"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("58mm بلوٹوتھ", "58mm BT Print"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            ReceiptPrintHelper.printRationCardWithSystemPrint(
                                context = context,
                                bill = bill,
                                language = language,
                                shopPhone = settings.shopPhone,
                                settings = settings
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ration_receipt_pdf_print")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("پرنٹ / PDF", "Print / PDF"), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaRow(
    label: String,
    value: String,
    bold: Boolean = false,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Medium,
            color = valueColor
        )
    }
}
