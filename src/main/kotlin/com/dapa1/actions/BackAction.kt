package com.dapa1.actions

import com.dapa1.utils.getBrowser
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * IntelliJ action for navigating browser history backward.
 *
 * Responsibilities:
 * - Resolve JBCefBrowser from the event
 * - Enable action only when navigation is possible
 * - Trigger back navigation safely
 */
class BackAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.getBrowser()?.cefBrowser?.goBack()
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        return e.getBrowser()?.cefBrowser?.canGoBack() == true
    }
}
