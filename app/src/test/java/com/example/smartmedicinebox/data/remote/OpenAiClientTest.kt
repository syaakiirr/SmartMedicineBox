package com.example.smartmedicinebox.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OpenAiClientTest {
    @Test
    fun parseResponse_returnsOutputText() {
        val response = """
            {
              "output": [
                {
                  "type": "message",
                  "content": [
                    {"type": "output_text", "text": "Paracetamol reduces pain and fever."}
                  ]
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "Paracetamol reduces pain and fever.",
            OpenAiClient.parseResponse(response)
        )
    }

    @Test
    fun parseResponse_returnsRefusalText() {
        val response = """
            {
              "output": [
                {
                  "type": "message",
                  "content": [
                    {"type": "refusal", "refusal": "I cannot help with that request."}
                  ]
                }
              ]
            }
        """.trimIndent()

        assertEquals(
            "I cannot help with that request.",
            OpenAiClient.parseResponse(response)
        )
    }

    @Test
    fun parseResponse_rejectsEmptyOutput() {
        assertThrows(OpenAiApiException::class.java) {
            OpenAiClient.parseResponse("{\"output\":[]}")
        }
    }

    @Test
    fun parseResponse_combinesMultipleTextParts() {
        val response = """
            {
              "status": "completed",
              "output": [
                {
                  "content": [
                    {"type": "output_text", "text": "First section."},
                    {"type": "output_text", "text": "Safety reminder."}
                  ]
                }
              ]
            }
        """.trimIndent()

        assertEquals("First section.\nSafety reminder.", OpenAiClient.parseResponse(response))
    }

    @Test
    fun parseResponse_rejectsIncompleteResponse() {
        val response = """
            {
              "status": "incomplete",
              "incomplete_details": {"reason": "max_output_tokens"},
              "output": []
            }
        """.trimIndent()

        assertThrows(OpenAiApiException::class.java) {
            OpenAiClient.parseResponse(response)
        }
    }
}
