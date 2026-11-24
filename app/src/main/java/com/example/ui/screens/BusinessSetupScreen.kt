package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.BusinessCatalog
import com.example.data.BusinessType
import com.example.i18n.AppLanguage

@Composable
fun BusinessSetupScreen(
    language: AppLanguage,
    initialBusinessType: BusinessType = BusinessType.KIRYANA,
    initialShopNameUrdu: String = "",
    initialShopNameEnglish: String = "",
    initialCityUrdu: String = "",
    isBusy: Boolean = false,
    onCompleteSetup: (
        selectedType: BusinessType,
        shopNameUrdu: String,
        shopNameEnglish: String,
        cityUrdu: String,
        loadPresetItems: Boolean
    ) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialBusinessType) }
    var searchQuery by remember { mutableStateOf("") }
    var shopNameUrdu by remember { mutableStateOf(initialShopNameUrdu) }
    var shopNameEnglish by remember { mutableStateOf(initialShopNameEnglish) }
    var cityUrdu by remember { mutableStateOf(initialCityUrdu) }
    var loadPresets by remember { mutableStateOf(true) }

    val filteredBusinesses = remember(searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) {
            BusinessType.entries
        } else {
            BusinessType.entries.filter {
                it.titleUrdu.contains(q, ignoreCase = true) ||
                    it.titleEnglish.contains(q, ignoreCase = true) ||
                    it.subtitleUrdu.contains(q, ignoreCase = true) ||
                    it.subtitleEnglish.contains(q, ignoreCase = true)
            }
        }
    }

    val emeraldDeep = Color(0xFF064E3B)
    val emeraldMid = Color(0xFF065F46)
    val goldAccent = Color(0xFFD4AF37)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(emeraldDeep, emeraldMid)))
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = language.pick("اپنا کاروبار منتخب کریں", "Select Your Business Type"),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFDE68A)
                )
                Text(
                    text = language.pick(
                        "کاروبار منتخب کریں تاکہ آپ کی دکان کے مطابق سامان کی تیار لسٹ لوڈ ہو جائے",
                        "Choose your business category to auto-load ready-made inventory items"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = {
                        Text(
                            language.pick(
                                "کاروبار تلاش کریں (مثلاً کریانہ، پنسار، کپڑا، موٹرسائیکل...)",
                                "Search business (e.g. Kiryana, Cloth, Auto, Bakery...)"
                            )
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = language.pick("دکان کی تفصیل درج کریں", "Enter Shop Details"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = emeraldDeep
                        )
                        OutlinedTextField(
                            value = shopNameUrdu,
                            onValueChange = { shopNameUrdu = it },
                            label = { Text(language.pick("دکان کا نام (اردو)*", "Shop Name (Urdu)*")) },
                            placeholder = { Text("مثلاً: ${selectedType.titleUrdu}") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = shopNameEnglish,
                            onValueChange = { shopNameEnglish = it },
                            label = { Text(language.pick("دکان کا نام (انگریزی)", "Shop Name (English)")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = cityUrdu,
                            onValueChange = { cityUrdu = it },
                            label = { Text(language.pick("شہر / بازار کا پتہ", "City / Market Address")) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            items(filteredBusinesses, key = { it.code }) { business ->
                val isSelected = selectedType == business
                val itemCount = BusinessCatalog.presetsFor(business).size
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedType = business
                            if (shopNameUrdu.isBlank()) shopNameUrdu = business.titleUrdu
                            if (shopNameEnglish.isBlank()) shopNameEnglish = business.titleEnglish
                        },
                    shape = RoundedCornerShape(14.dp),
                    border = if (isSelected) BorderStroke(2.dp, emeraldDeep) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) emeraldDeep else emeraldDeep.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFFFDE68A) else emeraldDeep
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = business.label(language),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = emeraldDeep
                            )
                            Text(
                                text = business.subtitle(language),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = language.pick("$itemCount تیار آئٹمز شامل ہیں", "$itemCount ready-made items included"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = emeraldDeep,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { loadPresets = !loadPresets },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = language.pick(
                            "منتخب کاروبار کی تیار آئٹم لسٹ اسٹاک میں شامل کریں",
                            "Auto-load ready-made items into inventory"
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (loadPresets) language.pick("ہاں ✓", "YES ✓") else language.pick("نہیں", "NO"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (loadPresets) emeraldDeep else MaterialTheme.colorScheme.outline
                    )
                }

                Button(
                    onClick = {
                        val finalUr = shopNameUrdu.trim().ifBlank { selectedType.titleUrdu }
                        val finalEn = shopNameEnglish.trim().ifBlank { selectedType.titleEnglish }
                        onCompleteSetup(selectedType, finalUr, finalEn, cityUrdu.trim(), loadPresets)
                    },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldDeep),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = language.pick("دکان شروع کریں ←", "Start My Store →"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFDE68A)
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
        }
    }
}
