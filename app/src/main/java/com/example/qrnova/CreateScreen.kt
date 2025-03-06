package com.example.qrnova

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateScreen() {
    var inputText by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
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
        ElevatedCard(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Enter Text to Generate QR Code") },
                    modifier = Modifier
                      //  .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                        .fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth()
                        .padding(4.dp),
                    onClick = {
                        if (inputText.isNotEmpty()) {
                            qrBitmap = generateQRCode(inputText)
                        } else {
                            Toast.makeText(context, "Enter text first", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Generate QR Code")
                }
            }
        }



        Spacer(modifier = Modifier.height(8.dp))

        ElevatedCard(
            modifier = Modifier.padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center

            ) {
                if (qrBitmap == null){
                    Text("No QR Code Generated")
                    Spacer(modifier = Modifier.height(2.dp))
                }
                qrBitmap?.let { bitmap ->
                    Text("QR Code Generated:")
                    Spacer(modifier = Modifier.height(2.dp))
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Generated QR Code",
                        modifier = Modifier.size(200.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row {

                        Button(
                            onClick = {
                                saveQRCodeToStorage(bitmap, context)
                            }
                        ) {
                            Text("Save QR Code")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = {
                                qrBitmap?.let { bitmap -> shareQRCode(bitmap, context) }
                            }
                        ){
                            Text("Share QR Code")
                        }
                    }
                }
            }
        }

    }
}

// Function to Generate QR Code Bitmap
private fun generateQRCode(text: String): Bitmap {
    val size = 512
    val bitMatrix: BitMatrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)

    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
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
