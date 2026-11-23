package com.example.services

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.Locale

data class ParsedScannedCode(
    val rawValue: String,
    val kind: ScannedCodeKind,
    val title: String = "",
    val accountOrIban: String = "",
    val amount: Double? = null,
    val reference: String = ""
)

enum class ScannedCodeKind {
    RAAST_EMV_QR,
    BANK_IBAN,
    RATION_OR_CNIC_CARD,
    PRODUCT_BARCODE,
    GENERIC_QR
}

/**
 * Official EMVCo Merchant-Presented QR Code & Pakistan RAAST IBAN payload builder,
 * CRC-16/CCITT-FALSE calculator, ZXing renderer, and smart scanner parser.
 */
object RaastEmvQrHelper {

    const val DEFAULT_MERCHANT_NAME = "Ahmad Bilal Kiryana Store"
    const val DEFAULT_MERCHANT_CITY = "Tanda Gujrat"
    const val CURRENCY_PKR = "586"
    const val COUNTRY_PK = "PK"
    const val MCC_GROCERY_STORE = "5411"
    const val RAAST_GUI = "pk.raast"

    /**
     * Generates a standards-compliant EMVCo / Pakistan RAAST QR payload string with valid CRC-16 checksum.
     */
    fun buildRaastEmvQrPayload(
        iban: String,
        accountNumber: String = "",
        merchantName: String = DEFAULT_MERCHANT_NAME,
        merchantCity: String = DEFAULT_MERCHANT_CITY,
        amountPkr: Double? = null,
        billReference: String = ""
    ): String {
        val cleanIban = normalizeIban(iban)
        val cleanAcc = accountNumber.filter { it.isLetterOrDigit() }.take(24)
        val cleanName = sanitizeAscii(merchantName.ifBlank { DEFAULT_MERCHANT_NAME }).take(25)
        val cleanCity = sanitizeAscii(merchantCity.ifBlank { DEFAULT_MERCHANT_CITY }).take(15)
        val cleanRef = sanitizeAscii(billReference).take(25)

        val sb = StringBuilder()

        // 00: Payload Format Indicator = "01"
        sb.append(tlv("00", "01"))

        // 01: Point of Initiation Method ("12" dynamic when amount present, "11" static otherwise)
        val hasAmount = amountPkr != null && amountPkr > 0.0
        sb.append(tlv("01", if (hasAmount) "12" else "11"))

        // 28: Merchant Account Information (Pakistan RAAST / Direct IBAN routing)
        val merchantAccountInfo = buildString {
            append(tlv("00", RAAST_GUI))
            if (cleanIban.isNotBlank()) {
                append(tlv("01", cleanIban))
            }
            if (cleanAcc.isNotBlank()) {
                append(tlv("02", cleanAcc))
            }
        }
        if (merchantAccountInfo.isNotBlank()) {
            sb.append(tlv("28", merchantAccountInfo))
        }

        // 52: Merchant Category Code (5411 = Grocery Stores / Kiryana)
        sb.append(tlv("52", MCC_GROCERY_STORE))

        // 53: Transaction Currency (586 = PKR)
        sb.append(tlv("53", CURRENCY_PKR))

        // 54: Transaction Amount (optional dynamic bill amount)
        if (hasAmount) {
            val formattedAmt = String.format(Locale.US, "%.2f", amountPkr)
            sb.append(tlv("54", formattedAmt))
        }

        // 58: Country Code (PK)
        sb.append(tlv("58", COUNTRY_PK))

        // 59: Merchant Name (max 25 chars)
        sb.append(tlv("59", cleanName))

        // 60: Merchant City (max 15 chars)
        sb.append(tlv("60", cleanCity))

        // 62: Additional Data Field Template (Bill Ref / Store Label)
        val additionalData = buildString {
            if (cleanRef.isNotBlank()) {
                append(tlv("01", cleanRef))
            }
            append(tlv("05", "HBL"))
        }
        sb.append(tlv("62", additionalData))

        // 63: CRC-16/CCITT-FALSE over entire payload including "6304"
        val withoutCrc = sb.toString() + "6304"
        val crcHex = computeCrc16CcittFalse(withoutCrc)
        return withoutCrc + crcHex
    }

    /**
     * Standard EMVCo CRC-16/CCITT-FALSE (polynomial 0x1021, initial value 0xFFFF).
     */
    fun computeCrc16CcittFalse(input: String): String {
        var crc = 0xFFFF
        val bytes = input.toByteArray(Charsets.UTF_8)
        for (b in bytes) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return String.format(Locale.US, "%04X", crc and 0xFFFF)
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                EncodeHintType.MARGIN to 1
            )
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(sizePx * sizePx)
            for (y in 0 until sizePx) {
                val offset = y * sizePx
                for (x in 0 until sizePx) {
                    pixels[offset + x] = if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE
                }
            }
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Intelligently parses scanned QR codes and barcodes (EMVCo/RAAST, IBAN, CNIC/Ration Card, or Product Barcode).
     */
    fun parseScannedCode(raw: String): ParsedScannedCode {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) {
            return ParsedScannedCode(rawValue = "", kind = ScannedCodeKind.GENERIC_QR)
        }

        // 1. EMVCo / Pakistan RAAST QR Payload (starts with 000201)
        if (trimmed.startsWith("000201") && trimmed.length >= 20) {
            val tags = parseTlvMap(trimmed)
            val merchantName = tags["59"].orEmpty()
            val amount = tags["54"]?.toDoubleOrNull()
            val accountTag = tags["28"] ?: tags["26"] ?: tags["29"] ?: tags["27"]
            val subTags = if (!accountTag.isNullOrBlank()) parseTlvMap(accountTag) else emptyMap()
            val ibanOrAcc = subTags["01"] ?: subTags["02"] ?: subTags["03"] ?: extractIban(trimmed)
            val addData = tags["62"]?.let { parseTlvMap(it) } ?: emptyMap()
            val ref = addData["01"] ?: addData["05"] ?: ""
            return ParsedScannedCode(
                rawValue = trimmed,
                kind = ScannedCodeKind.RAAST_EMV_QR,
                title = merchantName.ifBlank { "RAAST / Bank QR" },
                accountOrIban = ibanOrAcc,
                amount = amount,
                reference = ref
            )
        }

        // 2. Direct IBAN or Bank Account text
        val foundIban = extractIban(trimmed)
        if (foundIban.isNotBlank()) {
            val nameMatch = Regex("NAME:([^|;\\n]+)", RegexOption.IGNORE_CASE).find(trimmed)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            return ParsedScannedCode(
                rawValue = trimmed,
                kind = ScannedCodeKind.BANK_IBAN,
                title = nameMatch.ifBlank { "Bank Account / IBAN" },
                accountOrIban = foundIban
            )
        }

        // 3. Pakistani CNIC (13 digits) or Ration Card Number
        val cnicRegex = Regex("\\b\\d{5}-?\\d{7}-?\\d\\b")
        val cnicMatch = cnicRegex.find(trimmed)?.value
        if (cnicMatch != null || trimmed.contains("BOP", ignoreCase = true) || trimmed.contains("RASHAN", ignoreCase = true)) {
            return ParsedScannedCode(
                rawValue = trimmed,
                kind = ScannedCodeKind.RATION_OR_CNIC_CARD,
                title = "Ration Card / CNIC",
                accountOrIban = cnicMatch ?: trimmed.take(40)
            )
        }

        // 4. Retail Product Barcode (numeric EAN-8, UPC, EAN-13, Code128)
        if (trimmed.length in 6..18 && trimmed.all { it.isDigit() }) {
            return ParsedScannedCode(
                rawValue = trimmed,
                kind = ScannedCodeKind.PRODUCT_BARCODE,
                title = "Product Barcode",
                accountOrIban = trimmed
            )
        }

        return ParsedScannedCode(
            rawValue = trimmed,
            kind = ScannedCodeKind.GENERIC_QR,
            title = "Scanned QR / Code",
            accountOrIban = trimmed.take(60)
        )
    }

    private fun parseTlvMap(payload: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var idx = 0
        while (idx + 4 <= payload.length) {
            val tag = payload.substring(idx, idx + 2)
            val lenStr = payload.substring(idx + 2, idx + 4)
            val len = lenStr.toIntOrNull() ?: break
            val valueStart = idx + 4
            val valueEnd = valueStart + len
            if (len < 0 || valueEnd > payload.length) break
            result[tag] = payload.substring(valueStart, valueEnd)
            idx = valueEnd
        }
        return result
    }

    private fun extractIban(text: String): String {
        val compact = text.uppercase(Locale.US)
        val match = Regex("PK\\d{2}[A-Z0-9]{16,24}").find(compact)
        return match?.value.orEmpty()
    }

    private fun normalizeIban(iban: String): String =
        iban.uppercase(Locale.US).filter { it.isLetterOrDigit() }.take(34)

    private fun sanitizeAscii(text: String): String =
        text.trim().replace(Regex("[^\\x20-\\x7E]"), "").trim()

    private fun tlv(tag: String, value: String): String {
        val lengthStr = String.format(Locale.US, "%02d", value.length)
        return "$tag$lengthStr$value"
    }
}
