package com.example.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InstallSignUpSheet(
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var report by remember { mutableStateOf(InstallSummaryReport()) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorText = null
        UserInstallTracker.fetchAllRecords()
            .onSuccess { report = it }
            .onFailure { errorText = it.localizedMessage ?: "Failed to load records" }
        isLoading = false
    }

    val formatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = language.pick(
                    "انسٹال اور سائن اپ کرنے والوں کی لسٹ",
                    "Installs & Sign-Ups List"
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = language.pick(
                            "کل انسٹالز: ${report.totalInstalls}",
                            "Total Installs: ${report.totalInstalls}"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = language.pick(
                            "کل سائن اپ: ${report.totalSignUps}",
                            "Total Sign-Ups: ${report.totalSignUps}"
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    errorText != null -> {
                        Text(
                            text = errorText.orEmpty(),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    report.records.isEmpty() -> {
                        Text(
                            text = language.pick(
                                "ابھی تک کوئی انسٹال یا سائن اپ ریکارڈ نہیں ملا",
                                "No install or sign-up records found yet"
                            ),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(report.records, key = { it.id }) { item ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = item.storeName.ifBlank {
                                                language.pick("نام درج نہیں", "Unnamed Store")
                                            },
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${item.type} • ${item.deviceModel.ifBlank { "Android" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (item.phone.isNotBlank()) {
                                            Text(
                                                text = "${language.pick("فون", "Phone")}: ${item.phone}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (item.email.isNotBlank()) {
                                            Text(
                                                text = "${language.pick("ای میل", "Email")}: ${item.email}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (item.timestampMs > 0L) {
                                            Text(
                                                text = formatter.format(Date(item.timestampMs)),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(language.pick("بند کریں", "Close"))
            }
        }
    )
}
