package com.example.actions

import com.example.MyChatWindowFactory
import com.example.utils.resolveIoFile
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager

abstract class AbstractFileAction : AbstractBrowserAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project ?: run { e.presentation.isEnabledAndVisible = false; return }
        val toolWindow = ToolWindowManager.getInstance(project)
            .getToolWindow(MyChatWindowFactory.TOOL_WINDOW_ID)
        val hasFile = e.resolveIoFile() != null
        e.presentation.isVisible = hasFile
        e.presentation.isEnabled = hasFile && toolWindow != null
    }
}
