package com.dapa1.actions

import com.dapa1.MyChatWindowFactory
import com.dapa1.utils.resolveVirtualFiles
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
        val hasFiles = e.resolveVirtualFiles().isNotEmpty()
        e.presentation.isVisible = hasFiles
        e.presentation.isEnabled = hasFiles && toolWindow != null && toolWindow.isAvailable
    }
}
