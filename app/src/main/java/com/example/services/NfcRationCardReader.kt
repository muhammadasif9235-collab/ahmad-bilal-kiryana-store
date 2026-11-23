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

    fun checkHardwareStatus(context: Context): NfcHardwareStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcHardwareStatus.NOT_SUPPORTED
        return if (adapter.isEnabled) NfcHardwareStatus.READY else NfcHardwareStatus.DISABLED
    }

    /**
     * Enables real Android hardware NFC reading using BOTH Foreground Dispatch
     * (supporting NfcA, NfcB, IsoDep, MifareClassic, MifareUltralight, Ndef)
     * and Reader Mode for instant, uninterrupted smart-card detection (including BOP Ration Cards).
     */
    fun enableNfcReading(
        activity: Activity,
        onCardScanned: (ScannedNfcCard) -> Unit
    ) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return

        // 1. Register Foreground Dispatch with explicit tech lists
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
            // Gracefully ignore if Activity is not yet in resumed state
        }

        // 2. Enable Reader Mode (handles NfcA, NfcB, NfcF, NfcV, IsoDep, Mifare, Ndef directly without Android system popups)
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
            // Fall back cleanly to foreground dispatch if reader mode is unavailable
        }
    }

    fun disableNfcReading(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableReaderMode(activity)
        } catch (_: Exception) {
            // Ignore
        }
        try {
            adapter.disableForegroundDispatch(activity)
        } catch (_: Exception) {
            // Ignore
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

        // 1. Try NDEF text from Intent or live Ndef tech
        var ndefText = preReadNdefText.trim()
        if (ndefText.isBlank()) {
            ndefText = readNdefTextFromTag(tag)
        }

        // 2. If it's an IsoDep smart card (e.g. BOP / EMV Ration Card) with no NDEF text,
        // safely probe historical bytes without modifying card state
        var isoHint = ""
        if (ndefText.isBlank() && techList.any { it.equals("IsoDep", ignoreCase = true) }) {
            isoHint = readIsoDepHint(tag)
        }

        val suggestedCardNo = when {
            ndefText.isNotBlank() -> ndefText.take(60)
            isoHint.isNotBlank() -> "BOP-$compactUid"
            else -> "POS-$compactUid"
        }

        return ScannedNfcCard(
            uidHex = uidHex,
            uidDecimal = uidDecimal,
            techSummary = techSummary,
            ndefText = ndefText,
            suggestedCardNumber = suggestedCardNo,
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

    private fun readIsoDepHint(tag: Tag): String {
        val isoDep = IsoDep.get(tag) ?: return ""
        return try {
            isoDep.timeout = 1000
            isoDep.connect()
            val hist = isoDep.historicalBytes ?: isoDep.hiLayerResponse
            isoDep.close()
            hist?.joinToString("") { "%02X".format(it) }?.take(16).orEmpty()
        } catch (_: Exception) {
            try {
                isoDep.close()
            } catch (_: Exception) {
            }
            ""
        }
    }

    private fun decodeNdefMessage(message: NdefMessage): String {
        for (record in message.records) {
            val payload = record.payload ?: continue
            if (payload.isEmpty()) continue
            // RTD_TEXT record
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
            // Fallback printable UTF-8 payload
            val plain = String(payload, Charsets.UTF_8)
                .filter { it.code in 32..126 || it.isLetterOrDigit() }
                .trim()
            if (plain.length >= 4) return plain
        }
        return ""
    }

    fun playScanFeedback(context: Context) {
        // Tactile vibration pulse
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
            // Gracefully ignore if vibration unavailable
        }

        // Crisp acknowledgement beep
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
            // Gracefully ignore if audio channel unavailable
        }
    }
}
