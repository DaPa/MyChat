# Useful hints

[![Developers Forum](https://img.shields.io/badge/JetBrains%20Platform-Join-blue)](https://platform.jetbrains.com/)

## JCEF debug logging
1. Go to Help | Edit Custom VM Options, add the following as a new line there, then restart the IDE:
2. -Dide.browser.jcef.log.level=verbose
3. see https://youtrack.jetbrains.com/articles/SUPPORT-A-43/How-to-enable-debug-logging-in-IntelliJ-IDEA

## GUI inspection (enable "Internal Mode")
1. from the menu Help → Edit Custom VM Options... → add: "-Didea.is.internal=true" (in idea64.exe.vmoptions)
2. then "Ctrl+Alt+Click" on any GUI element to inspect

## JCEF remote debugging
See https://github.com/hltj/intellij/blob/master/reference_guide/jcef.md

1. "build.gradle.kts" adds "-Dide.browser.jcef.debug.port=9222" JVM property to IDE run configuration (idea64.exe.vmoptions)
2. Start a remote debugging session in Chrome: chrome://inspect/#devices
3. Check only "Discover network targets"
4. Click "Configure"
5. Add "localhost:9222"
6. Refresh the page or wait for automatic refresh
7. You should then see JCEF targets such as "Chat...", click "inspect" to get a normal Chromium DevTools window.
8. in Console, **DO NOT PASTE** instead manually type "allow pasting"
9. Then can paste any of the following:
    - window.matchMedia("(prefers-color-scheme: dark)").matches
    - navigator.userAgent
    - getComputedStyle(document.documentElement).colorScheme

# Useful links

- [IntelliJ Platform SDK Plugin SDK](https://plugins.jetbrains.com/docs/intellij)
- [IntelliJ Platform Gradle Plugin Documentation](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html)
- [JetBrains Marketplace Quality Guidelines](https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html)
- [IntelliJ SDK Code Samples](https://github.com/JetBrains/intellij-sdk-code-samples)
- [JetBrains issues](https://youtrack.jetbrains.com/issues)
- [JetBrains uploads](https://uploads.jetbrains.com/)
- [JetBrains support](https://intellij-support.jetbrains.com/hc/en-us)
- [JetBrains support](https://www.jetbrains.com/support/)
