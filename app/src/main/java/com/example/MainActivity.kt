package com.example

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
import com.example.auth.ShopSecurityVault
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutShopAccount
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
            // Gracefully continue in manual mode on devices without NFC
        }
    }

    override fun onPause() {
        try {
            NfcRationCardReader.disableNfcReading(this)
        } catch (_: Throwable) {
            // Ignore on devices without NFC
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
                val app = checkNotNull(this[APPLICATION_KEY])
                ShopViewModel(
                    repository = ShopRepository(app),
                    preferences = preferences,
                    networkMonitor = NetworkMonitor(app),
                    bluetoothPrinterManager = BluetoothPrinterManager(app)
                )
            }
        }
    )

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val productsState by viewModel.productsState.collectAsStateWithLifecycle()
    val customersState by viewModel.customersState.collectAsStateWithLifecycle()
    val ordersState by viewModel.ordersState.collectAsStateWithLifecycle()
    val ledgerState by viewModel.ledgerState.collectAsStateWithLifecycle()
    val movementsState by viewModel.stockMovementsState.collectAsStateWithLifecycle()
    val donationsState by viewModel.donationsState.collectAsStateWithLifecycle()
    val rationItemsState by viewModel.rationItemsState.collectAsStateWithLifecycle()
    val rationCardBillsState by viewModel.rationCardBillsState.collectAsStateWithLifecycle()
    val dailyCashEntriesState by viewModel.dailyCashEntriesState.collectAsStateWithLifecycle()
    val cartItems by viewModel.cartItems.collectAsStateWithLifecycle()
    val selectedCustomer by viewModel.selectedCustomer.collectAsStateWithLifecycle()
    val manualCustomerName by viewModel.manualCustomerName.collectAsStateWithLifecycle()
    val activeReceiptOrder by viewModel.activeReceiptOrder.collectAsStateWithLifecycle()
    val activePaymentReceipt by viewModel.activePaymentReceipt.collectAsStateWithLifecycle()
    val activeRationCardReceipt by viewModel.activeRationCardReceipt.collectAsStateWithLifecycle()
    val scannedNfcCard by viewModel.scannedNfcCard.collectAsStateWithLifecycle()
    val pairedPrinters by viewModel.pairedPrinters.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()

    val language = settings.language
    val products = (productsState as? UiState.Success)?.data ?: emptyList()
    val customers = (customersState as? UiState.Success)?.data ?: emptyList()
    val orders = (ordersState as? UiState.Success)?.data ?: emptyList()
    val ledgerEntries = (ledgerState as? UiState.Success)?.data ?: emptyList()
    val stockMovements = (movementsState as? UiState.Success)?.data ?: emptyList()
    val donations = (donationsState as? UiState.Success)?.data ?: emptyList()
    val rationItems = (rationItemsState as? UiState.Success)?.data ?: emptyList()
    val rationCardBills = (rationCardBillsState as? UiState.Success)?.data ?: emptyList()
    val dailyCashEntries = (dailyCashEntriesState as? UiState.Success)?.data ?: emptyList()

    val todayManualCashNet = remember(dailyCashEntries) {
        val startOfTodayMs = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayManual = dailyCashEntries.filter {
            it.entryDateMs >= startOfTodayMs && isManualRoznamchaEntry(it)
        }
        val inAmt = todayManual.filter { it.entryTypeEnum() == DailyCashType.CASH_IN }.sumOf { it.amount }
        val outAmt = todayManual.filter { it.entryTypeEnum() == DailyCashType.CASH_OUT }.sumOf { it.amount }
        inAmt - outAmt
    }

    val lowStockProducts = remember(products) { products.filter { it.isLowStock() } }

    var currentDestination by rememberSaveable { mutableStateOf(ShopDestination.HOME) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showQrPaymentModal by remember { mutableStateOf(false) }
    var showLowStockReportDialog by remember { mutableStateOf(false) }
    var editingOrder by remember { mutableStateOf<Order?>(null) }

    val isGuestUser = remember { ShopSecurityVault.isGuestMode(context) }
    LaunchedEffect(currentDestination, isGuestUser) {
        if (isGuestUser && (
            currentDestination == ShopDestination.KHATA ||
            currentDestination == ShopDestination.ROZNAMCHA ||
            currentDestination == ShopDestination.RATION_CARD
        )) {
            viewModel.postMessage(
                language.pick(
                    "گیسٹ موڈ میں کھاتہ، روزنامچہ اور راشن کارڈ POS بند ہیں — مکمل فیچرز کے لیے سیٹنگز سے سائن اپ کریں!",
                    "Khata, Cashbook & Ration Card POS are locked in Guest Mode — Sign up from Settings to unlock!"
                )
            )
            currentDestination = ShopDestination.HOME
        }
    }
        // Route real NFC card taps straight to the POS / پی او ایس Card screen
    val incomingNfc by incomingNfcFlow.collectAsStateWithLifecycle()
    LaunchedEffect(incomingNfc?.scannedAtMs) {
        val card = incomingNfc ?: return@LaunchedEffect
        showQrPaymentModal = false
        showBarcodeScanner = false
        viewModel.onNfcCardScanned(card)
        currentDestination = ShopDestination.RATION_CARD
        onConsumeIncomingNfc()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusBanner) {
        val msg = statusBanner
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusBanner()
        }
    }

    BackHandler(enabled = currentDestination != ShopDestination.HOME) {
        currentDestination = ShopDestination.HOME
    }

    val navTabs = listOf(
        ShopDestination.HOME,
        ShopDestination.BILLING,
        ShopDestination.RATION_CARD,
        ShopDestination.STOCK,
        ShopDestination.KHATA
    )

    val isSecondaryScreen = currentDestination == ShopDestination.SETTINGS ||
        currentDestination == ShopDestination.RATION_FUND ||
        currentDestination == ShopDestination.ROZNAMCHA ||
        currentDestination == ShopDestination.CUSTOMER_ORDER

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = EmeraldPrimary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    navigationIcon = {
                        if (isSecondaryScreen) {
                            IconButton(onClick = { currentDestination = ShopDestination.HOME }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = language.pick("واپس", "Back")
                                )
                            }
                        }
                    },
                    title = {
                        val isCloudLinked = !userEmail.isNullOrBlank()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.4.dp, GoldShimmer, CircleShape)
                                    .padding(1.5.dp)
                            ) {
                                ShopLogoBadge(
                                    resId = R.drawable.img_shop_logo,
                                    contentDescription = language.pick(
                                        "احمد بلال کریانہ سٹور لوگو",
                                        "Ahmad Bilal Kiryana Store Logo"
                                    ),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    maxWidthPx = 192
                                )
                            }
                            Column {
                                Text(
                                    text = language.pick(
                                        settings.storeNameUrdu,
                                        settings.storeNameEnglish
                                    ),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCloudLinked && isOnline) Color(0xFF4ADE80)
                                                else if (!isCloudLinked) Color(0xFF86EFAC)
                                                else GoldShimmer
                                            )
                                    )
                                    Icon(
                                        imageVector = when {
                                            isCloudLinked && isOnline -> Icons.Default.CloudDone
                                            isCloudLinked -> Icons.Default.CloudOff
                                            else -> Icons.Default.PhoneAndroid
                                        },
                                        contentDescription = null,
                                        tint = if (isCloudLinked && !isOnline) GoldShimmer else Color(0xFF86EFAC),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = when {
                                            isCloudLinked && isOnline -> language.pick("فون اسٹوریج • کلاؤڈ منسلک", "Local DB • Cloud Ready")
                                            isCloudLinked -> language.pick("آف لائن • فون میں محفوظ", "Offline • Saved on Phone")
                                            else -> language.pick("فون اسٹوریج • آف لائن فعال", "On-Device Storage • Offline Ready")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isCloudLinked && !isOnline) GoldShimmer else Color(0xFFD1FAE5),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showQrPaymentModal = true },
                            modifier = Modifier.testTag("topbar_qr_payment_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = language.pick("QR سکین و پیمنٹ", "QR Scan & Payment"),
                                tint = GoldShimmer
                            )
                        }
                        Surface(
                            color = SaffronSecondary,
                            shape = RoundedCornerShape(50.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .clickable { viewModel.toggleLanguage() }
                                .testTag("topbar_language_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (language == AppLanguage.URDU) "EN" else "اردو",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                currentDestination = if (currentDestination == ShopDestination.SETTINGS) {
                                    ShopDestination.HOME
                                } else {
                                    ShopDestination.SETTINGS
                                }
                            },
                            modifier = Modifier.testTag("topbar_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = language.pick("سیٹنگز", "Settings"),
                                tint = if (currentDestination == ShopDestination.SETTINGS) GoldShimmer else Color.White
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail && !isSecondaryScreen) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        navTabs.forEach { dest ->
                            val selected = currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = dest },
                                modifier = Modifier.testTag("nav_${dest.name.lowercase()}"),
                                icon = {
                                    if (dest == ShopDestination.BILLING && cartItems.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${cartItems.size}") } }) {
                                            Icon(dest.icon, contentDescription = null)
                                        }
                                    } else if (dest == ShopDestination.STOCK && lowStockProducts.isNotEmpty()) {
                                        BadgedBox(badge = { Badge { Text("${lowStockProducts.size}") } }) {
                                            Icon(dest.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(dest.icon, contentDescription = null)
                                    }
                                },
                                label = {
                                    Text(
                                        text = language.pick(dest.urduLabel, dest.englishLabel),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Spacer(Modifier.size(8.dp))
                        navTabs.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = { Icon(dest.icon, contentDescription = null) },
                                label = { Text(language.pick(dest.urduLabel, dest.englishLabel)) }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    if (isBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    val firstError = listOf(productsState, customersState, ordersState)
                        .filterIsInstance<UiState.Error>()
                        .firstOrNull()
                    if (firstError != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = firstError.message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentDestination) {
                            ShopDestination.HOME -> DashboardScreen(
                                language = language,
                                analytics = analytics,
                                recentOrders = orders,
                                lowStockProducts = lowStockProducts,
                                rationCardsServedCount = rationCardBills.size,
                                onGoToBilling = { currentDestination = ShopDestination.BILLING },
                                onOpenBarcodeScanner = {
                                    currentDestination = ShopDestination.BILLING
                                    showBarcodeScanner = true
                                },
                                onGoToStock = { currentDestination = ShopDestination.STOCK },
                                onGoToKhata = { currentDestination = ShopDestination.KHATA },
                                onOpenMaryamRationCard = { currentDestination = ShopDestination.RATION_CARD },
                                onOpenRationFund = { currentDestination = ShopDestination.RATION_FUND },
                                onSyncGujratRates = { viewModel.syncGujratMarketRates() },
                                onOpenReceipt = { viewModel.openReceipt(it) },
                                todayManualCashNet = todayManualCashNet,
                                onOpenRoznamcha = { currentDestination = ShopDestination.ROZNAMCHA },
                                onOpenCustomerOrder = { currentDestination = ShopDestination.CUSTOMER_ORDER },
                                onEditOrder = { editingOrder = it },
                                onOpenLowStockReport = { showLowStockReportDialog = true },
                                settings = settings
                            )

                            ShopDestination.BILLING -> BillingScreen(
                                language = language,
                                products = products,
                                customers = customers,
                                orders = orders,
                                cartItems = cartItems,
                                selectedCustomer = selectedCustomer,
                                manualCustomerName = manualCustomerName,
                                isBusy = isBusy,
                                onSelectCustomer = { viewModel.selectCustomerForBill(it) },
                                onManualCustomerNameChange = { viewModel.updateManualCustomerName(it) },
                                onAddProductToCart = { prod, qty -> viewModel.addProductToCart(prod, qty) },
                                onSetCartItemQuantity = { id, qty -> viewModel.setCartItemQuantity(id, qty) },
                                onClearCart = { viewModel.clearCart() },
                                onCheckout = { method, disc, paid, pending, note ->
                                    viewModel.checkoutCart(method, disc, paid, pending, note)
                                },
                                onOpenScanner = { showBarcodeScanner = true },
                                onOpenReceipt = { viewModel.openReceipt(it) },
                                onCompletePendingOrder = { order, method ->
                                    viewModel.completePendingOrder(order, method)
                                },
                                onCancelPendingOrder = { viewModel.cancelPendingOrder(it) },
                                onEditOrder = { editingOrder = it },
                                onDeleteOrder = { viewModel.deleteOrder(it.id) },
                                onOpenLowStockReport = { showLowStockReportDialog = true }
                            )

                            ShopDestination.RATION_CARD -> MaryamRationCardScreen(
                                language = language,
                                rationItems = rationItems,
                                rationBills = rationCardBills,
                                scannedNfcCard = scannedNfcCard,
                                isBusy = isBusy,
                                onClearScannedNfc = { viewModel.clearScannedNfcCard() },
                                onSaveRationItem = { id, ur, en, unit, price, order ->
                                    viewModel.saveRationCardItem(id, ur, en, unit, price, order)
                                },
                                onDeleteRationItem = { viewModel.deleteRationCardItem(it) },
                                onCheckoutRationBill = { items, uid, cardNo, name, cnic, phone, limit, fee, used, viaNfc, note, onClear ->
                                    viewModel.checkoutRationCardBill(
                                        cartItems = items,
                                        cardUid = uid,
                                        cardNumber = cardNo,
                                        beneficiaryName = name,
                                        cnic = cnic,
                                        phone = phone,
                                        subsidyLimit = limit,
                                        feeDeduction = fee,
                                        alreadyUsedThisMonth = used,
                                        scannedViaNfc = viaNfc,
                                        note = note,
                                        onSuccessClear = onClear
                                    )
                                },
                                onOpenReceipt = { viewModel.openRationCardReceipt(it) },
                                onOpenCharityFund = { currentDestination = ShopDestination.RATION_FUND },
                                lowStockProducts = lowStockProducts,
                                onOpenLowStockReport = { showLowStockReportDialog = true }
                            )

                            ShopDestination.STOCK -> StockScreen(
                                language = language,
                                products = products,
                                movements = stockMovements,
                                    analytics = analytics,
                                defaultMarginPercent = settings.defaultMarginPercent,
                                onSaveProduct = { id, ur, en, cat, unit, cost, sell, margin, auto, stock, low, code ->
                                    viewModel.saveProduct(id, ur, en, cat, unit, cost, sell, margin, auto, stock, low, code)
                                },
                                onDeleteProduct = { viewModel.deleteProduct(it) },
                                onRecordStockMovement = { prod, type, qty, cost, note ->
                                    viewModel.recordStockMovement(prod, type, qty, cost, note)
                                },
                                onSyncGujratMarketRates = { viewModel.syncGujratMarketRates() },
                                onOpenLowStockReport = { showLowStockReportDialog = true }
                            )

                            ShopDestination.ROZNAMCHA -> DailyRoznamchaScreen(
                                language = language,
                                settings = settings,
                                allCashEntries = dailyCashEntries,
                                onSaveEntry = { existingId, type, source, title, amt, refId, note, dateMs ->
                                    viewModel.saveDailyCashEntry(
                                        existingId = existingId,
                                        entryType = type,
                                        paymentSource = source,
                                        categoryOrTitle = title,
                                        amount = amt,
                                        referenceId = refId,
                                        note = note,
                                        entryDateMs = dateMs
                                    )
                                },
                                    onDeleteEntry = { id ->
        viewModel.deleteDailyCashEntry(id)
    },
    totalKhataUdhar = analytics.totalUdharReceivable
                            )

                            ShopDestination.KHATA -> KhataScreen(
                                language = language,
                                customers = customers,
                                ledgerEntries = ledgerEntries,
                                orders = orders,
                                onAddCustomer = { name, phone, addr, opening ->
                                    viewModel.addCustomer(name, phone, addr, opening)
                                },
                                onRecordTransaction = { cust, type, amt, note ->
                                    viewModel.recordCustomerTransaction(cust, type, amt, note)
                                },
                                onOpenEntryReceipt = { entry ->
                                    viewModel.openPaymentReceipt(entry)
                                },
                                onEditOrder = { order ->
                                    editingOrder = order
                                },
                                onUpdateTransaction = { entry, newType, newAmt, newNote ->
                                    viewModel.updateCustomerTransaction(entry, newType, newAmt, newNote)
                                },
                                onDeleteTransaction = { entry ->
                                    viewModel.deleteCustomerTransaction(entry)
                                },
                                onDeleteCustomer = { customerId ->
                                    viewModel.deleteCustomer(customerId)
                                }
                            )

                            ShopDestination.CUSTOMER_ORDER -> CustomerOrderScreen(
                                language = language,
                                settings = settings,
                                products = products,
                                onShowMessage = { viewModel.postMessage(it) }
                            )

                            ShopDestination.RATION_FUND -> RationFundScreen(
                                language = language,
                                donations = donations,
                                onRecordEntry = { type, name, phone, amt, summary, ref, senderAcc, tid, receiptPath ->
                                    viewModel.recordRationDonation(
                                        entryType = type,
                                        personName = name,
                                        phone = phone,
                                        amount = amt,
                                        rationSummary = summary,
                                        paymentRef = ref,
                                        senderAccount = senderAcc,
                                        transactionId = tid,
                                        receiptImagePath = receiptPath
                                    )
                                },
                                onUpdateDonationStatus = { id, status ->
                                    viewModel.updateDonationStatus(id, status)
                                },
                                onShowMessage = { viewModel.postMessage(it) }
                            )

                            ShopDestination.SETTINGS -> SettingsScreen(
                                language = language,
                                settings = settings,
                                isOnline = isOnline,
                                userEmail = userEmail,
                                pairedPrinters = pairedPrinters,
                                onToggleLanguage = { viewModel.toggleLanguage() },
                                onConnectGoogleAccount = {
                                    onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager.value,
                                        onAuthSuccess = {
                                            onGoogleUserUpdated()
                                            viewModel.postMessage(
                                                language.pick(
                                                    "گوگل اکاؤنٹ منسلک ہو گیا! اب آپ کلاؤڈ بیک اپ کر سکتے ہیں۔",
                                                    "Google Account connected! You can now run Cloud Backup."
                                                )
                                            )
                                        },
                                        onAuthError = { _ ->
                                            viewModel.postMessage(
                                                language.pick(
                                                    "اس APK پر گوگل لاگ اِن دستیاب نہیں — آپ کا تمام ڈیٹا فون میں محفوظ ہے۔",
                                                    "Google Sign-In unavailable on this standalone APK — all data remains safely saved on your phone."
                                                )
                                            )
                                        },
                                        scope = scope
                                    )
                                },
                                onBackupToCloudNow = { viewModel.backupLocalToCloud() },
                                onRestoreFromCloudNow = { viewModel.restoreCloudToLocal() },
                                onSyncGujratMarketRates = { viewModel.syncGujratMarketRates() },
                                onUpdateDefaultMargin = { viewModel.updateDefaultMargin(it) },
                                onUpdateShopPhone = { viewModel.updateShopPhone(it) },
                                onSaveStoreProfile = { nameUr, nameEn, addrUr, addrEn, phone, bank, title, accNo, iban, branch ->
                                    viewModel.saveStoreProfile(
                                        storeNameUrdu = nameUr,
                                        storeNameEnglish = nameEn,
                                        addressUrdu = addrUr,
                                        addressEnglish = addrEn,
                                        phone = phone,
                                        bankName = bank,
                                        accountTitle = title,
                                        accountNumber = accNo,
                                        iban = iban,
                                        branchName = branch
                                    )
                                },
                                onRefreshPairedPrinters = { viewModel.refreshPairedPrinters() },
                                onSelectBluetoothPrinter = { viewModel.selectBluetoothPrinter(it) },
                                onTestBluetoothPrint = { viewModel.printBluetoothTestSlip() },
                                onOpenRationFund = { currentDestination = ShopDestination.RATION_FUND },
                                onUpdateShopLocation = { lat, lng, addr ->
                                    viewModel.updateShopGpsLocation(lat, lng, addr)
                                },
                                onSignOut = {
                                    if (Firebase.auth.currentUser != null) {
                                        signOutShopAccount(
                                            credentialManager = credentialManager.value,
                                            scope = scope,
                                            onSignOutComplete = onSignedOut
                                        )
                                    } else {
                                        onSignedOut()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    val receiptOrder = activeReceiptOrder
    if (receiptOrder != null) {
        val matchedCustomerPhone = remember(receiptOrder.customerId, customers) {
            customers.firstOrNull { it.id == receiptOrder.customerId }?.phone.orEmpty()
        }
        AutoReceiptDialog(
            order = receiptOrder,
            language = language,
            settings = settings,
            customerPhone = matchedCustomerPhone,
            hasBluetoothPrinter = settings.selectedPrinterAddress.isNotBlank(),
            onBluetoothPrint = { viewModel.printOrderOnBluetooth(receiptOrder) },
            onEditOrder = { orderToEdit ->
                viewModel.dismissReceipt()
                editingOrder = orderToEdit
            },
            onDismiss = { viewModel.dismissReceipt() }
        )
    }

    val orderBeingEdited = editingOrder
    if (orderBeingEdited != null) {
        BillEditorDialog(
            order = orderBeingEdited,
            products = products,
            language = language,
            isBusy = isBusy,
            onSaveUpdatedOrder = { updatedItems, discount, extraCharges, finalOverride, method, paid, note ->
                viewModel.updateExistingOrder(
                    existingOrder = orderBeingEdited,
                    updatedItems = updatedItems,
                    discount = discount,
                    extraCharges = extraCharges,
                    finalTotalOverride = finalOverride,
                    paymentMethod = method,
                    paidAmountInput = paid,
                    note = note,
                    onUpdated = { editingOrder = null }
                )
            },
            onDismiss = { editingOrder = null }
        )
    }

    val rationReceipt = activeRationCardReceipt
    if (rationReceipt != null) {
        RationCardReceiptDialog(
            bill = rationReceipt,
            language = language,
            settings = settings,
            onBluetoothPrint = { viewModel.printRationCardBillOnBluetooth(rationReceipt) },
            onDismiss = { viewModel.dismissRationCardReceipt() }
        )
    }

    val payReceipt = activePaymentReceipt
    if (payReceipt != null) {
        val matchedCustomerPhone = remember(payReceipt.customerId, customers) {
            customers.firstOrNull { it.id == payReceipt.customerId }?.phone.orEmpty()
        }
        PaymentReceiptDialog(
            entry = payReceipt,
            language = language,
            settings = settings,
            customerPhone = matchedCustomerPhone,
            onBluetoothPrint = { viewModel.printLedgerEntryOnBluetooth(payReceipt) },
            onDismiss = { viewModel.dismissPaymentReceipt() }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            language = language,
            onBarcodeScanned = { code ->
                showBarcodeScanner = false
                viewModel.addProductByBarcode(code)
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    if (showQrPaymentModal) {
        QrScanAndPaymentDialog(
            language = language,
            settings = settings,
            onRecordPaymentReceived = { amountPkr, refNote ->
                viewModel.saveDailyCashEntry(
                    existingId = null,
                    entryType = DailyCashType.CASH_IN,
                    paymentSource = DailyPaymentSource.HBL_QR,
                    categoryOrTitle = language.pick(
                        "بینک QR پیمنٹ وصولی (${settings.bankName})",
                        "Bank QR Payment Received (${settings.bankName})"
                    ),
                    amount = amountPkr,
                    referenceId = refNote.ifBlank { "RAAST-QR" },
                    note = refNote.ifBlank {
                        language.pick("RAAST / بینک QR پیمنٹ", "RAAST / Bank QR Payment")
                    }
                )
            },
            onScannedProductBarcode = { barcode ->
                currentDestination = ShopDestination.BILLING
                viewModel.addProductByBarcode(barcode)
            },
            onScannedCardNumber = { scannedCode ->
                viewModel.onNfcCardScanned(
                    ScannedNfcCard(
                        uidHex = scannedCode.take(24),
                        techSummary = "QR / Camera Scan",
                        ndefText = scannedCode,
                        suggestedCardNumber = scannedCode
                    )
                )
                currentDestination = ShopDestination.RATION_CARD
            },
            onShowMessage = { viewModel.postMessage(it) },
            onDismiss = { showQrPaymentModal = false }
        )
    }

    if (showLowStockReportDialog) {
        LowStockReportDialog(
            language = language,
            settings = settings,
            lowStockProducts = lowStockProducts,
            onUpdateThreshold = { prod, newThreshold ->
                viewModel.saveProduct(
                    existingId = prod.id,
                    nameUrdu = prod.nameUrdu,
                    nameEnglish = prod.nameEnglish,
                    category = prod.categoryEnum(),
                    unit = prod.unit,
                    costPrice = prod.costPrice,
                    sellingPrice = prod.sellingPrice,
                    marginPercent = prod.marginPercent,
                    autoPriceEnabled = prod.autoPriceEnabled,
                    stockQty = prod.stockQty,
                    lowStockThreshold = newThreshold,
                    barcode = prod.barcode
                )
            },
            onQuickRestock = { prod, reorderQty ->
                viewModel.recordStockMovement(
                    product = prod,
                    type = StockMovementType.STOCK_IN,
                    quantity = reorderQty,
                    newUnitCost = prod.costPrice,
                    note = language.pick("کم اسٹاک رپورٹ سے فوری ری اسٹاک", "Quick restock from Low Stock Report")
                )
            },
            onReportGeneratedMessage = { msg -> viewModel.postMessage(msg) },
            onDismiss = { showLowStockReportDialog = false }
        )
    }
}
