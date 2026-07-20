package com.example.ui

import com.example.browser.BrowserDataKeys
import com.example.settings.MyChatSettings
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
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.JBColor
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
class MyChatWindow(private val project: Project) : Disposable {

    companion object {
        private val LOG = Logger.getInstance(MyChatWindow::class.java)

        /**
         * Number of columns for the URL input field (controls its preferred width).
         * JTextField uses character-width units, not pixels.
         */
        private const val INPUT_FIELD_COLUMNS = 25
    }

    /**
     * Finds the best matching SiteConfig for the given URL by normalized prefix match.
     */
    private fun findMatchingSite(currentUrl: String): MyChatSettings.SiteConfig? {
        val normalizedCurrent = normalizeUrl(currentUrl)
        return MyChatSettings.getInstance().state.sites.firstOrNull { site ->
            normalizedCurrent.startsWith(normalizeUrl(site.url))
        }
    }

    /**
     * Resolves and caches the button selector for the given URL.
     * Called on every page load (onLoadEnd) to handle navigation between sites.
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
     * Marked as @Volatile because it is updated from CEF threads (onLoadEnd)
     * and read from BGT (uiDataSnapshot for action updates).
     */
    @Volatile
    internal var currentSite: MyChatSettings.SiteConfig =
        MyChatSettings.getInstance().state.sites.firstOrNull() ?: MyChatSettings.SiteConfig(
            "",
            "ChatGPT",
            "https://chatgpt.com"
        )

    internal lateinit var browser: JBCefBrowser
    internal lateinit var findBar: BrowserFindBar
    private var dndTarget: DnDNativeTarget? = null

    private val cefLaunchDarkState = LafManager.getInstance().currentUIThemeLookAndFeel.isDark
    private var themeMismatchNotified = false

    private val content: JPanel = createContentPanel()

    /**
     * Creates the root panel and wires DataProvider + UI setup.
     */
    private fun createContentPanel(): JPanel =
        object : JPanel(BorderLayout()), UiDataProvider {
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

        MyThemeListener(this) { dark ->
            LOG.info("MyThemeListener callback fired. dark=$dark, cefLaunchDarkState=$cefLaunchDarkState")
            if (dark != cefLaunchDarkState) {
                if (!themeMismatchNotified) {
                    showThemeRestartNotification()
                    themeMismatchNotified = true
                }
            } else {
                themeMismatchNotified = false
            }
        }

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
                val label = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                val site = value as? MyChatSettings.SiteConfig
                if (site != null) {
                    label.text = site.name.ifEmpty { site.url }
                    label.setToolTipText(HtmlChunk.text(site.url))
                    label.icon = site.resolveIcon()
                }
                return label
            }
        }

        MyChatSettings.getInstance().state.sites.forEach { site -> combo.addItem(site) }

        val urlTextField = JTextField(currentSite.url, INPUT_FIELD_COLUMNS).apply {
            addActionListener {
                val customUrl = text.trim()
                if (customUrl.isNotEmpty()) {
                    browser.loadURL(customUrl)
                }
            }
            addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent) {
                    selectAll()
                }
            })
        }

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

        val toolbar = actionManager.createActionToolbar("MyChatToolbar", actionGroup, true)
        toolbar.setMiniMode(true)
        toolbar.targetComponent = browser.component

        findBar = BrowserFindBar(browser)

        val topPanel = JPanel(BorderLayout()).apply {
            add(combo, BorderLayout.WEST)
            add(urlTextField, BorderLayout.CENTER)
            add(toolbar.component, BorderLayout.EAST)
        }

        val northPanel = JPanel(BorderLayout()).apply {
            add(topPanel, BorderLayout.NORTH)
            add(findBar, BorderLayout.SOUTH)
        }
        panel.add(northPanel, BorderLayout.NORTH)
        panel.add(browser.component, BorderLayout.CENTER)

        panel.background = JBColor.PanelBackground

        setupFileDragAndDrop(browser)
        setupIntelliJDragAndDrop(panel, browser)

        browser.jbCefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
            override fun onLoadEnd(cefBrowser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                if (!frame.isMain) return
                val currentUrl = cefBrowser.url
                SwingUtilities.invokeLater { urlTextField.text = currentUrl }
                updateSiteConfig(currentUrl)
            }
        }, browser.cefBrowser)
    }

    private fun setupFileDragAndDrop(browser: JBCefBrowser) {
        LOG.info("setupFileDragAndDrop: browser.component=${browser.component.javaClass.name}")
        browser.component.dropTarget = DropTarget(
            browser.component, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
                override fun dragEnter(event: DropTargetDragEvent) = event.acceptOrRejectFileDrop()
                override fun dragOver(event: DropTargetDragEvent) = event.acceptOrRejectFileDrop()
                override fun drop(event: DropTargetDropEvent) {
                    LOG.info("AWT-DnD \u2014 drop event received!")
                    event.acceptDrop(DnDConstants.ACTION_COPY)
                    try {
                        val transferable = event.transferable
                        if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            event.dropComplete(false)
                            return
                        }
                        @Suppress("UNCHECKED_CAST")
                        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                        LOG.info("DnD \u2014 dropped ${files.size} file(s): ${files.map { it.name }}")
                        ApplicationManager.getApplication().executeOnPooledThread {
                            injectFilesViaJs(browser, files.filter { it.exists() }, currentSite)
                        }
                        event.dropComplete(true)
                    } catch (ex: Exception) {
                        LOG.error("DnD \u2014 drop handling failed", ex)
                        event.dropComplete(false)
                    }
                }
            }, true
        )
    }

    private fun setupIntelliJDragAndDrop(panel: JPanel, browser: JBCefBrowser) {
        LOG.info("setupIntelliJDragAndDrop: panel=${panel.javaClass.name}, browser.component=${browser.component.javaClass.name}")
        val target = object : DnDNativeTarget {
            override fun update(event: DnDEvent): Boolean {
                val files = extractVirtualFiles(event)
                LOG.debug("IntelliJDnD update() \u2014 extracted ${files.size} file(s): ${files.map { it.name }}")
                if (files.isNotEmpty()) {
                    event.isDropPossible = true
                    return false
                }
                return true
            }

            override fun drop(event: DnDEvent) {
                LOG.info("IntelliJDnD drop() \u2014 attachedObject=${event.attachedObject?.javaClass?.name}")
                val virtualFiles = extractVirtualFiles(event)
                if (virtualFiles.isEmpty()) return
                val ioFiles = virtualFiles.mapNotNull { vf ->
                    vf.canonicalPath?.let { File(it) }
                }.filter { it.exists() }
                if (ioFiles.isEmpty()) return
                LOG.info("IntelliJDnD \u2014 dropped ${ioFiles.size} file(s): ${ioFiles.map { it.name }}")
                ApplicationManager.getApplication().executeOnPooledThread {
                    injectFilesViaJs(browser, ioFiles, currentSite)
                }
            }
        }
        this.dndTarget = target
        DnDManager.getInstance().registerTarget(target, panel)
        DnDManager.getInstance().registerTarget(target, browser.component)
        LOG.info("IntelliJDnD \u2014 registered DnD target on panel and browser.component")
    }

    fun getContent(): JPanel = content

    override fun dispose() {
        dndTarget?.let {
            DnDManager.getInstance().unregisterTarget(it, content)
            DnDManager.getInstance().unregisterTarget(it, browser.component)
        }
    }
}
