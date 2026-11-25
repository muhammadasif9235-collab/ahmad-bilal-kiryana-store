Package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.auth.ShopSecurityVault
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.services.LocationHelper
import com.example.services.PairedBluetoothPrinter
import com.example.services.ReceiptFormatter
import com.example.ui.components.LegalDocumentTab
import com.example.ui.components.LegalDocumentsDialog
import com.example.ui.components.ShopLogoBadge
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber
import java.io.File
import java.util.Locale
import kotlinx.coroutines.launch

private val QUICK_PAKISTAN_BANKS = listOf(
    "HBL", "Meezan Bank", "UBL", "MCB", "Allied Bank",
    "JazzCash", "EasyPaisa", "SadaPay", "NayaPay"
)

private const val PROFILE_PHOTO_PREFS = "ahmad_bilal_profile_photo"
private const val KEY_SHOP_PHOTO_PATH = "custom_shop_photo_path"

fun getSavedShopPhotoPath(context: Context): String? =
    context.applicationContext
        .getSharedPreferences(PROFILE_PHOTO_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_SHOP_PHOTO_PATH, null)

private fun copyUriToInternalPhoto(context: Context, uri: Uri): String? {
    return try {
        val destFile = File(context.filesDir, "shop_profile_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        context.applicationContext
            .getSharedPreferences(PROFILE_PHOTO_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SHOP_PHOTO_PATH, destFile.absolutePath)
            .apply()
        destFile.absolutePath
    } catch (_: Exception) {
        null
    }
}

private fun clearSavedShopPhoto(context: Context) {
    context.applicationContext
        .getSharedPreferences(PROFILE_PHOTO_PREFS, Context.MODE_PRIVATE)
        .edit()
        .remove(KEY_SHOP_PHOTO_PATH)
        .apply()
}

@Composable
private fun SettingsAccordionBar(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = EmeraldPrimary,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (expanded) 1.4.dp else 1.dp,
            color = if (expanded) EmeraldPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = iconTint.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = EmeraldPrimary
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider()
                    content()
                }
            }
        }
    }
}

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
    onSyncGujratRates: () -> Unit,
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
    onUpdateShopLocation: (Double, Double, String?) -> Unit = { _, _, _ -> },
    onOpenBusinessSetup: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var expandedBar by remember { mutableStateOf<String?>(null) }
    fun toggleBar(key: String) {
        expandedBar = if (expandedBar == key) null else key
    }

    var customPhotoPath by remember { mutableStateOf(getSavedShopPhotoPath(context)) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            copyUriToInternalPhoto(context, uri)?.let { saved ->
                customPhotoPath = saved
            }
        }
    }

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
        if (granted) detectAndSaveShopGps()
    }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) onRefreshPairedPrinters()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, GoldShimmer),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .border(2.dp, GoldShimmer, CircleShape)
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            ShopLogoBadge(
                                resId = R.drawable.img_shop_logo,
                                contentDescription = settings.storeNameEnglish,
                                customPhotoPath = customPhotoPath,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = language.pick(settings.storeNameUrdu, settings.storeNameEnglish),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldPrimary
                            )
                            Text(
                                text = ShopSecurityVault.getRegisteredEmail(context).ifBlank {
                                    userEmail ?: language.pick("مقامی دکان اکاؤنٹ", "Local Shop Account")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.size(4.dp))
                                    Text(language.pick("تصویر تبدیل کریں", "Change Photo"), style = MaterialTheme.typography.labelSmall)
                                }
                                if (customPhotoPath != null) {
                                    OutlinedButton(
                                        onClick = {
                                            clearSavedShopPhoto(context)
                                            customPhotoPath = null
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(language.pick("لوگو بحال", "Reset Logo"), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                FilledTonalButton(
                    onClick = onOpenBusinessSetup,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = EmeraldPrimary.copy(alpha = 0.12f),
                        contentColor = EmeraldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = language.pick(
                            "کاروبار کی قسم تبدیل کریں / تیار سامان لوڈ کریں (15+ کاروبار)",
                            "Select Business Type / Load Ready-Made Items (15+ Businesses)"
                        ),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            item {
                SettingsAccordionBar(
                    title = language.pick("1. دکان کا پروفائل، فون اور GPS پتہ", "1. Shop Profile, Phone & GPS Address"),
                    subtitle = language.pick("دکان کا نام، واٹس ایپ نمبر اور لوکیشن", "Store name, phone & location"),
                    icon = Icons.Default.Store,
                    expanded = expandedBar == "profile",
                    onToggle = { toggleBar("profile") }
                ) {
                    OutlinedTextField(
                        value = storeNameUrduInput,
                        onValueChange = { storeNameUrduInput = it },
                        label = { Text(language.pick("دکان کا نام (اردو)", "Shop Name (Urdu)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_shop_name_urdu_input")
                    )
                    OutlinedTextField(
                        value = storeNameEnglishInput,
                        onValueChange = { storeNameEnglishInput = it },
                        label = { Text(language.pick("دکان کا نام (English)", "Shop Name (English)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_shop_name_english_input")
                    )
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text(language.pick("فون / واٹس ایپ نمبر", "Phone / WhatsApp")) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_shop_phone_input")
                    )
                    OutlinedTextField(
                        value = addressUrduInput,
                        onValueChange = { addressUrduInput = it },
                        label = { Text(language.pick("پتہ (اردو)", "Address (Urdu)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_shop_address_urdu_input")
                    )
                    OutlinedTextField(
                        value = addressEnglishInput,
                        onValueChange = { addressEnglishInput = it },
                        label = { Text(language.pick("پتہ (English)", "Address (English)")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_shop_address_english_input")
                    )
                    FilledTonalButton(
                        onClick = {
                            if (LocationHelper.hasLocationPermission(context)) detectAndSaveShopGps()
                            else shopLocationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        },
                        enabled = !isDetectingShopGps,
                        modifier = Modifier.fillMaxWidth().testTag("settings_detect_shop_gps_button")
                    ) {
                        if (isDetectingShopGps) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("GPS لوکیشن خودکار حاصل کریں", "Auto-Detect GPS Location"))
                    }
                    if (settings.shopLatitude != 0.0 || settings.shopLongitude != 0.0) {
                        val mapsUrl = LocationHelper.buildGoogleMapsUrl(settings.shopLatitude, settings.shopLongitude)
                        Text(
                            text = String.format(Locale.US, "GPS: %.5f, %.5f", settings.shopLatitude, settings.shopLongitude),
                            style = MaterialTheme.typography.labelSmall,
                            color = ProfitGreen,
                            modifier = Modifier.clickable {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    })
                                }
                            }
                        )
                    }
                    if (shopGpsError != null) {
                        Text(shopGpsError!!, style = MaterialTheme.typography.labelSmall, color = UdharCrimson)
                    }
                    Button(
                        onClick = {
                            onSaveStoreProfile(
                                storeNameUrduInput, storeNameEnglishInput, addressUrduInput, addressEnglishInput,
                                phoneInput, bankNameInput, accountTitleInput, accountNumberInput, ibanInput, branchInput
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("settings_save_profile_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("پروفائل محفوظ کریں", "Save Profile"))
                    }
                }
            }

            item {
                SettingsAccordionBar(
                    title = language.pick("2. ذاتی بینک اکاؤنٹ اور RAAST QR", "2. Personal Bank & Sales RAAST QR"),
                    subtitle = "${settings.bankName} • ${settings.hblAccountTitle}",
                    icon = Icons.Default.AccountBalance,
                    iconTint = ProfitGreen,
                    expanded = expandedBar == "bank",
                    onToggle = { toggleBar("bank") }
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        QUICK_PAKISTAN_BANKS.forEach { bank ->
                            FilterChip(
                                selected = bankNameInput.equals(bank, ignoreCase = true),
                                onClick = { bankNameInput = bank },
                                label = { Text(bank, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = bankNameInput,
                        onValueChange = { bankNameInput = it },
                        label = { Text(language.pick("بینک کا نام", "Bank Name")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_bank_name_input")
                    )
                    OutlinedTextField(
                        value = accountTitleInput,
                        onValueChange = { accountTitleInput = it },
                        label = { Text(language.pick("اکاؤنٹ ہولڈر کا نام", "Account Title")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_bank_title_input")
                    )
                    OutlinedTextField(
                        value = ibanInput,
                        onValueChange = { ibanInput = it.uppercase().replace(" ", "") },
                        label = { Text(language.pick("ذاتی IBAN (RAAST QR)", "Personal IBAN")) },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("settings_bank_iban_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = accountNumberInput,
                            onValueChange = { accountNumberInput = it },
                            label = { Text(language.pick("اکاؤنٹ نمبر", "Account #")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("settings_bank_account_input")
                        )
                        OutlinedTextField(
                            value = branchInput,
                            onValueChange = { branchInput = it },
                            label = { Text(language.pick("برانچ", "Branch")) },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("settings_bank_branch_input")
                        )
                    }
                    Button(
                        onClick = {
                            onSaveStoreProfile(
                                storeNameUrduInput, storeNameEnglishInput, addressUrduInput, addressEnglishInput,
                                phoneInput, bankNameInput, accountTitleInput, accountNumberInput, ibanInput, branchInput
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("بینک اور RAAST QR محفوظ کریں", "Save Bank & RAAST QR"))
                    }
                }
            }

            item {
                val isGuest = remember { ShopSecurityVault.isGuestMode(context) }
                var fingerprintEnabled by remember { mutableStateOf(ShopSecurityVault.isFingerprintLockEnabled(context)) }
                var backupPinInput by remember { mutableStateOf(ShopSecurityVault.getBackupPin(context)) }
                var securityNotice by remember { mutableStateOf<String?>(null) }

                SettingsAccordionBar(
                    title = language.pick("3. فنگر پرنٹ لاک اور 4 ہندسوں کا PIN", "3. Fingerprint Lock & 4-Digit PIN"),
                    subtitle = if (fingerprintEnabled && !isGuest) language.pick("فعال (ON)", "Enabled (ON)")
                    else language.pick("بند (OFF)", "Disabled (OFF)"),
                    icon = Icons.Default.Fingerprint,
                    expanded = expandedBar == "security",
                    onToggle = { toggleBar("security") }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isGuest) language.pick("گیسٹ موڈ میں بند ہے", "Locked in Guest Mode")
                            else language.pick("ایپ کھولتے وقت فنگر پرنٹ مانگیں", "Require fingerprint on app launch"),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = fingerprintEnabled && !isGuest,
                            enabled = !isGuest,
                            onCheckedChange = { wantEnable ->
                                if (wantEnable) {
                                    if (ShopSecurityVault.canUseBiometric(context)) {
                                        ShopSecurityVault.launchBiometricPrompt(
                                            context = context,
                                            title = language.pick("فنگر پرنٹ تصدیق", "Verify Fingerprint"),
                                            subtitle = language.pick("لاک فعال کرنے کے لیے انگلی لگائیں", "Touch sensor to enable lock"),
                                            negativeButtonText = language.pick("منسوخ", "Cancel"),
                                            onSuccess = {
                                                fingerprintEnabled = true
                                                ShopSecurityVault.setFingerprintLockEnabled(context, true)
                                                securityNotice = language.pick("فنگر پرنٹ لاک آن ہو گیا!", "Fingerprint Lock enabled!")
                                            },
                                            onError = { securityNotice = it }
                                        )
                                    } else {
                                        fingerprintEnabled = true
                                        ShopSecurityVault.setFingerprintLockEnabled(context, true)
                                        securityNotice = language.pick("ایپ لاک آن ہو گیا!", "App Lock enabled!")
                                    }
                                } else {
                                    fingerprintEnabled = false
                                    ShopSecurityVault.setFingerprintLockEnabled(context, false)
                                    securityNotice = language.pick("فنگر پرنٹ لاک بند کر دیا گیا۔", "Fingerprint Lock disabled.")
                                }
                            }
                        )
                    }
                    if (!isGuest) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = backupPinInput,
                                onValueChange = { backupPinInput = it.filter { c -> c.isDigit() }.take(4) },
                                label = { Text(language.pick("4 ہندسوں کا PIN", "4-Digit PIN")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(onClick = {
                                if (backupPinInput.length == 4) {
                                    ShopSecurityVault.setBackupPin(context, backupPinInput)
                                    securityNotice = language.pick("PIN محفوظ ہو گیا!", "PIN saved!")
                                }
                            }) {
                                Text(language.pick("محفوظ", "Save"))
                            }
                        }
                    }
                    if (securityNotice != null) {
                        Text(securityNotice!!, style = MaterialTheme.typography.labelSmall, color = ProfitGreen)
                    }
                }
            }

            item {
                SettingsAccordionBar(
                    title = language.pick("4. 58mm بلوٹوتھ تھرمل پرنٹر", "4. 58mm Bluetooth Thermal Printer"),
                    subtitle = settings.selectedPrinterName.ifBlank { language.pick("کوئی پرنٹر منتخب نہیں", "No printer selected") },
                    icon = Icons.Default.Print,
                    expanded = expandedBar == "printer",
                    onToggle = { toggleBar("printer") }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    btPermissionLauncher.launch(
                                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
                                    )
                                } else onRefreshPairedPrinters()
                            },
                            modifier = Modifier.weight(1f).testTag("settings_scan_bluetooth_button")
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("پرنٹر تلاش", "Find Printers"))
                        }
                        OutlinedButton(
                            onClick = onTestBluetoothPrint,
                            enabled = settings.selectedPrinterAddress.isNotBlank(),
                            modifier = Modifier.weight(1f).testTag("settings_test_print_button")
                        ) {
                            Text(language.pick("ٹیسٹ پرنٹ", "Test Print"))
                        }
                    }
                    pairedPrinters.forEach { printer ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectBluetoothPrinter(printer) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${printer.name} (${printer.address})", style = MaterialTheme.typography.bodySmall)
                            if (printer.address == settings.selectedPrinterAddress) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ProfitGreen)
                            }
                        }
                    }
                }
            }

            item {
                val isGoogleConnected = !userEmail.isNullOrBlank()
                SettingsAccordionBar(
                    title = language.pick("5. کلاؤڈ بیک اپ اور گوگل اکاؤنٹ", "5. Cloud Backup & Google Account"),
                    subtitle = userEmail ?: language.pick("لوکل فون اسٹوریج فعال", "Local Phone Storage Active"),
                    icon = if (isGoogleConnected && isOnline) Icons.Default.CloudDone else Icons.Default.PhoneAndroid,
                    iconTint = if (isGoogleConnected && !isOnline) WarningAmber else ProfitGreen,
                    expanded = expandedBar == "cloud",
                    onToggle = { toggleBar("cloud") }
                ) {
                    if (isGoogleConnected) {
                        if (settings.lastCloudBackupEpochMs > 0L) {
                            Text(
                                text = "${language.pick("آخری بیک اپ", "Last Backup")}: ${ReceiptFormatter.formatDateTime(settings.lastCloudBackupEpochMs)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onBackupToCloudNow, modifier = Modifier.weight(1f).testTag("settings_cloud_sync_button")) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(language.pick("بیک اپ", "Backup"))
                            }
                            OutlinedButton(onClick = onRestoreFromCloudNow, modifier = Modifier.weight(1f).testTag("settings_cloud_restore_button")) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text(language.pick("ری اسٹور", "Restore"))
                            }
                        }
                    } else {
                        FilledTonalButton(onClick = onConnectGoogleAccount, modifier = Modifier.fillMaxWidth().testTag("settings_connect_google_button")) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(language.pick("گوگل اکاؤنٹ منسلک کریں", "Connect Google Account"))
                        }
                    }
                }
            }

            item {
                SettingsAccordionBar(
                    title = language.pick("6. مارکیٹ ریٹ، منافع اور زبان", "6. Market Rates, Margin & Language"),
                    subtitle = "${settings.defaultMarginPercent}% • ${if (language == AppLanguage.URDU) "اردو (RTL)" else "English (LTR)"}",
                    icon = Icons.Default.Language,
                    expanded = expandedBar == "market",
                    onToggle = { toggleBar("market") }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = marginInput,
                            onValueChange = { marginInput = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text(language.pick("خودکار منافع (%)", "Auto Margin (%)")) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(onClick = { marginInput.toDoubleOrNull()?.let(onUpdateDefaultMargin) }) {
                            Text(language.pick("محفوظ", "Save"))
                        }
                    }
                    OutlinedButton(onClick = onSyncGujratRates, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("مارکیٹ ریٹس خودکار اپڈیٹ کریں", "Auto-Update Market Rates"))
                    }
                    Button(onClick = onToggleLanguage, modifier = Modifier.fillMaxWidth().testTag("settings_language_toggle_button")) {
                        Text(if (language == AppLanguage.URDU) "Switch to English (LTR)" else "اردو میں تبدیل کریں (RTL)")
                    }
                }
            }

            item {
                SettingsAccordionBar(
                    title = language.pick("7. راشن امداد فنڈ اور قانونی شرائط", "7. Ration Charity Fund & Legal Policies"),
                    subtitle = "PECA 2016 • SBP RAAST • HBL Charity Fund",
                    icon = Icons.Default.Policy,
                    expanded = expandedBar == "legal",
                    onToggle = { toggleBar("legal") }
                ) {
                    FilledTonalButton(onClick = onOpenRationFund, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("راشن امداد فنڈ (HBL) کھولیں", "Open Ration Charity Fund (HBL)"))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = { activeLegalTab = LegalDocumentTab.PRIVACY_POLICY },
                            modifier = Modifier.weight(1f).testTag("settings_privacy_policy_button")
                        ) {
                            Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("پرائیویسی", "Privacy"))
                        }
                        OutlinedButton(
                            onClick = { activeLegalTab = LegalDocumentTab.TERMS_AND_CONDITIONS },
                            modifier = Modifier.weight(1f).testTag("settings_terms_conditions_button")
                        ) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("شرائط و ضوابط", "Terms"))
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = UdharCrimson),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("settings_sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(language.pick("لاگ آؤٹ کریں (فون کا ڈیٹا محفوظ رہے گا)", "Log Out (Phone Data Stays Safe)"))
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
