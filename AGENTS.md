# Agent Guide for MyChat Project

This document provides technical details and a mapping of features to implementation files to help AI agents (and developers) quickly navigate and understand the codebase.

## Technical Context

### 1. Browser Host (JCEF)
The plugin uses `JBCefBrowser` to host the web-based chat services. 
- **Initialization**: `MyChatWindow` sets up the browser and its handlers.
- **Arguments**: Custom CEF arguments are provided via `MyChatCefArgsProvider` (e.g., for dark mode, GPU handling).
- **Locator**: Use `BrowserLocator` or `ActionUtils.getBrowser(e)` to find the browser instance.

### 2. Interaction via JavaScript Injection
Most "smart" features are implemented by executing JavaScript within the browser.
- **Injection Entry Point**: `ComponentUtils.kt` (methods like `injectFilesViaJs`, `injectContentAsMarkdown`, `injectSelectionAsMarkdown`).
- **JS Resources**: Script templates are located in `src/main/resources/js/` and loaded via `JsUtils.kt`.
- **Site Selectors**: Since different chat sites use different DOM structures, `ComponentUtils.kt` defines a `SELECTORS_JS_ARRAY` with common CSS selectors for finding the input area and attachment buttons.

### 3. Action Framework
Actions are organized in a hierarchy:
- `AbstractBrowserAction`: Base class for any action that needs to interact with the browser.
- `AbstractFileAction`: Base class for actions that operate on files (requires file availability and browser readiness).
- Navigation actions (Back, Forward, Refresh) are found in `com.example.actions`.

## Quick Feature Mapping

| Feature                      | Primary Implementation File(s)                                  |
|------------------------------|-----------------------------------------------------------------|
| **Tool Window**              | `MyChatWindowFactory.kt`, `MyChatWindow.kt`                     |
| **File Integration**         | `SendToMyChatAction.kt`, `ComponentUtils.kt`                    |
| **Selection/Text Embedding** | `EmbedToMyChatAction.kt`, `ComponentUtils.kt`                   |
| **JS Injection Logic**       | `ComponentUtils.kt`, `JsUtils.kt`, `src/main/resources/js/*.js` |
| **Site Configurations**      | `MyChatSettings.kt` (SiteConfig data class)                     |
| **Settings UI**              | `MyChatConfigurable.kt`, `MyChatSettings.kt`                    |
| **Browser Find Bar**         | `BrowserFindBar.kt`, `FindInBrowserAction.kt`                   |
| **Dark Mode/Theming**        | `MyThemeListener.kt`, `MyChatCefArgsProvider.kt`                |
| **Plugin Entry Points**      | `plugin.xml` (the "Source of Truth" for IDE integration)        |

## Common Modification Scenarios

- **Adding a new AI Chat Site**: 
    1. Define a new `SiteConfig` in `MyChatSettings.kt` (provide name and URL).
    2. Add an SVG icon to `src/main/resources/icons/`.
    3. Update `plugin.xml` if any site-specific actions are needed.
- **Fixing Injection on a Site**: 
    1. Check CSS selectors in `ComponentUtils.kt` (specifically `SELECTORS_JS_ARRAY`).
    2. Debug using CEF DevTools (`F12` in the browser window).
- **Updating JS logic**:
    1. Edit the relevant `.js` template in `src/main/resources/js/`.
    2. Check `JsUtils.kt` and `ComponentUtils.kt` for how the script is built and executed.
