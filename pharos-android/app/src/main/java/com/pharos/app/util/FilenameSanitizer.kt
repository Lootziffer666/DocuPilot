package com.pharos.app.util

object FilenameSanitizer {

    /**
     * Sanitize a project name for use in a filename.
     * Replaces non-alphanumeric characters (except hyphens and underscores) with underscores.
     * Collapses multiple underscores and trims.
     */
    fun sanitize(name: String): String {
        return name
            .trim()
            .replace(Regex("[^a-zA-Z0-9äöüÄÖÜß_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trimStart('_')
            .trimEnd('_')
            .take(100) // limit filename length
            .ifEmpty { "unnamed" }
    }
}
