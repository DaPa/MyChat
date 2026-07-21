package com.dapa1.actions

import com.dapa1.utils.getFindBar
import com.intellij.openapi.actionSystem.AnActionEvent

class FindInBrowserAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val findBar = e.getFindBar() ?: return
        findBar.open()
    }
}
