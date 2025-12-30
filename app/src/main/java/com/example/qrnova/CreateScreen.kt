package com.example.qrnova

 import android.annotation.SuppressLint
 import android.content.ContentValues
 import android.content.Context
 import android.content.Intent
 import android.content.res.Configuration
 import android.graphics.Bitmap
 import android.graphics.Color
 import android.net.Uri
 import android.os.Environment
 import android.provider.MediaStore
 import android.util.Log
 import android.widget.Toast
 import androidx.compose.foundation.Image
 import androidx.compose.foundation.background
 import androidx.compose.foundation.layout.Arrangement
 import androidx.compose.foundation.layout.Box
 import androidx.compose.foundation.layout.Column
 import androidx.compose.foundation.layout.Row
 import androidx.compose.foundation.layout.Spacer
 import androidx.compose.foundation.layout.fillMaxSize
 import androidx.compose.foundation.layout.fillMaxWidth
 import androidx.compose.foundation.layout.height
 import androidx.compose.foundation.layout.padding
 import androidx.compose.foundation.layout.size
 import androidx.compose.foundation.layout.width
 import androidx.compose.foundation.shape.RoundedCornerShape
 import androidx.compose.material.icons.Icons
 import androidx.compose.material.icons.filled.Download
 import androidx.compose.material.icons.filled.QrCode2
 import androidx.compose.material.icons.filled.Share
 import androidx.compose.material3.Button
 import androidx.compose.material3.CardDefaults
 import androidx.compose.material3.ElevatedCard
 import androidx.compose.material3.ExperimentalMaterial3Api
 import androidx.compose.material3.Icon
 import androidx.compose.material3.MaterialTheme
 import androidx.compose.material3.OutlinedButton
 import androidx.compose.material3.OutlinedTextField
 import androidx.compose.material3.Text
 import androidx.compose.material3.TopAppBar
 import androidx.compose.runtime.Composable
 import androidx.compose.runtime.LaunchedEffect
 import androidx.compose.runtime.getValue
 import androidx.compose.runtime.mutableStateOf
 import androidx.compose.runtime.remember
 import androidx.compose.runtime.setValue
 import androidx.compose.ui.Alignment
 import androidx.compose.ui.Modifier
 import androidx.compose.ui.graphics.asImageBitmap
 import androidx.compose.ui.platform.LocalConfiguration
 import androidx.compose.ui.platform.LocalContext
 import androidx.compose.ui.text.font.FontWeight
 import androidx.compose.ui.unit.dp
 import androidx.core.content.FileProvider
 import androidx.core.graphics.createBitmap
 import androidx.core.graphics.set
 import androidx.lifecycle.ViewModel
 import com.google.zxing.BarcodeFormat
 import com.google.zxing.MultiFormatWriter
 import com.google.zxing.common.BitMatrix
 import java.io.File
 import java.io.FileOutputStream
 import java.io.IOException

data class QrState(val inputText: String = "", val qrBitmap: Bitmap? = null, val isSaved: Boolean = false)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(viewModel: QrViewModel, historyViewModel: QrHistoryViewModel) {
    val qrState = viewModel.qrState
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val config = LocalConfiguration.current
    val isLandscape = config.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(qrState.qrBitmap) {
        if(qrState.qrBitmap != null && !viewModel.isHandled()) {
            qrState.qrBitmap.let { bitmap ->
                imageUri = storeQRCode(bitmap, context)
                Log.d("CreateScreen", "QR Code saved with URI: $imageUri")
            }
            historyViewModel.addCreated(qrState.inputText, imageUri.toString())
            viewModel.markHandled()
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "QR Nova",
                    fontWeight = FontWeight.Bold
                )
            }
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
//            val isLandscape = maxWidth > maxHeight

            if (isLandscape) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        QrInputSection(viewModel = viewModel, context = context)
                        QrUtilSection(qrBitmap = viewModel.qrState.qrBitmap)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        QrDisplaySection(qrBitmap = viewModel.qrState.qrBitmap)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    QrInputSection(viewModel,context)
                    QrDisplaySection(qrBitmap = viewModel.qrState.qrBitmap)
                    QrUtilSection(qrBitmap = viewModel.qrState.qrBitmap)
                }
            }
        }
    }
}

@Composable
private fun QrInputSection(viewModel: QrViewModel,context: Context) {
    QrInputField(
        inputText = viewModel.qrState.inputText,
        onTextChange = { viewModel.updateText(it) },
        onGenerate = {
            if (viewModel.qrState.inputText.isNotBlank()) {
                viewModel.generateQr()
            } else {
                Toast.makeText(context, "Enter text first", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

@Composable
private fun QrDisplaySection(qrBitmap: Bitmap?) {
    QrDisplayField(qrBitmap)
}

@Composable
private fun QrUtilSection(qrBitmap: Bitmap?) {
    val context = LocalContext.current
    QrUtilField(context, qrBitmap)
}


// Function to Generate QR Code Bitmap
private fun generateQRCode(text: String): Bitmap {
    val size = 512
    val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = createBitmap(size, size, Bitmap.Config.RGB_565)

    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap[x, y] = if (bitMatrix[x, y]) Color.BLACK else Color.WHITE
        }
    }
    return bitmap
}

@Composable
fun QrInputField(
    inputText: String,
    onTextChange: (String) -> Unit,
    onGenerate: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onTextChange,
                label = { Text("Enter text to generate QR") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onGenerate
            ) {
                Icon(Icons.Default.QrCode2, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Generate")
            }
        }
    }
}

@Composable
fun QrDisplayField(qrBitmap : Bitmap?) {
    ElevatedCard(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
//                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {
            if (qrBitmap == null) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "No QR",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(90.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "No QR Code Generated",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Generate a QR to preview it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            else {
                Text(
                    text = "Generated QR Code",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(16.dp))

//                Surface(
//                    tonalElevation = 4.dp,
//                    shape = RoundedCornerShape(16.dp),
//                    modifier = Modifier.size(220.dp)
//                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier.padding(12.dp)
                    )
//                }
            }
        }
    }
}

@Composable
fun QrUtilField(context: Context, qrBitmap: Bitmap?) {
    if (qrBitmap != null) {
        ElevatedCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
//                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        saveQRCodeToStorage(qrBitmap, context)
                    }
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save")
                }
//                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        shareQRCode(qrBitmap, context)
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Share")
                }
            }
        }
    }
}
private fun shareQRCode(bitmap: Bitmap, context: Context) {
    val directory = File(context.cacheDir, "shared_qr")
    if (!directory.exists()) directory.mkdirs()

    val file = File(directory, "QRCode_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
    }

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, uri)
        type = "image/png"
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share QR Code"))
}

// personal Storage
private fun storeQRCode(bitmap: Bitmap, context: Context): Uri? {
    val directory = File(context.filesDir, "QRNova")
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val nomediaFile = File(directory, ".nomedia")
    if (!nomediaFile.exists()) {
        nomediaFile.createNewFile()
    }
    val file = File(directory, "QRCode_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    Log.d("FileProviderDebug", "Saving file to: ${file.absolutePath}")
    return uri // Return the URI for further use, e.g., sharing
}
// Function to Save QR Code to Storage
//private fun saveQRCodeToStorage(bitmap: Bitmap, context: Context) {
//    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QRNova")
//    if (!directory.exists()) directory.mkdirs()
//
//    val file = File(directory, "QRCode_${System.currentTimeMillis()}.png")
//    FileOutputStream(file).use { out ->
//        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
//        out.flush()
//    }
//
//    // Trigger Media Scanner to make image visible in Gallery under "QRNova_Album"
//    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/jpeg")) { path, uri ->
//        println("Image saved and scanned: $path -> $uri")
//    }
//    Toast.makeText(context, "QR Code saved at ${file.absolutePath}", Toast.LENGTH_SHORT).show()
//}


// ... other imports
private fun saveQRCodeToStorage(bitmap: Bitmap, context: Context) {
    val collection =
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "QRCode_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.IS_PENDING, 1)
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QRNova")
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(collection, contentValues)

    uri?.let {
        try {
            resolver.openOutputStream(it).use { out ->
                if (out == null) throw IOException("Failed to open output stream.")
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(it, contentValues, null, null)
            Toast.makeText(context, "QR Code saved to Gallery", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Clean up the entry if something went wrong
            resolver.delete(uri, null, null)
            Toast.makeText(context, "Failed to save QR Code", Toast.LENGTH_SHORT).show()
            Log.e("SaveQRCode", "Error saving QR Code", e)
        }
    }
}
class QrViewModel : ViewModel() {
    var qrState by mutableStateOf(QrState())
        private set

    private var hasBeenHandled = false // <-- independent flag

    fun updateText(newText: String) {
        qrState = qrState.copy(inputText = newText, qrBitmap = null)
        hasBeenHandled = false // Reset handling when text changes
    }

    fun generateQr() {
        if (qrState.inputText.isNotBlank()) {
            qrState = qrState.copy(qrBitmap = generateQRCode(qrState.inputText))
            hasBeenHandled = false // Reset for this new bitmap
        }
    }

    fun markHandled() {
        hasBeenHandled = true
    }

    fun isHandled(): Boolean = hasBeenHandled

//    fun clearQr() {
//        qrState = QrState()
//        hasBeenHandled = false
//    }
}


