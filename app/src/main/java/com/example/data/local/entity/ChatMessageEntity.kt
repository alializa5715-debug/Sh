package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val role: String, // "user", "assistant", "system", "error"
    val content: String,
    val spokenText: String? = null,
    val mediaUri: String? = null,
    val mediaType: String? = null, // "image", "pdf", "text"
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val cardType: String? = null, // "quiz", "diagram", "image_result", "music_result", "video_result"
    val cardJson: String? = null
)
