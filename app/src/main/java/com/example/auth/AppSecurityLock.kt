package com.example.auth

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.ui.components.ShopLogoBadge
import com.example.ui.theme.EmeraldBright
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.UdharCrimson

object ShopSecurityVault {
    private const val PREFS_NAME = "ahmad_bilal_security_vault"
    private const val KEY_STAY_LOGGED_IN = "stay_logged_in"
    private const val KEY_IS_GUEST_MODE = "is_guest_mode"
    private const val KEY_FINGERPRINT_ENABLED = "fingerprint_lock_enabled"
    private const val KEY_BACKUP_PIN = "backup_security_pin"
    private const val KEY_REGISTERED_EMAIL = "registered_owner_email"
    private const val KEY_REGISTERED_PASS = "registered_owner_pass"
    private const val KEY_REGISTERED_NAME = "registered_owner_name"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isStayLoggedIn(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STAY_LOGGED_IN, true)

    fun setStayLoggedIn(context: Context, stay: Boolean) {
        prefs(context).edit().putBoolean(KEY_STAY_LOGGED_IN, stay).apply()
    }

    fun isGuestMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_GUEST_MODE, false)

    fun setGuestMode(context: Context, isGuest: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_GUEST_MODE, isGuest).apply()
    }

    fun isFingerprintLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FINGERPRINT_ENABLED, false)

    fun setFingerprintLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_FINGERPRINT_ENABLED, enabled).apply()
    }

    fun getBackupPin(context: Context): String =
        prefs(context).getString(KEY_BACKUP_PIN, "").orEmpty()

    fun setBackupPin(context: Context, pin: String) {
        prefs(context).edit().putString(KEY_BACKUP_PIN, pin.trim()).apply()
    }

    fun saveRegisteredAccount(context: Context, name: String, email: String, password: String) {
        prefs(context).edit()
            .putString(KEY_REGISTERED_NAME, name.trim())
            .putString(KEY_REGISTERED_EMAIL, email.trim().lowercase())
            .putString(KEY_REGISTERED_PASS, password)
            .apply()
    }

    fun getRegisteredEmail(context: Context): String =
        prefs(context).getString(KEY_REGISTERED_EMAIL, "").orEmpty()

    fun getRegisteredName(context: Context): String =
        prefs(context).getString(KEY_REGISTERED_NAME, "").orEmpty()

    fun verifyLocalAccount(context: Context, email: String, password: String): Boolean {
        val savedEmail = prefs(context).getString(KEY_REGISTERED_EMAIL, "").orEmpty()
        val savedPass = prefs(context).getString(KEY_REGISTERED_PASS, "").orEmpty()
        return savedEmail.isNotBlank() &&
            savedEmail.equals(email.trim(), ignoreCase = true) &&
            savedPass == password
    }

    fun canUseBiometric(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        return manager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun launchBiometricPrompt(
        context: Context,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            onError("Biometric prompt requires FragmentActivity")
            return
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }
            }
        )
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setNegativeButtonText(negativeButtonText)
        }
        prompt.authenticate(builder.build())
    }
}
@Composable
fun FingerprintAppLockScreen(
    language: AppLanguage,
    shopTitle: String,
    onUnlocked: () -> Unit,
    onSwitchToLoginScreen: () -> Unit
) {
    val context = LocalContext.current
    val savedPin = remember { ShopSecurityVault.getBackupPin(context) }
    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun triggerFingerprint() {
        errorMsg = null
        ShopSecurityVault.launchBiometricPrompt(
            context = context,
            title = language.pick("دکان کا فنگر پرنٹ لاک", "Shop Fingerprint Lock"),
            subtitle = language.pick(
                "کھاتہ اور روزنامچہ کھولنے کے لیے فنگر پرنٹ لگائیں",
                "Touch fingerprint sensor to unlock your shop"
            ),
            negativeButtonText = language.pick("PIN استعمال کریں", "Use PIN"),
            onSuccess = onUnlocked,
            onError = { err ->
                errorMsg = err
            }
        )
    }

    LaunchedEffect(Unit) {
        if (ShopSecurityVault.canUseBiometric(context)) {
            triggerFingerprint()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(EmeraldPrimaryDark, EmeraldPrimary, EmeraldBright)
                )
            )
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(1.5.dp, GoldShimmer),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(2.dp, GoldShimmer, CircleShape)
                        .padding(2.dp)
                ) {
                    ShopLogoBadge(
                        resId = R.drawable.img_shop_logo,
                        contentDescription = shopTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Text(
                    text = shopTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = language.pick(
                        "ایپ لاک ہے — فنگر پرنٹ لگائیں",
                        "App Locked — Scan Fingerprint to Unlock"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = { triggerFingerprint() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GoldShimmer)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = language.pick("فنگر پرنٹ اسکین کریں", "Unlock with Fingerprint"),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (savedPin.length == 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }.take(4)
                                pinInput = digits
                                if (digits.length == 4) {
                                    if (digits == savedPin) {
                                        onUnlocked()
                                    } else {
                                        errorMsg = language.pick("غلط 4 ہندسوں کا PIN!", "Incorrect 4-Digit PIN!")
                                        pinInput = ""
                                    }
                                }
                            },
                            label = { Text(language.pick("یا 4 ہندسوں کا PIN درج کریں", "Or Enter 4-Digit PIN")) },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        FilledTonalButton(
                            onClick = {
                                if (pinInput == savedPin) onUnlocked()
                                else errorMsg = language.pick("غلط 4 ہندسوں کا PIN!", "Incorrect 4-Digit PIN!")
                            }
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                        }
                    }
                }

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        style = MaterialTheme.typography.labelMedium,
                        color = UdharCrimson,
                        textAlign = TextAlign.Center
                    )
                }

                TextButton(onClick = onSwitchToLoginScreen) {
                    Text(
                        text = language.pick("اکاؤنٹ لاگ اِن اسکرین پر واپس جائیں", "Switch Account / Login Screen"),
                        color = SaffronSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
