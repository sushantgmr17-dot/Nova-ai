package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "generated_images")
data class GeneratedImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val prompt: String,
    val style: String,
    val aspectRatio: String,
    val imageBase64: String,
    val timestamp: Long = System.currentTimeMillis()
)
