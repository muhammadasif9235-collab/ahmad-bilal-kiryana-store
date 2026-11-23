package com.example.services

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.ToneGenerator
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import java.math.BigInteger

enum class NfcHardwareStatus {
    READY,
    DISABLED,
    NOT_SUPPORTED
}

data class ScannedNfcCard(
    val uidHex: String,
    val uidDecimal: String = "",
    val techSummary: String,
    val ndefText: String = "",
    val suggestedCardNumber: String = "",
    val maskedPan: String = "",
    val scannedAtMs: Long = System.currentTimeMillis()
)

object NfcRationCardReader {

    private val SUPPORTED_TECH_LISTS: Array<Array<String>> = arrayOf(
        arrayOf(NfcA::class.java.name),
        arrayOf(NfcB::class.java.name),
        arrayOf(NfcF::class.java.name),
        arrayOf(NfcV::class.java.name),
        arrayOf(IsoDep::class.java.name),
        arrayOf(MifareClassic::class.java.name),
        arrayOf(MifareUltralight::class.java.name),
        arrayOf(Ndef::class.java.name)
    )

    private val KNOWN_EMV_AIDS: List<ByteArray> = listOf(
        hexToBytes("A000000333010101"), // UnionPay / PayPak Debit (BOP Ration Card)
        hexToBytes("A000000333010102"), // UnionPay Credit
        hexToBytes("A000000333010103"), // UnionPay Quasi-Credit
        hexToBytes("A0000000031010"),   // Visa Debit / Credit
        hexToBytes("A0000000041010")    // Mastercard
    )

    fun maskCardPan(rawDigits: String): String {
        val digits = rawDigits.filter { it.isDigit() }
        if (digits.length < 12) return ""
        val first4 = digits.take(4)
        val last4 = digits.takeLast(4)
        return "$first4 **** **** $last4"
    }

    fun checkHardwareStatus(context: Context): NfcHardwareStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcHardwareStatus.NOT_SUPPORTED
        return if (adapter.isEnabled) NfcHardwareStatus.READY else NfcHardwareStatus.DISABLED
    }

    fun enableNfcReading(
        activity: Activity,
        onCardScanned: (ScannedNfcCard) -> Unit
    ) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return

        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, piFlags)
        val filters = arrayOf(
            IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
            IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
        )
        try {
            adapter.enableForegroundDispatch(activity, pendingIntent, filters, SUPPORTED_TECH_LISTS)
        } catch (_: Exception) {
        }

        val readerFlags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NFC_BARCODE or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS

        val extras = Bundle().apply {
            putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
        }

        try {
            adapter.enableReaderMode(
                activity,
                { tag ->
                    val card = extractCardFromTag(tag)
                    if (card != null) {
                        playScanFeedback(activity.applicationContext)
                        activity.runOnUiThread {
                            onCardScanned(card)
                        }
                    }
                },
                readerFlags,
                extras
            )
        } catch (_: Exception) {
        }
    }

    fun disableNfcReading(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
        } catch (_: Exception) {
        }
        try {
            adapter.disableForegroundDispatch(activity)
        } catch (_: Exception) {
        }
    }

    fun extractCardFromIntent(context: Context, intent: Intent?): ScannedNfcCard? {
        if (intent == null) return null
        val action = intent.action ?: return null
        if (action != NfcAdapter.ACTION_TAG_DISCOVERED &&
            action != NfcAdapter.ACTION_TECH_DISCOVERED &&
            action != NfcAdapter.ACTION_NDEF_DISCOVERED
        ) {
            return null
        }

        val tag: Tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return null

        val intentNdefText = extractNdefTextFromIntent(intent)
        val card = extractCardFromTag(tag, preReadNdefText = intentNdefText) ?: return null
        playScanFeedback(context.applicationContext)
        return card
    }

    fun extractCardFromTag(tag: Tag, preReadNdefText: String = ""): ScannedNfcCard? {
        val idBytes = tag.id ?: return null
        val uidHex = idBytes.joinToString(":") { byte -> "%02X".format(byte) }
        if (uidHex.isBlank()) return null

        val compactUid = idBytes.joinToString("") { byte -> "%02X".format(byte) }
        val uidDecimal = try {
            BigInteger(1, idBytes).toString()
        } catch (_: Exception) {
            ""
        }

        val techList = tag.techList?.map { it.substringAfterLast('.') } ?: emptyList()
        val techSummary = techList.take(4).joinToString(" • ").ifBlank { "NFC Smart Card" }

        var ndefText = preReadNdefText.trim()
        if (ndefText.isBlank()) {
            ndefText = readNdefTextFromTag(tag)
        }

        // Read actual 16-digit EMV Card Number (PAN) from BOP / PayPak / Visa / MC chip
        var maskedPan = ""
        if (techList.any { it.equals("IsoDep", ignoreCase = true) }) {
            val rawPan = readEmvCardPan(tag)
            if (rawPan.isNotBlank()) {
                maskedPan = maskCardPan(rawPan)
            }
        }
        if (maskedPan.isBlank() && ndefText.count { it.isDigit() } >= 12) {
            maskedPan = maskCardPan(ndefText)
        }

        val suggestedCardNo = when {
            maskedPan.isNotBlank() -> maskedPan
            ndefText.isNotBlank() -> ndefText.take(60)
            else -> "BOP-$compactUid"
        }

        return ScannedNfcCard(
            uidHex = uidHex,
            uidDecimal = uidDecimal,
            techSummary = techSummary,
            ndefText = ndefText,
            suggestedCardNumber = suggestedCardNo,
            maskedPan = maskedPan,
            scannedAtMs = System.currentTimeMillis()
        )
    }
        private fun extractNdefTextFromIntent(intent: Intent): String {
        return try {
            val rawMsgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }
            val firstMsg = rawMsgs?.firstOrNull() as? NdefMessage ?: return ""
            decodeNdefMessage(firstMsg)
        } catch (_: Exception) {
            ""
        }
    }

    private fun readNdefTextFromTag(tag: Tag): String {
        val ndef = Ndef.get(tag) ?: return ""
        return try {
            val cached = ndef.cachedNdefMessage
            if (cached != null) {
                decodeNdefMessage(cached)
            } else {
                ndef.connect()
                val msg = ndef.ndefMessage
                ndef.close()
                if (msg != null) decodeNdefMessage(msg) else ""
            }
        } catch (_: Exception) {
            try {
                ndef.close()
            } catch (_: Exception) {
            }
            ""
        }
    }

    /**
     * Safely reads the EMV Application PAN (Tag 5A) or Track 2 Equivalent (Tag 57)
     * from contactless smart cards (Bank of Punjab / PayPak / UnionPay / Visa / MC).
     */
    private fun readEmvCardPan(tag: Tag): String {
        val isoDep = IsoDep.get(tag) ?: return ""
        return try {
            isoDep.timeout = 1500
            isoDep.connect()

            val candidateAids = mutableListOf<ByteArray>()

            // 1. Query PPSE (2PAY.SYS.DDF01) to discover the card's actual AID
            val ppseCmd = hexToBytes("00A404000E325041592E5359532E444446303100")
            val ppseResp = transceiveSafe(isoDep, ppseCmd)
            if (isSuccessSw(ppseResp)) {
                extractAllTlvValues(ppseResp, 0x4F).forEach { aid ->
                    if (aid.size in 5..16) candidateAids.add(aid)
                }
            }

            // 2. Append standard BOP / PayPak / UnionPay / Visa / MC AIDs as fallback
            KNOWN_EMV_AIDS.forEach { known ->
                if (candidateAids.none { it.contentEquals(known) }) {
                    candidateAids.add(known)
                }
            }

            // 3. Select each AID and read AFL / SFI records for Tag 5A (PAN) or Tag 57 (Track 2)
            for (aid in candidateAids) {
                val selectCmd = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, aid.size.toByte()) +
                    aid + byteArrayOf(0x00)
                val selectResp = transceiveSafe(isoDep, selectCmd)
                if (!isSuccessSw(selectResp)) continue

                findPanInResponse(selectResp)?.let { pan ->
                    isoDep.close()
                    return pan
                }

                // Initiate Application Processing (GPO) with empty PDOL
                val gpoCmd = hexToBytes("80A8000002830000")
                val gpoResp = transceiveSafe(isoDep, gpoCmd)
                if (isSuccessSw(gpoResp)) {
                    findPanInResponse(gpoResp)?.let { pan ->
                        isoDep.close()
                        return pan
                    }
                }

                // Read standard EMV records (SFI 1..4, Records 1..5)
                for (sfi in 1..4) {
                    val p2 = ((sfi shl 3) or 0x04).toByte()
                    for (rec in 1..5) {
                        val readCmd = byteArrayOf(0x00, 0xB2.toByte(), rec.toByte(), p2, 0x00)
                        val recResp = transceiveSafe(isoDep, readCmd)
                        if (isSuccessSw(recResp)) {
                            findPanInResponse(recResp)?.let { pan ->
                                isoDep.close()
                                return pan
                            }
                        }
                    }
                }
            }

            isoDep.close()
            ""
        } catch (_: Exception) {
            try {
                isoDep.close()
            } catch (_: Exception) {
            }
            ""
        }
    }

    private fun transceiveSafe(isoDep: IsoDep, command: ByteArray): ByteArray {
        return try {
            isoDep.transceive(command)
        } catch (_: Exception) {
            ByteArray(0)
        }
    }

    private fun isSuccessSw(resp: ByteArray): Boolean {
        if (resp.size < 2) return false
        val sw1 = resp[resp.size - 2].toInt() and 0xFF
        val sw2 = resp[resp.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
    }

    private fun findPanInResponse(resp: ByteArray): String? {
        if (resp.size <= 2) return null
        val payload = resp.copyOfRange(0, resp.size - 2)

        // Tag 5A: Application Primary Account Number (BCD)
        extractAllTlvValues(payload, 0x5A).firstOrNull()?.let { panBytes ->
            val pan = bcdToDigits(panBytes)
            if (pan.length in 13..19 && luhnCheck(pan)) return pan
        }

        // Tag 57: Track 2 Equivalent Data (PAN followed by 'D')
        extractAllTlvValues(payload, 0x57).firstOrNull()?.let { track2Bytes ->
            val hex = track2Bytes.joinToString("") { "%02X".format(it) }
            val pan = hex.substringBefore('D').filter { it.isDigit() }
            if (pan.length in 13..19 && luhnCheck(pan)) return pan
        }

        return null
    }

    private fun extractAllTlvValues(data: ByteArray, targetTag: Int): List<ByteArray> {
        val results = mutableListOf<ByteArray>()
        var i = 0
        while (i < data.size) {
            val firstByte = data[i].toInt() and 0xFF
            if (firstByte == 0x00 || firstByte == 0xFF) {
                i++
                continue
            }
            var tag = firstByte
            i++
            if ((firstByte and 0x1F) == 0x1F && i < data.size) {
                tag = (tag shl 8) or (data[i].toInt() and 0xFF)
                i++
            }
            if (i >= data.size) break

            var len = data[i].toInt() and 0xFF
            i++
            if ((len and 0x80) != 0) {
                val numLenBytes = len and 0x7F
                if (numLenBytes == 0 || i + numLenBytes > data.size) break
                len = 0
                for (b in 0 until numLenBytes) {
                    len = (len shl 8) or (data[i].toInt() and 0xFF)
                    i++
                }
            }
                        if (len < 0 || i + len > data.size) break

            val value = data.copyOfRange(i, i + len)
            if (tag == targetTag) {
                results.add(value)
            }
            if ((firstByte and 0x20) != 0) {
                results.addAll(extractAllTlvValues(value, targetTag))
            }
            i += len
        }
        return results
    }

    private fun bcdToDigits(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (b in bytes) {
            val hi = (b.toInt() ushr 4) and 0x0F
            val lo = b.toInt() and 0x0F
            if (hi in 0..9) sb.append(hi)
            if (lo in 0..9) sb.append(lo)
        }
        return sb.toString()
    }

    private fun luhnCheck(digits: String): Boolean {
        var sum = 0
        var alternate = false
        for (idx in digits.length - 1 downTo 0) {
            var n = digits[idx] - '0'
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.filter { !it.isWhitespace() }
        return ByteArray(clean.length / 2) { idx ->
            clean.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
        }
    }

    private fun decodeNdefMessage(message: NdefMessage): String {
        for (record in message.records) {
            val payload = record.payload ?: continue
            if (payload.isEmpty()) continue
            if (payload.size > 3 && record.tnf == android.nfc.NdefRecord.TNF_WELL_KNOWN &&
                record.type.contentEquals(android.nfc.NdefRecord.RTD_TEXT)
            ) {
                val status = payload[0].toInt()
                val langLen = status and 0x3F
                val charset = if ((status and 0x80) == 0) Charsets.UTF_8 else Charsets.UTF_16
                if (1 + langLen < payload.size) {
                    return String(payload, 1 + langLen, payload.size - 1 - langLen, charset).trim()
                }
            }
            val plain = String(payload, Charsets.UTF_8)
                .filter { it.code in 32..126 || it.isLetterOrDigit() }
                .trim()
            if (plain.length >= 4) return plain
        }
        return ""
    }

    fun playScanFeedback(context: Context) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(110L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(110L)
                }
            }
        } catch (_: Exception) {
        }

        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            tone.startTone(ToneGenerator.TONE_PROP_ACK, 150)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                {
                    try {
                        tone.release()
                    } catch (_: Throwable) {
                    }
                },
                250L
            )
        } catch (_: Exception) {
        }
    }
}

