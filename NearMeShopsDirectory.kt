package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.services.LocationHelper
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import java.util.Locale

data class NearbyShopEntry(
    val id: String,
    val nameUrdu: String,
    val nameEnglish: String,
    val businessTypeLabel: String,
    val areaLabel: String,
    val distanceKm: Double,
    val shopPhone: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentUserShop: Boolean = false
)

private const val NEAR_ME_PREFS = "near_me_shops_directory_prefs"
private const val KEY_LIST_IN_NEAR_ME = "show_my_shop_in_near_me"

fun isShopListedInNearMe(context: Context): Boolean =
    context.applicationContext
        .getSharedPreferences(NEAR_ME_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_LIST_IN_NEAR_ME, true)

fun setShopListedInNearMe(context: Context, enabled: Boolean) {
    context.applicationContext
        .getSharedPreferences(NEAR_ME_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_LIST_IN_NEAR_ME, enabled)
        .apply()
}

private fun haversineDistanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    if (lat1 == 0.0 && lon1 == 0.0) return 0.0
    if (lat2 == 0.0 && lon2 == 0.0) return 1.5
    val earthRadiusKm = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
        Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
        Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadiusKm * c
}

@Composable
fun NearMeShopsSection(
    language: AppLanguage,
    settings: ShopSettings,
    selectedShop: NearbyShopEntry?,
    onSelectShop: (NearbyShopEntry) -> Unit,
    onRefreshLocation: (onComplete: (String) -> Unit) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var currentLat by remember(settings.shopLatitude) { mutableStateOf(settings.shopLatitude) }
    var currentLng by remember(settings.shopLongitude) { mutableStateOf(settings.shopLongitude) }
    var locationLabel by remember(settings.shopAddressUrdu, settings.shopAddressEnglish) {
        mutableStateOf(
            language.pick(settings.shopAddressUrdu, settings.shopAddressEnglish)
                .ifBlank { language.pick("موجودہ علاقہ (ٹانڈہ / گجرات)", "Current Area (Tanda / Gujrat)") }
        )
    }
    var searchQuery by remember { mutableStateOf("") }
    var orderNote by remember { mutableStateOf("") }

    val baseShops = remember(
        settings.storeNameUrdu,
        settings.storeNameEnglish,
        settings.shopPhone,
        settings.shopAddressUrdu,
        settings.shopAddressEnglish,
        currentLat,
        currentLng
    ) {
        val list = mutableListOf<NearbyShopEntry>()

        if (isShopListedInNearMe(context)) {
            list.add(
                NearbyShopEntry(
                    id = "current_active_shop",
                    nameUrdu = settings.storeNameUrdu.ifBlank { "میری دکان" },
                    nameEnglish = settings.storeNameEnglish.ifBlank { "My Shop" },
                    businessTypeLabel = language.pick("کریانہ / کاروبار سٹور", "Kiryana / Business Store"),
                    areaLabel = locationLabel,
                    distanceKm = 0.2,
                    shopPhone = settings.shopPhone.trim(),
                    latitude = if (currentLat != 0.0) currentLat else 32.5365,
                    longitude = if (currentLng != 0.0) currentLng else 73.9245,
                    isCurrentUserShop = true
                )
            )
        }

        val sampleNearby = listOf(
            NearbyShopEntry(
                id = "shop_tanda_kiryana_1",
                nameUrdu = "المدینہ کریانہ سٹور",
                nameEnglish = "Al-Madina Kiryana Store",
                businessTypeLabel = language.pick("کریانہ و راشن", "Kiryana & Ration"),
                areaLabel = language.pick("محلہ محمد نگر، ٹانڈہ", "Mohallah Muhammad Nagar, Tanda"),
                distanceKm = haversineDistanceKm(
                    if (currentLat != 0.0) currentLat else 32.5365,
                    if (currentLng != 0.0) currentLng else 73.9245,
                    32.5380,
                    73.9260
                ).coerceAtLeast(0.4),
                shopPhone = "",
                latitude = 32.5380,
                longitude = 73.9260
            ),
            NearbyShopEntry(
                id = "shop_tanda_crockery_2",
                nameUrdu = "رحمان کراکری و برتن ہاؤس",
                nameEnglish = "Rehman Crockery & Utensils",
                businessTypeLabel = language.pick("کراکری و برتن", "Crockery & Utensils"),
                areaLabel = language.pick("مین بازار، ٹانڈہ", "Main Bazaar, Tanda"),
                distanceKm = haversineDistanceKm(
                    if (currentLat != 0.0) currentLat else 32.5365,
                    if (currentLng != 0.0) currentLng else 73.9245,
                    32.5345,
                    73.9220
                ).coerceAtLeast(0.7),
                shopPhone = "",
                latitude = 32.5345,
                longitude = 73.9220
            ),
            NearbyShopEntry(
                id = "shop_gujrat_general_3",
                nameUrdu = "فیضان جنرل سٹور",
                nameEnglish = "Faizan General Store",
                businessTypeLabel = language.pick("جنرل سٹور و گروسری", "General Store & Grocery"),
                areaLabel = language.pick("جھنڈے وال روڈ، گجرات", "Jhanday Wal Road, Gujrat"),
                distanceKm = haversineDistanceKm(
                    if (currentLat != 0.0) currentLat else 32.5365,
                    if (currentLng != 0.0) currentLng else 73.9245,
                    32.5510,
                    74.0320
                ).coerceAtLeast(1.2),
                shopPhone = "",
                latitude = 32.5510,
                longitude = 74.0320
            )
        )
        list.addAll(sampleNearby)
        list.sortedBy { it.distanceKm }
    }

    val filteredShops = remember(baseShops, searchQuery, language) {
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) baseShops
        else baseShops.filter {
            it.nameUrdu.lowercase(Locale.ROOT).contains(q) ||
                it.nameEnglish.lowercase(Locale.ROOT).contains(q) ||
                it.businessTypeLabel.lowercase(Locale.ROOT).contains(q) ||
                it.areaLabel.lowercase(Locale.ROOT).contains(q)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, EmeraldPrimary.copy(alpha = 0.45f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = language.pick("قریبی کاروبار / دکانیں (Near Me)", "Near Me Shops & Businesses"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                    Text(
                        text = language.pick(
                            "موجودہ لوکیشن: $locationLabel",
                            "Current Location: $locationLabel"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = {
                        onRefreshLocation { detectedAddress ->
                            locationLabel = detectedAddress
                        }
                    }
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(language.pick("لائیو لوکیشن", "Live GPS"), style = MaterialTheme.typography.labelSmall)
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = {
                    Text(language.pick("دکان یا کاروبار تلاش کریں (مثلاً کریانہ، کراکری)", "Search shop or business (Kiryana, Crockery)"))
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            filteredShops.forEach { shop ->
                val isSelected = selectedShop?.id == shop.id
                val hasPhone = shop.shopPhone.isNotBlank()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectShop(shop) },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        width = if (isSelected) 1.6.dp else 1.dp,
                        color = if (isSelected) GoldShimmer else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) EmeraldPrimary.copy(alpha = 0.08f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                                Column {
                                    Text(
                                        text = language.pick(shop.nameUrdu, shop.nameEnglish),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${shop.businessTypeLabel} • ${shop.areaLabel}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Surface(
                                color = EmeraldPrimary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%.1f km", shop.distanceKm),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = null,
                                    tint = if (hasPhone) ProfitGreen else UdharCrimson,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (hasPhone) {
                                        language.pick("دکاندار نمبر: ${shop.shopPhone}", "Shop Number: ${shop.shopPhone}")
                                    } else {
                                        language.pick("دکاندار نے نمبر ایڈ نہیں کیا", "Shop number not added")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (hasPhone) ProfitGreen else UdharCrimson
                                )
                            }

                            if (isSelected) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = language.pick("منتخب", "Selected"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (selectedShop != null) {
                HorizontalDivider()
                Text(
                    text = language.pick(
                        "منتخب دکان: ${selectedShop.nameUrdu}",
                        "Selected Shop: ${selectedShop.nameEnglish}"
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
                OutlinedTextField(
                    value = orderNote,
                    onValueChange = { orderNote = it },
                    label = {
                        Text(language.pick("آرڈر کا سامان / تفصیل لکھیں", "Enter order items / details"))
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val cleanPhone = selectedShop.shopPhone.filter { it.isDigit() }
                            if (cleanPhone.isBlank()) {
                                onShowMessage(
                                    language.pick(
                                        "اس دکاندار کا نمبر ایپ میں ایڈ نہیں ہے — پہلے نمبر درج ہونا ضروری ہے۔",
                                        "This shop has no phone number added in the app."
                                    )
                                )
                                return@Button
                            }
                            val text = language.pick(
                                "السلام علیکم! مجھے ${selectedShop.nameUrdu} سے سامان کا آرڈر دینا ہے: ${orderNote.ifBlank { "سامان کی لسٹ" }}",
                                "Hello! I want to place an order with ${selectedShop.nameEnglish}: ${orderNote.ifBlank { "Order items" }}"
                            )
                            val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(text)}")
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }.onFailure {
                                onShowMessage(
                                    language.pick("واٹس ایپ / براؤزر کھولا نہیں جا سکا۔", "Could not open WhatsApp.")
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(language.pick("اسی دکان کو آرڈر بھیجیں", "Send Order to Shop"))
                    }
                    OutlinedButton(
                        onClick = {
                            val mapsUri = Uri.parse(
                                LocationHelper.buildGoogleMapsUrl(selectedShop.latitude, selectedShop.longitude)
                            )
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, mapsUri).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                })
                            }
                        }
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(language.pick("نقشہ", "Map"))
                    }
                }
            }
        }
    }
}
