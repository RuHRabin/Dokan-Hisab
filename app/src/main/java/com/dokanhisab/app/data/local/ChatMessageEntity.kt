package com.dokanhisab.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val role: String, // "user" or "model"
    val text: String,
    val modelId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
