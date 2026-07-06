package com.example.actions

import com.example.utils.getFindBar
import com.intellij.openapi.actionSystem.AnActionEvent

class FindInBrowserPreviousAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.getFindBar()?.performFind(forward = false, findNext = true)
    }
}
