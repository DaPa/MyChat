package com.example.actions

import com.example.utils.getFindBar
import com.intellij.openapi.actionSystem.AnActionEvent

class FindInBrowserAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val findBar = e.getFindBar() ?: return
        findBar.open()
    }
}
