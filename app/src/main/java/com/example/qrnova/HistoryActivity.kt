package com.example.qrnova

import android.annotation.SuppressLint
import android.os.Bundle
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.qrnova.ui.theme.QrnovaTheme
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
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
                                    // Handle close action
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
                        if (scannedResult != null && isPlainText(scannedResult)) {
                            PlainTextResultView(
                                result = scannedResult
                            )
                        }
                        HistoryScreen(viewModel = viewModel)
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
fun PlainTextResultView(result: String) {
    ElevatedCard {
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
        }
    }
}


@Composable
fun HistoryScreen(viewModel: QrHistoryViewModel) {
    val pagerState = rememberPagerState(initialPage = 0)
    val coroutineScope = rememberCoroutineScope()

    val tabs = listOf("Scanned", "Created")

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
            count = tabs.size,
            state = pagerState,
            modifier = Modifier.fillMaxSize()
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.background)
        ) { page ->
            when (page) {
                0 -> ScannedHistoryScreen(viewModel)
                1 -> CreatedHistoryScreen(viewModel)
            }
        }
    }
}

@Composable
fun ScannedHistoryScreen(viewModel: QrHistoryViewModel) {
    val history by viewModel.scannedHistory.collectAsState()
    ElevatedCard {
        if (history.isEmpty()) {
            Text("No scanned QR codes yet.")
        } else {
            LazyColumn {
                items(history) { item ->
                    Text(text = "QR: ${item.content}")
                    Text("Time: ${Date(item.timestamp)}")
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
                                modifier = Modifier.fillMaxWidth()
                                    .padding(8.dp)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.surface
                                    ),
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

