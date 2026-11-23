package com.example.data

import kotlin.math.roundToInt

data class GujratSeedProduct(
    val docId: String,
    val nameUrdu: String,
    val nameEnglish: String,
    val category: ProductCategory,
    val unit: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val marginPercent: Double,
    val stockQty: Double,
    val lowStockThreshold: Double,
    val barcode: String,
    val marketReferenceCost: Double = 0.0
)

/**
 * Comprehensive Pakistani Kiryana Catalog.
 * Starts with 0 stock and 0 price until the shopkeeper manually enters stock/rates
 * or syncs location-based market rates.
 */
object GujratCatalogData {

    private fun item(
        id: String,
        ur: String,
        en: String,
        cat: ProductCategory,
        unit: String,
        refCost: Double,
        margin: Double,
        lowStock: Double,
        barcode: String
    ) = GujratSeedProduct(
        docId = id,
        nameUrdu = ur,
        nameEnglish = en,
        category = cat,
        unit = unit,
        costPrice = 0.0,
        sellingPrice = 0.0,
        marginPercent = margin,
        stockQty = 0.0,
        lowStockThreshold = lowStock,
        barcode = barcode,
        marketReferenceCost = refCost
    )

    val initialProducts: List<GujratSeedProduct> = listOf(
        // 1. Flour & Rice (آٹا اور چاول)
        item("prod_atta_10kg", "گندم کا آٹا (10 کلو تھیلا)", "Wheat Flour 10kg Bag", ProductCategory.FLOUR_RICE, "bag", 1390.0, 5.0, 5.0, "8961001000004"),
        item("prod_atta_20kg", "گندم کا آٹا (20 کلو تھیلا)", "Wheat Flour 20kg Bag", ProductCategory.FLOUR_RICE, "bag", 2780.0, 5.0, 4.0, "8961001000011"),
        item("prod_atta_1kg", "کھلا چکی آٹا", "Chakki Atta (Loose)", ProductCategory.FLOUR_RICE, "kg", 138.0, 7.0, 10.0, "8961001000028"),
        item("prod_fine_atta", "فائن آٹا سفید", "Super Fine White Atta", ProductCategory.FLOUR_RICE, "kg", 150.0, 8.0, 10.0, "8961001000080"),
        item("prod_maida", "میدہ فائن", "Fine Maida Flour", ProductCategory.FLOUR_RICE, "kg", 155.0, 9.0, 5.0, "8961001000035"),
        item("prod_suji", "سوجی صاف", "Semolina (Suji)", ProductCategory.FLOUR_RICE, "kg", 165.0, 10.0, 5.0, "8961001000042"),
        item("prod_basmati_super", "سپر کرنل باسمتی چاول", "Super Kernel Basmati Rice", ProductCategory.FLOUR_RICE, "kg", 350.0, 10.0, 10.0, "8961001000059"),
        item("prod_kainat_rice", "کائنات اسٹیم باسمتی چاول", "Kainat Steam Basmati Rice", ProductCategory.FLOUR_RICE, "kg", 365.0, 10.0, 10.0, "8961001000097"),
        item("prod_sella_rice", "سیلا باسمتی چاول", "Sella Basmati Rice", ProductCategory.FLOUR_RICE, "kg", 335.0, 10.0, 10.0, "8961001000073"),
        item("prod_basmati_tota", "ٹوٹا باسمتی چاول", "Broken Basmati Rice", ProductCategory.FLOUR_RICE, "kg", 220.0, 10.0, 10.0, "8961001000066"),

        // 2. Sugar & All Pulses (چینی، گڑ اور ہر قسم کی دالیں)
        item("prod_sugar", "چینی سفید", "White Sugar", ProductCategory.SUGAR_PULSES, "kg", 142.0, 7.0, 15.0, "8961003000015"),
        item("prod_gur", "دیسی گڑ", "Desi Gur (Jaggery)", ProductCategory.SUGAR_PULSES, "kg", 210.0, 10.0, 5.0, "8961003000022"),
        item("prod_shakkar", "دیسی شکر", "Desi Shakkar (Brown Sugar)", ProductCategory.SUGAR_PULSES, "kg", 220.0, 10.0, 5.0, "8961003000091"),
        item("prod_daal_chana", "دال چنا", "Daal Chana (Split Chickpeas)", ProductCategory.SUGAR_PULSES, "kg", 315.0, 9.0, 6.0, "8961003000039"),
        item("prod_daal_masoor_washed", "دال مسور دھلی (لال)", "Daal Masoor Washed (Red)", ProductCategory.SUGAR_PULSES, "kg", 275.0, 10.0, 6.0, "8961003000046"),
        item("prod_daal_masoor_whole", "ثابت مسور (کالی مسور)", "Whole Masoor (Black Lentil)", ProductCategory.SUGAR_PULSES, "kg", 270.0, 10.0, 5.0, "8961003000107"),
        item("prod_daal_moong_washed", "دال مونگ دھلی", "Daal Moong Washed (Yellow)", ProductCategory.SUGAR_PULSES, "kg", 345.0, 9.0, 6.0, "8961003000053"),
        item("prod_daal_moong_chilka", "دال مونگ چھلکا", "Daal Moong Chilka (Green Split)", ProductCategory.SUGAR_PULSES, "kg", 330.0, 9.0, 5.0, "8961003000114"),
        item("prod_daal_mash_washed", "دال ماش دھلی", "Daal Mash Washed (White)", ProductCategory.SUGAR_PULSES, "kg", 495.0, 8.0, 5.0, "8961003000060"),
        item("prod_daal_mash_chilka", "دال ماش چھلکا (کالی)", "Daal Mash Chilka (Black Split)", ProductCategory.SUGAR_PULSES, "kg", 465.0, 8.0, 5.0, "8961003000121"),
        item("prod_daal_arhar", "دال ارہر (تور دال)", "Daal Arhar (Toor Dal)", ProductCategory.SUGAR_PULSES, "kg", 420.0, 9.0, 4.0, "8961003000138"),
        item("prod_white_chana", "سفید چنا کابلی", "White Chickpeas (Kabuli)", ProductCategory.SUGAR_PULSES, "kg", 365.0, 9.0, 5.0, "8961003000077"),
        item("prod_black_chana", "کالا چنا دیسی", "Black Chickpeas (Kala Chana)", ProductCategory.SUGAR_PULSES, "kg", 295.0, 9.0, 5.0, "8961003000145"),
        item("prod_red_lobia", "لال لوبیا", "Red Kidney Beans (Lal Lobia)", ProductCategory.SUGAR_PULSES, "kg", 390.0, 9.0, 5.0, "8961003000152"),
        item("prod_white_lobia", "سفید لوبیا", "White Kidney Beans (Safaid Lobia)", ProductCategory.SUGAR_PULSES, "kg", 370.0, 9.0, 4.0, "8961003000169"),
        item("prod_besan", "خالص بیسن", "Pure Gram Flour (Besan)", ProductCategory.SUGAR_PULSES, "kg", 335.0, 9.0, 5.0, "8961003000084"),

        // 3. Ghee & Cooking Oil Brands (صوفی، میزان، حبیب، کشمیر، ڈالڈا اور لوکل گھی)
        item("prod_sufi_ghee_1kg", "صوفی بناسپتی گھی (1 کلو)", "Sufi Banaspati Ghee 1kg", ProductCategory.GHEE_OIL, "pack", 565.0, 5.0, 6.0, "8961002000063"),
        item("prod_sufi_oil_1l", "صوفی کوکنگ آئل (1 لیٹر)", "Sufi Cooking Oil 1L", ProductCategory.GHEE_OIL, "liter", 580.0, 5.0, 6.0, "8961002000025"),
        item("prod_sufi_oil_5l", "صوفی کوکنگ آئل (5 لیٹر)", "Sufi Cooking Oil 5L", ProductCategory.GHEE_OIL, "tin", 2900.0, 5.0, 2.0, "8961002000070"),
        item("prod_meezan_ghee_1kg", "میزان بناسپتی گھی (1 کلو)", "Meezan Banaspati Ghee 1kg", ProductCategory.GHEE_OIL, "pack", 560.0, 5.0, 6.0, "8961002000087"),
        item("prod_meezan_oil_1l", "میزان کوکنگ آئل (1 لیٹر)", "Meezan Cooking Oil 1L", ProductCategory.GHEE_OIL, "liter", 575.0, 5.0, 6.0, "8961002000094"),
        item("prod_meezan_5l", "میزان کوکنگ آئل (5 لیٹر ٹن)", "Meezan Oil 5L Tin", ProductCategory.GHEE_OIL, "tin", 2920.0, 5.0, 2.0, "8961002000032"),
        item("prod_habib_ghee_1kg", "حبیب بناسپتی گھی (1 کلو)", "Habib Banaspati Ghee 1kg", ProductCategory.GHEE_OIL, "pack", 565.0, 5.0, 5.0, "8961002000100"),
        item("prod_habib_oil_1l", "حبیب کوکنگ آئل (1 لیٹر)", "Habib Cooking Oil 1L", ProductCategory.GHEE_OIL, "liter", 580.0, 5.0, 5.0, "8961002000117"),
        item("prod_dalda_ghee", "ڈالڈا بناسپتی گھی (1 کلو)", "Dalda Banaspati Ghee 1kg", ProductCategory.GHEE_OIL, "pack", 565.0, 5.0, 6.0, "8961002000018"),
        item("prod_dalda_oil_1l", "ڈالڈا کوکنگ آئل (1 لیٹر)", "Dalda Cooking Oil 1L", ProductCategory.GHEE_OIL, "liter", 585.0, 5.0, 5.0, "8961002000124"),
        item("prod_kashmir_ghee", "کشمیر بناسپتی گھی (1 کلو)", "Kashmir Banaspati Ghee 1kg", ProductCategory.GHEE_OIL, "pack", 550.0, 5.0, 6.0, "8961002000049"),
        item("prod_kashmir_oil_1l", "کشمیر کوکنگ آئل (1 لیٹر)", "Kashmir Cooking Oil 1L", ProductCategory.GHEE_OIL, "liter", 565.0, 5.0, 5.0, "8961002000131"),
        item("prod_eva_oil_1l", "ایوا کوکنگ آئل (1 لیٹر)", "Eva Cooking Oil 1L", ProductCategory.GHEE_OIL, "liter", 575.0, 5.0, 4.0, "8961002000148"),
        item("prod_local_ghee_1kg", "لوکل بناسپتی گھی (1 کلو)", "Local Banaspati Ghee 1kg", ProductCategory.GHEE_OIL, "pack", 510.0, 6.0, 6.0, "8961002000155"),
        item("prod_mustard_oil", "سرسوں کا خالص تیل", "Pure Mustard Oil", ProductCategory.GHEE_OIL, "liter", 520.0, 7.0, 4.0, "8961002000056"),

        // 4. Tea Brands & Sizes (پتی: لپٹن، سپریم، ٹپال، وائٹل — پاؤ، آدھا کلو، کلو)
        item("prod_lipton_250", "لپٹن یلو لیبل پتی (پاؤ / 250 گرام)", "Lipton Yellow Label Tea 250g", ProductCategory.TEA_DAIRY, "pack", 430.0, 7.0, 5.0, "8961004000029"),
        item("prod_lipton_500", "لپٹن یلو لیبل پتی (آدھا کلو / 500 گرام)", "Lipton Yellow Label Tea 500g", ProductCategory.TEA_DAIRY, "pack", 850.0, 7.0, 4.0, "8961004000074"),
        item("prod_lipton_1kg", "لپٹن یلو لیبل پتی (1 کلو)", "Lipton Yellow Label Tea 1kg", ProductCategory.TEA_DAIRY, "pack", 1680.0, 6.0, 3.0, "8961004000081"),
        item("prod_lipton_sachet", "لپٹن یلو لیبل ساشے (چھوٹا پیک)", "Lipton Yellow Label Sachet", ProductCategory.TEA_DAIRY, "pack", 45.0, 11.0, 15.0, "8961004000098"),
        item("prod_supreme_250", "سپریم چائے پتی (پاؤ / 250 گرام)", "Brooke Bond Supreme Tea 250g", ProductCategory.TEA_DAIRY, "pack", 420.0, 7.0, 5.0, "8961004000104"),
        item("prod_supreme_500", "سپریم چائے پتی (آدھا کلو / 500 گرام)", "Brooke Bond Supreme Tea 500g", ProductCategory.TEA_DAIRY, "pack", 830.0, 7.0, 4.0, "8961004000111"),
        item("prod_supreme_1kg", "سپریم چائے پتی (1 کلو)", "Brooke Bond Supreme Tea 1kg", ProductCategory.TEA_DAIRY, "pack", 1640.0, 6.0, 3.0, "8961004000128"),
        item("prod_tapal_250", "ٹپال دانے دار چائے (پاؤ / 250 گرام)", "Tapal Danedar Tea 250g", ProductCategory.TEA_DAIRY, "pack", 425.0, 7.0, 5.0, "8961004000012"),
        item("prod_tapal_500", "ٹپال دانے دار چائے (آدھا کلو / 500 گرام)", "Tapal Danedar Tea 500g", ProductCategory.TEA_DAIRY, "pack", 840.0, 7.0, 4.0, "8961004000135"),
        item("prod_tapal_1kg", "ٹپال دانے دار چائے (1 کلو)", "Tapal Danedar Tea 1kg", ProductCategory.TEA_DAIRY, "pack", 1660.0, 6.0, 3.0, "8961004000142"),
        item("prod_vital_250", "وائٹل چائے پتی (پاؤ / 250 گرام)", "Vital Tea 250g", ProductCategory.TEA_DAIRY, "pack", 410.0, 7.0, 4.0, "8961004000159"),
        item("prod_vital_500", "وائٹل چائے پتی (آدھا کلو / 500 گرام)", "Vital Tea 500g", ProductCategory.TEA_DAIRY, "pack", 810.0, 7.0, 3.0, "8961004000166"),

        // 5. Packed Dairy, Bakery & Eggs (ڈبے والا دودھ، بیکری اور انڈے — کھلا دودھ ختم)
        item("prod_olpers_milk", "اولپرز دودھ پیکٹ (1 لیٹر)", "Olper's UHT Milk 1L", ProductCategory.TEA_DAIRY, "pack", 310.0, 6.0, 6.0, "8961004000036"),
        item("prod_olpers_250", "اولپرز دودھ چھوٹا پیک (250ml)", "Olper's UHT Milk 250ml", ProductCategory.TEA_DAIRY, "pack", 85.0, 10.0, 12.0, "8961004000173"),
        item("prod_milkpak_1l", "نیسلے ملک پیک دودھ (1 لیٹر)", "Nestle MilkPak 1L", ProductCategory.TEA_DAIRY, "pack", 315.0, 6.0, 6.0, "8961004000180"),
        item("prod_everyday_85", "نیسلے ایوری ڈے (85 گرام)", "Nestle Everyday 85g", ProductCategory.TEA_DAIRY, "pack", 195.0, 9.0, 6.0, "8961004000067"),
        item("prod_everyday_sachet", "ایوری ڈے ساشے (چھوٹا پیک)", "Nestle Everyday Sachet", ProductCategory.TEA_DAIRY, "pack", 35.0, 12.0, 20.0, "8961004000197"),
        item("prod_tarang_1l", "ترنگ چائے وائٹنر (1 لیٹر)", "Tarang Tea Whitener 1L", ProductCategory.TEA_DAIRY, "pack", 250.0, 7.0, 6.0, "8961004000203"),
        item("prod_eggs", "فارمی انڈے (درجن)", "Farm Eggs (Dozen)", ProductCategory.TEA_DAIRY, "dozen", 260.0, 9.0, 4.0, "8961004000050"),
        item("prod_reg_bread_lg", "رجسٹرڈ بڑی ڈبل روٹی (Registered Bread)", "Registered Large Bread", ProductCategory.TEA_DAIRY, "piece", 145.0, 10.0, 4.0, "8961004000227"),
        item("prod_reg_bread_sm", "رجسٹرڈ چھوٹی ڈبل روٹی", "Registered Small Bread", ProductCategory.TEA_DAIRY, "piece", 80.0, 12.0, 4.0, "8961004000234"),
        item("prod_bakery_bun", "میٹھا بن / برگر بن (Bun)", "Bakery Sweet / Burger Bun", ProductCategory.TEA_DAIRY, "piece", 32.0, 15.0, 6.0, "8961004000241"),
        
