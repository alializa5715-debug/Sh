package com.example.data.network.providers

import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.network.HttpClientFactory
import com.example.data.network.SpeechCleaner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAiCompatibleProvider : AiProvider {

    private val client = HttpClientFactory.okHttpClient
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun listModels(config: ApiConfigEntity): Result<List<DiscoveredModel>> =
        withContext(Dispatchers.IO) {
            val apiKey = config.apiKey.trim()
            val rawBase = config.baseUrl.trim().ifEmpty { "https://api.openai.com/v1" }
            val baseUrl = rawBase.trimEnd('/')

            try {
                val reqBuilder = Request.Builder()
                    .url("$baseUrl/models")
                    .get()

                if (apiKey.isNotBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer $apiKey")
                }
                if (!config.organizationId.isNullOrBlank()) {
                    reqBuilder.addHeader("OpenAI-Organization", config.organizationId)
                }

                client.newCall(reqBuilder.build()).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception(parseError(body, response.code)))
                    }

                    val json = JSONObject(body)
                    val dataArr = json.optJSONArray("data") ?: JSONArray()
                    val discovered = mutableListOf<DiscoveredModel>()

                    val isGroq = baseUrl.contains("groq.com", ignoreCase = true)
                    val isOllama = baseUrl.contains("localhost") || baseUrl.contains("127.0.0.1") || baseUrl.contains("11434")
                    val isOpenAi = baseUrl.contains("openai.com", ignoreCase = true)
                    val isDeepSeek = baseUrl.contains("deepseek.com", ignoreCase = true)
                    val isXAi = baseUrl.contains("x.ai", ignoreCase = true)

                    for (i in 0 until dataArr.length()) {
                        val m = dataArr.getJSONObject(i)
                        val modelId = m.optString("id").trim()
                        if (modelId.isBlank()) continue

                        val lowerId = modelId.lowercase()
                        // Keep ONLY eligible chat-compatible models
                        if (lowerId.contains("embedding") || lowerId.contains("moderation") ||
                            lowerId.contains("whisper") || lowerId.contains("tts") ||
                            lowerId.contains("dall-e") || lowerId.contains("image") ||
                            lowerId.contains("realtime") || lowerId.contains("audio-preview") ||
                            lowerId.contains("babbage") || lowerId.contains("davinci") ||
                            lowerId.contains("instruct")
                        ) {
                            continue
                        }

                        val isVision = lowerId.contains("vision") || lowerId.contains("4o") ||
                                lowerId.contains("vl") || lowerId.contains("omni")
                        val isFast = lowerId.contains("mini") || lowerId.contains("8b") ||
                                lowerId.contains("instant") || lowerId.contains("flash") ||
                                lowerId.contains("small")

                        val hasFreeTier = when {
                            isGroq -> true
                            isOllama -> true
                            isOpenAi -> false
                            isXAi -> false
                            isDeepSeek -> false
                            else -> false
                        }

                        val freeTierNote = when {
                            isGroq -> "Free tier available via Groq Developer Console"
                            isOllama -> "Free self-hosted local model"
                            isOpenAi -> "Paid API (OpenAI platform credits required)"
                            isXAi -> "Paid API (xAI console credits required)"
                            isDeepSeek -> "Paid API (DeepSeek pay-as-you-go credits required)"
                            else -> "Paid / Custom provider endpoint"
                        }

                        val speedCategory = if (isFast) "LIGHTNING" else "FAST"

                        discovered.add(
                            DiscoveredModel(
                                modelId = modelId,
                                displayName = modelId.replace("-", " ").replace("_", " ").split(" ").joinToString(" ") { word ->
                                    word.replaceFirstChar { char -> if (char.isLowerCase()) char.titlecase() else char.toString() }
                                },
                                description = "Discovered chat model from $baseUrl",
                                version = "",
                                freeTier = hasFreeTier,
                                freeTierNote = freeTierNote,
                                inputCapabilities = if (isVision) "text,image" else "text",
                                outputCapabilities = "text",
                                supportsVision = isVision,
                                supportsImageGeneration = false,
                                supportsAudio = false,
                                supportsStreaming = true,
                                supportsRealtime = false,
                                contextWindow = 0,
                                speedCategory = speedCategory
                            )
                        )
                    }

                    if (discovered.isEmpty()) {
                        return@withContext Result.failure(Exception("No compatible chat models discovered from $baseUrl."))
                    }

                    // Show ONLY the latest 4 eligible/accessible chat models
                    val latest4 = discovered.sortedWith(
                        compareByDescending<DiscoveredModel> { it.modelId.contains("4o") }
                            .thenByDescending { it.modelId.contains("grok-2") }
                            .thenByDescending { it.modelId.contains("llama-3.3") }
                            .thenByDescending { it.modelId.contains("llama-3.1") }
                            .thenByDescending { it.modelId.contains("deepseek-chat") }
                            .thenByDescending { it.modelId.contains("o3") }
                            .thenByDescending { it.modelId.contains("o1") }
                    ).take(4)

                    Result.success(latest4)
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to discover models from $baseUrl: ${e.localizedMessage ?: e.message}"))
            }
        }

    override suspend fun verifyModel(
        config: ApiConfigEntity,
        modelId: String,
        capability: String
    ): Result<ModelVerificationResult> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        val rawBase = config.baseUrl.trim().ifEmpty { "https://api.openai.com/v1" }
        val baseUrl = rawBase.trimEnd('/')
        val cleanModel = modelId.trim()

        if (cleanModel.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Model ID cannot be empty."))
        }

        val startTime = System.currentTimeMillis()

        try {
            val payload = JSONObject()
            payload.put("model", cleanModel)
            val messagesArr = JSONArray()
            messagesArr.put(JSONObject().put("role", "user").put("content", "ping"))
            payload.put("messages", messagesArr)
            payload.put("max_tokens", 1)

            val reqBuilder = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(payload.toString().toRequestBody(jsonMediaType))

            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            if (!config.organizationId.isNullOrBlank()) {
                reqBuilder.addHeader("OpenAI-Organization", config.organizationId)
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()

                if (response.code == 404 || body.contains("model_not_found", ignoreCase = true)) {
                    return@withContext Result.success(
                        ModelVerificationResult(
                            verified = false,
                            latencyMs = latency,
                            message = "Model '$cleanModel' was not found (404) on this endpoint.",
                            statusCode = 404,
                            is404 = true
                        )
                    )
                }

                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.success(
                        ModelVerificationResult(
                            verified = false,
                            latencyMs = latency,
                            message = errMsg,
                            statusCode = response.code,
                            is404 = response.code == 404
                        )
                    )
                }

                Result.success(
                    ModelVerificationResult(
                        verified = true,
                        latencyMs = latency,
                        message = "Model '$cleanModel' verified successfully ($latency ms).",
                        statusCode = 200,
                        is404 = false
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Verification failed for $cleanModel: ${e.localizedMessage ?: e.message}"))
        }
    }

    override suspend fun testConnection(config: ApiConfigEntity): Result<ConnectionTestResult> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val apiKey = config.apiKey.trim()
            val rawBase = config.baseUrl.trim().ifEmpty { "https://api.openai.com/v1" }
            val baseUrl = rawBase.trimEnd('/')

            // 1. Discover models
            val modelsResult = listModels(config)
            if (modelsResult.isFailure) {
                val err = modelsResult.exceptionOrNull()
                return@withContext Result.failure(err ?: Exception("Connection failed to $baseUrl"))
            }

            val models = modelsResult.getOrNull().orEmpty()
            val modelNames = models.map { it.modelId }
            val latency = System.currentTimeMillis() - startTime

            // 2. If a specific model is targeted, verify it
            val targetModel = config.modelName.trim()
            if (targetModel.isNotBlank()) {
                val verifyRes = verifyModel(config, targetModel)
                if (verifyRes.isSuccess) {
                    val v = verifyRes.getOrNull()!!
                    if (!v.verified) {
                        return@withContext Result.success(
                            ConnectionTestResult(
                                success = false,
                                latencyMs = v.latencyMs,
                                message = if (v.is404) "Model '$targetModel' not found (404) on endpoint." else v.message,
                                availableModels = modelNames,
                                discoveredModelsCount = models.size
                            )
                        )
                    }
                }
            }

            Result.success(
                ConnectionTestResult(
                    success = true,
                    latencyMs = latency,
                    message = "Connected to $baseUrl. ${models.size} models discovered.",
                    availableModels = modelNames,
                    discoveredModelsCount = models.size
                )
            )
        }

    override suspend fun generateChat(
        config: ApiConfigEntity,
        messages: List<ChatMessageEntity>,
        memoryPrompt: String?,
        onChunk: ((String) -> Unit)?
    ): Result<ChatResponse> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        val rawBase = config.baseUrl.trim().ifEmpty { "https://api.openai.com/v1" }
        val baseUrl = rawBase.trimEnd('/')
        val model = config.modelName.trim()

        if (model.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No model configured. Please select a model in Central API Hub."))
        }

        try {
            val payload = JSONObject()
            payload.put("model", model)

            val messagesArr = JSONArray()

            val systemContent = StringBuilder(com.example.data.NovaIdentity.SYSTEM_INSTRUCTION)
            if (!memoryPrompt.isNullOrBlank()) {
                systemContent.append("\n\nUser's Long-Term Preferences & Memory:\n").append(memoryPrompt)
            }
            messagesArr.put(JSONObject().put("role", "system").put("content", systemContent.toString()))

            val relevant = messages.takeLast(12)
            for (msg in relevant) {
                if (msg.role == "error" || msg.role == "system") continue
                val role = if (msg.role == "assistant") "assistant" else "user"

                if (msg.role == "user" && !msg.mediaUri.isNullOrBlank() && msg.mediaType?.startsWith("image") == true) {
                    val contentParts = JSONArray()
                    contentParts.put(JSONObject().put("type", "text").put("text", msg.content))
                    val imgObj = JSONObject().put("url", "data:image/jpeg;base64,${msg.mediaUri.substringAfter("base64,")}")
                    contentParts.put(JSONObject().put("type", "image_url").put("image_url", imgObj))
                    messagesArr.put(JSONObject().put("role", role).put("content", contentParts))
                } else {
                    messagesArr.put(JSONObject().put("role", role).put("content", msg.content))
                }
            }

            payload.put("messages", messagesArr)
            payload.put("temperature", 0.7)

            val reqBuilder = Request.Builder()
                .url("$baseUrl/chat/completions")
                .post(payload.toString().toRequestBody(jsonMediaType))

            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }
            if (!config.organizationId.isNullOrBlank()) {
                reqBuilder.addHeader("OpenAI-Organization", config.organizationId)
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 404 || body.contains("model_not_found", ignoreCase = true)) {
                    return@withContext Result.failure(Exception("[404_MODEL_NOT_FOUND] Model '$model' does not exist or is deprecated."))
                }

                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(body)
                val choices = json.optJSONArray("choices") ?: JSONArray()
                if (choices.length() == 0) {
                    return@withContext Result.failure(Exception("Empty response from AI provider."))
                }

                val messageObj = choices.getJSONObject(0).optJSONObject("message")
                val text = messageObj?.optString("content").orEmpty().ifBlank { "I received your message." }
                val cleanSpeech = SpeechCleaner.cleanForSpeech(text)

                onChunk?.invoke(text)

                Result.success(
                    ChatResponse(
                        text = text,
                        spokenText = cleanSpeech,
                        modelUsed = model
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Chat request failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    override suspend fun generateImage(
        config: ApiConfigEntity,
        prompt: String,
        aspectRatio: String,
        size: String
    ): Result<ImageResult> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        val rawBase = config.baseUrl.trim().ifEmpty { "https://api.openai.com/v1" }
        val baseUrl = rawBase.trimEnd('/')
        val model = config.modelName.trim().ifBlank { "dall-e-3" }

        try {
            val payload = JSONObject()
            payload.put("prompt", prompt)
            payload.put("model", model)
            payload.put("n", 1)
            payload.put("size", "1024x1024")
            payload.put("response_format", "b64_json")

            val reqBuilder = Request.Builder()
                .url("$baseUrl/images/generations")
                .post(payload.toString().toRequestBody(jsonMediaType))

            if (apiKey.isNotBlank()) {
                reqBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            client.newCall(reqBuilder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 404 || body.contains("model_not_found", ignoreCase = true)) {
                    return@withContext Result.failure(Exception("[404_MODEL_NOT_FOUND] Image model '$model' was not found (404)."))
                }
                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: JSONArray()
                if (data.length() == 0) {
                    return@withContext Result.failure(Exception("No image returned by provider."))
                }

                val first = data.getJSONObject(0)
                val b64 = first.optString("b64_json")
                val url = first.optString("url")

                if (b64.isNotBlank()) {
                    Result.success(ImageResult(base64Data = b64, mimeType = "image/png"))
                } else if (url.isNotBlank()) {
                    Result.success(ImageResult(imageUrl = url, mimeType = "image/png"))
                } else {
                    Result.failure(Exception("Image data was missing from provider response."))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Image generation failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    private fun parseError(body: String, statusCode: Int): String {
        return try {
            val json = JSONObject(body)
            val errorObj = json.optJSONObject("error")
            val message = errorObj?.optString("message") ?: body
            when (statusCode) {
                401 -> "Invalid or expired API key. Please check your credentials in the API Hub."
                404 -> "Model unavailable or endpoint not found ($statusCode): $message"
                429 -> "Rate limit reached (429): Quota exceeded or too many requests."
                500, 502, 503 -> "Server error ($statusCode): The provider service encountered an error."
                else -> "Provider error ($statusCode): $message"
            }
        } catch (e: Exception) {
            "API error ($statusCode): ${body.take(200)}"
        }
    }
}
