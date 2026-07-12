package com.example.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicNone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.Primary
import com.example.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val userSpeechText by viewModel.userSpeechText.collectAsStateWithLifecycle()
    val aiSpeechResponse by viewModel.aiSpeechResponse.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
    }

    // Concentric pulsing mic rings animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale1"
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale2"
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha2"
    )

    Scaffold(
        containerColor = Color(0xFF0F0C20) // Deep cosmic navy
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Close Button
            IconButton(
                onClick = {
                    viewModel.stopSpeaking()
                    viewModel.stopListening()
                    onBack()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close Voice Screen", tint = Color.White)
            }

            // Subtitle Info & Transcripts Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Nova Voice AI",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                // User transcription subtitles
                if (userSpeechText.isNotEmpty()) {
                    Text(
                        text = "“ $userSpeechText ”",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // AI Response speech text
                Text(
                    text = aiSpeechResponse,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Visual central pulsing microphone
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                if (isListening || isSpeaking) {
                    // Ring 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(pulseScale2)
                            .background(
                                color = if (isListening) Color(0xFFF39C12).copy(alpha = pulseAlpha2) else Primary.copy(alpha = pulseAlpha2),
                                shape = CircleShape
                            )
                    )

                    // Ring 2
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .scale(pulseScale1)
                            .background(
                                color = if (isListening) Color(0xFFF1C40F).copy(alpha = pulseAlpha1) else Secondary.copy(alpha = pulseAlpha1),
                                shape = CircleShape
                            )
                    )
                }

                // Inner core button
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = when {
                                    isListening -> listOf(Color(0xFFF39C12), Color(0xFFD35400))
                                    isSpeaking -> listOf(Primary, Secondary)
                                    else -> listOf(Color(0xFF2C3E50), Color(0xFF34495E))
                                }
                            )
                        )
                        .clickable {
                            if (!hasAudioPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isListening) {
                                    viewModel.stopListening()
                                } else {
                                    viewModel.startListening()
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isListening -> {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Listening...",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        isSpeaking -> {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Speaking...",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.MicNone,
                                contentDescription = "Tap to talk",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Instructions label
            Text(
                text = when {
                    isListening -> "Nova is listening. Speak clearly now..."
                    isSpeaking -> "Nova is answering..."
                    else -> "Tap the mic button and ask me anything."
                },
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 56.dp)
            )
        }
    }
}
