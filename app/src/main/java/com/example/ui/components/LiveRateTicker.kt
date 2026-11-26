package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

data class TickerRateEntry(
    val categoryUrdu: String,
    val categoryEnglish: String,
    val nameUrdu: String,
    val nameEnglish: String,
    val priceTextUrdu: String,
    val priceTextEnglish: String,
    val isShopPromo: Boolean = false,
    val isAppServicePromo: Boolean = false
)

object DailyMarketTickerManager {
    private const val PREFS_NAME = "ahmad_bilal_daily_ticker_cache"
    private const val KEY_LAST_FETCH_MS = "last_fetch_ms"
    private const val KEY_RATES_JSON = "cached_rates_json"
    private const val TWENTY_FOUR_HOURS_MS = 24L * 60L * 60L * 1000L

    fun getDefaultGujratRates(): List<TickerRateEntry> = listOf(
        // گوشت و پولٹری
        TickerRateEntry(
            "گوشت و پولٹری", "Meat & Poultry",
            "برائلر مرغی زندہ (تھوک)", "Live Broiler (Wholesale)",
            "روپے 424 / کلو", "Rs 424 / kg"
        ),
        TickerRateEntry(
            "گوشت و پولٹری", "Meat & Poultry",
            "برائلر مرغی زندہ (پرچون)", "Live Broiler (Retail)",
            "روپے 438 / کلو", "Rs 438 / kg"
        ),
        TickerRateEntry(
            "گوشت و پولٹری", "Meat & Poultry",
            "برائلر مرغی گوشت (پرچون)", "Chicken Meat (Retail)",
            "روپے 635 / کلو", "Rs 635 / kg"
        ),
        TickerRateEntry(
            "گوشت و پولٹری", "Meat & Poultry",
            "انڈہ فارمی تھوک (1 پیٹی = 12 کریٹ / 360 انڈے)", "Farm Eggs Crate (12 Trays / 360 Eggs)",
            "روپے 9,090 / پیٹی", "Rs 9,090 / crate"
        ),
        TickerRateEntry(
            "گوشت و پولٹری", "Meat & Poultry",
            "انڈہ فارمی فی درجن", "Farm Eggs Per Dozen",
            "روپے 307 / درجن", "Rs 307 / dozen"
        ),
        TickerRateEntry(
            "گوشت و پولٹری", "Meat & Poultry",
            "چھوٹا گوشت (مٹن)", "Mutton",
            "روپے 2,000 / کلو", "Rs 2,000 / kg"
        ),
        TickerRateEntry(
            "گوشت و پولٹری", "Meat & Poultry",
            "بڑا گوشت (بیف)", "Beef",
            "روپے 1,000 / کلو", "Rs 1,000 / kg"
        ),

        // کریانہ و اجناس
        TickerRateEntry("کریانہ ریٹس", "Kiryana Rates", "چینی (شوگر)", "Sugar", "روپے 145 / کلو", "Rs 145 / kg"),
        TickerRateEntry("کریانہ ریٹس", "Kiryana Rates", "درجہ اول گھی", "Cooking Ghee", "روپے 520 / کلو", "Rs 520 / kg"),
        TickerRateEntry("کریانہ ریٹس", "Kiryana Rates", "کوکنگ آئل", "Cooking Oil", "روپے 535 / لیٹر", "Rs 535 / L"),
        TickerRateEntry("کریانہ ریٹس", "Kiryana Rates", "سپر کرنل باسمتی چاول", "Super Basmati Rice", "روپے 290 / کلو", "Rs 290 / kg"),
        TickerRateEntry("کریانہ ریٹس", "Kiryana Rates", "چکی آٹا (20 کلو تھیلا)", "Flour 20kg Bag", "روپے 1,780 / تھیلا", "Rs 1,780 / bag"),
        TickerRateEntry("کریانہ ریٹس", "Kiryana Rates", "دال چنا", "Chana Dal", "روپے 265 / کلو", "Rs 265 / kg"),
        TickerRateEntry("کریانہ ریٹس", "Kiryana Rates", "دال مسور", "Masoor Dal", "روپے 285 / کلو", "Rs 285 / kg"),

        // منڈی سبزیاں
        TickerRateEntry("منڈی سبزیاں", "Mandi Vegetables", "آلو نیا", "Fresh Potato", "روپے 75 / کلو", "Rs 75 / kg"),
        TickerRateEntry("منڈی سبزیاں", "Mandi Vegetables", "پیاز", "Onion", "روپے 115 / کلو", "Rs 115 / kg"),
        TickerRateEntry("منڈی سبزیاں", "Mandi Vegetables", "ٹماٹر", "Tomato", "روپے 95 / کلو", "Rs 95 / kg"),
        TickerRateEntry("منڈی سبزیاں", "Mandi Vegetables", "ادرک", "Ginger", "روپے 540 / کلو", "Rs 540 / kg"),
        TickerRateEntry("منڈی سبزیاں", "Mandi Vegetables", "لہسن دیسی", "Local Garlic", "روپے 480 / کلو", "Rs 480 / kg"),
        TickerRateEntry("منڈی سبزیاں", "Mandi Vegetables", "ہری مرچ", "Green Chili", "روپے 140 / کلو", "Rs 140 / kg"),

        // منڈی فروٹس / پھل
        TickerRateEntry("منڈی فروٹس", "Mandi Fruits", "کینو / مالٹا", "Kinnow / Orange", "روپے 180 / درجن", "Rs 180 / dozen"),
        TickerRateEntry("منڈی فروٹس", "Mandi Fruits", "سیب کالا کولو", "Kala Kulu Apple", "روپے 260 / کلو", "Rs 260 / kg"),
        TickerRateEntry("منڈی فروٹس", "Mandi Fruits", "کیلا", "Banana", "روپے 160 / درجن", "Rs 160 / dozen"),
        TickerRateEntry("منڈی فروٹس", "Mandi Fruits", "امرود", "Guava", "روپے 150 / کلو", "Rs 150 / kg"),
        TickerRateEntry("منڈی فروٹس", "Mandi Fruits", "انار", "Pomegranate", "روپے 340 / کلو", "Rs 340 / kg")
    )

    suspend fun loadDailyRates(context: Context, shopProducts: List<Product>): List<TickerRateEntry> {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastFetch = prefs.getLong(KEY_LAST_FETCH_MS, 0L)
        val now = System.currentTimeMillis()
        val cachedJson = prefs.getString(KEY_RATES_JSON, null)

        if (!cachedJson.isNullOrBlank() && (now - lastFetch) < TWENTY_FOUR_HOURS_MS) {
            val parsed = parseJsonRates(cachedJson)
            if (parsed.isNotEmpty()) return parsed
        }

        val cloudRates = tryFetchFromFirestore()
        if (cloudRates.isNotEmpty()) {
            prefs.edit()
                .putLong(KEY_LAST_FETCH_MS, now)
                .putString(KEY_RATES_JSON, toJsonString(cloudRates))
                .apply()
            return cloudRates
        }

        if (!cachedJson.isNullOrBlank()) {
            val parsed = parseJsonRates(cachedJson)
            if (parsed.isNotEmpty()) return parsed
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

        return (getDefaultGujratRates() + liveShopItems).distinctBy { it.nameUrdu }
    }

    private suspend fun tryFetchFromFirestore(): List<TickerRateEntry> {
        return try {
            val doc = FirebaseFirestore.getInstance()
                .collection("market_rates")
                .document("daily")
                .get()
                .await()

            val rawItems = doc.get("items") as? List<*> ?: return emptyList()
            rawItems.mapNotNull { entry ->
                val map = entry as? Map<*, *> ?: return@mapNotNull null
                val nameUr = map["nameUrdu"]?.toString()?.trim().orEmpty()
                val priceUr = map["priceUrdu"]?.toString()?.trim().orEmpty()
                if (nameUr.isBlank() || priceUr.isBlank()) return@mapNotNull null

                TickerRateEntry(
                    categoryUrdu = map["categoryUrdu"]?.toString()?.trim().orEmpty().ifBlank { "منڈی ریٹ" },
                    categoryEnglish = map["categoryEnglish"]?.toString()?.trim().orEmpty().ifBlank { "Market Rate" },
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

    private fun parseJsonRates(json: String): List<TickerRateEntry> {
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<TickerRateEntry>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    TickerRateEntry(
                        categoryUrdu = obj.optString("cu", "منڈی ریٹ"),
                        categoryEnglish = obj.optString("ce", "Market Rate"),
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
    var rawRates by remember { mutableStateOf(DailyMarketTickerManager.getDefaultGujratRates()) }

    LaunchedEffect(products.size) {
        rawRates = DailyMarketTickerManager.loadDailyRates(context, products)
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

        val appServicePromo = TickerRateEntry(
            categoryUrdu = "ایپ سروس",
            categoryEnglish = "App Service",
            nameUrdu = "اپنے کاروبار کی علیحدہ ایپ مناسب ریٹ میں بنوانے کے لیے رابطہ کریں",
            nameEnglish = "Get a custom app for your business at reasonable rates",
            priceTextUrdu = "03401558166 (WhatsApp / Call)",
            priceTextEnglish = "03401558166 (WhatsApp / Call)",
            isAppServicePromo = true
        )

        val combined = mutableListOf<TickerRateEntry>()
        var promoCycle = 0
        rawRates.forEachIndexed { index, item ->
            combined.add(item)
            if ((index + 1) % 7 == 0) {
                if (promoCycle % 2 == 0) {
                    combined.add(shopPromo)
                } else {
                    combined.add(appServicePromo)
                }
                promoCycle++
            }
        }
        if (combined.none { it.isShopPromo }) {
            combined.add(shopPromo)
        }
        if (combined.none { it.isAppServicePromo }) {
            combined.add(appServicePromo)
        }
        combined
    }

    val fullTickerText = remember(tickerFeed, language) {
        tickerFeed.joinToString(separator = "     ✦     ") { item ->
            when {
                item.isShopPromo -> language.pick(
                    "★ ${item.nameUrdu} — ${item.priceTextUrdu} ★",
                    "★ ${item.nameEnglish} — ${item.priceTextEnglish} ★"
                )
                item.isAppServicePromo -> language.pick(
                    "📱 ${item.nameUrdu}: ${item.priceTextUrdu}",
                    "📱 ${item.nameEnglish}: ${item.priceTextEnglish}"
                )
                else -> language.pick(
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
                    text = language.pick("لائیو ریٹس", "LIVE RATES"),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    maxLines = 1
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

@Composable
fun CustomAppPromoTopStrip(
    language: AppLanguage,
    onContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF102A43),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onContactClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = language.pick(
                    "اپنے کاروبار کی علیحدہ ایپ مناسب ریٹ میں بنوائیں",
                    "Custom business app at affordable rates"
                ),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF0F4F8),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "03401558166",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldShimmer,
                maxLines = 1
            )
        }
    }
}
