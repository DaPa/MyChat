package com.example.actions

import com.example.utils.getBrowser
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * IntelliJ action for refreshing the browser.
 * This action finds the JBCefBrowser via component key searching.
 */
class RefreshAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.getBrowser()?.cefBrowser?.reload()
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        return e.getBrowser()?.cefBrowser?.isLoading == false
    }
}
