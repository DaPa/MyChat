package com.example.browser

import com.example.MyChatWindowFactory
import com.example.ui.MyChatWindow
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager

object BrowserLocator {

    val MY_CHAT_WINDOW_KEY = Key.create<MyChatWindow>("MyChatWindow")

    fun dataContext(project: Project): DataContext? {
        val toolWindow =
            ToolWindowManager.getInstance(project)
                .getToolWindow(MyChatWindowFactory.TOOL_WINDOW_ID)
                ?: return null

        val content = toolWindow.contentManager.contents.firstOrNull()
            ?: return null

        val myChatWindow = content.getUserData(MY_CHAT_WINDOW_KEY)
            ?: return null

        // Return a BGT-safe DataContext that pulls directly from the MyChatWindow instance
        // without traversing the Swing component hierarchy.
        return DataContext { dataId ->
            when (dataId) {
                BrowserDataKeys.BROWSER.name -> myChatWindow.browser
                BrowserDataKeys.CURRENT_SITE.name -> myChatWindow.currentSite
                BrowserDataKeys.FIND_BAR.name -> myChatWindow.findBar
                else -> null
            }
        }
    }
}
