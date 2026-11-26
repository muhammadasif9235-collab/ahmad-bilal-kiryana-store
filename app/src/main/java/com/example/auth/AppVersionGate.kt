package com.example.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class VersionGateConfig(
    val minSupportedVersionCode: Long = 0L,
    val latestVersionCode: Long = 0L,
    val latestVersionName: String = "",
    val apkDownloadUrl: String = "",
    val messageUrdu: String = "",
    val messageEnglish: String = ""
)

object VersionGateManager {
    suspend fun fetchVersionConfig(): VersionGateConfig? {
        return try {
            val snap = FirebaseFirestore.getInstance()
                .collection("app_config")
                .document("version_control")
                .get()
                .await()

            if (!snap.exists()) return null

            VersionGateConfig(
                minSupportedVersionCode = snap.getLong("minSupportedVersionCode") ?: 0L,
                latestVersionCode = snap.getLong("latestVersionCode") ?: 0L,
                latestVersionName = snap.getString("latestVersionName").orEmpty(),
                apkDownloadUrl = snap.getString("apkDownloadUrl").orEmpty(),
                messageUrdu = snap.getString("forceUpdateMessageUrdu").orEmpty(),
                messageEnglish = snap.getString("forceUpdateMessageEnglish").orEmpty()
            )
        } catch (_: Throwable) {
            null
        }
    }
}

@Composable
fun ForceUpdateGateDialog(
    currentVersionCode: Long,
    language: AppLanguage,
    isOnline: Boolean
) {
    val context = LocalContext.current
    var config by remember { mutableStateOf<VersionGateConfig?>(null) }

    LaunchedEffect(isOnline, currentVersionCode) {
        if (isOnline) {
            config = VersionGateManager.fetchVersionConfig()
        }
    }

    val activeConfig = config ?: return
    val isExpired = activeConfig.minSupportedVersionCode > 0L &&
        currentVersionCode < activeConfig.minSupportedVersionCode

    if (!isExpired) return

    val defaultUrdu = "آپ کا ایپ ورژن ایکسپائر ہو چکا ہے۔ آگے بڑھنے کے لیے براہِ کرم نیا ورژن ڈاؤن لوڈ کریں۔"
    val defaultEnglish = "Your app version has expired. Please download the latest version to continue."

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Text(
                text = language.pick("لازمی اپ ڈیٹ درکار ہے", "Update Required"),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = language.pick(
                        activeConfig.messageUrdu.ifBlank { defaultUrdu },
                        activeConfig.messageEnglish.ifBlank { defaultEnglish }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (activeConfig.latestVersionName.isNotBlank()) {
                    Text(
                        text = language.pick(
                            "تازہ ورژن: ${activeConfig.latestVersionName}",
                            "Latest Version: ${activeConfig.latestVersionName}"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            if (activeConfig.apkDownloadUrl.isNotBlank()) {
                Button(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activeConfig.apkDownloadUrl.trim()))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Throwable) {
                        }
                    }
                ) {
                    Text(language.pick("نیا ورژن ڈاؤن لوڈ کریں", "Download Update"))
                }
            }
        }
    )
}

@Composable
fun BusinessAppOfferStarBadge(
    language: AppLanguage,
    modifier: Modifier = Modifier,
    phoneNumber: String = "03401558166"
) {
    val context = LocalContext.current
    var showOfferDialog by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "star_flicker")
    val flickerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flicker_alpha"
    )
    val starScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_scale"
    )

    Surface(
        color = Color(0xFF063729),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier
            .graphicsLayer {
                alpha = flickerAlpha
                scaleX = starScale
                scaleY = starScale
            }
            .border(
                width = 1.2.dp,
                color = Color(0xFFFBBF24),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { showOfferDialog = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "✦",
                color = Color(0xFFFBBF24),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = language.pick("خوشخبری • ایپ آفر", "HOT OFFER • APP"),
                color = Color(0xFFFEF3C7),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }

    if (showOfferDialog) {
        AlertDialog(
            onDismissRequest = { showOfferDialog = false },
            title = {
                Text(
                    text = "✦ دھماکہ خیز آفر • Hot Business App Offer ✦",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "خوشخبری! AI، کمپیوٹر اور سوشل میڈیا کے جدید دور میں اپنے کاروبار کو نئی جدت دیں۔ اب اپنی علیحدہ، تیز اور پروفیشنل کاروباری ایپلیکیشن نہایت مناسب ریٹ میں بنوائیں!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Good News! Give your business a modern edge in the era of AI, computers, and social media. Get your own custom business app built at affordable rates!",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "رابطہ / Contact (WhatsApp & Call): $phoneNumber",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showOfferDialog = false
                        try {
                            val cleanNum = phoneNumber.trim()
                            val waUri = Uri.parse("https://wa.me/92${cleanNum.removePrefix("0")}")
                            val intent = Intent(Intent.ACTION_VIEW, waUri)
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: Throwable) {
                            try {
                                val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phoneNumber.trim()}"))
                                callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(callIntent)
                            } catch (_: Throwable) {
                            }
                        }
                    }
                ) {
                    Text(language.pick("واٹس ایپ پر رابطہ کریں", "Chat on WhatsApp"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOfferDialog = false }) {
                    Text(language.pick("بند کریں", "Close"))
                }
            }
        )
    }
}
