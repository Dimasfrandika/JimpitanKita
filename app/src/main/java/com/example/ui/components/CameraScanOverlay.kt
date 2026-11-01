package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.House
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.FocusMeteringAction
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.GlobalHistogramBinarizer
import kotlinx.coroutines.launch
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.delay
import java.util.concurrent.Executors

/**
 * Pemindai Kamera Nyata untuk QR Code Jimpitan Warga
 * Menggunakan CameraX Live Preview + ZXing ImageAnalysis Analyzer
 * Dilengkapi dengan reticle viewfinder, senter (torch), dan konfirmasi visual instan.
 */
@Composable
fun CameraScanOverlay(
    houses: List<House>,
    onDismiss: () -> Unit,
    onHouseScanned: (House) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isFlashlightOn by remember { mutableStateOf(false) }
    var scannedHouse by remember { mutableStateOf<House?>(null) }
    var isSuccessAnimActive by remember { mutableStateOf(false) }
    var unrecognizedQrMessage by remember { mutableStateOf<String?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var analyzerRef by remember { mutableStateOf<QrCodeAnalyzer?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
            try {
                cameraProviderRef?.unbindAll()
            } catch (_: Exception) {}
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    // Infinite animation for laser scan line
    val infiniteTransition = rememberInfiniteTransition(label = "laser_sweep")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_pos"
    )

    // Handle scan success trigger with haptic and audio feedback
    val onQrDecoded: (String) -> Unit = { rawText ->
        if (!isSuccessAnimActive) {
            val matchedHouse = matchHouseFromQr(rawText, houses)
            if (matchedHouse != null) {
                isSuccessAnimActive = true
                scannedHouse = matchedHouse
                unrecognizedQrMessage = null

                // Haptic feedback
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(120)
                    }
                } catch (_: Exception) {}

                // Audio feedback
                com.example.util.NotificationSoundHelper.playNotificationAlert(context)
            } else {
                unrecognizedQrMessage = "QR Terdeteksi: \"$rawText\" (Bukan kode warga RT)"
                // Auto resume scanning after 1.8 seconds so user can aim properly
                coroutineScope.launch {
                    delay(1800)
                    unrecognizedQrMessage = null
                    analyzerRef?.resume()
                }
            }
        }
    }

    LaunchedEffect(scannedHouse) {
        if (scannedHouse != null) {
            delay(600) // Brief visual feedback delay before opening input form
            scannedHouse?.let { onHouseScanned(it) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("camera_scan_overlay_root")
        ) {
            if (hasCameraPermission) {
                // Real CameraX Live Preview Stream with TextureView mode to prevent BufferQueue abandoned errors
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }

                        // Touch to focus anywhere on screen
                        previewView.setOnTouchListener { view, event ->
                            if (event.action == android.view.MotionEvent.ACTION_UP) {
                                try {
                                    val factory = previewView.meteringPointFactory
                                    val point = factory.createPoint(event.x, event.y)
                                    val action = FocusMeteringAction.Builder(
                                        point,
                                        FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                    )
                                        .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                        .build()
                                    cameraControl?.startFocusAndMetering(action)
                                } catch (_: Exception) {}
                                view.performClick()
                            }
                            true
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                cameraProviderRef = provider

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val analyzer = QrCodeAnalyzer { qrText ->
                                    ContextCompat.getMainExecutor(ctx).execute {
                                        onQrDecoded(qrText)
                                    }
                                }
                                analyzerRef = analyzer

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setTargetResolution(android.util.Size(720, 1280))
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                provider.unbindAll()
                                val camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )

                                cameraControl = camera.cameraControl

                                // Trigger initial center autofocus for sharp close-up QR scanning
                                previewView.post {
                                    try {
                                        val factory = previewView.meteringPointFactory
                                        val point = factory.createPoint(
                                            previewView.width / 2f,
                                            previewView.height / 2f
                                        )
                                        val action = FocusMeteringAction.Builder(
                                            point,
                                            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                        )
                                            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
                                            .build()
                                        camera.cameraControl.startFocusAndMetering(action)
                                    } catch (_: Exception) {}
                                }
                            } catch (e: Exception) {
                                Log.e("CameraScanOverlay", "Inisialisasi CameraX error: ${e.message}", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Fallback Permission Required UI
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0F172A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Izin Kamera Diperlukan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Untuk memindai stiker kode QR kaleng jimpitan warga, izinkan aplikasi mengakses kamera HP Anda.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = Color(0xFFCBD5E1)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Beri Izin Kamera", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Viewfinder Dark Mask & Laser Guide Cutout
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val frameSize = (canvasWidth * 0.72f).coerceAtMost(300.dp.toPx())
                val frameLeft = (canvasWidth - frameSize) / 2f
                val frameTop = (canvasHeight * 0.28f)

                val cutoutRect = Rect(
                    offset = Offset(frameLeft, frameTop),
                    size = Size(frameSize, frameSize)
                )

                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            rect = cutoutRect,
                            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx())
                        )
                    )
                }

                // Dark semi-transparent mask outside viewfinder
                clipPath(path, clipOp = ClipOp.Difference) {
                    drawRect(
                        color = Color(0xCC000000),
                        size = size
                    )
                }

                // Laser Scanning Sweep Line inside cutout
                clipPath(path) {
                    if (!isSuccessAnimActive && hasCameraPermission) {
                        val laserY = frameTop + (frameSize * laserPosition)
                        drawLine(
                            color = Color(0xFF10B981),
                            start = Offset(frameLeft + 16f, laserY),
                            end = Offset(frameLeft + frameSize - 16f, laserY),
                            strokeWidth = 3.5.dp.toPx()
                        )
                    }
                }
            }

            // Top Bar Controls (Close & Flashlight/Senter)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x881E293B))
                        .testTag("scan_overlay_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup Pemindai",
                        tint = Color.White
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Pemindai QR Jimpitan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Arahkan kamera ke stiker kaleng warga",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                IconButton(
                    onClick = {
                        isFlashlightOn = !isFlashlightOn
                        try {
                            cameraControl?.enableTorch(isFlashlightOn)
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isFlashlightOn) Color(0xFFEAB308) else Color(0x881E293B))
                        .testTag("scan_overlay_flash_btn")
                ) {
                    Icon(
                        imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Senter",
                        tint = if (isFlashlightOn) Color.Black else Color.White
                    )
                }
            }

            // Central Viewfinder Reticle Frame Guide
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                val frameBorderColor = if (isSuccessAnimActive) Color(0xFF22C55E) else Color(0xFF10B981)

                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .border(
                            width = if (isSuccessAnimActive) 3.5.dp else 1.5.dp,
                            color = frameBorderColor.copy(alpha = if (isSuccessAnimActive) 1f else 0.4f),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .testTag("scanner_target_frame"),
                    contentAlignment = Alignment.Center
                ) {
                    // Frame Corner Accents
                    CornerBrackets(color = frameBorderColor)

                    // Target Reticle Icon
                    if (!isSuccessAnimActive) {
                        Icon(
                            imageVector = Icons.Default.CropFree,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.25f),
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    // Visual Feedback Upon Successful Scan
                    AnimatedVisibility(
                        visible = isSuccessAnimActive,
                        enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF22C55E),
                            shadowElevation = 8.dp,
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Berhasil Memindai",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Feedback / Status Banner
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success House Banner
                AnimatedVisibility(
                    visible = isSuccessAnimActive && scannedHouse != null,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut()
                ) {
                    scannedHouse?.let { house ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF064E3B),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF22C55E)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4ADE80),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "QR Valid: ${house.block} ${house.houseNumber}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Kepala Keluarga: ${house.headOfFamily}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFA7F3D0)
                                    )
                                }
                            }
                        }
                    }
                }

                // Unrecognized QR warning
                if (unrecognizedQrMessage != null && !isSuccessAnimActive) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF7F1D1D),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = unrecognizedQrMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * Match raw scanned text from QR code to a registered House
 */
private fun matchHouseFromQr(rawText: String, houses: List<House>): House? {
    val clean = rawText.trim()
    if (clean.isEmpty()) return null

    // 1. Exact match on qrPayload
    houses.find { it.qrPayload.equals(clean, ignoreCase = true) }?.let { return it }

    // 2. Exact match on ID
    houses.find { it.id.equals(clean, ignoreCase = true) }?.let { return it }

    // 3. Normalized comparison (removing dashes, colons, spaces)
    val normClean = clean.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
    houses.find {
        val normPayload = it.qrPayload.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        val normId = it.id.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        normClean == normPayload || normClean == normId || normClean.endsWith(normId) || normId.endsWith(normClean)
    }?.let { return it }

    // 4. JIMPITAN prefix parsing: JIMPITAN:RT02:A01 or JIMPITAN:RT02:RT02-A01
    if (clean.contains("JIMPITAN", ignoreCase = true)) {
        val lastSegment = clean.substringAfterLast(":").trim()
        if (lastSegment.isNotEmpty()) {
            houses.find {
                it.id.contains(lastSegment, ignoreCase = true) ||
                it.qrPayload.contains(lastSegment, ignoreCase = true) ||
                it.houseNumber.contains(lastSegment, ignoreCase = true) ||
                it.block.contains(lastSegment, ignoreCase = true)
            }?.let { return it }
        }
    }

    // 5. Block and Number match (e.g. "Blok A 01" or "A01" or "A-01")
    houses.find {
        val blockClean = it.block.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        val numClean = it.houseNumber.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
        normClean.contains(blockClean) && normClean.contains(numClean)
    }?.let { return it }

    // 6. Substring match
    houses.find {
        clean.contains(it.id, ignoreCase = true) ||
        it.id.contains(clean, ignoreCase = true)
    }?.let { return it }

    return null
}

/**
 * CameraX ImageAnalysis Analyzer yang membaca QR Code menggunakan ZXing
 * Menggunakan toBitmap() untuk menjamin decoding akurat tanpa distorsi rowStride YUV
 */
private class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf<DecodeHintType, Any>(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        setHints(hints)
    }

    @Volatile
    private var isScanning = true

    fun resume() {
        isScanning = true
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        try {
            val originalBitmap = imageProxy.toBitmap()
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            val rotatedBitmap = if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, false)
            } else {
                originalBitmap
            }

            val width = rotatedBitmap.width
            val height = rotatedBitmap.height
            val pixels = IntArray(width * height)
            rotatedBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            var rawResult: Result? = null

            // 1. Coba HybridBinarizer standar
            try {
                rawResult = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            } catch (_: Exception) {
                // 2. Coba GlobalHistogramBinarizer (lebih optimal untuk pencahayaan ronda malam & stiker kaleng)
                try {
                    rawResult = reader.decodeWithState(BinaryBitmap(GlobalHistogramBinarizer(source)))
                } catch (_: Exception) {
                    // 3. Coba inversi warna bila stiker memantulkan cahaya senter
                    try {
                        rawResult = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source.invert())))
                    } catch (_: Exception) {
                        // QR belum terbaca pada frame ini
                    }
                }
            } finally {
                reader.reset()
            }

            if (rawResult != null && rawResult.text.isNotBlank()) {
                isScanning = false
                onQrCodeScanned(rawResult.text)
            }
        } catch (e: Exception) {
            Log.e("QrCodeAnalyzer", "Analyzer frame error: ${e.message}", e)
        } finally {
            imageProxy.close()
        }
    }
}

/**
 * L-shaped corner brackets for viewfinder frame
 */
@Composable
private fun CornerBrackets(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = 4.dp.toPx()
        val cornerLen = 28.dp.toPx()
        val w = size.width
        val h = size.height

        // Top-Left Corner
        drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), stroke)
        drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), stroke)

        // Top-Right Corner
        drawLine(color, Offset(w, 0f), Offset(w - cornerLen, 0f), stroke)
        drawLine(color, Offset(w, 0f), Offset(w, cornerLen), stroke)

        // Bottom-Left Corner
        drawLine(color, Offset(0f, h), Offset(cornerLen, h), stroke)
        drawLine(color, Offset(0f, h), Offset(0f, h - cornerLen), stroke)

        // Bottom-Right Corner
        drawLine(color, Offset(w, h), Offset(w - cornerLen, h), stroke)
        drawLine(color, Offset(w, h), Offset(w, h - cornerLen), stroke)
    }
}
