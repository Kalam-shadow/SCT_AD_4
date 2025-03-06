package com.example.qrnova

import android.os.Build
import android.os.Bundle
import android.widget.Space
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BottomNavigation
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.qrnova.ui.theme.QrnovaTheme

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        setContent {
            QrnovaTheme {
                val navController = rememberNavController()
                val topLevelRoutes = listOf(
                    TopLevelRoute("QR Scan", "Scanner", Icons.Default.Search),
                    TopLevelRoute("QR Create", "Creator", Icons.Default.Build)
                )

                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                Column(modifier = Modifier.fillMaxSize()) {
                    // Main Content Area
                    Box(
                        modifier = Modifier
                            .weight(1f) // Takes up all available space
                            .fillMaxWidth()
                    ) {
                        when (currentDestination?.route) {
                            NavRoute.Scanner.route -> OverlayTopBar(modifier = Modifier.zIndex(1f))
                        }
                        NavHost(
                            navController = navController,
                            startDestination = NavRoute.Scanner.route,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable(NavRoute.Scanner.route) { QRscanScreen() }
                            composable(NavRoute.Creator.route) { QRcreateScreen() }
                        }
                    }
                    BottomNavigationBar(navController, currentDestination, topLevelRoutes)
                }
            }
        }
    }

    @Composable
    fun BottomNavigationBar(
        navController: androidx.navigation.NavHostController,
        currentDestination: androidx.navigation.NavDestination?,
        topLevelRoutes: List<TopLevelRoute>
    ) {
        BottomNavigation(
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            topLevelRoutes.forEach { topLevelRoute ->
                BottomNavigationItem(
                    modifier = Modifier.padding(8.dp),
                    icon = { Icon(topLevelRoute.icon, contentDescription = topLevelRoute.name,
                        tint = MaterialTheme.colorScheme.onSurface) },
                    label = { Text(topLevelRoute.name,color = MaterialTheme.colorScheme.onSurface) },
                    selected = currentDestination?.route == topLevelRoute.route,
                    onClick = {
                        navController.navigate(topLevelRoute.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    data class TopLevelRoute(val name: String, val route: String, val icon: ImageVector)

    sealed class NavRoute(val route: String) {
        data object Scanner : NavRoute("Scanner")
        data object Creator : NavRoute("Creator")
    }

    @RequiresApi(Build.VERSION_CODES.R)
    @Composable
    fun QRscanScreen() {
        ScanScreen()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun OverlayTopBar(modifier: Modifier) {
        TopAppBar(
            title = { Text("QR Nova", fontWeight = FontWeight.Bold) },
            
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent,
                titleContentColor = Color.White),
            modifier = modifier
        )
    }

    @Composable
    fun QRcreateScreen() {
        CreateScreen()
    }
}
