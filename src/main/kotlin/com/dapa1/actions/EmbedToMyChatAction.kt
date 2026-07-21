package com.dapa1.actions

import com.dapa1.utils.ComponentUtils.injectContentAsMarkdown
import com.dapa1.utils.ComponentUtils.injectSelectionAsMarkdown
import com.dapa1.utils.getBrowser
import com.dapa1.utils.resolveIoFiles
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger

/**
 * Embed the currently active editor file(s) to the MyChat browser.
 * Works from the editor tab context menu, main menu, or keyboard shortcut.
 */
class EmbedToMyChatAction : AbstractFileAction() {

    companion object {
        private val LOG = Logger.getInstance(EmbedToMyChatAction::class.java)
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (!e.presentation.isVisible || !e.presentation.isEnabled) return

        e.presentation.text = "Embed To Chat"
        val hasRealFiles = e.resolveIoFiles().isNotEmpty()
        var enable = hasRealFiles
        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null && e.place == ActionPlaces.EDITOR_POPUP && editor.selectionModel.hasSelection()) {
            enable = true
            e.presentation.text = "Embed Selection"
        }
        e.presentation.isVisible = enable
        e.presentation.isEnabled = e.presentation.isEnabled && enable
    }

    override fun actionPerformed(e: AnActionEvent) {
        val browser = e.getBrowser() ?: return

        // Priority 1: If there is an active selection in the editor, embed only that.
        val editor = e.getData(CommonDataKeys.EDITOR)
        val selectionModel = editor?.selectionModel
        if (selectionModel != null && selectionModel.hasSelection()) {
            val selectedText = selectionModel.selectedText
            val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
            if (selectedText != null && virtualFile != null) {
                LOG.info("EmbedToMyChatAction — selection from: ${virtualFile.name}")
                injectSelectionAsMarkdown(browser, virtualFile.name, selectedText)
                return
            }
        }

        // Priority 2: Fallback to embedding the entire file(s).
        val files = e.resolveIoFiles().takeIf { it.isNotEmpty() } ?: return

        LOG.info("EmbedToMyChatAction — files: ${files.map { it.name }}")
        injectContentAsMarkdown(browser, files)
    }

}
