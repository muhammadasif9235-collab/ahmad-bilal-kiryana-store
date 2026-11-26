package com.example.data

import com.example.i18n.AppLanguage

enum class BusinessType(
    val code: String,
    val titleUrdu: String,
    val titleEnglish: String,
    val subtitleUrdu: String,
    val subtitleEnglish: String
) {
    KIRYANA(
        "kiryana",
        "کریانہ و جنرل سٹور",
        "Kiryana & General Store",
        "آٹا، چینی، گھی، چاول، دالیں، صابن، چائے",
        "Flour, Sugar, Ghee, Rice, Pulses, Soap, Tea"
    ),
    PANSAR(
        "pansar",
        "پنسار و جڑی بوٹیاں",
        "Pansar & Herbal Store",
        "دیسی شہد، کلونجی، اسپغول، جڑی بوٹیاں، روغن",
        "Honey, Black Seed, Psyllium, Herbs, Oils"
    ),
    COSMETICS(
        "cosmetics",
        "منیاری و کاسمیٹکس",
        "Cosmetics & General",
        "فیس کریم، لوشن، پرفیوم، شیمپو، چوڑیاں، میک اپ",
        "Creams, Lotion, Perfume, Shampoo, Bangles"
    ),
    CLOTH(
        "cloth",
        "کپڑا و گارمنٹس",
        "Cloth & Garments",
        "لٹھا، واش اینڈ ویئر، لان سوٹ، کاٹن، کھدر، شال",
        "Latha, Wash & Wear, Lawn, Cotton, Khaddar"
    ),
    SHOES(
        "shoes",
        "جوتا و چپل سٹور",
        "Shoes & Footwear",
        "جینٹس شوز، لیڈیز سینڈل، پشاوری چپل، جوگرز",
        "Gents Shoes, Ladies Sandals, Chappal, Joggers"
    ),
    CROCKERY(
        "crockery",
        "کراکری و برتن سٹور",
        "Crockery & Utensils",
        "برتن، جگ، گلاس، سٹیل دیگچیاں، ڈنر سیٹ، کٹورے",
        "Utensils, Jugs, Glasses, Steel Degchis, Dinner Sets"
    ),
    AUTO_PARTS(
        "auto_parts",
        "آٹو پارٹس و موٹرسائیکل",
        "Auto & Bike Parts",
        "موبل آئل، بریک شو، چین گراری، پلگ، ٹیوب، ٹائر",
        "Engine Oil, Brake Shoe, Chain Set, Plug, Tyres"
    ),
    MOBILE_ELECTRONICS(
        "mobile_electronics",
        "موبائل و الیکٹرانکس",
        "Mobile & Electronics",
        "چارجر، ہینڈز فری، ڈیٹا کیبل، پاور بینک، کور",
        "Chargers, Handsfree, Cables, Power Bank, Covers"
    ),
    HARDWARE_PAINT(
        "hardware_paint",
        "ہارڈویئر و پینٹ سٹور",
        "Hardware & Paint",
        "وال پینٹ، برش، کیل، پیچ، لاک، پائپ، نلکا",
        "Wall Paint, Brush, Nails, Locks, Pipes, Taps"
    ),
    ELECTRIC_SOLAR(
        "electric_solar",
        "الیکٹرک و سولر سٹور",
        "Electric & Solar",
        "ایل ای ڈی بلب، پنکھا، سوئچ بورڈ، تار، بریکر",
        "LED Bulbs, Fans, Switch Boards, Wires, Breakers"
    ),
    PHARMACY(
        "pharmacy",
        "میڈیکل و فارمیسی",
        "Medical & Pharmacy",
        "ٹیبلٹس، سیرپ، پٹی، سرنج، تھرمامیٹر، فرسٹ ایڈ",
        "Tablets, Syrups, Bandages, Syringes, First Aid"
    ),
    BAKERY_SWEETS(
        "bakery_sweets",
        "بیکری و مٹھائی",
        "Bakery & Sweets",
        "ڈبل روٹی، رس، کیک، بسکٹ، برفی، گلاب جامن، سموسہ",
        "Bread, Rusk, Cake, Biscuits, Mithai, Samosa"
    ),
    VEGETABLES_FRUITS(
        "vegetables_fruits",
        "سبزی و فروٹ منڈی",
        "Vegetables & Fruits",
        "آلو، پیاز، ٹماٹر، ادرک، لہسن، سیب، کیلا، آم",
        "Potato, Onion, Tomato, Ginger, Apple, Banana"
    ),
    MEAT_POULTRY(
        "meat_poultry",
        "پولٹری و گوشت شاپ",
        "Meat & Poultry Shop",
        "برائلر مرغی، دیسی مرغی، مٹن، بیف، انڈے",
        "Broiler Chicken, Desi Chicken, Mutton, Beef, Eggs"
    ),
    STATIONERY(
        "stationery",
        "اسٹیشنری و بک شاپ",
        "Stationery & Books",
        "کاپیاں، رجسٹر، بال پین، پنسل، جیومیٹری بکس",
        "Notebooks, Registers, Pens, Pencils, Geometry"
    ),
    AGRICULTURE(
        "agriculture",
        "کھاد و زرعی ادویات",
        "Agri & Fertilizer",
        "یوریا کھاد، ڈی اے پی، بیج، زرعی اسپرے",
        "Urea, DAP Fertilizer, Seeds, Agri Sprays"
    );

    fun label(language: AppLanguage): String =
        if (language == AppLanguage.URDU) titleUrdu else titleEnglish

    fun subtitle(language: AppLanguage): String =
        if (language == AppLanguage.URDU) subtitleUrdu else subtitleEnglish

    companion object {
        fun fromCode(code: String?): BusinessType =
            entries.firstOrNull { it.code.equals(code?.trim(), ignoreCase = true) } ?: KIRYANA
    }
}

data class BusinessPresetItem(
    val nameUrdu: String,
    val nameEnglish: String,
    val unit: String,
    val costPrice: Double,
    val sellingPrice: Double,
    val initialStock: Double,
    val lowStockThreshold: Double,
    val categoryName: String = "OTHER",
    val isCustomAdded: Boolean = false
)

object BusinessCatalog {

    private val customItemsByType: MutableMap<BusinessType, MutableList<BusinessPresetItem>> =
        mutableMapOf()

    fun addCustomPresetItem(
        type: BusinessType,
        nameUrdu: String,
        nameEnglish: String = nameUrdu,
        unit: String = "piece",
        costPrice: Double = 0.0,
        sellingPrice: Double = 0.0,
        initialStock: Double = 10.0,
        lowStockThreshold: Double = 2.0,
        categoryName: String = "CUSTOM"
    ): BusinessPresetItem? {
        val cleanUrdu = nameUrdu.trim()
        val cleanEnglish = nameEnglish.trim().ifBlank { cleanUrdu }
        if (cleanUrdu.isEmpty() && cleanEnglish.isEmpty()) return null

        val newItem = BusinessPresetItem(
            nameUrdu = cleanUrdu.ifBlank { cleanEnglish },
            nameEnglish = cleanEnglish,
            unit = unit.trim().ifBlank { "piece" },
            costPrice = costPrice.coerceAtLeast(0.0),
            sellingPrice = sellingPrice.coerceAtLeast(0.0),
            initialStock = initialStock.coerceAtLeast(0.0),
            lowStockThreshold = lowStockThreshold.coerceAtLeast(0.0),
            categoryName = categoryName.trim().ifBlank { "CUSTOM" },
            isCustomAdded = true
        )

        val list = customItemsByType.getOrPut(type) { mutableListOf() }
        list.removeAll {
            it.nameUrdu.equals(newItem.nameUrdu, ignoreCase = true) &&
                it.nameEnglish.equals(newItem.nameEnglish, ignoreCase = true)
        }
        list.add(0, newItem)
        return newItem
    }

    fun editablePresetsFor(type: BusinessType): List<BusinessPresetItem> {
        val customList = customItemsByType[type].orEmpty()
        return customList + presetsFor(type)
    }

    fun presetsFor(type: BusinessType): List<BusinessPresetItem> = when (type) {
        BusinessType.KIRYANA -> listOf(
            BusinessPresetItem("آٹا (20 کلو تھیلا)", "Flour 20kg Bag", "bag", 1750.0, 1850.0, 25.0, 5.0),
            BusinessPresetItem("چینی دانے دار", "White Sugar", "kg", 138.0, 148.0, 100.0, 15.0),
            BusinessPresetItem("بناسپتی گھی (1 کلو)", "Banaspati Ghee 1kg", "pack", 495.0, 525.0, 40.0, 8.0),
            BusinessPresetItem("کوکنگ آئل (1 لیٹر)", "Cooking Oil 1L", "pack", 510.0, 540.0, 40.0, 8.0),
            BusinessPresetItem("سپر کرنل باسمتی چاول", "Super Kernel Basmati Rice", "kg", 270.0, 300.0, 60.0, 10.0),
            BusinessPresetItem("دال چنا", "Chana Dal", "kg", 240.0, 270.0, 40.0, 8.0),
            BusinessPresetItem("دال مسور", "Masoor Dal", "kg", 260.0, 290.0, 35.0, 6.0),
            BusinessPresetItem("چائے کی پتی (200 گرام)", "Tea Pack 200g", "pack", 310.0, 340.0, 30.0, 6.0),
            BusinessPresetItem("کپڑے دھونے والا سرف (500 گرام)", "Washing Powder 500g", "pack", 175.0, 195.0, 35.0, 6.0),
            BusinessPresetItem("نہانے والا صابن", "Bath Soap", "piece", 95.0, 110.0, 48.0, 10.0),
            BusinessPresetItem("آئیوڈین نمک (800 گرام)", "Iodized Salt 800g", "pack", 45.0, 55.0, 50.0, 10.0),
            BusinessPresetItem("لال مرچ پاؤڈر (200 گرام)", "Red Chilli Powder 200g", "pack", 190.0, 220.0, 25.0, 5.0)
        )
        BusinessType.PANSAR -> listOf(
            BusinessPresetItem("خالص دیسی شہد (500 گرام)", "Pure Desi Honey 500g", "pack", 850.0, 1050.0, 15.0, 3.0),
            BusinessPresetItem("کلونجی (100 گرام)", "Black Seed Kalonji 100g", "pack", 90.0, 120.0, 30.0, 5.0),
            BusinessPresetItem("اسپغول چھلکا (100 گرام)", "Psyllium Husk 100g", "pack", 220.0, 260.0, 25.0, 5.0),
            BusinessPresetItem("تخم ملنگا", "Basil Seeds Tukhm Malanga", "kg", 650.0, 780.0, 15.0, 3.0),
            BusinessPresetItem("عرقِ گلاب خالص", "Pure Rose Water Bottle", "piece", 110.0, 150.0, 30.0, 6.0),
            BusinessPresetItem("روغنِ بادام شیریں", "Sweet Almond Oil", "piece", 280.0, 350.0, 20.0, 4.0),
            BusinessPresetItem("دیسی اجوائن", "Carom Seeds Ajwain", "kg", 380.0, 460.0, 15.0, 3.0),
            BusinessPresetItem("سونف سبز", "Fennel Seeds Saunf", "kg", 420.0, 500.0, 20.0, 4.0)
        )
        BusinessType.COSMETICS -> listOf(
            BusinessPresetItem("فیس بیوٹی کریم", "Face Beauty Cream", "piece", 220.0, 280.0, 30.0, 6.0),
            BusinessPresetItem("باڈی لوشن (200ml)", "Moisturizing Lotion 200ml", "piece", 260.0, 320.0, 24.0, 5.0),
            BusinessPresetItem("لیڈیز پرفیوم اسپرے", "Ladies Perfume Spray", "piece", 450.0, 580.0, 20.0, 4.0),
            BusinessPresetItem("ہربل شیمپو بوتل", "Herbal Shampoo Bottle", "piece", 310.0, 370.0, 25.0, 5.0),
            BusinessPresetItem("آملہ ہیئر آئل (200ml)", "Amla Hair Oil 200ml", "piece", 180.0, 220.0, 30.0, 6.0),
            BusinessPresetItem("کانچ کی چوڑیاں سیٹ", "Glass Bangles Set", "pack", 140.0, 200.0, 40.0, 8.0),
            BusinessPresetItem("فیس پاؤڈر کمپیکٹ", "Compact Face Powder", "piece", 240.0, 320.0, 20.0, 4.0),
            BusinessPresetItem("نیل پالش", "Nail Polish", "piece", 80.0, 120.0, 50.0, 10.0)
        )
        BusinessType.CLOTH -> listOf(
            BusinessPresetItem("سفید لٹھا سوٹ (4.5 میٹر)", "White Latha Suit 4.5m", "piece", 1650.0, 2100.0, 25.0, 5.0),
            BusinessPresetItem("واش اینڈ ویئر سوٹ", "Gents Wash & Wear Suit", "piece", 1850.0, 2350.0, 30.0, 5.0),
            BusinessPresetItem("لیڈیز تھری پیس لان سوٹ", "Ladies 3-Piece Lawn Suit", "piece", 2200.0, 2850.0, 35.0, 6.0),
            BusinessPresetItem("کاٹن سوٹ (جینٹس)", "Gents Cotton Suit", "piece", 1950.0, 2500.0, 20.0, 4.0),
            BusinessPresetItem("گرم شال (سردیوں کے لیے)", "Winter Warm Shawl", "piece", 1100.0, 1500.0, 20.0, 4.0),
            BusinessPresetItem("شفون دوپٹہ", "Chiffon Dupatta", "piece", 450.0, 650.0, 30.0, 6.0),
            BusinessPresetItem("بچوں کا ریڈی میڈ سوٹ", "Kids Readymade Suit", "piece", 850.0, 1150.0, 25.0, 5.0)
        )
        BusinessType.SHOES -> listOf(
            BusinessPresetItem("جینٹس لیدر شوز", "Gents Leather Shoes", "piece", 2100.0, 2750.0, 20.0, 4.0),
            BusinessPresetItem("پشاوری چپل", "Peshawari Chappal", "piece", 1450.0, 1950.0, 25.0, 5.0),
            BusinessPresetItem("لیڈیز فینسی سینڈل", "Ladies Fancy Sandal", "piece", 1150.0, 1600.0, 30.0, 6.0),
            BusinessPresetItem("اسپورٹس جوگرز", "Sports Joggers", "piece", 1800.0, 2400.0, 20.0, 4.0),
            BusinessPresetItem("بچوں کے سکول شوز", "Kids School Shoes", "piece", 850.0, 1150.0, 30.0, 6.0),
            BusinessPresetItem("کاٹن جرابیں جوڑا", "Cotton Socks Pair", "piece", 90.0, 140.0, 60.0, 12.0)
        )
        BusinessType.CROCKERY -> listOf(
            BusinessPresetItem("سٹیل کی دیگچی سیٹ", "Stainless Steel Degchi Set", "set", 2400.0, 3100.0, 15.0, 3.0),
            BusinessPresetItem("پانی کا جگ (پلاسٹک / شیشہ)", "Water Jug", "piece", 320.0, 450.0, 25.0, 5.0),
            BusinessPresetItem("پانی پینے کے گلاس (6 عدد)", "Drinking Glasses Set of 6", "pack", 540.0, 720.0, 30.0, 6.0),
            BusinessPresetItem("چینی / میلامائن ڈنر سیٹ", "Dinner Set", "set", 4200.0, 5500.0, 10.0, 2.0),
            BusinessPresetItem("سالن / چاول کے کٹورے (6 عدد)", "Bowls Set of 6", "pack", 480.0, 650.0, 25.0, 5.0),
            BusinessPresetItem("کھانے کے چمچ (درجن)", "Dining Spoons Dozen", "dozen", 360.0, 500.0, 30.0, 6.0),
            BusinessPresetItem("چائے کی کیتلی / فلاسک", "Tea Kettle / Flask", "piece", 780.0, 1050.0, 18.0, 4.0),
            BusinessPresetItem("کھانا گرم رکھنے والا ہاٹ پاٹ", "Hot Pot", "piece", 950.0, 1250.0, 15.0, 3.0)
        )
        BusinessType.AUTO_PARTS -> listOf(
            BusinessPresetItem("موبل آئل 70cc/125cc (0.7L)", "Motorcycle Engine Oil 0.7L", "piece", 680.0, 820.0, 40.0, 8.0),
            BusinessPresetItem("موٹرسائیکل بریک شو سیٹ", "Bike Brake Shoe Set", "pack", 260.0, 350.0, 30.0, 6.0),
            BusinessPresetItem("چین گراری سیٹ 70cc", "Chain Sprocket Kit 70cc", "pack", 1150.0, 1450.0, 15.0, 3.0),
            BusinessPresetItem("اسپارک پلگ (NGK)", "Spark Plug NGK", "piece", 280.0, 360.0, 40.0, 8.0),
            BusinessPresetItem("موٹرسائیکل ٹیوب (2.50-17)", "Bike Tube 2.50-17", "piece", 480.0, 620.0, 25.0, 5.0),
            BusinessPresetItem("ہیڈ لائٹ ایل ای ڈی بلب", "Headlight LED Bulb", "piece", 220.0, 320.0, 30.0, 6.0),
            BusinessPresetItem("ایئر فلٹر فوم", "Air Filter Foam", "piece", 90.0, 150.0, 40.0, 8.0)
        )
        BusinessType.MOBILE_ELECTRONICS -> listOf(
            BusinessPresetItem("فاسٹ چارجر مکمل (Type-C)", "Fast Charger Complete Type-C", "piece", 550.0, 850.0, 25.0, 5.0),
            BusinessPresetItem("اوریجنل ہینڈز فری", "Stereo Handsfree", "piece", 220.0, 350.0, 40.0, 8.0),
            BusinessPresetItem("فاسٹ ڈیٹا کیبل", "Fast Data Cable", "piece", 150.0, 250.0, 50.0, 10.0),
            BusinessPresetItem("گلاس اسکرین پروٹیکٹر", "Tempered Glass Protector", "piece", 70.0, 150.0, 60.0, 12.0),
            BusinessPresetItem("موبائل سلیکون بیک کور", "Silicone Mobile Cover", "piece", 140.0, 250.0, 50.0, 10.0),
            BusinessPresetItem("پاور بینک (10000mAh)", "Power Bank 10000mAh", "piece", 1850.0, 2400.0, 10.0, 2.0)
        )
        BusinessType.HARDWARE_PAINT -> listOf(
            BusinessPresetItem("ڈسٹیمپر وال پینٹ (گیلن)", "Wall Distemper Paint Gallon", "piece", 1150.0, 1400.0, 20.0, 4.0),
            BusinessPresetItem("پینٹ برش (3 انچ)", "Paint Brush 3 Inch", "piece", 140.0, 200.0, 30.0, 6.0),
            BusinessPresetItem("لوہے کے کیل", "Iron Nails", "kg", 260.0, 320.0, 40.0, 8.0),
            BusinessPresetItem("دروازے کا تالا (ہیوی ڈیوٹی)", "Heavy Duty Door Padlock", "piece", 380.0, 500.0, 25.0, 5.0),
            BusinessPresetItem("پلاسٹک ٹوٹی / نلکا", "PVC Water Tap", "piece", 120.0, 180.0, 35.0, 7.0),
            BusinessPresetItem("پی وی سی پائپ (1 انچ لینتھ)", "PVC Pipe 1 Inch", "piece", 420.0, 520.0, 30.0, 6.0)
        )
        BusinessType.ELECTRIC_SOLAR -> listOf(
            BusinessPresetItem("ایل ای ڈی بلب (12 واٹ)", "LED Bulb 12W", "piece", 180.0, 240.0, 50.0, 10.0),
            BusinessPresetItem("ایل ای ڈی ٹیوب لائٹ (4 فٹ)", "LED Tube Light 4ft", "piece", 450.0, 580.0, 25.0, 5.0),
            BusinessPresetItem("سوئچ بورڈ (6 بٹن مکمل)", "Switch Board 6-Way Complete", "piece", 340.0, 450.0, 25.0, 5.0),
            BusinessPresetItem("بجلی کی تار 3/29 (کوائل)", "Electric Wire 3/29 Coil", "pack", 2600.0, 3100.0, 12.0, 3.0),
            BusinessPresetItem("سرکٹ بریکر (32 ایمپیئر)", "Circuit Breaker 32A", "piece", 380.0, 480.0, 20.0, 4.0),
            BusinessPresetItem("ایکسٹینشن لیڈ بورڈ", "Extension Power Strip", "piece", 420.0, 550.0, 20.0, 4.0)
        )
        BusinessType.PHARMACY -> listOf(
            BusinessPresetItem("پیناڈول گولیاں (پتہ)", "Panadol Tablets Strip", "pack", 28.0, 35.0, 100.0, 20.0),
            BusinessPresetItem("بروفن سیرپ بوتل", "Brufen Syrup Bottle", "piece", 95.0, 115.0, 30.0, 6.0),
            BusinessPresetItem("کاٹن بینڈیج پٹی", "Cotton Bandage Roll", "piece", 35.0, 50.0, 50.0, 10.0),
            BusinessPresetItem("ڈسپوزایبل سرنج (5cc)", "Disposable Syringe 5cc", "piece", 12.0, 20.0, 100.0, 20.0),
            BusinessPresetItem("او آر ایس ساشے", "ORS Sachet", "pack", 22.0, 30.0, 80.0, 15.0),
            BusinessPresetItem("سینی پلاسٹ پٹی", "Adhesive Plaster Strip", "piece", 8.0, 15.0, 100.0, 20.0)
        )
        BusinessType.BAKERY_SWEETS -> listOf(
            BusinessPresetItem("بڑی ڈبل روٹی (بریڈ)", "Large Milky Bread", "piece", 135.0, 160.0, 30.0, 6.0),
            BusinessPresetItem("کیک رس پیکٹ", "Cake Rusk Pack", "pack", 160.0, 200.0, 25.0, 5.0),
            BusinessPresetItem("خالص کھویا برفی", "Pure Khoya Barfi", "kg", 950.0, 1200.0, 15.0, 3.0),
            BusinessPresetItem("گلاب جامن", "Gulab Jamun", "kg", 800.0, 1000.0, 15.0, 3.0),
            BusinessPresetItem("مکس نمکو پیکٹ", "Mix Nimko Pack", "pack", 110.0, 150.0, 30.0, 6.0),
            BusinessPresetItem("کولڈ ڈرنک (1.5 لیٹر)", "Cold Drink 1.5L", "piece", 165.0, 190.0, 36.0, 8.0)
        )
        BusinessType.VEGETABLES_FRUITS -> listOf(
            BusinessPresetItem("آلو نیا", "Fresh Potatoes", "kg", 65.0, 85.0, 100.0, 15.0),
            BusinessPresetItem("پیاز سرخ", "Red Onions", "kg", 95.0, 120.0, 100.0, 15.0),
            BusinessPresetItem("ٹماٹر تازہ", "Fresh Tomatoes", "kg", 110.0, 140.0, 50.0, 10.0),
            BusinessPresetItem("ادرک دیسی", "Fresh Ginger", "kg", 480.0, 580.0, 15.0, 3.0),
            BusinessPresetItem("لہسن دیسی", "Fresh Garlic", "kg", 420.0, 520.0, 15.0, 3.0),
            BusinessPresetItem("سیب کالا کولو", "Kala Kulu Apples", "kg", 220.0, 280.0, 40.0, 8.0),
            BusinessPresetItem("کیلا (درجن)", "Fresh Bananas Dozen", "dozen", 120.0, 160.0, 30.0, 6.0)
        )
        BusinessType.MEAT_POULTRY -> listOf(
            BusinessPresetItem("برائلر مرغی صاف گوشت", "Broiler Chicken Meat", "kg", 540.0, 610.0, 50.0, 10.0),
            BusinessPresetItem("دیسی مرغی", "Desi Chicken", "kg", 1150.0, 1350.0, 15.0, 3.0),
            BusinessPresetItem("بکرا گوشت (مٹن)", "Fresh Mutton", "kg", 1850.0, 2100.0, 20.0, 4.0),
            BusinessPresetItem("بڑا گوشت (بیف)", "Fresh Beef", "kg", 950.0, 1100.0, 30.0, 6.0),
            BusinessPresetItem("فارمی انڈے (درجن)", "Farm Eggs Dozen", "dozen", 290.0, 330.0, 30.0, 6.0)
        )
        BusinessType.STATIONERY -> listOf(
            BusinessPresetItem("اسکول کاپی (160 صفحات)", "School Notebook 160 Pages", "piece", 95.0, 130.0, 60.0, 12.0),
            BusinessPresetItem("رجسٹر بڑا (300 صفحات)", "Large Register 300 Pages", "piece", 220.0, 290.0, 30.0, 6.0),
            BusinessPresetItem("بال پین (نیلا / کالا)", "Ballpoint Pen", "piece", 15.0, 25.0, 100.0, 20.0),
            BusinessPresetItem("کچی پنسل", "Lead Pencil", "piece", 12.0, 20.0, 100.0, 20.0),
            BusinessPresetItem("جیومیٹری بکس", "Geometry Box", "piece", 180.0, 250.0, 25.0, 5.0)
        )
        BusinessType.AGRICULTURE -> listOf(
            BusinessPresetItem("یوریا کھاد بوری (50 کلو)", "Urea Fertilizer Bag 50kg", "bag", 4450.0, 4650.0, 30.0, 5.0),
            BusinessPresetItem("ڈی اے پی کھاد بوری (50 کلو)", "DAP Fertilizer Bag 50kg", "bag", 11800.0, 12200.0, 20.0, 4.0),
            BusinessPresetItem("جڑی بوٹی مار اسپرے (1 لیٹر)", "Herbicide Spray 1L", "piece", 1350.0, 1600.0, 25.0, 5.0),
            BusinessPresetItem("ہائبرڈ مکئی بیج پیکٹ", "Hybrid Maize Seed Pack", "pack", 3200.0, 3600.0, 15.0, 3.0)
        )
    }
}
