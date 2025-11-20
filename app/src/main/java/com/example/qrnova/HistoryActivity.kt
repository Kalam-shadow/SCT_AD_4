package com.example.qrnova

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import com.example.qrnova.ui.theme.QrnovaTheme
import kotlinx.coroutines.launch
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
                            },
                            actions = {
                                IconButton(onClick = {
                                    finish()
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close")
                                }
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
                        if (scannedResult != null) {
                            if (isPlainText(scannedResult)) {
                                PlainTextResultView(
                                    result = scannedResult,
                                    onClose = { finish() },
                                    onShare = { viewModel.shareScannedQrCodes(this@HistoryActivity, mutableStateSetOf(scannedResult)) },
                                    onCopy = { copyToClipboard(scannedResult) }
                                )
                            }else{
                                openUrl(scannedResult)
                                finish()
                            }
                        }else{
                            HistoryScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("QR Result", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun openUrl(url: String) {
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
                    startActivity(intent)
                }

                Patterns.PHONE.matcher(cleaned).matches() -> {
                    val intent = Intent(Intent.ACTION_DIAL, "tel:$cleaned".toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                Patterns.EMAIL_ADDRESS.matcher(cleaned).matches() -> {
                    val intent = Intent(Intent.ACTION_SENDTO, "mailto:$cleaned".toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                cleaned.startsWith("geo:") -> {
                    val intent = Intent(Intent.ACTION_VIEW, cleaned.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                cleaned.startsWith("mailto:") -> {
                    val intent = Intent(Intent.ACTION_SENDTO, cleaned.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                cleaned.startsWith("tel:") -> {
                    val intent = Intent(Intent.ACTION_DIAL, cleaned.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
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

    private fun isPlainText(text: String): Boolean {
        return !Patterns.WEB_URL.matcher(text).matches() &&
                !Patterns.PHONE.matcher(text).matches() &&
                !Patterns.EMAIL_ADDRESS.matcher(text).matches() &&
                !text.startsWith("geo:") &&
                !text.startsWith("mailto:") &&
                !text.startsWith("tel:") &&
                !text.startsWith("http:") &&
                !text.startsWith("https:")
    }
}

@Composable
fun PlainTextResultView(
    result: String,
    onClose: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit
) { Column {
        ElevatedCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
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
        Spacer(modifier = Modifier.weight(1f))
        ElevatedCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
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
                    IconButton(onClick = { onClose() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { onCopy() }) {
                        Icon(Icons.Default.CopyAll, contentDescription = "copy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = { onShare() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            }
        }
    }
}


@Composable
fun HistoryScreen(viewModel: QrHistoryViewModel) {
    val tabs = listOf("Scanned", "Created")

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = {tabs.size}
    )
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) { page ->
            when (page) {
                0 -> ScannedHistoryScreen(viewModel)
                1 -> CreatedHistoryScreen(viewModel)
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannedHistoryScreen(viewModel: QrHistoryViewModel) {
    val history by viewModel.scannedHistory.collectAsState()
    val selectedItems = remember { mutableStateSetOf<String>() }
    val inSelectionMode by remember { derivedStateOf { selectedItems.isNotEmpty() } }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            if (inSelectionMode) {
                MediumTopAppBar(
                    title = { Text("${selectedItems.size} selected") },
                    actions = {
                        IconButton(onClick = {
                            // Handle select all logic
                            if (selectedItems.size == history.size) {
                                selectedItems.clear()
                            } else {
                                toggleAllItems(selectedItems, history.map { it.content })
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = {
                            // Handle share logic
                            viewModel.shareScannedQrCodes(context, selectedItems)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }

                        IconButton(onClick = {
                            // Handle delete logic
                            viewModel.deleteScannedQr(selectedItems)
                            selectedItems.clear()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedItems.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ManageHistory,
                            contentDescription = "Empty History",
                            modifier = Modifier.size(80.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Scanned QR Codes Yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { item ->
                        val isSelected = selectedItems.contains(item.content)

                        ElevatedCard(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectionMode) {
                                            toggleItem(selectedItems, item.content)
                                        }
                                    },
                                    onLongClick = {
                                        toggleItem(selectedItems, item.content)
                                    }
                                ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()

                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if(Patterns.WEB_URL.matcher(item.content).matches()){
                                    Icon(
                                        Icons.Default.Link, contentDescription = "link"
                                    )
                                }else{
                                    Icon(
                                        Icons.Default.TextFields, contentDescription = "Text"
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.content,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MutableCollectionMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatedHistoryScreen(viewModel: QrHistoryViewModel) {
    val history by viewModel.createdHistory.collectAsState()
    val selectedItems = remember { mutableStateSetOf<String>() }
    val inSelectionMode by remember { derivedStateOf { selectedItems.isNotEmpty() } }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            if (inSelectionMode) {
                MediumTopAppBar(
                    title = { Text("${selectedItems.size} selected") },
                    actions = {
                        IconButton(onClick = {
                            // Handle select all logic
                            if (selectedItems.size == history.size) {
                                selectedItems.clear()
                            } else {
                                toggleAllItems(selectedItems, history.map { it.imageUri })
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = {
                            // Handle share logic
                            viewModel.shareQrCodes(context,selectedItems)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }

                        IconButton(onClick = {
                            // Handle delete logic
                            viewModel.deleteQrCodes(selectedItems)
                            selectedItems.clear()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectedItems.clear() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No created QR codes yet.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history) { item ->
                        val isSelected = selectedItems.contains(item.imageUri)

                        ElevatedCard(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .combinedClickable(
                                    onClick = {
                                        if (inSelectionMode) {
                                            toggleItem(selectedItems, item.imageUri)
                                        }
                                    },
                                    onLongClick = {
                                        toggleItem(selectedItems, item.imageUri)
                                    }
                                ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.surfaceContainer
                                    )
                                    .padding(12.dp)
                                ,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(item.imageUri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(end = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(text = "QR: ${item.content}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(text = "Saved at: ${Date(item.timestamp)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun toggleAllItems(set: MutableSet<String>, items: List<String>) {
    set.clear()
    set.addAll(items)
}

private fun toggleItem(set: MutableSet<String>, item: String) {
    if (!set.add(item)) set.remove(item)
}

