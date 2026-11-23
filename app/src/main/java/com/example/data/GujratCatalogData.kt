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
        
        // 6. Baby Diapers / Pampers (پیمپرز: کیئر بیبی، رائل زون، کین بیبی — تمام سائز)
        item("prod_carebaby_s", "کیئر بیبی پیمپر سمال (Small)", "Care Baby Diaper Small Pack", ProductCategory.SOAP_CLEANING, "pack", 480.0, 10.0, 4.0, "8961006000061"),
        item("prod_carebaby_m", "کیئر بیبی پیمپر میڈیم (Medium)", "Care Baby Diaper Medium Pack", ProductCategory.SOAP_CLEANING, "pack", 520.0, 10.0, 4.0, "8961006000078"),
        item("prod_carebaby_l", "کیئر بیبی پیمپر لارج (Large)", "Care Baby Diaper Large Pack", ProductCategory.SOAP_CLEANING, "pack", 560.0, 10.0, 4.0, "8961006000085"),
        item("prod_carebaby_xl", "کیئر بیبی پیمپر ایکسٹرا لارج (XL)", "Care Baby Diaper Extra Large Pack", ProductCategory.SOAP_CLEANING, "pack", 600.0, 10.0, 4.0, "8961006000092"),
        item("prod_royalzone_s", "رائل زون پیمپر سمال (Small)", "Royal Zone Diaper Small Pack", ProductCategory.SOAP_CLEANING, "pack", 450.0, 11.0, 4.0, "8961006000108"),
        item("prod_royalzone_m", "رائل زون پیمپر میڈیم (Medium)", "Royal Zone Diaper Medium Pack", ProductCategory.SOAP_CLEANING, "pack", 490.0, 11.0, 4.0, "8961006000115"),
        item("prod_royalzone_l", "رائل زون پیمپر لارج (Large)", "Royal Zone Diaper Large Pack", ProductCategory.SOAP_CLEANING, "pack", 530.0, 11.0, 4.0, "8961006000122"),
        item("prod_royalzone_xl", "رائل زون پیمپر ایکسٹرا لارج (XL)", "Royal Zone Diaper Extra Large Pack", ProductCategory.SOAP_CLEANING, "pack", 570.0, 11.0, 4.0, "8961006000139"),
        item("prod_canbebe_m", "کین بیبی پیمپر میڈیم (Medium)", "Canbebe Diaper Medium Pack", ProductCategory.SOAP_CLEANING, "pack", 620.0, 9.0, 3.0, "8961006000146"),
        item("prod_canbebe_l", "کین بیبی پیمپر لارج (Large)", "Canbebe Diaper Large Pack", ProductCategory.SOAP_CLEANING, "pack", 660.0, 9.0, 3.0, "8961006000153"),
        item("prod_canbebe_xl", "کین بیبی پیمپر ایکسٹرا لارج (XL)", "Canbebe Diaper Extra Large Pack", ProductCategory.SOAP_CLEANING, "pack", 700.0, 9.0, 3.0, "8961006000160"),

        // 7. Soap, Surf & Personal Care (صابن، سرف اور صفائی)
        item("prod_surf_excel_500", "سرف ایکسل (500 گرام)", "Surf Excel 500g", ProductCategory.SOAP_CLEANING, "pack", 290.0, 10.0, 5.0, "8961006000016"),
        item("prod_surf_excel_1kg", "سرف ایکسل (1 کلو)", "Surf Excel 1kg", ProductCategory.SOAP_CLEANING, "pack", 560.0, 9.0, 4.0, "8961006000177"),
        item("prod_ariel_500", "ایریل سرف (500 گرام)", "Ariel Washing Powder 500g", ProductCategory.SOAP_CLEANING, "pack", 285.0, 10.0, 5.0, "8961006000184"),
        item("prod_brite_500", "برائٹ ٹوٹل سرف (500 گرام)", "Brite Total 500g", ProductCategory.SOAP_CLEANING, "pack", 265.0, 10.0, 5.0, "8961006000191"),
        item("prod_lifebuoy", "لائف بوائے صابن", "Lifebuoy Soap Bar", ProductCategory.SOAP_CLEANING, "piece", 115.0, 12.0, 8.0, "8961006000023"),
        item("prod_lux", "لکس صابن", "Lux Beauty Soap", ProductCategory.SOAP_CLEANING, "piece", 135.0, 11.0, 8.0, "8961006000030"),
        item("prod_safeguard", "سیف گارڈ صابن", "Safeguard Antibacterial Soap", ProductCategory.SOAP_CLEANING, "piece", 145.0, 10.0, 6.0, "8961006000207"),
        item("prod_lemon_max", "لیمن میکس برتن صابن", "Lemon Max Dishwash Bar", ProductCategory.SOAP_CLEANING, "piece", 65.0, 15.0, 10.0, "8961006000047"),
        item("prod_colgate", "کولگیٹ ٹوتھ پیسٹ (75 گرام)", "Colgate Toothpaste 75g", ProductCategory.SOAP_CLEANING, "piece", 185.0, 12.0, 5.0, "8961006000054"),

        // 8. Spices & Condiments (مصالحہ جات)
        item("prod_salt", "نیشنل آیوڈین نمک (800 گرام)", "National Iodized Salt 800g", ProductCategory.SPICES, "pack", 65.0, 15.0, 8.0, "8961005000019"),
        item("prod_red_chilli", "لال مرچ پاؤڈر (200 گرام)", "Red Chilli Powder 200g", ProductCategory.SPICES, "pack", 240.0, 12.0, 5.0, "8961005000026"),
        item("prod_red_chilli_loose", "کھلی لال مرچ پسائی", "Loose Red Chilli Powder", ProductCategory.SPICES, "kg", 980.0, 12.0, 4.0, "8961005000064"),
        item("prod_turmeric", "ہلدی پاؤڈر (100 گرام)", "Turmeric Powder 100g", ProductCategory.SPICES, "pack", 120.0, 15.0, 5.0, "8961005000033"),
        item("prod_coriander", "دھنیا پاؤڈر (200 گرام)", "Coriander Powder 200g", ProductCategory.SPICES, "pack", 170.0, 14.0, 5.0, "8961005000071"),
        item("prod_garam_masala", "گرم مصالحہ ثابت (100 گرام)", "Whole Garam Masala 100g", ProductCategory.SPICES, "pack", 210.0, 14.0, 4.0, "8961005000088"),
        item("prod_shan_biryani", "شان بریانی مصالحہ", "Shan Biryani Masala", ProductCategory.SPICES, "pack", 125.0, 12.0, 6.0, "8961005000040"),
        item("prod_shan_qorma", "شان قورمہ مصالحہ", "Shan Qorma Masala", ProductCategory.SPICES, "pack", 125.0, 12.0, 6.0, "8961005000095"),
        item("prod_black_pepper", "کالی مرچ ثابت (100 گرام)", "Whole Black Pepper 100g", ProductCategory.SPICES, "pack", 260.0, 14.0, 4.0, "8961005000057"),

        // 9. Cold Drinks, NR Bottles, Juices & Cigarettes (بوتلیں، جوس اور سگریٹس)
        item("prod_pepsi_1l", "پیپسی بوتل (1 لیٹر)", "Pepsi 1L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 145.0, 10.0, 6.0, "8961007000051"),
        item("prod_pepsi_15l", "پیپسی بوتل (1.5 لیٹر)", "Pepsi 1.5L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 190.0, 10.0, 6.0, "8961007000020"),
        item("prod_pepsi_nr", "پیپسی این آر بوتل (NR / شیشہ)", "Pepsi NR Returnable Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 52.0, 15.0, 12.0, "8961007000068"),
        item("prod_coke_1l", "کوکا کولا بوتل (1 لیٹر)", "Coca-Cola 1L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 145.0, 10.0, 6.0, "8961007000075"),
        item("prod_coke_15l", "کوکا کولا بوتل (1.5 لیٹر)", "Coca-Cola 1.5L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 190.0, 10.0, 6.0, "8961007000082"),
        item("prod_coke_nr", "کوکا کولا این آر بوتل (NR)", "Coca-Cola NR Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 52.0, 15.0, 12.0, "8961007000099"),
        item("prod_sprite_1l", "سپرائٹ بوتل (1 لیٹر)", "Sprite 1L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 145.0, 10.0, 6.0, "8961007000105"),
        item("prod_sprite_15l", "سپرائٹ بوتل (1.5 لیٹر)", "Sprite 1.5L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 190.0, 10.0, 6.0, "8961007000112"),
        item("prod_7up_1l", "سیون اپ بوتل (1 لیٹر)", "7UP 1L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 145.0, 10.0, 6.0, "8961007000129"),
        item("prod_7up_15l", "سیون اپ بوتل (1.5 لیٹر)", "7UP 1.5L Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 190.0, 10.0, 6.0, "8961007000136"),
        item("prod_7up_nr", "سیون اپ این آر بوتل (NR)", "7UP NR Bottle", ProductCategory.DRINKS_SNACKS, "bottle", 52.0, 15.0, 12.0, "8961007000143"),
        item("prod_shezan_1l", "شہزان مینگو جوس (1 لیٹر)", "Shezan Mango Juice 1L", ProductCategory.DRINKS_SNACKS, "pack", 215.0, 10.0, 5.0, "8961007000150"),
        item("prod_shezan_250", "شہزان جوس چھوٹا ڈبہ (250ml)", "Shezan Juice 250ml Pack", ProductCategory.DRINKS_SNACKS, "pack", 55.0, 15.0, 12.0, "8961007000167"),
        item("prod_fruita_1l", "نیسلے فروٹا وائٹلز جوس (1 لیٹر)", "Nestle Fruita Vitals 1L", ProductCategory.DRINKS_SNACKS, "pack", 265.0, 9.0, 4.0, "8961007000174"),
        item("prod_local_juice_1l", "لوکل مینگو / فروٹ جوس (1 لیٹر)", "Local Mango/Fruit Juice 1L", ProductCategory.DRINKS_SNACKS, "pack", 140.0, 14.0, 6.0, "8961007000181"),
        item("prod_rooh_afza", "روح افزا (800 ملی لیٹر)", "Rooh Afza 800ml", ProductCategory.DRINKS_SNACKS, "bottle", 490.0, 8.0, 4.0, "8961007000013"),
        item("prod_capstan_red", "کیپسٹن سرخ سگریٹ (ڈبی)", "Capstan Red Pack (20s)", ProductCategory.DRINKS_SNACKS, "pack", 220.0, 7.0, 10.0, "8961007000198"),
        item("prod_capstan_white", "کیپسٹن سفید سگریٹ (ڈبی)", "Capstan White / Light Pack", ProductCategory.DRINKS_SNACKS, "pack", 220.0, 7.0, 10.0, "8961007000204"),
        item("prod_gold_leaf", "جان پلیئر گولڈ لیف سگریٹ (ڈبی)", "Gold Leaf Cigarette Pack", ProductCategory.DRINKS_SNACKS, "pack", 520.0, 6.0, 6.0, "8961007000211"),
        item("prod_gold_flake", "گولڈ فلیک سگریٹ (ڈبی)", "Gold Flake Cigarette Pack", ProductCategory.DRINKS_SNACKS, "pack", 160.0, 8.0, 10.0, "8961007000228"),
        item("prod_morven_gold", "مورون گولڈ سگریٹ (ڈبی)", "Morven Gold Cigarette Pack", ProductCategory.DRINKS_SNACKS, "pack", 215.0, 7.0, 8.0, "8961007000235"),
        item("prod_red_white", "ریڈ اینڈ وائٹ سگریٹ (ڈبی)", "Red & White Cigarette Pack", ProductCategory.DRINKS_SNACKS, "pack", 175.0, 8.0, 8.0, "8961007000242"),

        // 10. Bakery Snacks, Lay's, Slanty, Kurkure, Papad & Toffees
        item("prod_cream_roll", "بیکری کریم رول (Cream Roll)", "Bakery Cream Roll", ProductCategory.DRINKS_SNACKS, "piece", 32.0, 15.0, 8.0, "8961007000259"),
        item("prod_rusk_pack", "بیکری پاپے / رسک پیک", "Bakery Rusk / Papay Pack", ProductCategory.DRINKS_SNACKS, "pack", 110.0, 12.0, 5.0, "8961007000266"),
        item("prod_sooper", "پیک فریز سوپر بسکٹ", "Peek Freans Sooper Pack", ProductCategory.DRINKS_SNACKS, "pack", 32.0, 15.0, 12.0, "8961007000037"),
        item("prod_knorr", "کنور نوڈلز چٹ پٹا", "Knorr Chatt Patta Noodles", ProductCategory.DRINKS_SNACKS, "pack", 52.0, 12.0, 10.0, "8961007000044"),
        item("prod_lays_masala_20", "لیز مصالحہ (20 روپے والا)", "Lay's Masala Rs. 20 Pack", ProductCategory.DRINKS_SNACKS, "pack", 17.0, 15.0, 12.0, "8961007000273"),
        item("prod_lays_masala_30", "لیز مصالحہ (30 روپے والا)", "Lay's Masala Rs. 30 Pack", ProductCategory.DRINKS_SNACKS, "pack", 25.0, 15.0, 10.0, "8961007000280"),
        item("prod_lays_masala_50", "لیز مصالحہ (50 روپے والا)", "Lay's Masala Rs. 50 Pack", ProductCategory.DRINKS_SNACKS, "pack", 42.0, 15.0, 8.0, "8961007000297"),
        item("prod_lays_masala_70", "لیز مصالحہ (70 روپے والا)", "Lay's Masala Rs. 70 Pack", ProductCategory.DRINKS_SNACKS, "pack", 60.0, 14.0, 6.0, "8961007000303"),
        item("prod_lays_masala_100", "لیز مصالحہ (100 روپے والا)", "Lay's Masala Rs. 100 Pack", ProductCategory.DRINKS_SNACKS, "pack", 85.0, 15.0, 5.0, "8961007000310"),
        item("prod_lays_yogurt_30", "لیز یوگرٹ اینڈ ہرب (30 روپے والا)", "Lay's Yogurt & Herb Rs. 30", ProductCategory.DRINKS_SNACKS, "pack", 25.0, 15.0, 10.0, "8961007000327"),
        item("prod_lays_yogurt_50", "لیز یوگرٹ اینڈ ہرب (50 روپے والا)", "Lay's Yogurt & Herb Rs. 50", ProductCategory.DRINKS_SNACKS, "pack", 42.0, 15.0, 8.0, "8961007000334"),
        item("prod_lays_yogurt_70", "لیز یوگرٹ اینڈ ہرب (70 روپے والا)", "Lay's Yogurt & Herb Rs. 70", ProductCategory.DRINKS_SNACKS, "pack", 60.0, 14.0, 6.0, "8961007000341"),
        item("prod_lays_yogurt_100", "لیز یوگرٹ اینڈ ہرب (100 روپے والا)", "Lay's Yogurt & Herb Rs. 100", ProductCategory.DRINKS_SNACKS, "pack", 85.0, 15.0, 5.0, "8961007000358"),
        item("prod_lays_cheese_30", "لیز فرینچ چیز (30 روپے والا)", "Lay's French Cheese Rs. 30", ProductCategory.DRINKS_SNACKS, "pack", 25.0, 15.0, 10.0, "8961007000365"),
        item("prod_lays_cheese_50", "لیز فرینچ چیز (50 روپے والا)", "Lay's French Cheese Rs. 50", ProductCategory.DRINKS_SNACKS, "pack", 42.0, 15.0, 8.0, "8961007000372"),
        item("prod_lays_cheese_100", "لیز فرینچ چیز (100 روپے والا)", "Lay's French Cheese Rs. 100", ProductCategory.DRINKS_SNACKS, "pack", 85.0, 15.0, 5.0, "8961007000389"),
        item("prod_lays_salted_30", "لیز سالٹڈ / نمکین (30 روپے والا)", "Lay's Classic Salted Rs. 30", ProductCategory.DRINKS_SNACKS, "pack", 25.0, 15.0, 10.0, "8961007000396"),
        item("prod_lays_salted_50", "لیز سالٹڈ / نمکین (50 روپے والا)", "Lay's Classic Salted Rs. 50", ProductCategory.DRINKS_SNACKS, "pack", 42.0, 15.0, 8.0, "8961007000402"),
        item("prod_lays_paprika_30", "لیز پپریکا (30 روپے والا)", "Lay's Paprika Rs. 30 Pack", ProductCategory.DRINKS_SNACKS, "pack", 25.0, 15.0, 10.0, "8961007000419"),
        item("prod_lays_paprika_50", "لیز پپریکا (50 روپے والا)", "Lay's Paprika Rs. 50 Pack", ProductCategory.DRINKS_SNACKS, "pack", 42.0, 15.0, 8.0, "8961007000426"),
        item("prod_lays_paprika_100", "لیز پپریکا (100 روپے والا)", "Lay's Paprika Rs. 100 Pack", ProductCategory.DRINKS_SNACKS, "pack", 85.0, 15.0, 5.0, "8961007000433"),
        item("prod_slanty_20", "سلنٹی (20 روپے والا پیک)", "Kolson Slanty Rs. 20 Pack", ProductCategory.DRINKS_SNACKS, "pack", 17.0, 15.0, 12.0, "8961007000440"),
        item("prod_kurkure_20", "کرکرے (20 روپے والا پیک)", "Kurkure Rs. 20 Pack", ProductCategory.DRINKS_SNACKS, "pack", 17.0, 15.0, 12.0, "8961007000457"),
        item("prod_kurkure_40", "کرکرے (40 روپے والا پیک)", "Kurkure Rs. 40 Pack", ProductCategory.DRINKS_SNACKS, "pack", 34.0, 15.0, 10.0, "8961007000464"),
        item("prod_kurkure_60", "کرکرے (60 روپے والا پیک)", "Kurkure Rs. 60 Pack", ProductCategory.DRINKS_SNACKS, "pack", 51.0, 15.0, 8.0, "8961007000471"),
        item("prod_local_papad_sm", "لوکل پاپڑ چھوٹا پیک (10/20 روپے)", "Local Papad Small Pack", ProductCategory.DRINKS_SNACKS, "pack", 8.0, 20.0, 20.0, "8961007000488"),
        item("prod_local_papad_lg", "لوکل پاپڑ بڑا پیکٹ / تھیلا", "Local Papad Family Pack", ProductCategory.DRINKS_SNACKS, "pack", 40.0, 20.0, 10.0, "8961007000495"),
        item("prod_toffees_jar", "بچوں کی ٹافیاں / کینڈیز پیک", "Kids Toffees & Candies Pack", ProductCategory.DRINKS_SNACKS, "pack", 120.0, 20.0, 5.0, "8961007000501")
    )

    fun calculateAutoSellingPrice(costPrice: Double, marginPercent: Double): Double {
        if (costPrice <= 0.0) return 0.0
        val raw = costPrice * (1.0 + marginPercent / 100.0)
        return ((raw / 5.0).roundToInt() * 5.0).coerceAtLeast(costPrice)
    }

    fun gujratMarketCostLookup(product: Product): Double {
        val matched = initialProducts.firstOrNull {
            (product.barcode.isNotBlank() && it.barcode == product.barcode) ||
                it.docId == product.id ||
                it.nameUrdu == product.nameUrdu
        }
        return matched?.marketReferenceCost?.takeIf { it > 0.0 } ?: product.costPrice
    }
}
