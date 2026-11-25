package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.FingerprintAppLockScreen
import com.example.auth.InstallSignUpSheet
import com.example.auth.ShopSecurityVault
import com.example.auth.UserInstallTracker
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutShopAccount
import com.example.data.BusinessType
import com.example.data.DailyCashType
import com.example.data.DailyPaymentSource
import com.example.data.Order
import com.example.data.ShopPreferences
import com.example.data.ShopRepository
import com.example.data.ShopSettings
import com.example.data.StockMovementType
import com.example.data.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppLanguage
import com.example.i18n.pick
import com.example.services.BarcodeScannerDialog
import com.example.services.BluetoothPrinterManager
import com.example.services.NetworkMonitor
import com.example.services.NfcRationCardReader
import com.example.services.QrScanAndPaymentDialog
import com.example.services.ScannedNfcCard
import com.example.ui.components.AutoReceiptDialog
import com.example.ui.components.BillEditorDialog
import com.example.ui.components.LowStockReportDialog
import com.example.ui.components.PaymentReceiptDialog
import com.example.ui.components.RationCardReceiptDialog
import com.example.ui.components.ShopLogoBadge
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.BusinessSetupScreen
import com.example.ui.screens.CustomerOrderScreen
import com.example.ui.screens.DailyRoznamchaScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KhataScreen
import com.example.ui.screens.MaryamRationCardScreen
import com.example.ui.screens.RationFundScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StockScreen
import com.example.ui.screens.isManualRoznamchaEntry
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.KiryanaTheme
import com.example.ui.theme.SaffronSecondary
import com.example.viewmodel.ShopViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ShopDestination(
    val urduLabel: String,
    val englishLabel: String,
    val icon: ImageVector
) {
    HOME("ہوم", "Home", Icons.Default.Dashboard),
    BILLING("بلنگ", "Bill", Icons.Default.ReceiptLong),
    RATION_CARD("پی او ایس", "POS", Icons.Default.CreditCard),
    STOCK("اسٹاک", "Stock", Icons.Default.Inventory2),
    KHATA("کھاتہ", "Khata", Icons.Default.AccountBalanceWallet),
    ROZNAMCHA("روزنامچہ", "Cashbook", Icons.Default.MenuBook),
    CUSTOMER_ORDER("سامان آرڈر", "Order", Icons.Default.LocalShipping),
    RATION_FUND("امداد فنڈ", "Charity", Icons.Default.VolunteerActivism),
    SETTINGS("سیٹنگز", "Settings", Icons.Default.Settings)
}

class MainActivity : FragmentActivity() {

    private val _incomingNfcCard = MutableStateFlow<ScannedNfcCard?>(null)
    val incomingNfcCard: StateFlow<ScannedNfcCard?> = _incomingNfcCard.asStateFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleNfcIntent(intent)
        setContent {
            AppRoot(
                incomingNfcFlow = incomingNfcCard,
                onConsumeIncomingNfc = { _incomingNfcCard.value = null }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            NfcRationCardReader.enableNfcReading(this) { card ->
                _incomingNfcCard.value = card
            }
        } catch (_: Throwable) {
        }
    }

    override fun onPause() {
        try {
            NfcRationCardReader.disableNfcReading(this)
        } catch (_: Throwable) {
        }
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        val card = try {
            NfcRationCardReader.extractCardFromIntent(this, intent)
        } catch (_: Throwable) {
            null
        }
        if (card != null) {
            _incomingNfcCard.value = card
        }
    }
}

private const val BUSINESS_PREFS = "ahmad_bilal_business_setup"
private const val KEY_BUSINESS_DONE = "business_setup_completed_v1"
private const val KEY_BUSINESS_CODE = "selected_business_code"

@Composable
fun AppRoot(
    incomingNfcFlow: StateFlow<ScannedNfcCard?>,
    onConsumeIncomingNfc: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { ShopPreferences(context.applicationContext) }
    val loadedSettings by preferences.settingsFlow.collectAsStateWithLifecycle(initialValue = null)
    val settings = loadedSettings ?: ShopSettings()
    val language = settings.language

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    KiryanaTheme(language = language) {
        CompositionLocalProvider(LocalAppLanguage provides language) {
            if (loadedSettings == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(EmeraldPrimary)
                )
            } else {
                val stayLoggedIn = remember { ShopSecurityVault.isStayLoggedIn(context) }
                var sessionUnlocked by rememberSaveable { mutableStateOf(false) }
                var biometricUnlocked by rememberSaveable {
                    mutableStateOf(!ShopSecurityVault.isFingerprintLockEnabled(context))
                }
                val isGuest = ShopSecurityVault.isGuestMode(context)
                val effectivelyLoggedIn =
                    (stayLoggedIn && (settings.localOwnerLoggedIn || currentUser != null)) || sessionUnlocked

                if (!effectivelyLoggedIn) {
                    AuthScreen(
                        language = language,
                        onToggleLanguage = {
                            scope.launch {
                                val next = if (language == AppLanguage.URDU) AppLanguage.ENGLISH else AppLanguage.URDU
                                preferences.setLanguage(next)
                            }
                        },
                        onLocalOwnerLogin = {
                            sessionUnlocked = true
                            biometricUnlocked = true
                            scope.launch { preferences.setLocalOwnerLoggedIn(true) }
                        },
                        onGoogleAuthSuccess = {
                            currentUser = Firebase.auth.currentUser
                            sessionUnlocked = true
                            biometricUnlocked = true
                            scope.launch { preferences.setLocalOwnerLoggedIn(true) }
                        },
                        onGuestLogin = {
                            sessionUnlocked = true
                            biometricUnlocked = true
                            scope.launch { preferences.setLocalOwnerLoggedIn(true) }
                        }
                    )
                } else if (!isGuest && ShopSecurityVault.isFingerprintLockEnabled(context) && !biometricUnlocked) {
                    FingerprintAppLockScreen(
                        language = language,
                        shopTitle = language.pick(settings.storeNameUrdu, settings.storeNameEnglish),
                        onUnlocked = { biometricUnlocked = true },
                        onSwitchToLoginScreen = {
                            sessionUnlocked = false
                            scope.launch { preferences.setLocalOwnerLoggedIn(false) }
                        }
                    )
                } else {
                    MainShopShell(
                        userEmail = currentUser?.email,
                        preferences = preferences,
                        incomingNfcFlow = incomingNfcFlow,
                        onConsumeIncomingNfc = onConsumeIncomingNfc,
                        onGoogleUserUpdated = { currentUser = Firebase.auth.currentUser },
                        onSignedOut = {
                            currentUser = Firebase.auth.currentUser
                            sessionUnlocked = false
                            ShopSecurityVault.setGuestMode(context, false)
                            scope.launch { preferences.setLocalOwnerLoggedIn(false) }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShopShell(
    userEmail: String?,
    preferences: ShopPreferences,
    incomingNfcFlow: StateFlow<ScannedNfcCard?>,
    onConsumeIncomingNfc: () -> Unit,
    onGoogleUserUpdated: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { lazy { CredentialManager.create(context) } }

    val viewModel: ShopViewModel = viewModel(
        key = "ahmad_bilal_local_shop_vm",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY
