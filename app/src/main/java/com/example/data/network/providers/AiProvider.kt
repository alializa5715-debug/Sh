package com.example.data.network.providers

import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.ChatMessageEntity

data class ConnectionTestResult(
    val success: Boolean,
    val latencyMs: Long,
    val message: String,
    val availableModels: List<String> = emptyList(),
    val discoveredModelsCount: Int = 0
)

data class ModelVerificationResult(
    val verified: Boolean,
    val latencyMs: Long,
    val message: String,
    val statusCode: Int = 200,
    val is404: Boolean = false
)

data class DiscoveredModel(
    val modelId: String,
    val displayName: String,
    val description: String = "",
    val version: String = "",
    val freeTier: Boolean = false,
    val freeTierNote: String = "",
    val inputCapabilities: String = "text",
    val outputCapabilities: String = "text",
    val supportsVision: Boolean = false,
    val supportsImageGeneration: Boolean = false,
    val supportsAudio: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsRealtime: Boolean = false,
    val contextWindow: Int = 0,
    val speedCategory: String = "FAST" // "LIGHTNING", "FAST", "STANDARD", "REASONING"
)

data class ChatResponse(
    val text: String,
    val spokenText: String,
    val cardType: String? = null,
    val cardJson: String? = null,
    val modelUsed: String = ""
)

data class ImageResult(
    val imageUrl: String? = null,
    val base64Data: String? = null,
    val mimeType: String = "image/png"
)

interface AiProvider {
    suspend fun listModels(config: ApiConfigEntity): Result<List<DiscoveredModel>>
    suspend fun verifyModel(config: ApiConfigEntity, modelId: String, capability: String = "chat"): Result<ModelVerificationResult>
    suspend fun testConnection(config: ApiConfigEntity): Result<ConnectionTestResult>
    suspend fun generateChat(
        config: ApiConfigEntity,
        messages: List<ChatMessageEntity>,
        memoryPrompt: String? = null,
        onChunk: ((String) -> Unit)? = null
    ): Result<ChatResponse>
    suspend fun generateImage(
        config: ApiConfigEntity,
        prompt: String,
        aspectRatio: String,
        size: String
    ): Result<ImageResult>
}

