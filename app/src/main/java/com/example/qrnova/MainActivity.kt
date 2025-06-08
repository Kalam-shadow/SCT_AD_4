package com.example.qrnova

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ManageHistory
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*

import com.example.qrnova.ui.theme.QrnovaTheme

class MainActivity : ComponentActivity() {

    private var initialSharedImageUri: Uri? = null
    private val viewModel: QrViewModel by viewModels()
    private val historyViewModel: QrHistoryViewModel by viewModels()


    @SuppressLint("UnusedBoxWithConstraintsScope")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Capture shared image on launch
        initialSharedImageUri = getSharedImageUri(intent)

        setContent {
            QrnovaTheme {
                val navController = rememberNavController()
                val scanResult = remember { mutableStateOf("") }
                val isPortrait = resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                val historyViewModel = QrHistoryViewModel(application)


                // Decode image only once on start
                LaunchedEffect(Unit) {
                    initialSharedImageUri?.let { uri ->
                        val qrText = decodeQRCodeFromImage(this@MainActivity, uri) ?: ""
                        Log.d("QRnova", "Decoded on launch: $qrText")
                        if (qrText.isNotEmpty()) scanResult.value = qrText
                    }
                }

                val topLevelRoutes = listOf(
                    TopLevelRoute("QR Scan", "Scanner", Icons.Default.Search),
                    TopLevelRoute("QR Create", "Creator", Icons.Default.Build)
                )

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Main content layout

                if (isPortrait) {
                    // Portrait mode — content + bottom bar
                    Column(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            if (currentDestination?.route == NavRoute.Scanner.route) {
                                OverlayTopBar(modifier = Modifier.zIndex(1f))
                            }

                            NavHost(
                                navController = navController,
                                startDestination = NavRoute.Scanner.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(NavRoute.Scanner.route) {
                                    QRscanScreen(historyViewModel)
                                }
                                composable(NavRoute.Creator.route) {
                                    QRcreateScreen()
                                }
                            }
                        }
                        BottomNavigationBar(navController, currentDestination, topLevelRoutes)
                    }
                } else {
                    // Landscape mode — content + side nav
                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (currentDestination?.route == NavRoute.Scanner.route) {
                                OverlayTopBar(modifier = Modifier.zIndex(1f))
                            }

                            NavHost(
                                navController = navController,
                                startDestination = NavRoute.Scanner.route,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                composable(NavRoute.Scanner.route) {
                                    QRscanScreen(historyViewModel)
                                }
                                composable(NavRoute.Creator.route) {
                                    QRcreateScreen()
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxHeight()) {
                            // Side nav in landscape
                            Column(
                                modifier = Modifier
//                                .width(80.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(vertical = 16.dp),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                topLevelRoutes.forEach { route ->
                                    val isSelected = currentDestination?.route == route.route
                                    IconButton(
                                        onClick = {
                                            navController.navigate(route.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .weight(1f)
                                    ) {

                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.12f
                                                    )
                                                    else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .padding(8.dp)
                                        ) {
                                            // Icon + Label
                                            Icon(
                                                imageVector = route.icon,
                                                contentDescription = route.name,
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            SmallFloatingActionButton(
                                onClick = {
                                    val historyIntent = Intent(this@MainActivity, HistoryActivity::class.java).apply {
                                        putExtra("scanResult", scanResult.value)
                                    }
                                    startActivity(historyIntent)
                                },
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ManageHistory,
                                        contentDescription = "Show History",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getSharedImageUri(intent: Intent?): Uri? {
        return if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } else null
    }

    // ---- UI Components ---- //

    @Composable
    fun BottomNavigationBar(
        navController: NavHostController,
        currentDestination: NavDestination?,
        topLevelRoutes: List<TopLevelRoute>
    ) {
        Box(
            Modifier.fillMaxWidth()
        ) {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                topLevelRoutes.forEach { route ->
                    val isSelected = currentDestination?.route == route.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = route.icon,
                                contentDescription = route.name,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = route.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        },
                        alwaysShowLabel = true
                    )
                }
            }
            // History toggle button

            FloatingActionButton(
                onClick = {
                    val historyIntent = Intent(this@MainActivity, HistoryActivity::class.java)
                    startActivity(historyIntent)
                },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 8.dp)
            ){
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.ManageHistory,
                        contentDescription = "Show History",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    data class TopLevelRoute(val name: String, val route: String, val icon: ImageVector)

    sealed class NavRoute(val route: String) {
        object Scanner : NavRoute("Scanner")
        object Creator : NavRoute("Creator")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Composable
    fun QRscanScreen(historyViewModel: QrHistoryViewModel) {
        ScanScreen(historyViewModel) // Make ScanScreen accept this
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OverlayTopBar(modifier: Modifier) {
        TopAppBar(
            title = { Text("QR Nova", fontWeight = FontWeight.Bold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White
            ),
            modifier = modifier
        )
    }

    @Composable
    fun QRcreateScreen() {
        CreateScreen(viewModel, historyViewModel) // Make CreateScreen accept this
    }
}
