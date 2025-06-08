package com.example.qrnova

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.qrnova.ui.theme.QrnovaTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.font.FontWeight
import coil.compose.rememberAsyncImagePainter
import java.util.Date


class HistoryActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scannedResult = intent?.getStringExtra("scanResult")
        val viewModel = QrHistoryViewModel(application)


        setContent {
            QrnovaTheme {
                Scaffold (
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "QR Nova",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    },
                    modifier = Modifier.fillMaxSize(),
                ) {innerPadding ->
                    Column (
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding) ,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (scannedResult != null && isPlainText(scannedResult)) {
                            PlainTextResultView(
                                result = scannedResult,
                                onClose = { finish() }
                            )
                        }
                        HistoryScreen(viewModel = viewModel, onClose = { finish() })
                    }
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
fun HistoryScreen(viewModel: QrHistoryViewModel, onClose: () -> Unit) {
    var showScanned by remember { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
    ) {
        Text(
            text = if (showScanned) "Scanned QR History" else "Created QR History",
            style = MaterialTheme.typography.titleSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { showScanned = !showScanned },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (showScanned) "Switch to Created History" else "Switch to Scanned History")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showScanned) {
            ScannedHistoryScreen(viewModel)
        } else {
            CreatedHistoryScreen(viewModel)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close")
        }
    }
}


@Composable
fun ScannedHistoryScreen(viewModel: QrHistoryViewModel) {
    val history by viewModel.scannedHistory.collectAsState()
    LazyColumn {
        items(history) { item ->
            Text(text = "QR: ${item.content}")
            Text("Time: ${Date(item.timestamp)}")
        }
    }
}

@Composable
fun CreatedHistoryScreen(viewModel: QrHistoryViewModel) {
    val history by viewModel.createdHistory.collectAsState()
    LazyColumn {
        items(history) { item ->
            Text("QR: ${item.content}")
            Image(painter = rememberAsyncImagePainter(item.imageUri), contentDescription = null)
            Text("Saved at: ${Date(item.timestamp)}")
        }
    }
}
