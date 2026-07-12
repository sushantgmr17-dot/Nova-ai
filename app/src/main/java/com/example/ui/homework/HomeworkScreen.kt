package com.example.ui.homework

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.Primary
import com.example.ui.theme.Secondary
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkScreen(
    viewModel: HomeworkViewModel,
    onBack: () -> Unit,
    onNavigateToChat: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val ocrResult by viewModel.ocrResult.collectAsStateWithLifecycle()
    val homeworkSolution by viewModel.homeworkSolution.collectAsStateWithLifecycle()
    val pdfSummary by viewModel.pdfSummary.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Homework, 1: OCR Scanner, 2: PDF Summarizer
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Academic Tools", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = Primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeTab]),
                            color = Primary
                        )
                    }
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = {
                            activeTab = 0
                            viewModel.clearAll()
                        },
                        icon = { Icon(Icons.Default.School, contentDescription = "Homework Solver") },
                        text = { Text("Homework", style = MaterialTheme.typography.labelSmall) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = {
                            activeTab = 1
                            viewModel.clearAll()
                        },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "OCR Scanner") },
                        text = { Text("OCR Scan", style = MaterialTheme.typography.labelSmall) }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = {
                            activeTab = 2
                            viewModel.clearAll()
                        },
                        icon = { Icon(Icons.Default.Description, contentDescription = "PDF Summarizer") },
                        text = { Text("PDF Summarize", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (activeTab) {
                0 -> HomeworkTab(
                    viewModel = viewModel,
                    isLoading = isLoading,
                    solution = homeworkSolution,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(it))
                        scope.launch { snackbarHostState.showSnackbar("Solution copied!") }
                    }
                )
                1 -> OcrTab(
                    viewModel = viewModel,
                    isLoading = isLoading,
                    ocrResult = ocrResult,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(it))
                        scope.launch { snackbarHostState.showSnackbar("Copied text to clipboard!") }
                    },
                    onSendToChat = {
                        onNavigateToChat(it)
                    }
                )
                2 -> PdfTab(
                    viewModel = viewModel,
                    isLoading = isLoading,
                    pdfSummary = pdfSummary,
                    onCopy = {
                        clipboardManager.setText(AnnotatedString(it))
                        scope.launch { snackbarHostState.showSnackbar("Summary copied!") }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkTab(
    viewModel: HomeworkViewModel,
    isLoading: Boolean,
    solution: String?,
    onCopy: (String) -> Unit
) {
    val context = LocalContext.current
    var questionText by remember { mutableStateOf("") }
    var attachedImageUri by remember { mutableStateOf<Uri?>(null) }
    var attachedImageBase64 by remember { mutableStateOf<String?>(null) }
    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val subjects = listOf("Mathematics", "Physics", "Chemistry", "Biology", "History", "Computer Science", "Literature", "General Study")

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachedImageUri = uri
        if (uri != null) {
            attachedImageBase64 = convertUriToBase64(context, uri)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Homework Expert",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Ask complex questions or upload assignment photos to receive guided, step-by-step academic solutions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Subject selector dropdown
        Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            ExposedDropdownMenuBox(
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = !dropdownExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSubject,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Subject") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                ExposedDropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false }
                ) {
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject) },
                            onClick = {
                                selectedSubject = subject
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Question Input field
        OutlinedTextField(
            value = questionText,
            onValueChange = { questionText = it },
            label = { Text("Type your homework question here...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            ),
            shape = RoundedCornerShape(12.dp),
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Image Attachment Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .background(Primary.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Attach photo", tint = Primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Attach Question Photo", color = Primary, fontWeight = FontWeight.Bold)
            }

            if (attachedImageUri != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            attachedImageUri = null
                            attachedImageBase64 = null
                        }
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove photo", tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Image", color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        }

        // Preview Attached Image
        if (attachedImageUri != null) {
            Spacer(modifier = Modifier.height(12.dp))
            val bitmap = remember(attachedImageUri) {
                try {
                    val stream = context.contentResolver.openInputStream(attachedImageUri!!)
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Attached Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                viewModel.solveHomework(selectedSubject, questionText, attachedImageBase64)
            },
            enabled = !isLoading && (questionText.isNotBlank() || attachedImageBase64 != null),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Icon(Icons.Default.Psychology, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Solve step-by-step", fontWeight = FontWeight.Bold)
            }
        }

        // Solution Output Area
        if (solution != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Tutor Explanation:",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = solution,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onCopy(solution) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Solution")
                    }
                }
            }
        }
    }
}

@Composable
fun OcrTab(
    viewModel: HomeworkViewModel,
    isLoading: Boolean,
    ocrResult: String?,
    onCopy: (String) -> Unit,
    onSendToChat: (String) -> Unit
) {
    val context = LocalContext.current
    var documentUri by remember { mutableStateOf<Uri?>(null) }
    var documentBase64 by remember { mutableStateOf<String?>(null) }

    val scannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        documentUri = uri
        if (uri != null) {
            documentBase64 = convertUriToBase64(context, uri)
        }
    }

    // Scanning animation properties
    val transition = rememberInfiniteTransition(label = "scanner")
    val animOffsetY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scannerLine"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Smart Document OCR Scanner",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Select any document image, whiteboard snap, or screenshot to instantly extract its raw text.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (documentUri == null) {
            // Drop zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { scannerPickerLauncher.launch("image/*") }
                    .border(2.dp, Primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = "Scan icon",
                        tint = Primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Upload Document Image",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "PNG, JPEG supported",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Image and scanning display
            val bitmap = remember(documentUri) {
                try {
                    val stream = context.contentResolver.openInputStream(documentUri!!)
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = "Extracted Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )

                    if (isLoading) {
                        // Moving scan bar overlay
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val scanY = animOffsetY * maxHeight.value
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .offset(y = scanY.dp)
                                    .background(Primary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { scannerPickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Pick Another")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Image")
                }

                Button(
                    onClick = {
                        documentBase64?.let { viewModel.scanImageForText(it) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    enabled = !isLoading && documentBase64 != null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Icon(Icons.Default.FlashOn, contentDescription = "Scan")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extract Text")
                    }
                }
            }
        }

        if (ocrResult != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Extracted Document Text:",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Secondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = ocrResult,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { onCopy(ocrResult) },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Text")
                        }
                        
                        Button(
                            onClick = { onSendToChat("Please explain and analyze this scanned text:\n\n$ocrResult") },
                            colors = ButtonDefaults.buttonColors(containerColor = Secondary)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = "Chat")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Analyze in Chat")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PdfTab(
    viewModel: HomeworkViewModel,
    isLoading: Boolean,
    pdfSummary: String?,
    onCopy: (String) -> Unit
) {
    val context = LocalContext.current
    var attachedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var attachedPdfName by remember { mutableStateOf<String?>(null) }
    var attachedPdfSize by remember { mutableStateOf<Int?>(null) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachedPdfUri = uri
        if (uri != null) {
            attachedPdfName = getFileName(context, uri) ?: "document.pdf"
            attachedPdfSize = getFileSize(context, uri) ?: 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "AI PDF Document Summarizer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Upload any PDF textbook chapter, journal paper, or work report to generate structured takeaways and summaries instantly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (attachedPdfUri == null) {
            // Drop zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { documentPickerLauncher.launch("application/pdf") }
                    .border(2.dp, Secondary.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = "Upload PDF",
                        tint = Secondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Upload PDF Document",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Limit 15MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // Document card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "PDF File",
                        tint = Color(0xFFE74C3C),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = attachedPdfName ?: "document.pdf",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        Text(
                            text = "${((attachedPdfSize ?: 0) / 1024f / 1024f).format(2)} MB",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    IconButton(onClick = {
                        attachedPdfUri = null
                        attachedPdfName = null
                        attachedPdfSize = null
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Remove")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val uri = attachedPdfUri ?: return@Button
                    val bytes = readBytesFromUri(context, uri)
                    if (bytes != null) {
                        viewModel.summarizePdf(attachedPdfName ?: "document.pdf", bytes)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.Summarize, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate AI Summary", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (pdfSummary != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Document Summary:",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = pdfSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onCopy(pdfSummary) },
                        colors = ButtonDefaults.buttonColors(containerColor = Secondary, contentColor = Color.White),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Summary")
                    }
                }
            }
        }
    }
}

// Helpers
fun convertUriToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        bytes
    } catch (e: Exception) {
        null
    }
}

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

fun getFileSize(context: Context, uri: Uri): Int? {
    var result: Int? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (index != -1) {
                    result = cursor.getInt(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    return result
}

fun Float.format(digits: Int) = String.format("%.${digits}f", this)
