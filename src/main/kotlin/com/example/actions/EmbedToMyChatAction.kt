package com.example.actions

import com.example.browser.BrowserDataKeys
import com.example.browser.BrowserLocator
import com.example.utils.ComponentUtils.injectContentAsMarkdown
import com.example.utils.resolveIoFile
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger

/**
 * Embed the currently active editor file to the MyChat browser.
 * Works from the editor tab context menu, main menu, or keyboard shortcut.
 */
// TODO: ChatGPT retains previous embedded text when re-opening the IDE. How to clean it?
class EmbedToMyChatAction : AbstractFileAction() {

    companion object {
        private val LOG = Logger.getInstance(EmbedToMyChatAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val ioFile = e.resolveIoFile() ?: return
        val dataContext = BrowserLocator.dataContext(project) ?: return
        val browser = BrowserDataKeys.BROWSER.getData(dataContext) ?: return

        LOG.info("EmbedToMyChatAction — file: ${ioFile.name}")
        injectContentAsMarkdown(browser, listOf(ioFile))
    }

}
