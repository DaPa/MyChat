package com.example.utils

import com.example.browser.BrowserDataKeys
import com.example.ui.BrowserFindBar
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.ui.jcef.JBCefBrowser
import java.io.File

fun AnActionEvent.getBrowser(): JBCefBrowser? {
    return getData(BrowserDataKeys.BROWSER)
}

fun AnActionEvent.getFindBar(): BrowserFindBar? {
    return getData(BrowserDataKeys.FIND_BAR)
}

fun AnActionEvent.getHomeUrl(): String? {
    return getData(BrowserDataKeys.CURRENT_SITE)?.url
}

fun AnActionEvent.resolveIoFile(): File? {
    val vf = getData(CommonDataKeys.VIRTUAL_FILE)
        ?: getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.firstOrNull()
        ?: return null
    if (vf.isDirectory) return null
    return vf.canonicalPath?.let { File(it) }?.takeIf { it.exists() }
}
