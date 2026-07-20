package com.example.utils

/**
 * Utility functions for MIME type resolution and Markdown language mapping.
 */
internal object MimeUtils {

    /**
     * Returns the MIME type for the given filename based on its extension,
     * falling back to `application/octet-stream` for unknown types.
     */
    fun mimeTypeForFilename(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return when (ext) {
            "txt", "md", "kt", "java", "py", "rb", "go", "rs",
            "c", "cpp", "h", "hpp", "sh", "bat", "gradle" -> "text/plain"

            "html", "htm" -> "text/html"
            "css" -> "text/css"
            "js", "ts", "mjs" -> "text/javascript"
            "json" -> "application/json"
            "xml" -> "application/xml"
            "pdf" -> "application/pdf"
            "csv" -> "text/csv"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            "svg" -> "image/svg+xml"
            "zip" -> "application/zip"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            else -> "application/octet-stream"
        }
    }

    /**
     * Maps a file extension to a Markdown language identifier.
     */
    fun extToMarkdownLang(ext: String): String = when (ext) {
        "kt", "kts" -> "kotlin"
        "java" -> "java"
        "py" -> "python"
        "js", "mjs" -> "javascript"
        "ts", "tsx" -> "typescript"
        "json" -> "json"
        "xml" -> "xml"
        "html", "htm" -> "html"
        "css" -> "css"
        "sh", "bash" -> "bash"
        "sql" -> "sql"
        "md" -> "markdown"
        "go" -> "go"
        "rs" -> "rust"
        "c", "h" -> "c"
        "cpp", "hpp" -> "cpp"
        "cs" -> "csharp"
        else -> ""
    }
}
