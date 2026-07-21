package com.dapa1.browser

import com.dapa1.MyChatWindowFactory
import com.dapa1.ui.MyChatWindow
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindowManager

object BrowserLocator {

    val MY_CHAT_WINDOW_KEY = Key.create<MyChatWindow>("MyChatWindow")

    fun dataContext(project: Project): DataContext? {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(MyChatWindowFactory.TOOL_WINDOW_ID)
                ?: return null

        // Avoid triggering content creation on a background thread.
        // On BGT, toolWindow.contentManager property access (getContentManager())
        // would trigger createContentIfNeeded(), which requires EDT.
        val contentManager = if (ApplicationManager.getApplication().isDispatchThread) {
            toolWindow.contentManager
        } else {
            toolWindow.contentManagerIfCreated
        } ?: return null

        val content = contentManager.contents.firstOrNull()
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
