package com.example.ui.image

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.FileProvider
import com.example.model.GeneratedImage
import com.example.ui.theme.Primary
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageGenerationScreen(
    viewModel: ImageGenerationViewModel
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val generatedImageBase64 by viewModel.generatedImageBase64.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val imageHistory by viewModel.imageHistory.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(0) } // 0: Create, 1: Gallery
    var selectedGalleryImage by remember { mutableStateOf<GeneratedImage?>(null) }

    var prompt by remember { mutableStateOf("") }
    
    val styles = listOf("None", "Realistic", "Anime", "Cartoon", "Cyberpunk", "Pixar-style", "Fantasy", "Portrait", "Landscape")
    val proStyles = listOf("Cyberpunk", "Pixar-style", "Fantasy")
    var selectedStyle by remember { mutableStateOf(styles.first()) }
    
    val ratios = listOf("1:1", "9:16", "16:9")
    val proRatios = listOf("9:16", "16:9")
    var selectedRatio by remember { mutableStateOf(ratios.first()) }
    
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
                    title = { Text("AI Image Studio", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
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
                        onClick = { activeTab = 0 },
                        text = { Text("Create", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Gallery (${imageHistory.size})", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (activeTab == 0) {
            // Create tab
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Result Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Primary)
                    } else if (generatedImageBase64 != null) {
                        val bitmap = remember(generatedImageBase64) {
                            try {
                                val decodedBytes = Base64.decode(generatedImageBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "Generated Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                "Failed to decode generated image",
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        Text(
                            "Your masterpiece will appear here",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Buttons for Generated Image
                if (generatedImageBase64 != null && !isLoading) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { 
                                viewModel.saveImageToGallery(context, generatedImageBase64!!)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Image saved to Pictures/NovaAI!")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download HD")
                        }
                        
                        Button(
                            onClick = { shareImage(context, generatedImageBase64!!) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Share")
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        label = { Text("Describe the image you want...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Art Style", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        styles.forEach { style ->
                            val isPro = style in proStyles
                            FilterChip(
                                selected = selectedStyle == style,
                                onClick = { 
                                    if (isPro && !isPremium) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("👑 $style is a PRO style. Go Premium in Settings to unlock!")
                                        }
                                    } else {
                                        selectedStyle = style
                                    }
                                },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(style)
                                        if (isPro && !isPremium) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.WorkspacePremium,
                                                contentDescription = "Pro Style",
                                                tint = Primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Aspect Ratio", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ratios.forEach { ratio ->
                            val isPro = ratio in proRatios
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { 
                                    if (isPro && !isPremium) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("👑 $ratio is a PRO aspect ratio. Go Premium in Settings to unlock!")
                                        }
                                    } else {
                                        selectedRatio = ratio
                                    }
                                },
                                label = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(ratio)
                                        if (isPro && !isPremium) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.WorkspacePremium,
                                                contentDescription = "Pro Aspect Ratio",
                                                tint = Primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.padding(end = 8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { 
                            viewModel.generateImage(prompt, selectedStyle, selectedRatio)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        enabled = !isLoading && prompt.isNotBlank()
                    ) {
                        Text("Generate Image", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            // Gallery / History tab
            if (imageHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Your image library is empty. Go create some masterpieces!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(32.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(imageHistory, key = { it.id }) { image ->
                        val bitmap = remember(image.imageBase64) {
                            try {
                                val decodedBytes = Base64.decode(image.imageBase64, Base64.DEFAULT)
                                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable { selectedGalleryImage = image },
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = image.prompt,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Corrupt", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = image.prompt,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full screen detail viewer dialog for saved images
    selectedGalleryImage?.let { image ->
        val clipboardManager = LocalClipboardManager.current
        val galleryBitmap = remember(image.imageBase64) {
            try {
                val decodedBytes = Base64.decode(image.imageBase64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }

        Dialog(
            onDismissRequest = { selectedGalleryImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Close button
                    IconButton(
                        onClick = { selectedGalleryImage = null },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }

                    // Delete button
                    IconButton(
                        onClick = {
                            viewModel.deleteImage(image.id)
                            selectedGalleryImage = null
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }

                    // Main image
                    if (galleryBitmap != null) {
                        Image(
                            bitmap = galleryBitmap,
                            contentDescription = image.prompt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                                .padding(vertical = 50.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            "Cannot load image",
                            color = Color.White,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // bottom bar with prompt, actions, and copies
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(24.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = image.prompt,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Style: ${image.style}", color = Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White.copy(alpha = 0.15f))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            SuggestionChip(
                                onClick = {},
                                label = { Text("Aspect: ${image.aspectRatio}", color = Color.White) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = Color.White.copy(alpha = 0.15f))
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(image.prompt))
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Prompt copied to clipboard!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Prompt")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Prompt")
                            }

                            Button(
                                onClick = {
                                    viewModel.saveImageToGallery(context, image.imageBase64)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Image saved to Pictures/NovaAI!")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Primary, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save HD")
                            }

                            Button(
                                onClick = { shareImage(context, image.imageBase64) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
}

fun shareImage(context: Context, base64: String) {
    try {
        val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "shared_image.png")
        val fileOutputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream)
        fileOutputStream.close()

        val uri: Uri = FileProvider.getUriForFile(context, context.packageName + ".provider", file)

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
