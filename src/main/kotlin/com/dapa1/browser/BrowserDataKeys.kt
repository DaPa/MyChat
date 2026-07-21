package com.dapa1.browser

import com.dapa1.settings.MyChatSettings
import com.dapa1.ui.BrowserFindBar
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.ui.jcef.JBCefBrowser

/**
 * Central registry of DataKeys used by the browser tool window.
 */
object BrowserDataKeys {

    /**
     * Provides access to the active JBCefBrowser instance
     * through IntelliJ's DataContext.
     */
    val BROWSER: DataKey<JBCefBrowser> =
        DataKey.create("com.dapa1.ui.jcef.JBCefBrowser")
    val CURRENT_SITE: DataKey<MyChatSettings.SiteConfig> =
        DataKey.create("com.dapa1.settings.MyChatSettings.SiteConfig")
    val FIND_BAR: DataKey<BrowserFindBar> =
        DataKey.create("com.dapa1.ui.BrowserFindBar")
}
