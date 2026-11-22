package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
import androidx.compose.runtime.key
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
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.signOutShopAccount
import com.example.data.ShopPreferences
import com.example.data.ShopRepository
import com.example.data.ShopSettings
import com.example.data.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppLanguage
import com.example.i18n.pick
import com.example.services.BarcodeScannerDialog
import com.example.services.BluetoothPrinterManager
import com.example.services.NetworkMonitor
import com.example.services.NfcRationCardReader
import com.example.services.ScannedNfcCard
import com.example.ui.components.AutoReceiptDialog
import com.example.ui.components.PaymentReceiptDialog
import com.example.ui.components.RationCardReceiptDialog
import com.example.ui.screens.BillingScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.KhataScreen
import com.example.ui.screens.MaryamRationCardScreen
import com.example.ui.screens.RationFundScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StockScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.KiryanaTheme
import com.example.ui.theme.SaffronSecondary
import com.example.viewmodel.ShopViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
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
    RATION_CARD("راشن کارڈ", "Rashan", Icons.Default.CreditCard),
    STOCK("اسٹاک", "Stock", Icons.Default.Inventory2),
    KHATA("کھاتہ", "Khata", Icons.Default.AccountBalanceWallet),
    RATION_FUND("امداد فنڈ", "Charity", Icons.Default.VolunteerActivism),
    SETTINGS("سیٹنگز", "Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

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
        NfcRationCardReader.enableForegroundDispatch(this)
    }

    override fun onPause() {
        NfcRationCardReader.disableForegroundDispatch(this)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        val card = NfcRationCardReader.extractCardFromIntent(intent)
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
    val settings by preferences.settingsFlow.collectAsStateWithLifecycle(initialValue = ShopSettings())
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
            val user = currentUser
            if (user == null) {
                AuthScreen(
                    language = language,
                    onToggleLanguage = {
                        scope.launch {
                            val next = if (language == AppLanguage.URDU) AppLanguage.ENGLISH else AppLanguage.URDU
                            preferences.setLanguage(next)
                        }
                    },
                    onAuthSuccess = {
                        currentUser = Firebase.auth.currentUser
                    }
                )
            } else {
                val uid = user.uid
                key(uid) {
                    MainShopShell(
                        currentUserId = uid,
                        userEmail = user.email,
                        preferences = preferences,
                        incomingNfcFlow = incomingNfcFlow,
                        onConsumeIncomingNfc = onConsumeIncomingNfc,
                        onSignedOut = { currentUser = Firebase.auth.currentUser }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShopShell(
    currentUserId: String,
    userEmail: String?,
    preferences: ShopPreferences,
    incomingNfcFlow: StateFlow<ScannedNfcCard?>,
    onConsumeIncomingNfc: () -> Unit,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { lazy { CredentialManager.create(context) } }

    val viewModel: ShopViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                ShopViewModel(
                    repository = ShopRepository(app),
                    preferences = preferences,
                    networkMonitor = NetworkMonitor(app),
                    bluetoothPrinterManager = BluetoothPrinterManager(app),
                    currentUserId = currentUserId
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

    var currentDestination by rememberSaveable { mutableStateOf(ShopDestination.HOME) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    // Route real NFC card taps straight to the Maryam Nawaz Ration Card screen
    val incomingNfc by incomingNfcFlow.collectAsStateWithLifecycle()
    LaunchedEffect(incomingNfc?.scannedAtMs) {
        val card = incomingNfc ?: return@LaunchedEffect
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
        currentDestination == ShopDestination.RATION_FUND

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
                        Column {
                            Text(
                                text = language.pick(
                                    "احمد بلال کریانہ سٹور ٹانڈہ",
                                    "Ahmad Bilal Kiryana, Tanda"
                                ),
                                style = MaterialTheme.typography.titleLarge,
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
                                        .background(if (isOnline) Color(0xFF4ADE80) else GoldShimmer)
                                )
                                Icon(
                                    imageVector = if (isOnline) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isOnline) Color(0xFF86EFAC) else GoldShimmer,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (isOnline) {
                                        language.pick("آن لائن • کلاؤڈ بیک اپ", "Online • Cloud Synced")
                                    } else {
                                        language.pick("آف لائن موڈ • لوکل محفوظ", "Offline Mode • Local")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnline) Color(0xFFD1FAE5) else GoldShimmer
                                )
                            }
                        }
                    },
                    actions = {
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
                                lowStockProducts = products.filter { it.isLowStock() },
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
                                onOpenReceipt = { viewModel.openReceipt(it) }
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
                                onCancelPendingOrder = { viewModel.cancelPendingOrder(it) }
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
                                onCheckoutRationBill = { items, uid, cardNo, name, cnic, phone, limit, used, viaNfc, note, onClear ->
                                    viewModel.checkoutRationCardBill(
                                        items, uid, cardNo, name, cnic, phone, limit, used, viaNfc, note, onClear
                                    )
                                },
                                onOpenReceipt = { viewModel.openRationCardReceipt(it) },
                                onOpenCharityFund = { currentDestination = ShopDestination.RATION_FUND }
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
                                onSyncGujratMarketRates = { viewModel.syncGujratMarketRates() }
                            )

                            ShopDestination.KHATA -> KhataScreen(
                                language = language,
                                customers = customers,
                                ledgerEntries = ledgerEntries,
                                onAddCustomer = { name, phone, addr, opening ->
                                    viewModel.addCustomer(name, phone, addr, opening)
                                },
                                onRecordTransaction = { cust, type, amt, note ->
                                    viewModel.recordCustomerTransaction(cust, type, amt, note)
                                }
                            )

                            ShopDestination.RATION_FUND -> RationFundScreen(
                                language = language,
                                donations = donations,
                                onRecordEntry = { type, name, phone, amt, summary, ref ->
                                    viewModel.recordRationDonation(type, name, phone, amt, summary, ref)
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
                                onSyncCloudNow = { viewModel.syncCloudNow() },
                                onSyncGujratMarketRates = { viewModel.syncGujratMarketRates() },
                                onUpdateDefaultMargin = { viewModel.updateDefaultMargin(it) },
                                onUpdateShopPhone = { viewModel.updateShopPhone(it) },
                                onRefreshPairedPrinters = { viewModel.refreshPairedPrinters() },
                                onSelectBluetoothPrinter = { viewModel.selectBluetoothPrinter(it) },
                                onTestBluetoothPrint = { viewModel.printBluetoothTestSlip() },
                                onOpenRationFund = { currentDestination = ShopDestination.RATION_FUND },
                                onSignOut = {
                                    signOutShopAccount(
                                        credentialManager = credentialManager.value,
                                        scope = scope,
                                        onSignOutComplete = onSignedOut
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Automatic Receipt Modal after every normal bill
    val receiptOrder = activeReceiptOrder
    if (receiptOrder != null) {
        AutoReceiptDialog(
            order = receiptOrder,
            language = language,
            shopPhone = settings.shopPhone,
            hasBluetoothPrinter = settings.selectedPrinterAddress.isNotBlank(),
            onBluetoothPrint = { viewModel.printOrderOnBluetooth(receiptOrder) },
            onDismiss = { viewModel.dismissReceipt() }
        )
    }

    // Automatic Receipt Modal after Maryam Nawaz Ration Card bill
    val rationReceipt = activeRationCardReceipt
    if (rationReceipt != null) {
        RationCardReceiptDialog(
            bill = rationReceipt,
            language = language,
            shopPhone = settings.shopPhone,
            onBluetoothPrint = { viewModel.printRationCardBillOnBluetooth(rationReceipt) },
            onDismiss = { viewModel.dismissRationCardReceipt() }
        )
    }

    // Automatic Payment Receipt after Udhar Khata transaction
    val payReceipt = activePaymentReceipt
    if (payReceipt != null) {
        PaymentReceiptDialog(
            entry = payReceipt,
            language = language,
            onDismiss = { viewModel.dismissPaymentReceipt() }
        )
    }

    // CameraX + ML Kit Barcode Scanner Dialog
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
}
