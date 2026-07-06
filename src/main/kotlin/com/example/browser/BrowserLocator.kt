package com.example.browser

import com.example.MyChatWindowFactory
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager

object BrowserLocator {

    fun dataContext(project: Project): DataContext? {
        val toolWindow =
            ToolWindowManager.getInstance(project)
                .getToolWindow(MyChatWindowFactory.TOOL_WINDOW_ID)
                ?: return null

        val content = toolWindow.contentManager.contents.firstOrNull()
            ?: return null

        val component = content.component

        // ✅ Modern DataContext usage
        return DataManager.getInstance().getDataContext(component)
    }
}
