package com.example.services

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build

enum class NfcHardwareStatus {
    READY,
    DISABLED,
    NOT_SUPPORTED
}

data class ScannedNfcCard(
    val uidHex: String,
    val techSummary: String,
    val ndefText: String = "",
    val scannedAtMs: Long = System.currentTimeMillis()
)

object NfcRationCardReader {

    fun checkHardwareStatus(context: Context): NfcHardwareStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context) ?: return NfcHardwareStatus.NOT_SUPPORTED
        return if (adapter.isEnabled) NfcHardwareStatus.READY else NfcHardwareStatus.DISABLED
    }

    fun enableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        if (!adapter.isEnabled) return
        val intent = Intent(activity, activity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags)
        try {
            adapter.enableForegroundDispatch(activity, pendingIntent, null, null)
        } catch (_: Exception) {
            // Gracefully ignore if Activity is not yet in resumed state
        }
    }

    fun disableForegroundDispatch(activity: Activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity) ?: return
        try {
            adapter.disableForegroundDispatch(activity)
        } catch (_: Exception) {
            // Ignore
        }
    }

    fun extractCardFromIntent(intent: Intent?): ScannedNfcCard? {
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

        val uidHex = tag.id?.joinToString(":") { byte -> "%02X".format(byte) } ?: ""
        if (uidHex.isBlank()) return null

        val techSummary = tag.techList
            ?.map { it.substringAfterLast('.') }
            ?.take(3)
            ?.joinToString(" • ")
            ?: "NFC Smart Card"

        var ndefText = ""
        try {
            val rawMsgs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            }
            val firstMsg = rawMsgs?.firstOrNull() as? NdefMessage
            val firstPayload = firstMsg?.records?.firstOrNull()?.payload
            if (firstPayload != null && firstPayload.size > 3) {
                val langLen = firstPayload[0].toInt() and 0x3F
                if (1 + langLen < firstPayload.size) {
                    ndefText = String(firstPayload, 1 + langLen, firstPayload.size - 1 - langLen, Charsets.UTF_8).trim()
                }
            }
        } catch (_: Exception) {
            // Non-NDEF EMV/IsoDep smart cards (like BOP Rashan Cards) rely on UID
        }

        return ScannedNfcCard(
            uidHex = uidHex,
            techSummary = techSummary,
            ndefText = ndefText,
            scannedAtMs = System.currentTimeMillis()
        )
    }
}
