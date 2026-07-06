package com.example

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

private const val BUNDLE = "messages.MyChatMessagesBundle"  // points to "resources/messages/MyChatMessagesBundle.properties"

internal object MyChatMessagesBundle {
    // DynamicBundle is a JetBrains platform utility class used for internationalization (i18n) in IntelliJ-based plugins.
    private val instance = DynamicBundle(MyChatMessagesBundle::class.java, BUNDLE)

    @JvmStatic
    fun message(key: @PropertyKey(resourceBundle = BUNDLE) String, vararg params: Any?): @Nls String {
        return instance.getMessage(key, *params)
    }

    @Suppress("unused")
    @JvmStatic
    fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any?): Supplier<@Nls String> {
        return instance.getLazyMessage(key, *params)
    }
}
