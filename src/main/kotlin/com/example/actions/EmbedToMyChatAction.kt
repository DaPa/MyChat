package com.example.actions

import com.example.utils.ComponentUtils.injectContentAsMarkdown
import com.example.utils.getBrowser
import com.example.utils.resolveIoFiles
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger

/**
 * Embed the currently active editor file(s) to the MyChat browser.
 * Works from the editor tab context menu, main menu, or keyboard shortcut.
 */
// TODO: ChatGPT retains previous embedded text when re-opening the IDE. How to clean it?
class EmbedToMyChatAction : AbstractFileAction() {

    companion object {
        private val LOG = Logger.getInstance(EmbedToMyChatAction::class.java)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val browser = e.getBrowser() ?: return
        val files = e.resolveIoFiles().takeIf { it.isNotEmpty() } ?: return

        LOG.info("EmbedToMyChatAction — files: ${files.map { it.name }}")
        injectContentAsMarkdown(browser, files)
    }

}
