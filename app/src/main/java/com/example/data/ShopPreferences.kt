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

data class ShopSettings(
    val language: AppLanguage = AppLanguage.URDU,
    val selectedPrinterName: String = "",
    val selectedPrinterAddress: String = "",
    val defaultMarginPercent: Double = 8.0,
    val lastRateSyncEpochMs: Long = 0L,
    val shopPhone: String = "0300-0000000",
    val shopAddressUrdu: String = "مین بازار ٹانڈہ، ضلع گجرات، پنجاب",
    val shopAddressEnglish: String = "Main Bazaar Tanda, District Gujrat, Punjab",
    val localOwnerLoggedIn: Boolean = false,
    val lastCloudBackupEpochMs: Long = 0L
)

class ShopPreferences(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val PRINTER_NAME = stringPreferencesKey("bt_printer_name")
        val PRINTER_ADDRESS = stringPreferencesKey("bt_printer_address")
        val DEFAULT_MARGIN = doublePreferencesKey("default_margin_percent")
        val LAST_RATE_SYNC = longPreferencesKey("last_rate_sync_ms")
        val SHOP_PHONE = stringPreferencesKey("shop_phone")
        val LOCAL_OWNER_LOGGED_IN = booleanPreferencesKey("local_owner_logged_in")
        val LAST_CLOUD_BACKUP = longPreferencesKey("last_cloud_backup_ms")
    }

    val settingsFlow: Flow<ShopSettings> = context.shopDataStore.data.map { prefs ->
        ShopSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            selectedPrinterName = prefs[Keys.PRINTER_NAME] ?: "",
            selectedPrinterAddress = prefs[Keys.PRINTER_ADDRESS] ?: "",
            defaultMarginPercent = prefs[Keys.DEFAULT_MARGIN] ?: 8.0,
            lastRateSyncEpochMs = prefs[Keys.LAST_RATE_SYNC] ?: 0L,
            shopPhone = prefs[Keys.SHOP_PHONE] ?: "0300-0000000",
            localOwnerLoggedIn = prefs[Keys.LOCAL_OWNER_LOGGED_IN] ?: false,
            lastCloudBackupEpochMs = prefs[Keys.LAST_CLOUD_BACKUP] ?: 0L
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
}
