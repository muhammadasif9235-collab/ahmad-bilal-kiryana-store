package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.shopDataStore by preferencesDataStore(name = "ahmad_bilal_kiryana_prefs")

object DonationCharityAccount {
    const val BANK_NAME: String = "HBL"
    const val ACCOUNT_TITLE: String = "Muhammad Asif"
    const val ACCOUNT_NUMBER: String = "16857900668003"
    const val IBAN: String = "PK08HABB0016857900668003"
    const val BRANCH_NAME: String = "MOTA, (TANDA)"
}

data class ShopSettings(
    val language: AppLanguage = AppLanguage.URDU,
    val selectedPrinterName: String = "",
    val selectedPrinterAddress: String = "",
    val defaultMarginPercent: Double = 8.0,
    val lastRateSyncEpochMs: Long = 0L,
    val storeNameUrdu: String = "احمد بلال کریانہ سٹور ٹانڈہ",
    val storeNameEnglish: String = "Ahmad Bilal Kiryana Store, Tanda",
    val shopPhone: String = "",
    val shopAddressUrdu: String = "مین بازار ٹانڈہ، ضلع گجرات، پنجاب",
    val shopAddressEnglish: String = "Main Bazaar Tanda, District Gujrat, Punjab",
    val localOwnerLoggedIn: Boolean = false,
    val storeSetupCompleted: Boolean = false,
    val lastCloudBackupEpochMs: Long = 0L,
    val bankName: String = "",
    val hblAccountTitle: String = "",
    val hblAccountNumber: String = "",
    val hblIban: String = "",
    val hblBranchName: String = "",
    val defaultPosFee: Double = 100.0,
    val shopLatitude: Double = 0.0,
    val shopLongitude: Double = 0.0
)

class ShopPreferences(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val PRINTER_NAME = stringPreferencesKey("bt_printer_name")
        val PRINTER_ADDRESS = stringPreferencesKey("bt_printer_address")
        val DEFAULT_MARGIN = doublePreferencesKey("default_margin_percent")
        val LAST_RATE_SYNC = longPreferencesKey("last_rate_sync_ms")
        val STORE_NAME_URDU = stringPreferencesKey("store_name_urdu")
        val STORE_NAME_ENGLISH = stringPreferencesKey("store_name_english")
        val SHOP_PHONE = stringPreferencesKey("shop_phone")
        val SHOP_ADDRESS_URDU = stringPreferencesKey("shop_address_urdu")
        val SHOP_ADDRESS_ENGLISH = stringPreferencesKey("shop_address_english")
        val LOCAL_OWNER_LOGGED_IN = booleanPreferencesKey("local_owner_logged_in")
        val STORE_SETUP_COMPLETED = booleanPreferencesKey("store_setup_completed")
        val LAST_CLOUD_BACKUP = longPreferencesKey("last_cloud_backup_ms")
        val BANK_NAME = stringPreferencesKey("bank_name")
        val HBL_ACCOUNT_TITLE = stringPreferencesKey("hbl_account_title")
        val HBL_ACCOUNT_NUMBER = stringPreferencesKey("hbl_account_number")
        val HBL_IBAN = stringPreferencesKey("hbl_iban")
        val HBL_BRANCH_NAME = stringPreferencesKey("hbl_branch_name")
        val DEFAULT_POS_FEE = doublePreferencesKey("default_pos_fee")
        val SHOP_LATITUDE = doublePreferencesKey("shop_latitude")
        val SHOP_LONGITUDE = doublePreferencesKey("shop_longitude")
    }

    val settingsFlow: Flow<ShopSettings> = context.shopDataStore.data.map { prefs ->
        ShopSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            selectedPrinterName = prefs[Keys.PRINTER_NAME] ?: "",
            selectedPrinterAddress = prefs[Keys.PRINTER_ADDRESS] ?: "",
            defaultMarginPercent = prefs[Keys.DEFAULT_MARGIN] ?: 8.0,
            lastRateSyncEpochMs = prefs[Keys.LAST_RATE_SYNC] ?: 0L,
            storeNameUrdu = prefs[Keys.STORE_NAME_URDU]?.takeIf { it.isNotBlank() } ?: "احمد بلال کریانہ سٹور ٹانڈہ",
            storeNameEnglish = prefs[Keys.STORE_NAME_ENGLISH]?.takeIf { it.isNotBlank() } ?: "Ahmad Bilal Kiryana Store, Tanda",
            shopPhone = prefs[Keys.SHOP_PHONE] ?: "",
            shopAddressUrdu = prefs[Keys.SHOP_ADDRESS_URDU]?.takeIf { it.isNotBlank() } ?: "مین بازار ٹانڈہ، ضلع گجرات، پنجاب",
            shopAddressEnglish = prefs[Keys.SHOP_ADDRESS_ENGLISH]?.takeIf { it.isNotBlank() } ?: "Main Bazaar Tanda, District Gujrat, Punjab",
            localOwnerLoggedIn = prefs[Keys.LOCAL_OWNER_LOGGED_IN] ?: false,
            storeSetupCompleted = prefs[Keys.STORE_SETUP_COMPLETED] ?: false,
            lastCloudBackupEpochMs = prefs[Keys.LAST_CLOUD_BACKUP] ?: 0L,
            bankName = prefs[Keys.BANK_NAME] ?: "",
            hblAccountTitle = prefs[Keys.HBL_ACCOUNT_TITLE] ?: "",
            hblAccountNumber = prefs[Keys.HBL_ACCOUNT_NUMBER] ?: "",
            hblIban = prefs[Keys.HBL_IBAN] ?: "",
            hblBranchName = prefs[Keys.HBL_BRANCH_NAME] ?: "",
            defaultPosFee = prefs[Keys.DEFAULT_POS_FEE] ?: 100.0,
            shopLatitude = prefs[Keys.SHOP_LATITUDE] ?: 0.0,
            shopLongitude = prefs[Keys.SHOP_LONGITUDE] ?: 0.0
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.shopDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setBluetoothPrinter(name: String, address: String) {
        context.shopDataStore.edit {
            it[Keys.PRINTER_NAME] = name
            it[Keys.PRINTER_ADDRESS] = address
        }
    }

    suspend fun setDefaultMargin(margin: Double) {
        context.shopDataStore.edit {
            it[Keys.DEFAULT_MARGIN] = margin.coerceIn(0.0, 100.0)
        }
    }

    suspend fun setLastRateSyncTime(epochMs: Long) {
        context.shopDataStore.edit { it[Keys.LAST_RATE_SYNC] = epochMs }
    }

    suspend fun setShopPhone(phone: String) {
        context.shopDataStore.edit { it[Keys.SHOP_PHONE] = phone.take(30) }
    }

    suspend fun setLocalOwnerLoggedIn(loggedIn: Boolean) {
        context.shopDataStore.edit { it[Keys.LOCAL_OWNER_LOGGED_IN] = loggedIn }
    }

    suspend fun setLastCloudBackupTime(epochMs: Long) {
        context.shopDataStore.edit { it[Keys.LAST_CLOUD_BACKUP] = epochMs }
    }

    suspend fun setDefaultPosFee(fee: Double) {
        context.shopDataStore.edit {
            it[Keys.DEFAULT_POS_FEE] = fee.coerceIn(0.0, 10000.0)
        }
    }

    suspend fun saveStoreProfile(
        storeNameUrdu: String,
        storeNameEnglish: String,
        addressUrdu: String,
        addressEnglish: String,
        phone: String,
        bankName: String,
        accountTitle: String,
        accountNumber: String,
        iban: String,
        branchName: String,
        markSetupCompleted: Boolean = true
    ) {
        context.shopDataStore.edit {
            val cleanUr = storeNameUrdu.trim().take(80)
            val cleanEn = storeNameEnglish.trim().ifBlank { cleanUr }.take(80)
            if (cleanUr.isNotBlank()) it[Keys.STORE_NAME_URDU] = cleanUr
            if (cleanEn.isNotBlank()) it[Keys.STORE_NAME_ENGLISH] = cleanEn
            val cleanAddrUr = addressUrdu.trim().take(120)
            val cleanAddrEn = addressEnglish.trim().ifBlank { cleanAddrUr }.take(120)
            if (cleanAddrUr.isNotBlank()) it[Keys.SHOP_ADDRESS_URDU] = cleanAddrUr
            if (cleanAddrEn.isNotBlank()) it[Keys.SHOP_ADDRESS_ENGLISH] = cleanAddrEn
            it[Keys.SHOP_PHONE] = phone.trim().take(30)
            it[Keys.BANK_NAME] = bankName.trim().take(40)
            it[Keys.HBL_ACCOUNT_TITLE] = accountTitle.trim().take(60)
            it[Keys.HBL_ACCOUNT_NUMBER] = accountNumber.trim().take(34)
            it[Keys.HBL_IBAN] = iban.trim().uppercase().replace(" ", "").take(34)
            it[Keys.HBL_BRANCH_NAME] = branchName.trim().take(60)
            if (markSetupCompleted) it[Keys.STORE_SETUP_COMPLETED] = true
        }
    }

    suspend fun setHblBankDetails(
        accountTitle: String,
        accountNumber: String,
        iban: String,
        branchName: String,
        bankName: String = ""
    ) {
        context.shopDataStore.edit {
            it[Keys.BANK_NAME] = bankName.trim().take(40)
            it[Keys.HBL_ACCOUNT_TITLE] = accountTitle.trim().take(60)
            it[Keys.HBL_ACCOUNT_NUMBER] = accountNumber.trim().take(34)
            it[Keys.HBL_IBAN] = iban.trim().uppercase().replace(" ", "").take(34)
            it[Keys.HBL_BRANCH_NAME] = branchName.trim().take(60)
        }
    }

    suspend fun applyOwnerDefaultBankIfEmpty() {
        // نیچے والے ذاتی/راست بینک خانوں میں کسی دوسرے کی تفصیل خودکار نہیں بھری جائے گی
    }

    suspend fun setShopLocation(
        latitude: Double,
        longitude: Double,
        addressUrdu: String? = null,
        addressEnglish: String? = null
    ) {
        context.shopDataStore.edit {
            it[Keys.SHOP_LATITUDE] = latitude
            it[Keys.SHOP_LONGITUDE] = longitude
            val cleanUr = addressUrdu?.trim()?.take(160).orEmpty()
            val cleanEn = addressEnglish?.trim()?.take(160).orEmpty()
            if (cleanUr.isNotBlank()) it[Keys.SHOP_ADDRESS_URDU] = cleanUr
            if (cleanEn.isNotBlank()) it[Keys.SHOP_ADDRESS_ENGLISH] = cleanEn
        }
    }
}
