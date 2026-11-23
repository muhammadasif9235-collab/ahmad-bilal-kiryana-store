package com.example.data

data class SeedRationItem(
    val docId: String,
    val nameUrdu: String,
    val nameEnglish: String,
    val unit: String,
    val price: Double,
    val sortOrder: Int
)

/**
 * Default eligible grocery items & District Gujrat / Tanda PKR market prices for the
 * Punjab Government Maryam Nawaz Rashan Card (Rs. 3,000/month subsidy via BOP / PESSI / PSER).
 * Every item's name, unit, and price can be manually edited by the shopkeeper.
 */
object MaryamRationCatalogData {

    const val DEFAULT_MONTHLY_SUBSIDY_PKR = 3000.0
    const val DEFAULT_SERVICE_FEE_PKR = 100.0
    const val DEFAULT_NET_RATION_PKR = 2900.0
    const val HELPLINE_NUMBER = "1105"

    val defaultItems: List<SeedRationItem> = listOf(
        SeedRationItem("mn_atta_10kg", "گندم کا آٹا (10 کلو تھیلا)", "Wheat Flour (Atta 10kg Bag)", "bag", 880.0, 1),
        SeedRationItem("mn_atta_20kg", "گندم کا آٹا (20 کلو تھیلا)", "Wheat Flour (Atta 20kg Bag)", "bag", 1750.0, 2),
        SeedRationItem("mn_ghee_1kg", "بناسپتی گھی (1 کلو پیک)", "Banaspati Ghee (1kg Pack)", "kg", 550.0, 3),
        SeedRationItem("mn_oil_1l", "کوکنگ آئل (1 لیٹر پاؤچ)", "Cooking Oil (1L Pouch)", "liter", 570.0, 4),
        SeedRationItem("mn_sugar_1kg", "چینی سفید (1 کلو)", "White Sugar (1kg)", "kg", 155.0, 5),
        SeedRationItem("mn_daal_chana", "دال چنا (1 کلو)", "Daal Chana (1kg)", "kg", 290.0, 6),
        SeedRationItem("mn_daal_masoor", "دال مسور (1 کلو)", "Daal Masoor (1kg)", "kg", 310.0, 7),
        SeedRationItem("mn_daal_moong", "دال مونگ (1 کلو)", "Daal Moong (1kg)", "kg", 330.0, 8),
        SeedRationItem("mn_daal_mash", "دال ماش (1 کلو)", "Daal Mash (1kg)", "kg", 470.0, 9),
        SeedRationItem("mn_rice_basmati", "سپر باسمتی چاول (1 کلو)", "Super Basmati Rice (1kg)", "kg", 330.0, 10),
        SeedRationItem("mn_tea_190g", "چائے کی پتی (190 گرام پیک)", "Black Tea Pack (190g)", "pack", 390.0, 11),
        SeedRationItem("mn_besan_1kg", "خالص بیسن (1 کلو)", "Gram Flour / Besan (1kg)", "kg", 310.0, 12),
        SeedRationItem("mn_chana_black", "کالا / سفید چنا (1 کلو)", "Chickpeas / Chana (1kg)", "kg", 330.0, 13),
        SeedRationItem("mn_salt_800g", "آیوڈین والا نمک (800 گرام)", "Iodized Salt (800g Pack)", "pack", 60.0, 14),
        SeedRationItem("mn_red_chili", "سرخ مرچ پاؤڈر (200 گرام)", "Red Chili Powder (200g)", "pack", 250.0, 15),
        SeedRationItem("mn_turmeric", "ہلدی پاؤڈر (100 گرام)", "Turmeric Powder (100g)", "pack", 120.0, 16),
        SeedRationItem("mn_milk_1l", "دودھ پیک (1 لیٹر)", "UHT Milk Pack (1L)", "liter", 290.0, 17),
        SeedRationItem("mn_soap", "گھریلو صابن (1 عدد)", "Household Soap Bar", "piece", 130.0, 18)
    )

    /**
     * Quick preset bundle (Rs. 2,890) fitting cleanly inside the Rs. 2,900 Net POS Ration limit
     * (Rs. 3,000 total card quota - Rs. 100 service fee deduction):
     * 1x Atta 10kg (880) + 2x Ghee 1kg (1100) + 2x Sugar 1kg (310) + 1x Daal Chana (290)
     * + 1x Besan (310) = Rs. 2,890
     */
    val standard3000PackQuantities: Map<String, Double> = mapOf(
        "mn_atta_10kg" to 1.0,
        "mn_ghee_1kg" to 2.0,
        "mn_sugar_1kg" to 2.0,
        "mn_daal_chana" to 1.0,
        "mn_besan_1kg" to 1.0
    )
}
