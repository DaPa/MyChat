package com.example.ui

import com.intellij.ide.ui.LafManager
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager

class MyThemeListener(
    parentDisposable: Disposable,
    private val themeChangedCallback: (Boolean) -> Unit
) {

    init {
        // Notify immediately with current state.
        themeChangedCallback(isDark())

        ApplicationManager.getApplication().messageBus
            .connect(parentDisposable)
            .subscribe(
                LafManagerListener.TOPIC,
                LafManagerListener {
                    themeChangedCallback(isDark())
                }
            )
    }

    fun isDark(): Boolean {
        return LafManager.getInstance().currentUIThemeLookAndFeel.isDark
    }
}
