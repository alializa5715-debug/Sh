package com.example.data.network

object SpeechCleaner {

    /**
     * Cleans an AI response specifically for text-to-speech rendering:
     * - Intelligently removes or replaces code blocks with short natural audio cues
     * - Strips JSON blocks and raw programming syntax
     * - Strips Markdown headers, bold, italics, strikethrough, blockquotes
     * - Strips citation brackets like [1], [source], (link)
     * - Strips raw URLs
     * - Strips emojis and decorative Unicode symbols
     * - Strips internal system notes, tool calls, and UI metadata
     * - Collapses multiple whitespaces and cleans punctuation
     */
    fun cleanForSpeech(rawText: String): String {
        if (rawText.isBlank()) return ""

        var text = rawText

        // 1. Remove Markdown code blocks (e.g. ```kotlin ... ```)
        // Replace with brief natural transition: "Here is the code." or skip entirely
        text = text.replace(Regex("```[a-zA-Z]*\\n[\\s\\S]*?```"), " Here is the code snippet. ")
        text = text.replace(Regex("`([^`\\n]+)`"), "$1") // strip inline code backticks while preserving text

        // 2. Remove JSON or tool metadata blocks
        text = text.replace(Regex("\\{[\\s\\S]*?\\}"), "")
        text = text.replace(Regex("Note of integration:[^\\n]*", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("Internal note:[^\\n]*", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("System instruction:[^\\n]*", RegexOption.IGNORE_CASE), "")
        text = text.replace(Regex("Tool metadata:[^\\n]*", RegexOption.IGNORE_CASE), "")

        // 3. Remove URLs
        text = text.replace(Regex("https?://\\S+"), "")
        text = text.replace(Regex("www\\.\\S+"), "")

        // 4. Clean Markdown formatting
        // Headers (#, ##, ###)
        text = text.replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
        // Bold / Italics (**text**, *text*, __text__, _text_)
        text = text.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
        text = text.replace(Regex("\\*([^*]+)\\*"), "$1")
        text = text.replace(Regex("__([^_]+)__"), "$1")
        text = text.replace(Regex("_([^_]+)_"), "$1")
        text = text.replace(Regex("~~([^~]+)~~"), "$1")
        // Markdown bullet points and blockquotes
        text = text.replace(Regex("^[\\s]*[-*+]\\s+", RegexOption.MULTILINE), "")
        text = text.replace(Regex("^[\\s]*>\\s*", RegexOption.MULTILINE), "")
        // Markdown tables (e.g. | col | col |)
        text = text.replace(Regex("\\|[^\\n]+\\|"), "")
        // Citations like [1], [2], [source]
        text = text.replace(Regex("\\[\\d+\\]"), "")
        text = text.replace(Regex("\\[[^\\]]+\\]\\([^\\)]+\\)"), "$1") // link text only

        // 5. Remove Emojis and miscellaneous symbols
        // Match Unicode Emoji ranges, pictorials, and modifiers
        val emojiPattern = Regex("[\\p{So}\\p{Sk}\\p{Sm}\\p{Cs}\\x{1F300}-\\x{1F9FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}]", RegexOption.IGNORE_CASE)
        text = text.replace(emojiPattern, "")

        // 6. Clean mathematical symbols & programming syntax artifacts
        text = text.replace(Regex("[<>{}\\[\\]|~^\\\\]"), "")

        // 7. Normalize line breaks and multiple spaces
        text = text.replace(Regex("\\n+"), " ")
        text = text.replace(Regex("\\s{2,}"), " ")

        return text.trim()
    }
}
