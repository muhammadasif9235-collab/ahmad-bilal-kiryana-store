package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CartItem
import com.example.data.Customer
import com.example.data.DonationEntryType
import com.example.data.LedgerEntry
import com.example.data.LedgerEntryType
import com.example.data.Order
import com.example.data.OrderStatus
import com.example.data.PaymentMethod
import com.example.data.Product
import com.example.data.ProductCategory
import com.example.data.RationCardBill
import com.example.data.RationCardItem
import com.example.data.RationCartItem
import com.example.data.RationDonation
import com.example.data.ShopPreferences
import com.example.data.ShopRepository
import com.example.data.ShopSettings
import com.example.data.StockMovement
import com.example.data.StockMovementType
import com.example.data.UiState
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.services.BluetoothPrinterManager
import com.example.services.NetworkMonitor
import com.example.services.PairedBluetoothPrinter
import com.example.services.ScannedNfcCard
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class ShopAnalytics(
    val todaySales: Double = 0.0,
    val todayProfit: Double = 0.0,
    val totalSalesAllTime: Double = 0.0,
    val totalProfitAllTime: Double = 0.0,
    val totalUdharReceivable: Double = 0.0,
    val totalInventoryValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val pendingOrderCount: Int = 0,
    val dailySalesLast7Days: List<Pair<String, Double>> = emptyList()
)

class ShopViewModel(
    private val repository: ShopRepository,
    private val preferences: ShopPreferences,
    private val networkMonitor: NetworkMonitor,
    private val bluetoothPrinterManager: BluetoothPrinterManager
) : ViewModel() {

    private companion object {
        const val TAG = "ShopViewModel"
        const val STOP_TIMEOUT_MS = 5000L
    }

    // --- SETTINGS & CONNECTIVITY ---
    val settings: StateFlow<ShopSettings> = preferences.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ShopSettings())

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnlineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), networkMonitor.isCurrentlyOnline())

    // --- LOCAL-FIRST ROOM REACTIVE STREAMS (Always work offline & on standalone APKs) ---
    val productsState: StateFlow<UiState<List<Product>>> =
        repository.observeProducts().toUiState("products")

    val customersState: StateFlow<UiState<List<Customer>>> =
        repository.observeCustomers().toUiState("customers")

    val ordersState: StateFlow<UiState<List<Order>>> =
        repository.observeOrders().toUiState("orders")

    val ledgerState: StateFlow<UiState<List<LedgerEntry>>> =
        repository.observeLedgerEntries().toUiState("ledgerEntries")

    val stockMovementsState: StateFlow<UiState<List<StockMovement>>> =
        repository.observeStockMovements().toUiState("stockMovements")

    val donationsState: StateFlow<UiState<List<RationDonation>>> =
        repository.observeDonations().toUiState("donations")

    val rationItemsState: StateFlow<UiState<List<RationCardItem>>> =
        repository.observeRationCardItems().toUiState("rationItems")

    val rationCardBillsState: StateFlow<UiState<List<RationCardBill>>> =
        repository.observeRationCardBills().toUiState("rationCardBills")

    // Compute dashboard analytics off the Main Thread so frame rendering never stalls
    val analytics: StateFlow<ShopAnalytics> = combine(
        productsState,
        customersState,
        ordersState
    ) { prodState, custState, ordState ->
        val products = (prodState as? UiState.Success)?.data ?: emptyList()
        val customers = (custState as? UiState.Success)?.data ?: emptyList()
        val orders = (ordState as? UiState.Success)?.data ?: emptyList()
        computeAnalytics(products, customers, orders)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ShopAnalytics())

    private fun <T> Flow<T>.toUiState(label: String): StateFlow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing $label", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load $label"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = UiState.Loading
            )

    // --- BILLING CART & RECEIPT STATE ---
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _manualCustomerName = MutableStateFlow("")
    val manualCustomerName: StateFlow<String> = _manualCustomerName.asStateFlow()

    private val _activeReceiptOrder = MutableStateFlow<Order?>(null)
    val activeReceiptOrder: StateFlow<Order?> = _activeReceiptOrder.asStateFlow()

    private val _activePaymentReceipt = MutableStateFlow<LedgerEntry?>(null)
    val activePaymentReceipt: StateFlow<LedgerEntry?> = _activePaymentReceipt.asStateFlow()

    private val _activeRationCardReceipt = MutableStateFlow<RationCardBill?>(null)
    val activeRationCardReceipt: StateFlow<RationCardBill?> = _activeRationCardReceipt.asStateFlow()

    private val _scannedNfcCard = MutableStateFlow<ScannedNfcCard?>(null)
    val scannedNfcCard: StateFlow<ScannedNfcCard?> = _scannedNfcCard.asStateFlow()

    private val _pairedPrinters = MutableStateFlow<List<PairedBluetoothPrinter>>(emptyList())
    val pairedPrinters: StateFlow<List<PairedBluetoothPrinter>> = _pairedPrinters.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            delay(350)
            repository.seedGujratCatalogIfEmpty()
            repository.seedMaryamRationItemsIfEmpty()
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun postMessage(message: String) {
        _statusBanner.value = message
    }

    private fun showMessage(urdu: String, english: String) {
        _statusBanner.value = settings.value.language.pick(urdu, english)
    }

    // --- LANGUAGE & SETTINGS ---
    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (settings.value.language == AppLanguage.URDU) AppLanguage.ENGLISH else AppLanguage.URDU
            preferences.setLanguage(next)
        }
    }

    fun updateDefaultMargin(margin: Double) {
        viewModelScope.launch {
            preferences.setDefaultMargin(margin)
            showMessage("منافع کی شرح محفوظ ہو گئی", "Default profit margin saved")
        }
    }

    fun updateShopPhone(phone: String) {
        viewModelScope.launch {
            preferences.setShopPhone(phone)
            showMessage("دکان کا فون نمبر محفوظ ہو گیا", "Shop phone number saved")
        }
    }

    // --- BILLING CART OPERATIONS ---
    fun selectCustomerForBill(customer: Customer?) {
        _selectedCustomer.value = customer
        if (customer != null) {
            _manualCustomerName.value = customer.name
        }
    }

    fun updateManualCustomerName(name: String) {
        _manualCustomerName.value = name
        if (_selectedCustomer.value?.name != name) {
            _selectedCustomer.value = null
        }
    }

    fun addProductToCart(product: Product, qtyDelta: Double = 1.0) {
        _cartItems.update { current ->
            val idx = current.indexOfFirst { it.product.id == product.id }
            if (idx >= 0) {
                val updated = current.toMutableList()
                val existing = updated[idx]
                updated[idx] = existing.copy(quantity = (existing.quantity + qtyDelta).coerceAtLeast(0.25))
                updated
            } else {
                current + CartItem(product = product, quantity = qtyDelta.coerceAtLeast(0.25))
            }
        }
    }

    fun setCartItemQuantity(productId: String, newQty: Double) {
        _cartItems.update { current ->
            if (newQty <= 0.0) {
                current.filterNot { it.product.id == productId }
            } else {
                current.map { if (it.product.id == productId) it.copy(quantity = newQty) else it }
            }
        }
    }

    fun setCartItemCustomPrice(productId: String, customPrice: Double) {
        _cartItems.update { current ->
            current.map {
                if (it.product.id == productId) it.copy(customUnitPrice = customPrice.coerceAtLeast(0.0)) else it
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
        _selectedCustomer.value = null
        _manualCustomerName.value = ""
    }

    fun addProductByBarcode(barcode: String): Boolean {
        val products = (productsState.value as? UiState.Success)?.data ?: return false
        val matched = products.firstOrNull {
            it.barcode.equals(barcode.trim(), ignoreCase = true)
        }
        return if (matched != null) {
            addProductToCart(matched, 1.0)
            showMessage(
                "${matched.nameUrdu} بل میں شامل ہو گیا",
                "Added ${matched.nameEnglish} to bill"
            )
            true
        } else {
            showMessage(
                "بارکوڈ ($barcode) کسی آئٹم سے میچ نہیں ہوا",
                "No item matched barcode: $barcode"
            )
            false
        }
    }

    fun checkoutCart(
        paymentMethod: PaymentMethod,
        discount: Double,
        paidAmount: Double,
        saveAsPendingOrder: Boolean,
        note: String
    ) {
        val items = _cartItems.value
        if (items.isEmpty()) {
            showMessage("بل میں کم از کم ایک آئٹم شامل کریں", "Add at least one item to the bill")
            return
        }
        if (!saveAsPendingOrder && (paymentMethod == PaymentMethod.UDHAR || paymentMethod == PaymentMethod.PARTIAL) && _selectedCustomer.value == null) {
            showMessage(
                "ادھار بل کے لیے کسٹمر لسٹ سے کسٹمر منتخب کریں",
                "Select a customer from Udhar Khata for credit billing"
            )
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createOrderOrBill(
                cartItems = items,
                customer = _selectedCustomer.value,
                customerNameInput = _manualCustomerName.value,
                paymentMethod = paymentMethod,
                discount = discount,
                paidAmountInput = paidAmount,
                saveAsPendingOrder = saveAsPendingOrder,
                note = note
            )
            _isBusy.value = false
            result.onSuccess { createdOrder ->
                clearCart()
                _activeReceiptOrder.value = createdOrder
                if (saveAsPendingOrder) {
                    showMessage("آرڈر محفوظ ہو گیا (#${createdOrder.billNumber})", "Order saved (#${createdOrder.billNumber})")
                } else {
                    showMessage("بل بن گیا اور خودکار رسید تیار ہے", "Bill completed & receipt generated")
                }
            }.onFailure { err ->
                showMessage("بل محفوظ نہ ہو سکا: ${err.localizedMessage}", "Failed to save bill: ${err.localizedMessage}")
            }
        }
    }

    fun completePendingOrder(order: Order, paymentMethod: PaymentMethod) {
        val products = (productsState.value as? UiState.Success)?.data ?: emptyList()
        val customers = (customersState.value as? UiState.Success)?.data ?: emptyList()
        val customer = customers.firstOrNull { it.id == order.customerId }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.completePendingOrder(order, products, customer, paymentMethod)
            _isBusy.value = false
            result.onSuccess { completed ->
                _activeReceiptOrder.value = completed
                showMessage("آرڈر مکمل ہو گیا اور رسید تیار ہے", "Order completed & receipt generated")
            }.onFailure { err ->
                showMessage("آرڈر مکمل نہ ہو سکا: ${err.localizedMessage}", "Failed: ${err.localizedMessage}")
            }
        }
    }

    fun cancelPendingOrder(orderId: String) {
        viewModelScope.launch {
            repository.cancelPendingOrder(orderId).onSuccess {
                showMessage("آرڈر منسوخ کر دیا گیا", "Order cancelled")
            }
        }
    }

    fun openReceipt(order: Order) {
        _activeReceiptOrder.value = order
    }

    fun dismissReceipt() {
        _activeReceiptOrder.value = null
    }

    fun dismissPaymentReceipt() {
        _activePaymentReceipt.value = null
    }

    // --- PRODUCT & STOCK OPERATIONS ---
    fun saveProduct(
        existingId: String?,
        nameUrdu: String,
        nameEnglish: String,
        category: ProductCategory,
        unit: String,
        costPrice: Double,
        sellingPrice: Double,
        marginPercent: Double,
        autoPriceEnabled: Boolean,
        stockQty: Double,
        lowStockThreshold: Double,
        barcode: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveProduct(
                existingId, nameUrdu, nameEnglish, category, unit,
                costPrice, sellingPrice, marginPercent, autoPriceEnabled,
                stockQty, lowStockThreshold, barcode
            )
            _isBusy.value = false
            result.onSuccess {
                showMessage("آئٹم کامیابی سے محفوظ ہو گیا", "Product saved successfully")
            }.onFailure { err ->
                showMessage("آئٹم محفوظ نہ ہو سکا: ${err.localizedMessage}", "Save failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId).onSuccess {
                showMessage("آئٹم حذف کر دیا گیا", "Product deleted")
            }
        }
    }

    fun syncGujratMarketRates() {
        val products = (productsState.value as? UiState.Success)?.data ?: emptyList()
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.applyGujratMarketRates(products, settings.value.defaultMarginPercent)
            _isBusy.value = false
            result.onSuccess { count ->
                preferences.setLastRateSyncTime(System.currentTimeMillis())
                showMessage(
                    "ضلع گجرات مارکیٹ کے مطابق $count اشیاء کی قیمتیں خودکار اپڈیٹ ہو گئیں",
                    "Auto-updated $count item prices to Gujrat market rates"
                )
            }.onFailure { err ->
                showMessage("قیمتیں اپڈیٹ نہ ہو سکیں: ${err.localizedMessage}", "Update failed: ${err.localizedMessage}")
            }
        }
    }

    fun recordStockMovement(
        product: Product,
        type: StockMovementType,
        quantity: Double,
        newUnitCost: Double,
        note: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordStockMovement(product, type, quantity, newUnitCost, note)
            _isBusy.value = false
            result.onSuccess {
                showMessage("اسٹاک ریکارڈ اپڈیٹ ہو گیا", "Stock movement recorded")
            }.onFailure { err ->
                showMessage("اسٹاک اپڈیٹ نہ ہو سکا: ${err.localizedMessage}", "Stock update failed: ${err.localizedMessage}")
            }
        }
    }

    // --- CUSTOMER UDHAR KHATA OPERATIONS ---
    fun addCustomer(name: String, phone: String, address: String, openingUdhar: Double) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.addCustomer(name, phone, address, openingUdhar)
            _isBusy.value = false
            result.onSuccess {
                showMessage("نیا کسٹمر کھاتہ شامل ہو گیا", "Customer added to Udhar Khata")
            }.onFailure { err ->
                showMessage("کسٹمر محفوظ نہ ہو سکا: ${err.localizedMessage}", "Failed: ${err.localizedMessage}")
            }
        }
    }

    fun recordCustomerTransaction(
        customer: Customer,
        entryType: LedgerEntryType,
        amount: Double,
        note: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordLedgerTransaction(customer, entryType, amount, note)
            _isBusy.value = false
            result.onSuccess { entry ->
                _activePaymentReceipt.value = entry
                val msgUr = if (entryType == LedgerEntryType.CREDIT) "وصولی درج ہو گئی" else "ادھار درج ہو گیا"
                val msgEn = if (entryType == LedgerEntryType.CREDIT) "Payment received" else "Udhar recorded"
                showMessage(msgUr, msgEn)
            }.onFailure { err ->
                showMessage("اندراج نہ ہو سکا: ${err.localizedMessage}", "Failed: ${err.localizedMessage}")
            }
        }
    }

    // --- RATION CHARITY FUND ---
    fun recordRationDonation(
        entryType: DonationEntryType,
        personName: String,
        phone: String,
        amount: Double,
        rationSummary: String,
        paymentRef: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordRationDonation(
                entryType = entryType,
                personName = personName,
                phone = phone,
                amount = amount,
                rationSummary = rationSummary,
                paymentRef = paymentRef
            )
            _isBusy.value = false
            result.onSuccess {
                if (entryType == DonationEntryType.DONATION_IN) {
                    showMessage("راشن عطیہ کامیابی سے درج ہو گیا۔ جزاک اللہ خیراً!", "Ration donation recorded. JazakAllah Khair!")
                } else {
                    showMessage("مستحق خاندان کو راشن کی تقسیم درج ہو گئی", "Ration distribution recorded")
                }
            }.onFailure { err ->
                showMessage("اندراج نہ ہو سکا: ${err.localizedMessage}", "Failed: ${err.localizedMessage}")
            }
        }
    }

    // --- MARYAM NAWAZ RATION CARD (NFC & RS. 3,000 SUBSIDY) ---
    fun onNfcCardScanned(card: ScannedNfcCard) {
        _scannedNfcCard.value = card
        showMessage(
            "مریم نواز راشن کارڈ NFC اسکین ہو گیا (${card.uidHex})",
            "Maryam Nawaz Ration Card scanned via NFC (${card.uidHex})"
        )
    }

    fun clearScannedNfcCard() {
        _scannedNfcCard.value = null
    }

    fun saveRationCardItem(
        existingId: String?,
        nameUrdu: String,
        nameEnglish: String,
        unit: String,
        price: Double,
        sortOrder: Int
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveRationCardItem(existingId, nameUrdu, nameEnglish, unit, price, sortOrder)
            _isBusy.value = false
            result.onSuccess {
                showMessage("راشن آئٹم محفوظ ہو گیا", "Ration item saved")
            }.onFailure { err ->
                showMessage("محفوظ نہ ہو سکا: ${err.localizedMessage}", "Save failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteRationCardItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteRationCardItem(itemId).onSuccess {
                showMessage("راشن آئٹم حذف کر دیا گیا", "Ration item deleted")
            }
        }
    }

    fun checkoutRationCardBill(
        cartItems: List<RationCartItem>,
        cardUid: String,
        cardNumber: String,
        beneficiaryName: String,
        cnic: String,
        phone: String,
        subsidyLimit: Double,
        alreadyUsedThisMonth: Double,
        scannedViaNfc: Boolean,
        note: String,
        onSuccessClear: () -> Unit
    ) {
        if (cartItems.isEmpty()) {
            showMessage("بل میں کم از کم ایک راشن آئٹم شامل کریں", "Add at least one ration item")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createRationCardBill(
                cartItems = cartItems,
                cardUid = cardUid,
                cardNumber = cardNumber,
                beneficiaryName = beneficiaryName,
                cnic = cnic,
                phone = phone,
                subsidyLimit = subsidyLimit,
                alreadyUsedThisMonth = alreadyUsedThisMonth,
                scannedViaNfc = scannedViaNfc,
                note = note
            )
            _isBusy.value = false
            result.onSuccess { bill ->
                onSuccessClear()
                _activeRationCardReceipt.value = bill
                showMessage(
                    "مریم نواز راشن کارڈ بل محفوظ ہو گیا (#${bill.billNumber})",
                    "Ration Card bill saved (#${bill.billNumber})"
                )
            }.onFailure { err ->
                showMessage("بل محفوظ نہ ہو سکا: ${err.localizedMessage}", "Failed: ${err.localizedMessage}")
            }
        }
    }

    fun openRationCardReceipt(bill: RationCardBill) {
        _activeRationCardReceipt.value = bill
    }

    fun dismissRationCardReceipt() {
        _activeRationCardReceipt.value = null
    }

    fun printRationCardBillOnBluetooth(bill: RationCardBill) {
        val address = settings.value.selectedPrinterAddress
        if (address.isBlank()) {
            showMessage("پہلے سیٹنگز میں بلوٹوتھ پرنٹر منتخب کریں", "Select a Bluetooth printer in Settings first")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = bluetoothPrinterManager.printRationCardReceipt(address, bill, settings.value.shopPhone)
            _isBusy.value = false
            result.onSuccess {
                showMessage("راشن کارڈ رسید پرنٹ ہو گئی", "Ration Card receipt sent to printer")
            }.onFailure { err ->
                showMessage("پرنٹ ناکام: ${err.localizedMessage}", "Print failed: ${err.localizedMessage}")
            }
        }
    }

    // --- OPTIONAL GOOGLE CLOUD BACKUP & RESTORE ---
    fun backupLocalToCloud() {
        if (!repository.isCloudConnected()) {
            showMessage(
                "کلاؤڈ بیک اپ کے لیے پہلے گوگل اکاؤنٹ منسلک کریں (آپ کا تمام ڈیٹا فون میں محفوظ ہے)",
                "Connect a Google account first for Cloud Backup (your data is safely stored on this phone)"
            )
            return
        }
        if (!isOnline.value) {
            showMessage(
                "اس وقت آف لائن موڈ ہے — انٹرنیٹ منسلک کر کے کلاؤڈ بیک اپ کریں",
                "Currently offline — connect to the internet to run Cloud Backup"
            )
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.backupLocalToCloud()
            _isBusy.value = false
            result.onSuccess { count ->
                preferences.setLastCloudBackupTime(System.currentTimeMillis())
                showMessage(
                    "کلاؤڈ بیک اپ مکمل! $count ریکارڈز گوگل کلاؤڈ پر محفوظ ہو گئے",
                    "Cloud Backup complete! $count records backed up to Google Cloud"
                )
            }.onFailure { err ->
                showMessage(
                    "کلاؤڈ بیک اپ ناکام: ${err.localizedMessage ?: "دوبارہ کوشش کریں"}",
                    "Cloud Backup failed: ${err.localizedMessage ?: "Please try again"}"
                )
            }
        }
    }

    fun restoreCloudToLocal() {
        if (!repository.isCloudConnected()) {
            showMessage(
                "کلاؤڈ سے ری اسٹور کرنے کے لیے پہلے گوگل اکاؤنٹ منسلک کریں",
                "Connect your Google account first to restore from Cloud"
            )
            return
        }
        if (!isOnline.value) {
            showMessage(
                "ری اسٹور کے لیے انٹرنیٹ کنکشن درکار ہے",
                "Internet connection is required to restore from Cloud"
            )
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.restoreCloudToLocal()
            _isBusy.value = false
            result.onSuccess { count ->
                showMessage(
                    "کلاؤڈ سے $count ریکارڈز فون میں کامیابی سے ری اسٹور ہو گئے",
                    "Restored $count records from Google Cloud to phone"
                )
            }.onFailure { err ->
                showMessage(
                    "ری اسٹور ناکام: ${err.localizedMessage ?: "دوبارہ کوشش کریں"}",
                    "Cloud Restore failed: ${err.localizedMessage ?: "Please try again"}"
                )
            }
        }
    }

    // --- BLUETOOTH PRINTER ---
    fun refreshPairedPrinters() {
        val result = bluetoothPrinterManager.getPairedDevices()
        result.onSuccess { list ->
            _pairedPrinters.value = list
            if (list.isEmpty()) {
                showMessage(
                    "کوئی پیئرڈ بلوٹوتھ پرنٹر نہیں ملا — پہلے فون کی بلوٹوتھ سیٹنگ میں پیئر کریں",
                    "No paired Bluetooth devices found — pair your printer in Android Settings first"
                )
            }
        }.onFailure { err ->
            showMessage(
                "بلوٹوتھ دستیاب نہیں: ${err.localizedMessage}",
                "Bluetooth error: ${err.localizedMessage}"
            )
        }
    }

    fun selectBluetoothPrinter(printer: PairedBluetoothPrinter) {
        viewModelScope.launch {
            preferences.setBluetoothPrinter(printer.name, printer.address)
            showMessage(
                "بلوٹوتھ پرنٹر منتخب ہو گیا: ${printer.name}",
                "Selected Bluetooth printer: ${printer.name}"
            )
        }
    }

    fun printOrderOnBluetooth(order: Order) {
        val address = settings.value.selectedPrinterAddress
        if (address.isBlank()) {
            showMessage(
                "پہلے سیٹنگز میں بلوٹوتھ پرنٹر منتخب کریں",
                "Please select a Bluetooth printer in Settings first"
            )
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = bluetoothPrinterManager.printOrderReceipt(address, order, settings.value.shopPhone)
            _isBusy.value = false
            result.onSuccess {
                showMessage("رسید بلوٹوتھ پرنٹر پر پرنٹ ہو گئی", "Receipt sent to Bluetooth printer")
            }.onFailure { err ->
                showMessage(
                    "بلوٹوتھ پرنٹ ناکام: ${err.localizedMessage}",
                    "Bluetooth print failed: ${err.localizedMessage}"
                )
            }
        }
    }

    fun printBluetoothTestSlip() {
        val address = settings.value.selectedPrinterAddress
        if (address.isBlank()) {
            showMessage("پہلے بلوٹوتھ پرنٹر منتخب کریں", "Select a Bluetooth printer first")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = bluetoothPrinterManager.printTestSlip(address)
            _isBusy.value = false
            result.onSuccess {
                showMessage("ٹیسٹ پرنٹ بھیج دیا گیا", "Test slip sent to printer")
            }.onFailure { err ->
                showMessage("ٹیسٹ پرنٹ ناکام: ${err.localizedMessage}", "Test print failed: ${err.localizedMessage}")
            }
        }
    }

    // --- ANALYTICS COMPUTATION ---
    fun computeAnalytics(
        products: List<Product>,
        customers: List<Customer>,
        orders: List<Order>
    ): ShopAnalytics {
        val completedOrders = orders.filter { it.statusEnum() == OrderStatus.COMPLETED }
        val pendingCount = orders.count { it.statusEnum() == OrderStatus.PENDING }

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfTodayMs = cal.timeInMillis

        val todayOrders = completedOrders.filter {
            (it.createdAt?.toDate()?.time ?: System.currentTimeMillis()) >= startOfTodayMs
        }

        // Build last 7 days buckets for mini sales bar chart
        val dayBuckets = (6 downTo 0).map { daysAgo ->
            val dayStart = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startMs = dayStart.timeInMillis
            val endMs = startMs + 86_400_000L
            val label = "${dayStart.get(Calendar.DAY_OF_MONTH)}/${dayStart.get(Calendar.MONTH) + 1}"
            val dayTotal = completedOrders.filter {
                val t = it.createdAt?.toDate()?.time ?: 0L
                t in startMs until endMs
            }.sumOf { it.totalAmount }
            label to dayTotal
        }

        return ShopAnalytics(
            todaySales = todayOrders.sumOf { it.totalAmount },
            todayProfit = todayOrders.sumOf { it.profitAmount },
            totalSalesAllTime = completedOrders.sumOf { it.totalAmount },
            totalProfitAllTime = completedOrders.sumOf { it.profitAmount },
            totalUdharReceivable = customers.filter { it.balance > 0 }.sumOf { it.balance },
            totalInventoryValue = products.sumOf { it.stockQty * it.costPrice },
            lowStockCount = products.count { it.isLowStock() },
            pendingOrderCount = pendingCount,
            dailySalesLast7Days = dayBuckets
        )
    }
}
