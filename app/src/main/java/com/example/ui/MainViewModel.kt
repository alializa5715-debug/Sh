package com.example.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ApiConfigEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.DiscoveredModelEntity
import com.example.data.local.entity.MemoryEntity
import com.example.service.NetworkMonitor
import com.example.service.SpeechService
import com.example.data.network.providers.ConnectionTestResult
import com.example.data.network.providers.ImageResult
import com.example.data.network.providers.ModelVerificationResult
import com.example.data.network.providers.ProviderRegistry
import com.example.data.repository.ApiHubRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.MemoryRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

enum class AppTab {
    CHAT,
    LIVE_VOICE,
    HISTORY,
    SETTINGS,
    API_HUB
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val apiHubRepository = ApiHubRepository(application)
    val memoryRepository = MemoryRepository(application)
    val chatRepository = ChatRepository(application, apiHubRepository, memoryRepository)
    val speechService = SpeechService(application)
    private val networkMonitor = NetworkMonitor(application)

    private val prefs: SharedPreferences = application.getSharedPreferences("nova_conversation_prefs", Context.MODE_PRIVATE)
    private val KEY_ACTIVE_SESSION_ID = "active_session_id"
    private var messageCollectorJob: Job? = null

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val allConfigs: StateFlow<List<ApiConfigEntity>> = apiHubRepository.allConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val enabledConfigs: StateFlow<List<ApiConfigEntity>> = apiHubRepository.enabledConfigs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<ChatSessionEntity>> = chatRepository.sessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = memoryRepository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDiscoveredModels: StateFlow<List<DiscoveredModelEntity>> = apiHubRepository.modelRegistry.getAllModels()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(AppTab.CHAT)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _currentMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val currentMessages: StateFlow<List<ChatMessageEntity>> = _currentMessages.asStateFlow()

    private val _selectedModelConfig = MutableStateFlow<ApiConfigEntity?>(null)
    val selectedModelConfig: StateFlow<ApiConfigEntity?> = _selectedModelConfig.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isLiveVoiceActive = MutableStateFlow(false)
    val isLiveVoiceActive: StateFlow<Boolean> = _isLiveVoiceActive.asStateFlow()

    private val _isSpeechCleanerEnabled = MutableStateFlow(true)
    val isSpeechCleanerEnabled: StateFlow<Boolean> = _isSpeechCleanerEnabled.asStateFlow()

    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    private val _isDiscoveringModels = MutableStateFlow(false)
    val isDiscoveringModels: StateFlow<Boolean> = _isDiscoveringModels.asStateFlow()

    // Testing / Diagnostics State
    private val _testingConfigId = MutableStateFlow<String?>(null)
    val testingConfigId: StateFlow<String?> = _testingConfigId.asStateFlow()

    private val _testResult = MutableStateFlow<ConnectionTestResult?>(null)
    val testResult: StateFlow<ConnectionTestResult?> = _testResult.asStateFlow()

    private val _modelVerificationResult = MutableStateFlow<ModelVerificationResult?>(null)
    val modelVerificationResult: StateFlow<ModelVerificationResult?> = _modelVerificationResult.asStateFlow()

    // Attachment State
    private val _attachedImageBase64 = MutableStateFlow<String?>(null)
    val attachedImageBase64: StateFlow<String?> = _attachedImageBase64.asStateFlow()

    private val _attachedFileName = MutableStateFlow<String?>(null)
    val attachedFileName: StateFlow<String?> = _attachedFileName.asStateFlow()

    init {
        viewModelScope.launch {
            apiHubRepository.initializeDefaultsIfNeeded()
            val defaultCfg = apiHubRepository.getActiveChatConfig()
            _selectedModelConfig.value = defaultCfg

            // App Restart: restore the exact active conversation from preferences if valid
            val savedSessionId = prefs.getString(KEY_ACTIVE_SESSION_ID, null)
            val sessionExists = if (!savedSessionId.isNullOrBlank()) {
                chatRepository.getSessionOnce(savedSessionId) != null
            } else false

            if (sessionExists && !savedSessionId.isNullOrBlank()) {
                selectSession(savedSessionId)
            } else {
                val existingSessions = chatRepository.sessions.firstOrNull()
                if (!existingSessions.isNullOrEmpty()) {
                    selectSession(existingSessions.first().id)
                } else {
                    createNewChat()
                }
            }
        }
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun selectSession(sessionId: String) {
        if (sessionId.isBlank()) return

        // 1. Immediately cancel any existing message collection to prevent cross-conversation leaks
        messageCollectorJob?.cancel()

        // 2. Clear current messages immediately so stale conversation data never lingers or leaks
        _currentMessages.value = emptyList()
        _currentSessionId.value = sessionId

        // 3. Persist active conversation ID for seamless app restart restoration
        prefs.edit().putString(KEY_ACTIVE_SESSION_ID, sessionId).apply()

        // 4. Launch dedicated collector for this specific sessionId only
        messageCollectorJob = viewModelScope.launch {
            chatRepository.getMessages(sessionId).collect { msgs ->
                if (_currentSessionId.value == sessionId) {
                    _currentMessages.value = msgs
                }
            }
        }

        // 5. Restore conversation-specific model configuration if recorded
        viewModelScope.launch {
            val session = chatRepository.getSessionOnce(sessionId)
            if (session != null && session.modelUsed.isNotBlank()) {
                val matching = enabledConfigs.value.firstOrNull { it.modelName == session.modelUsed }
                if (matching != null && _selectedModelConfig.value?.modelName != session.modelUsed) {
                    _selectedModelConfig.value = matching
                }
            }
        }
    }

    fun createNewChat() {
        // 1. Cancel active message collection immediately
        messageCollectorJob?.cancel()
        // 2. Clear current messages immediately
        _currentMessages.value = emptyList()
        _currentSessionId.value = null

        viewModelScope.launch {
            val modelName = _selectedModelConfig.value?.modelName.orEmpty()
            val newId = chatRepository.createNewSession("New Conversation", modelName)
            selectSession(newId)
            _currentTab.value = AppTab.CHAT
        }
    }

    fun renameSession(sessionId: String, title: String) {
        viewModelScope.launch {
            chatRepository.renameSession(sessionId, title)
        }
    }

    fun togglePinSession(sessionId: String, isPinned: Boolean) {
        viewModelScope.launch {
            chatRepository.togglePinSession(sessionId, isPinned)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                messageCollectorJob?.cancel()
                _currentMessages.value = emptyList()
                val remaining = chatRepository.sessions.firstOrNull()?.filter { it.id != sessionId }.orEmpty()
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    createNewChat()
                }
            }
        }
    }

    fun clearAllSessions() {
        viewModelScope.launch {
            messageCollectorJob?.cancel()
            _currentMessages.value = emptyList()
            chatRepository.clearAll()
            createNewChat()
        }
    }

    fun setSelectedConfig(config: ApiConfigEntity) {
        _selectedModelConfig.value = config
        viewModelScope.launch {
            apiHubRepository.setDefault(config.id)
            // If model is blank, dynamically discover and select
            if (config.modelName.isBlank() && config.apiKey.isNotBlank()) {
                refreshModelsForConfig(config)
            }
        }
    }

    fun selectModelForActiveConfig(modelId: String) {
        val current = _selectedModelConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(
                modelName = modelId,
                status = "CONNECTED"
            )
            apiHubRepository.saveConfig(updated, performTestFirst = false)
            _selectedModelConfig.value = updated

            // Verify the newly chosen model in background
            apiHubRepository.modelRegistry.verifySingleModel(updated, modelId, "chat")
        }
    }

    fun refreshModelsForConfig(config: ApiConfigEntity) {
        _isDiscoveringModels.value = true
        viewModelScope.launch {
            val res = apiHubRepository.refreshAndAutoSelectModel(config, capability = if (config.category.contains("Image")) "image_gen" else "chat")
            res.onSuccess { updated ->
                if (_selectedModelConfig.value?.id == config.id) {
                    _selectedModelConfig.value = updated
                }
            }
            _isDiscoveringModels.value = false
        }
    }

    fun verifySpecificModel(config: ApiConfigEntity, modelId: String) {
        viewModelScope.launch {
            val res = apiHubRepository.modelRegistry.verifySingleModel(config, modelId, capability = "chat")
            _modelVerificationResult.value = res.getOrNull()
        }
    }

    fun attachImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                        if (ratio > 1) {
                            Bitmap.createScaledBitmap(bitmap, 1024, (1024 / ratio).toInt(), true)
                        } else {
                            Bitmap.createScaledBitmap(bitmap, (1024 * ratio).toInt(), 1024, true)
                        }
                    } else bitmap

                    val outputStream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val bytes = outputStream.toByteArray()
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    _attachedImageBase64.value = b64
                    _attachedFileName.value = "Image attached"
                }
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    fun clearAttachment() {
        _attachedImageBase64.value = null
        _attachedFileName.value = null
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        val mediaB64 = _attachedImageBase64.value
        if (trimmed.isBlank() && mediaB64 == null) return

        val sId = _currentSessionId.value ?: return
        val currentCfg = _selectedModelConfig.value

        _isSending.value = true
        val mediaUri = if (mediaB64 != null) "data:image/jpeg;base64,$mediaB64" else null
        val mediaType = if (mediaB64 != null) "image/jpeg" else null

        clearAttachment()

        viewModelScope.launch {
            val result = chatRepository.sendMessage(
                sessionId = sId,
                userText = if (trimmed.isNotBlank()) trimmed else "Please analyze this attached image.",
                mediaUri = mediaUri,
                mediaType = mediaType,
                specificConfigId = currentCfg?.id,
                memoryEnabled = _isMemoryEnabled.value
            )

            _isSending.value = false

            result.onSuccess { message ->
                if (_isLiveVoiceActive.value && !message.spokenText.isNullOrBlank()) {
                    speechService.speak(message.spokenText)
                }
            }
        }
    }

    fun toggleSpeechCleaner(enabled: Boolean) {
        _isSpeechCleanerEnabled.value = enabled
    }

    fun toggleMemory(enabled: Boolean) {
        _isMemoryEnabled.value = enabled
    }

    fun addMemory(fact: String, category: String) {
        viewModelScope.launch {
            memoryRepository.addMemory(fact, category)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            memoryRepository.deleteMemory(id)
        }
    }

    fun clearAllMemories() {
        viewModelScope.launch {
            memoryRepository.clearAll()
        }
    }

    fun setLiveVoiceActive(active: Boolean) {
        _isLiveVoiceActive.value = active
        if (!active) {
            speechService.stopListening()
            speechService.stopSpeaking()
        }
    }

    // API Hub Actions
    fun testConfig(config: ApiConfigEntity) {
        _testingConfigId.value = config.id
        viewModelScope.launch {
            val res = apiHubRepository.testConnection(config)
            res.fold(
                onSuccess = { testRes ->
                    _testResult.value = testRes
                    val msg = testRes.message
                    val status = if (testRes.success) {
                        "VERIFIED"
                    } else if (msg.contains("401", ignoreCase = true) || msg.contains("403", ignoreCase = true) || msg.contains("key", ignoreCase = true) || msg.contains("unauthorized", ignoreCase = true)) {
                        "INVALID_KEY"
                    } else if (msg.contains("429", ignoreCase = true) || msg.contains("rate", ignoreCase = true) || msg.contains("quota", ignoreCase = true)) {
                        "RATE_LIMITED"
                    } else if (msg.contains("404", ignoreCase = true) || msg.contains("model", ignoreCase = true) || msg.contains("unavailable", ignoreCase = true)) {
                        "MODEL_UNAVAILABLE"
                    } else {
                        "ERROR"
                    }
                    apiHubRepository.saveConfig(
                        config.copy(
                            status = status,
                            lastTestedTimestamp = System.currentTimeMillis(),
                            lastLatencyMs = testRes.latencyMs,
                            lastErrorMessage = if (testRes.success) null else testRes.message
                        ),
                        performTestFirst = false
                    )
                },
                onFailure = { err ->
                    val errMsg = err.message ?: "Unknown test failure"
                    val status = if (errMsg.contains("401", ignoreCase = true) || errMsg.contains("403", ignoreCase = true) || errMsg.contains("key", ignoreCase = true) || errMsg.contains("unauthorized", ignoreCase = true)) {
                        "INVALID_KEY"
                    } else if (errMsg.contains("429", ignoreCase = true) || errMsg.contains("rate", ignoreCase = true) || errMsg.contains("quota", ignoreCase = true)) {
                        "RATE_LIMITED"
                    } else if (errMsg.contains("404", ignoreCase = true) || errMsg.contains("model", ignoreCase = true) || errMsg.contains("unavailable", ignoreCase = true)) {
                        "MODEL_UNAVAILABLE"
                    } else {
                        "ERROR"
                    }
                    _testResult.value = ConnectionTestResult(
                        success = false,
                        latencyMs = 0,
                        message = errMsg
                    )
                    apiHubRepository.saveConfig(
                        config.copy(
                            status = status,
                            lastTestedTimestamp = System.currentTimeMillis(),
                            lastErrorMessage = errMsg
                        ),
                        performTestFirst = false
                    )
                }
            )
            _testingConfigId.value = null
        }
    }

    fun saveConfig(config: ApiConfigEntity, performTest: Boolean = true) {
        viewModelScope.launch {
            val res = apiHubRepository.saveConfig(config, performTest)
            if (config.isDefault) {
                _selectedModelConfig.value = res.getOrNull() ?: config
            }
        }
    }

    fun deleteConfig(id: String) {
        viewModelScope.launch {
            apiHubRepository.deleteConfig(id)
            if (_selectedModelConfig.value?.id == id) {
                _selectedModelConfig.value = apiHubRepository.getActiveChatConfig()
            }
        }
    }

    fun toggleConfigEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch {
            apiHubRepository.toggleEnabled(id, enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechService.destroy()
    }
}
