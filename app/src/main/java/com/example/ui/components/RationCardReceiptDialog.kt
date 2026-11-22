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
    shopPhone: String,
    onBluetoothPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = ReceiptFormatter.formatDateTime(bill.createdAt?.toDate()?.time ?: System.currentTimeMillis())

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
                    .heightIn(max = 690.dp)
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
                            text = language.pick("مریم نواز راشن کارڈ رسید", "Ration Card Receipt"),
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
                                ReceiptFormatter.SHOP_NAME_URDU,
                                ReceiptFormatter.SHOP_NAME_ENGLISH
                            ),
                            style = MaterialTheme.typography.headlineMedium,
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
                                    "مریم نواز راشن کارڈ • حکومت پنجاب سبسڈی",
                                    "Maryam Nawaz Ration Card • Punjab Govt"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = language.pick(
                                ReceiptFormatter.SHOP_CITY_URDU,
                                ReceiptFormatter.SHOP_CITY_ENGLISH
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        MetaRow(language.pick("رسید نمبر:", "Receipt #:"), "${bill.billNumber} (${bill.monthKey})")
                        MetaRow(language.pick("تاریخ:", "Date:"), dateStr)
                        MetaRow(language.pick("صارف کا نام:", "Beneficiary:"), bill.beneficiaryName, bold = true)
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
                            language.pick("کل راشن بل:", "Total Ration Bill:"),
                            formatPkr(bill.totalAmount, language),
                            bold = true
                        )
                        MetaRow(
                            language.pick("راشن کارڈ سے کٹوتی:", "Ration Card Deducted:"),
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
                            language.pick("کارڈ میں بقایا کوٹہ:", "Remaining Monthly Quota:"),
                            formatPkr(bill.remainingSubsidy, language)
                        )
                    }
                }

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
                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("بلوٹوتھ پرنٹ", "BT Print"), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            ReceiptPrintHelper.printRationCardWithSystemPrint(
                                context = context,
                                bill = bill,
                                language = language,
                                shopPhone = shopPhone
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ration_receipt_pdf_print")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("پرنٹ / PDF", "Print / PDF"), maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = {
                        val shareText = ReceiptFormatter.buildRationCardShareText(bill, language, shopPhone)
                        ReceiptPrintHelper.shareText(
                            context = context,
                            text = shareText,
                            title = language.pick("راشن کارڈ رسید شیئر کریں", "Share Ration Card Receipt")
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ration_receipt_share")
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(language.pick("واٹس ایپ پر رسید شیئر کریں", "Share Receipt on WhatsApp"))
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
