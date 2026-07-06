# MyChat

[![Twitter Follow](https://img.shields.io/badge/follow-%40JBPlatform-1DA1F2?logo=twitter)](https://twitter.com/JBPlatform)
[![Developers Forum](https://img.shields.io/badge/JetBrains%20Platform-Join-blue)][jb:forum]

## Nice hints

Enable "Internal Mode": menu Help -> Edit Custom VM Options... -> add: "-Didea.is.internal=true" (in idea64.exe.vmoptions), then "Ctrl+Alt+Click" on any GUI element to inspect 

// Gradle plugins: https://plugins.gradle.org/
// Marketplace: https://plugins.jetbrains.com/search?excludeTags=theme&search=AI+chat
// IntelliJ Platform forum: https://platform.jetbrains.com/c/intellij-platform/5
// for all icons see https://intellij-icons.jetbrains.design/
// see https://plugins.jetbrains.com/docs/intellij/ui-guidelines-welcome.html
// see https://github.com/JetBrains/intellij-community
// see https://github.com/JetBrains/intellij-platform-gradle-plugin
// see https://plugins.jetbrains.com/docs/intellij/ui-customization.html
// for all colors see https://jetbrains.design/intellij/colors/
// for all fonts see https://jetbrains.design/intellij/fonts/
// for all components see https://jetbrains.design/intellij/components/
// for all actions see https://jetbrains.design/intellij/actions/
// for all dialogs see https://jetbrains.design/intellij/dialogs/
// for all panels see https://jetbrains.design/intellij/panels/
// for all tabs see https://jetbrains.design/intellij/tabs/
// for all lists see https://jetbrains.design/intellij/lists/
// for all trees see https://jetbrains.design/intellij/trees/
// for all tables see https://jetbrains.design/intellij/tables/
// for all buttons see https://jetbrains.design/intellij/buttons/
// for all text fields see https://jetbrains.design/intellij/text-fields/
// for all text areas see https://jetbrains.design/intellij/text-areas/
// for all checkboxes see https://jetbrains.design/intellij/checkboxes/
// for all radio buttons see https://jetbrains.design/intellij/radio-buttons/
// for all combo boxes see https://jetbrains.design/intellij/combo-boxes/
// for all progress bars see https://jetbrains.design/intellij/progress-bars/
// for all sliders see https://jetbrains.design/intellij/sliders/
// for all spinners see https://jetbrains.design/intellij/spinners/
// for all tooltips see https://jetbrains.design/intellij/tooltips/
// for all status bars see https://jetbrains.design/intellij/status-bars/
// for all tool windows see https://jetbrains.design/intellij/tool-windows/
// for all toolbars see https://jetbrains.design/intellij/toolbars/
// for all menus see https://jetbrains.design/intellij/menus/
// for all actions see https://jetbrains.design/intellij/actions/
// for all notifications see https://jetbrains.design/intellij/notifications/
// for all messages see https://jetbrains.design/intellij/messages/
// for all dialogs see https://jetbrains.design/intellij/dialogs/
// for all panels see https://jetbrains.design/intellij/panels/
// for all tabs see https://jetbrains.design/intellij/tabs/
// for all lists see https://jetbrains.design/intellij/lists/
// for all trees see https://jetbrains.design/intellij/trees/
// for all tables see https://jetbrains.design/intellij/tables/
// for all buttons see https://jetbrains.design/intellij/buttons/
// for all text fields see https://jetbrains.design/intellij/text-fields/

## Plugin structure

A generated project contains the following content structure:

```
.
├── .run/                   Predefined Run/Debug Configurations
├── build/                  Output build directory
├── gradle
│   ├── wrapper/            Gradle Wrapper
│   ├── libs.versions.toml  Version catalog
├── src                     Plugin sources
│   ├── main
│   │   ├── kotlin/         Kotlin production sources
│   │   └── resources/      Resources - plugin.xml, icons, messages
├── .gitignore              Git ignoring rules
├── build.gradle.kts        Gradle build configuration
├── gradle.properties       Gradle configuration properties
├── gradlew                 *nix Gradle Wrapper script
├── gradlew.bat             Windows Gradle Wrapper script
├── README.md               README
└── settings.gradle.kts     Gradle project settings
```

In addition to the configuration files, the most crucial part is the `src` directory, which contains our implementation
and the manifest for our plugin – [plugin.xml][file:plugin.xml].

> [!NOTE]
> To use Java in your plugin, create the `/src/main/java` directory.

## Plugin configuration file

The plugin configuration file is a [plugin.xml][file:plugin.xml] file located in the `src/main/resources/META-INF`
directory.
It provides general information about the plugin, its dependencies, extensions, and listeners.

You can read more about this file in the [Plugin Configuration File][docs:plugin.xml] section of our documentation.

If you're still not quite sure what this is all about, read [Introduction to IntelliJ Platform][docs:intro].

## Predefined Run/Debug configurations

Within the default project structure, there is a `.run` directory provided containing predefined *Run/Debug
configurations* that expose corresponding Gradle tasks:

| Configuration name | Description                                                                                                                                                                         |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Run Plugin         | Runs [`:runIde`][gh:intellij-platform-gradle-plugin-runIde] IntelliJ Platform Gradle Plugin task. Use the *Debug* icon for plugin debugging.                                        |
| Run Tests          | Runs [`:test`][gradle:lifecycle-tasks] Gradle task.                                                                                                                                 |
| Run Verifications  | Runs [`:verifyPlugin`][gh:intellij-platform-gradle-plugin-verifyPlugin] IntelliJ Platform Gradle Plugin task to check the plugin compatibility against the specified IntelliJ IDEs. |

> [!NOTE]
> You can find the logs from the running task in the `idea.log` tab.

## Publishing the plugin

> [!TIP]
> Make sure to follow all guidelines listed in [Publishing a Plugin][docs:publishing] to follow all recommended and
> required steps.

Releasing a plugin to [JetBrains Marketplace](https://plugins.jetbrains.com) is a straightforward operation that uses
the `publishPlugin` Gradle task provided by
the [intellij-platform-gradle-plugin][gh:intellij-platform-gradle-plugin-docs].

You can also upload the plugin to the [JetBrains Plugin Repository](https://plugins.jetbrains.com/plugin/upload)
manually via UI.

## Useful links

- [IntelliJ Platform SDK Plugin SDK][docs]
- [IntelliJ Platform Gradle Plugin Documentation][gh:intellij-platform-gradle-plugin-docs]
- [IntelliJ Platform Explorer][jb:ipe]
- [JetBrains Marketplace Quality Guidelines][jb:quality-guidelines]
- [IntelliJ Platform UI Guidelines][jb:ui-guidelines]
- [JetBrains Marketplace Paid Plugins][jb:paid-plugins]
- [IntelliJ SDK Code Samples][gh:code-samples]

[docs]: https://plugins.jetbrains.com/docs/intellij

[docs:intro]: https://plugins.jetbrains.com/docs/intellij/intellij-platform.html?from=IJPluginTemplate

[docs:plugin.xml]: https://plugins.jetbrains.com/docs/intellij/plugin-configuration-file.html?from=IJPluginTemplate

[docs:publishing]: https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate

[file:plugin.xml]: ./src/main/resources/META-INF/plugin.xml

[gh:code-samples]: https://github.com/JetBrains/intellij-sdk-code-samples

[gh:intellij-platform-gradle-plugin]: https://github.com/JetBrains/intellij-platform-gradle-plugin

[gh:intellij-platform-gradle-plugin-docs]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html

[gh:intellij-platform-gradle-plugin-runIde]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#runIde

[gh:intellij-platform-gradle-plugin-verifyPlugin]: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-tasks.html#verifyPlugin

[gradle:lifecycle-tasks]: https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks

[jb:github]: https://github.com/JetBrains/.github/blob/main/profile/README.md

[jb:forum]: https://platform.jetbrains.com/

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:paid-plugins]: https://plugins.jetbrains.com/docs/marketplace/paid-plugins-marketplace.html

[jb:quality-guidelines]: https://plugins.jetbrains.com/docs/marketplace/quality-guidelines.html

[jb:ipe]: https://jb.gg/ipe

[jb:ui-guidelines]: https://jetbrains.github.io/ui
