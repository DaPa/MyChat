package com.example.utils

import com.example.settings.MyChatSettings
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefBrowser
import java.io.File

internal object ComponentUtils {
    private val LOG = Logger.getInstance("com.example.utils.ComponentUtils")

    /**
     * Milliseconds to wait after clicking an "Attach" button before querying for the
     * file input that it reveals. Extracted from the magic `300` literal so it is
     * easy to tune if a site is slower than average.
     */
    private const val ATTACH_BTN_REVEAL_DELAY_MS = 300

    private val INJECT_FILES_JS_TEMPLATE: String by lazy { loadJs("injectFiles.js") }
    private val FILE_BLOB_JS_TEMPLATE: String by lazy { loadJs("fileBlob.js") }
    private val MARKDOWN_JS_TEMPLATE: String by lazy { loadJs("markdown.js") }

    private fun buildMarkdownText(markdown: String): String =
        MARKDOWN_JS_TEMPLATE
            .replace("__ESCAPED_MARKDOWN_TEXT__", markdown)

    /**
     * Loads the JS template from resources once and caches it.
     * The template is read at class-load time; it never changes at runtime.
     */
    private fun loadJs(resourceName: String): String {
        return ComponentUtils::class.java
            .getResourceAsStream("/js/$resourceName")
            ?.bufferedReader()
            ?.readText()
            ?: error("Missing resource: /js/$resourceName")
    }

    /** Selectors serialized once — order is the priority order. */
    private val SELECTORS_JS_ARRAY = listOf(
        "[aria-label*=\"Add files and more\"]",              // ChatGPT
        "[aria-label*=\"Add and manage sources\"]",          // Copilot
        "[aria-label*=\"More input options\"]",              // AI Studio
        "[aria-label*=\"Lisää tiedostoja ja työkaluja\"]",
        "[aria-label*=\"Lisää tiedostoja\"]",
        "[aria-label*=\"Lisää\"]",
        "[aria-label*=\"Add files and tools\"]",
        "[aria-label*=\"Add files\"]",
        "[aria-label*=\"Upload and tools\"]",                // Gemini
        "[aria-label*=\"Upload & tools\"]",
        "[aria-label*=\"Add files, connectors, and more\"]", // Claude
        "[aria-label*=\"Add files or tools\"]",              // Perplexity
        "[aria-label*=\"Add images or PDFs\"]",              // DuckDuckGo AI
        "[aria-label*=\"Attach\"]",                          // Grok
        "button[title*=\"Attach\"]",                         // generics
        "[data-testid*=\"attach\"]",
        "[aria-label*=\"Add\"]",
        "[aria-label*=\"Upload\"]",
        "[data-testid=\"composer-attach-btn\"]",
        "[data-testid=\"composer-plus-btn\"]"
    )
    val jsArrayLiteral = SELECTORS_JS_ARRAY.joinToString(
        prefix = "[",
        postfix = "]"
    ) { selector ->
        "\"" + selector.replace("\"", "\\\"") + "\""
    }

    /**
     * Escapes a filename so it can be safely embedded inside a JS double-quoted string.
     *
     * Handles: backslash (must be first), double-quote, newline, carriage return, tab,
     * and backtick (not a JS escape hazard inside "..." but present in some filenames
     * and harmless to escape defensively).
     *
     * Note: single-quotes and template-literal delimiters are not relevant here because
     * the injected JS uses double-quoted string literals for filenames.
     */
    private fun escapeForJsString(name: String): String =
        name
            .replace("\\", "\\\\")   // must be first — avoids double-escaping
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")

    // Escape for JS string embedding
    private fun escapeForMarkdownString(markdown: String): String =
        markdown
            .replace("\\", "\\\\")   // must be first — avoids double-escaping
            .replace("`", "\\`")   // template literal safe
            .replace("$", "\\$")

    /**
     * Returns the MIME type for the given filename based on its extension,
     * falling back to `application/octet-stream` for unknown types.
     *
     * Why this matters: several AI chat UIs (Claude, Copilot, ChatGPT) inspect
     * `file.type` on the synthetic File object before deciding whether to accept
     * or preview it. An empty string (the default when no type is passed to
     * `new Blob(...)`) is treated as unknown/rejected by some sites.
     * `application/octet-stream` is the correct RFC 2046 fallback for arbitrary
     * binary data and is handled gracefully by all tested sites.
     */
    private fun mimeTypeForFilename(name: String): String {
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

    private fun prepareFileForCopilot(file: File): Pair<File, Boolean> {
        val ext = file.extension.lowercase()
        val copilotSupported = setOf(
            "bash", "bmp", "c", "config", "cpp", "cs", "css", "csv",
            "dart", "doc", "docm", "docx", "dot", "ehtml", "eml", "fluid", "gif",
            "h", "htm", "html", "ini", "java", "jfif", "jpe", "jpeg", "jpg", "js", "json", "jsx",
            "log", "loop", "lua", "md", "page", "pdf", "php", "pjp", "pjpeg", "pl", "png", "ppsm", "ppt", "pptx", "py",
            "rs", "rtf", "sh", "shtm", "shtml", "sql", "text", "tsv", "tsx", "txt",
            "utf8", "webp", "xls", "xlsm", "xlsx", "xml", "yaml", "yml",
            // etc.
        )
        if (ext in copilotSupported) return Pair(file, false)  // false = not a temp file, no copy needed

        val tempDir = File(System.getProperty("java.io.tmpdir"), "MyChat").also { it.mkdirs() }
        val tempFile = File(tempDir, "${file.name}.txt")
        file.copyTo(tempFile, overwrite = true)
        return Pair(tempFile, true)  // true = we created it, we clean it up
    }

    /**
     * Injects the given files into the browser page via JavaScript.
     * Reads each file, base64-encodes it, and uses a synthetic DataTransfer
     * to set the files on the page's file input element.
     *
     * Used by both drag-and-drop handling (MyChatWindowFactory) and
     * the "Send to MyChat" action (SendToMyChatAction).
     *
     * ⚠️ This function reads file bytes synchronously on the calling thread.
     * Callers are responsible for ensuring this does not run on the EDT.
     * The AWT-DnD drop handler dispatches to a pooled thread before calling here.
     * SendToMyChatAction.actionPerformed() runs on BGT — also safe.
     */
    // TODO: drag&drop 3 files from Projects => all 3 are attached, but right-click and SendToMyChatAction attaches only the first file!
    fun injectFilesViaJs(browser: JBCefBrowser, files: List<File>, currentSite: MyChatSettings.SiteConfig? = null) {
        val tempFiles = mutableListOf<File>()
        val fileBlobsJs = buildString {
            for (file in files) {
                val size = file.length()
                if (size !in 10..5_000_000) {
                    LOG.warn("injectFilesViaJs — skipping ${file.name}: size $size bytes outside [10B, 5MB]")
                    continue
                }

                val (file, isTempCopy) = if (currentSite?.isCopilot() == true)
                    prepareFileForCopilot(file) else Pair(file, false)
                if (isTempCopy) tempFiles += file

                val base64 = java.util.Base64.getEncoder().encodeToString(file.readBytes())
                val safeName = escapeForJsString(file.name)
                val mimeType = mimeTypeForFilename(file.name)

                append(
                    FILE_BLOB_JS_TEMPLATE
                        .replace("__BASE64_DATA__", base64)
                        .replace("__MIME_TYPE__", mimeType)
                        .replace("__SAFE_NAME__", safeName)
                ).append("(dataTransfer);\n\n")

            }
        }

        if (fileBlobsJs.isEmpty()) {
            tempFiles.forEach {
                val deleted = it.delete()
                if (!deleted) LOG.warn("injectFilesViaJs — failed to remove temp file: ${it.path}")
            }
            return
        }

        val js = INJECT_FILES_JS_TEMPLATE
            .replace("__FILE_BLOBS__", fileBlobsJs)
            .replace("__SELECTORS_JS_ARRAY__", jsArrayLiteral)
            .replace("__ATTACH_DELAY_MS__", ATTACH_BTN_REVEAL_DELAY_MS.toString())

        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)

        // JS is already built — base64 content is embedded in the string, the actual files are no longer needed!
        tempFiles.forEach {
            val deleted = it.delete()
            if (!deleted) LOG.warn("injectFilesViaJs — failed to delete temp file: ${it.path}")
        }
    }

    // TODO: Size limit: No hard limit, but very large files will produce an enormous JS string. You may want
    //          to cap at e.g. 50KB per file or truncate with a note.
    // TODO: When to use which approach?
    //          You could offer both strategies and let the user or site config decide:
    //          enum class InjectionMode { FILE_UPLOAD, MARKDOWN_PASTE }
    //          Then in injectFilesViaJs / your drop handler, branch on the mode — or automatically fall back
    //          to markdown when prepareFileForCopilot would otherwise trigger.
    // TODO: ChatGPT preferences:
    //           < 50 lines: paste directly into chat.  => injectContentAsMarkdown
    //          50–500 lines: upload the file.  => injectFilesViaJs
    fun injectContentAsMarkdown(browser: JBCefBrowser, files: List<File>) {
        val markdown = buildString {
            for (file in files) {
                val ext = file.extension.lowercase()
                val lang = extToMarkdownLang(ext)
                append("**${file.name}**\n")
                append("```$lang\n")
                append(file.readText(Charsets.UTF_8))
                append("\n```\n\n")
            }
        }
        // Escape for JS string embedding
        val escaped = escapeForMarkdownString(markdown)
        val js = buildMarkdownText(escaped)
        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    private fun extToMarkdownLang(ext: String) = when (ext) {
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