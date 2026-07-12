package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Primary
import com.example.ui.theme.Secondary

data class ToolItem(val title: String, val icon: ImageVector, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (String?) -> Unit,
    onNavigateToAcademic: () -> Unit
) {
    val tools = listOf(
        ToolItem("AI Chat", Icons.Default.AutoAwesome, Primary),
        ToolItem("Code Assist", Icons.Default.Code, Secondary),
        ToolItem("Homework", Icons.Default.School, Color(0xFFE17055)),
        ToolItem("Translate", Icons.Default.Translate, Color(0xFF00B894)),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nova AI", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Hello, Explorer",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Text(
                text = "What would you like to create today?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(tools, key = { it.title }) { tool ->
                    ToolCard(tool = tool) {
                        if (tool.title == "Homework") {
                            onNavigateToAcademic()
                        } else {
                            onNavigateToChat("Start a session for: ${tool.title}")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Recent Prompts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            PromptPill("Write a sci-fi story about Mars", onNavigateToChat)
            Spacer(modifier = Modifier.height(8.dp))
            PromptPill("Explain quantum computing", onNavigateToChat)
            Spacer(modifier = Modifier.height(8.dp))
            PromptPill("Write a Python script for sorting", onNavigateToChat)
        }
    }
}

@Composable
fun ToolCard(tool: ToolItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(tool.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tool.icon,
                    contentDescription = tool.title,
                    tint = tool.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = tool.title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun PromptPill(prompt: String, onClick: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(prompt) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Text(
            text = prompt,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
