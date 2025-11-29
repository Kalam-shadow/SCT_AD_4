package com.example.qrnova

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ScanScreen(
    viewModel: QrHistoryViewModel,
    activity: MainActivity,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var zoomState by remember { mutableFloatStateOf(1f) }

    // var isGestureZoom by remember { mutableStateOf(false) }
    // var scanState by remember { mutableStateOf(false) }

    var isTorchOn by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()
    val permissionGranted = remember { mutableStateOf(false) }
    val showPermissionDeniedUI = remember { mutableStateOf(false) }

    //Result from BottomSheet
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    var result by remember { mutableStateOf<String?>(null) }

//    LaunchedEffect(shouldResetScanState.value) {
//        if (shouldResetScanState.value) {
//            scanState = false
//            shouldResetScanState.value = false
//        }
//    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    if (sheetState.isVisible && result != null) {
        ModalBottomSheet(
            onDismissRequest = {result = null},
            sheetState = sheetState
        ) {
            ResultContent(result!!,viewModel,context)
        }
    }

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { resultState ->
        if (resultState.resultCode == Activity.RESULT_OK) {
            val outputUri = UCrop.getOutput(resultState.data!!)
            outputUri?.let { uri ->
                val qrText = decodeQRCodeFromImage(context, uri) ?: "No QR found"
                result = qrText
                viewModel.addScanned(result!!)
                scope.launch {
                    sheetState.show()
                }
            }
        } else if (resultState.resultCode == UCrop.RESULT_ERROR) {
            val error = UCrop.getError(resultState.data!!)
            Log.e("uCrop", "Crop error", error)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { selectedImageUri ->
                val destinationUri = Uri.fromFile(
                    File(context.cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                )

                val uCrop = UCrop.of(selectedImageUri, destinationUri)
                    .withAspectRatio(1f, 1f) // you can remove or customize
                    .withOptions(UCrop.Options().apply {
                        setFreeStyleCropEnabled(true)
                        setCompressionFormat(Bitmap.CompressFormat.JPEG)
                        setCompressionQuality(90)
                    })

                cropLauncher.launch(uCrop.getIntent(context))
            }
        }
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted.value = isGranted
//        showPermissionDeniedUI.value = !isGranted
        if (!isGranted) {
            val showRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
            showPermissionDeniedUI.value = !showRationale // i.e., permanently denied if no rationale
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            permissionGranted.value = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold {innerPadding ->
        if (showPermissionDeniedUI.value) {
            // Show permission denied UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Camera permission is permanently denied.",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    ElevatedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = "package:${context.packageName}".toUri()
                            }
                            context.startActivity(intent)
                        }
                    ) {
                        Text("Open App Settings")
                    }
                }

            }
            return@Scaffold
        }else if(permissionGranted.value) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val androidViewSection = @UiComposable @Composable {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                //                               isGestureZoom = true
                                val newZoom = (zoomState * zoom).coerceIn(0.5f, 9f)
                                zoomState = newZoom
                                cameraControl?.setZoomRatio(newZoom)
                            }
                        },
                    factory = { androidViewContext ->
                        val previewView = PreviewView(androidViewContext)

                        val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
                            ProcessCameraProvider.getInstance(androidViewContext)

                        cameraProviderFuture.addListener({
                            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )

                                cameraControl = camera!!.cameraControl
                                cameraControl?.setZoomRatio(1.2f)

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (result != null) {
                                        imageProxy.close() // Important: always close the proxy
                                        return@setAnalyzer
                                    }
                                    try {
                                       coroutineScope.launch {
                                           val scannedResult = scanQRCode(imageProxy)
                                           if (scannedResult != null) {
                                               result = scannedResult
                                               viewModel.addScanned(result!!)
                                               Log.d("QRScan", "QR Code detected: $result")

                                               scope.launch {
                                                   sheetState.show()
                                               }

                                               if (zoomState < 2f) {
                                                   zoomState += 0.1f
                                                   cameraControl?.setZoomRatio(zoomState)
                                               }
                                           }
                                       }
                                    } catch (e: Exception) {
                                        Log.e("QRScan", "QR Code scanning error", e)
                                    }
                                }

                            } catch (exc: Exception) {
                                Log.e("QRScan", "Use case binding failed", exc)
                            }
                        }, ContextCompat.getMainExecutor(androidViewContext))
                        previewView
                    }
                )
                }
                androidViewSection()
                Image(
                    painter = painterResource(id = R.drawable.neo_frame),
                    contentDescription = "Scanner Frame",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .aspectRatio(1f)
                        .fillMaxSize(0.9f), // adjust as needed
                    contentScale = ContentScale.FillBounds
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(color = Color.Black.copy(alpha = 0.4f))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(onClick = {
                        isTorchOn = !isTorchOn
                        cameraControl?.enableTorch(isTorchOn)
                    }) {
                        Icon(
                            imageVector = if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                            contentDescription = "Toggle Flash",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    ElevatedButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*") // Open image picker
                        },
                        colors = ButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.LightGray,
                            disabledContentColor = Color.DarkGray,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Image,
                            contentDescription = "Select Image",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Upload QR Code")
                    }
                }
            }
        }else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Requesting camera permission…")
            }
        }
    }
}

private fun copyToClipboard(text: String, context: Context) {
    val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("QR Result", text)
    clipboard.setPrimaryClip(clip)
}

private fun openUrl(url: String, context: Context) {
    val cleaned = url.trim()
    try {
        when {
            Patterns.WEB_URL.matcher(cleaned).matches() || cleaned.contains(".") -> {
                val fixedUrl =
//                        cleaned.startsWith("http://") ||
                    if (cleaned.startsWith("https://")) {
                        cleaned
                    } else {
                        "https://$cleaned"
                    }
                val intent = Intent(Intent.ACTION_VIEW, fixedUrl.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }

            Patterns.PHONE.matcher(cleaned).matches() -> {
                val intent = Intent(Intent.ACTION_DIAL, "tel:$cleaned".toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            Patterns.EMAIL_ADDRESS.matcher(cleaned).matches() -> {
                val intent = Intent(Intent.ACTION_SENDTO, "mailto:$cleaned".toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            cleaned.startsWith("geo:") -> {
                val intent = Intent(Intent.ACTION_VIEW, cleaned.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            cleaned.startsWith("mailto:") -> {
                val intent = Intent(Intent.ACTION_SENDTO, cleaned.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            cleaned.startsWith("tel:") -> {
                val intent = Intent(Intent.ACTION_DIAL, cleaned.toUri())
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            else -> {
                Log.e("QRNova", "Unsupported URL format: $cleaned")
            }
        }
    } catch (_: ActivityNotFoundException) {
        Log.e(
            "QRNova",
            "No application can handle this request. Please install a web browser or check your URL."
        )
    }
}

@Composable
private fun ResultContent(result: String, viewModel: QrHistoryViewModel,context: Context){
    Column {
        ElevatedCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Scan Result:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {
                    if (Patterns.WEB_URL.matcher(result).matches()) {
                        Icon(
                            Icons.Default.Link, contentDescription = "link"
                        )
                    } else {
                        Icon(
                            Icons.Default.TextFields, contentDescription = "Text"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = result,
                        fontSize = 16.sp,
                    )
                }

            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        ElevatedCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tool Box:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row {
                    Spacer(modifier = Modifier.weight(0.5f))
                    IconButton(onClick = { copyToClipboard(
                        result,
                        context = context
                    ) }) {
                        Icon(Icons.Default.CopyAll, contentDescription = "copy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { viewModel.shareScannedQrCodes(context, mutableStateSetOf(
                        result
                    )) }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { openUrl(result, context) }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Continue")
                    }
                }
            }
        }
    }
}
fun decodeQRCodeFromImage(context: Context, imageUri: Uri): String? {
    return try {
//        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, imageUri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
//        } else {
//            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
//        }

        val argbBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val intArray = IntArray(argbBitmap.width * argbBitmap.height)
        argbBitmap.getPixels(intArray, 0, argbBitmap.width, 0, 0, argbBitmap.width, argbBitmap.height)

        val luminanceSource = RGBLuminanceSource(argbBitmap.width, argbBitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))
        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.TRY_HARDER to true,
                    DecodeHintType.PURE_BARCODE to false
                )
            )
        }
        reader.decode(binaryBitmap).text
    }catch (e: NotFoundException) {
        Log.w("QRScan", "No QR code found in image", e)
        null
    }catch (e: Exception) {
        Log.e("QRScan", "Error decoding QR from image", e)
        null
    }
}


@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun scanQRCode(imageProxy: ImageProxy): String? = suspendCancellableCoroutine { cont ->
    val mediaImage = imageProxy.image
    val rotation = imageProxy.imageInfo.rotationDegrees

    if (mediaImage != null) {
        val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()

        val scanner = BarcodeScanning.getClient(options)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val result = barcodes.firstOrNull()?.rawValue
                cont.resume(result, onCancellation = null)
            }
            .addOnFailureListener {
                cont.resume(null, onCancellation = null)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        Log.e("QRScan", "ImageProxy has no media image")
        imageProxy.close()
        cont.resume(null, onCancellation = null)
    }
}


