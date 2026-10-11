package com.dapa1.settings

import com.dapa1.utils.normalizeUrl
import com.intellij.openapi.components.*
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

@Service(Service.Level.APP)
@State(
    name = "MyChatSettings",
    storages = [Storage("MyChatSettings.xml")]
)
class MyChatSettings : PersistentStateComponent<MyChatSettings.State> {

    /**
     * Represents one configured chat site.
     *
     * Why iconPath (String) instead of Icon:
     *  - Icon (javax.swing.Icon) is not serializable — it cannot be written to XML
     *  - We store the path and resolve the Icon lazily at display time
     *
     * iconPath variants:
     *  - Bundled resource : "/icons/ChatGPT.svg"  (classpath, starts with "/")
     *  - User file        : "/home/user/my.svg" or "C:\icons\my.png"  (absolute filesystem path)
     *
     * icon field is @Transient — it is never written to MyChatSettings.xml,
     * only held in memory as a cache after being loaded from iconPath.
     */
    data class SiteConfig(
        var iconPath: String = "",   // persisted: classpath resource or absolute file path
        var name: String = "",
        var url: String = "",
        var expectedStrings: MutableList<String> = mutableListOf(), // Site-specific phrases/selectors. One entry per string.
    ) {
        // Transient: derived from iconPath at runtime, never serialized.
        // Loaded on first access via loadIcon().
        @get:com.intellij.util.xmlb.annotations.Transient
        var icon: Icon? = null

        /**
         * Returns the Icon for this site, loading it if not yet cached.
         * Falls back to the generic icon if the path is blank or loading fails.
         */
        fun resolveIcon(): Icon {
            if (icon == null) {
                icon = loadIcon(iconPath)
            }
            return icon ?: MyChatIcons.generic
        }

        fun isCopilot() =
            normalizeUrl(url).startsWith("m365.cloud.microsoft")
    }

    class State(
        /**
         * When enabled, MyChat displays IDE notifications for upload-path messages
         * received from JavaScript running inside an embedded JCEF browser.
         *
         * This is primarily a diagnostic option and is disabled by default to avoid
         * showing notifications during normal plugin use.
         */
        var uploadDebugNotificationsEnabled: Boolean = false,

        var sites: MutableList<SiteConfig> = defaultSites()
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
        // Icons are @Transient and lost on deserialization — clear cached icons
        // so resolveIcon() reloads them from iconPath on next access.
        this.state.sites.forEach { it.icon = null }
    }

    companion object {
        fun getInstance(): MyChatSettings = service()

        /**
         * Loads an Icon from either a classpath resource path or an absolute filesystem path.
         *
         * Classpath path  (starts with "/") : loaded via IconLoader — works for bundled SVGs/PNGs
         * Filesystem path (absolute)        : loaded via IconLoader.getIcon(file.toURI().toURL())
         *
         * Returns null on any failure (file not found, bad format, etc.) so callers can fall back.
         */
        fun loadIcon(path: String): Icon? {
            if (path.isBlank()) return null
            return try {
                val file = java.io.File(path)
                if (file.exists()) {
                    // Treat as absolute filesystem path (user-supplied icon)
                    IconLoader.getIcon(file.toURI().toURL().toString(), MyChatSettings::class.java)
                } else if (path.startsWith("/")) {
                    // Treat as classpath resource (bundled icon)
                    IconLoader.getIcon(path, MyChatSettings::class.java)
                } else {
                    null
                }
            } catch (e: Exception) {
                //println("MyChatSettings: failed to load icon from '$path': ${e.message}")
                null
            }
        }

        fun selectors(vararg values: String): MutableList<String> = values.toMutableList()

        fun defaultSites(): MutableList<SiteConfig> = mutableListOf(
            SiteConfig(
                "/icons/Copilot.svg", "Copilot", "https://m365.cloud.microsoft/chat",
                selectors("[aria-label*=\"Add and manage sources\"]")
            ),
            SiteConfig(
                "/icons/ChatGPT.svg", "ChatGPT", "https://chatgpt.com",
                selectors("[aria-label*=\"Add files and more\"]")
            ),
            SiteConfig(
                "/icons/Claude.svg", "Claude", "https://claude.ai",
                selectors("[aria-label*=\"Add files, connectors, and more\"]")
            ),
            SiteConfig(
                "/icons/Perplexity.svg", "Perplexity", "https://www.perplexity.ai",
                selectors("[aria-label*=\"Add files or tools\"]")
            ),
            SiteConfig(
                "/icons/Grok.svg", "Grok", "https://grok.com",
                selectors("[aria-label*=\"Attach\"]")
            ),
            SiteConfig(
                "/icons/ManusIM.svg", "Manus", "https://manus.im",
                selectors("")
            ),
            SiteConfig(
                "/icons/Gemini.svg", "Gemini", "https://gemini.google.com/app",
                selectors("[aria-label*=\"Upload and tools\"]")
            ),
            SiteConfig(
                "/icons/Google.svg", "Google", "https://google.com/ai",
                selectors("[aria-label*=\"Add files and tools\"]", "[aria-label*=\"Lisää tiedostoja ja työkaluja\"]")
            ),
            SiteConfig(
                "/icons/DuckDuckGo.svg", "Duck", "https://duck.ai/chat",
                selectors("[aria-label*=\"Add images or PDFs\"]")
            ),
            SiteConfig("/icons/generic.svg", "generic", "https://start.duckduckgo.com"),
        )
    }

}  // class MyChatSettings


/**
 * Centralized icon registry for bundled MyChat icons.
 * These are always available (shipped with the plugin).
 * User-supplied icons are loaded dynamically via MyChatSettings.loadIcon().
 */
object MyChatIcons {
    val generic: Icon = "/icons/generic.svg".load()
    val toolWindow: Icon = "/icons/toolWindow.svg".load()

    private fun String.load(): Icon =
        IconLoader.getIcon(this, MyChatIcons::class.java)
}
