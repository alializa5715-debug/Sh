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

class GeminiProvider : AiProvider {

    private val client = HttpClientFactory.okHttpClient
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun listModels(config: ApiConfigEntity): Result<List<DiscoveredModel>> =
        withContext(Dispatchers.IO) {
            val apiKey = config.apiKey.trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty. Configure it in API Hub."))
            }

            val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trimEnd('/') else "https://generativelanguage.googleapis.com"
            val url = "$baseUrl/v1beta/models?key=$apiKey"

            try {
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception(parseError(body, response.code)))
                    }

                    val json = JSONObject(body)
                    val modelsArray = json.optJSONArray("models") ?: JSONArray()
                    val discovered = mutableListOf<DiscoveredModel>()

                    for (i in 0 until modelsArray.length()) {
                        val m = modelsArray.getJSONObject(i)
                        val rawName = m.optString("name")
                        val modelId = rawName.removePrefix("models/")
                        val displayName = m.optString("displayName").ifBlank { modelId }
                        val description = m.optString("description")
                        val version = m.optString("version")
                        val inputTokenLimit = m.optInt("inputTokenLimit", 0)

                        // Check supported methods
                        val methodsArr = m.optJSONArray("supportedGenerationMethods")
                        val methods = mutableListOf<String>()
                        if (methodsArr != null) {
                            for (j in 0 until methodsArr.length()) {
                                methods.add(methodsArr.getString(j))
                            }
                        }

                        // Filter for models supporting content generation
                        val supportsGenerate = methods.contains("generateContent")
                        if (!supportsGenerate) continue

                        val lowerId = modelId.lowercase()
                        val isImageGen = lowerId.contains("image") || lowerId.contains("imagen")
                        if (isImageGen) continue

                        val isFlash = lowerId.contains("flash") || lowerId.contains("lite")
                        val isPro = lowerId.contains("pro")
                        val isEmbedding = lowerId.contains("embedding") || lowerId.contains("embed")
                        if (isEmbedding) continue

                        // Skip older deprecated generations
                        if (lowerId.contains("gemini-1.0") || lowerId.contains("bison")) continue

                        val speedCategory = when {
                            isFlash -> "LIGHTNING"
                            isPro -> "REASONING"
                            else -> "FAST"
                        }

                        // Google AI Studio models have generous free tier allowances
                        val hasFreeTier = true
                        val freeTierNote = "Free tier available in Google AI Studio"

                        val inputCaps = if (isImageGen) "text" else "text,image,audio"
                        val outputCaps = if (isImageGen) "image" else "text"

                        discovered.add(
                            DiscoveredModel(
                                modelId = modelId,
                                displayName = displayName,
                                description = description,
                                version = version,
                                freeTier = hasFreeTier,
                                freeTierNote = freeTierNote,
                                inputCapabilities = inputCaps,
                                outputCapabilities = outputCaps,
                                supportsVision = !isImageGen,
                                supportsImageGeneration = isImageGen,
                                supportsAudio = !isImageGen && (isFlash || isPro),
                                supportsStreaming = !isImageGen,
                                supportsRealtime = lowerId.contains("live") || lowerId.contains("realtime"),
                                contextWindow = inputTokenLimit,
                                speedCategory = speedCategory
                            )
                        )
                    }

                    if (discovered.isEmpty()) {
                        return@withContext Result.failure(Exception("No compatible chat models discovered from Google AI Studio."))
                    }

                    // Show ONLY the latest 4 eligible/accessible chat models
                    val latest4 = discovered.sortedWith(
                        compareByDescending<DiscoveredModel> { it.modelId.contains("2.5") }
                            .thenByDescending { it.modelId.contains("2.0") }
                            .thenByDescending { it.modelId.contains("flash") }
                            .thenByDescending { it.modelId.contains("1.5") }
                    ).take(4)

                    Result.success(latest4)
                }
            } catch (e: Exception) {
                Result.failure(Exception("Failed to discover models from Google AI Studio: ${e.localizedMessage ?: e.message}"))
            }
        }

    override suspend fun verifyModel(
        config: ApiConfigEntity,
        modelId: String,
        capability: String
    ): Result<ModelVerificationResult> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty."))
        }

        val cleanModel = modelId.trim().removePrefix("models/")
        if (cleanModel.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Model ID is empty."))
        }

        val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trimEnd('/') else "https://generativelanguage.googleapis.com"
        val url = "$baseUrl/v1beta/models/$cleanModel:generateContent?key=$apiKey"
        val startTime = System.currentTimeMillis()

        try {
            val payload = JSONObject()
            val contentsArr = JSONArray()
            val contentObj = JSONObject().put("role", "user")
            val partsArr = JSONArray().put(JSONObject().put("text", "ping"))
            contentObj.put("parts", partsArr)
            contentsArr.put(contentObj)
            payload.put("contents", contentsArr)

            val genConfig = JSONObject().put("maxOutputTokens", 2)
            payload.put("generationConfig", genConfig)

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val body = response.body?.string().orEmpty()

                if (response.code == 404 || body.contains("NOT_FOUND", ignoreCase = true)) {
                    return@withContext Result.success(
                        ModelVerificationResult(
                            verified = false,
                            latencyMs = latency,
                            message = "Model '$cleanModel' was not found (404) or is deprecated.",
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
            Result.failure(Exception("Verification request failed for $cleanModel: ${e.localizedMessage ?: e.message}"))
        }
    }

    override suspend fun testConnection(config: ApiConfigEntity): Result<ConnectionTestResult> =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            val apiKey = config.apiKey.trim()
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Gemini API key is empty. Please enter your API key."))
            }

            // 1. Discover actual models
            val modelsResult = listModels(config)
            if (modelsResult.isFailure) {
                val err = modelsResult.exceptionOrNull()
                return@withContext Result.failure(err ?: Exception("Failed to list models from Gemini."))
            }

            val models = modelsResult.getOrNull().orEmpty()
            val modelNames = models.map { it.modelId }
            val latency = System.currentTimeMillis() - startTime

            // 2. If a specific model is targeted in config, verify it specifically
            val targetModel = config.modelName.trim().removePrefix("models/")
            if (targetModel.isNotBlank()) {
                val verifyRes = verifyModel(config, targetModel)
                if (verifyRes.isSuccess) {
                    val v = verifyRes.getOrNull()!!
                    if (!v.verified) {
                        return@withContext Result.success(
                            ConnectionTestResult(
                                success = false,
                                latencyMs = v.latencyMs,
                                message = if (v.is404) "Selected model '$targetModel' not found (404). Please choose an available model." else v.message,
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
                    message = "Connected to Google AI Studio. ${models.size} models discovered.",
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
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is missing. Please configure it in API Hub."))
        }

        // Use dynamically configured model without hardcoded defaults
        val rawModel = config.modelName.trim().removePrefix("models/")
        if (rawModel.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("No model selected. Please select a model in Central API Hub."))
        }

        val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trimEnd('/') else "https://generativelanguage.googleapis.com"
        val url = "$baseUrl/v1beta/models/$rawModel:generateContent?key=$apiKey"

        try {
            val payload = JSONObject()

            // System instructions with Nova identity
            val systemParts = mutableListOf<String>()
            systemParts.add(com.example.data.NovaIdentity.SYSTEM_INSTRUCTION)
            if (!memoryPrompt.isNullOrBlank()) {
                systemParts.add("User's Long-Term Preferences & Memory:\n$memoryPrompt")
            }
            val sysInstructionObj = JSONObject()
            val sysPartsArr = JSONArray()
            sysPartsArr.put(JSONObject().put("text", systemParts.joinToString("\n\n")))
            sysInstructionObj.put("parts", sysPartsArr)
            payload.put("systemInstruction", sysInstructionObj)

            // History
            val contentsArr = JSONArray()
            val relevantMessages = messages.takeLast(12)
            for (msg in relevantMessages) {
                if (msg.role == "error" || msg.role == "system") continue
                val role = if (msg.role == "user") "user" else "model"
                val contentObj = JSONObject().put("role", role)
                val partsArr = JSONArray()

                if (msg.role == "user" && !msg.mediaUri.isNullOrBlank() && msg.mediaType?.startsWith("image") == true) {
                    val base64Data = msg.mediaUri.substringAfter("base64,", msg.mediaUri)
                    val mime = if (msg.mediaUri.contains("image/png")) "image/png" else "image/jpeg"
                    val inlineDataObj = JSONObject()
                        .put("mimeType", mime)
                        .put("data", base64Data)
                    partsArr.put(JSONObject().put("inlineData", inlineDataObj))
                }

                partsArr.put(JSONObject().put("text", msg.content))
                contentObj.put("parts", partsArr)
                contentsArr.put(contentObj)
            }
            payload.put("contents", contentsArr)

            val genConfig = JSONObject()
                .put("temperature", 0.7)
                .put("topP", 0.95)
            payload.put("generationConfig", genConfig)

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 404 || body.contains("NOT_FOUND", ignoreCase = true)) {
                    return@withContext Result.failure(Exception("[404_MODEL_NOT_FOUND] Model '$rawModel' does not exist or is deprecated."))
                }

                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext Result.failure(Exception("No response generated by model. The content may have triggered safety filters."))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val contentObj = firstCandidate.optJSONObject("content")
                val parts = contentObj?.optJSONArray("parts")
                val textBuilder = StringBuilder()
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val text = part.optString("text")
                        if (text.isNotEmpty()) {
                            textBuilder.append(text)
                            onChunk?.invoke(text)
                        }
                    }
                }

                val fullText = textBuilder.toString().ifBlank { "I received your message." }
                val cleanSpeech = SpeechCleaner.cleanForSpeech(fullText)

                Result.success(
                    ChatResponse(
                        text = fullText,
                        spokenText = cleanSpeech,
                        modelUsed = rawModel
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gemini generation failed: ${e.localizedMessage ?: e.message}"))
        }
    }

    override suspend fun generateImage(
        config: ApiConfigEntity,
        prompt: String,
        aspectRatio: String,
        size: String
    ): Result<ImageResult> = withContext(Dispatchers.IO) {
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is not configured for image generation."))
        }

        val rawModel = config.modelName.trim().removePrefix("models/")
        if (rawModel.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Image model is not configured."))
        }

        val baseUrl = if (config.baseUrl.isNotBlank()) config.baseUrl.trimEnd('/') else "https://generativelanguage.googleapis.com"
        val url = "$baseUrl/v1beta/models/$rawModel:generateContent?key=$apiKey"

        try {
            val payload = JSONObject()
            val contentsArr = JSONArray()
            val contentObj = JSONObject()
            val partsArr = JSONArray().put(JSONObject().put("text", prompt))
            contentObj.put("parts", partsArr)
            contentsArr.put(contentObj)
            payload.put("contents", contentsArr)

            val modalities = JSONArray().put("IMAGE").put("TEXT")
            val imageConfig = JSONObject()
                .put("aspectRatio", if (aspectRatio.isNotBlank()) aspectRatio else "1:1")
                .put("imageSize", if (size.isNotBlank()) size else "1K")

            val genConfig = JSONObject()
                .put("responseModalities", modalities)
                .put("imageConfig", imageConfig)
            payload.put("generationConfig", genConfig)

            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.code == 404 || body.contains("NOT_FOUND", ignoreCase = true)) {
                    return@withContext Result.failure(Exception("[404_MODEL_NOT_FOUND] Image model '$rawModel' was not found (404)."))
                }
                if (!response.isSuccessful) {
                    val errMsg = parseError(body, response.code)
                    return@withContext Result.failure(Exception(errMsg))
                }

                val json = JSONObject(body)
                val candidates = json.optJSONArray("candidates") ?: JSONArray()
                if (candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Image generation returned no candidates."))
                }

                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts") ?: JSONArray()

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val mime = inlineData.optString("mimeType", "image/png")
                        val data = inlineData.optString("data")
                        if (data.isNotBlank()) {
                            return@withContext Result.success(
                                ImageResult(
                                    base64Data = data,
                                    mimeType = mime
                                )
                            )
                        }
                    }
                }

                Result.failure(Exception("Model did not return image data in the response."))
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
                400 -> "Bad Request: $message"
                401, 403 -> "Authentication failed ($statusCode): Invalid or unauthorized API key."
                404 -> "Model not found (404): The requested model does not exist or is deprecated."
                429 -> "Rate limit reached (429): Quota exhausted or rate limit hit."
                500, 503 -> "Server error ($statusCode): Google AI Studio temporarily unavailable."
                else -> "Error ($statusCode): $message"
            }
        } catch (e: Exception) {
            "API error ($statusCode): ${body.take(200)}"
        }
    }
}
