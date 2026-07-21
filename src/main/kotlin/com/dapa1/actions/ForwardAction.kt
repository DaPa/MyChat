package com.dapa1.actions

import com.dapa1.utils.getBrowser
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * IntelliJ action for navigating browser history forward.
 * This action finds the JBCefBrowser via component key searching.
 */
class ForwardAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.getBrowser()?.cefBrowser?.goForward()
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        return e.getBrowser()?.cefBrowser?.canGoForward() == true
    }
}

