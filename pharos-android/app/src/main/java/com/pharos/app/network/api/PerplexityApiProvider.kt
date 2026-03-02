package com.pharos.app.network.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class PerplexityApiProvider : AiApiProvider {

    override val name: String = "Perplexity"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun testApiKey(apiKey: String): String = withContext(Dispatchers.IO) {
        val body = buildRequestBody(
            "You are a test assistant.",
            "Reply with exactly: {\"status\": \"ok\"}"
        )

        val request = buildRequest(apiKey, body)
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val code = response.code
            val errorBody = response.body?.string() ?: ""
            response.close()
            throw IOException("API test failed (HTTP $code): $errorBody")
        }

        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        response.close()
        extractContentFromResponse(responseBody)
    }

    override suspend fun analyzeDocument(
        apiKey: String,
        fileName: String,
        mimeType: String,
        textContent: String
    ): String = withContext(Dispatchers.IO) {
        val systemPrompt = """You are a document analysis assistant. Analyze the provided document and return ONLY a valid JSON object (no markdown, no code fences) with this exact structure:
{
  "topics": ["topic1", "topic2"],
  "project_suggestions": ["project1", "project2"],
  "summary": "A concise summary of the document content",
  "action_items": ["action1", "action2"],
  "confidence": 0.85
}

Rules:
- topics: 2-6 short topic keywords/phrases
- project_suggestions: 1-3 project names this document could belong to
- summary: 2-4 sentences summarizing the document
- action_items: 0-5 actionable items found in the document
- confidence: 0.0 to 1.0 indicating how confident you are in the analysis
- Return ONLY the JSON object, nothing else"""

        val userPrompt = """Analyze this document:

Filename: $fileName
Type: $mimeType

Content:
$textContent"""

        val body = buildRequestBody(systemPrompt, userPrompt)
        val request = buildRequest(apiKey, body)

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            val code = response.code
            val errorBody = response.body?.string() ?: ""
            response.close()
            throw IOException("API call failed (HTTP $code): $errorBody")
        }

        val responseBody = response.body?.string() ?: throw IOException("Empty response")
        response.close()
        extractContentFromResponse(responseBody)
    }

    private fun buildRequestBody(systemPrompt: String, userPrompt: String): String {
        val body = JsonObject().apply {
            addProperty("model", "sonar")
            add("messages", gson.toJsonTree(listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt)
            )))
            addProperty("max_tokens", 1024)
            addProperty("temperature", 0.1)
        }
        return gson.toJson(body)
    }

    private fun buildRequest(apiKey: String, body: String): Request {
        return Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody(jsonMediaType))
            .build()
    }

    private fun extractContentFromResponse(responseBody: String): String {
        return try {
            val json = gson.fromJson(responseBody, JsonObject::class.java)
            val choices = json.getAsJsonArray("choices")
            if (choices != null && choices.size() > 0) {
                val message = choices[0].asJsonObject.getAsJsonObject("message")
                message.get("content").asString
            } else {
                responseBody
            }
        } catch (e: Exception) {
            responseBody
        }
    }

    companion object {
        private const val API_URL = "https://api.perplexity.ai/chat/completions"
    }
}
