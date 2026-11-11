package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audio.AudioPlayerManager
import com.example.data.AppSettings
import com.example.overlay.FloatingWindowService
import com.example.permissions.PermissionManager
import com.example.shizuku.ShellManager
import com.example.shizuku.ShizukuManager
import com.example.ui.AudioLibraryScreen
import com.example.ui.DiagnosticsScreen
import com.example.ui.HomeScreen
import com.example.ui.SettingsScreen
import com.example.ui.ShizukuSetupScreen
import com.example.ui.theme.AriyanTheme
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    data object Shizuku : Screen("shizuku", "Shizuku", Icons.Filled.Code, Icons.Outlined.Code)
    data object Library : Screen("library", "Library", Icons.Filled.MusicNote, Icons.Outlined.MusicNote)
    data object Diagnostics : Screen("diagnostics", "Test & Diag", Icons.Filled.GraphicEq, Icons.Outlined.GraphicEq)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private lateinit var playerManager: AudioPlayerManager
    private lateinit var shizukuManager: ShizukuManager
    private lateinit var shellManager: ShellManager
    private lateinit var permissionManager: PermissionManager
    private lateinit var appSettings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        playerManager = AudioPlayerManager.getInstance(this)
        shizukuManager = ShizukuManager.getInstance(this)
        shellManager = ShellManager.getInstance(this)
        permissionManager = PermissionManager(this)
        appSettings = AppSettings.getInstance(this)

        if (appSettings.autoStartFloating.value && permissionManager.canDrawOverlays()) {
            FloatingWindowService.start(this)
        }

        setContent {
            AriyanTheme {
                MainAppContent(
                    playerManager = playerManager,
                    shizukuManager = shizukuManager,
                    shellManager = shellManager,
                    permissionManager = permissionManager,
                    appSettings = appSettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        shizukuManager.refreshStatus()
    }
}

@Composable
fun MainAppContent(
    playerManager: AudioPlayerManager,
    shizukuManager: ShizukuManager,
    shellManager: ShellManager,
    permissionManager: PermissionManager,
    appSettings: AppSettings
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                shizukuManager.refreshStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val screens = listOf(
        Screen.Home,
        Screen.Shizuku,
        Screen.Library,
        Screen.Diagnostics,
        Screen.Settings
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DeepBlack,
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .background(DarkSurface)
                    .navigationBarsPadding()
            ) {
                screens.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CyanAccent,
                            selectedTextColor = CyanAccent,
                            indicatorColor = NeonPurple.copy(alpha = 0.35f),
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        playerManager = playerManager,
                        shizukuManager = shizukuManager,
                        permissionManager = permissionManager,
                        onNavigateToShizuku = { navController.navigate(Screen.Shizuku.route) },
                        onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
                        onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
                    )
                }

                composable(Screen.Shizuku.route) {
                    ShizukuSetupScreen(
                        shizukuManager = shizukuManager,
                        shellManager = shellManager
                    )
                }

                composable(Screen.Library.route) {
                    AudioLibraryScreen(
                        playerManager = playerManager
                    )
                }

                composable(Screen.Diagnostics.route) {
                    DiagnosticsScreen(
                        playerManager = playerManager,
                        shizukuManager = shizukuManager
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        appSettings = appSettings,
                        permissionManager = permissionManager,
                        shizukuManager = shizukuManager
                    )
                }
            }
        }
    }
}
