package com.example.data.network.providers

data class ProviderTemplate(
    val name: String,
    val category: String, // "Chat / LLM", "Image Generation", "Voice", "Vision"
    val providerType: String, // "GEMINI", "OPENAI_COMPATIBLE"
    val defaultBaseUrl: String,
    val initialSuggestedModel: String = "",
    val apiKeyUrl: String,
    val hasFreeTier: Boolean,
    val freeTierLabel: String,
    val description: String
)

object ProviderRegistry {

    private val geminiProvider = GeminiProvider()
    private val openAiProvider = OpenAiCompatibleProvider()

    fun getProvider(providerType: String): AiProvider {
        return when (providerType.uppercase()) {
            "GEMINI" -> geminiProvider
            else -> openAiProvider
        }
    }

    val templates: List<ProviderTemplate> = listOf(
        ProviderTemplate(
            name = "Google Gemini",
            category = "Chat / LLM",
            providerType = "GEMINI",
            defaultBaseUrl = "https://generativelanguage.googleapis.com",
            initialSuggestedModel = "", // Discovered dynamically from /v1beta/models
            apiKeyUrl = "https://aistudio.google.com/app/apikey",
            hasFreeTier = true,
            freeTierLabel = "Get Free Gemini API Key",
            description = "Google AI Studio with dynamic model discovery, multimodal vision, and free tier."
        ),
        ProviderTemplate(
            name = "xAI Grok",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "https://api.x.ai/v1",
            initialSuggestedModel = "",
            apiKeyUrl = "https://console.x.ai",
            hasFreeTier = false,
            freeTierLabel = "Get xAI Grok API Key",
            description = "xAI frontier intelligence and real-time reasoning models."
        ),
        ProviderTemplate(
            name = "OpenAI",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "https://api.openai.com/v1",
            initialSuggestedModel = "",
            apiKeyUrl = "https://platform.openai.com/api-keys",
            hasFreeTier = false,
            freeTierLabel = "Get OpenAI API Key",
            description = "Frontier GPT reasoning and general multimodal intelligence."
        ),
        ProviderTemplate(
            name = "Groq Cloud",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "https://api.groq.com/openai/v1",
            initialSuggestedModel = "",
            apiKeyUrl = "https://console.groq.com/keys",
            hasFreeTier = true,
            freeTierLabel = "Get Free Groq API Key",
            description = "Ultra-fast inference on LPUs with open-source Meta Llama & Mistral."
        ),
        ProviderTemplate(
            name = "DeepSeek",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "https://api.deepseek.com",
            initialSuggestedModel = "",
            apiKeyUrl = "https://platform.deepseek.com/api_keys",
            hasFreeTier = false,
            freeTierLabel = "Get DeepSeek API Key",
            description = "Competitive open-weights reasoning model with low-cost pay-as-you-go API."
        ),
        ProviderTemplate(
            name = "Local Ollama / Custom",
            category = "Chat / LLM",
            providerType = "OPENAI_COMPATIBLE",
            defaultBaseUrl = "http://localhost:11434/v1",
            initialSuggestedModel = "",
            apiKeyUrl = "https://ollama.com",
            hasFreeTier = true,
            freeTierLabel = "Ollama Setup Guide",
            description = "Run open-source models completely locally or through custom reverse proxies."
        )
    )
}
