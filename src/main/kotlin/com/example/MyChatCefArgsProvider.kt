package com.example

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefAppRequiredArgumentsProvider

class MyChatCefArgsProvider : JBCefAppRequiredArgumentsProvider {

    private val log = Logger.getInstance(MyChatWindowFactory::class.java)
    override val options: List<String>
        get() {
            val dark = LafManager.getInstance().currentUIThemeLookAndFeel.isDark
            //println("dark   = $dark")
            log.info("dark = $dark")

            return listOf(
                if (dark) "--force-dark-mode"
                else "--force-light-mode"
            )
        }
}
