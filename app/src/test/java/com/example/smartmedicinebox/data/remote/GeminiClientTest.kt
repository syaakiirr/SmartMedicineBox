package com.example.smartmedicinebox.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeminiClientTest {
    @Test
    fun parseResponse_returnsOutputText() {
        val response = """
            {
              "candidates": [{
                "content": {
                  "parts": [{"text": "Paracetamol reduces pain and fever."}],
                  "role": "model"
                },
                "finishReason": "STOP"
              }]
            }
        """.trimIndent()

        assertEquals(
            "Paracetamol reduces pain and fever.",
            GeminiClient.parseResponse(response)
        )
    }

    @Test
    fun parseResponse_rejectsSafetyBlock() {
        val response = """
            {
              "promptFeedback": {
                "blockReason": "SAFETY"
              }
            }
        """.trimIndent()

        assertThrows(GeminiApiException::class.java) {
            GeminiClient.parseResponse(response)
        }
    }

    @Test
    fun parseResponse_rejectsEmptyOutput() {
        assertThrows(GeminiApiException::class.java) {
            GeminiClient.parseResponse("{\"candidates\":[]}")
        }
    }

    @Test
    fun parseResponse_combinesMultipleTextParts() {
        val response = """
            {
              "candidates": [{
                "content": {
                  "parts": [
                    {"text": "First section."},
                    {"text": "Safety reminder."}
                  ]
                },
                "finishReason": "STOP"
              }]
            }
        """.trimIndent()

        assertEquals("First section.\nSafety reminder.", GeminiClient.parseResponse(response))
    }

    @Test
    fun parseResponse_rejectsBlockedFinishReason() {
        val response = """
            {
              "candidates": [{
                "content": {"parts": []},
                "finishReason": "SAFETY"
              }]
            }
        """.trimIndent()

        assertThrows(GeminiApiException::class.java) {
            GeminiClient.parseResponse(response)
        }
    }

    @Test
    fun parseResponse_rejectsTruncatedResponse() {
        val response = """
            {
              "candidates": [{
                "content": {"parts": [{"text": "Partial answer"}]},
                "finishReason": "MAX_TOKENS"
              }]
            }
        """.trimIndent()

        assertThrows(GeminiApiException::class.java) {
            GeminiClient.parseResponse(response)
        }
    }

    @Test
    fun shouldRetry_retriesTemporaryServerErrorsTwice() {
        assertEquals(true, GeminiClient.shouldRetry(503, 0))
        assertEquals(true, GeminiClient.shouldRetry(503, 1))
        assertEquals(false, GeminiClient.shouldRetry(503, 2))
        assertEquals(false, GeminiClient.shouldRetry(429, 0))
    }
}
