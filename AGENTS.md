# Agent’s Guide for MyChat Project

This document provides technical details and a mapping of features to implementation files to help AI agents (and developers) quickly navigate and understand the codebase.

## Technical Context

### 1. Browser Host (JCEF)
The plugin uses `JBCefBrowser` to host the web-based chat services. 
- **Initialization**: `MyChatWindow` sets up the browser and its handlers.
- **Arguments**: Custom CEF arguments are provided via `MyChatCefArgsProvider` (e.g., for dark mode, GPU handling).
- **Locator**: Use `BrowserLocator` or `ActionUtils.getBrowser(e)` to find the browser instance.
- **Limitations**: The current JCEF version used in the project **does not support `CefFindHandler`**. Do NOT attempt to implement real-time match counts (e.g., "1/5") in the find bar using `onFindResult`, as the API is missing/unavailable.

### 2. Interaction via JavaScript Injection
Most "smart" features are implemented by executing JavaScript within the browser.
- **Injection Entry Point**: `ComponentUtils.kt` (methods like `injectFilesViaJs`, `injectContentAsMarkdown`, `injectSelectionAsMarkdown`).
- **JS Resources**: Script templates are located in `src/main/resources/js/` and loaded via `JsUtils.kt`.
- **Site Selectors**: `ComponentUtils.kt` dynamically builds a search array for attachment buttons using `expectedStrings` from `SiteConfig` (found in `MyChatSettings.kt`) plus a set of hardcoded default fallback selectors.

### 3. Lifecycle & State Management
- **Startup**: `MyChatStartupActivity` (a `ProjectActivity`) initializes JCEF post-startup to avoid service dependency issues. It also handles cookie persistence (`persist_session_cookies = true`) and cleans up temporary files.
- **Project State**: `MyChatProjectState` tracks project-level flags like `jcefReady`.

### 4. Action Framework
Actions are organized in a hierarchy:
- `AbstractBrowserAction`: Base class for any action that needs to interact with the browser.
- `AbstractFileAction`: Base class for actions that operate on files (requires file availability and browser readiness).
- **Navigation actions**: Back, Forward, Refresh, Home, Stop (in `com.example.actions`).
- **Integration actions**: `SendToMyChatAction`, `EmbedToMyChatAction`.
- **Utility actions**: `ClearCookiesAction`, `DebugAction` (F12), `FindInBrowserAction` (Ctrl+F).

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
| **Lifecycle & Persistence**  | `MyChatStartupActivity.kt`, `MyChatProjectState.kt`             |
| **Dark Mode/Theming**        | `MyThemeListener.kt`, `MyChatCefArgsProvider.kt`                |
| **Plugin Entry Points**      | `plugin.xml` (the "Source of Truth" for IDE integration)        |


## Development Scope
Unless the task explicitly asks for it:
- Do NOT add tests (unit tests, integration tests, or UI tests).
- Do NOT add, update, or maintain translation/localization resources.
- Do NOT add new language strings, resource bundles, or i18n infrastructure.

## Common Modification Scenarios

- **Adding a new AI Chat Site**: 
    1. Define a new `SiteConfig` in `MyChatSettings.kt` (provide name, URL, and `expectedStrings` for the attachment button).
    2. Add an SVG icon to `src/main/resources/icons/`.
    3. Update `plugin.xml` if any site-specific actions are needed.
- **Fixing Injection on a Site**: 
    1. Check `expectedStrings` in `MyChatSettings.kt` (specifically `defaultSites`).
    2. Debug using CEF DevTools (`F12` in the browser window).
- **Updating JS logic**:
    1. Edit the relevant `.js` template in `src/main/resources/js/`.
    2. Check `JsUtils.kt` and `ComponentUtils.kt` for how the script is built and executed.
- **Handling File Compatibility**:
    1. If a site (like Copilot) has specific file type requirements, update `prepareFileForCopilot` in `ComponentUtils.kt`.
- **Not-working File-injections**:
    1. `manus.im` hard to identify the `button`.
    2. `gemini.google.com` needs a second click on a `Files` button (which opens a native file picker dialog) and then exposes `input[type="file"]`. Current implementation uses a single click; see `injectFiles.js` and `ComponentUtils.kt` for potential multi-click generalization logic (e.g., `clickSequence`). Some future investigation hints below:
  Scan for all buttons:
  ````js
  [...document.querySelectorAll("button")]
    .filter(b => b.textContent?.trim())
    .map(b => ({
        text: b.textContent.trim(),
        aria: b.getAttribute("aria-label")
    }))
  ````
  Find a `Files` button:
  ````js
  const filesBtn =
      [...document.querySelectorAll("button")]
          .find(b => b.textContent?.trim() === "Files");
  console.log(filesBtn);
  ````
  Click the button:
  ````js
  filesBtn.click();
  ````
  Check if `input[type="file"]` is visible:
  ````js
  const fileInput = document.querySelector("input[type='file']");
  console.log(fileInput);
  ````

Maybe better generalize the architecture! Instead of:
  ````Kotlin
  attachSelectors: MutableList<String>
  ````
Consider:
  ````Kotlin
  clickSequence: MutableList<String>
  ````
Examples:
  ````Kotlin
  Copilot:
  [
    "[aria-label*=\"Add and manage sources\"]"
  ]
  ````
  ````Kotlin
  ChatGPT:
  [
    "[aria-label*=\"Add files and more\"]"
  ]
  ````
  ````Kotlin
  Gemini:
  [
    "[aria-label*=\"Upload and tools\"]",
    "TEXT:Files"
  ]
  ````
Then the JS becomes:
  ````js
  for (const selector of selectors) {
      click(selector);
      await delay(ATTACH_DELAY_MS);
  }
  ````
followed by:
  ````js
  input = document.querySelector('input[type="file"]');
  ````
Which means no Gemini-specific code is required.
