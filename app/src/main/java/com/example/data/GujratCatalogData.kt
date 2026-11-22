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
    val barcode: String
)

/**
 * Realistic District Gujrat, Punjab (Pakistan) retail & mandi Kiryana rates in PKR.
 */
object GujratCatalogData {

    val initialProducts: List<GujratSeedProduct> = listOf(
        // Flour & Rice (آٹا اور چاول)
        GujratSeedProduct("prod_atta_20kg", "گندم کا آٹا (20 کلو تھیلا)", "Wheat Flour 20kg Bag", ProductCategory.FLOUR_RICE, "bag", 2780.0, 2920.0, 5.0, 20.0, 5.0, "8961001000011"),
        GujratSeedProduct("prod_atta_1kg", "کھلا چکی آٹا", "Chakki Atta (Loose)", ProductCategory.FLOUR_RICE, "kg", 138.0, 148.0, 7.2, 85.0, 15.0, "8961001000028"),
        GujratSeedProduct("prod_maida", "میدہ فائن", "Fine Maida Flour", ProductCategory.FLOUR_RICE, "kg", 155.0, 170.0, 9.7, 30.0, 8.0, "8961001000035"),
        GujratSeedProduct("prod_suji", "سوجی صاف", "Semolina (Suji)", ProductCategory.FLOUR_RICE, "kg", 165.0, 185.0, 12.1, 25.0, 5.0, "8961001000042"),
        GujratSeedProduct("prod_basmati_super", "سپر کرنل باسمتی چاول", "Super Kernel Basmati Rice", ProductCategory.FLOUR_RICE, "kg", 350.0, 385.0, 10.0, 60.0, 12.0, "8961001000059"),
        GujratSeedProduct("prod_basmati_tota", "ٹوٹا باسمتی چاول", "Broken Basmati Rice", ProductCategory.FLOUR_RICE, "kg", 220.0, 245.0, 11.4, 45.0, 10.0, "8961001000066"),
        GujratSeedProduct("prod_sella_rice", "سیلا باسمتی چاول", "Sella Basmati Rice", ProductCategory.FLOUR_RICE, "kg", 335.0, 370.0, 10.4, 40.0, 10.0, "8961001000073"),

        // Ghee & Cooking Oil (گھی اور کوکنگ آئل)
        GujratSeedProduct("prod_dalda_ghee", "ڈالڈا بناسپتی گھی (1 کلو)", "Dalda Banaspati Ghee 1kg", ProductCategory.GHEE_OIL, "pack", 565.0, 595.0, 5.3, 32.0, 8.0, "8961002000018"),
        GujratSeedProduct("prod_sufi_oil", "صوفی کوکنگ آئل (1 لیٹر)", "Sufi Cooking Oil 1L", ProductCategory.GHEE_OIL, "liter", 580.0, 610.0, 5.2, 28.0, 8.0, "8961002000025"),
        GujratSeedProduct("prod_meezan_5l", "میزان کوکنگ آئل (5 لیٹر ٹن)", "Meezan Oil 5L Tin", ProductCategory.GHEE_OIL, "tin", 2920.0, 3060.0, 4.8, 12.0, 3.0, "8961002000032"),
        GujratSeedProduct("prod_kashmir_ghee", "کشمیر بناسپتی گھی (1 کلو)", "Kashmir Banaspati Ghee 1kg", ProductCategory.GHEE_OIL, "pack", 550.0, 580.0, 5.5, 24.0, 6.0, "8961002000049"),
        GujratSeedProduct("prod_mustard_oil", "سرسوں کا خالص تیل", "Pure Mustard Oil", ProductCategory.GHEE_OIL, "liter", 520.0, 560.0, 7.7, 15.0, 4.0, "8961002000056"),

        // Sugar & Pulses (چینی اور دالیں)
        GujratSeedProduct("prod_sugar", "چینی سفید", "White Sugar", ProductCategory.SUGAR_PULSES, "kg", 142.0, 152.0, 7.0, 100.0, 20.0, "8961003000015"),
        GujratSeedProduct("prod_gur", "دیسی گڑ گجرات", "Desi Gur (Jaggery)", ProductCategory.SUGAR_PULSES, "kg", 210.0, 235.0, 11.9, 25.0, 5.0, "8961003000022"),
        GujratSeedProduct("prod_daal_chana", "دال چنا", "Daal Chana (Gram Pulse)", ProductCategory.SUGAR_PULSES, "kg", 315.0, 345.0, 9.5, 35.0, 8.0, "8961003000039"),
        GujratSeedProduct("prod_daal_masoor", "دال مسور لال", "Daal Masoor (Red Lentil)", ProductCategory.SUGAR_PULSES, "kg", 275.0, 305.0, 10.9, 30.0, 8.0, "8961003000046"),
        GujratSeedProduct("prod_daal_moong", "دال مونگ دھلی", "Daal Moong Washed", ProductCategory.SUGAR_PULSES, "kg", 345.0, 375.0, 8.7, 28.0, 6.0, "8961003000053"),
        GujratSeedProduct("prod_daal_mash", "دال ماش دھلی", "Daal Mash Washed", ProductCategory.SUGAR_PULSES, "kg", 495.0, 535.0, 8.1, 22.0, 5.0, "8961003000060"),
        GujratSeedProduct("prod_white_chana", "سفید چنا قابلی", "White Chickpeas", ProductCategory.SUGAR_PULSES, "kg", 365.0, 400.0, 9.6, 26.0, 6.0, "8961003000077"),
        GujratSeedProduct("prod_besan", "خالص بیسن", "Gram Flour (Besan)", ProductCategory.SUGAR_PULSES, "kg", 335.0, 365.0, 9.0, 30.0, 6.0, "8961003000084"),

        // Tea & Dairy (چائے اور دودھ)
        GujratSeedProduct("prod_tapal_tea", "ٹپال دانے دار چائے (190 گرام)", "Tapal Danedar Tea 190g", ProductCategory.TEA_DAIRY, "pack", 390.0, 420.0, 7.7, 30.0, 6.0, "8961004000012"),
        GujratSeedProduct("prod_lipton_tea", "لپٹن یلو لیبل (190 گرام)", "Lipton Yellow Label 190g", ProductCategory.TEA_DAIRY, "pack", 400.0, 430.0, 7.5, 24.0, 6.0, "8961004000029"),
        GujratSeedProduct("prod_olpers_milk", "اولپرز دودھ (1 لیٹر)", "Olper's Milk 1L", ProductCategory.TEA_DAIRY, "pack", 310.0, 330.0, 6.5, 36.0, 10.0, "8961004000036"),
        GujratSeedProduct("prod_fresh_milk", "تازہ کھلا دودھ", "Fresh Loose Milk", ProductCategory.TEA_DAIRY, "liter", 210.0, 230.0, 9.5, 25.0, 5.0, "8961004000043"),
        GujratSeedProduct("prod_eggs", "فارمی انڈے (درجن)", "Farm Eggs (Dozen)", ProductCategory.TEA_DAIRY, "dozen", 250.0, 275.0, 10.0, 18.0, 5.0, "8961004000050"),
        GujratSeedProduct("prod_everyday", "نیسلے ایوری ڈے (85 گرام)", "Nestle Everyday 85g", ProductCategory.TEA_DAIRY, "pack", 195.0, 215.0, 10.3, 25.0, 6.0, "8961004000067"),

        // Spices (مصالحہ جات)
        GujratSeedProduct("prod_salt", "نیشنل آیوڈین نمک (800 گرام)", "National Iodized Salt 800g", ProductCategory.SPICES, "pack", 65.0, 80.0, 23.1, 40.0, 10.0, "8961005000019"),
        GujratSeedProduct("prod_red_chilli", "لال مرچ پاؤڈر (200 گرام)", "Red Chilli Powder 200g", ProductCategory.SPICES, "pack", 240.0, 275.0, 14.6, 20.0, 5.0, "8961005000026"),
        GujratSeedProduct("prod_turmeric", "ہلدی پاؤڈر (100 گرام)", "Turmeric Powder 100g", ProductCategory.SPICES, "pack", 120.0, 145.0, 20.8, 22.0, 5.0, "8961005000033"),
        GujratSeedProduct("prod_shan_biryani", "شان بریانی مصالحہ", "Shan Biryani Masala", ProductCategory.SPICES, "pack", 125.0, 140.0, 12.0, 35.0, 8.0, "8961005000040"),
        GujratSeedProduct("prod_black_pepper", "کالی مرچ ثابت (100 گرام)", "Whole Black Pepper 100g", ProductCategory.SPICES, "pack", 260.0, 300.0, 15.4, 15.0, 4.0, "8961005000057"),

        // Soap & Cleaning (صابن اور صفائی)
        GujratSeedProduct("prod_surf_excel", "سرف ایکسل (500 گرام)", "Surf Excel 500g", ProductCategory.SOAP_CLEANING, "pack", 290.0, 320.0, 10.3, 28.0, 6.0, "8961006000016"),
        GujratSeedProduct("prod_lifebuoy", "لائف بوائے صابن", "Lifebuoy Soap Bar", ProductCategory.SOAP_CLEANING, "piece", 115.0, 130.0, 13.0, 40.0, 10.0, "8961006000023"),
        GujratSeedProduct("prod_lux", "لکس صابن", "Lux Beauty Soap", ProductCategory.SOAP_CLEANING, "piece", 135.0, 150.0, 11.1, 35.0, 8.0, "8961006000030"),
        GujratSeedProduct("prod_lemon_max", "لیمن میکس برتن صابن", "Lemon Max Dishwash Bar", ProductCategory.SOAP_CLEANING, "piece", 65.0, 80.0, 23.1, 45.0, 10.0, "8961006000047"),
        GujratSeedProduct("prod_colgate", "کولگیٹ ٹوتھ پیسٹ", "Colgate Toothpaste 75g", ProductCategory.SOAP_CLEANING, "piece", 185.0, 210.0, 13.5, 20.0, 5.0, "8961006000054"),

        // Drinks & Snacks (مشروبات و بسکٹ)
        GujratSeedProduct("prod_rooh_afza", "روح افزا (800 ملی لیٹر)", "Rooh Afza 800ml", ProductCategory.DRINKS_SNACKS, "bottle", 490.0, 530.0, 8.2, 16.0, 4.0, "8961007000013"),
        GujratSeedProduct("prod_pepsi_15l", "پیپسی (1.5 لیٹر)", "Pepsi 1.5L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 190.0, 210.0, 10.5, 24.0, 6.0, "8961007000020"),
        GujratSeedProduct("prod_sooper", "پیک فریز سوپر بسکٹ", "Peek Freans Sooper Pack", ProductCategory.DRINKS_SNACKS, "pack", 32.0, 40.0, 25.0, 50.0, 12.0, "8961007000037"),
        GujratSeedProduct("prod_knorr", "کنور نوڈلز چٹ پٹا", "Knorr Chatt Patta Noodles", ProductCategory.DRINKS_SNACKS, "pack", 52.0, 60.0, 15.4, 40.0, 10.0, "8961007000044")
    )

    /**
     * Calculates selling price rounded cleanly to nearest 5 PKR for Pakistani Kiryana pricing.
     */
    fun calculateAutoSellingPrice(costPrice: Double, marginPercent: Double): Double {
        if (costPrice <= 0.0) return 0.0
        val raw = costPrice * (1.0 + marginPercent / 100.0)
        return ((raw / 5.0).roundToInt() * 5.0).coerceAtLeast(costPrice)
    }

    /**
     * Gujrat Mandi Reference Cost Rates lookup by barcode or docId for automatic rate sync.
     */
    fun gujratMarketCostLookup(product: Product): Double {
        val matched = initialProducts.firstOrNull {
            (product.barcode.isNotBlank() && it.barcode == product.barcode) ||
                it.docId == product.id ||
                it.nameUrdu == product.nameUrdu
        }
        return matched?.costPrice ?: product.costPrice
    }
}
