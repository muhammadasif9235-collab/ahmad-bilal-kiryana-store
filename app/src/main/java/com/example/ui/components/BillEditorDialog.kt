package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Order
import com.example.data.OrderItem
import com.example.data.PaymentMethod
import com.example.data.Product
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.label
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import java.util.Locale
import kotlin.math.round

private fun round2(value: Double): Double = round(value * 100.0) / 100.0

private fun formatCleanNumber(value: Double): String {
    val r = round(value * 1000.0) / 1000.0
    return if (r % 1.0 == 0.0) r.toLong().toString()
    else String.format(Locale.US, "%.3f", r).trimEnd('0').trimEnd('.')
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BillEditorDialog(
    order: Order,
    products: List<Product>,
    language: AppLanguage,
    isBusy: Boolean,
    onSaveUpdatedOrder: (
        updatedItems: List<OrderItem>,
        discount: Double,
        extraCharges: Double,
        finalTotalOverride: Double?,
        paymentMethod: PaymentMethod,
        paidAmount: Double,
        note: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    val editableItems = remember(order.id) {
        mutableStateListOf<OrderItem>().apply { addAll(order.items) }
    }

    val initialAutoTotal = remember(order.id) {
        round2((order.subtotal - order.discount).coerceAtLeast(0.0))
    }
    val initialExtra = remember(order.id) {
        if (order.totalAmount > initialAutoTotal + 0.01) round2(order.totalAmount - initialAutoTotal) else 0.0
    }

    var discountStr by remember(order.id) {
        mutableStateOf(if (order.discount > 0.0) formatCleanNumber(order.discount) else "0")
    }
    var extraChargesStr by remember(order.id) {
        mutableStateOf(if (initialExtra > 0.0) formatCleanNumber(initialExtra) else "0")
    }
    var useManualFinalTotal by remember(order.id) {
        mutableStateOf(
            order.totalAmount < initialAutoTotal - 0.01
        )
    }
    var manualFinalTotalStr by remember(order.id) {
        mutableStateOf(formatCleanNumber(order.totalAmount))
    }
    var paymentMethod by remember(order.id) { mutableStateOf(order.paymentMethodEnum()) }
    var paidAmountStr by remember(order.id) {
        mutableStateOf(formatCleanNumber(order.paidAmount))
    }
    var noteStr by remember(order.id) { mutableStateOf(order.note) }

    var showAddCatalogDialog by remember { mutableStateOf(false) }
    var showAddCustomItemDialog by remember { mutableStateOf(false) }

    val subtotal = round2(editableItems.sumOf { round2(it.quantity * it.unitPrice) })
    val discount = (discountStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val extraCharges = (extraChargesStr.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    val calculatedTotal = round2((subtotal - discount + extraCharges).coerceAtLeast(0.0))
    val effectiveFinalTotal = if (useManualFinalTotal) {
        round2((manualFinalTotalStr.toDoubleOrNull() ?: calculatedTotal).coerceAtLeast(0.0))
    } else {
        calculatedTotal
    }

    val effectivePaid = when (paymentMethod) {
        PaymentMethod.CASH -> effectiveFinalTotal
        PaymentMethod.UDHAR -> 0.0
        PaymentMethod.PARTIAL -> (paidAmountStr.toDoubleOrNull() ?: 0.0).coerceIn(0.0, effectiveFinalTotal)
    }
    val effectiveUdhar = round2((effectiveFinalTotal - effectivePaid).coerceAtLeast(0.0))
    val udharDelta = round2(effectiveUdhar - order.remainingUdhar)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 740.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = language.pick(
                                    "بل ایڈٹ / رقم کم یا زیادہ کریں",
                                    "Edit Bill & Adjust Amount"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "#${order.billNumber} • ${order.customerName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("bill_editor_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = language.pick("بند کریں", "Close"))
                    }
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Add Item Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { showAddCatalogDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_editor_add_catalog_item")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("اسٹاک سے آئٹم", "Add Catalog Item"), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = { showAddCustomItemDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_editor_add_custom_item")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("دستی آئٹم", "Custom Item"), maxLines = 1)
                        }
                    }

                    // Editable Items List
                    Text(
                        text = language.pick(
                            "بل کی اشیاء (${editableItems.size}) — مقدار، وزن یا ریٹ تبدیل کریں:",
                            "Bill Items (${editableItems.size}) — Edit qty, weight, or rate:"
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )

                    if (editableItems.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = language.pick(
                                    "بل میں کوئی آئٹم نہیں۔ اوپر سے نیا آئٹم شامل کریں یا حتمی رقم درج کریں۔",
                                    "No items in bill. Add an item above or enter a manual final total."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        editableItems.forEachIndexed { idx, item ->
                            EditableBillItemCard(
                                item = item,
                                language = language,
                                onUpdate = { updated ->
                                    editableItems[idx] = updated
                                    if (!useManualFinalTotal) {
                                        val nextSub = round2(editableItems.sumOf { round2(it.quantity * it.unitPrice) })
                                        manualFinalTotalStr = formatCleanNumber((nextSub - discount + extraCharges).coerceAtLeast(0.0))
                                    }
                                },
                                onRemove = {
                                    editableItems.removeAt(idx)
                                    if (!useManualFinalTotal) {
                                        val nextSub = round2(editableItems.sumOf { round2(it.quantity * it.unitPrice) })
                                        manualFinalTotalStr = formatCleanNumber((nextSub - discount + extraCharges).coerceAtLeast(0.0))
                                    }
                                }
                            )
                        }
                    }

                    HorizontalDivider()

                    // Discount & Extra Charges Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = discountStr,
                            onValueChange = { raw ->
                                discountStr = raw.filter { c -> c.isDigit() || c == '.' }
                                if (!useManualFinalTotal) {
                                    val d = discountStr.toDoubleOrNull() ?: 0.0
                                    manualFinalTotalStr = formatCleanNumber((subtotal - d + extraCharges).coerceAtLeast(0.0))
                                }
                            },
                            label = { Text(language.pick("رعایت / کم (Rs.)", "Discount (-)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_editor_discount_input")
                        )
                        OutlinedTextField(
                            value = extraChargesStr,
                            onValueChange = { raw ->
                                extraChargesStr = raw.filter { c -> c.isDigit() || c == '.' }
                                if (!useManualFinalTotal) {
                                    val ex = extraChargesStr.toDoubleOrNull() ?: 0.0
                                    manualFinalTotalStr = formatCleanNumber((subtotal - discount + ex).coerceAtLeast(0.0))
                                }
                            },
                            label = { Text(language.pick("اضافی چارجز (+)", "Extra Charges (+)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_editor_extra_input")
                        )
                    }

                    // Manual Final Bill Total Override Card (Freely Increase/Decrease Final Total)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (useManualFinalTotal) WarningAmber else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language.pick(
                                        "حتمی بل رقم تبدیل کریں (کم یا زیادہ):",
                                        "Final Bill Total (Manual Override):"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (useManualFinalTotal) {
                                    TextButton(
                                        onClick = {
                                            useManualFinalTotal = false
                                            manualFinalTotalStr = formatCleanNumber(calculatedTotal)
                                        }
                                    ) {
                                        Text(language.pick("آٹو ری سیٹ", "Auto Reset"))
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = if (useManualFinalTotal) manualFinalTotalStr else formatCleanNumber(calculatedTotal),
                                onValueChange = { raw ->
                                    useManualFinalTotal = true
                                    manualFinalTotalStr = raw.filter { c -> c.isDigit() || c == '.' }
                                },
                                label = { Text(language.pick("حتمی بل ٹوٹل (روپے)", "Final Bill Total (PKR)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bill_editor_final_total_input")
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(-100.0, -50.0, -20.0, 20.0, 50.0, 100.0).forEach { step ->
                                    val labelTxt = if (step > 0) "+${step.toLong()}" else "${step.toLong()}"
                                    FilterChip(
                                        selected = false,
                                        onClick = {
                                            val next = (effectiveFinalTotal + step).coerceAtLeast(0.0)
                                            useManualFinalTotal = true
                                            manualFinalTotalStr = formatCleanNumber(next)
                                        },
                                        label = {
                                            Text(
                                                text = labelTxt,
                                                fontWeight = FontWeight.Bold,
                                                color = if (step < 0) UdharCrimson else ProfitGreen
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Payment Mode & Paid Amount
                    Text(
                        text = language.pick("ادائیگی کا طریقہ:", "Payment Mode:"),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            PaymentMethod.CASH,
                            PaymentMethod.UDHAR,
                            PaymentMethod.PARTIAL
                        ).forEach { method ->
                            val enabled = method == PaymentMethod.CASH || order.customerId.isNotBlank()
                            FilterChip(
                                selected = paymentMethod == method,
                                enabled = enabled,
                                onClick = {
                                    paymentMethod = method
                                    if (method == PaymentMethod.CASH) {
                                        paidAmountStr = formatCleanNumber(effectiveFinalTotal)
                                    } else if (method == PaymentMethod.UDHAR) {
                                        paidAmountStr = "0"
                                    }
                                },
                                label = {
                                    Text(
                                        text = method.label(language),
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (paymentMethod == PaymentMethod.PARTIAL) {
                        OutlinedTextField(
                            value = paidAmountStr,
                            onValueChange = { paidAmountStr = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(language.pick("نقد وصول شدہ رقم (روپے)", "Cash Paid Amount (PKR)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bill_editor_paid_input")
                        )
                    }

                    OutlinedTextField(
                        value = noteStr,
                        onValueChange = { noteStr = it },
                        label = { Text(language.pick("ترمیم کا نوٹ / وجہ (اختیاری)", "Adjustment Note / Reason (optional)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live Recalculated Summary Strip
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(language.pick("پرانا بل ٹوٹل:", "Previous Total:"), style = MaterialTheme.typography.bodySmall)
                                Text(formatPkr(order.totalAmount, language), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    language.pick("نیا حتمی ٹوٹل:", "Updated Final Total:"),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    formatPkr(effectiveFinalTotal, language),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(language.pick("نقد وصولی:", "Paid:"), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    formatPkr(effectivePaid, language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ProfitGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    language.pick("نیا کھاتہ ادھار بقایا:", "Updated Khata Udhar:"),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    formatPkr(effectiveUdhar, language),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (effectiveUdhar > 0.0) UdharCrimson else ProfitGreen
                                )
                            }
                            if (order.customerId.isNotBlank() && kotlin.math.abs(udharDelta) >= 0.01) {
                                val deltaText = if (udharDelta > 0) "+${formatPkr(udharDelta, language)}" else "-${formatPkr(-udharDelta, language)}"
                                Text(
                                    text = language.pick(
                                        "کسٹمر کھاتہ میں خودکار تبدیلی: $deltaText",
                                        "Auto Khata Ledger Adjustment: $deltaText"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (udharDelta > 0) UdharCrimson else ProfitGreen
                                )
                            }
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.85f)
                    ) {
                        Text(language.pick("منسوخ", "Cancel"))
                    }
                    Button(
                        onClick = {
                            onSaveUpdatedOrder(
                                editableItems.toList(),
                                discount,
                                extraCharges,
                                if (useManualFinalTotal) effectiveFinalTotal else null,
                                paymentMethod,
                                effectivePaid,
                                noteStr
                            )
                        },
                        enabled = !isBusy && (editableItems.isNotEmpty() || effectiveFinalTotal > 0.0),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("bill_editor_save_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(
                            language.pick("محفوظ کریں اور رسید کھولیں", "Save & Open Receipt"),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    // Add from Catalog Dialog
    if (showAddCatalogDialog) {
        CatalogItemPickerDialog(
            products = products,
            language = language,
            onPickProduct = { prod ->
                val existingIdx = editableItems.indexOfFirst { it.productId == prod.id && prod.id.isNotBlank() }
                if (existingIdx >= 0) {
                    val cur = editableItems[existingIdx]
                    val nextQty = cur.quantity + 1.0
                    editableItems[existingIdx] = cur.copy(
                        quantity = nextQty,
                        lineTotal = round2(nextQty * cur.unitPrice)
                    )
                } else {
                    editableItems.add(
                        OrderItem(
                            productId = prod.id,
                            nameUrdu = prod.nameUrdu,
                            nameEnglish = prod.nameEnglish,
                            unit = prod.unit,
                            quantity = 1.0,
                            unitPrice = prod.sellingPrice,
                            costPrice = prod.costPrice,
                            lineTotal = round2(prod.sellingPrice)
                        )
                    )
                }
                showAddCatalogDialog = false
            },
            onDismiss = { showAddCatalogDialog = false }
        )
    }

    // Add Custom Manual Item Dialog
    if (showAddCustomItemDialog) {
        AddCustomBillItemDialog(
            language = language,
            onConfirm = { name, unit, qty, rate ->
                editableItems.add(
                    OrderItem(
                        productId = "",
                        nameUrdu = name,
                        nameEnglish = name,
                        unit = unit,
                        quantity = qty,
                        unitPrice = rate,
                        costPrice = rate * 0.9,
                        lineTotal = round2(qty * rate)
                    )
                )
                showAddCustomItemDialog = false
            },
            onDismiss = { showAddCustomItemDialog = false }
        )
    }
}

@Composable
private fun EditableBillItemCard(
    item: OrderItem,
    language: AppLanguage,
    onUpdate: (OrderItem) -> Unit,
    onRemove: () -> Unit
) {
    var qtyText by remember(item.productId, item.nameUrdu, item.quantity) {
        mutableStateOf(formatCleanNumber(item.quantity))
    }
    var rateText by remember(item.productId, item.nameUrdu, item.unitPrice) {
        mutableStateOf(formatCleanNumber(item.unitPrice))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.URDU) item.nameUrdu else item.nameEnglish,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${formatQty(item.quantity)} ${unitLabel(item.unit, language)} × ${item.unitPrice.toLong()} = ${formatPkr(item.lineTotal, language)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = language.pick("آئٹم ہٹائیں", "Remove item"),
                        tint = UdharCrimson
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { c -> c.isDigit() || c == '.' }
                        qtyText = cleaned
                        val parsed = cleaned.toDoubleOrNull()
                        if (parsed != null && parsed > 0.0) {
                            onUpdate(item.copy(quantity = parsed, lineTotal = round2(parsed * item.unitPrice)))
                        }
                    },
                    label = { Text("${language.pick("مقدار", "Qty")} (${unitLabel(item.unit, language)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = rateText,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { c -> c.isDigit() || c == '.' }
                        rateText = cleaned
                        val parsed = cleaned.toDoubleOrNull()
                        if (parsed != null && parsed >= 0.0) {
                            onUpdate(item.copy(unitPrice = parsed, lineTotal = round2(item.quantity * parsed)))
                        }
                    },
                    label = { Text(language.pick("ریٹ (Rs.)", "Rate (PKR)")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CatalogItemPickerDialog(
    products: List<Product>,
    language: AppLanguage,
    onPickProduct: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(products, query) {
        val q = query.trim()
        if (q.isBlank()) products.take(40)
        else products.filter {
            it.nameUrdu.contains(q, true) || it.nameEnglish.contains(q, true) || it.barcode.contains(q, true)
        }.take(40)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("اسٹاک سے آئٹم شامل کریں", "Add Item from Catalog")) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(language.pick("آئٹم تلاش کریں...", "Search item...")) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(filtered, key = { _, p -> p.id }) { _, prod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onPickProduct(prod) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (language == AppLanguage.URDU) prod.nameUrdu else prod.nameEnglish,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatPkr(prod.sellingPrice, language)} / ${unitLabel(prod.unit, language)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("بند کریں", "Close"))
            }
        }
    )
}

@Composable
private fun AddCustomBillItemDialog(
    language: AppLanguage,
    onConfirm: (name: String, unit: String, qty: Double, rate: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("kg") }
    var qtyStr by remember { mutableStateOf("1") }
    var rateStr by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(language.pick("دستی آئٹم شامل کریں", "Add Custom Bill Item")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(language.pick("آئٹم کا نام*", "Item Name*")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(language.pick("مقدار*", "Qty*")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rateStr,
                        onValueChange = { rateStr = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(language.pick("ریٹ (Rs.)*", "Rate (PKR)*")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("kg", "pcs", "pack", "liter").forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(unitLabel(u, language), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = qtyStr.toDoubleOrNull() ?: 0.0
                    val rate = rateStr.toDoubleOrNull() ?: 0.0
                    if (name.isNotBlank() && qty > 0.0 && rate >= 0.0) {
                        onConfirm(name.trim(), unit, qty, rate)
                    }
                },
                enabled = name.isNotBlank() && (qtyStr.toDoubleOrNull() ?: 0.0) > 0.0 && (rateStr.toDoubleOrNull() ?: -1.0) >= 0.0
            ) {
                Text(language.pick("شامل کریں", "Add"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("منسوخ", "Cancel"))
            }
        }
    )
}
