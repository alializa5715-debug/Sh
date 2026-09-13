package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "discovered_models")
data class DiscoveredModelEntity(
    @PrimaryKey val id: String, // format: "$providerType:$modelId"
    val providerType: String, // e.g. "GEMINI", "OPENAI_COMPATIBLE"
    val modelId: String, // the exact model ID from provider API (e.g. "gemini-2.5-flash", "llama-3.3-70b-versatile")
    val displayName: String,
    val description: String = "",
    val version: String = "",
    val status: String = "DISCOVERED", // "DISCOVERED", "VERIFIED", "UNAVAILABLE"
    val availability: String = "AVAILABLE", // "AVAILABLE", "UNAVAILABLE", "DEPRECATED"
    val freeTier: Boolean = false,
    val freeTierNote: String = "",
    val inputCapabilities: String = "text", // comma-separated: text,image,audio
    val outputCapabilities: String = "text", // comma-separated: text,image
    val supportsStreaming: Boolean = true,
    val supportsVision: Boolean = false,
    val supportsImageGeneration: Boolean = false,
    val supportsAudio: Boolean = false,
    val supportsRealtime: Boolean = false,
    val contextWindow: Int = 0,
    val speedCategory: String = "FAST", // "LIGHTNING", "FAST", "STANDARD", "REASONING"
    val lastChecked: Long = 0L,
    val lastVerified: Long = 0L
) {
    val isVerified: Boolean get() = status == "VERIFIED"
    val isAvailable: Boolean get() = availability == "AVAILABLE"
}
