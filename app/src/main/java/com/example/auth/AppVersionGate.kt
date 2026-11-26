package com.example.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
