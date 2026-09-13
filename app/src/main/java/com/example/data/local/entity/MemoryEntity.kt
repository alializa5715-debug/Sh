package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val fact: String,
    val category: String = "Preference", // "Preference", "Personal", "Work", "Topic"
    val createdAt: Long = System.currentTimeMillis(),
    val isEnabled: Boolean = true
)
