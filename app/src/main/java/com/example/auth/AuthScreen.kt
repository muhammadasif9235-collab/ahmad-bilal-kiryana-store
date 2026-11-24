package com.example.auth

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.ui.components.AsyncBannerImage
import com.example.ui.components.LegalDocumentTab
import com.example.ui.components.LegalDocumentsDialog
import com.example.ui.components.ShopLogoBadge
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.SaffronSecondary

private const val LEGAL_CONSENT_PREFS = "ahmad_bilal_legal_consent"
private const val KEY_FIRST_TIME_LEGAL_ACCEPTED = "first_time_legal_accepted_v1"

private fun hasAcceptedFirstTimeLegal(context: Context): Boolean =
    context.applicationContext
        .getSharedPreferences(LEGAL_CONSENT_PREFS, Context.MODE_PRIVATE)
        .getBoolean(KEY_FIRST_TIME_LEGAL_ACCEPTED, false)

private fun saveFirstTimeLegalAccepted(context: Context) {
    context.applicationContext
        .getSharedPreferences(LEGAL_CONSENT_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putBoolean(KEY_FIRST_TIME_LEGAL_ACCEPTED, true)
        .apply()
}

@Composable
fun AuthScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onLocalOwnerLogin: () -> Unit,
    onGoogleAuthSuccess: () -> Unit,
    onGuestLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { lazy { CredentialManager.create(context) } }

    var isSignUpMode by remember {
        mutableStateOf(ShopSecurityVault.getRegisteredEmail(context).isBlank())
    }
    var ownerNameInput by remember { mutableStateOf(ShopSecurityVault.getRegisteredName(context)) }
    var emailInput by remember { mutableStateOf(ShopSecurityVault.getRegisteredEmail(context)) }
    var passwordInput by remember { mutableStateOf("") }
    var stayLoggedIn by remember { mutableStateOf(ShopSecurityVault.isStayLoggedIn(context)) }

    var isGoogleLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var legalAccepted by remember { mutableStateOf(hasAcceptedFirstTimeLegal(context)) }
    var showMandatoryConsentGate by remember { mutableStateOf(false) }
    var pendingProceedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var activeLegalDialogTab by remember { mutableStateOf<LegalDocumentTab?>(null) }

    fun requireLegalConsentFirst(proceed: () -> Unit) {
        if (legalAccepted) {
            proceed()
        } else {
            pendingProceedAction = proceed
            showMandatoryConsentGate = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.testTag("auth_language_toggle")
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(if (language == AppLanguage.URDU) "English (LTR)" else "اردو (RTL)")
                }
            }

            // Hero Header Card with Large Full-Bleed Circular Store Crest
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(listOf(GoldShimmer, SaffronSecondary, GoldShimmer)),
                        shape = RoundedCornerShape(24.dp)
                    )
            ) {
                AsyncBannerImage(
                    resId = R.drawable.img_kiryana_banner_1795314088201,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xB8042D1F), Color(0xE2063927), Color(0xF5042519))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(CircleShape)
                            .border(2.5.dp, GoldShimmer, CircleShape)
                    ) {
                        ShopLogoBadge(
                            resId = R.drawable.img_shop_logo,
                            contentDescription = "Store Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = language.pick("احمد بلال کریانہ سٹور ٹانڈہ", "Ahmad Bilal Kiryana Store, Tanda"),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (language == AppLanguage.URDU) 23.sp else 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = language.pick(
                            "پروفیشنل POS • ڈیجیٹل کھاتہ • روزنامچہ • فنگر پرنٹ سیکیورٹی",
                            "Smart POS • Digital Khata • Cashbook • Biometric Security"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldShimmer,
                        textAlign = TextAlign.Center
                    )
                }
            }
                        // Sign Up / Login Account Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isSignUpMode,
                            onClick = {
                                isSignUpMode = true
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    text = language.pick("نیا اکاؤنٹ (Sign Up)", "Create Account"),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !isSignUpMode,
                            onClick = {
                                isSignUpMode = false
                                errorMessage = null
                            },
                            label = {
                                Text(
                                    text = language.pick("لاگ اِن (Login)", "Sign In"),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isSignUpMode) {
                        OutlinedTextField(
                            value = ownerNameInput,
                            onValueChange = { ownerNameInput = it },
                            label = { Text(language.pick("دکاندار کا نام*", "Shop Owner Name*")) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text(language.pick("ای میل یا موبائل آئی ڈی*", "Email or Mobile ID*")) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text(language.pick("پاس ورڈ (کم از کم 4 ہندسے)*", "Password (min 4 chars)*")) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // "Stay Logged In" Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { stayLoggedIn = !stayLoggedIn },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = stayLoggedIn,
                            onCheckedChange = { stayLoggedIn = it }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = language.pick(
                                    "مجھے لاگ اِن رکھیں (Stay Logged In)",
                                    "Remember Me (Stay Logged In)"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = language.pick(
                                    "ٹک نہ کرنے پر ہر بار دوبارہ لاگ اِن ہونا پڑے گا",
                                    "Uncheck to require login every time the app opens"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (errorMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    // Submit Sign Up / Login Button
                    Button(
                        onClick = {
                            val email = emailInput.trim()
                            val pass = passwordInput.trim()
                            if (email.isBlank() || pass.length < 4) {
                                errorMessage = language.pick(
                                    "براہِ کرم درست ای میل/آئی ڈی اور کم از کم 4 ہندسوں کا پاس ورڈ لکھیں۔",
                                    "Please enter valid Email/ID and at least 4-character password."
                                )
                                return@Button
                            }
                            if (!isSignUpMode && !ShopSecurityVault.verifyLocalAccount(context, email, pass)) {
                                errorMessage = language.pick(
                                    "غلط ای میل یا پاس ورڈ! اگر نیا اکاؤنٹ بنانا ہے تو اوپر «نیا اکاؤنٹ (Sign Up)» منتخب کریں۔",
                                    "Invalid Email or Password! Tap 'Create Account' above if you haven't signed up yet."
                                )
                                return@Button
                            }
                            requireLegalConsentFirst {
                                if (isSignUpMode) {
                                    val name = ownerNameInput.trim().ifBlank { "Shop Owner" }
                                    ShopSecurityVault.saveRegisteredAccount(context, name, email, pass)
                                }
                                ShopSecurityVault.setStayLoggedIn(context, stayLoggedIn)
                                ShopSecurityVault.setGuestMode(context, false)
                                onLocalOwnerLogin()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("local_owner_login_button")
                    ) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = GoldShimmer)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isSignUpMode) {
                                language.pick("اکاؤنٹ بنائیں اور دکان کھولیں", "Sign Up & Open Store")
                            } else {
                                language.pick("لاگ اِن کریں اور دکان کھولیں", "Login & Open Store")
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    HorizontalDivider()

                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            requireLegalConsentFirst {
                                errorMessage = null
                                isGoogleLoading = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager.value,
                                    onAuthSuccess = {
                                        isGoogleLoading = false
                                        ShopSecurityVault.setStayLoggedIn(context, stayLoggedIn)
                                        ShopSecurityVault.setGuestMode(context, false)
                                        onGoogleAuthSuccess()
                                    },
                                    onAuthError = { _ ->
                                        isGoogleLoading = false
                                        errorMessage = language.pick(
                                            "اس ڈیوائس پر گوگل لاگ اِن دستیاب نہیں — براہِ کرم اوپر ای میل/پاس ورڈ سے سائن اپ یا لاگ اِن کریں۔",
                                            "Google Sign-In unavailable on this device — please use Email/Password Sign Up above."
                                        )
                                    },
                                    scope = scope,
                                    onAuthCancelled = { isGoogleLoading = false }
                                )
                            }
                        },
                        enabled = !isGoogleLoading,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.2.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isGoogleLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                        } else {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = EmeraldPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = language.pick(
                                    "گوگل اکاؤنٹ سے سائن اپ / لاگ اِن (Google)",
                                    "Sign Up / Login with Google"
                                ),
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }

                    // Guest Mode Button (Triggers Privacy Policy & Terms First!)
                    FilledTonalButton(
                        onClick = {
                            requireLegalConsentFirst {
                                ShopSecurityVault.setGuestMode(context, true)
                                ShopSecurityVault.setStayLoggedIn(context, false)
                                onGuestLogin()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("guest_mode_login_button")
                    ) {
                        Icon(Icons.Default.PersonOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = language.pick(
                                "گیسٹ کے طور پر داخل ہوں (محدود فیچرز)",
                                "Continue as Guest (Limited Features)"
                            ),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TextButton(onClick = { activeLegalDialogTab = LegalDocumentTab.PRIVACY_POLICY }) {
                            Icon(Icons.Default.Policy, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("پرائیویسی پالیسی", "Privacy Policy"), style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { activeLegalDialogTab = LegalDocumentTab.TERMS_AND_CONDITIONS }) {
                            Icon(Icons.Default.Gavel, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(language.pick("شرائط و ضوابط", "Terms & Rules"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }

    // First-Time Mandatory Privacy Policy & Terms Gate
    if (showMandatoryConsentGate) {
        AlertDialog(
            onDismissRequest = { showMandatoryConsentGate = false },
            icon = { Icon(Icons.Default.Policy, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    text = language.pick(
                        "پرائیویسی پالیسی اور شرائط و ضوابط",
                        "Privacy Policy & Terms of Use"
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = language.pick(
                            "ایپ استعمال کرنے سے پہلے براہِ کرم پرائیویسی پالیسی اور شرائط سے اتفاق کریں۔ یہ ایک نجی دکان POS سسٹم ہے (غیر سرکاری) اور آپ کا ڈیٹا PECA 2016 کے تحت محفوظ رہتا ہے۔",
                            "Before entering the app, please review and accept our Privacy Policy and Terms & Conditions. This is an independent private POS app (Non-Government) compliant with PECA 2016."
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { activeLegalDialogTab = LegalDocumentTab.PRIVACY_POLICY },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(language.pick("پرائیویسی پڑھیں", "Read Privacy"), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { activeLegalDialogTab = LegalDocumentTab.TERMS_AND_CONDITIONS },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(language.pick("شرائط پڑھیں", "Read Terms"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        saveFirstTimeLegalAccepted(context)
                        legalAccepted = true
                        showMandatoryConsentGate = false
                        pendingProceedAction?.invoke()
                        pendingProceedAction = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GoldShimmer)
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = language.pick("منظور ہے اور آگے بڑھیں", "I Agree & Continue"),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showMandatoryConsentGate = false }) {
                    Text(language.pick("منسوخ", "Cancel"))
                }
            }
        )
    }

    val legalTab = activeLegalDialogTab
    if (legalTab != null) {
        LegalDocumentsDialog(
            initialTab = legalTab,
            language = language,
            onDismiss = { activeLegalDialogTab = null }
        )
    }
}
