package com.example.actions

import com.example.utils.getBrowser
import com.example.utils.getHomeUrl
import com.intellij.openapi.actionSystem.AnActionEvent

/**
 * IntelliJ action for navigating browser home.
 * This action finds the JBCefBrowser via component key searching.
 */
class HomeAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val browser = e.getBrowser() ?: return
        val homeUrl = e.getHomeUrl() ?: return

        browser.loadURL(homeUrl)
    }

    override fun isEnabled(e: AnActionEvent): Boolean {
        return e.getHomeUrl() != null
    }
}
