package com.example.ui.components

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Product
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.SaffronSecondary
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class TickerRateEntry(
    val categoryUrdu: String,
    val categoryEnglish: String,
    val nameUrdu: String,
    val nameEnglish: String,
    val priceTextUrdu: String,
    val priceTextEnglish: String,
    val isShopPromo: Boolean = false
)

data class ResolvedMarketRegion(
    val regionKey: String,
    val labelUrdu: String,
    val labelEnglish: String,
    val isGujratArea: Boolean
)

object DailyMarketTickerManager {
    private const val PREFS_NAME = "ahmad_bilal_daily_ticker_cache"
    private const val TWENTY_FOUR_HOURS_MS = 24L * 60L * 60L * 1000L

    fun resolveRegionInfo(context: Context, settings: ShopSettings): ResolvedMarketRegion {
        val combinedAddress = "${settings.shopAddressUrdu} ${settings.shopAddressEnglish}".lowercase(Locale.ROOT)
        val gpsCity = tryDetectCityFromCoordinates(
            context = context,
            lat = settings.shopLatitude,
            lng = settings.shopLongitude
        )

        val rawLocationText = listOfNotNull(
            gpsCity?.takeIf { it.isNotBlank() },
            combinedAddress.takeIf { it.isNotBlank() }
        ).joinToString(" ").lowercase(Locale.ROOT)

        return when {
            rawLocationText.contains("gujrat") || rawLocationText.contains("گجرات") ||
                rawLocationText.contains("tanda") || rawLocationText.contains("ٹانڈہ") ||
                rawLocationText.contains("kharian") || rawLocationText.contains("کھاریاں") ||
                rawLocationText.contains("lalamusa") || rawLocationText.contains("لالہ موسیٰ") ||
                rawLocationText.contains("sarai alamgir") || rawLocationText.contains("سرائے عالمگیر") ->
                ResolvedMarketRegion("gujrat", "گجرات منڈی", "Gujrat Mandi", isGujratArea = true)

            rawLocationText.contains("lahore") || rawLocationText.contains("لاہور") ->
                ResolvedMarketRegion("lahore", "لاہور منڈی", "Lahore Mandi", isGujratArea = false)

            rawLocationText.contains("gujranwala") || rawLocationText.contains("گوجرانوالہ") ->
                ResolvedMarketRegion("gujranwala", "گوجرانوالہ منڈی", "Gujranwala Mandi", isGujratArea = false)

            rawLocationText.contains("sialkot") || rawLocationText.contains("سیالکوٹ") ->
                ResolvedMarketRegion("sialkot", "سیالکوٹ منڈی", "Sialkot Mandi", isGujratArea = false)

            rawLocationText.contains("faisalabad") || rawLocationText.contains("فیصل آباد") ->
                ResolvedMarketRegion("faisalabad", "فیصل آباد منڈی", "Faisalabad Mandi", isGujratArea = false)

            rawLocationText.contains("multan") || rawLocationText.contains("ملتان") ->
                ResolvedMarketRegion("multan", "ملتان منڈی", "Multan Mandi", isGujratArea = false)

            rawLocationText.contains("karachi") || rawLocationText.contains("کراچی") ->
                ResolvedMarketRegion("karachi", "کراچی منڈی", "Karachi Mandi", isGujratArea = false)

            rawLocationText.contains("rawalpindi") || rawLocationText.contains("راولپنڈی") ||
                rawLocationText.contains("islamabad") || rawLocationText.contains("اسلام آباد") ->
                ResolvedMarketRegion("twin_cities", "راولپنڈی / اسلام آباد منڈی", "Rwp / Isb Mandi", isGujratArea = false)

            else -> {
                val cleanCity = gpsCity?.trim().orEmpty()
                if (cleanCity.isNotBlank()) {
                    val slug = cleanCity.lowercase(Locale.ROOT)
                        .replace(Regex("[^a-z0-9]+"), "_")
                        .trim('_')
                        .ifBlank { "local_market" }
                    ResolvedMarketRegion(
                        regionKey = slug,
                        labelUrdu = "$cleanCity مارکیٹ",
                        labelEnglish = "$cleanCity Market",
                        isGujratArea = false
                    )
                } else {
                    ResolvedMarketRegion(
                        regionKey = "local_market",
                        labelUrdu = "مقامی مارکیٹ",
                        labelEnglish = "Local Market",
                        isGujratArea = false
                    )
                }
            }
        }
    }

    private fun tryDetectCityFromCoordinates(
        context: Context,
        lat: Double,
        lng: Double
    ): String? {
        var targetLat = lat
        var targetLng = lng

        if (targetLat == 0.0 && targetLng == 0.0) {
            try {
                val lm = context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                val providers = lm?.getProviders(true).orEmpty()
                var bestLoc: Location? = null
                for (provider in providers) {
                    val loc = try {
                        lm?.getLastKnownLocation(provider)
                    } catch (_: SecurityException) {
                        null
                    }
                    if (loc != null && (bestLoc == null || loc.accuracy < bestLoc.accuracy)) {
                        bestLoc = loc
                    }
                }
                if (bestLoc != null) {
                    targetLat = bestLoc.latitude
                    targetLng = bestLoc.longitude
                }
            } catch (_: Throwable) {
            }
        }

        if (targetLat == 0.0 && targetLng == 0.0) return null

        return try {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(context.applicationContext, Locale.ENGLISH)
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(targetLat, targetLng, 1)
            val addr = addresses?.firstOrNull()
            addr?.locality?.takeIf { it.isNotBlank() }
                ?: addr?.subAdminArea?.takeIf { it.isNotBlank() }
                ?: addr?.adminArea?.takeIf { it.isNotBlank() }
        } catch (_: Throwable) {
            null
        }
    }

    fun getDefaultRatesForRegion(region: ResolvedMarketRegion): List<TickerRateEntry> {
        val catUr = region.labelUrdu
        val catEn = region.labelEnglish
        return listOf(
            // گوشت و پولٹری
            TickerRateEntry(
                catUr, catEn,
                "برائلر مرغی زندہ (تھوک)", "Live Broiler (Wholesale)",
                "روپے 424 / کلو", "Rs 424 / kg"
            ),
            TickerRateEntry(
                catUr, catEn,
                "برائلر مرغی زندہ (پرچون)", "Live Broiler (Retail)",
                "روپے 438 / کلو", "Rs 438 / kg"
            ),
            TickerRateEntry(
                catUr, catEn,
                "برائلر مرغی گوشت (پرچون)", "Chicken Meat (Retail)",
                "روپے 635 / کلو", "Rs 635 / kg"
            ),
            TickerRateEntry(
                catUr, catEn,
                "انڈہ فارمی تھوک (1 پیٹی = 12 کریٹ / 360 انڈے)", "Farm Eggs Crate (12 Trays / 360 Eggs)",
                "روپے 9,090 / پیٹی", "Rs 9,090 / crate"
            ),
            TickerRateEntry(
                catUr, catEn,
                "انڈہ فارمی فی درجن", "Farm Eggs Per Dozen",
                "روپے 307 / درجن", "Rs 307 / dozen"
            ),
            TickerRateEntry(
                catUr, catEn,
                "چھوٹا گوشت (مٹن)", "Mutton",
                "روپے 2,000 / کلو", "Rs 2,000 / kg"
            ),
            TickerRateEntry(
                catUr, catEn,
                "بڑا گوشت (بیف)", "Beef",
                "روپے 1,000 / کلو", "Rs 1,000 / kg"
            ),

            // کریانہ و اجناس
            TickerRateEntry(catUr, catEn, "چینی (شوگر)", "Sugar", "روپے 145 / کلو", "Rs 145 / kg"),
            TickerRateEntry(catUr, catEn, "درجہ اول گھی", "Cooking Ghee", "روپے 520 / کلو", "Rs 520 / kg"),
            TickerRateEntry(catUr, catEn, "کوکنگ آئل", "Cooking Oil", "روپے 535 / لیٹر", "Rs 535 / L"),
            TickerRateEntry(catUr, catEn, "سپر کرنل باسمتی چاول", "Super Basmati Rice", "روپے 290 / کلو", "Rs 290 / kg"),
            TickerRateEntry(catUr, catEn, "چکی آٹا (20 کلو تھیلا)", "Flour 20kg Bag", "روپے 1,780 / تھیلا", "Rs 1,780 / bag"),
            TickerRateEntry(catUr, catEn, "دال چنا", "Chana Dal", "روپے 265 / کلو", "Rs 265 / kg"),
            TickerRateEntry(catUr, catEn, "دال مسور", "Masoor Dal", "روپے 285 / کلو", "Rs 285 / kg"),

            // سبزیاں
            TickerRateEntry(catUr, catEn, "آلو نیا", "Fresh Potato", "روپے 75 / کلو", "Rs 75 / kg"),
            TickerRateEntry(catUr, catEn, "پیاز", "Onion", "روپے 115 / کلو", "Rs 115 / kg"),
            TickerRateEntry(catUr, catEn, "ٹماٹر", "Tomato", "روپے 95 / کلو", "Rs 95 / kg"),
            TickerRateEntry(catUr, catEn, "ادرک", "Ginger", "روپے 540 / کلو", "Rs 540 / kg"),
            TickerRateEntry(catUr, catEn, "لہسن دیسی", "Local Garlic", "روپے 480 / کلو", "Rs 480 / kg"),
            TickerRateEntry(catUr, catEn, "ہری مرچ", "Green Chili", "روپے 140 / کلو", "Rs 140 / kg"),

            // پھل / فروٹس
            TickerRateEntry(catUr, catEn, "کینو / مالٹا", "Kinnow / Orange", "روپے 180 / درجن", "Rs 180 / dozen"),
            TickerRateEntry(catUr, catEn, "سیب کالا کولو", "Kala Kulu Apple", "روپے 260 / کلو", "Rs 260 / kg"),
            TickerRateEntry(catUr, catEn, "کیلا", "Banana", "روپے 160 / درجن", "Rs 160 / dozen"),
            TickerRateEntry(catUr, catEn, "امرود", "Guava", "روپے 150 / کلو", "Rs 150 / kg"),
            TickerRateEntry(catUr, catEn, "انار", "Pomegranate", "روپے 340 / کلو", "Rs 340 / kg")
        )
    }

    suspend fun loadDailyRates(
        context: Context,
        settings: ShopSettings,
        shopProducts: List<Product>
    ): Pair<ResolvedMarketRegion, List<TickerRateEntry>> {
        val region = withContext(Dispatchers.IO) {
            resolveRegionInfo(context, settings)
        }
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val keyLastFetch = "last_fetch_ms_${region.regionKey}"
        val keyRatesJson = "cached_rates_json_${region.regionKey}"

        val lastFetch = prefs.getLong(keyLastFetch, 0L)
        val now = System.currentTimeMillis()
        val cachedJson = prefs.getString(keyRatesJson, null)

        if (!cachedJson.isNullOrBlank() && (now - lastFetch) < TWENTY_FOUR_HOURS_MS) {
            val parsed = parseJsonRates(cachedJson, region)
            if (parsed.isNotEmpty()) return region to parsed
        }

        val cloudRates = tryFetchFromFirestore(region)
        if (cloudRates.isNotEmpty()) {
            prefs.edit()
                .putLong(keyLastFetch, now)
                .putString(keyRatesJson, toJsonString(cloudRates))
                .apply()
            return region to cloudRates
        }

        if (!cachedJson.isNullOrBlank()) {
            val parsed = parseJsonRates(cachedJson, region)
            if (parsed.isNotEmpty()) return region to parsed
        }

        val liveShopItems = shopProducts
            .filter { it.sellingPrice > 0 }
            .take(6)
            .map { prod ->
                val priceInt = prod.sellingPrice.toInt()
                TickerRateEntry(
                    categoryUrdu = "دکان ریٹ",
                    categoryEnglish = "Store Rate",
                    nameUrdu = prod.nameUrdu,
                    nameEnglish = prod.nameEnglish,
                    priceTextUrdu = "روپے $priceInt / ${prod.unit}",
                    priceTextEnglish = "Rs $priceInt / ${prod.unit}"
                )
            }

        val fallbackList = (getDefaultRatesForRegion(region) + liveShopItems).distinctBy { it.nameUrdu }
        return region to fallbackList
    }

    private suspend fun tryFetchFromFirestore(region: ResolvedMarketRegion): List<TickerRateEntry> {
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = if (region.regionKey != "local_market") {
                val regionalSnap = db.collection("market_rates").document(region.regionKey).get().await()
                if (regionalSnap.exists()) regionalSnap else db.collection("market_rates").document("daily").get().await()
            } else {
                db.collection("market_rates").document("daily").get().await()
            }

            val rawItems = doc.get("items") as? List<*> ?: return emptyList()
            rawItems.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val nameUr = map["nameUrdu"]?.toString()?.trim().orEmpty()
                val priceUr = map["priceUrdu"]?.toString()?.trim().orEmpty()
                if (nameUr.isBlank() || priceUr.isBlank()) return@mapNotNull null

                TickerRateEntry(
                    categoryUrdu = map["categoryUrdu"]?.toString()?.trim().orEmpty().ifBlank { region.labelUrdu },
                    categoryEnglish = map["categoryEnglish"]?.toString()?.trim().orEmpty().ifBlank { region.labelEnglish },
                    nameUrdu = nameUr,
                    nameEnglish = map["nameEnglish"]?.toString()?.trim().orEmpty().ifBlank { nameUr },
                    priceTextUrdu = priceUr,
                    priceTextEnglish = map["priceEnglish"]?.toString()?.trim().orEmpty().ifBlank { priceUr }
                )
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun toJsonString(list: List<TickerRateEntry>): String {
        val arr = JSONArray()
        list.forEach { item ->
            val obj = JSONObject()
            obj.put("cu", item.categoryUrdu)
            obj.put("ce", item.categoryEnglish)
            obj.put("nu", item.nameUrdu)
            obj.put("ne", item.nameEnglish)
            obj.put("pu", item.priceTextUrdu)
            obj.put("pe", item.priceTextEnglish)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseJsonRates(json: String, region: ResolvedMarketRegion): List<TickerRateEntry> {
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<TickerRateEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    TickerRateEntry(
                        categoryUrdu = obj.optString("cu", region.labelUrdu),
                        categoryEnglish = obj.optString("ce", region.labelEnglish),
                        nameUrdu = obj.optString("nu"),
                        nameEnglish = obj.optString("ne"),
                        priceTextUrdu = obj.optString("pu"),
                        priceTextEnglish = obj.optString("pe")
                    )
                )
            }
            result
        } catch (_: Throwable) {
            emptyList()
        }
    }
}

@Composable
fun LiveMarketRateTicker(
    language: AppLanguage,
    settings: ShopSettings,
    products: List<Product>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var resolvedRegion by remember {
        mutableStateOf(
            ResolvedMarketRegion(
                regionKey = "local_market",
                labelUrdu = "مقامی مارکیٹ",
                labelEnglish = "Local Market",
                isGujratArea = false
            )
        )
    }
    var rawRates by remember {
        mutableStateOf(DailyMarketTickerManager.getDefaultRatesForRegion(resolvedRegion))
    }

    LaunchedEffect(
        settings.shopAddressUrdu,
        settings.shopAddressEnglish,
        settings.shopLatitude,
        settings.shopLongitude,
        products.size
    ) {
        val (region, rates) = DailyMarketTickerManager.loadDailyRates(context, settings, products)
        resolvedRegion = region
        rawRates = rates
    }

    val tickerFeed = remember(rawRates, settings.storeNameUrdu, settings.storeNameEnglish, settings.shopAddressUrdu) {
        val shopPromo = TickerRateEntry(
            categoryUrdu = "خوش آمدید",
            categoryEnglish = "Welcome",
            nameUrdu = "${settings.storeNameUrdu}، ${settings.shopAddressUrdu}",
            nameEnglish = "${settings.storeNameEnglish}, ${settings.shopAddressEnglish}",
            priceTextUrdu = "تازہ و معیاری اشیاء اور مناسب نرخ",
            priceTextEnglish = "Fresh Quality Grocery & Best Rates",
            isShopPromo = true
        )

        val combined = mutableListOf<TickerRateEntry>()
        rawRates.forEachIndexed { index, item ->
            combined.add(item)
            if ((index + 1) % 7 == 0) {
                combined.add(shopPromo)
            }
        }
        if (combined.none { it.isShopPromo }) {
            combined.add(shopPromo)
        }
        combined
    }

    val fullTickerText = remember(tickerFeed, language) {
        tickerFeed.joinToString(separator = "     ✦     ") { item ->
            if (item.isShopPromo) {
                language.pick(
                    "★ ${item.nameUrdu} — ${item.priceTextUrdu} ★",
                    "★ ${item.nameEnglish} — ${item.priceTextEnglish} ★"
                )
            } else {
                language.pick(
                    "[${item.categoryUrdu}] ${item.nameUrdu}: ${item.priceTextUrdu}",
                    "[${item.categoryEnglish}] ${item.nameEnglish}: ${item.priceTextEnglish}"
                )
            }
        }
    }

    Surface(
        color = Color(0xFF063729),
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.8.dp, color = GoldShimmer.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SaffronSecondary)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                Text(
                    text = language.pick(resolvedRegion.labelUrdu, resolvedRegion.labelEnglish),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = fullTickerText,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFEF3C7),
                maxLines = 1,
                overflow = TextOverflow.Visible,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 0,
                        initialDelayMillis = 400,
                        velocity = 42.dp
                    )
            )
        }
    }
}
