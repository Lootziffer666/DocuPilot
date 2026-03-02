package com.pharos.app

import com.pharos.app.domain.usecase.ProjectClusteringUseCase
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectClusteringTest {

    @Test
    fun `normalizeProjectName trims and capitalizes`() {
        assertEquals("Hello world", ProjectClusteringUseCase.normalizeProjectName("  hello world  "))
    }

    @Test
    fun `normalizeProjectName capitalizes first letter`() {
        assertEquals("Android development", ProjectClusteringUseCase.normalizeProjectName("android development"))
    }

    @Test
    fun `normalizeProjectName handles already capitalized`() {
        assertEquals("Already fine", ProjectClusteringUseCase.normalizeProjectName("Already fine"))
    }

    @Test
    fun `normalizeProjectName handles single char`() {
        assertEquals("A", ProjectClusteringUseCase.normalizeProjectName("a"))
    }

    @Test
    fun `normalizeProjectName handles empty string`() {
        assertEquals("", ProjectClusteringUseCase.normalizeProjectName(""))
    }

    @Test
    fun `normalizeProjectName preserves spaces inside`() {
        assertEquals("Multi word project name", ProjectClusteringUseCase.normalizeProjectName("multi word project name"))
    }
}
