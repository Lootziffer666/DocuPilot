package com.pharos.app

import com.pharos.app.util.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class JsonParserTest {

    @Test
    fun `parseAnalysisResponse parses valid JSON`() {
        val json = """
        {
            "topics": ["kotlin", "android"],
            "project_suggestions": ["Mobile App"],
            "summary": "This is a test summary.",
            "action_items": ["Review code", "Write tests"],
            "confidence": 0.85
        }
        """.trimIndent()

        val result = JsonParser.parseAnalysisResponse(json)
        assertNotNull(result)
        assertEquals(2, result!!.topics.size)
        assertEquals("kotlin", result.topics[0])
        assertEquals("android", result.topics[1])
        assertEquals(1, result.projectSuggestions.size)
        assertEquals("Mobile App", result.projectSuggestions[0])
        assertEquals("This is a test summary.", result.summary)
        assertEquals(2, result.actionItems.size)
        assertEquals(0.85, result.confidence, 0.001)
    }

    @Test
    fun `parseAnalysisResponse handles code fences`() {
        val json = """```json
        {
            "topics": ["testing"],
            "project_suggestions": ["Test Project"],
            "summary": "A summary.",
            "action_items": [],
            "confidence": 0.5
        }
        ```""".trimIndent()

        val result = JsonParser.parseAnalysisResponse(json)
        assertNotNull(result)
        assertEquals("testing", result!!.topics[0])
        assertEquals(0.5, result.confidence, 0.001)
    }

    @Test
    fun `parseAnalysisResponse handles backticks only`() {
        val json = """```
        {
            "topics": ["topic1"],
            "project_suggestions": [],
            "summary": "Summary here.",
            "action_items": [],
            "confidence": 0.7
        }
        ```""".trimIndent()

        val result = JsonParser.parseAnalysisResponse(json)
        assertNotNull(result)
        assertEquals("topic1", result!!.topics[0])
    }

    @Test
    fun `parseAnalysisResponse returns null for invalid JSON`() {
        val result = JsonParser.parseAnalysisResponse("not json at all")
        assertNull(result)
    }

    @Test
    fun `parseAnalysisResponse handles empty fields`() {
        val json = """
        {
            "topics": [],
            "project_suggestions": [],
            "summary": "",
            "action_items": [],
            "confidence": 0.0
        }
        """.trimIndent()

        val result = JsonParser.parseAnalysisResponse(json)
        assertNotNull(result)
        assertEquals(0, result!!.topics.size)
        assertEquals("", result.summary)
        assertEquals(0.0, result.confidence, 0.001)
    }

    @Test
    fun `cleanJsonResponse removes code fences`() {
        val input = "```json\n{\"key\": \"value\"}\n```"
        val cleaned = JsonParser.cleanJsonResponse(input)
        assertEquals("{\"key\": \"value\"}", cleaned)
    }

    @Test
    fun `cleanJsonResponse handles plain JSON`() {
        val input = """{"key": "value"}"""
        val cleaned = JsonParser.cleanJsonResponse(input)
        assertEquals("{\"key\": \"value\"}", cleaned)
    }

    @Test
    fun `toJsonArray and fromJsonArray roundtrip`() {
        val original = listOf("item1", "item2", "item3")
        val json = JsonParser.toJsonArray(original)
        val restored = JsonParser.fromJsonArray(json)
        assertEquals(original, restored)
    }

    @Test
    fun `fromJsonArray handles empty JSON`() {
        val result = JsonParser.fromJsonArray("[]")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `fromJsonArray handles invalid JSON`() {
        val result = JsonParser.fromJsonArray("not json")
        assertEquals(emptyList<String>(), result)
    }

    @Test
    fun `fromJsonArray handles null-like input`() {
        val result = JsonParser.fromJsonArray("")
        assertEquals(emptyList<String>(), result)
    }
}
