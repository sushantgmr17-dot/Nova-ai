package com.example.ui.chat

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ChatMessage
import com.example.ui.theme.Primary
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onNavigateToVoice: () -> Unit
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val generatingMessage by viewModel.generatingMessage.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val selectedPersona by viewModel.selectedPersona.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editInputText by remember { mutableStateOf("") }
    
    val suggestions = remember(messages) { getSuggestedPrompts(messages) }
    
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // TTS initialized
            }
        }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spokenText = matches[0]
                inputText = if (inputText.isEmpty()) spokenText else "$inputText $spokenText"
            }
        }
    }

    LaunchedEffect(error) {
        error?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Retry",
                duration = SnackbarDuration.Indefinite
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.retryLastMessage()
            } else {
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Nova Chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToVoice) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Voice Assistant",
                            tint = Primary
                        )
                    }

                    var showPersonaMenu by remember { mutableStateOf(false) }
                    
                    Box {
                        TextButton(
                            onClick = { showPersonaMenu = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (selectedPersona) {
                                        "concise" -> "Concise ⚡"
                                        "detailed" -> "Detailed 📚"
                                        "creative" -> "Creative 🎨"
                                        else -> "Balanced ⚖️"
                                    },
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showPersonaMenu,
                            onDismissRequest = { showPersonaMenu = false }
                        ) {
                            val options = listOf(
                                "balanced" to "Balanced ⚖️",
                                "concise" to "Concise ⚡",
                                "detailed" to "Detailed 📚",
                                "creative" to "Creative 🎨"
                            )
                            options.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setPersona(value)
                                        showPersonaMenu = false
                                    },
                                    leadingIcon = {
                                        RadioButton(
                                            selected = selectedPersona == value,
                                            onClick = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column {
                    if (!isPremium) {
                        AdBanner()
                    }
                    if (suggestions.isNotEmpty() && !isLoading) {
                        SuggestedPromptsCarousel(
                            suggestions = suggestions,
                            onSuggestionClick = { suggestion ->
                                // Clean up any visual emoji prefixes from suggestion text before sending if they exist,
                                // or send it directly. Direct send is usually best and very clean!
                                val cleanText = suggestion.replace(Regex("^\\s*[\\p{So}\\p{Cn}]+\\s*"), "")
                                viewModel.sendMessage(cleanText)
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Nova...") },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    }
                                    speechRecognizerLauncher.launch(intent)
                                }
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice Input")
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Primary, RoundedCornerShape(24.dp))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            } // close Column
            } // close Surface
        } // close bottomBar
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            reverseLayout = false
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    onPlayAudio = { text ->
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    },
                    onEditClick = { msg ->
                        editingMessage = msg
                        editInputText = msg.text
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            if (generatingMessage != null) {
                item {
                    MessageBubble(
                        message = ChatMessage(role = "model", text = generatingMessage!!, sessionId = 0),
                        onPlayAudio = null
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            } else if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(24.dp)
                            .padding(top = 16.dp),
                        color = Primary
                    )
                }
            }
        }
    }

    if (editingMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                editingMessage = null 
                editInputText = ""
            },
            title = { 
                Text("Edit Message", fontWeight = FontWeight.Bold) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Editing this message will delete all subsequent messages in this session and trigger a regeneration from this point.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = editInputText,
                        onValueChange = { editInputText = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val msg = editingMessage
                        if (msg != null && editInputText.isNotBlank()) {
                            viewModel.editMessage(msg.id, editInputText)
                        }
                        editingMessage = null
                        editInputText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)
                ) {
                    Text("Save & Regenerate")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        editingMessage = null 
                        editInputText = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdBanner() {
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-3940256099942544/6300978111" // Test banner ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    onPlayAudio: ((String) -> Unit)? = null,
    onEditClick: ((ChatMessage) -> Unit)? = null
) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .widthIn(max = if (isUser) 280.dp else 340.dp)
                    .background(
                        color = if (isUser) Primary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isUser) 20.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 20.dp
                        )
                    )
                    .then(
                        if (isUser && onEditClick != null) {
                            Modifier.clickable { onEditClick(message) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(16.dp)
            ) {
                if (isUser) {
                    Text(
                        text = message.text,
                        color = Color.White
                    )
                } else {
                    val blocks = remember(message.text) { parseMessageContent(message.text) }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        blocks.forEach { block ->
                            when (block) {
                                is MessageContentBlock.Text -> {
                                    Text(
                                        text = block.text,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                                is MessageContentBlock.Code -> {
                                    CodeBlock(
                                        language = block.language,
                                        code = block.code
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (isUser && onEditClick != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    IconButton(
                        onClick = { onEditClick(message) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit message",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Tap to edit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onEditClick(message) }
                    )
                }
            }
            if (!isUser && onPlayAudio != null) {
                IconButton(
                    onClick = { onPlayAudio(message.text) },
                    modifier = Modifier.size(32.dp).padding(top = 4.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "Read aloud",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SuggestedPromptsCarousel(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = "Suggested Prompts",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Primary,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(suggestions) { suggestion ->
                SuggestionChip(
                    text = suggestion,
                    onClick = { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        modifier = modifier.heightIn(min = 40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun getSuggestedPrompts(messages: List<ChatMessage>): List<String> {
    if (messages.isEmpty()) {
        return listOf(
            "🚀 Explain Quantum Computing",
            "📝 Write a sci-fi short story",
            "🗺️ Plan a 3-day Tokyo trip",
            "💡 Suggest a cool business idea"
        )
    }

    // Scan messages for keywords to provide context-aware options
    val concatenatedText = messages.takeLast(4).joinToString(" ") { it.text.lowercase() }

    return when {
        concatenatedText.contains("code") || 
        concatenatedText.contains("function") || 
        concatenatedText.contains("program") || 
        concatenatedText.contains("kotlin") || 
        concatenatedText.contains("java") || 
        concatenatedText.contains("python") || 
        concatenatedText.contains("bug") || 
        concatenatedText.contains("error") || 
        concatenatedText.contains("api") || 
        concatenatedText.contains("class") -> listOf(
            "🔍 Explain this code step-by-step",
            "🛠️ Write a unit test for this",
            "⚡ How can I optimize this?",
            "🐛 Find bugs in this snippet"
        )

        concatenatedText.contains("story") || 
        concatenatedText.contains("write") || 
        concatenatedText.contains("poem") || 
        concatenatedText.contains("essay") || 
        concatenatedText.contains("blog") || 
        concatenatedText.contains("draft") || 
        concatenatedText.contains("creative") || 
        concatenatedText.contains("lyric") || 
        concatenatedText.contains("script") -> listOf(
            "✍️ Make the tone more professional",
            "📖 Write a sequel or follow-up",
            "📝 Summarize into bullet points",
            "🏷️ Suggest 5 catchy titles"
        )

        concatenatedText.contains("explain") || 
        concatenatedText.contains("why") || 
        concatenatedText.contains("how") || 
        concatenatedText.contains("history") || 
        concatenatedText.contains("science") || 
        concatenatedText.contains("physics") || 
        concatenatedText.contains("math") || 
        concatenatedText.contains("learn") || 
        concatenatedText.contains("teach") -> listOf(
            "💡 Give me a real-world analogy",
            "⚖️ What are the counter-arguments?",
            "📌 What are the key takeaways?",
            "❓ Quiz me on this topic!"
        )

        concatenatedText.contains("plan") || 
        concatenatedText.contains("trip") || 
        concatenatedText.contains("travel") || 
        concatenatedText.contains("itinerary") || 
        concatenatedText.contains("visit") || 
        concatenatedText.contains("vacation") || 
        concatenatedText.contains("hotel") || 
        concatenatedText.contains("flight") || 
        concatenatedText.contains("tourism") -> listOf(
            "💰 Give me a budget breakdown",
            "🍽️ Recommend local foods to try",
            "🎒 What should I pack?",
            "🚨 Safety tips for this place"
        )

        else -> listOf(
            "🤔 Tell me more about that",
            "💡 Give me a practical example",
            "⚖️ What are the pros and cons?",
            "🌱 Explain it like I'm 5"
        )
    }
}

sealed class MessageContentBlock {
    data class Text(val text: String) : MessageContentBlock()
    data class Code(val language: String, val code: String) : MessageContentBlock()
}

fun parseMessageContent(text: String): List<MessageContentBlock> {
    val blocks = mutableListOf<MessageContentBlock>()
    val lines = text.split("\n")
    var inCodeBlock = false
    var currentLanguage = ""
    val currentCode = StringBuilder()
    val currentText = StringBuilder()

    for (line in lines) {
        if (line.trim().startsWith("```")) {
            if (inCodeBlock) {
                // End of code block
                blocks.add(MessageContentBlock.Code(currentLanguage, currentCode.toString().trimEnd()))
                currentCode.clear()
                inCodeBlock = false
            } else {
                // Start of code block
                if (currentText.isNotEmpty()) {
                    blocks.add(MessageContentBlock.Text(currentText.toString().trimEnd()))
                    currentText.clear()
                }
                inCodeBlock = true
                currentLanguage = line.trim().substring(3).trim()
            }
        } else {
            if (inCodeBlock) {
                currentCode.append(line).append("\n")
            } else {
                currentText.append(line).append("\n")
            }
        }
    }

    if (inCodeBlock) {
        // Unclosed code block
        blocks.add(MessageContentBlock.Code(currentLanguage, currentCode.toString().trimEnd()))
    } else if (currentText.isNotEmpty()) {
        blocks.add(MessageContentBlock.Text(currentText.toString().trimEnd()))
    }

    return blocks
}

@Composable
fun CodeBlock(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var isCopied by remember { mutableStateOf(false) }

    LaunchedEffect(code) {
        isCopied = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0A0C10))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        // Header bar with language name and copy button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF141722))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" }.lowercase(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        clipboardManager.setText(AnnotatedString(code))
                        isCopied = true
                    }
                    .testTag("copy_code_button")
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = if (isCopied) "Copied" else "Copy code",
                    tint = if (isCopied) Color(0xFF4CAF50) else Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCopied) "Copied!" else "Copy",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = if (isCopied) Color(0xFF4CAF50) else Primary
                )
            }
        }

        // Code space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFDFE4EA),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

