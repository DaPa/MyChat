package com.dapa1.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent


abstract class AbstractBrowserAction : AnAction() {

    /**
     * Allows subclasses to refine whether the action should be enabled.
     * Called only if a browser is present.
     */
    protected open fun isEnabled(e: AnActionEvent): Boolean = true

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = isEnabled(e)
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
}
