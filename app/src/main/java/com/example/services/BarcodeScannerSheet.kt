package com.example.services

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.ShopSettings
import com.example.i18n.AppLanguage
import com.example.i18n.formatPkr
import com.example.i18n.pick
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ProfitGreen
import com.example.ui.theme.SaffronSecondary
import com.example.ui.theme.UdharCrimson
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

@Composable
fun BarcodeScannerDialog(
    language: AppLanguage,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var manualCode by remember { mutableStateOf("") }
    var alreadyDetected by remember { mutableStateOf(false) }
    var torchEnabled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = language.pick("بارکوڈ / QR اسکینر", "Barcode & QR Scanner"),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = language.pick("بند کریں", "Close")
                        )
                    }
                }

                if (hasPermission) {
                    LiveCameraQrScannerView(
                        language = language,
                        torchEnabled = torchEnabled,
                        onToggleTorch = { torchEnabled = !torchEnabled },
                        onDetected = { code ->
                            if (!alreadyDetected && code.isNotBlank()) {
                                alreadyDetected = true
                                onBarcodeScanned(code)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                    Text(
                        text = language.pick(
                            "پروڈکٹ کے بارکوڈ یا QR کوڈ کو کیمرے کے فریم کے سامنے رکھیں",
                            "Hold product barcode or QR code inside the frame to scan"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = language.pick(
                                    "کیمرہ سے بارکوڈ اسکین کرنے کے لیے اجازت درکار ہے",
                                    "Camera permission is required to scan barcodes"
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                modifier = Modifier.testTag("grant_camera_button")
                            ) {
                                Text(language.pick("کیمرہ کی اجازت دیں", "Allow Camera"))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))
                Text(
                    text = language.pick("یا بارکوڈ نمبر دستی درج کریں:", "Or enter barcode manually:"),
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCode,
                        onValueChange = { manualCode = it },
                        placeholder = { Text("8961001000011") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_barcode_input")
                    )
                    Button(
                        onClick = {
                            if (manualCode.isNotBlank()) onBarcodeScanned(manualCode.trim())
                        },
                        enabled = manualCode.isNotBlank(),
                        modifier = Modifier.testTag("submit_manual_barcode_button")
                    ) {
                        Text(language.pick("تلاش", "Find"))
                    }
                }
            }
        }
    }
}

/**
 * Reusable live rear-camera QR & Barcode scanner with viewfinder overlay and flashlight toggle.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun LiveCameraQrScannerView(
    language: AppLanguage,
    torchEnabled: Boolean,
    onToggleTorch: () -> Unit,
    onDetected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isPaused: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val previewView = remember { PreviewView(context) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var hasFlashUnit by remember { mutableStateOf(false) }

    LaunchedEffect(torchEnabled, boundCamera) {
        try {
            if (boundCamera?.cameraInfo?.hasFlashUnit() == true) {
                boundCamera?.cameraControl?.enableTorch(torchEnabled)
            }
        } catch (_: Exception) {
            // Gracefully ignore if torch control unavailable
        }
    }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    if (isPaused) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val raw = barcodes.firstOrNull()?.rawValue
                                if (!raw.isNullOrBlank()) {
                                    onDetected(raw)
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                boundCamera = camera
                hasFlashUnit = camera.cameraInfo.hasFlashUnit()
                if (hasFlashUnit && torchEnabled) {
                    camera.cameraControl.enableTorch(true)
                }
            } catch (e: Exception) {
                Log.w("BarcodeScanner", "Camera binding failed: ${e.message}")
            }
        }
        cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                boundCamera?.cameraControl?.enableTorch(false)
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
            scanner.close()
            executor.shutdown()
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxW = size.width * 0.72f
            val boxH = size.height * 0.56f
            val left = (size.width - boxW) / 2f
            val top = (size.height - boxH) / 2f
            drawRoundRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                cornerRadius = CornerRadius(24f, 24f),
                style = Stroke(width = 6f)
            )
            drawLine(
                color = Color(0xFF4ADE80),
                start = Offset(left + 16f, size.height / 2f),
                end = Offset(left + boxW - 16f, size.height / 2f),
                strokeWidth = 4f
            )
        }
        if (hasFlashUnit || true) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
            ) {
                IconButton(
                    onClick = onToggleTorch,
                    modifier = Modifier.testTag("camera_flashlight_toggle")
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = language.pick("فلیش لائٹ", "Toggle Flashlight"),
                        tint = if (torchEnabled) Color(0xFFFDE68A) else Color.White
                    )
                }
            }
        }
    }
}

private val QR_QUICK_AMOUNTS = listOf(500, 1000, 2000, 5000)

/**
 * 2-Tab "QR سکین و پیمنٹ" Modal:
 * - Tab 1: میرا بینک پیمنٹ QR کوڈ (Official EMVCo / Pakistan RAAST QR with CRC-16, Bank IBAN,
 *   Merchant Name, Currency PKR 586, optional amount + "پیمنٹ موصول ہو گئی — کھاتے میں جمع کریں" CTA)
 * - Tab 2: کیو آر / کارڈ سکین کریں (Live rear CameraX scanner with Flashlight Torch toggle + smart parser)
 */
@Composable
fun QrScanAndPaymentDialog(
    language: AppLanguage,
    settings: ShopSettings,
    initialTab: Int = 0,
    onRecordPaymentReceived: (amountPkr: Double, referenceNote: String) -> Unit,
    onScannedProductBarcode: (String) -> Unit,
    onScannedCardNumber: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 1)) }

    // Tab 1: My Bank Payment EMVCo / RAAST QR state
    var amountStr by remember { mutableStateOf("") }
    var referenceNote by remember { mutableStateOf("") }
    var showAmountRequiredError by remember { mutableStateOf(false) }

    val parsedAmount = amountStr.toDoubleOrNull()?.takeIf { it > 0.0 }
    val merchantName = remember(settings.storeNameEnglish, settings.hblAccountTitle) {
        settings.storeNameEnglish
            .ifBlank { settings.hblAccountTitle }
            .ifBlank { RaastEmvQrHelper.DEFAULT_MERCHANT_NAME }
    }
    val merchantCity = remember(settings.shopAddressEnglish) {
        settings.shopAddressEnglish
            .split(",")
            .firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: RaastEmvQrHelper.DEFAULT_MERCHANT_CITY
    }
    val emvPayload = remember(
        settings.hblIban,
        settings.hblAccountNumber,
        merchantName,
        merchantCity,
        parsedAmount,
        referenceNote
    ) {
        RaastEmvQrHelper.buildRaastEmvQrPayload(
            iban = settings.hblIban,
            accountNumber = settings.hblAccountNumber,
            merchantName = merchantName,
            merchantCity = merchantCity,
            amountPkr = parsedAmount,
            billReference = referenceNote
        )
    }
    val crcHex = remember(emvPayload) { emvPayload.takeLast(4) }
    val qrBitmap by produceState<Bitmap?>(initialValue = null, key1 = emvPayload) {
        value = withContext(Dispatchers.Default) {
            RaastEmvQrHelper.generateQrBitmap(emvPayload, 512)
        }
    }

    // Tab 2: CameraX QR / Card Scanner state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var torchEnabled by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<ParsedScannedCode?>(null) }
    var manualCode by remember { mutableStateOf("") }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(activeTab) {
        if (activeTab == 1 && !hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
        if (activeTab != 1) {
            torchEnabled = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 20.dp)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = EmeraldPrimary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = language.pick("QR سکین و پیمنٹ", "QR Scan & Payment"),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = language.pick(
                                    "EMVCo / پاکستان RAAST پیمنٹ اور کیمرہ اسکینر",
                                    "EMVCo / Pakistan RAAST QR & Camera Scanner"
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_qr_payment_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = language.pick("بند کریں", "Close")
                        )
                    }
                }

                // 2-Tab Selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                        modifier = Modifier.testTag("qr_modal_tab_my_qr")
                    ) {
                        Text(
                            text = language.pick("میرا بینک پیمنٹ QR کوڈ", "My Bank Payment QR"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    SegmentedButton(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                        modifier = Modifier.testTag("qr_modal_tab_scan")
                    ) {
                        Text(
                            text = language.pick("کیو آر / کارڈ سکین کریں", "Scan QR / Card"),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }

                if (activeTab == 0) {
                    // TAB 1: Official EMVCo / Pakistan RAAST Payment QR
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = EmeraldPrimary.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "EMVCo • RAAST • PKR (${RaastEmvQrHelper.CURRENCY_PKR}) • CRC-16: $crcHex",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                            }
                        }

                        // QR Code Display Frame
                        val bmp = qrBitmap
                        if (bmp != null) {
                            Box(
                                modifier = Modifier
                                    .size(216.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White)
                                    .border(2.5.dp, EmeraldPrimary, RoundedCornerShape(18.dp))
                                    .padding(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = language.pick(
                                        "میرا بینک پیمنٹ RAAST کیو آر کوڈ",
                                        "My Bank Payment RAAST QR Code"
                                    ),
                                    modifier = Modifier.fillMaxSize()
                                )
                                Surface(
                                    color = EmeraldPrimary,
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .border(2.dp, Color.White, CircleShape)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = settings.bankName.take(4).uppercase().ifBlank { "BANK" },
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        // Saved Bank Account Details Card
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${settings.bankName} • ${settings.hblAccountTitle}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboard.setText(AnnotatedString(settings.hblIban))
                                            onShowMessage(
                                                language.pick(
                                                    "IBAN کاپی ہو گیا: ${settings.hblIban}",
                                                    "IBAN copied: ${settings.hblIban}"
                                                )
                                            )
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = language.pick("IBAN کاپی", "Copy IBAN"),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${language.pick("دکان / مرچنٹ", "Merchant")}: $merchantName",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "IBAN: ${settings.hblIban}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (settings.hblAccountNumber.isNotBlank()) {
                                    Text(
                                        text = "${language.pick("اکاؤنٹ نمبر", "Account #")}: ${settings.hblAccountNumber} • ${settings.hblBranchName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Optional Amount Input + Quick Chips
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { raw ->
                                amountStr = raw.filter { it.isDigit() || it == '.' }
                                showAmountRequiredError = false
                            },
                            label = {
                                Text(language.pick("رقم (اختیاری - روپے PKR)", "Optional Amount (PKR)"))
                            },
                            placeholder = {
                                Text(language.pick("مثلاً 1500 (خالی = کوئی بھی رقم)", "e.g. 1500 (blank = any amount)"))
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("raast_qr_amount_input")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = parsedAmount == null,
                                onClick = { amountStr = "" },
                                label = { Text(language.pick("Static (آزاد)", "Static (Any)")) }
                            )
                            QR_QUICK_AMOUNTS.forEach { amt ->
                                FilterChip(
                                    selected = parsedAmount?.toLong() == amt.toLong(),
                                    onClick = {
                                        amountStr = amt.toString()
                                        showAmountRequiredError = false
                                    },
                                    label = { Text(if (language == AppLanguage.URDU) "روپے $amt" else "Rs. $amt") }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = referenceNote,
                            onValueChange = { referenceNote = it },
                            label = {
                                Text(language.pick("حوالہ / کسٹمر کا نام (اختیاری)", "Reference / Customer Note (optional)"))
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("raast_qr_ref_input")
                        )

                        if (showAmountRequiredError) {
                            Text(
                                text = language.pick(
                                    "براہ کرم کھاتے میں جمع کرنے کے لیے موصول شدہ رقم درج کریں۔",
                                    "Please enter the received PKR amount to credit into the ledger."
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = UdharCrimson
                            )
                        }

                        // Primary Credit-to-Account CTA Button
                        Button(
                            onClick = {
                                val amt = parsedAmount
                                if (amt == null || amt <= 0.0) {
                                    showAmountRequiredError = true
                                } else {
                                    onRecordPaymentReceived(amt, referenceNote.trim())
                                    onDismiss()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ProfitGreen,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("record_qr_payment_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                text = language.pick(
                                    "پیمنٹ موصول ہو گئی — کھاتے میں جمع کریں",
                                    "Payment Received — Credit to Account"
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    // TAB 2: Live Rear CameraX QR / Card Scanner with Flashlight Torch Toggle
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (hasCameraPermission) {
                            LiveCameraQrScannerView(
                                language = language,
                                torchEnabled = torchEnabled,
                                onToggleTorch = { torchEnabled = !torchEnabled },
                                isPaused = scannedResult != null,
                                onDetected = { raw ->
                                    if (scannedResult == null && raw.isNotBlank()) {
                                        NfcRationCardReader.playScanFeedback(context)
                                        scannedResult = RaastEmvQrHelper.parseScannedCode(raw)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = language.pick(
                                        "QR کوڈ، بینک QR، یا کارڈ بارکوڈ کیمرے کے سامنے رکھیں",
                                        "Point rear camera at any QR, Bank QR, or Card barcode"
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                FilledTonalButton(
                                    onClick = { torchEnabled = !torchEnabled },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.testTag("qr_tab_torch_pill")
                                ) {
                                    Icon(
                                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = null,
                                        tint = if (torchEnabled) SaffronSecondary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(Modifier.size(4.dp))
                                    Text(
                                        text = if (torchEnabled) {
                                            language.pick("فلیش آن", "Torch ON")
                                        } else {
                                            language.pick("فلیش آف", "Torch OFF")
                                        },
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = language.pick(
                                            "کیو آر / کارڈ سکین کرنے کے لیے کیمرہ کی اجازت درکار ہے",
                                            "Camera permission is required to scan QR codes and cards"
                                        ),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Button(
                                        onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                                        modifier = Modifier.testTag("qr_modal_grant_camera_button")
                                    ) {
                                        Text(language.pick("کیمرہ کی اجازت دیں", "Allow Camera"))
                                    }
                                }
                            }
                        }

                        // Scanned Result Smart Action Card
                        val parsed = scannedResult
                        if (parsed != null) {
                            Surface(
                                color = ProfitGreen.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.5.dp, ProfitGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = parsed.title.ifBlank { language.pick("اسکین شدہ کوڈ", "Scanned Code") },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfitGreen
                                        )
                                        OutlinedButton(
                                            onClick = { scannedResult = null },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.size(4.dp))
                                            Text(language.pick("دوبارہ", "Again"), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Text(
                                        text = parsed.accountOrIban.ifBlank { parsed.rawValue },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (parsed.amount != null && parsed.amount > 0.0) {
                                        Text(
                                            text = "${language.pick("رقم", "Amount")}: ${formatPkr(parsed.amount, language)}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val toCopy = parsed.accountOrIban.ifBlank { parsed.rawValue }
                                                clipboard.setText(AnnotatedString(toCopy))
                                                onShowMessage(language.pick("کاپی ہو گیا: $toCopy", "Copied: $toCopy"))
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.size(4.dp))
                                            Text(language.pick("کاپی", "Copy"))
                                        }
                                        when (parsed.kind) {
                                            ScannedCodeKind.PRODUCT_BARCODE -> {
                                                Button(
                                                    onClick = {
                                                        onScannedProductBarcode(parsed.rawValue)
                                                        onDismiss()
                                                    },
                                                    modifier = Modifier.weight(1.3f)
                                                ) {
                                                    Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.size(4.dp))
                                                    Text(language.pick("بل میں شامل", "Add to Bill"))
                                                }
                                            }
                                            ScannedCodeKind.RATION_OR_CNIC_CARD -> {
                                                Button(
                                                    onClick = {
                                                        onScannedCardNumber(parsed.accountOrIban.ifBlank { parsed.rawValue })
                                                        onDismiss()
                                                    },
                                                    modifier = Modifier.weight(1.3f)
                                                ) {
                                                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.size(4.dp))
                                                    Text(language.pick("POS کارڈ میں کھولیں", "Use in POS"))
                                                }
                                            }
                                            else -> {
                                                Button(
                                                    onClick = {
                                                        onScannedCardNumber(parsed.accountOrIban.ifBlank { parsed.rawValue })
                                                        onDismiss()
                                                    },
                                                    modifier = Modifier.weight(1.3f)
                                                ) {
                                                    Icon(Icons.Default.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.size(4.dp))
                                                    Text(language.pick("POS / استعمال", "Use Code"))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider()

                        // Manual fallback input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualCode,
                                onValueChange = { manualCode = it },
                                placeholder = { Text(language.pick("یا کارڈ / بارکوڈ نمبر دستی لکھیں", "Or type card / barcode manually")) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    val clean = manualCode.trim()
                                    if (clean.isNotBlank()) {
                                        scannedResult = RaastEmvQrHelper.parseScannedCode(clean)
                                    }
                                },
                                enabled = manualCode.isNotBlank()
                            ) {
                                Text(language.pick("پڑھیں", "Parse"))
                            }
                        }
                    }
                }
            }
        }
    }
}
