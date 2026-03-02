package com.pharos.app

import com.pharos.app.util.FilenameSanitizer
import org.junit.Assert.assertEquals
import org.junit.Test

class FilenameSanitizerTest {

    @Test
    fun `sanitize replaces special characters`() {
        assertEquals("Hello_World", FilenameSanitizer.sanitize("Hello World!"))
    }

    @Test
    fun `sanitize handles german umlauts`() {
        assertEquals("Ärger_mit_Öffnung", FilenameSanitizer.sanitize("Ärger mit Öffnung"))
    }

    @Test
    fun `sanitize collapses multiple underscores`() {
        assertEquals("a_b_c", FilenameSanitizer.sanitize("a___b___c"))
    }

    @Test
    fun `sanitize trims leading and trailing underscores`() {
        assertEquals("hello", FilenameSanitizer.sanitize("___hello___"))
    }

    @Test
    fun `sanitize preserves hyphens`() {
        assertEquals("my-project", FilenameSanitizer.sanitize("my-project"))
    }

    @Test
    fun `sanitize limits length to 100`() {
        val longName = "a".repeat(200)
        assertEquals(100, FilenameSanitizer.sanitize(longName).length)
    }

    @Test
    fun `sanitize returns unnamed for empty input`() {
        assertEquals("unnamed", FilenameSanitizer.sanitize(""))
    }

    @Test
    fun `sanitize returns unnamed for only special chars`() {
        assertEquals("unnamed", FilenameSanitizer.sanitize("!@#$%^&*()"))
    }

    @Test
    fun `sanitize handles whitespace only`() {
        assertEquals("unnamed", FilenameSanitizer.sanitize("   "))
    }

    @Test
    fun `sanitize preserves numbers`() {
        assertEquals("Project_2024", FilenameSanitizer.sanitize("Project 2024"))
    }

    @Test
    fun `sanitize handles mixed content`() {
        assertEquals(
            "My_Project-v2_Final",
            FilenameSanitizer.sanitize("My Project-v2 (Final)")
        )
    }
}
