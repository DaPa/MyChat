package com.example.utils

import com.example.browser.BrowserDataKeys
import com.example.browser.BrowserLocator
import com.example.settings.MyChatSettings
import com.example.ui.BrowserFindBar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.ui.jcef.JBCefBrowser
import java.io.File

fun AnActionEvent.getBrowser(): JBCefBrowser? {
    val browser = getData(BrowserDataKeys.BROWSER)
    if (browser != null) return browser

    val project = project ?: return null
    val dataContext = BrowserLocator.dataContext(project) ?: return null
    return BrowserDataKeys.BROWSER.getData(dataContext)
}

fun AnActionEvent.getCurrentSite(): MyChatSettings.SiteConfig? {
    val site = getData(BrowserDataKeys.CURRENT_SITE)
    if (site != null) return site

    val project = project ?: return null
    val dataContext = BrowserLocator.dataContext(project) ?: return null
    return BrowserDataKeys.CURRENT_SITE.getData(dataContext)
}

fun AnActionEvent.getFindBar(): BrowserFindBar? {
    return getData(BrowserDataKeys.FIND_BAR)
}

fun AnActionEvent.getHomeUrl(): String? {
    return getData(BrowserDataKeys.CURRENT_SITE)?.url
}

fun AnActionEvent.resolveIoFiles(): List<File> {
    val vfs = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        ?: getData(CommonDataKeys.VIRTUAL_FILE)?.let { arrayOf(it) }
        ?: return emptyList()

    return vfs.filter { !it.isDirectory }
        .mapNotNull { vf -> vf.canonicalPath?.let { File(it) } }
        .filter { it.exists() }
}
