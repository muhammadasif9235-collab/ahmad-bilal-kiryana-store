package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.data.Product
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.formatQty
import com.example.i18n.pick
import com.example.i18n.unitLabel
import com.example.services.DetectedLocation
import com.example.services.LocationHelper
import com.example.services.ReceiptPrintHelper
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import kotlinx.coroutines.launch
import java.util.Locale

private const val VERIFIED_ORDER_WHATSAPP_NUMBER = "+923401558166"

@Composable
fun CustomerOrderScreen(
    language: AppLanguage,
    settings: ShopSettings,
    products: List<Product>,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var addressText by remember { mutableStateOf("") }
    var customOrderNote by remember { mutableStateOf("") }
    var productSearch by remember { mutableStateOf("") }

    var detectedLocation by remember { mutableStateOf<DetectedLocation?>(null) }
    var isDetectingGps by remember { mutableStateOf(false) }
    var gpsStatusMessage by remember { mutableStateOf<String?>(null) }
    var gpsError by remember { mutableStateOf<String?>(null) }

    val selectedQuantities = remember { mutableStateMapOf<String, Double>() }

    fun runGpsDetection() {
        if (isDetectingGps) return
        scope.launch {
            isDetectingGps = true
            gpsError = null
            gpsStatusMessage = language.pick(
                "GPS لوکیشن حاصل کی جا رہی ہے...",
                "Detecting exact GPS location..."
            )
            val result = LocationHelper.detectCurrentLocation(context)
            isDetectingGps = false
            result.onSuccess { loc ->
                detectedLocation = loc
                if (addressText.isBlank() || addressText.startsWith("GPS:")) {
                    addressText = loc.addressLine
                } else {
                    addressText = loc.addressLine
                }
                gpsStatusMessage = language.pick(
                    "GPS لوکیشن کامیابی سے حاصل ہو گئی!",
                    "Exact GPS location detected!"
                )
            }.onFailure { err ->
                gpsStatusMessage = null
                gpsError = err.localizedMessage ?: language.pick(
                    "لوکیشن حاصل نہ ہو سکی — براہِ کرم موبائل GPS آن کریں یا پتہ خود لکھیں۔",
                    "Could not detect GPS — please enable Location or type your address manually."
                )
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            runGpsDetection()
        } else {
            gpsError = language.pick(
                "لوکیشن کی اجازت نہیں دی گئی۔ آپ اپنا پتہ نیچے خود درج کر سکتے ہیں۔",
                "Location permission denied. You can still enter your delivery address manually below."
            )
        }
    }

    // Automatic GPS detection on opening the screen if permission is already granted
    LaunchedEffect(Unit) {
        if (LocationHelper.hasLocationPermission(context)) {
            runGpsDetection()
        }
    }

    val filteredProducts = remember(products, productSearch) {
        val q = productSearch.trim()
        val list = if (q.isBlank()) products else products.filter {
            it.nameUrdu.contains(q, true) || it.nameEnglish.contains(q, true)
        }
        list.take(25)
    }

    val selectedOrderItems = remember(products, selectedQuantities.toMap()) {
        products.mapNotNull { prod ->
            val qty = selectedQuantities[prod.id] ?: 0.0
            if (qty > 0.0) prod to qty else null
        }
    }
    val estimatedTotal = selectedOrderItems.sumOf { (prod, qty) -> prod.sellingPrice * qty }

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
            // 1. Hero Header Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, GoldShimmer.copy(alpha = 0.7f)),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(EmeraldPrimaryDark, EmeraldPrimary, EmeraldBright)
                                )
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = GoldShimmer,
                            modifier = Modifier.size(34.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = language.pick(
                                    "سامان آرڈر کریں (واٹس ایپ + آٹو GPS)",
                                    "Order Groceries (WhatsApp + Auto-GPS)"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = language.pick(
                                    "تصدیق شدہ واٹس ایپ نمبر $VERIFIED_ORDER_WHATSAPP_NUMBER پر لائیو Google Maps لوکیشن کے ساتھ آرڈر بھیجیں",
                                    "Send verified order with live Google Maps GPS link to $VERIFIED_ORDER_WHATSAPP_NUMBER"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        }
                    }
                }
            }

            // 2. Customer Info & Auto-GPS Location Detection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = language.pick(
                                    "کسٹمر تفصیل اور خودکار GPS لوکیشن",
                                    "Customer Details & Auto-GPS Location"
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                label = { Text(language.pick("آپ کا نام*", "Your Name*")) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("order_customer_name_input")
                            )
                            OutlinedTextField(
                                value = customerPhone,
                                onValueChange = { customerPhone = it },
                                label = { Text(language.pick("فون نمبر*", "Phone Number*")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("order_customer_phone_input")
                            )
                        }

                        // Auto-Detect GPS Button
                        FilledTonalButton(
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    runGpsDetection()
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isDetectingGps,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_auto_detect_gps_button")
                        ) {
                            if (isDetectingGps) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = language.pick(
                                    "موجودہ لوکیشن حاصل کریں (Auto-Detect Location)",
                                    "Auto-Detect Current GPS Location"
                                ),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (detectedLocation != null) {
                            val loc = detectedLocation!!
                            Surface(
                                color = ProfitGreen.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = ProfitGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = language.pick(
                                                "GPS کوآرڈینیٹس اور Google Maps لنک تیار ہے:",
                                                "GPS Coordinates & Google Maps Link Ready:"
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "Lat: %.6f, Lng: %.6f", loc.latitude, loc.longitude),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.clickable {
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(loc.mapsUrl)).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            }
                                        }
                                    ) {
                                        Text(
                                            text = loc.mapsUrl,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        } else if (gpsStatusMessage != null) {
                            Text(
                                text = gpsStatusMessage!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (gpsError != null) {
                            Text(
                                text = gpsError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = UdharCrimson
                            )
                        }

                        OutlinedTextField(
                            value = addressText,
                            onValueChange = { addressText = it },
                            label = {
                                Text(
                                    language.pick(
                                        "مکمل ڈلیوری پتہ / گلی، محلہ (دستی ترمیم بھی ممکن ہے)",
                                        "Full Delivery Address / Street & Area (editable)"
                                    )
                                )
                            },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_address_input")
                        )

                        OutlinedTextField(
                            value = customOrderNote,
                            onValueChange = { customOrderNote = it },
                            label = {
                                Text(
                                    language.pick(
                                        "سامان کی فہرست یا خصوصی ہدایت (مثلاً 10 کلو آٹا، 2 کلو چینی...)",
                                        "Custom Grocery List / Delivery Note (optional)"
                                    )
                                )
                            },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("order_custom_list_input")
                        )
                    }
                }
            }

            // 3. Quick Product Picker from Shop Catalog
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = language.pick(
                            "دکان کے سامان سے منتخب کریں (${selectedOrderItems.size} آئٹمز • اندازاً ${formatPkr(estimatedTotal, language)})",
                            "Pick Items from Catalog (${selectedOrderItems.size} items • Est. ${formatPkr(estimatedTotal, language)})"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = productSearch,
                        onValueChange = { productSearch = it },
                        placeholder = { Text(language.pick("آٹا، چینی، گھی، چاول تلاش کریں...", "Search flour, sugar, ghee, rice...")) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            items(filteredProducts, key = { it.id }) { prod ->
                val qty = selectedQuantities[prod.id] ?: 0.0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (qty > 0.0) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.URDU) prod.nameUrdu else prod.nameEnglish,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatPkr(prod.sellingPrice, language)} / ${unitLabel(prod.unit, language)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (qty > 0.0) {
                                IconButton(
                                    onClick = {
                                        val next = (qty - 1.0).coerceAtLeast(0.0)
                                        if (next <= 0.0) selectedQuantities.remove(prod.id)
                                        else selectedQuantities[prod.id] = next
                                    }
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = language.pick("کم کریں", "Decrease"))
                                }
                                Text(
                                    text = formatQty(qty),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { selectedQuantities[prod.id] = qty + 1.0 }
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = language.pick("شامل کریں", "Add"),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Send Verified WhatsApp Order with Clickable Google Maps Link to +923401558166
            item {
                HorizontalDivider()
                Button(
                    onClick = {
                        if (selectedOrderItems.isEmpty() && customOrderNote.isBlank()) {
                            onShowMessage(
                                language.pick(
                                    "براہِ کرم کم از کم ایک آئٹم منتخب کریں یا سامان کی فہرست لکھیں",
                                    "Please select at least one item or write your grocery list"
                                )
                            )
                            return@Button
                        }
                        val orderMessage = buildCustomerWhatsAppOrderMessage(
                            language = language,
                            settings = settings,
                            customerName = customerName.trim(),
                            customerPhone = customerPhone.trim(),
                            address = addressText.trim(),
                            detectedLocation = detectedLocation,
                            selectedItems = selectedOrderItems,
                            estimatedTotal = estimatedTotal,
                            customNote = customOrderNote.trim()
                        )
                        ReceiptPrintHelper.sendViaWhatsApp(
                            context = context,
                            phone = VERIFIED_ORDER_WHATSAPP_NUMBER,
                            message = orderMessage
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 15.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_send_whatsapp_button")
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = language.pick(
                            "واٹس ایپ پر تصدیق شدہ آرڈر بھیجیں ($VERIFIED_ORDER_WHATSAPP_NUMBER)",
                            "Send Verified WhatsApp Order ($VERIFIED_ORDER_WHATSAPP_NUMBER)"
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

private fun buildCustomerWhatsAppOrderMessage(
    language: AppLanguage,
    settings: ShopSettings,
    customerName: String,
    customerPhone: String,
    address: String,
    detectedLocation: DetectedLocation?,
    selectedItems: List<Pair<Product, Double>>,
    estimatedTotal: Double,
    customNote: String
): String {
    val sb = StringBuilder()
    val shopName = if (language == AppLanguage.URDU) settings.storeNameUrdu else settings.storeNameEnglish
    if (language == AppLanguage.URDU) {
        sb.appendLine("============================")
        sb.appendLine("نیا گھریلو سامان آرڈر (Verified GPS)")
        sb.appendLine(shopName)
        sb.appendLine("============================")
        if (customerName.isNotBlank()) sb.appendLine("کسٹمر کا نام: $customerName")
        if (customerPhone.isNotBlank()) sb.appendLine("موبائل نمبر: $customerPhone")
        if (address.isNotBlank()) sb.appendLine("ڈلیوری پتہ: $address")
        if (detectedLocation != null) {
            sb.appendLine(
                String.format(
                    Locale.US,
                    "GPS کوآرڈینیٹس: %.6f, %.6f",
                    detectedLocation.latitude,
                    detectedLocation.longitude
                )
            )
            sb.appendLine("گوگل میپس لوکیشن لنک:")
            sb.appendLine(detectedLocation.mapsUrl)
        }
        sb.appendLine("----------------------------")
        if (selectedItems.isNotEmpty()) {
            sb.appendLine("منتخب سامان:")
            selectedItems.forEachIndexed { idx, (prod, qty) ->
                val lineTotal = prod.sellingPrice * qty
                sb.appendLine(
                    "${idx + 1}. ${prod.nameUrdu} (${formatQty(qty)} ${unitLabel(prod.unit, AppLanguage.URDU)}) = ${formatPkr(lineTotal, AppLanguage.URDU)}"
                )
            }
            sb.appendLine("اندازاً کل رقم: ${formatPkr(estimatedTotal, AppLanguage.URDU)}")
        }
        if (customNote.isNotBlank()) {
            sb.appendLine("اضافی سامان / نوٹ:")
            sb.appendLine(customNote)
        }
        sb.appendLine("============================")
        sb.append("براہِ کرم آرڈر کی تصدیق فرمائیں۔ شکریہ!")
    } else {
        sb.appendLine("============================")
        sb.appendLine("NEW GROCERY ORDER (Verified GPS)")
        sb.appendLine(shopName)
        sb.appendLine("============================")
        if (customerName.isNotBlank()) sb.appendLine("Customer: $customerName")
        if (customerPhone.isNotBlank()) sb.appendLine("Phone: $customerPhone")
        if (address.isNotBlank()) sb.appendLine("Delivery Address: $address")
        if (detectedLocation != null) {
            sb.appendLine(
                String.format(
                    Locale.US,
                    "GPS Coordinates: %.6f, %.6f",
                    detectedLocation.latitude,
                    detectedLocation.longitude
                )
            )
            sb.appendLine("Google Maps Location Link:")
            sb.appendLine(detectedLocation.mapsUrl)
        }
        sb.appendLine("----------------------------")
        if (selectedItems.isNotEmpty()) {
            sb.appendLine("Selected Items:")
            selectedItems.forEachIndexed { idx, (prod, qty) ->
                val lineTotal = prod.sellingPrice * qty
                sb.appendLine(
                    "${idx + 1}. ${prod.nameEnglish} (${formatQty(qty)} ${unitLabel(prod.unit, AppLanguage.ENGLISH)}) = ${formatPkr(lineTotal, AppLanguage.ENGLISH)}"
                )
            }
            sb.appendLine("Estimated Total: ${formatPkr(estimatedTotal, AppLanguage.ENGLISH)}")
        }
        if (customNote.isNotBlank()) {
            sb.appendLine("Additional Items / Note:")
            sb.appendLine(customNote)
        }
        sb.appendLine("============================")
        sb.append("Please confirm my order. Thank you!")
    }
    return sb.toString()
}
