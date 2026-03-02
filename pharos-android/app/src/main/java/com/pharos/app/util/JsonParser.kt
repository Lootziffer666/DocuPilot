package com.pharos.app.util

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import com.pharos.app.network.model.AnalysisResponse

object JsonParser {

    private val gson = Gson()

    /**
     * Parse the AI response JSON, tolerant of markdown code fences and extra whitespace.
     */
    fun parseAnalysisResponse(raw: String): AnalysisResponse? {
        val cleaned = cleanJsonResponse(raw)
        return try {
            gson.fromJson(cleaned, AnalysisResponse::class.java)
        } catch (e: JsonSyntaxException) {
            null
        }
    }

    /**
     * Remove markdown code fences and trim whitespace from JSON response.
     */
    fun cleanJsonResponse(raw: String): String {
        var cleaned = raw.trim()

        // Remove ```json ... ``` or ``` ... ``` wrapping
        if (cleaned.startsWith("```")) {
            val firstNewline = cleaned.indexOf('\n')
            if (firstNewline > 0) {
                cleaned = cleaned.substring(firstNewline + 1)
            }
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length - 3)
        }

        return cleaned.trim()
    }

    /**
     * Serialize a list of strings to JSON array string.
     */
    fun toJsonArray(list: List<String>): String = gson.toJson(list)

    /**
     * Deserialize a JSON array string to a list of strings.
     */
    fun fromJsonArray(json: String): List<String> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
