package com.example.qrnova

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var scanResult by remember { mutableStateOf<String?>(null) }
    var cameraControl by remember { mutableStateOf<androidx.camera.core.CameraControl?>(null) }
    var zoomState by remember { mutableFloatStateOf(1f) }

    // Request Camera Permission
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                (context as ComponentActivity),
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        ElevatedCard (modifier = Modifier.fillMaxWidth()
            .padding(24.dp)
            .height(320.dp)
        ) {
            AndroidView(
                modifier = Modifier.weight(1f)
                    .pointerInput(Unit){
                        detectTransformGestures{_,_,zoom,_ ->
                            val newZoom  = (zoomState + zoom).coerceIn(1f, 5f)
                            zoomState = newZoom
                            cameraControl?.setZoomRatio(newZoom)
                        }
                    },
                factory = { androidViewContext ->
                    val previewView = androidx.camera.view.PreviewView(androidViewContext)

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
                            .also { analyzer ->
                                analyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val result = scanQRCode(imageProxy)
                                    result?.let {
                                        scanResult = it
                                        Log.d("QRScan", "QR Code detected: $it")
                                        cameraControl?.setZoomRatio(2.5f)
                                    }
                                    imageProxy.close()
                                }
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            val camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                            cameraControl = camera.cameraControl
                        } catch (exc: Exception) {
                            Log.e("QRScan", "Use case binding failed", exc)
                        }
                    }, ContextCompat.getMainExecutor(androidViewContext))

                    previewView
                }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        scanResult?.let { result ->
            val isLink = result.startsWith("http://") || result.startsWith("https://") || result.startsWith("bit.ly")
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
            Text(
                text = "Scanned: $result",
                textDecoration = TextDecoration.Underline,
                style = MaterialTheme.typography.bodyMedium,
                modifier = if (isLink) {
                        Modifier.clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result))
                            ContextCompat.startActivity(context, intent, null)
                        }
                            .align(Alignment.CenterHorizontally)
                            .padding(8.dp)
                    } else {
                        Modifier.align(Alignment.CenterHorizontally)
                            .padding(8.dp)
                    }
                )
            }
        }


        Spacer(modifier = Modifier.weight(1f))
        // Show scan result
        scanResult?.let { result ->
            Snackbar(
                action = {
                    TextButton(onClick = { scanResult = null }) {
                        Text("OK")
                    }
                }
            ) {
                Text("Scanned: $result")
            }
        }
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
        val result = MultiFormatReader().decode(binaryBitmap)
        result.text
    } catch (e: Exception) {
        null
    }
}
