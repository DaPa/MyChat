package com.dapa1.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.keymap.impl.ui.KeymapPanel
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.ui.SearchTextField
import java.awt.Container
import java.awt.Window

/**
 * IntelliJ action that navigates to: IDE Settings → Keymap → Plugins → MyChat
 *
 * Steps performed:
 *  1. Starts a background polling thread
 *  2. Opens the Settings dialog (blocks the EDT — modal)
 *  3. Polling thread finds the dialog, locates the Keymap filter field, and injects "MyChat"
 *  4. Keeps re-injecting until the text is stable (the panel clears it during initialization)
 *
 * Why polling instead of a callback/listener:
 *  - showSettingsDialog() is modal and blocks the EDT until the dialog is closed
 *  - There is no public IntelliJ API to hook into the Settings dialog lifecycle
 *  - A background thread is the only way to act concurrently with the modal dialog
 *
 * ❗ Fragile points (may break on IntelliJ version upgrades):
 *  - findKeymapSearchFieldInOpenSettings() searches all visible windows for KeymapPanel
 *  - findSearchFieldUnderKeymapPanel() relies on the Swing component hierarchy
 *  - KeymapPanel is an internal IntelliJ class (com.intellij.openapi.keymap.impl.ui)
 */
class OpenMyChatKeymapAction : AbstractBrowserAction() {

    companion object {
        private const val FILTER_TEXT = "MyChat"
        private const val MAX_ATTEMPTS = 15          // 15 × 200ms = 3s max total wait
        private const val POLL_INTERVAL_MS = 200L
        private const val STABLE_THRESHOLD = 4       // text must survive 4 consecutive checks (~800ms)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project

        // Must start the thread BEFORE showSettingsDialog(), because showSettingsDialog()
        // blocks the EDT (it's modal). The thread runs concurrently while the dialog is open.
        startFilterInjectionThread()

        // Blocks the EDT until the user closes the Settings dialog.
        // This is why all Swing interaction must happen on the background thread via invokeAndWait.
        ShowSettingsUtil.getInstance().showSettingsDialog(project, KeymapPanel::class.java)
    }

    /**
     * Starts a daemon thread that polls for the Settings dialog and injects the keymap filter.
     *
     * Why a raw Thread instead of coroutines or ExecutorService:
     *  - No coroutine dependency needed for a simple one-shot background task
     *  - The thread is short-lived (exits after injection or timeout)
     *
     * Thread lifecycle:
     *  Phase 1 — Discovery: poll until the Settings dialog and its KeymapPanel are visible
     *  Phase 2 — Injection: set the filter text, re-set if cleared, exit when stable
     */
    private fun startFilterInjectionThread() {
        Thread {
            var field: SearchTextField? = null
            var attempts = 0
            var stable = 0
            var injected = false

            while (attempts++ < MAX_ATTEMPTS) {
                Thread.sleep(POLL_INTERVAL_MS)

                // All Swing access must happen on the EDT.
                // invokeAndWait() blocks this background thread until the EDT task completes,
                // so the result (field / stable counter) is safely readable after the call.
                javax.swing.SwingUtilities.invokeAndWait {
                    val f = field  // local snapshot required for Kotlin smart cast (field is a var)

                    if (f == null) {
                        // Phase 1: dialog not found yet — keep searching
                        field = findKeymapSearchFieldInOpenSettings()
                    } else {
                        // Phase 2: dialog found — maintain the filter text
                        if (f.text != FILTER_TEXT) {
                            // Text was cleared (panel re-initializing) — re-inject
                            stable = 0
                            f.text = FILTER_TEXT
                            // Firing actionListeners simulates pressing Enter in the search field,
                            // which triggers the KeymapPanel's internal filter/search logic.
                            // Without this, the text appears in the box, but the tree isn't filtered.
                            f.textEditor.actionListeners.forEach {
                                it.actionPerformed(
                                    java.awt.event.ActionEvent(f.textEditor, 0, "")
                                )
                            }
                        } else {
                            // Text survived this cycle — increment stability counter
                            stable++
                        }
                    }
                }

                // Consider injection successful only after the text has been stable for
                // STABLE_THRESHOLD consecutive polls. This guards against the panel clearing
                // the field during its own initialization (observed to happen 1-2 times).
                if (stable > STABLE_THRESHOLD) {
                    injected = true
                    break
                }
            }

            // Logs appear in: <IDE system dir>/log/idea.log
            // Example: C:\Users\<user>\AppData\Local\JetBrains\IntelliJIdea2026.1\log\idea.log
            // 2026-06-03 20:07:29,014 [39137] INFO - STDOUT - OpenMyChatKeymapAction: injected 'MyChat' after 8 attempts
            if (injected) {
                println("OpenMyChatKeymapAction: injected '$FILTER_TEXT' after $attempts attempts")
            } else {
                println("OpenMyChatKeymapAction: failed after $attempts attempts — stable=$stable field=$field")
            }
        }.also { it.isDaemon = true }  // daemon = thread won't prevent JVM shutdown if IDE exits
            .start()
    }

    // --- Swing tree traversal ---

    /**
     * Entry point: scans all open AWT windows for the IntelliJ Settings dialog by
     * looking for a KeymapPanel, then finds the SearchTextField inside it.
     *
     * Returns null if the dialog isn't open yet or KeymapPanel isn't visible yet.
     * The caller is expected to retry (polling loop).
     */
    private fun findKeymapSearchFieldInOpenSettings(): SearchTextField? {
        // We search through all visible windows for a KeymapPanel. This is more robust
        // than trying to identify the SettingsDialog via reflection on internal
        // DialogWrapper fields (which broke in 2026.2).
        return Window.getWindows()
            .asSequence()
            .filter { it.isVisible }.firstNotNullOfOrNull { findSearchFieldUnderKeymapPanel(it as Container) }
    }

    /**
     * Recursively searches the component tree for a KeymapPanel,
     * then delegates to findFirstSearchTextField() within that subtree.
     *
     * Why restrict to KeymapPanel subtree:
     *  - The Settings dialog contains at least two SearchTextField instances:
     *    (a) the global settings search box (top-left "Q-" field)
     *    (b) the Keymap filter field (inside KeymapPanel)
     *  - We must target (b) specifically
     *  - Searching the whole tree would match (a) first (DFS order)
     *
     * Note: findAndFillSearchTextField() is a named intermediate step intentionally kept
     * separate for clarity and to make it easier to add KeymapPanel-specific logic later
     * (e.g., waiting for the panel to finish loading before searching).
     */
    private fun findSearchFieldUnderKeymapPanel(container: Container): SearchTextField? {
        for (child in container.components) {
            if (child is KeymapPanel) {
                // Found the KeymapPanel — now look for the filter field inside it only
                return findFirstSearchTextField(child)
            }
            if (child is Container) {
                val result = findSearchFieldUnderKeymapPanel(child)
                if (result != null) return result
            }
        }
        return null
        // Returns null if KeymapPanel isn't in the tree yet (panel not yet rendered).
        // The polling loop will retry in the next cycle.
    }

    /**
     * Depth-first search for the first SearchTextField inside a container subtree.
     *
     * Stops at the first match — there should be exactly one SearchTextField
     * inside KeymapPanel (the filter field confirmed via UI Inspector).
     *
     * DFS traversal order matches the component rendering order,
     * so the first found is always the topmost/leftmost visible field.
     */
    private fun findFirstSearchTextField(container: Container): SearchTextField? {
        for (child in container.components) {
            if (child is SearchTextField) return child
            if (child is Container) {
                val result = findFirstSearchTextField(child)
                if (result != null) return result
            }
        }
        return null
    }

}  // class OpenMyChatKeymapAction