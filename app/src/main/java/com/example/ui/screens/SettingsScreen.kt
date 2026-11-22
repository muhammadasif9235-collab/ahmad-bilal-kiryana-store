package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.services.PairedBluetoothPrinter
import com.example.services.ReceiptFormatter
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    language: AppLanguage,
    settings: ShopSettings,
    isOnline: Boolean,
    userEmail: String?,
    pairedPrinters: List<PairedBluetoothPrinter>,
    onToggleLanguage: () -> Unit,
    onSyncCloudNow: () -> Unit,
    onSyncGujratMarketRates: () -> Unit,
    onUpdateDefaultMargin: (Double) -> Unit,
    onUpdateShopPhone: (String) -> Unit,
    onRefreshPairedPrinters: () -> Unit,
    onSelectBluetoothPrinter: (PairedBluetoothPrinter) -> Unit,
    onTestBluetoothPrint: () -> Unit,
    onOpenRationFund: () -> Unit,
    onSignOut: () -> Unit
) {
    var marginInput by remember(settings.defaultMarginPercent) {
        mutableStateOf(settings.defaultMarginPercent.toString())
    }
    var phoneInput by remember(settings.shopPhone) {
        mutableStateOf(settings.shopPhone)
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
            // 1. Language Switcher (Urdu RTL / English LTR)
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

            // 2. Cloud Backup & Offline Mode Card
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
                            Icon(
                                imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (isOnline) ProfitGreen else WarningAmber
                            )
                            Column {
                                Text(
                                    text = language.pick("آف لائن موڈ اور کلاؤڈ بیک اپ", "Offline Mode & Cloud Backup"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isOnline) {
                                        language.pick("آن لائن • گوگل کلاؤڈ سے منسلک", "Online • Connected to Google Cloud")
                                    } else {
                                        language.pick("آف لائن موڈ • ڈیٹا فون میں محفوظ ہو رہا ہے", "Offline Mode • Saving locally on device")
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isOnline) ProfitGreen else WarningAmber
                                )
                            }
                        }
                        if (!userEmail.isNullOrBlank()) {
                            Text(
                                text = "${language.pick("اکاؤنٹ", "Account")}: $userEmail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FilledTonalButton(
                            onClick = onSyncCloudNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_cloud_sync_button")
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(language.pick("ابھی کلاؤڈ سنک / بیک اپ کریں", "Sync & Backup to Cloud Now"))
                        }
                    }
                }
            }

            // 3. Bluetooth Thermal Printer Connectivity
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
                                text = language.pick("بلوٹوتھ تھرمل پرنٹر", "Bluetooth Thermal Printer"),
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
                                    "58mm یا 80mm بلوٹوتھ رسید پرنٹر منسلک کرنے کے لیے نیچے تلاش کریں",
                                    "Search paired 58mm / 80mm ESC/POS Bluetooth thermal printers below"
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
                                Text(language.pick("ٹیسٹ پرنٹ", "Test Print"))
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

            // 4. Gujrat Market Rates & Shop Profile
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
                            text = language.pick("دکان کی تفصیل اور گجرات ریٹ سیٹنگز", "Shop Info & Gujrat Rate Settings"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = phoneInput,
                                onValueChange = { phoneInput = it },
                                label = { Text(language.pick("رسید پر فون نمبر", "Phone on Receipt")) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FilledTonalButton(onClick = { onUpdateShopPhone(phoneInput) }) {
                                Text(language.pick("محفوظ", "Save"))
                            }
                        }

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

            // 5. Ration Donation Fund Shortcut
            item {
                FilledTonalButton(
                    onClick = onOpenRationFund,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VolunteerActivism, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(language.pick("راشن امداد فنڈ اور HBL اکاؤنٹ کھولیں", "Open Ration Charity Fund (HBL Account)"))
                }
            }

            // 6. Sign Out Button
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
                    Text(language.pick("گوگل اکاؤنٹ سے لاگ آؤٹ کریں", "Sign Out of Google Account"))
                }
            }
        }
    }
}
