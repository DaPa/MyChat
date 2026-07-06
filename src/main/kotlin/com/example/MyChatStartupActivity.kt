package com.example

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.jcef.JBCefApp
import java.io.File

/**
 * Runs after the IDE is fully initialized — all services including ProxyMigrationService
 * are available by this point.
 *
 * Why this exists:
 *  During IDE startup, ToolWindowSetInitializer.registerToolWindows() calls
 *  createToolWindowContent() for any tool window marked as auto-shown. This happens
 *  inside the startup flush — before postStartupActivity runs and before all platform
 *  services are ready.
 *
 *  JBCefBrowser construction triggers JBCefApp$Holder.<clinit> (static init), which
 *  accesses ProxyMigrationService via HttpConfigurable. That service isn't ready yet,
 *  causing: "Class initialization must not depend on services"
 *
 * Solution (two parts):
 *  1. MyChatWindowFactory.shouldBeAvailable() returns false until jcefReady = true.
 *     This prevents the tool window from being auto-shown during startup, so
 *     createToolWindowContent() is never called in the dangerous window.
 *
 *  2. This activity runs post-startup, sets jcefReady = true, and calls
 *     toolWindow.setAvailable(true) — making the stripe button appear.
 *     From this point, JCEF can be safely initialized when the user opens the window.
 *
 * DumbAware: runs during indexing, no need to wait for smart mode.
 */
class MyChatStartupActivity : ProjectActivity, DumbAware {

    companion object {
        private val LOG = Logger.getInstance(MyChatStartupActivity::class.java)
    }

    override suspend fun execute(project: Project) {
        // startup cleanup in MyChatStartupActivity to catch any leftovers from a previous crashed session
        ApplicationManager.getApplication().executeOnPooledThread {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "MyChat")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { it.delete() }
                LOG.info("MyChatStartupActivity — cleaned up ${tempDir.path}")
            }
        }

        // All platform services are ready at this point.
        // Touch JCEF on the EDT — JBCefApp is EDT-bound.
        ApplicationManager.getApplication().invokeLater {

            if (!JBCefApp.isSupported()) {
                LOG.warn("JCEF not supported on this platform — MyChat tool window will not be available.")
                return@invokeLater
            }

            // Get instance (this initializes JCEF if not already done)
            val app = JBCefApp.getInstance()

            val settings = app.cefSettings
            if (settings == null) {
                LOG.warn("JCEF settings not available — cannot configure cookie persistence.")
                return@invokeLater
            }

            // Log the path IntelliJ assigned — useful for locating the Cookies file on disk.
            // Typically: %LOCALAPPDATA%\JetBrains\IntelliJIdea<ver>\jcef_cache (Windows)
            val path = settings.cache_path
            LOG.info("JCEF cache_path = $path")

            // IntelliJ already sets cache_path — do NOT overwrite it.
            // Only set persist_session_cookies, which IntelliJ never sets (defaults to false).
            if (!settings.persist_session_cookies) {
                settings.persist_session_cookies = true
                LOG.info("persist_session_cookies enabled, cache at $path")
            }

            LOG.info("JCEF ready — marking MyChat tool window available.")

            // Set the flag so shouldBeAvailable() now returns true.
            myChatJcefReady = true

            ToolWindowManager.getInstance(project)
                .getToolWindow(MyChatWindowFactory.TOOL_WINDOW_ID)?.isAvailable = true
        }
    }
}
