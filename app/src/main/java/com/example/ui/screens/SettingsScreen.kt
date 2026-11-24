package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ShopSettings
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Switch
import com.example.auth.ShopSecurityVault
import com.example.ui.theme.EmeraldPrimary
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.services.LocationHelper
import com.example.services.PairedBluetoothPrinter
import com.example.services.ReceiptFormatter
import com.example.ui.components.LegalDocumentTab
import com.example.ui.components.LegalDocumentsDialog
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.launch
import java.util.Locale

private val QUICK_PAKISTAN_BANKS = listOf(
    "HBL",
    "Meezan Bank",
    "UBL",
    "MCB",
    "Allied Bank",
    "JazzCash",
    "EasyPaisa",
    "SadaPay",
    "NayaPay"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    language: AppLanguage,
    settings: ShopSettings,
    isOnline: Boolean,
    userEmail: String?,
    pairedPrinters: List<PairedBluetoothPrinter>,
    onToggleLanguage: () -> Unit,
    onConnectGoogleAccount: () -> Unit,
    onBackupToCloudNow: () -> Unit,
    onRestoreFromCloudNow: () -> Unit,
    onSyncGujratMarketRates: () -> Unit,
    onUpdateDefaultMargin: (Double) -> Unit,
    onUpdateShopPhone: (String) -> Unit,
    onSaveStoreProfile: (
        storeNameUrdu: String,
        storeNameEnglish: String,
        addressUrdu: String,
        addressEnglish: String,
        phone: String,
        bankName: String,
        accountTitle: String,
        accountNumber: String,
        iban: String,
        branchName: String
    ) -> Unit,
    onRefreshPairedPrinters: () -> Unit,
    onSelectBluetoothPrinter: (PairedBluetoothPrinter) -> Unit,
    onTestBluetoothPrint: () -> Unit,
    onOpenRationFund: () -> Unit,
    onSignOut: () -> Unit,
    onUpdateShopLocation: (Double, Double, String?) -> Unit = { _, _, _ -> }
) {
    var storeNameUrduInput by remember(settings.storeNameUrdu) { mutableStateOf(settings.storeNameUrdu) }
    var storeNameEnglishInput by remember(settings.storeNameEnglish) { mutableStateOf(settings.storeNameEnglish) }
    var phoneInput by remember(settings.shopPhone) { mutableStateOf(settings.shopPhone) }
    var addressUrduInput by remember(settings.shopAddressUrdu) { mutableStateOf(settings.shopAddressUrdu) }
    var addressEnglishInput by remember(settings.shopAddressEnglish) { mutableStateOf(settings.shopAddressEnglish) }

    var bankNameInput by remember(settings.bankName) { mutableStateOf(settings.bankName) }
    var accountTitleInput by remember(settings.hblAccountTitle) { mutableStateOf(settings.hblAccountTitle) }
    var accountNumberInput by remember(settings.hblAccountNumber) { mutableStateOf(settings.hblAccountNumber) }
    var ibanInput by remember(settings.hblIban) { mutableStateOf(settings.hblIban) }
    var branchInput by remember(settings.hblBranchName) { mutableStateOf(settings.hblBranchName) }

    var marginInput by remember(settings.defaultMarginPercent) {
        mutableStateOf(settings.defaultMarginPercent.toString())
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDetectingShopGps by remember { mutableStateOf(false) }
    var shopGpsError by remember { mutableStateOf<String?>(null) }
    var activeLegalTab by remember { mutableStateOf<LegalDocumentTab?>(null) }

    fun detectAndSaveShopGps() {
        if (isDetectingShopGps) return
        scope.launch {
            isDetectingShopGps = true
            shopGpsError = null
            val result = LocationHelper.detectCurrentLocation(context)
            isDetectingShopGps = false
            result.onSuccess { loc ->
                addressUrduInput = loc.addressLine
                addressEnglishInput = loc.addressLine
                onUpdateShopLocation(loc.latitude, loc.longitude, loc.addressLine)
            }.onFailure { err ->
                shopGpsError = err.localizedMessage ?: language.pick(
                    "GPS لوکیشن حاصل نہ ہو سکی — براہِ کرم موبائل GPS آن کریں۔",
                    "Could not detect shop GPS — please enable device Location."
                )
            }
        }
    }

    val shopLocationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            detectAndSaveShopGps()
        } else {
            shopGpsError = language.pick(
                "GPS لوکیشن کی اجازت نہیں دی گئی۔",
                "Location permission was not granted."
            )
        }
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            onRefreshPairedPrinters()
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
            // 1. Shopkeeper Profile & Personal Sales Bank / RAAST QR Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = language.pick(
                                        "دکان کا پروفائل اور ذاتی سیلز بینک / RAAST QR",
                                        "Shop Profile & Personal Sales Bank / RAAST QR"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = language.pick(
                                        "ہر دکاندار اپنی دکان کا نام، فون، پتہ اور ذاتی بینک / IBAN محفوظ کر سکتا ہے۔",
                                        "Save your own Shop Name, Phone, Address, and Personal Bank / IBAN."
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        OutlinedTextField(
                            value = storeNameUrduInput,
                            onValueChange = { storeNameUrduInput = it },
                            label = { Text(language.pick("دکان کا نام (اردو)", "Shop Name (Urdu)")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_shop_name_urdu_input")
                        )

                        OutlinedTextField(
                            value = storeNameEnglishInput,
                            onValueChange = { storeNameEnglishInput = it },
                            label = { Text(language.pick("دکان کا نام (English / رسید اور QR)", "Shop Name (English / Receipts & QR)")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_shop_name_english_input")
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text(language.pick("دکان کا فون / واٹس ایپ نمبر", "Shop Phone / WhatsApp Number")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_shop_phone_input")
                        )

                        OutlinedTextField(
                            value = addressUrduInput,
                            onValueChange = { addressUrduInput = it },
                            label = { Text(language.pick("دکان کا پتہ (اردو)", "Shop Address (Urdu)")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_shop_address_urdu_input")
                        )

                        OutlinedTextField(
                            value = addressEnglishInput,
                            onValueChange = { addressEnglishInput = it },
                            label = { Text(language.pick("دکان کا پتہ (English)", "Shop Address (English)")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_shop_address_english_input")
                        )

                        // Auto-Detect & Save Shop GPS Location
                        FilledTonalButton(
                            onClick = {
                                if (LocationHelper.hasLocationPermission(context)) {
                                    detectAndSaveShopGps()
                                } else {
                                    shopLocationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            },
                            enabled = !isDetectingShopGps,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_detect_shop_gps_button")
                        ) {
                            if (isDetectingShopGps) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.size(6.dp))
                            Text(
                                language.pick(
                                    "دکان کی GPS لوکیشن خودکار حاصل اور محفوظ کریں",
                                    "Auto-Detect & Save Shop GPS Location"
                                )
                            )
                        }

                        if (settings.shopLatitude != 0.0 || settings.shopLongitude != 0.0) {
                            val mapsUrl = LocationHelper.buildGoogleMapsUrl(settings.shopLatitude, settings.shopLongitude)
                            Surface(
                                color = ProfitGreen.copy(alpha = 0.09f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                            )
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                "Saved GPS: %.6f, %.6f",
                                                settings.shopLatitude,
                                                settings.shopLongitude
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                        Text(
                                            text = mapsUrl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        if (shopGpsError != null) {
                            Text(
                                text = shopGpsError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = UdharCrimson
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = ProfitGreen)
                            Text(
                                text = language.pick(
                                    "ذاتی بینک اکاؤنٹ / RAAST QR (صرف دکان کی سیلز کے لیے)",
                                    "Personal Bank / RAAST QR (Shop Sales ONLY)"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Surface(
                            color = ProfitGreen.copy(alpha = 0.09f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ProfitGreen.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.QrCode2, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(20.dp))
                                Text(
                                    text = language.pick(
                                        "یہ بینک نام اور IBAN صرف آپ کی دکان کی سیلز RAAST QR پیمنٹ کے لیے استعمال ہوگا۔ راشن امداد فنڈ کا مرکزی HBL اکاؤنٹ مستقل اور لاک ہے۔",
                                        "Used ONLY for your shop's sales RAAST QR code. The Ration Charity Fund HBL account remains permanently locked."
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            QUICK_PAKISTAN_BANKS.forEach { bank ->
                                val selected = bankNameInput.equals(bank, ignoreCase = true)
                                FilterChip(
                                    selected = selected,
                                    onClick = { bankNameInput = bank },
                                    label = { Text(bank, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = bankNameInput,
                            onValueChange = { bankNameInput = it },
                            label = { Text(language.pick("ذاتی بینک کا نام (مثلاً HBL / Meezan / JazzCash)", "Personal Bank Name")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_bank_name_input")
                        )

                        OutlinedTextField(
                            value = accountTitleInput,
                            onValueChange = { accountTitleInput = it },
                            label = { Text(language.pick("اکاؤنٹ ہولڈر کا نام (Account Title)", "Account Title")) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_bank_title_input")
                        )

                        OutlinedTextField(
                            value = ibanInput,
                            onValueChange = { ibanInput = it.uppercase().replace(" ", "") },
                            label = { Text(language.pick("ذاتی IBAN نمبر (RAAST QR کے لیے)*", "Personal IBAN (for Sales RAAST QR)*")) },
                            placeholder = { Text("PK00XXXX0000000000000000") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_bank_iban_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = accountNumberInput,
                                onValueChange = { accountNumberInput = it },
                                label = { Text(language.pick("اکاؤنٹ نمبر", "Account #")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_bank_account_input")
                            )
                            OutlinedTextField(
                                value = branchInput,
                                onValueChange = { branchInput = it },
                                label = { Text(language.pick("برانچ / شہر", "Branch")) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_bank_branch_input")
                            )
                        }

                        Button(
                            onClick = {
                                onSaveStoreProfile(
                                    storeNameUrduInput,
                                    storeNameEnglishInput,
                                    addressUrduInput,
                                    addressEnglishInput,
                                    phoneInput,
                                    bankNameInput,
                                    accountTitleInput,
                                    accountNumberInput,
                                    ibanInput,
                                    branchInput
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_save_profile_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(
                                language.pick(
                                    "دکان اور ذاتی بینک پروفائل محفوظ کریں",
                                    "Save Shop & Personal Bank Profile"
                                )
                            )
                        }
                    }
                }
            }
                     
            // 1b. Biometric Fingerprint & 4-Digit PIN App Lock Card
            item {
                val isGuest = remember { ShopSecurityVault.isGuestMode(context) }
                var fingerprintEnabled by remember {
                    mutableStateOf(ShopSecurityVault.isFingerprintLockEnabled(context))
                }
                var backupPinInput by remember {
                    mutableStateOf(ShopSecurityVault.getBackupPin(context))
                }
                var securityNotice by remember { mutableStateOf<String?>(null) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, EmeraldPrimary.copy(alpha = 0.45f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = language.pick(
                                            "فنگر پرنٹ لاک (App Lock)",
                                            "Biometric Fingerprint Lock"
                                        ),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isGuest) {
                                            language.pick(
                                                "گیسٹ موڈ میں بند ہے — مکمل فیچرز کے لیے سائن اپ کریں",
                                                "Locked in Guest Mode — Sign up to unlock"
                                            )
                                        } else {
                                            language.pick(
                                                "آن کرنے پر ہر بار ایپ فنگر پرنٹ سے کھلے گی",
                                                "Require fingerprint scan every time app opens"
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = fingerprintEnabled && !isGuest,
                                enabled = !isGuest,
                                onCheckedChange = { wantEnable ->
                                    if (wantEnable) {
                                        if (ShopSecurityVault.canUseBiometric(context)) {
                                            ShopSecurityVault.launchBiometricPrompt(
                                                context = context,
                                                title = language.pick("فنگر پرنٹ تصدیق", "Verify Fingerprint"),
                                                subtitle = language.pick(
                                                    "ایپ لاک فعال کرنے کے لیے انگلی لگائیں",
                                                    "Touch sensor to enable Fingerprint Lock"
                                                ),
                                                negativeButtonText = language.pick("منسوخ", "Cancel"),
                                                onSuccess = {
                                                    fingerprintEnabled = true
                                                    ShopSecurityVault.setFingerprintLockEnabled(context, true)
                                                    securityNotice = language.pick(
                                                        "فنگر پرنٹ لاک کامیابی سے آن ہو گیا!",
                                                        "Fingerprint Lock enabled successfully!"
                                                    )
                                                },
                                                onError = { err -> securityNotice = err }
                                            )
                                        } else {
                                            fingerprintEnabled = true
                                            ShopSecurityVault.setFingerprintLockEnabled(context, true)
                                            securityNotice = language.pick(
                                                "ایپ لاک آن ہو گیا (نیچے 4 ہندسوں کا PIN بھی محفوظ کر لیں)",
                                                "App Lock enabled (set a 4-digit backup PIN below)"
                                            )
                                        }
                                    } else {
                                        fingerprintEnabled = false
                                        ShopSecurityVault.setFingerprintLockEnabled(context, false)
                                        securityNotice = language.pick(
                                            "فنگر پرنٹ لاک بند کر دیا گیا۔",
                                            "Fingerprint Lock disabled."
                                        )
                                    }
                                }
                            )
                        }

                        if (!isGuest) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = backupPinInput,
                                    onValueChange = { backupPinInput = it.filter { c -> c.isDigit() }.take(4) },
                                    label = { Text(language.pick("4 ہندسوں کا بیک اپ PIN", "4-Digit Backup PIN")) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = {
                                        if (backupPinInput.length == 4) {
                                            ShopSecurityVault.setBackupPin(context, backupPinInput)
                                            securityNotice = language.pick("4 ہندسوں کا PIN محفوظ ہو گیا!", "4-Digit PIN saved!")
                                        } else {
                                            securityNotice = language.pick("پورا 4 ہندسوں کا PIN لکھیں", "Enter all 4 digits")
                                        }
                                    }
                                ) {
                                    Text(language.pick("PIN محفوظ", "Save PIN"))
                                }
                            }
                        }

                        if (securityNotice != null) {
                            Text(
                                text = securityNotice!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = ProfitGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 2. Bluetooth 58mm ESC/POS Thermal Printer Connectivity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = language.pick("58mm بلوٹوتھ تھرمل پرنٹر (ESC/POS)", "58mm Bluetooth Thermal Printer (ESC/POS)"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (settings.selectedPrinterName.isNotBlank()) {
                            Text(
                                text = "${language.pick("منتخب پرنٹر", "Selected Printer")}: ${settings.selectedPrinterName} (${settings.selectedPrinterAddress})",
                                style = MaterialTheme.typography.bodySmall,
                                color = ProfitGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = language.pick(
                                    "بلنگ، کھاتہ اور POS رسیدوں کے لیے 58mm بلوٹوتھ پرنٹر منسلک کریں",
                                    "Connect a paired 58mm ESC/POS Bluetooth thermal printer for Billing, Khata & POS receipts"
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        btPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.BLUETOOTH_CONNECT,
                                                Manifest.permission.BLUETOOTH_SCAN
                                            )
                                        )
                                    } else {
                                        onRefreshPairedPrinters()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_scan_bluetooth_button")
                            ) {
                                Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(language.pick("پرنٹر تلاش کریں", "Find Printers"))
                            }

                            OutlinedButton(
                                onClick = onTestBluetoothPrint,
                                enabled = settings.selectedPrinterAddress.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_test_print_button")
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(language.pick("58mm ٹیسٹ پرنٹ", "58mm Test Print"))
                            }
                        }

                        if (pairedPrinters.isNotEmpty()) {
                            HorizontalDivider()
                            pairedPrinters.forEach { printer ->
                                val isSelected = printer.address == settings.selectedPrinterAddress
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectBluetoothPrinter(printer) }
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(printer.name, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            printer.address,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Language Switcher (Urdu RTL / English LTR)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = language.pick("زبان اور تحریر (اردو / English)", "Language & Direction"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = language.pick(
                                "اردو دائیں سے بائیں (RTL) اور انگلش بائیں سے دائیں (LTR) میں خودکار تبدیل ہو جاتی ہے۔",
                                "Switch seamlessly between Urdu (Right-to-Left) and English (Left-to-Right)."
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onToggleLanguage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_language_toggle_button")
                        ) {
                            Text(
                                if (language == AppLanguage.URDU) "Switch to English (LTR)"
                                else "اردو میں تبدیل کریں (RTL)"
                            )
                        }
                    }
                }
            }

            // 4. Local Phone Storage & Optional One-Tap Google Cloud Backup Card
            item {
                val isGoogleConnected = !userEmail.isNullOrBlank()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (isGoogleConnected && isOnline) Icons.Default.CloudDone
                                else if (isGoogleConnected) Icons.Default.CloudOff
                                else Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = if (isGoogleConnected && isOnline) ProfitGreen
                                else if (!isGoogleConnected) ProfitGreen
                                else WarningAmber
                            )
                            Column {
                                Text(
                                    text = language.pick("فون اسٹوریج اور اختیاری کلاؤڈ بیک اپ", "Local Storage & Optional Cloud Backup"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when {
                                        isGoogleConnected && isOnline -> language.pick(
                                            "فون میں محفوظ • گوگل کلاؤڈ بیک اپ کے لیے تیار",
                                            "Saved on Phone • Ready for Google Cloud Backup"
                                        )
                                        isGoogleConnected -> language.pick(
                                            "آف لائن موڈ • تمام ڈیٹا فون میں محفوظ ہے",
                                            "Offline Mode • All store data saved safely on phone"
                                        )
                                        else -> language.pick(
                                            "لوکل موڈ فعال • تمام اسٹاک، بل اور کھاتہ فون میں محفوظ ہے",
                                            "Local Mode Active • All store, customer & khata data saved on phone"
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isGoogleConnected && !isOnline) WarningAmber else ProfitGreen
                                )
                            }
                        }

                        if (isGoogleConnected) {
                            Text(
                                text = "${language.pick("منسلک گوگل اکاؤنٹ", "Connected Google Account")}: $userEmail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (settings.lastCloudBackupEpochMs > 0L) {
                                Text(
                                    text = "${language.pick("آخری کلاؤڈ بیک اپ", "Last Cloud Backup")}: ${
                                        ReceiptFormatter.formatDateTime(settings.lastCloudBackupEpochMs)
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onBackupToCloudNow,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_cloud_sync_button")
                                ) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(language.pick("کلاؤڈ بیک اپ", "Backup Now"))
                                }
                                OutlinedButton(
                                    onClick = onRestoreFromCloudNow,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("settings_cloud_restore_button")
                                ) {
                                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.size(6.dp))
                                    Text(language.pick("ری اسٹور کریں", "Restore"))
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onConnectGoogleAccount,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("settings_connect_google_button")
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    language.pick(
                                        "گوگل اکاؤنٹ منسلک کریں (اختیاری کلاؤڈ بیک اپ)",
                                        "Connect Google Account (Optional Cloud Backup)"
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 5. Gujrat Market Rates & Auto Margin Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = language.pick("گجرات مارکیٹ ریٹ اور منافع سیٹنگز", "Gujrat Market Rate & Margin Settings"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = marginInput,
                                onValueChange = { marginInput = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text(language.pick("خودکار منافع شرح (%)", "Default Auto Margin (%)")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(
                                onClick = {
                                    marginInput.toDoubleOrNull()?.let(onUpdateDefaultMargin)
                                }
                            ) {
                                Text(language.pick("محفوظ", "Save"))
                            }
                        }

                        if (settings.lastRateSyncEpochMs > 0L) {
                            Text(
                                text = "${language.pick("آخری گجرات ریٹ اپڈیٹ", "Last Gujrat Rate Sync")}: ${
                                    ReceiptFormatter.formatDateTime(settings.lastRateSyncEpochMs)
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = onSyncGujratMarketRates,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(language.pick("گجرات مارکیٹ ریٹس خودکار اپڈیٹ کریں", "Auto-Update Gujrat Market Rates"))
                        }
                    }
                }
            }

            // 6. Locked Central Ration Donation Fund Card (Read-Only)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = ProfitGreen, modifier = Modifier.size(18.dp))
                            Text(
                                text = language.pick(
                                    "راشن امداد فنڈ — مرکزی HBL اکاؤنٹ (مستقل اور لاک)",
                                    "Ration Charity Fund — Central HBL Account (Locked)"
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "HBL • Muhammad Asif • PK08HABB0016857900668003",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilledTonalButton(
                            onClick = onOpenRationFund,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(language.pick("راشن امداد فنڈ اور HBL اکاؤنٹ کھولیں", "Open Ration Charity Fund (HBL Account)"))
                        }
                    }
                }
            }

            // 7. Legal, Privacy Policy (PECA 2016) & Terms (SBP RAAST / Non-Government Disclaimer)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Policy, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = language.pick(
                                        "قانونی معلومات، پرائیویسی اور شرائط",
                                        "Legal, Privacy Policy & Terms"
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = language.pick(
                                        "PECA 2016 • SBP RAAST • غیر سرکاری ڈسکلیمر (مرزا محمد آصف)",
                                        "PECA 2016 • SBP RAAST • Non-Government Disclaimer (Mirza Muhammad Asif)"
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { activeLegalTab = LegalDocumentTab.PRIVACY_POLICY },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_privacy_policy_button")
                            ) {
                                Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    language.pick("پرائیویسی پالیسی", "Privacy Policy"),
                                    maxLines = 1
                                )
                            }
                            OutlinedButton(
                                onClick = { activeLegalTab = LegalDocumentTab.TERMS_AND_CONDITIONS },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_terms_conditions_button")
                            ) {
                                Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(
                                    language.pick("شرائط و ضوابط", "Terms & Rules"),
                                    maxLines = 1
                                )
                            }
                        }

                        Text(
                            text = language.pick(
                                "آپریٹر: مرزا محمد آصف • احمد بلال کریانہ سٹور، محلہ محمد نگر، ٹانڈہ، ضلع گجرات، پنجاب، پاکستان • واٹس ایپ: +923401558166",
                                "Operator: Mirza Muhammad Asif • Ahmad Bilal Karyana Store, Mohalla Muhammad Nagar, Tanda, District Gujrat, Punjab, Pakistan • WhatsApp: +923401558166"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 8. Log Out / Return to Login Screen Button (Local Data Stays Safe)
            item {
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UdharCrimson),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        language.pick(
                            "لاگ آؤٹ کریں (فون کا ڈیٹا محفوظ رہے گا)",
                            "Log Out (Local Phone Data Stays Safe)"
                        )
                    )
                }
            }
        }
    }

    val legalTab = activeLegalTab
    if (legalTab != null) {
        LegalDocumentsDialog(
            initialTab = legalTab,
            language = language,
            onDismiss = { activeLegalTab = null }
        )
    }
}
