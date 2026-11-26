package com.example.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object EncryptedFieldVault {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "ahmad_bilal_shop_aes256_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val PREFIX = "ENC_V1:"

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }

        val existingEntry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingEntry != null) {
            return existingEntry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty() || plainText.startsWith(PREFIX)) return plainText
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val cipherBytes = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

            val combined = ByteArray(1 + iv.size + cipherBytes.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(cipherBytes, 0, combined, 1 + iv.size, cipherBytes.size)

            PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Throwable) {
            plainText
        }
    }

    fun decrypt(cipherText: String): String {
        if (cipherText.isEmpty() || !cipherText.startsWith(PREFIX)) return cipherText
        return try {
            val encodedPayload = cipherText.removePrefix(PREFIX)
            val combined = Base64.decode(encodedPayload, Base64.NO_WRAP)
            if (combined.size < 4) return ""

            val ivLength = combined[0].toInt() and 0xFF
            if (ivLength <= 0 || combined.size <= 1 + ivLength) return ""

            val iv = ByteArray(ivLength)
            System.arraycopy(combined, 1, iv, 0, ivLength)

            val cipherBytes = ByteArray(combined.size - 1 - ivLength)
            System.arraycopy(combined, 1 + ivLength, cipherBytes, 0, cipherBytes.size)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            val plainBytes = cipher.doFinal(cipherBytes)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (_: Throwable) {
            ""
        }
    }

    fun isEncrypted(value: String): Boolean = value.startsWith(PREFIX)
}
