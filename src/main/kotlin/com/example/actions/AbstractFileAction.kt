package com.example.actions

import com.example.MyChatWindowFactory
import com.example.utils.getBrowser
import com.example.utils.resolveIoFiles
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

abstract class AbstractFileAction : AbstractBrowserAction() {

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
