package com.example.actions

import com.example.MyChatWindowFactory
import com.example.utils.getBrowser
import com.example.utils.resolveIoFiles
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

/**
 * Base class for actions that operate on files (e.g., Send To, Embed To).
 * Handles the common visibility and enablement logic based on file availability
 * and the browser state.
 */
abstract class AbstractFileAction : AbstractBrowserAction() {

    /**
     * Updates the action presentation. The action is visible only if files are available
     * and enabled only if the MyChat tool window is initialized and the browser is ready.
     */
    override fun update(e: AnActionEvent) {
        val project = e.project ?: run { e.presentation.isEnabledAndVisible = false; return }
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(MyChatWindowFactory.TOOL_WINDOW_ID)
        val files = e.resolveIoFiles()
        val hasFiles = files.isNotEmpty()
        e.presentation.isVisible = hasFiles
        e.presentation.isEnabled = hasFiles && toolWindow != null && e.getBrowser() != null
    }
}
