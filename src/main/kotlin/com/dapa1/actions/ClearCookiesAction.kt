package com.dapa1.actions

import com.dapa1.utils.getBrowser
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * IntelliJ action for clearing browser cookies.
 *
 * Responsibilities:
 * - Resolve JBCefBrowser
 * - Clear all cookies via CefCookieManager
 */
class ClearCookiesAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        // Deletes all cookies (null domain + null name = global wipe)
        e.getBrowser()?.jbCefCookieManager?.deleteCookies(null, null)
    }
}
