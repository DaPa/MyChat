package com.example

import com.example.browser.BrowserLocator
import com.example.settings.MyChatIcons
import com.example.ui.MyChatWindow
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.example.services.MyChatProjectState

/**
 * Factory class registered in plugin.xml via <toolWindow factoryClass=...>.
 * IntelliJ instantiates this class and calls createToolWindowContent() when the
 * tool window is first shown.
 *
 * Execution flow:
 *  1. IntelliJ loads the plugin and reads plugin.xml
 *  2. Finds toolWindow factoryClass = MyChatWindowFactory
 *  3. Calls shouldBeAvailable() to decide if the window appears in the stripe
 *  4. Calls init() to set the stripe label
 *  5. Calls createToolWindowContent() when the user first opens the tool window
 *  6. MyChatWindow builds the Swing UI and embeds the JCEF browser
 */
class MyChatWindowFactory : ToolWindowFactory {

    companion object {
        /**
         * Tool window ID as registered in plugin.xml (`id="MyChatWindow"`).
         * Use this constant instead of hardcoding the ID string.
         */
        const val TOOL_WINDOW_ID = "MyChatWindow"
    }

    /**
     * Tracks whether JCEF has been safely pre-initialized by MyChatStartupActivity.
     * Returns false until MyChatStartupActivity sets jcefReady=true (post-full platform init).
     * Prevents createToolWindowContent() being called before JCEF services are ready.
     */
    override fun shouldBeAvailable(project: Project): Boolean =
        MyChatProjectState.getInstance(project).jcefReady

    /**
     * Called once when the tool window is registered, before content is created.
     * Used here to set the stripe button label from the i18n message bundle.
     * Note: init() runs even if the user never opens the tool window.
     */
    override fun init(toolWindow: ToolWindow) {
        val baseTitle = MyChatMessagesBundle.message("chatwindow.stripe.MyChatWindow")
        val version = PluginManager.getPluginByClass(this::class.java)?.version ?: ""
        toolWindow.stripeTitle = if (version.isNotEmpty()) "$baseTitle $version" else baseTitle

        toolWindow.setIcon(MyChatIcons.toolWindow)
        //LOG.info("init")
    }

    /**
     * Core entry point — called the first time the user opens the tool window.
     * Creates the UI and registers it as a content tab in the tool window.
     *
     * Guard: JCEF (Java Chromium Embedded Framework) is not available in all environments:
     *  - Headless environments (CI, remote, unit tests) have no display
     *  - Some JetBrains IDEs ship without JCEF (e.g., certain remote dev setups)
     * Returning early silently skips content creation; the tool window opens but stays empty.
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        //LOG.info("createToolWindowContent")
        /**
         * JCEF is safe to access here because MyChatStartupActivity has already pre-warmed
         * JBCefApp (called isSupported() + getInstance()) in a post-startup context where
         * all platform services including ProxyMigrationService are fully available.
         * By the time the user opens the tool window, <clinit> has already completed.
         */
        if (!JBCefApp.isSupported() || ApplicationManager.getApplication().isHeadlessEnvironment) {
            return
        }

        val myToolWindow = MyChatWindow(project)

        /**
         * ContentFactory wraps the Swing panel into IntelliJ's content tab system.
         * Parameters: (component, displayName, isLockable)
         * - displayName null = no tab title shown (single-tab tool windows typically omit it)
         * - isLockable = false means the content can be replaced/closed by the platform
         */
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        content.setDisposer(myToolWindow)
        content.putUserData(BrowserLocator.MY_CHAT_WINDOW_KEY, myToolWindow)
        toolWindow.contentManager.addContent(content)
    }

}  // class MyChatWindowFactory
