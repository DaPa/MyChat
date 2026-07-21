package com.dapa1.actions

import com.dapa1.utils.ComponentUtils.injectFilesViaJs
import com.dapa1.utils.getBrowser
import com.dapa1.utils.getCurrentSite
import com.dapa1.utils.resolveIoFiles
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger

/**
 * Sends the currently active editor file(s) to the MyChat browser.
 * Works from the editor tab context menu, main menu, or keyboard shortcut.
 */
class SendToMyChatAction : AbstractFileAction() {

    companion object {
        private val LOG = Logger.getInstance(SendToMyChatAction::class.java)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val hasRealFiles = e.resolveIoFiles().isNotEmpty()
        e.presentation.isVisible = hasRealFiles
        e.presentation.isEnabled = e.presentation.isEnabled && hasRealFiles
    }

    override fun actionPerformed(e: AnActionEvent) {
        val browser = e.getBrowser() ?: return
        val files = e.resolveIoFiles().takeIf { it.isNotEmpty() } ?: return
        val currentSite = e.getCurrentSite()

        LOG.info("SendToMyChat — files: ${files.map { it.name }}")
        injectFilesViaJs(browser, files, currentSite)
    }

}
