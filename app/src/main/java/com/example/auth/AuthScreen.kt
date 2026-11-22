package com.example.auth

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.ui.components.AsyncBannerImage
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.EmeraldPrimaryDark
import com.example.ui.theme.GoldShimmer
import com.example.ui.theme.SaffronSecondary
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { lazy { CredentialManager.create(context) } }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Wait for initial Activity frames & SurfaceSyncGroup to settle before triggering CredentialManager IPC
    LaunchedEffect(Unit) {
        withFrameNanos { }
        withFrameNanos { }
        delay(350)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager.value,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { },
            scope = scope
        )
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Language Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FilledTonalButton(
                    onClick = onToggleLanguage,
                    shape = RoundedCornerShape(50.dp),
                    modifier = Modifier.testTag("auth_language_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(if (language == AppLanguage.URDU) "English (LTR)" else "اردو (RTL)")
                }
            }

            // Royal Emerald & Antique Gold Hero Marquee
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(236.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .border(
                        width = 1.6.dp,
                        brush = Brush.linearGradient(listOf(GoldShimmer, SaffronSecondary, GoldShimmer)),
                        shape = RoundedCornerShape(26.dp)
                    )
            ) {
                AsyncBannerImage(
                    resId = R.drawable.img_kiryana_banner_1795314088201,
                    contentDescription = language.pick("احمد بلال کریانہ سٹور ٹانڈہ", "Ahmad Bilal Kiryana Store, Tanda"),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0x99042D1F),
                                    Color(0xCC063927),
                                    Color(0xEE042519)
                                )
                            )
                        )
                        .drawBehind {
                            val ringColor = Color(0x33FDE68A)
                            drawCircle(
                                color = ringColor,
                                radius = size.minDimension * 0.65f,
                                center = Offset(size.width * 0.12f, size.height * 0.15f),
                                style = Stroke(width = 2.5f)
                            )
                            drawCircle(
                                color = ringColor,
                                radius = size.minDimension * 0.45f,
                                center = Offset(size.width * 0.92f, size.height * 0.85f),
                                style = Stroke(width = 2f)
                            )
                        }
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        color = SaffronSecondary,
                        shape = RoundedCornerShape(50.dp)
                    ) {
                        Text(
                            text = language.pick("ٹانڈہ • ضلع گجرات، پنجاب", "Tanda • District Gujrat, Punjab"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = language.pick("احمد بلال کریانہ سٹور ٹانڈہ", "Ahmad Bilal Kiryana Store, Tanda"),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = if (language == AppLanguage.URDU) 27.sp else 23.sp,
                            lineHeight = if (language == AppLanguage.URDU) 40.sp else 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(88.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(50.dp))
                            .background(Brush.horizontalGradient(listOf(Color.Transparent, GoldShimmer, Color.Transparent)))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = language.pick(
                            "معیاری کریانہ، مریم نواز راشن کارڈ اور ڈیجیٹل کھاتہ",
                            "Quality Kiryana, Maryam Nawaz Ration Card & Digital Khata"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldShimmer.copy(alpha = 0.95f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Feature Highlights Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = language.pick(
                            "دکان کا مکمل اور جدید ڈیجیٹل نظام",
                            "Complete Modern Kiryana & Ration POS"
                        ),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FeatureBullet(
                        icon = Icons.Default.ReceiptLong,
                        text = language.pick(
                            "اردو/انگلش بلنگ، آرڈرز اور خودکار رسید پرنٹنگ",
                            "Urdu RTL & English billing, orders & auto receipts"
                        )
                    )
                    FeatureBullet(
                        icon = Icons.Default.CreditCard,
                        text = language.pick(
                            "مریم نواز راشن کارڈ NFC اسکین اور 3,000 روپے سبسڈی بل",
                            "Maryam Nawaz Ration Card NFC scan & Rs. 3,000 subsidy billing"
                        )
                    )
                    FeatureBullet(
                        icon = Icons.Default.Inventory2,
                        text = language.pick(
                            "گجرات مارکیٹ ریٹس، اسٹاک ان/آؤٹ اور منافع ریکارڈ",
                            "Gujrat market rates, Stock In/Out & profit analytics"
                        )
                    )
                    FeatureBullet(
                        icon = Icons.Default.Print,
                        text = language.pick(
                            "ادھار کھاتہ، بارکوڈ اسکینر اور بلوٹوتھ تھرمل پرنٹر",
                            "Udhar Khata ledger, barcode scanner & Bluetooth printer"
                        )
                    )
                    FeatureBullet(
                        icon = Icons.Default.CloudDone,
                        text = language.pick(
                            "آف لائن موڈ + محفوظ گوگل کلاؤڈ بیک اپ",
                            "Works offline + automatic Google Cloud backup"
                        )
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Primary Google Sign-In Button
            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager.value,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        }
                    )
                },
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = language.pick(
                            "گوگل اکاؤنٹ سے لاگ ان کریں (کلاؤڈ بیک اپ)",
                            "Sign in with Google (Cloud Backup)"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = language.pick(
                    "ایک بار لاگ ان کے بعد آپ انٹرنیٹ کے بغیر (آف لائن موڈ) میں بھی مکمل بلنگ، راشن کارڈ اور کھاتہ چلا سکتے ہیں۔",
                    "Once signed in, your shop works seamlessly offline and syncs automatically to Google Cloud when online."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
private fun FeatureBullet(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(EmeraldPrimaryDark, EmeraldPrimary))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldShimmer,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
