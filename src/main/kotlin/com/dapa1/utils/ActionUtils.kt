package com.dapa1.utils

import com.dapa1.browser.BrowserDataKeys
import com.dapa1.browser.BrowserLocator
import com.dapa1.settings.MyChatSettings
import com.dapa1.ui.BrowserFindBar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import java.io.File

/**
 * Retrieves the [JBCefBrowser] instance associated with the current action event.
 * It first checks the event's data context, then falls back to locating it via [BrowserLocator].
 */
fun AnActionEvent.getBrowser(): JBCefBrowser? {
    val browser = getData(BrowserDataKeys.BROWSER)
    if (browser != null) return browser

    val project = project ?: return null
    val dataContext = BrowserLocator.dataContext(project) ?: return null
    return BrowserDataKeys.BROWSER.getData(dataContext)
}

/**
 * Retrieves the [MyChatSettings.SiteConfig] for the site currently loaded in the browser.
 */
fun AnActionEvent.getCurrentSite(): MyChatSettings.SiteConfig? {
    val site = getData(BrowserDataKeys.CURRENT_SITE)
    if (site != null) return site

    val project = project ?: return null
    val dataContext = BrowserLocator.dataContext(project) ?: return null
    return BrowserDataKeys.CURRENT_SITE.getData(dataContext)
}

/**
 * Retrieves the [BrowserFindBar] associated with the browser.
 */
fun AnActionEvent.getFindBar(): BrowserFindBar? {
    return getData(BrowserDataKeys.FIND_BAR)
}

/**
 * Returns the home URL of the current site configuration.
 */
fun AnActionEvent.getHomeUrl(): String? {
    return getData(BrowserDataKeys.CURRENT_SITE)?.url
}

/**
 * Resolves the [File] objects for the virtual files associated with the action event.
 * Filters out directories and ensures files exist on the local file system.
 */
fun AnActionEvent.resolveIoFiles(): List<File> {
    val vfs = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        ?: getData(CommonDataKeys.VIRTUAL_FILE)?.let { arrayOf(it) }
        ?: return emptyList()

    return vfs.filter { !it.isDirectory }
        .mapNotNull { vf -> vf.canonicalPath?.let { File(it) } }
        .filter { it.exists() }
}

/**
 * Resolves the virtual files associated with the current action event.
 *
 * Supports both:
 * - Single-file selections via [CommonDataKeys.VIRTUAL_FILE]
 * - Multi-file selections via [CommonDataKeys.VIRTUAL_FILE_ARRAY]
 *
 * Directories are filtered out because MyChat actions currently operate on
 * file content rather than folder structures.
 *
 * Unlike [resolveIoFiles], this method preserves non-local files such as:
 * - jrt:// (JDK runtime files)
 * - jar:// (library sources/classes)
 * - decompiled class files
 *
 * @return All selected non-directory [VirtualFile]s, or an empty list if
 * no file selection is available in the current action context.
 */
fun AnActionEvent.resolveVirtualFiles(): List<VirtualFile> {
    val vfs = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        ?: getData(CommonDataKeys.VIRTUAL_FILE)?.let(::arrayOf)
        ?: return emptyList()

    return vfs.filter { !it.isDirectory }
}
