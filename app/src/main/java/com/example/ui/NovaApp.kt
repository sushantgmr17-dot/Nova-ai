package com.example.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ChatViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.homework.HomeworkScreen
import com.example.ui.homework.HomeworkViewModel
import com.example.ui.voice.VoiceScreen
import com.example.ui.voice.VoiceViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.image.ImageGenerationScreen
import com.example.ui.image.ImageGenerationViewModel
import com.example.ui.settings.SettingsViewModel
import com.example.ui.admin.AdminScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.theme.Primary
import com.example.ui.theme.MyApplicationTheme

@Composable
fun NovaApp() {
    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val useDarkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    MyApplicationTheme(darkTheme = useDarkTheme) {
        val navController = rememberNavController()
        
        val chatViewModel: ChatViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val historyViewModel: HistoryViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val imageViewModel: ImageGenerationViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val adminViewModel: AdminViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val homeworkViewModel: HomeworkViewModel = viewModel(factory = AppViewModelProvider.Factory)
        val voiceViewModel: VoiceViewModel = viewModel(factory = AppViewModelProvider.Factory)

        val isAppLockEnabled by settingsViewModel.isAppLockEnabled.collectAsStateWithLifecycle()
        var isUnlocked by remember { mutableStateOf(false) }

        if (isAppLockEnabled && !isUnlocked) {
            PinUnlockScreen(
                onCorrectPin = { isUnlocked = true },
                verifyPin = { settingsViewModel.verifyPin(it) }
            )
        } else {

        Scaffold(
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                // Only show bottom bar on home, image, and history
                val isMainScreen = currentDestination?.route in listOf("home", "image", "history", "settings")
                
                if (isMainScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                            label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.route == "home" } == true,
                        onClick = {
                            navController.navigate("home") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            indicatorColor = Primary.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Image, contentDescription = "Image") },
                        label = { Text("Image") },
                        selected = currentDestination?.hierarchy?.any { it.route == "image" } == true,
                        onClick = {
                            navController.navigate("image") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            indicatorColor = Primary.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.History, contentDescription = "History") },
                        label = { Text("History") },
                        selected = currentDestination?.hierarchy?.any { it.route == "history" } == true,
                        onClick = {
                            navController.navigate("history") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            indicatorColor = Primary.copy(alpha = 0.2f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") },
                        selected = currentDestination?.hierarchy?.any { it.route == "settings" } == true,
                        onClick = {
                            navController.navigate("settings") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            indicatorColor = Primary.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "login",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("login") {
                com.example.ui.auth.LoginScreen(
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate("signup") }
                )
            }
            composable("signup") {
                com.example.ui.auth.SignUpScreen(
                    onSignUpSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.navigateUp() }
                )
            }
            composable("home") {
                HomeScreen(
                    onNavigateToChat = { prompt ->
                        chatViewModel.startNewSession(prompt)
                        navController.navigate("chat")
                    },
                    onNavigateToAcademic = {
                        navController.navigate("homework")
                    }
                )
            }
            composable("image") {
                ImageGenerationScreen(viewModel = imageViewModel)
            }
            composable("chat") {
                ChatScreen(
                    viewModel = chatViewModel,
                    onBack = { navController.navigateUp() },
                    onNavigateToVoice = { navController.navigate("voice") }
                )
            }
            composable("history") {
                HistoryScreen(
                    viewModel = historyViewModel,
                    onSessionSelected = { sessionId ->
                        chatViewModel.loadSession(sessionId)
                        navController.navigate("chat")
                    }
                )
            }
            composable("settings") {
                com.example.ui.settings.SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToAdmin = { navController.navigate("admin") }
                )
            }
            composable("admin") {
                AdminScreen(
                    viewModel = adminViewModel,
                    onBack = { navController.navigateUp() }
                )
            }
            composable("homework") {
                HomeworkScreen(
                    viewModel = homeworkViewModel,
                    onBack = { navController.navigateUp() },
                    onNavigateToChat = { prompt ->
                        chatViewModel.startNewSession(prompt)
                        navController.navigate("chat")
                    }
                )
            }
            composable("voice") {
                VoiceScreen(
                    viewModel = voiceViewModel,
                    onBack = { navController.navigateUp() }
                )
            }
        }
    }
    }
    }
}

@Composable
fun PinUnlockScreen(
    onCorrectPin: () -> Unit,
    verifyPin: (String) -> Boolean
) {
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "App Locked",
            tint = Primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "App Locked",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Enter your 4-digit PIN to access Nova AI",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
        )

        // PIN Indicators (4 dots)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            for (i in 0 until 4) {
                val filled = i < enteredPin.length
                val color = if (filled) Primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(color, shape = CircleShape)
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Keypad Grid (1-9, Clear, 0, Backspace)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(280.dp)
        ) {
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("C", "0", "B")
            )
            for (row in keys) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (key in row) {
                        if (key == "C") {
                            IconButton(
                                onClick = {
                                    enteredPin = ""
                                    errorMessage = null
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Text("Clear", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                            }
                        } else if (key == "B") {
                            IconButton(
                                onClick = {
                                    if (enteredPin.isNotEmpty()) {
                                        enteredPin = enteredPin.dropLast(1)
                                        errorMessage = null
                                    }
                                },
                                modifier = Modifier.size(64.dp)
                            ) {
                                Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        } else {
                            FilledTonalButton(
                                onClick = {
                                    if (enteredPin.length < 4) {
                                        val newPin = enteredPin + key
                                        enteredPin = newPin
                                        errorMessage = null
                                        if (newPin.length == 4) {
                                            if (verifyPin(newPin)) {
                                                onCorrectPin()
                                            } else {
                                                errorMessage = "Incorrect PIN"
                                                enteredPin = ""
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
