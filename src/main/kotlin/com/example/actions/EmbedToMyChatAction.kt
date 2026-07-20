package com.example.actions

import com.example.utils.ComponentUtils.injectContentAsMarkdown
import com.example.utils.ComponentUtils.injectSelectionAsMarkdown
import com.example.utils.getBrowser
import com.example.utils.resolveIoFiles
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
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

    override fun update(e: AnActionEvent) {
        super.update(e)
        if (!e.presentation.isVisible || !e.presentation.isEnabled) return

        val editor = e.getData(CommonDataKeys.EDITOR)
        if (editor != null && e.place == ActionPlaces.EDITOR_POPUP && editor.selectionModel.hasSelection()) {
            e.presentation.text = "- Embed Selection"
        } else {
            e.presentation.text = "- Embed To"
        }
    }

    override fun actionPerformed(e: AnActionEvent) {
        val browser = e.getBrowser() ?: return

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

        val files = e.resolveIoFiles().takeIf { it.isNotEmpty() } ?: return

        LOG.info("EmbedToMyChatAction — files: ${files.map { it.name }}")
        injectContentAsMarkdown(browser, files)
    }

}
