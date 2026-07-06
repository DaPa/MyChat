package com.example.actions

import com.example.browser.BrowserDataKeys
import com.example.browser.BrowserLocator
import com.example.utils.ComponentUtils.injectFilesViaJs
import com.example.utils.resolveIoFile
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger

/**
 * Sends the currently active editor file to the MyChat browser.
 * Works from the editor tab context menu, main menu, or keyboard shortcut.
 */
class SendToMyChatAction : AbstractFileAction() {

    companion object {
        private val LOG = Logger.getInstance(SendToMyChatAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val ioFile = e.resolveIoFile() ?: return
        val dataContext = BrowserLocator.dataContext(project) ?: return
        val browser = BrowserDataKeys.BROWSER.getData(dataContext) ?: return
        val currentSite = BrowserDataKeys.CURRENT_SITE.getData(dataContext) ?: return

        LOG.info("SendToMyChat — file: ${ioFile.name}")
        injectFilesViaJs(browser, listOf(ioFile), currentSite)
    }

}
