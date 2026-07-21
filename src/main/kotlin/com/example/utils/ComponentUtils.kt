package com.example.utils

import com.example.settings.MyChatSettings
import com.example.utils.JsUtils.escapeForJsString
import com.example.utils.JsUtils.escapeForJsTemplateLiteral
import com.example.utils.JsUtils.loadJs
import com.example.utils.MimeUtils.extToMarkdownLang
import com.example.utils.MimeUtils.mimeTypeForFilename
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefBrowser
import java.io.File

internal object ComponentUtils {
    private val LOG = Logger.getInstance("com.example.utils.ComponentUtils")

    private const val ATTACH_BTN_REVEAL_DELAY_MS = 300

    private val INJECT_FILES_JS_TEMPLATE: String by lazy { loadJs("injectFiles.js") }
    private val FILE_BLOB_JS_TEMPLATE: String by lazy { loadJs("fileBlob.js") }
    private val MARKDOWN_JS_TEMPLATE: String by lazy { loadJs("markdown.js") }

    private fun buildMarkdownText(markdown: String): String =
        MARKDOWN_JS_TEMPLATE
            .replace("__ESCAPED_MARKDOWN_TEXT__", markdown)

    /**
     * Prepare the file for Copilot by converting it to a text file if necessary.
     * See: https://support.microsoft.com/en-us/Microsoft-365-Copilot/file-formats-supported-by-microsoft-365-copilot
     */
    private fun prepareFileForCopilot(file: File): Pair<File, Boolean> {
        val ext = file.extension.lowercase()
        val copilotSupported = setOf(
            "bash", "bmp", "c", "config", "cpp", "cs", "css", "csv",
            "dart", "doc", "docm", "docx", "dot", "ehtml", "eml", "fluid", "gif",
            "h", "htm", "html", "ini", "java", "jfif", "jpe", "jpeg", "jpg", "js", "json", "jsx",
            "log", "loop", "lua", "md", "page", "pdf", "php", "pjp", "pjpeg", "pl", "png", "ppsm", "ppt", "pptx", "py",
            "rs", "rtf", "sh", "shtm", "shtml", "sql", "text", "tsv", "tsx", "txt",
            "utf8", "webp", "xls", "xlsm", "xlsx", "xml", "yaml", "yml",
        )
        if (ext in copilotSupported) return Pair(file, false)

        val tempDir = File(System.getProperty("java.io.tmpdir"), "MyChat").also { it.mkdirs() }
        val tempFile = File(tempDir, "${file.name}.txt")
        file.copyTo(tempFile, overwrite = true)
        return Pair(tempFile, true)
    }

    /**
     * Injects the specified files into the chat browser by creating File blobs and simulating
     * a DataTransfer event. This method attempts to find an "Attach" button on the page
     * and click it to reveal the file input if necessary.
     *
     * @param browser The browser instance to inject files into.
     * @param files The list of files to be injected.
     * @param currentSite The configuration of the current site, used for site-specific processing (e.g., Copilot).
     */
    fun injectFilesViaJs(browser: JBCefBrowser, files: List<File>, currentSite: MyChatSettings.SiteConfig? = null) {
        val tempFiles = mutableListOf<File>()
        val fileBlobsJs = buildString {
            for (file in files) {
                val size = file.length()
                // Limit file size to avoid browser memory issues and comply with most AI service limits.
                // Minimum 10 bytes to ignore empty/corrupt files.
                if (size !in 10..5_000_000) {
                    LOG.warn("injectFilesViaJs — skipping ${file.name}: size $size bytes outside [10B, 5MB]")
                    continue
                }

                val (processedFile, isTempCopy) = if (currentSite?.isCopilot() == true)
                    prepareFileForCopilot(file) else Pair(file, false)
                if (isTempCopy) tempFiles += processedFile

                val base64 = java.util.Base64.getEncoder().encodeToString(processedFile.readBytes())
                val safeName = escapeForJsString(processedFile.name)
                val mimeType = mimeTypeForFilename(processedFile.name)

                append(
                    FILE_BLOB_JS_TEMPLATE
                        .replace("__BASE64_DATA__", base64)
                        .replace("__MIME_TYPE__", mimeType)
                        .replace("__SAFE_NAME__", safeName)
                ).append("\n")
            }
        }

        if (fileBlobsJs.isEmpty()) {
            cleanupTempFiles(tempFiles)
            return
        }

        val sites = MyChatSettings.getInstance().state.sites

        val orderedSites =
            listOfNotNull(currentSite) +
            sites.filter { it !== currentSite }

        val phrases =
            buildList {
                addAll(
                    orderedSites.flatMap { it.expectedStrings }
                )

                addAll(
                    listOf(
                        "button[title*=\"Attach\"]",
                        "[data-testid*=\"attach\"]",
                        "[aria-label*=\"Add\"]",
                        "[aria-label*=\"Upload\"]",
                        "[data-testid=\"composer-attach-btn\"]",
                        "[data-testid=\"composer-plus-btn\"]"
                    )
                )
            }

        val jsArrayLiteral =
            phrases.joinToString(
                prefix = "[",
                postfix = "]"
            ) {
                "\"${escapeForJsString(it)}\""
            }


        val js = INJECT_FILES_JS_TEMPLATE
            .replace("__FILE_BLOBS__", fileBlobsJs)
            .replace("__SELECTORS_JS_ARRAY__", jsArrayLiteral)
            .replace("__ATTACH_DELAY_MS__", ATTACH_BTN_REVEAL_DELAY_MS.toString())

        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
        cleanupTempFiles(tempFiles)
    }

    /**
     * Reads the content of the specified files and injects them into the chat browser
     * formatted as Markdown code blocks.
     */
    fun injectContentAsMarkdown(browser: JBCefBrowser, files: List<File>) {
        val markdown = buildString {
            for (file in files) {
                val size = file.length()
                if (size > 5_000_000) {
                    LOG.warn("injectContentAsMarkdown — skipping ${file.name}: size $size bytes > 5MB")
                    continue
                }
                val ext = file.extension.lowercase()
                val lang = extToMarkdownLang(ext)
                append("**${file.name}**\n")
                append("```$lang\n")
                append(file.readText(Charsets.UTF_8))
                append("\n```\n\n")
            }
        }
        if (markdown.isBlank()) return
        val escaped = escapeForJsTemplateLiteral(markdown)
        val js = buildMarkdownText(escaped)
        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    /**
     * Injects the provided text selection into the chat browser formatted as a Markdown code block,
     * including the source filename.
     */
    fun injectSelectionAsMarkdown(browser: JBCefBrowser, filename: String, selection: String) {
        val ext = filename.substringAfterLast('.', "").lowercase()
        val lang = extToMarkdownLang(ext)
        val markdown = buildString {
            append("**$filename (selection)**\n")
            append("```$lang\n")
            append(selection)
            append("\n```\n\n")
        }
        val escaped = escapeForJsTemplateLiteral(markdown)
        val js = buildMarkdownText(escaped)
        browser.cefBrowser.executeJavaScript(js, browser.cefBrowser.url, 0)
    }

    private fun cleanupTempFiles(files: List<File>) {
        files.forEach {
            val deleted = it.delete()
            if (!deleted) LOG.warn("cleanupTempFiles — failed to delete temp file: ${it.path}")
        }
    }
}