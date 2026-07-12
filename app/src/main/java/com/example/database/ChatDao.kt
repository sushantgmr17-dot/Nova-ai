package com.example.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.model.ChatMessage
import com.example.model.ChatSession
import com.example.model.GeneratedImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Query("DELETE FROM chat_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)
    
    @Query("UPDATE chat_messages SET text = :text WHERE id = :id")
    suspend fun updateMessageText(id: Int, text: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId AND id > :messageId")
    suspend fun deleteMessagesAfter(sessionId: Int, messageId: Int)
    
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Int)
    
    @Query("SELECT COUNT(*) FROM chat_sessions")
    fun getTotalSessions(): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM chat_messages")
    fun getTotalMessages(): Flow<Int>
    
    @Query("UPDATE chat_sessions SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateSessionFavorite(id: Int, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeneratedImage(image: GeneratedImage): Long

    @Query("SELECT * FROM generated_images ORDER BY timestamp DESC")
    fun getAllGeneratedImages(): Flow<List<GeneratedImage>>

    @Query("DELETE FROM generated_images WHERE id = :id")
    suspend fun deleteGeneratedImageById(id: Int)

    @Query("DELETE FROM generated_images")
    suspend fun deleteAllGeneratedImages()
    
    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()
    
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAllMessages()
}
