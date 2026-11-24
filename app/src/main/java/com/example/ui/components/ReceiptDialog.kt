package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.LedgerEntry
import com.example.data.LedgerEntryType
import com.example.data.Order
import com.example.data.OrderStatus
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.label
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.services.ReceiptFormatter
import com.example.services.ReceiptPrintHelper
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson

@Composable
fun AutoReceiptDialog(
    order: Order,
    language: AppLanguage,
    settings: ShopSettings,
    customerPhone: String = "",
    hasBluetoothPrinter: Boolean,
    onBluetoothPrint: () -> Unit,
    onEditOrder: ((Order) -> Unit)? = null,
    onDeleteOrder: ((Order) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = ReceiptFormatter.formatDateTime(order.createdAt?.toDate()?.time ?: System.currentTimeMillis())
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && onDeleteOrder != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
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
                        "کیا آپ واقعی بل نمبر ${order.billNumber} (${order.customerName}) کو مستقل طور پر ڈیلیٹ کرنا چاہتے ہیں؟",
                        "Are you sure you want to permanently delete Bill #${order.billNumber} (${order.customerName})?"
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteOrder(order)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UdharCrimson)
                ) {
                    Text(language.pick("جی ہاں، ڈیلیٹ کریں", "Yes, Delete"), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(language.pick("منسوخ", "Cancel"))
                }
            }
        )
    }

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
                    .heightIn(max = 700.dp)
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
                            text = if (order.statusEnum() == OrderStatus.PENDING) {
                                language.pick("آرڈر رسید تیار ہے", "Order Slip Ready")
                            } else {
                                language.pick("خودکار بل رسید", "Auto Bill Receipt")
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = language.pick("بند کریں", "Close")
                        )
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
                            text = language.pick(settings.storeNameUrdu, settings.storeNameEnglish),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = language.pick(settings.shopAddressUrdu, settings.shopAddressEnglish),
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

                        DashedReceiptDivider()

                        ReceiptMetaRow(label = language.pick("بل نمبر:", "Bill #:"), value = order.billNumber)
                        ReceiptMetaRow(label = language.pick("تاریخ:", "Date:"), value = dateStr)
                        ReceiptMetaRow(label = language.pick("کسٹمر کا نام:", "Customer:"), value = order.customerName, emphasize = true)
                        ReceiptMetaRow(label = language.pick("ادائیگی:", "Payment:"), value = order.paymentMethodEnum().label(language))

                        DashedReceiptDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(language.pick("اشیاء", "Item"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.2f))
                            Text(language.pick("مقدار", "Qty"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                            Text(language.pick("رقم", "Total"), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                        }

                        order.items.forEach { item ->
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
                                Text(formatQty(item.quantity), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                                Text("${item.lineTotal.toLong()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                        }
                                                Spacer(Modifier.height(4.dp))
                        if (order.discount > 0.0) {
                            ReceiptMetaRow(label = language.pick("کل قیمت:", "Subtotal:"), value = formatPkr(order.subtotal, language))
                            ReceiptMetaRow(label = language.pick("رعایت:", "Discount:"), value = "-${formatPkr(order.discount, language)}")
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(language.pick("واجب الادا کل:", "Grand Total:"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(formatPkr(order.totalAmount, language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        ReceiptMetaRow(label = language.pick("وصول شدہ رقم:", "Paid Amount:"), value = formatPkr(order.paidAmount, language))
                        if (order.remainingUdhar > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(language.pick("بقایا ادھار:", "Remaining Udhar:"), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UdharCrimson)
                                Text(formatPkr(order.remainingUdhar, language), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = UdharCrimson)
                            }
                        }

                        DashedReceiptDivider()
                        Text(
                            text = language.pick("خریداری کا شکریہ! ${settings.storeNameUrdu}", "Thank you for shopping at ${settings.storeNameEnglish}!"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Row 1: WhatsApp Text & PDF
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            ReceiptPrintHelper.shareOrderOnWhatsAppText(context, order, language, settings, customerPhone)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.weight(1f).testTag("receipt_whatsapp_text_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("واٹس ایپ ٹیکسٹ", "WhatsApp Text"), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            ReceiptPrintHelper.shareOrderPdfOnWhatsApp(context, order, language, settings)
                        },
                        modifier = Modifier.weight(1f).testTag("receipt_whatsapp_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("واٹس ایپ PDF", "WhatsApp PDF"), maxLines = 1)
                    }
                }

                // Row 2: 58mm Bluetooth & Print/PDF
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onBluetoothPrint,
                        modifier = Modifier.weight(1f).testTag("receipt_bluetooth_print_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(if (hasBluetoothPrinter) language.pick("58mm بلوٹوتھ", "58mm BT Print") else language.pick("58mm پرنٹ", "58mm BT Print"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            ReceiptPrintHelper.printOrderWithSystemPrint(context, order, language, settings.shopPhone, settings)
                        },
                        modifier = Modifier.weight(1f).testTag("receipt_pdf_print_button")
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("پرنٹ / PDF", "Print / PDF"), maxLines = 1)
                    }
                }

                // Row 3: Edit Bill & Delete Bill
                if (onEditOrder != null || onDeleteOrder != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onEditOrder != null) {
                            OutlinedButton(
                                onClick = { onEditOrder(order) },
                                modifier = Modifier.weight(1.3f).testTag("receipt_edit_bill_button")
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(language.pick("بل ایڈٹ کریں", "Edit Bill"), maxLines = 1)
                            }
                        }
                        if (onDeleteOrder != null) {
                            Button(
                                onClick = { showDeleteConfirm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = UdharCrimson),
                                modifier = Modifier.weight(1f).testTag("receipt_delete_bill_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(language.pick("بل ڈیلیٹ", "Delete Bill"), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentReceiptDialog(
    entry: LedgerEntry,
    language: AppLanguage,
    settings: ShopSettings,
    customerPhone: String = "",
    onBluetoothPrint: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isCredit = entry.entryTypeEnum() == LedgerEntryType.CREDIT
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isCredit) ProfitGreen else UdharCrimson,
                    modifier = Modifier.size(42.dp)
                )
                Text(
                    text = language.pick(settings.storeNameUrdu, settings.storeNameEnglish),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isCredit) {
                        language.pick("کھاتہ وصولی رسید محفوظ ہو گئی", "Khata Payment Receipt Saved")
                    } else {
                        language.pick("ادھار کھاتہ اندراج محفوظ ہو گیا", "Khata Udhar Entry Recorded")
                    },
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = entry.customerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatPkr(entry.amount, language),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCredit) ProfitGreen else UdharCrimson
                )
                Text(
                    text = language.pick(
                        "نیا بقایا ادھار: ${formatPkr(entry.balanceAfter, AppLanguage.URDU)}",
                        "Updated Balance: ${formatPkr(entry.balanceAfter, AppLanguage.ENGLISH)}"
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(2.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            ReceiptPrintHelper.shareKhataOnWhatsAppText(
                                context = context,
                                entry = entry,
                                language = language,
                                settings = settings,
                                customerPhone = customerPhone
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("khata_receipt_whatsapp_text_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("واٹس ایپ ٹیکسٹ", "WhatsApp"), maxLines = 1)
                    }
                    FilledTonalButton(
                        onClick = {
                            ReceiptPrintHelper.shareKhataPdfOnWhatsApp(
                                context = context,
                                entry = entry,
                                language = language,
                                settings = settings
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("khata_receipt_whatsapp_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("واٹس ایپ PDF", "PDF"), maxLines = 1)
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
                            .testTag("khata_receipt_bt_print_button")
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("58mm بلوٹوتھ", "58mm BT"), maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("khata_receipt_done_button")
                    ) {
                        Text(language.pick("ٹھیک ہے", "Done"))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptMetaRow(label: String, value: String, emphasize: Boolean = false) {
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
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun DashedReceiptDivider() {
    val lineColor = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .height(2.dp)
    ) {
        drawLine(
            color = lineColor,
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }
}
