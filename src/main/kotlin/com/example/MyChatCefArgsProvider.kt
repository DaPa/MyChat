package com.example

import com.intellij.ide.ui.LafManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.jcef.JBCefAppRequiredArgumentsProvider

class MyChatCefArgsProvider : JBCefAppRequiredArgumentsProvider {

    private val log = Logger.getInstance(MyChatWindowFactory::class.java)
    override val options: List<String>
        get() {
            val dark = LafManager.getInstance().currentUIThemeLookAndFeel.isDark
            //println("dark  = $dark")
            log.info("dark = $dark")

            return listOf(
                if (dark) "--force-dark-mode"
                else "--force-light-mode",
                // Fixes "white window" and rendering issues for DevTools and popups on Windows.
                // --disable-gpu-compositing: Avoids conflicts between main and secondary window GPU contexts.
                // --disable-features=CalculateNativeWinOcclusion: Fixes Chromium misidentifying a window as hidden.
                "--disable-gpu-compositing",
                //"--disable-features=CalculateNativeWinOcclusion",
            )
        }
}
