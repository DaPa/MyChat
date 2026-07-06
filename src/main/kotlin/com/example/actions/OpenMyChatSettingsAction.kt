package com.example.actions

import com.example.settings.MyChatConfigurable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil

/**
 * IntelliJ action to open the IDE Settings and jump to Other Settings -> MyChat.
 */
class OpenMyChatSettingsAction : AbstractBrowserAction() {

    override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            e.project,
            MyChatConfigurable::class.java  // jumps directly to your configurable
        )
    }
}
