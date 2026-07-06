package com.example.actions

import com.example.utils.getFindBar
import com.intellij.openapi.actionSystem.AnActionEvent

class FindInBrowserNextAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.getFindBar()?.performFind(forward = true, findNext = true)
    }
}
