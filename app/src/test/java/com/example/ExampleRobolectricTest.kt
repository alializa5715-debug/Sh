package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.entity.ApiConfigEntity
import com.example.data.network.SpeechCleaner
import com.example.data.network.providers.ProviderRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Nova AI", appName)
  }

  @Test
  fun `speech cleaner removes markdown and code blocks`() {
    val input = "### Hello World!\nHere is `inline code` and **bold text**."
    val cleaned = SpeechCleaner.cleanForSpeech(input)
    assertFalse(cleaned.contains("#"))
    assertFalse(cleaned.contains("`"))
    assertFalse(cleaned.contains("*"))
    assertTrue(cleaned.contains("Hello World!"))
    assertTrue(cleaned.contains("Here is inline code and bold text"))
  }

  @Test
  fun `speech cleaner removes urls and json`() {
    val input = "Check https://example.com and {\"key\": \"value\"} now."
    val cleaned = SpeechCleaner.cleanForSpeech(input)
    assertFalse(cleaned.contains("https://"))
    assertFalse(cleaned.contains("\"key\""))
  }

  @Test
  fun `provider registry contains expected providers`() {
    val templates = ProviderRegistry.templates
    assertTrue(templates.any { it.name.contains("Gemini") })
    assertTrue(templates.any { it.name.contains("OpenAI") })
    assertTrue(templates.any { it.name.contains("Groq") })
    assertTrue(templates.isNotEmpty())
  }

  @Test
  fun `api config entity masks secret key correctly`() {
    val config = ApiConfigEntity(
      name = "Test API",
      apiKey = "sk-1234567890abcdef"
    )
    val masked = config.maskedApiKey
    assertTrue(masked.startsWith("••••"))
    assertTrue(masked.endsWith("cdef"))
    assertFalse(masked.contains("1234567890"))
  }
}
