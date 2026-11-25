package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BusinessType
import com.example.data.CartItem
import com.example.data.Customer
import com.example.data.DailyCashEntry
import com.example.data.DailyCashType
import com.example.data.DailyPaymentSource
import com.example.data.DonationEntryType
import com.example.data.DonationStatus
import com.example.data.LedgerEntry
import com.example.data.LedgerEntryType
import com.example.data.MaryamRationCatalogData
import com.example.data.Order
import com.example.data.OrderItem
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
import com.example.data.Wholesaler
import com.example.data.WholesalerEntryType
import com.example.data.WholesalerLedgerEntry
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
import java.util.Locale

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

    val dailyCashEntriesState: StateFlow<UiState<List<DailyCashEntry>>> =
        repository.observeDailyCashEntries().toUiState("dailyCashEntries")

    val wholesalersState: StateFlow<UiState<List<Wholesaler>>> =
        repository.observeWholesalers().toUiState("wholesalers")

    val wholesalerEntriesState: StateFlow<UiState<List<WholesalerLedgerEntry>>> =
        repository.observeWholesalerEntries().toUiState("wholesalerEntries")

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

    private val _manualCustomerPhone = MutableStateFlow("")
    val manualCustomerPhone: StateFlow<String> = _manualCustomerPhone.asStateFlow()

    private val _activeReceiptOrder = MutableStateFlow<Order?>(null)
    val activeReceiptOrder: StateFlow<Order?> = _activeReceiptOrder.asStateFlow()

    private val _activePaymentReceipt = MutableStateFlow<LedgerEntry?>(null)
    val activePaymentReceipt: StateFlow<LedgerEntry?> = _activePaymentReceipt.asStateFlow()

    private val _activeRationCardReceipt = MutableStateFlow<RationCardBill?>(null)
    val activeRationCardReceipt: StateFlow<RationCardBill?> = _activeRationCardReceipt.asStateFlow()

    private val _scannedNfcCard = MutableStateFlow<ScannedNfcCard?>(null)
    val scannedNfcCard: StateFlow<ScannedNfcCard?> = _scannedNfcCard.asStateFlow()

    private val _pendingPosScannedCardNumber = MutableStateFlow<String?>(null)
    val pendingPosScannedCardNumber: StateFlow<String?> = _pendingPosScannedCardNumber.asStateFlow()

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

    fun updateHblBankDetails(
        accountTitle: String,
        accountNumber: String,
        iban: String,
        branchName: String,
        bankName: String = settings.value.bankName
    ) {
        viewModelScope.launch {
            preferences.setHblBankDetails(accountTitle, accountNumber, iban, branchName, bankName)
            showMessage("بینک اکاؤنٹ اور RAAST QR تفصیلات محفوظ ہو گئیں", "Bank account & RAAST QR settings saved")
        }
    }

    fun saveStoreProfile(
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
    ) {
        viewModelScope.launch {
            preferences.saveStoreProfile(
                storeNameUrdu = storeNameUrdu,
                storeNameEnglish = storeNameEnglish,
                addressUrdu = addressUrdu,
                addressEnglish = addressEnglish,
                phone = phone,
                bankName = bankName,
                accountTitle = accountTitle,
                accountNumber = accountNumber,
                iban = iban,
                branchName = branchName,
                markSetupCompleted = true
            )
            showMessage("دکان کا پروفائل اور بینک سیٹ اپ محفوظ ہو گیا!", "Store profile & bank setup saved!")
        }
    }

    fun updateDefaultPosFee(fee: Double) {
        viewModelScope.launch {
            preferences.setDefaultPosFee(fee)
            showMessage("ڈیفالٹ POS سروس فیس محفوظ ہو گئی (Rs. ${fee.toLong()})", "Default POS fee saved (Rs. ${fee.toLong()})")
        }
    }

    fun updateShopGpsLocation(
        latitude: Double,
        longitude: Double,
        detectedAddress: String?
    ) {
        viewModelScope.launch {
            preferences.setShopLocation(
                latitude = latitude,
                longitude = longitude,
                addressUrdu = detectedAddress,
                addressEnglish = detectedAddress
            )
            showMessage(
                "دکان کی GPS لوکیشن اور پتہ محفوظ ہو گیا!",
                "Shop GPS location & address saved!"
            )
        }
    }

    // --- BILLING CART OPERATIONS ---
    fun selectCustomerForBill(customer: Customer?) {
        _selectedCustomer.value = customer
        if (customer != null) {
            _manualCustomerName.value = customer.name
            _manualCustomerPhone.value = customer.phone
        }
    }

    fun updateManualCustomerName(name: String) {
        _manualCustomerName.value = name
        if (_selectedCustomer.value?.name != name) {
            _selectedCustomer.value = null
        }
    }

    fun updateManualCustomerPhone(phone: String) {
        _manualCustomerPhone.value = phone
    }

    fun addProductToCart(product: Product, qtyDelta: Double = 1.0) {
        if (qtyDelta <= 0.0) return
        _cartItems.update { current ->
            val idx = current.indexOfFirst { it.product.id == product.id }
            if (idx >= 0) {
                val updated = current.toMutableList()
                val existing = updated[idx]
                updated[idx] = existing.copy(quantity = (existing.quantity + qtyDelta).coerceAtLeast(0.001))
                updated
            } else {
                current + CartItem(product = product, quantity = qtyDelta.coerceAtLeast(0.001))
            }
        }
    }

    fun addProductByRupeeAmount(product: Product, amountPkr: Double, replaceExisting: Boolean = false) {
        if (amountPkr <= 0.0 || product.sellingPrice <= 0.0) return
        val exactWeight = (amountPkr / product.sellingPrice).coerceAtLeast(0.001)
        if (replaceExisting) {
            setCartItemQuantity(product.id, exactWeight)
        } else {
            addProductToCart(product, exactWeight)
        }
    }

    fun setCartItemQuantity(productId: String, newQty: Double) {
        _cartItems.update { current ->
            if (newQty <= 0.0005) {
                current.filterNot { it.product.id == productId }
            } else {
                val exists = current.any { it.product.id == productId }
                if (exists) {
                    current.map { if (it.product.id == productId) it.copy(quantity = newQty) else it }
                } else {
                    val product = (productsState.value as? UiState.Success)?.data
                        ?.firstOrNull { it.id == productId }
                    if (product != null) {
                        current + CartItem(product = product, quantity = newQty)
                    } else {
                        current
                    }
                }
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
        _manualCustomerPhone.value = ""
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

        val (unpackedPhone, cleanNote) = if (note.startsWith("\u001FCP:")) {
            val endIdx = note.indexOf('\u001F', startIndex = 4)
            if (endIdx > 4) {
                val phonePart = note.substring(4, endIdx).trim()
                val restNote = note.substring(endIdx + 1)
                phonePart to restNote
            } else {
                "" to note
            }
        } else {
            "" to note
        }
        val effectivePhone = unpackedPhone.ifBlank { _manualCustomerPhone.value.trim() }

        val cleanManualName = _manualCustomerName.value.trim()
        val isWalkInPlaceholder = cleanManualName.isBlank() ||
            cleanManualName.equals("نقد کسٹمر", ignoreCase = true) ||
            cleanManualName.equals("Walk-in Customer", ignoreCase = true)
        val subtotal = items.sumOf { it.lineTotal }
        val grandTotal = (subtotal - discount.coerceIn(0.0, subtotal)).coerceAtLeast(0.0)
        val estimatedUdhar = when {
            saveAsPendingOrder -> 0.0
            paymentMethod == PaymentMethod.UDHAR -> grandTotal
            paymentMethod == PaymentMethod.PARTIAL -> (grandTotal - paidAmount.coerceIn(0.0, grandTotal)).coerceAtLeast(0.0)
            else -> 0.0
        }
        val isCreditMode = !saveAsPendingOrder &&
            (paymentMethod == PaymentMethod.UDHAR || paymentMethod == PaymentMethod.PARTIAL) &&
            estimatedUdhar > 0.0
        if (isCreditMode && _selectedCustomer.value == null && isWalkInPlaceholder) {
            showMessage(
                "ادھار بل کے لیے کسٹمر منتخب کریں یا نیا کسٹمر نام درج کریں",
                "Select a customer or type a new customer name for credit billing"
            )
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.createOrderOrBill(
                cartItems = items,
                customer = _selectedCustomer.value,
                customerNameInput = cleanManualName,
                customerPhoneInput = effectivePhone,
                paymentMethod = paymentMethod,
                discount = discount,
                paidAmountInput = paidAmount,
                saveAsPendingOrder = saveAsPendingOrder,
                note = cleanNote
            )
            _isBusy.value = false
            result.onSuccess { createdOrder ->
                clearCart()
                _activeReceiptOrder.value = createdOrder
                if (saveAsPendingOrder) {
                    showMessage("آرڈر محفوظ ہو گیا (#${createdOrder.billNumber})", "Order saved (#${createdOrder.billNumber})")
                } else if (createdOrder.remainingUdhar > 0.0) {
                    showMessage(
                        "بل بن گیا اور ادھار کھاتہ میں خودکار درج ہو گیا (#${createdOrder.billNumber})",
                        "Bill saved & credit auto-posted to Khata (#${createdOrder.billNumber})"
                    )
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

    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            repository.deleteOrder(orderId).onSuccess {
                showMessage("بل ریکارڈ ڈیلیٹ ہو گیا", "Bill record deleted")
            }
        }
    }

    fun updateExistingOrder(
        existingOrder: Order,
        updatedItems: List<OrderItem>,
        discount: Double,
        extraCharges: Double,
        finalTotalOverride: Double?,
        paymentMethod: PaymentMethod,
        paidAmountInput: Double,
        note: String,
        onUpdated: (Order) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateExistingOrder(
                existingOrder = existingOrder,
                updatedItems = updatedItems,
                discount = discount,
                extraCharges = extraCharges,
                finalTotalOverride = finalTotalOverride,
                paymentMethod = paymentMethod,
                paidAmountInput = paidAmountInput,
                note = note
            )
            _isBusy.value = false
            result.onSuccess { updatedOrder ->
                _activeReceiptOrder.value = updatedOrder
                onUpdated(updatedOrder)
                showMessage(
                    "بل #${updatedOrder.billNumber} اپڈیٹ ہو گیا اور کھاتہ بیلنس خودکار درست ہو گیا!",
                    "Bill #${updatedOrder.billNumber} updated & Khata balance recalculated!"
                )
            }.onFailure { err ->
                showMessage(
                    "بل اپڈیٹ نہ ہو سکا: ${err.localizedMessage}",
                    "Bill update failed: ${err.localizedMessage}"
                )
            }
        }
    }

    fun openReceipt(order: Order) {
        _activeReceiptOrder.value = order
    }

    fun dismissReceipt() {
        _activeReceiptOrder.value = null
    }

    fun openPaymentReceipt(entry: LedgerEntry) {
        _activePaymentReceipt.value = entry
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
            }.onFailure { err ->
                showMessage("آئٹم حذف نہ ہو سکا: ${err.localizedMessage}", "Delete failed: ${err.localizedMessage}")
            }
        }
    }

    fun switchBusinessType(
        businessType: BusinessType,
        clearExistingProducts: Boolean = true
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.applyBusinessCatalog(
                businessType = businessType,
                clearExistingProducts = clearExistingProducts
            )
            _isBusy.value = false
            result.onSuccess { count ->
                showMessage(
                    "${businessType.titleUrdu} کی $count اشیاء لوڈ ہو گئیں",
                    "Loaded $count items for ${businessType.titleEnglish}"
                )
            }.onFailure { err ->
                showMessage(
                    "کاروبار تبدیل نہ ہو سکا: ${err.localizedMessage}",
                    "Failed to switch business: ${err.localizedMessage}"
                )
            }
        }
    }

    fun syncGujratMarketRates() {
        val products = (productsState.value as? UiState.Success)?.data ?: emptyList()
        val currentSettings = settings.value
        val deviceLocale = Locale.getDefault()
        val countryCode = deviceLocale.country.ifBlank { "PK" }

        val addressEn = currentSettings.shopAddressEnglish.trim()
        val addressUr = currentSettings.shopAddressUrdu.trim()
        val combinedHint = "$addressEn $addressUr"

        val displayRegionUr = when {
            addressUr.isNotBlank() -> addressUr
            addressEn.isNotBlank() -> addressEn
            countryCode.equals("PK", ignoreCase = true) -> "مقامی پاکستان منڈی"
            else -> deviceLocale.getDisplayCountry(Locale.ENGLISH).ifBlank { countryCode }
        }
        val displayRegionEn = when {
            addressEn.isNotBlank() -> addressEn
            addressUr.isNotBlank() -> addressUr
            countryCode.equals("PK", ignoreCase = true) -> "Local Pakistan Market"
            else -> deviceLocale.getDisplayCountry(Locale.ENGLISH).ifBlank { countryCode }
        }

        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.applyGujratMarketRates(
                products = products,
                fallbackMarginPercent = currentSettings.defaultMarginPercent,
                locationHint = combinedHint,
                countryCode = countryCode
            )
            _isBusy.value = false
            result.onSuccess { count ->
                preferences.setLastRateSyncTime(System.currentTimeMillis())
                showMessage(
                    "$displayRegionUr کی مقامی مارکیٹ کے مطابق $count اشیاء کی قیمتیں اپڈیٹ ہو گئیں",
                    "Auto-updated $count item prices for $displayRegionEn market"
                )
            }.onFailure { err ->
                showMessage(
                    "قیمتیں اپڈیٹ نہ ہو سکیں: ${err.localizedMessage}",
                    "Update failed: ${err.localizedMessage}"
                )
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

    fun updateCustomerTransaction(
        entry: LedgerEntry,
        newType: LedgerEntryType,
        newAmount: Double,
        newNote: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateLedgerTransaction(entry, newType, newAmount, newNote)
            _isBusy.value = false
            result.onSuccess {
                showMessage("کھاتہ انٹری اپڈیٹ ہو گئی اور بیلنس تازہ ہو گیا", "Ledger entry updated & balance recalculated")
            }.onFailure { err ->
                showMessage("اپڈیٹ نہ ہو سکا: ${err.localizedMessage}", "Update failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteCustomerTransaction(entry: LedgerEntry) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteLedgerTransaction(entry)
            _isBusy.value = false
            result.onSuccess {
                showMessage("انٹری ڈیلیٹ ہو گئی اور بقایا بیلنس اپڈیٹ ہو گیا", "Entry deleted & remaining balance recalculated")
            }.onFailure { err ->
                showMessage("ڈیلیٹ نہ ہو سکا: ${err.localizedMessage}", "Delete failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteCustomer(customerId)
            _isBusy.value = false
            result.onSuccess {
                showMessage("کسٹمر کھاتہ مستقل طور پر ڈیلیٹ ہو گیا", "Customer account permanently deleted")
            }.onFailure { err ->
                showMessage("کھاتہ ڈیلیٹ نہ ہو سکا: ${err.localizedMessage}", "Failed to delete customer: ${err.localizedMessage}")
            }
        }
    }

    // --- RATION CHARITY FUND (Pending Until Shop Owner Verifies in HBL) ---
    fun recordRationDonation(
        entryType: DonationEntryType,
        personName: String,
        phone: String,
        amount: Double,
        rationSummary: String,
        paymentRef: String,
        senderAccount: String = "",
        transactionId: String = "",
        receiptImagePath: String = ""
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordRationDonation(
                entryType = entryType,
                personName = personName,
                phone = phone,
                amount = amount,
                rationSummary = rationSummary,
                paymentRef = paymentRef,
                senderAccount = senderAccount,
                transactionId = transactionId,
                receiptImagePath = receiptImagePath
            )
            _isBusy.value = false
            result.onSuccess {
                if (entryType == DonationEntryType.DONATION_IN) {
                    showMessage(
                        "عطیہ «تصدیق باقی (PENDING)» کے طور پر محفوظ ہو گیا۔ HBL میں رقم چیک کر کے تصدیق کریں۔",
                        "Donation submitted as PENDING. Verify in HBL to add to the official fund."
                    )
                } else {
                    showMessage("مستحق خاندان کو راشن کی تقسیم درج ہو گئی", "Ration distribution recorded")
                }
            }.onFailure { err ->
                showMessage("اندراج نہ ہو سکا: ${err.localizedMessage}", "Failed: ${err.localizedMessage}")
            }
        }
    }

    fun updateDonationStatus(donationId: String, newStatus: DonationStatus) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateDonationStatus(donationId, newStatus)
            _isBusy.value = false
            result.onSuccess {
                when (newStatus) {
                    DonationStatus.VERIFIED -> showMessage(
                        "عطیہ تصدیق ہو گیا اور HBL راشن فنڈ میں شامل کر دیا گیا۔ جزاک اللہ خیراً!",
                        "Donation VERIFIED and added to official HBL Charity Fund!"
                    )
                    DonationStatus.REJECTED -> showMessage(
                        "عطیہ مسترد کر دیا گیا (فنڈ میں شامل نہیں کیا گیا)",
                        "Donation marked as REJECTED (not added to fund)"
                    )
                    DonationStatus.PENDING -> showMessage("اسٹیٹس اپڈیٹ ہو گیا", "Status updated")
                }
            }.onFailure { err ->
                showMessage("اسٹیٹس تبدیل نہ ہو سکا: ${err.localizedMessage}", "Update failed: ${err.localizedMessage}")
            }
        }
    }

    // --- POS CARD (NFC & RS. 3,000 - RS. 100 FEE = RS. 2,900 NET RATION) ---
    fun onNfcCardScanned(card: ScannedNfcCard) {
        _scannedNfcCard.value = card
        showMessage(
            "POS کارڈ NFC سے سکین ہو گیا! (${card.uidHex})",
            "POS Card scanned via NFC! (${card.uidHex})"
        )
    }

    fun clearScannedNfcCard() {
        _scannedNfcCard.value = null
    }

    fun setPendingPosScannedCardNumber(cardNo: String?) {
        _pendingPosScannedCardNumber.value = cardNo
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
                showMessage("POS آئٹم محفوظ ہو گیا", "POS item saved")
            }.onFailure { err ->
                showMessage("محفوظ نہ ہو سکا: ${err.localizedMessage}", "Save failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteRationCardItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteRationCardItem(itemId).onSuccess {
                showMessage("POS آئٹم حذف کر دیا گیا", "POS item deleted")
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
        feeDeduction: Double = MaryamRationCatalogData.DEFAULT_SERVICE_FEE_PKR,
        alreadyUsedThisMonth: Double,
        scannedViaNfc: Boolean,
        note: String,
        onSuccessClear: () -> Unit
    ) {
        if (cartItems.isEmpty()) {
            showMessage("بل میں کم از کم ایک POS آئٹم شامل کریں", "Add at least one POS item")
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
                feeDeduction = feeDeduction,
                alreadyUsedThisMonth = alreadyUsedThisMonth,
                scannedViaNfc = scannedViaNfc,
                note = note
            )
            _isBusy.value = false
            result.onSuccess { bill ->
                onSuccessClear()
                _activeRationCardReceipt.value = bill
                showMessage(
                    "POS کارڈ بل محفوظ ہو گیا (#${bill.billNumber})",
                    "POS Card bill saved (#${bill.billNumber})"
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

    fun deleteRationCardBill(billId: String) {
        viewModelScope.launch {
            repository.deleteRationCardBill(billId).onSuccess {
                showMessage("POS کارڈ ریکارڈ ڈیلیٹ ہو گیا", "POS card bill deleted")
            }
        }
    }

    // --- DAILY CASH IN / OUT LEDGER ---
    fun saveDailyCashEntry(
        existingId: String?,
        entryType: DailyCashType,
        paymentSource: DailyPaymentSource,
        categoryOrTitle: String,
        amount: Double,
        referenceId: String,
        note: String,
        entryDateMs: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveDailyCashEntry(
                existingId = existingId,
                entryType = entryType,
                paymentSource = paymentSource,
                categoryOrTitle = categoryOrTitle,
                amount = amount,
                referenceId = referenceId,
                note = note,
                entryDateMs = entryDateMs
            )
            _isBusy.value = false
            result.onSuccess {
                if (entryType == DailyCashType.CASH_IN) {
                    showMessage("آمدن / سیل کھاتے میں محفوظ ہو گئی", "Cash In / Daily Sale recorded")
                } else {
                    showMessage("اخراجات / خریداری محفوظ ہو گئی", "Cash Out / Expense recorded")
                }
            }.onFailure { err ->
                showMessage("اندراج نہ ہو سکا: ${err.localizedMessage}", "Save failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteDailyCashEntry(entryId: String) {
        viewModelScope.launch {
            repository.deleteDailyCashEntry(entryId).onSuccess {
                showMessage("روزانہ کیش انٹری ڈیلیٹ ہو گئی", "Daily cash entry deleted")
            }
        }
    }

    // --- WHOLESALERS / SUPPLIERS IN / OUT LEDGER ---
    fun saveWholesaler(
        existingId: String?,
        name: String,
        phone: String,
        cityOrMarket: String,
        notes: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.saveWholesaler(existingId, name, phone, cityOrMarket, notes)
            _isBusy.value = false
            result.onSuccess {
                showMessage("ہول سیلر / سپلائر پروفائل محفوظ ہو گیا", "Wholesaler profile saved")
            }.onFailure { err ->
                showMessage("محفوظ نہ ہو سکا: ${err.localizedMessage}", "Save failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteWholesaler(wholesalerId: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteWholesaler(wholesalerId)
            _isBusy.value = false
            result.onSuccess {
                showMessage("ہول سیلر کھاتہ ڈیلیٹ ہو گیا", "Wholesaler account deleted")
            }.onFailure { err ->
                showMessage("ڈیلیٹ نہ ہو سکا: ${err.localizedMessage}", "Delete failed: ${err.localizedMessage}")
            }
        }
    }

    fun recordWholesalerEntry(
        wholesaler: Wholesaler,
        entryType: WholesalerEntryType,
        amount: Double,
        invoiceNumber: String,
        description: String,
        paymentSource: DailyPaymentSource,
        referenceId: String,
        entryDateMs: Long = System.currentTimeMillis(),
        onSaved: (WholesalerLedgerEntry) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordWholesalerEntry(
                wholesaler = wholesaler,
                entryType = entryType,
                amount = amount,
                invoiceNumber = invoiceNumber,
                description = description,
                paymentSource = paymentSource,
                referenceId = referenceId,
                entryDateMs = entryDateMs
            )
            _isBusy.value = false
            result.onSuccess { saved ->
                onSaved(saved)
                if (entryType == WholesalerEntryType.STOCK_IN) {
                    showMessage("ہول سیلر سامان خریداری درج ہو گئی", "Wholesaler stock purchase recorded")
                } else {
                    showMessage("ہول سیلر ادائیگی درج ہو گئی", "Wholesaler payment recorded")
                }
            }.onFailure { err ->
                showMessage("اندراج نہ ہو سکا: ${err.localizedMessage}", "Save failed: ${err.localizedMessage}")
            }
        }
    }

    fun updateWholesalerEntry(
        entry: WholesalerLedgerEntry,
        newType: WholesalerEntryType,
        newAmount: Double,
        newInvoiceNumber: String,
        newDescription: String,
        newPaymentSource: DailyPaymentSource,
        newReferenceId: String
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.updateWholesalerEntry(
                entry = entry,
                newType = newType,
                newAmount = newAmount,
                newInvoiceNumber = newInvoiceNumber,
                newDescription = newDescription,
                newPaymentSource = newPaymentSource,
                newReferenceId = newReferenceId
            )
            _isBusy.value = false
            result.onSuccess {
                showMessage("ہول سیلر انٹری اپڈیٹ ہو گئی", "Wholesaler entry updated")
            }.onFailure { err ->
                showMessage("اپڈیٹ نہ ہو سکا: ${err.localizedMessage}", "Update failed: ${err.localizedMessage}")
            }
        }
    }

    fun deleteWholesalerEntry(entry: WholesalerLedgerEntry) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteWholesalerEntry(entry)
            _isBusy.value = false
            result.onSuccess {
                showMessage("ہول سیلر انٹری ڈیلیٹ ہو گئی اور بقایا تازہ ہو گیا", "Wholesaler entry deleted & balance updated")
            }.onFailure { err ->
                showMessage("ڈیلیٹ نہ ہو سکا: ${err.localizedMessage}", "Delete failed: ${err.localizedMessage}")
            }
        }
    }

    fun printRationCardBillOnBluetooth(bill: RationCardBill) {
        val currentSettings = settings.value
        val address = currentSettings.selectedPrinterAddress
        if (address.isBlank()) {
            showMessage("پہلے سیٹنگز میں بلوٹوتھ پرنٹر منتخب کریں", "Select a Bluetooth printer in Settings first")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = bluetoothPrinterManager.printRationCardReceipt(
                deviceAddress = address,
                bill = bill,
                shopPhone = currentSettings.shopPhone,
                shopNameEnglish = currentSettings.storeNameEnglish,
                shopAddressEnglish = currentSettings.shopAddressEnglish
            )
            _isBusy.value = false
            result.onSuccess {
                showMessage("POS کارڈ رسید 58mm پرنٹر پر پرنٹ ہو گئی", "POS receipt sent to 58mm Bluetooth printer")
            }.onFailure { err ->
                showMessage("پرنٹ ناکام: ${err.localizedMessage}", "Print failed: ${err.localizedMessage}")
            }
        }
    }

    fun printLedgerEntryOnBluetooth(entry: LedgerEntry) {
        val currentSettings = settings.value
        val address = currentSettings.selectedPrinterAddress
        if (address.isBlank()) {
            showMessage("پہلے سیٹنگز میں بلوٹوتھ پرنٹر منتخب کریں", "Select a Bluetooth printer in Settings first")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = bluetoothPrinterManager.printPaymentReceipt(
                deviceAddress = address,
                entry = entry,
                shopPhone = currentSettings.shopPhone,
                shopNameEnglish = currentSettings.storeNameEnglish,
                shopAddressEnglish = currentSettings.shopAddressEnglish
            )
            _isBusy.value = false
            result.onSuccess {
                showMessage("کھاتہ رسید 58mm پرنٹر پر پرنٹ ہو گئی", "Khata receipt sent to 58mm Bluetooth printer")
            }.onFailure { err ->
                showMessage("بلوٹوتھ پرنٹ ناکام: ${err.localizedMessage}", "Bluetooth print failed: ${err.localizedMessage}")
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
        val currentSettings = settings.value
        val address = currentSettings.selectedPrinterAddress
        if (address.isBlank()) {
            showMessage(
                "پہلے سیٹنگز میں بلوٹوتھ پرنٹر منتخب کریں",
                "Please select a Bluetooth printer in Settings first"
            )
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = bluetoothPrinterManager.printOrderReceipt(
                deviceAddress = address,
                order = order,
                shopPhone = currentSettings.shopPhone,
                shopNameEnglish = currentSettings.storeNameEnglish,
                shopAddressEnglish = currentSettings.shopAddressEnglish
            )
            _isBusy.value = false
            result.onSuccess {
                showMessage("رسید 58mm بلوٹوتھ پرنٹر پر پرنٹ ہو گئی", "Receipt sent to 58mm Bluetooth printer")
            }.onFailure { err ->
                showMessage(
                    "بلوٹوتھ پرنٹ ناکام: ${err.localizedMessage}",
                    "Bluetooth print failed: ${err.localizedMessage}"
                )
            }
        }
    }

    fun printBluetoothTestSlip() {
        val currentSettings = settings.value
        val address = currentSettings.selectedPrinterAddress
        if (address.isBlank()) {
            showMessage("پہلے بلوٹوتھ پرنٹر منتخب کریں", "Select a Bluetooth printer first")
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val result = bluetoothPrinterManager.printTestSlip(
                deviceAddress = address,
                shopNameEnglish = currentSettings.storeNameEnglish,
                shopAddressEnglish = currentSettings.shopAddressEnglish,
                shopPhone = currentSettings.shopPhone
            )
            _isBusy.value = false
            result.onSuccess {
                showMessage("58mm ٹیسٹ پرنٹ بھیج دیا گیا", "58mm test slip sent to printer")
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
