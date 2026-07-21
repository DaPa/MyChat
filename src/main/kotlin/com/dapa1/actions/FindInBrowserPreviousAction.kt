package com.dapa1.actions

import com.dapa1.utils.getFindBar
import com.intellij.openapi.actionSystem.AnActionEvent

class FindInBrowserPreviousAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.getFindBar()?.performFind(forward = false, findNext = true)
    }
}
