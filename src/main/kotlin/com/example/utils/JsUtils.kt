package com.example.utils

/**
 * Utility functions for JavaScript generation and escaping.
 */
internal object JsUtils {

    /**
     * Loads the JS template from resources once and caches it.
     */
    fun loadJs(resourceName: String): String {
        return JsUtils::class.java
            .getResourceAsStream("/js/$resourceName")
            ?.bufferedReader()
            ?.readText()
            ?: error("Missing resource: /js/$resourceName")
    }

    /**
     * Escapes a string so it can be safely embedded inside a JS double-quoted string.
     */
    fun escapeForJsString(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    /**
     * Escapes a string for use in a JS template literal.
     */
    fun escapeForJsTemplateLiteral(text: String): String =
        text
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
}
