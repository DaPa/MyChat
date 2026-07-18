package com.example

import com.example.browser.BrowserDataKeys
import com.example.settings.MyChatIcons
import com.example.settings.MyChatSettings
import com.example.ui.BrowserFindBar
import com.example.ui.MyThemeListener
import com.example.utils.ComponentUtils.injectFilesViaJs
import com.example.utils.acceptOrRejectFileDrop
import com.example.utils.extractVirtualFiles
import com.example.utils.normalizeUrl
import com.example.utils.registerConsoleSuppressingDisplayHandler
import com.intellij.ide.dnd.DnDEvent
import com.intellij.ide.dnd.DnDManager
import com.intellij.ide.dnd.DnDNativeTarget
import com.intellij.ide.setToolTipText
import com.intellij.ide.ui.LafManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.UiDataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.JBColor
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.jcef.JBCefApp
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.awt.Component
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.io.File
import javax.swing.DefaultListCellRenderer
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.SwingUtilities

/**
 * Tracks whether JCEF has been safely pre-initialized by MyChatStartupActivity.
 *
 * Why top-level (not in companion object):
 *  IntelliJ warns that ToolWindowFactory companion objects may only contain a logger
 *  and compile-time constants (const val). A @Volatile var is not a constant, so it
 *  must live outside the companion. Top-level scope in Kotlin compiles to a static
 *  field on a file-level class, which is equally accessible from MyChatStartupActivity.
 *
 * Why @Volatile:
 *  MyChatStartupActivity sets this from the EDT (via invokeLater).
 *  shouldBeAvailable() may be read from any thread by the platform.
 *  @Volatile ensures writes are immediately visible across threads without locking.
 */
@Volatile
var myChatJcefReady: Boolean = false

/**
 * Factory class registered in plugin.xml via <toolWindow factoryClass="...">.
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

        /**
         * Number of columns for the URL input field (controls its preferred width).
         * JTextField uses character-width units, not pixels.
         */
        private const val INPUT_FIELD_COLUMNS = 25

        private val LOG = Logger.getInstance(MyChatWindowFactory::class.java)
    }

    /**
     * Returns false until MyChatStartupActivity sets jcefReady=true (post-full platform init).
     * Prevents createToolWindowContent() being called before JCEF services are ready.
     */
    override fun shouldBeAvailable(project: Project) = myChatJcefReady

    /**
     * Called once when the tool window is registered, before content is created.
     * Used here to set the stripe button label from the i18n message bundle.
     * Note: init() runs even if the user never opens the tool window.
     */
    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = MyChatMessagesBundle.message("chatwindow.stripe.MyChatWindow")
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
        toolWindow.contentManager.addContent(content)
    }

    /**
     * Encapsulates all UI logic for the chat tool window.
     *
     * Separated from MyChatWindowFactory to keep factory responsibilities (registration,
     * lifecycle hooks) distinct from UI construction responsibilities.
     *
     * Owns:
     *  - The JCEF browser instance
     *  - The URL input field
     *  - The navigation toolbar
     *  - The JS injection logic
     *  - Site config matching (URL → button selector)
     */
    class MyChatWindow(private val project: Project): Disposable {  // class MyChatWindow

        companion object {
            private val LOG = Logger.getInstance(MyChatWindow::class.java)
        }

        /**
         * Finds the best matching SiteConfig for the given URL by normalized prefix match.
         *
         * Example: currentUrl = "https://chatgpt.com/c/abc123"
         *   site.url = "https://chatgpt.com" → normalized = "chatgpt.com" → matches ✅
         *   site.url = "https://google.com"  → normalized = "google.com"  → no match ❌
         *
         * Uses normalizeUrl() from UrlUtils — both sides are lowercased and stripped, so no
         * ignoreCase flag is needed on startsWith().
         *
         * Current strategy: first match wins (list order matters).
         * Alternative (commented out below): longest match wins — more precise when one
         * site URL is a prefix of another (e.g. "m365.cloud.microsoft" vs. "m365.cloud.microsoft/chat").
         */
        private fun findMatchingSite(currentUrl: String): MyChatSettings.SiteConfig? {
            val normalizedCurrent = normalizeUrl(currentUrl)
            return MyChatSettings.getInstance().state.sites.firstOrNull { site ->
                normalizedCurrent.startsWith(normalizeUrl(site.url))
            }

            /**
             * Alternative: longest match wins (more precise when overlapping prefixes exist)
             * return MyChatSettings.getInstance().state.sites
             *     .filter { normalizeUrl(currentUrl).startsWith(normalizeUrl(it.url)) }
             *     .maxByOrNull { it.url.length }
             */
        }

        /**
         * Resolves and caches the button selector for the given URL.
         * Called on every page load (onLoadEnd) to handle navigation between sites.
         *
         * Reads live from MyChatSettings so that settings changes take effect on the
         * next page load without requiring a tool window restart.
         *
         * Sets currentPlusBtn to "" if no site matches — the JS will then query
         * '[data-testid=""]' which matches nothing, effectively a no-op.
         */
        private fun updateSiteConfig(url: String) {
            val matched = findMatchingSite(url)
            if (matched != null) {
                LOG.info("updateSiteConfig — matched site: ${matched.url}")
                currentSite = matched
            } else {
                LOG.info("updateSiteConfig — no matching site for URL: $url")
            }
        }

        /**
         * Returns the first configured site, falling back to ChatGPT defaults.
         * Used to determine the initial URL to load when the browser is created.
         *
         * Reads live from MyChatSettings each time so it reflects any settings change.
         *
         * TODO: consider exposing a "default site" setting in MyChatConfigurable
         * rather than always using the first item.
         */
        private var currentSite: MyChatSettings.SiteConfig =
            MyChatSettings.getInstance().state.sites.firstOrNull() ?: MyChatSettings.SiteConfig(
                "",
                "ChatGPT",
                "https://chatgpt.com"
            )

        /**
         * The JCEF browser instance. Initialized as the very first statement inside
         * the content apply{} block, so it is available to all subsequent statements.
         *
         * Lifecycle: created once, lives as long as the tool window content lives.
         * JCEF browsers hold native resources — they are disposed by the JBCefBrowser
         * itself when the parent component is removed from the hierarchy.
         */
        private lateinit var browser: JBCefBrowser
        private lateinit var findBar: BrowserFindBar
        private var dndTarget: DnDNativeTarget? = null

        // Captured once, before the browser/content are built — this is the theme
        // baked into the current CEF process via --force-dark-mode/--force-light-mode.
        // Must be declared BEFORE `content`, since building `content` synchronously
        // triggers MyThemeListener's immediate callback, which reads this value.
        private val cefLaunchDarkState = LafManager.getInstance().currentUIThemeLookAndFeel.isDark
        private var themeMismatchNotified = false

        /**
         * The root Swing panel of the tool window content.
         *
         * Built using Kotlin's apply{} builder pattern: the JPanel is created,
         * then all child components are added inside the apply block.
         *
         * Layout: BorderLayout
         *   NORTH → topPanel (URL field plus toolbar)
         *   CENTER → browser.component (fills remaining space)
         */
        private val content: JPanel = createContentPanel()

        /**
         * Creates the root panel and wires DataProvider + UI setup.
         */
        private fun createContentPanel(): JPanel =
            object : JPanel(BorderLayout()), UiDataProvider {

                /**
                 * Supplies data to IntelliJ's DataContext.
                 */
                override fun uiDataSnapshot(sink: DataSink) {
                    sink[BrowserDataKeys.BROWSER] = browser
                    sink[BrowserDataKeys.CURRENT_SITE] = currentSite
                    sink[BrowserDataKeys.FIND_BAR] = findBar
                }

            }.apply {
                setupUi(this)
            }

        private fun showThemeRestartNotification() {
            val notification = NotificationGroupManager.getInstance()
                .getNotificationGroup("MyChat.ThemeChangeNotification")
                .createNotification(
                    "MyChat browser theme is out of sync",
                    "The IDE theme changed. MyChat's embedded browser needs an IDE restart " +
                        "to match the new theme for sites using \"System\" appearance.",
                    NotificationType.INFORMATION
                )

            notification.addAction(object : NotificationAction("Restart IDE") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()
                    ApplicationManager.getApplication().restart()
                }
            })

            notification.addAction(object : NotificationAction("Dismiss") {
                override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                    notification.expire()
                }
            })

            // ModalityState.any() bypasses the "no UI updates while a modal dialog is open"
            // guard, so the balloon shows immediately even if the theme change came from
            // inside the modal Settings dialog (live preview), instead of being queued
            // until the dialog closes.
            ApplicationManager.getApplication().invokeLater(
                { notification.notify(project) },
                ModalityState.any()
            )
        }

        /**
         * Contains all UI initialization logic.
         */
        private fun setupUi(panel: JPanel) {

            browser = JBCefBrowser(currentSite.url)
            Disposer.register(this, browser)

            //println("cefLaunchDarkState   = $cefLaunchDarkState")
            //println("debug port = ${System.getProperty("ide.browser.jcef.debug.port")}")
            //println("cef args   = ${System.getProperty("ide.browser.jcef.command.line.args")}")
            //LOG.info("JCEF debug port = ${System.getProperty("ide.browser.jcef.debug.port")}")
            //LOG.info("JCEF command line args = ${System.getProperty("ide.browser.jcef.command.line.args")}")

            MyThemeListener(this) { dark ->

                LOG.info("MyThemeListener callback fired. dark=$dark, cefLaunchDarkState=$cefLaunchDarkState")

                // New: warn when the IDE theme has drifted from what CEF was launched with.
                if (dark != cefLaunchDarkState) {
                    if (!themeMismatchNotified) {
                        showThemeRestartNotification()
                        themeMismatchNotified = true
                    }
                } else {
                    // User flipped back to the original theme — no restart needed anymore.
                    themeMismatchNotified = false
                }
            }

            /**
             * Register the console-suppressing display handler once here, not inside onLoadEnd.
             * Registering inside onLoadEnd would accumulate N handlers after N navigations,
             * causing duplicate log lines that multiply without a bound.
             * Handler logic and the suppression pattern list live in CefUtils.
             */
            registerConsoleSuppressingDisplayHandler(browser, LOG)

            val combo = ComboBox<MyChatSettings.SiteConfig>()
            combo.addActionListener {
                val selected = combo.selectedItem as? MyChatSettings.SiteConfig ?: return@addActionListener
                browser.loadURL(selected.url)
                updateSiteConfig(selected.url)
            }

            combo.renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
                ): Component {
                    val label = super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus
                    ) as JLabel

                    val site = value as? MyChatSettings.SiteConfig
                    if (site != null) {
                        label.text = site.name.ifEmpty { site.url }
                        // HtmlChunk.text() escapes the URL for safe HTML display.
                        label.setToolTipText(HtmlChunk.text(site.url))
                        /**
                         * resolveIcon() loads from iconPath on the first call, then caches.
                         * Replaces the old direct site.icon field which no longer exists —
                         * Icon is now @Transient and accessed only via resolveIcon().
                         */
                        label.icon = site.resolveIcon()
                    }
                    return label
                }
            }  // combo.renderer

            // Populate combo from live settings — reads fresh on content creation.
            MyChatSettings.getInstance().state.sites.forEach { site -> combo.addItem(site) }

            /**
             * URL input field — shows the current URL and allows manual navigation.
             * INPUT_FIELD_COLUMNS controls preferred width (character units, not pixels).
             */
            val urlTextField = JTextField(currentSite.url, INPUT_FIELD_COLUMNS).apply {

                // Press Enter → navigate to typed URL
                addActionListener {
                    val customUrl = text.trim()
                    if (customUrl.isNotEmpty()) {
                        browser.loadURL(customUrl)
                    }
                }

                // Click into field → select all text (allows immediate overtyping)
                addFocusListener(object : FocusAdapter() {
                    override fun focusGained(e: FocusEvent) {
                        selectAll()
                    }
                })
            }

            /**
             * Build the action group for the navigation toolbar.
             * Action IDs must match the <action id="..."> values in plugin.xml.
             * getAction() returns null if the ID is not registered in plugin.xml.
             * DefaultActionGroup.add() is @NotNull — passing null throws IllegalArgumentException.
             * Use addIfNotNull() to safely skip unregistered actions rather than crash.
             */
            val actionManager = ActionManager.getInstance()
            val actionGroup = DefaultActionGroup().apply {
                fun addIfNotNull(id: String) {
                    actionManager.getAction(id)?.let { add(it) }
                }
                addIfNotNull("MyChat.FindInBrowser")
                addIfNotNull("MyChat.Back")
                addIfNotNull("MyChat.Forward")
                addIfNotNull("MyChat.Refresh")
                addIfNotNull("MyChat.Stop")
                addIfNotNull("MyChat.Home")
                addIfNotNull("MyChat.ClearCookies")
                addIfNotNull("MyChat.Debug")
                addIfNotNull("MyChat.OpenSettings")
                addIfNotNull("MyChat.OpenKeymap")
            }

            /**
             * Create the toolbar from the action group.
             * "MyChatToolbar" is the toolbar place ID — used by IntelliJ for customization context.
             * horizontal = true → buttons arranged in a row.
             * setMiniMode(true) → compact icon-only buttons (no text labels).
             * targetComponent = browser.component → DataContext source for toolbar actions;
             * this is how getBrowser() in ActionUtils finds the browser via CONTEXT_COMPONENT.
             */
            val toolbar = actionManager.createActionToolbar("MyChatToolbar", actionGroup, true)
            toolbar.setMiniMode(true)
            toolbar.targetComponent = browser.component

            findBar = BrowserFindBar(browser)

            // Top bar: URL field stretches to fill available width; toolbar is pinned to the right.
            val topPanel = JPanel(BorderLayout()).apply {
                add(combo, BorderLayout.WEST)
                add(urlTextField, BorderLayout.CENTER)
                add(toolbar.component, BorderLayout.EAST)
            }

            val northPanel = JPanel(BorderLayout()).apply {
                add(topPanel, BorderLayout.NORTH)
                add(findBar, BorderLayout.SOUTH) // ✅ find bar below toolbar
            }
            panel.add(northPanel, BorderLayout.NORTH)
            panel.add(browser.component, BorderLayout.CENTER)

            // Use a theme-aware neutral background rather than hardcoded DARK_GRAY,
            // which looks wrong in the light theme. In practice rarely visible since
            // the browser fills CENTER and topPanel fills NORTH.
            panel.background = JBColor.PanelBackground

            /**
             * --- Drag-and-Drop support ---
             *
             * Problem: JCEF renders via a native window layered on top of the Swing component.
             * When IntelliJ's project tree initiates a Swing DnD drag, the native CEF surface
             * intercepts the mouse events before Swing sees them — the drop never arrives.
             *
             * Solution: install a Swing DropTarget on browser.component that intercepts the
             * drop event at the Swing level, extracts the file paths from the transferable,
             * and injects them into the page via JavaScript (simulating a file drop on the
             * chat input element). This works because:
             * 1. Swing DnD operates at the Java level, above the native CEF overlay
             * 2. We handle the drop ourselves rather than forwarding it to the native layer
             * 3. JavaScript can programmatically create a DataTransfer and dispatch a drop event
             *
             * Limitation: the JS injection approach works for sites that handle the 'drop' event
             * on their input/composer element. Most AI chat UIs (ChatGPT, Claude, Copilot) do.
             * Sites that rely on the browser's native file picker won't benefit.
             */
            setupFileDragAndDrop(browser)
            setupIntelliJDragAndDrop(panel, browser)

            /**
             * Register a load handler to run after each page finishes loading.
             * CefLoadHandlerAdapter is a no-op adapter — we only override onLoadEnd.
             *
             * onLoadEnd fires on the CEF I/O thread, NOT the EDT.
             * Swing updates (urlTextField.text) must be dispatched via SwingUtilities.invokeLater.
             * CEF calls (cefBrowser.executeJavaScript) are safe to call directly from the CEF thread.
             */
            browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {

                override fun onLoadEnd(cefBrowser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                    // Skip sub-frames (iframes, etc.) — only process the main page load.
                    // Without this guard, the JS would be injected multiple times per page.
                    if (!frame.isMain) return

                    val currentUrl = cefBrowser.url

                    // Update the URL bar — must be on EDT since JTextField is a Swing component.
                    SwingUtilities.invokeLater { urlTextField.text = currentUrl }

                    // Match the current URL against configured sites to resolve the button selector.
                    // Must happen before building the JS script below.
                    updateSiteConfig(currentUrl)

                }

            }, browser.cefBrowser)

        }

        /**
         * Installs a Swing DropTarget on the browser component to handle file drops.
         *
         * File reading is dispatched to a pooled thread to avoid blocking the EDT.
         * Size filtering is delegated entirely to injectFilesViaJs — no duplicate check here.
         * acceptOrRejectFileDrop() is defined in DnDUtils and shared between dragEnter/dragOver.
         */
        private fun setupFileDragAndDrop(browser: JBCefBrowser) {
            LOG.info("setupFileDragAndDrop: browser.component=${browser.component.javaClass.name}")

            browser.component.dropTarget = DropTarget(
                browser.component, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {

                    override fun dragEnter(event: DropTargetDragEvent) = event.acceptOrRejectFileDrop()
                    override fun dragOver(event: DropTargetDragEvent) = event.acceptOrRejectFileDrop()

                    override fun drop(event: DropTargetDropEvent) {
                        LOG.info("AWT-DnD — drop event received!")
                        event.acceptDrop(DnDConstants.ACTION_COPY)

                        try {
                            val transferable = event.transferable
                            if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                event.dropComplete(false)
                                return
                            }

                            @Suppress("UNCHECKED_CAST")
                            val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                            LOG.info("DnD — dropped ${files.size} file(s): ${files.map { it.name }}")

                            // Read files on a background thread to avoid an EDT freeze for large files.
                            // Size filtering and base64 encoding are handled inside injectFilesViaJs.
                            ApplicationManager.getApplication().executeOnPooledThread {
                                injectFilesViaJs(browser, files.filter { it.exists() }, currentSite)
                            }

                            // Mark complete immediately — the actual injection is async.
                            event.dropComplete(true)

                        } catch (ex: Exception) {
                            LOG.error("DnD — drop handling failed", ex)
                            event.dropComplete(false)
                        }
                    }
                }, true
            )
        }

        /**
         * Registers an IntelliJ DnDNativeTarget on the content panel so that drags
         * originating from editor tabs (and other IntelliJ DnD sources) are accepted.
         *
         * This complements setupFileDragAndDrop() which handles standard AWT/Swing DnD
         * (e.g., files dragged from the Project tool window or the OS file manager).
         *
         * extractVirtualFiles() is defined in DnDUtils and handles all known drag-source
         * object types (TransferableWrapper, VirtualFile[], DnDNativeTarget.EventInfo, etc.).
         */
        private fun setupIntelliJDragAndDrop(panel: JPanel, browser: JBCefBrowser) {
            LOG.info("setupIntelliJDragAndDrop: panel=${panel.javaClass.name}, browser.component=${browser.component.javaClass.name}")
            val target = object : DnDNativeTarget {
                override fun update(event: DnDEvent): Boolean {
                    val files = extractVirtualFiles(event)
                    LOG.debug("IntelliJDnD update() — extracted ${files.size} file(s): ${files.map { it.name }}")
                    if (files.isNotEmpty()) {
                        event.isDropPossible = true
                        return false  // false = we handle it, don't delegate to parent
                    }
                    return true  // true = delegate to parent
                }

                override fun drop(event: DnDEvent) {
                    LOG.info("IntelliJDnD drop() — attachedObject=${event.attachedObject?.javaClass?.name}")
                    val virtualFiles = extractVirtualFiles(event)
                    if (virtualFiles.isEmpty()) return

                    val ioFiles = virtualFiles.mapNotNull { vf ->
                        vf.canonicalPath?.let { File(it) }
                    }.filter { it.exists() }

                    if (ioFiles.isEmpty()) return

                    LOG.info("IntelliJDnD — dropped ${ioFiles.size} file(s): ${ioFiles.map { it.name }}")
                    injectFilesViaJs(browser, ioFiles, currentSite)
                }
            }
            this.dndTarget = target

            // Register on both panel and browser component — JCEF's heavyweight native window
            // sits on top of the panel, so the panel alone may never receive the drag.
            DnDManager.getInstance().registerTarget(target, panel)
            DnDManager.getInstance().registerTarget(target, browser.component)
            LOG.info("IntelliJDnD — registered DnD target on panel and browser.component")
        }

        /**
         * Exposes the root panel to createToolWindowContent().
         * MyChatWindow is an internal class — this is its only public surface.
         */
        fun getContent(): JPanel = content

        override fun dispose() {
            dndTarget?.let {
                DnDManager.getInstance().unregisterTarget(it, content)
                DnDManager.getInstance().unregisterTarget(it, browser.component)
            }
        }

    }

}  // class MyChatWindowFactory
