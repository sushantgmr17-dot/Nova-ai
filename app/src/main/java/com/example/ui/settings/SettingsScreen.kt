package com.example.ui.settings

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.Primary
import com.example.ui.profile.UserProfileManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onNavigateToAdmin: () -> Unit) {
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activity = LocalContext.current as Activity

    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showThemeDialog by remember { mutableStateOf(false) }
    
    var showProfileDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPremiumDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                SettingsCategory("Account")
                
                val avatarEmoji = when (userProfile.avatarIndex) {
                    0 -> "🤖"
                    1 -> "🚀"
                    2 -> "🎨"
                    3 -> "⚡"
                    4 -> "🌟"
                    5 -> "🦄"
                    else -> "👤"
                }
                SettingsItem(
                    icon = Icons.Default.Person, 
                    title = "Profile ($avatarEmoji)", 
                    subtitle = "${userProfile.name} • ${userProfile.email}"
                ) {
                    showProfileDialog = true
                }
                
                if (isPremium) {
                    SettingsItem(
                        icon = Icons.Default.WorkspacePremium, 
                        title = "Premium Plan", 
                        subtitle = "Active - Unlimited Access (₹99/mo)"
                    ) {
                        showPremiumDialog = true
                    }
                } else {
                    SettingsItem(
                        icon = Icons.Default.WorkspacePremium, 
                        title = "Upgrade to Premium", 
                        subtitle = "Fast responses, no ads, pro image gen for ₹99"
                    ) {
                        showPremiumDialog = true
                    }
                }
                
                SettingsCategory("Administration")
                SettingsItem(Icons.Default.AdminPanelSettings, "Admin Panel", "Manage application data and settings") {
                    onNavigateToAdmin()
                }

                SettingsCategory("Security")
                val appLockStatus = if (isAppLockEnabled) "Enabled" else "Disabled"
                SettingsItem(Icons.Default.Lock, "App Lock PIN", "Require a 4-digit PIN to open the app ($appLockStatus)") {
                    showPinDialog = true
                }
                
                SettingsCategory("Preferences")
                val themeLabel = when (themeMode) {
                    "light" -> "Light mode"
                    "dark" -> "Dark mode"
                    else -> "System default"
                }
                SettingsItem(Icons.Default.DarkMode, "Theme", themeLabel) {
                    showThemeDialog = true
                }
                SettingsItem(
                    icon = Icons.Default.Language, 
                    title = "Language", 
                    subtitle = userProfile.preferredLanguage
                ) {
                    showLanguageDialog = true
                }
                SettingsItem(Icons.Default.Notifications, "Notifications", "On") {}
                
                SettingsCategory("Data & Privacy")
                SettingsItem(Icons.Default.Delete, "Clear History", "Delete all local chat history") {}
                SettingsItem(Icons.Default.Security, "Privacy Policy", "Read our privacy guidelines") {}
                
                SettingsCategory("About")
                SettingsItem(Icons.Default.Info, "Version", "1.0.0") {}
            }
        }
    }

    if (showProfileDialog) {
        ProfileEditDialog(
            currentName = userProfile.name,
            currentEmail = userProfile.email,
            currentAvatarIndex = userProfile.avatarIndex,
            onDismiss = { showProfileDialog = false },
            onSave = { name, email, avatarIndex ->
                viewModel.updateProfile(name, email, avatarIndex)
            }
        )
    }

    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguageCode = userProfile.preferredLanguageCode,
            onDismiss = { showLanguageDialog = false },
            onSave = { name, code ->
                viewModel.updateLanguage(name, code)
            }
        )
    }

    if (showPremiumDialog) {
        PremiumSubscriptionDialog(
            isPremium = isPremium,
            onDismiss = { showPremiumDialog = false },
            onSubscribe = { viewModel.setLocalPremium(true) },
            onUnsubscribe = { viewModel.setLocalPremium(false) }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val options = listOf(
                        "system" to "System default",
                        "light" to "Light mode",
                        "dark" to "Dark mode"
                    )
                    options.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setThemeMode(value)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = themeMode == value,
                                onClick = {
                                    viewModel.setThemeMode(value)
                                    showThemeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPinDialog = false
                pinText = ""
                pinError = null
            },
            title = { 
                Text(if (isAppLockEnabled) "Disable PIN Lock" else "Enable PIN Lock")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (isAppLockEnabled) 
                            "Enter your 4-digit PIN to disable App Lock:" 
                        else 
                            "Create a 4-digit PIN to secure your application:"
                    )
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                pinText = input
                                pinError = null
                            }
                        },
                        label = { Text("4-digit PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError != null) {
                        Text(
                            text = pinError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinText.length != 4) {
                            pinError = "PIN must be exactly 4 digits"
                        } else {
                            if (isAppLockEnabled) {
                                if (viewModel.verifyPin(pinText)) {
                                    viewModel.disableAppLock()
                                    showPinDialog = false
                                    pinText = ""
                                    pinError = null
                                } else {
                                    pinError = "Incorrect PIN"
                                }
                            } else {
                                viewModel.enableAppLock(pinText)
                                showPinDialog = false
                                pinText = ""
                                pinError = null
                            }
                        }
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        pinText = ""
                        pinError = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProfileEditDialog(
    currentName: String,
    currentEmail: String,
    currentAvatarIndex: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var email by remember { mutableStateOf(currentEmail) }
    var selectedAvatarIndex by remember { mutableStateOf(currentAvatarIndex) }

    val avatars = listOf("🤖", "🚀", "🎨", "⚡", "🌟", "🦄")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Choose your Avatar:", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    avatars.forEachIndexed { index, emoji ->
                        val isSelected = selectedAvatarIndex == index
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { selectedAvatarIndex = index }
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emoji, fontSize = 24.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, email, selectedAvatarIndex)
                    onDismiss()
                },
                enabled = name.isNotBlank() && email.isNotBlank()
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun LanguageSelectionDialog(
    currentLanguageCode: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val languages = UserProfileManager.LANGUAGES
    var searchQuery by remember { mutableStateOf("") }
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) languages
        else languages.filter { it.first.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Language", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Language...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(modifier = Modifier.height(280.dp).fillMaxWidth()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLanguages.size) { index ->
                            val (name, code) = filteredLanguages[index]
                            val isSelected = currentLanguageCode == code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSave(name, code)
                                        onDismiss()
                                    }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else Color.Transparent
                                    )
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSave(name, code)
                                        onDismiss()
                                    }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun PremiumSubscriptionDialog(
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit,
    onUnsubscribe: () -> Unit
) {
    var showCheckout by remember { mutableStateOf(false) }
    var cardNumber by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }
    var paymentSuccess by remember { mutableStateOf(false) }

    if (isPremium && !showCheckout) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Premium Active", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thank you for subscribing! You have active access to all premium features:")
                    PremiumFeatureRow("⚡ Super-fast response speed")
                    PremiumFeatureRow("🚫 100% Ad-Free experience")
                    PremiumFeatureRow("🎨 Image Generator Pro features unlocked")
                    PremiumFeatureRow("🌍 All world languages supported")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUnsubscribe()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Subscription")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    } else if (paymentSuccess) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Payment Successful! 🎉", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Congratulations! You are now a Premium user. All pro features have been instantly unlocked.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismiss()
                        paymentSuccess = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Awesome!")
                }
            }
        )
    } else if (showCheckout) {
        AlertDialog(
            onDismissRequest = { showCheckout = false },
            title = { Text("Simulated Checkout", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Amount to Pay: ₹99.00 (Rupees)", fontWeight = FontWeight.Bold, color = Primary)
                    
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) cardNumber = it },
                        label = { Text("Card Number (16 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = cardExpiry,
                            onValueChange = { if (it.length <= 5) cardExpiry = it },
                            label = { Text("Expiry (MM/YY)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = cardCvv,
                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) cardCvv = it },
                            label = { Text("CVV") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Text(
                        "This is a secure simulation. Any test inputs will complete successfully and unlock your account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubscribe()
                        paymentSuccess = true
                        showCheckout = false
                    },
                    enabled = cardNumber.length == 16 && cardCvv.length == 3,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Pay ₹99 & Activate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckout = false }) {
                    Text("Back")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Go Premium", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Upgrade today for just ₹99 / month to unlock maximum performance:",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PremiumFeatureRow("⚡ Fast Responses (High-priority AI queues)")
                        PremiumFeatureRow("🚫 Ad-Free Experience (Fully remove all banners)")
                        PremiumFeatureRow("🎨 Image Generator Pro (Unlocks aspect ratios & HD)")
                        PremiumFeatureRow("🌍 All Languages Supported (Speak in 20+ languages)")
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Only ₹99 / month", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Cancel anytime • 100% Secure Checkout", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showCheckout = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upgrade Now for ₹99")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Maybe Later")
                }
            }
        )
    }
}

@Composable
fun PremiumFeatureRow(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
