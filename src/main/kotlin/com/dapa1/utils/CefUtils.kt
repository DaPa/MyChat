package com.dapa1.utils

import com.dapa1.settings.MyChatSettings
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.CefSettings
import org.cef.browser.CefBrowser
import org.cef.handler.CefDisplayHandlerAdapter

/**
 * CEF console message patterns that are noisy but entirely harmless.
 * Matched with [String.contains] — a substring match is enough.
 *
 * Extracted from MyChatWindowFactory so they can be maintained in one place
 * and reused by any future tool window that embeds a JBCefBrowser.
 */
val SUPPRESSED_CONSOLE_PATTERNS: List<String> = listOf(
    // ChatGPT: Google Identity / FedCM migration warnings
    "GSI_LOGGER",
    // Grok recruitment banner
    "Curious? Join us at https://x.ai/careers",
    // DuckDuckGo i18n noise
    "useTranslation:",
    // Microsoft Office survey styling deprecation
    "OfficeBrowserFeedback",
    // iframe sandbox same-origin warning (Microsoft 365)
    "both allow-scripts and allow-same-origin",
    // Microsoft Edge Actions capability flag
    "EdgeActionsCapabilityFlagDisabled",
    // Permissions Policy / local-network-access
    "local-network-access",
    // React hydration errors
    "Hydration",
    // Deprecated DOMNodeInserted mutation event (Gemini)
    "DOMNodeInserted",
)

/**
 * Creates and registers a [CefDisplayHandlerAdapter] on [browser] that:
 *  - Suppresses console messages matching any pattern in [SUPPRESSED_CONSOLE_PATTERNS]
 *  - Forwards all other messages to IntelliJ's [Logger] at INFO level
 *
 * Extracted from MyChatWindowFactory so any future CEF-embedding tool window
 * can suppress the same noisy patterns with a single call.
 *
 * @param browser  The browser instance to attach the handler to.
 * @param logger   The caller's own Logger, so log lines are attributed to the
 *                 correct class rather than to CefUtils itself.
 */
fun registerConsoleSuppressingDisplayHandler(browser: JBCefBrowser, project: Project, logger: Logger) {
    val handler = object : CefDisplayHandlerAdapter() {
        override fun onConsoleMessage(
            cefBrowser: CefBrowser,
            level: CefSettings.LogSeverity,
            message: String,
            source: String,
            line: Int
        ): Boolean {

            // Handle MyChat-specific upload diagnostics before general log suppression.
            val uploadPathPrefix = "MYCHAT_UPLOAD_PATH: "
            if (message.startsWith(uploadPathPrefix)) {
                /*
                 * Always consume this MyChat-specific console message. When diagnostic
                 * notifications are disabled, the message should not fall through into
                 * the normal CEF logging path.
                 */
                val notificationsEnabled =
                    MyChatSettings.getInstance().state.uploadDebugNotificationsEnabled

                if (notificationsEnabled) {
                    var notificationText = message.removePrefix(uploadPathPrefix)

                    /*
                     * Preserve the existing display behavior by removing the URL scheme.
                     * Consider retaining the scheme if the notification should display
                     * the complete URL exactly as emitted by JavaScript.
                     */
                    if (notificationText.startsWith("https://")) {
                        notificationText = notificationText.removePrefix("https://")
                    }

                    notify(project, "MyChat upload", notificationText)
                }

                return true
            }

            // Spam logs suppression logic.
            if (SUPPRESSED_CONSOLE_PATTERNS.any { message.contains(it) }) {
                return true  // suppress — don't forward to CEF's own logger
            }
            logger.info("CEF Console [$level] $message ($source:$line)")
            return false  // false = also let CEF handle it normally
        }
    }
    browser.jbCefClient.addDisplayHandler(handler, browser.cefBrowser)
}

private fun notify(project: Project, title: String, content: String) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("MyChat.Notification")
        .createNotification(
            title,
            content,
            NotificationType.INFORMATION
        )
        .notify(project)
}
