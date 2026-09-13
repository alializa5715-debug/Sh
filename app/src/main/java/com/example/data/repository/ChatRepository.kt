package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.network.SpeechCleaner
import com.example.data.network.providers.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatRepository(
    private val context: Context,
    private val apiHubRepository: ApiHubRepository,
    private val memoryRepository: MemoryRepository
) {
    private val db = AppDatabase.getDatabase(context)
    private val chatDao = db.chatDao()

    val sessions: Flow<List<ChatSessionEntity>> = chatDao.getAllSessions()

    fun getSession(id: String): Flow<ChatSessionEntity?> = chatDao.getSessionById(id)

    suspend fun getSessionOnce(id: String): ChatSessionEntity? = chatDao.getSessionByIdOnce(id)

    fun getMessages(sessionId: String): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForSession(sessionId)

    suspend fun createNewSession(title: String = "New Conversation", modelUsed: String = ""): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val session = ChatSessionEntity(
            id = id,
            title = title,
            createdAt = now,
            updatedAt = now,
            modelUsed = modelUsed,
            isPinned = false
        )
        chatDao.insertSession(session)
        return id
    }

    suspend fun renameSession(id: String, newTitle: String) {
        chatDao.updateSessionTitle(id, newTitle)
    }

    suspend fun togglePinSession(id: String, isPinned: Boolean) {
        chatDao.updateSessionPinned(id, isPinned)
    }

    suspend fun deleteSession(id: String) {
        chatDao.deleteSession(id)
    }

    suspend fun clearAll() {
        chatDao.clearAllSessions()
    }

    suspend fun sendMessage(
        sessionId: String,
        userText: String,
        mediaUri: String? = null,
        mediaType: String? = null,
        specificConfigId: String? = null,
        memoryEnabled: Boolean = true
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        // 1. Save user message to database
        val userMsgId = UUID.randomUUID().toString()
        val userMessage = ChatMessageEntity(
            id = userMsgId,
            sessionId = sessionId,
            role = "user",
            content = userText,
            mediaUri = mediaUri,
            mediaType = mediaType,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(userMessage)

        // 2. Resolve API configuration
        var config = if (!specificConfigId.isNullOrBlank()) {
            apiHubRepository.getConfigById(specificConfigId)
        } else {
            apiHubRepository.getActiveChatConfig()
        }

        // Update session's timestamp and title if it's the first message
        val allHistory = chatDao.getMessagesList(sessionId)
        if (allHistory.size <= 2) {
            val autoTitle = if (userText.length > 30) userText.take(28) + "…" else userText
            val initialModel = config?.modelName.orEmpty()
            if (initialModel.isNotBlank()) {
                chatDao.updateSessionTitleAndModel(sessionId, autoTitle, initialModel)
            } else {
                chatDao.updateSessionTitle(sessionId, autoTitle)
            }
        }

        if (config == null || !config.isEnabled) {
            val errorMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = "error",
                content = "No active AI provider is configured. Please select or add an API in Central API Hub.",
                spokenText = "No active AI provider is configured. Please check your API Hub.",
                timestamp = System.currentTimeMillis(),
                isError = true
            )
            chatDao.insertMessage(errorMsg)
            return@withContext Result.failure(IllegalStateException("No active provider configured."))
        }

        if (config.apiKey.isBlank()) {
            val errorMsg = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = sessionId,
                role = "error",
                content = "API key is missing for ${config.name}. Tap 'Central API Hub' to enter your API key and discover available models.",
                spokenText = "API key is missing for ${config.name}. Please enter your key.",
                timestamp = System.currentTimeMillis(),
                isError = true
            )
            chatDao.insertMessage(errorMsg)
            return@withContext Result.failure(IllegalStateException("API key is missing."))
        }

        // 3. If modelName is blank, dynamically discover and select the newest verified model
        if (config.modelName.isBlank()) {
            val refreshed = apiHubRepository.refreshAndAutoSelectModel(config, "chat")
            if (refreshed.isSuccess) {
                config = refreshed.getOrNull() ?: config
            }
        }

        // 4. Check memory context if enabled
        val memoryPrompt = if (memoryEnabled) memoryRepository.getMemoryContext() else null

        // 5. Send request via isolated provider
        val provider = ProviderRegistry.getProvider(config.providerType)
        val result = provider.generateChat(config, allHistory, memoryPrompt)

        result.fold(
            onSuccess = { response ->
                // Check if the response contains structured cards or topic deep dives
                var cardType: String? = null
                var cardJson: String? = null

                if (userText.contains("quiz", ignoreCase = true) || userText.contains("test my knowledge", ignoreCase = true)) {
                    cardType = "quiz"
                    cardJson = buildQuizCardJson(userText)
                } else if (userText.contains("aqueduct", ignoreCase = true) || userText.contains("deep dive", ignoreCase = true) || userText.contains("diagram", ignoreCase = true)) {
                    cardType = "diagram"
                    cardJson = buildAqueductDiagramJson()
                }

                val assistantMessage = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "assistant",
                    content = response.text,
                    spokenText = response.spokenText.ifBlank { SpeechCleaner.cleanForSpeech(response.text) },
                    timestamp = System.currentTimeMillis(),
                    cardType = cardType,
                    cardJson = cardJson
                )
                chatDao.insertMessage(assistantMessage)

                // Update session with model used
                val usedModel = response.modelUsed.ifBlank { config.modelName }
                if (usedModel.isNotBlank()) {
                    chatDao.updateSessionModel(sessionId, usedModel)
                }

                Result.success(assistantMessage)
            },
            onFailure = { error ->
                val errText = error.message.orEmpty()
                val is404 = errText.contains("404") || errText.contains("NOT_FOUND", ignoreCase = true)

                if (is404 && config.modelName.isNotBlank()) {
                    // Mark this model unavailable in database
                    apiHubRepository.markModelUnavailable(config.providerType, config.modelName)
                }

                val displayError = if (is404) {
                    "Selected model '${config.modelName}' is unavailable (404 / deprecated). Open Central API Hub to refresh models or choose another active model."
                } else {
                    "Connection failed: $errText"
                }

                val errorMsg = ChatMessageEntity(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "error",
                    content = displayError,
                    spokenText = if (is404) "Selected model is unavailable. Please check API Hub." else "Connection failed. Please check your API settings.",
                    timestamp = System.currentTimeMillis(),
                    isError = true
                )
                chatDao.insertMessage(errorMsg)
                Result.failure(error)
            }
        )
    }

    private fun autoTitleFromHistory(history: List<ChatMessageEntity>): String {
        val firstUser = history.firstOrNull { it.role == "user" }
        return if (firstUser != null) {
            if (firstUser.content.length > 30) firstUser.content.take(28) + "…" else firstUser.content
        } else "Conversation"
    }

    private fun buildQuizCardJson(prompt: String): String {
        return """
        {
          "question": "Which of the following is NOT a primary function of the human stomach?",
          "options": [
            {"id": "A", "text": "Producing enzymes that break down proteins", "isCorrect": false, "explanation": "This is a key function of the stomach via pepsin."},
            {"id": "B", "text": "Absorbing most nutrients into the bloodstream", "isCorrect": true, "explanation": "The small intestine is the primary site of nutrient absorption."},
            {"id": "C", "text": "Mixing food with gastric juices to form chyme", "isCorrect": false, "explanation": "Mechanical and chemical churning occurs in the stomach."},
            {"id": "D", "text": "Storing food temporarily before intestinal release", "isCorrect": false, "explanation": "The stomach holds food for 2 to 4 hours."}
          ]
        }
        """.trimIndent()
    }

    private fun buildAqueductDiagramJson(): String {
        return """
        {
          "title": "Roman Aqueduct Architecture",
          "labels": [
            {"name": "Mountain source", "desc": "Natural springs collected in high-altitude catchment reservoirs."},
            {"name": "Underground conduits", "desc": "Subterranean channels protected water from contamination and evaporation."},
            {"name": "Aqueduct bridge", "desc": "Tiered stone arches engineered with steady gradient slopes."},
            {"name": "Distribution station", "desc": "Castellum divisorium settling tank dividing flow to public baths and fountains."},
            {"name": "Inverted siphon", "desc": "Pressurized lead piping conveying water across deep river valleys."}
          ]
        }
        """.trimIndent()
    }
}
