package com.example.qrnova

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrnova.ui.theme.QrnovaTheme

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scannedResult = intent?.getStringExtra("scanResult")

        setContent {
            QrnovaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (scannedResult != null && isPlainText(scannedResult)) {
                        PlainTextResultView(
                            result = scannedResult,
                            onClose = { finish() }
                        )
                    }
                    HistoryScreen(onClose = { finish() })

                }
            }
        }
    }

    private fun isPlainText(text: String): Boolean {
        return !Patterns.WEB_URL.matcher(text).matches() &&
                !Patterns.PHONE.matcher(text).matches() &&
                !Patterns.EMAIL_ADDRESS.matcher(text).matches() &&
                !text.startsWith("geo:") &&
                !text.startsWith("mailto:") &&
                !text.startsWith("tel:") &&
                !text.startsWith("http")
    }
}

@Composable
fun PlainTextResultView(result: String, onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Scanned Text",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = result,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}
@Composable
fun HistoryScreen(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Scan History",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Replace this with your own list logic later
        repeat(5) { index ->
            Text(
                text = "QR #${index + 1} – Dummy content",
                fontSize = 16.sp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}
