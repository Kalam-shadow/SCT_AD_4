package com.example.qrnova

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var zoomState by remember { mutableFloatStateOf(1f) }
    var isGestureZoom by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarState by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }
    var camera: Camera? by remember { mutableStateOf(null) }
    val coroutineScope = rememberCoroutineScope()

    fun resultAndSnack(it: String){
        coroutineScope.launch {
            snackbarState = true
            snackbarHostState.currentSnackbarData?.dismiss()

            val snackbarResult = snackbarHostState.showSnackbar(
                message = "Scanned: $it",
                actionLabel = "Ok",
                duration = SnackbarDuration.Short, // Use Short duration so it disappears quickly
                withDismissAction = true
            )

            snackbarState = false // Reset state to allow another scan

            if (snackbarResult == SnackbarResult.ActionPerformed) {
                if (Patterns.WEB_URL.matcher(it).matches()) {
                    val intent = Intent(Intent.ACTION_VIEW,
                        it.toUri())
                    context.startActivity(intent)
                }
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { selectedImageUri ->
                val qrText = decodeQRCodeFromImage(context, selectedImageUri)
                if (qrText != null) {
                    scanResult = qrText
                    Log.d("QRScan", "Scanned from image: $qrText")
                    resultAndSnack(qrText)
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("No QR code found in the image.", duration = SnackbarDuration.Short)
                    }
                }
            }
        }
    )

    // Handle Camera Permission
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }
    }

    Scaffold (
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            isGestureZoom = true
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
                                try {
                                    val result = scanQRCode(imageProxy)
                                    result?.let {
                                        if (!snackbarState) {
                                            scanResult = it
                                            Log.d("QRScan", "QR Code detected: $it")
                                            resultAndSnack(it)


                                            if (zoomState < 2f) {
                                                zoomState += 0.1f
                                                cameraControl?.setZoomRatio(zoomState)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("QRScan", "QR Code scanning error", e)
                                } finally {
                                    imageProxy.close()
                                }
                            }

                        } catch (exc: Exception) {
                            Log.e("QRScan", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(androidViewContext))

                    previewView
                }
            )
            // Bottom Action Bar (Torch)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(color = Color.Black.copy(alpha = 0.4f),shape = CircleShape)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
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
                    ElevatedButton(onClick = {
                        imagePickerLauncher.launch("image/*") // Open image picker
                    }) {
                        Text("Upload QR Code")
                    }
                }
            }
        }
    }
}
fun decodeQRCodeFromImage(context: Context, imageUri: Uri): String? {
    try {
        // Use ImageDecoder for decoding the image
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, imageUri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE) // Force a non-hardware bitmap
            }
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        }


        // Convert to ZXing-compatible LuminanceSource
        val intArray = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(intArray, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

        val luminanceSource = RGBLuminanceSource(bitmap.width, bitmap.height, intArray)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(luminanceSource))

        return MultiFormatReader().decode(binaryBitmap).text
    } catch (e: Exception) {
        Log.e("QRScan", "Error decoding QR from image", e)
        return null
    }
}


// Function to decode QR code using ZXing
private fun scanQRCode(imageProxy: ImageProxy): String? {
    val buffer = imageProxy.planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    val width = imageProxy.width
    val height = imageProxy.height
    val source = PlanarYUVLuminanceSource(bytes, width, height, 0, 0, width, height, false)
    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

    return try {
        MultiFormatReader().decode(binaryBitmap).text
    } catch (_: NotFoundException) {
        null
    }
}


