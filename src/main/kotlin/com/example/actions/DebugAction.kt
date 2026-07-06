package com.example.actions

import com.example.utils.getBrowser
import com.intellij.openapi.actionSystem.AnActionEvent


/**
 * IntelliJ action for opening Chromium DevTools
 * for the embedded JBCefBrowser instance.
 */
class DebugAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.getBrowser()?.openDevtools()
    }
}
