package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.i18n.AppLanguage
import com.example.i18n.pick
import com.example.services.ReceiptPrintHelper
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.UdharCrimson
import com.example.ui.theme.WarningAmber

enum class LegalDocumentTab {
    PRIVACY_POLICY,
    TERMS_AND_CONDITIONS
}

@Composable
fun LegalDocumentsDialog(
    initialTab: LegalDocumentTab,
    language: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 740.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (activeTab == LegalDocumentTab.PRIVACY_POLICY) {
                                Icons.Default.Policy
                            } else {
                                Icons.Default.Gavel
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = if (activeTab == LegalDocumentTab.PRIVACY_POLICY) {
                                    language.pick("پرائیویسی پالیسی (Privacy Policy)", "Privacy Policy (پرائیویسی پالیسی)")
                                } else {
                                    language.pick("شرائط و ضوابط (Terms & Conditions)", "Terms & Conditions (شرائط و ضوابط)")
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "PECA 2016 • SBP RAAST • Laws of Pakistan",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("legal_dialog_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = language.pick("بند کریں", "Close"))
                    }
                }

                // Switcher between Privacy Policy & Terms
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeTab == LegalDocumentTab.PRIVACY_POLICY,
                        onClick = { activeTab = LegalDocumentTab.PRIVACY_POLICY },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("legal_tab_privacy")
                    ) {
                        Text(
                            language.pick("پرائیویسی پالیسی", "Privacy Policy"),
                            maxLines = 1
                        )
                    }
                    SegmentedButton(
                        selected = activeTab == LegalDocumentTab.TERMS_AND_CONDITIONS,
                        onClick = { activeTab = LegalDocumentTab.TERMS_AND_CONDITIONS },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("legal_tab_terms")
                    ) {
                        Text(
                            language.pick("شرائط و ضوابط", "Terms & Conditions"),
                            maxLines = 1
                        )
                    }
                }

                // Scrollable Bilingual Legal Body
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Prominent Non-Government Disclaimer Banner
                    Surface(
                        color = UdharCrimson.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, UdharCrimson.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WarningAmber,
                                    contentDescription = null,
                                    tint = UdharCrimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "غیر سرکاری ایپ ڈسکلیمر / Non-Government Disclaimer",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = UdharCrimson
                                )
                            }
                            Text(
                                text = "یہ ایک آزاد، نجی POS اور ڈیجیٹل کھاتہ ایپ ہے۔ یہ ایپ حکومتِ پنجاب (Government of Punjab)، بینک آف پنجاب (Bank of Punjab / BOP)، اسٹیٹ بینک آف پاکستان یا کسی بھی سرکاری ادارے کی آفیشل ایپ نہیں ہے اور نہ ہی اس کا کسی سرکاری ادارے سے کوئی تعلق ہے۔",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "This is an independent, private retail POS & Digital Khata software application. It is NOT affiliated with, endorsed by, sponsored by, or an official application of the Government of Punjab, the Bank of Punjab (BOP), or any government agency.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 2. Operator & Contact Details Card
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "آپریٹر اور رابطہ تفصیلات / Operator Details",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "آپریٹر / مالک: مرزا محمد آصف (Mirza Muhammad Asif)\nسٹور: احمد بلال کریانہ سٹور (Ahmad Bilal Karyana Store)\nپتہ: محلہ محمد نگر، ٹانڈہ، ضلع گجرات، پنجاب، پاکستان\nAddress: Mohalla Muhammad Nagar, Tanda, District Gujrat, Punjab, Pakistan",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable {
                                    ReceiptPrintHelper.sendViaWhatsApp(
                                        context = context,
                                        phone = "+923401558166",
                                        message = "Assalam-o-Alaikum, I have a query regarding the Ahmad Bilal Karyana Store POS app."
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = ProfitGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "WhatsApp Support: +923401558166",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfitGreen
                                )
                            }
                        }
                    }

                    if (activeTab == LegalDocumentTab.PRIVACY_POLICY) {
                        BilingualLegalSection(
                            titleUrdu = "1. لوکل ڈیٹا پرائیویسی کی ضمانت (PECA 2016)",
                            titleEnglish = "1. Local-First Data Privacy Guarantee (PECA 2016)",
                            bodyUrdu = "آپ کی دکان کا تمام کاروباری ریکارڈ (بلز، اسٹاک، کسٹمر ادھار کھاتہ، روزنامچہ، اور ذاتی بینک IBAN) مکمل طور پر آپ کے اپنے موبائل فون میں لوکل ڈیٹابیس میں محفوظ رہتا ہے۔ ہم آپ کا ڈیٹا کسی تیسری پارٹی کو فروخت یا شیئر نہیں کرتے۔ کلاؤڈ بیک اپ صرف تب چلتا ہے جب آپ خود اپنے گوگل اکاؤنٹ سے بیک اپ دبائیں۔ یہ پالیسی پاکستان کے الیکٹرانک کرائمز ایکٹ (PECA 2016) اور ای ٹی او 2002 کے اصولوں کے مطابق تیار کی گئی ہے۔",
                            bodyEnglish = "All store records—including bills, inventory, customer Udhar Khata, Daily Roznamcha, and personal bank IBAN settings—are stored strictly on your own mobile device by default. We never sell, rent, or secretly harvest your business or customer data. Optional cloud backup runs only when you explicitly connect your Google account and trigger a backup. Data handling follows Pakistani legal principles including the Prevention of Electronic Crimes Act (PECA 2016) and Electronic Transactions Ordinance (ETO 2002)."
                        )

                        BilingualLegalSection(
                            titleUrdu = "2. NFC، کیمرہ، GPS اور بلوٹوتھ اجازت ناموں کی وضاحت",
                            titleEnglish = "2. NFC, Camera, GPS & Bluetooth Permission Disclosures",
                            bodyUrdu = "• NFC: صرف POS اسکرین پر فزیکل کارڈ کا UID/نمبر پڑھنے کے لیے استعمال ہوتا ہے۔ ایپ کبھی بھی بینک PIN، پاس ورڈ یا خفیہ CVV نہیں پڑھتی اور نہ محفوظ کرتی ہے۔\n• کیمرہ (Camera): صرف بارکوڈ یا QR کوڈ سکین کرنے کے لیے آن ڈیمانڈ کھلتا ہے؛ پسِ منظر میں کوئی تصویر یا ویڈیو ریکارڈ نہیں کی جاتی۔\n• GPS لوکیشن: صرف اس وقت استعمال ہوتی ہے جب آپ «موجودہ لوکیشن حاصل کریں» بٹن دبائیں (آرڈر ڈلیوری یا دکان کا پتہ سیٹ کرنے کے لیے)۔ پسِ منظر (Background) میں کوئی لوکیشن ٹریکنگ نہیں کی جاتی۔\n• بلوٹوتھ (Bluetooth): صرف 58mm تھرمل رسید پرنٹر سے کنیکٹ ہو کر بل پرنٹ کرنے کے لیے استعمال ہوتا ہے۔",
                            bodyEnglish = "• NFC: Used strictly while the app is open on the POS screen to read physical card UID/tag info for billing speed. The app never reads, stores, or transmits bank PINs, passwords, or CVVs.\n• Camera: Accessed strictly on-demand when scanning product barcodes or QR codes; no silent photo/video capture occurs.\n• GPS Location (Foreground Only): Requested only when you tap 'Auto-Detect Location' to attach a Google Maps delivery link to your WhatsApp order or save your shop address. We never track location in the background.\n• Bluetooth: Used solely to print 58mm ESC/POS receipts to paired thermal printers."
                        )

                        BilingualLegalSection(
                            titleUrdu = "3. صارف کے حقوق اور ڈیٹا حذف کرنا",
                            titleEnglish = "3. User Control, Editing & Deletion Rights",
                            bodyUrdu = "آپ کسی بھی وقت اپنے بل، کھاتہ انٹری، روزنامچہ اندراج یا دکان کا پروفائل ایڈٹ یا ڈیلیٹ کر سکتے ہیں۔ ایپ ان انسٹال کرنے یا فون سے ایپ ڈیٹا کلیئر کرنے پر تمام لوکل ڈیٹا مستقل طور پر حذف ہو جاتا ہے۔ کسی بھی شکایت یا مدد کے لیے واٹس ایپ +923401558166 پر رابطہ کریں۔",
                            bodyEnglish = "You retain full ownership and control over your records and may edit or delete any bill, Khata entry, Roznamcha transaction, or profile detail at any time. Clearing app storage or uninstalling permanently deletes local device data. For privacy inquiries under Pakistani law, contact Mirza Muhammad Asif via WhatsApp at +923401558166."
                        )
                    } else {
                        BilingualLegalSection(
                            titleUrdu = "1. سافٹ ویئر کا استعمال اور غیر سرکاری حیثیت",
                            titleEnglish = "1. Scope of Use & Non-Government Status",
                            bodyUrdu = "یہ ایپ دکانداروں کے لیے ایک نجی بلنگ، اسٹاک، روزنامچہ اور ادھار کھاتہ ٹول ہے۔ یہ حکومتِ پنجاب، مریم نواز راشن کارڈ پروگرام یا بینک آف پنجاب کا سرکاری پورٹل نہیں ہے۔ کارڈ کوٹہ، سروس فیس اور اشیاء کے ریٹس دکاندار خود درج اور تصدیق کرتا ہے۔",
                            bodyEnglish = "This software is provided as an independent private billing, inventory, Roznamcha, and Khata utility for retail merchants. It is NOT an official portal of the Government of Punjab or Bank of Punjab. All item rates, card quotas, and service fees are entered and verified solely by the shopkeeper."
                        )

                        BilingualLegalSection(
                            titleUrdu = "2. SBP RAAST اور بینک پیمنٹ QR کی ذمہ داری",
                            titleEnglish = "2. SBP RAAST & Bank QR Payment Responsibility Rules",
                            bodyUrdu = "اسٹیٹ بینک آف پاکستان (SBP) کے RAAST / EMVCo QR اصولوں کے مطابق، یہ ایپ صرف آپ کے درج کردہ ذاتی بینک IBAN سے معیاری QR کوڈ بناتی ہے۔ ایپ خود کوئی بینک، پیمنٹ گیٹ وے یا رقم رکھنے والا ادارہ نہیں ہے۔ ہر دکاندار کی ذاتی ذمہ داری ہے کہ:\n• درست ذاتی IBAN اور بینک نام درج کرے۔\n• سامان دینے یا بل وصول شدہ قرار دینے سے پہلے اپنی آفیشل بینک ایپ یا بینک SMS میں رقم کی آمد کی خود تصدیق کرے۔\nغلط IBAN یا غیر تصدیق شدہ ٹرانسفر کی صورت میں ایپ آپریٹر کسی مالی نقصان کا ذمہ دار نہیں ہوگا۔",
                            bodyEnglish = "In alignment with State Bank of Pakistan (SBP) RAAST / EMVCo QR guidelines, this app merely formats a standard QR code using the shopkeeper's self-entered bank IBAN. The app is NOT a bank, payment gateway, or escrow intermediary and never processes or holds funds. Each shopkeeper is solely responsible for:\n• Entering an accurate personal Bank Name & IBAN.\n• Independently verifying incoming RAAST/IBFT funds directly in their official banking app or bank SMS before handing over goods or marking any bill paid.\nThe operator bears no liability for misdirected transfers or unverified payments."
                        )

                        BilingualLegalSection(
                            titleUrdu = "3. راشن امداد فنڈ اور کاروباری حسابات",
                            titleEnglish = "3. Charity Fund Verification & Business Accuracy",
                            bodyUrdu = "راشن امداد فنڈ میں درج عطیات سرکاری یا آٹومیٹک تصدیق شدہ نہیں ہوتے بلکہ اس وقت تک «تصدیق باقی (PENDING)» رہتے ہیں جب تک مرکزی HBL اکاؤنٹ اسٹیٹمنٹ میں رقم کی تصدیق نہ ہو جائے۔ دکاندار اپنے تمام بلز، قیمتوں، کسٹمر کھاتوں اور پاکستانی ٹیکس/تجارتی قوانین کی تعمیل کا خود ذمہ دار ہے۔",
                            bodyEnglish = "Charity fund records remain PENDING until manually confirmed against official bank statements. Shopkeepers are solely responsible for the accuracy of their pricing, manual bill edits, customer Khata balances, Roznamcha entries, and compliance with applicable Pakistani commercial and tax laws."
                        )

                        BilingualLegalSection(
                            titleUrdu = "4. نافذ العمل قانون اور عدالتی دائرہ اختیار (پاکستان)",
                            titleEnglish = "4. Governing Law & Jurisdiction (Pakistan)",
                            bodyUrdu = "ان شرائط و ضوابط پر اسلامی جمہوریہ پاکستان کے قوانین (بشمول PECA 2016، کنٹریکٹ ایکٹ 1872 اور SBP ریگولیشنز) لاگو ہوں گے۔ کسی بھی قانونی تنازعہ کی صورت میں ضلع گجرات / پنجاب، پاکستان کی مجاز عدالتوں کو اختیارِ سماعت حاصل ہوگا۔",
                            bodyEnglish = "These Terms & Conditions are governed by and construed in accordance with the laws of the Islamic Republic of Pakistan (including PECA 2016, the Contract Act 1872, and SBP payment regulations). Any dispute arising from use of this application shall be subject to the exclusive jurisdiction of competent courts in District Gujrat, Punjab, Pakistan."
                        )
                    }
                }

                HorizontalDivider()

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("legal_dialog_accept_close_button")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(language.pick("سمجھ گیا / بند کریں", "Understood / Close"))
                }
            }
        }
    }
}

@Composable
private fun BilingualLegalSection(
    titleUrdu: String,
    titleEnglish: String,
    bodyUrdu: String,
    bodyEnglish: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = titleUrdu,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = titleEnglish,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = bodyUrdu,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Text(
                text = bodyEnglish,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
