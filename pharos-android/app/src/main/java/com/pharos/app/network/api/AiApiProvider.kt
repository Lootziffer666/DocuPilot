package com.pharos.app.network.api

/**
 * Abstract provider layer for AI API calls.
 * MVP: Perplexity. Extendable for other providers.
 */
interface AiApiProvider {

    val name: String

    /**
     * Test the API key by making a minimal request.
     * Returns a success message or throws an exception.
     */
    suspend fun testApiKey(apiKey: String): String

    /**
     * Analyze a document by sending its text content to the AI API.
     * Returns the raw JSON response string.
     */
    suspend fun analyzeDocument(
        apiKey: String,
        fileName: String,
        mimeType: String,
        textContent: String
    ): String
}
