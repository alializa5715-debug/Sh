package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_configs")
data class ApiConfigEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val category: String = "Chat / LLM",
    val providerType: String = "GEMINI", // GEMINI, OPENAI_COMPATIBLE, ANTHROPIC, CUSTOM
    val apiKey: String = "",
    val baseUrl: String = "",
    val modelName: String = "",
    val organizationId: String? = null,
    val isEnabled: Boolean = true,
    val isDefault: Boolean = false,
    val status: String = "UNTESTED", // CONNECTED, UNTESTED, ERROR, MODEL_UNAVAILABLE
    val lastTestedTimestamp: Long = 0L,
    val lastLatencyMs: Long = 0L,
    val lastErrorMessage: String? = null,
    val supportedCapabilities: String = "chat,vision,streaming"
) {
    val isConnected: Boolean get() = status == "CONNECTED"

    val maskedApiKey: String
        get() {
            if (apiKey.isBlank()) return "Not configured"
            if (apiKey.length <= 8) return "••••••••"
            return "••••••••${apiKey.takeLast(4)}"
        }
}
