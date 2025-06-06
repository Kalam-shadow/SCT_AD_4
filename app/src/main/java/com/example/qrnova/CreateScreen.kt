package com.example.qrnova

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
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

data class QrState(val inputText: String = "", val qrBitmap: Bitmap? = null)

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen(viewModel: QrViewModel) {
    val qrState = viewModel.qrState
    val context = LocalContext.current

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

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isLandscape = maxWidth > maxHeight

            val inputSection = @Composable {
                QrInputField(
                    inputText = qrState.inputText,
                    onTextChange = { viewModel.updateText(it) },
                    onGenerate = {
                        if (qrState.inputText.isNotBlank()) {
                            viewModel.generateQr()
                        } else {
                            Toast.makeText(context, "Enter text first", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            val displaySection = @Composable {
                QrDisplayField(context, qrState.qrBitmap)
            }

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        inputSection()
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        displaySection()
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    inputSection()
                    displaySection()
                }
            }
        }
    }
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
        modifier = Modifier.padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { onTextChange(it)},
                label = { Text("Enter Text to Generate QR Code") },
                modifier = Modifier
                    //  .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp)
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                onClick = {
                    onGenerate()
                }
            ) {
                Text("Generate QR Code")
            }
        }
    }
}

@Composable
fun QrDisplayField(context : Context,qrBitmap : Bitmap?) {
    ElevatedCard(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center

        ) {
            if (qrBitmap == null) {
                Text("No QR Code Generated")
                Spacer(modifier = Modifier.height(2.dp))
            }
            else {
                Text("QR Code Generated:")
                Spacer(modifier = Modifier.height(2.dp))
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Generated QR Code",
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row {

                    Button(
                        onClick = {
                            saveQRCodeToStorage(qrBitmap, context)
                        }
                    ) {
                        Text("Save QR Code")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            shareQRCode(qrBitmap, context)
                        }
                    ) {
                        Text("Share QR Code")
                    }
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

// Function to Save QR Code to Storage
private fun saveQRCodeToStorage(bitmap: Bitmap, context: Context) {
    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "QRNova")
    if (!directory.exists()) directory.mkdirs()

    val file = File(directory, "QRCode_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        out.flush()
    }

    Toast.makeText(context, "QR Code saved at ${file.absolutePath}", Toast.LENGTH_SHORT).show()
}

class QrViewModel : ViewModel() {
    var qrState by mutableStateOf(QrState())
        private set

    fun updateText(newText: String) {
        qrState = qrState.copy(inputText = newText, qrBitmap = null)
    }

    fun generateQr() {
        if (qrState.inputText.isNotBlank()) {
            qrState = qrState.copy(qrBitmap = generateQRCode(qrState.inputText))
        }
    }
}
