package com.example.actions

import com.example.utils.getBrowser
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * IntelliJ action to stopping browser loading.
 * This action finds the JBCefBrowser via component key searching.
 */
class StopAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.getBrowser()?.cefBrowser?.stopLoad()
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        return e.getBrowser()?.cefBrowser?.isLoading == true
    }
}
