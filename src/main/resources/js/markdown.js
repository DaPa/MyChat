(
    // Used in ComponentUtils.kt, see MARKDOWN_JS_TEMPLATE.
    function () {
        const MARKDOWN_TEXT = `__ESCAPED_MARKDOWN_TEXT__`;

        /**
         * Finds a supported chat editor in the supplied document or in one
         * of its same-origin child frames.
         *
         * @param {Document} doc Document in which to search.
         * @returns {HTMLElement|null} The detected editor, or null.
         */
        function findEditor(doc) {
            // Try the stable ID first: Microsoft Copilot.
            const byId = doc.getElementById('m365-chat-editor-target-element');
            if (byId) return byId;

            // Try the Lexical editor attribute as a fallback.
            const byLexical = doc.querySelector('[data-lexical-editor="true"]');
            if (byLexical) return byLexical;

            const SELECTORS = [
                'div[contenteditable="true"]',
                'span[contenteditable="true"]',   // ← Copilot uses span, not div!
                'textarea[data-id="root"]',
                'textarea#prompt-textarea',
                '#prompt-textarea',
                'textarea[placeholder]',
            ];

            for (const selector of SELECTORS) {
                const editor = doc.querySelector(selector);

                if (
                    editor instanceof doc.defaultView.HTMLTextAreaElement ||
                    editor instanceof doc.defaultView.HTMLInputElement ||
                    editor?.isContentEditable
                ) {
                    return editor;
                }
            }

            // Search same-origin frames recursively.
            for (const frame of doc.querySelectorAll('iframe')) {
                try {
                    const frameDocument = frame.contentDocument;
                    if (!frameDocument) continue;

                    const editor = findEditor(frameDocument);
                    if (editor) return editor;
                } catch (_) {
                    // Ignore cross-origin frames, which cannot be inspected.
                }
            }

            return null;
        }

        /**
         * Places a collapsed DOM selection at the end of a contenteditable editor.
         *
         * Calling HTMLElement.focus() alone does not guarantee that contenteditable
         * editors such as Lexical have a valid insertion range. A real selection
         * range is required when appending content through beforeinput.
         *
         * @param {HTMLElement} editor The contenteditable editor root.
         * @returns {boolean} true when the caret was successfully positioned.
         */
        function placeCaretAtEnd(editor) {
            const editorWindow = editor.ownerDocument.defaultView;
            const selection = editorWindow?.getSelection();

            if (!selection) {
                console.warn("MyChat: unable to obtain the editor selection");
                return false;
            }

            const range = editor.ownerDocument.createRange();

            // Select the complete editor content and collapse the range at its end.
            range.selectNodeContents(editor);
            range.collapse(false);

            selection.removeAllRanges();
            selection.addRange(range);

            return true;
        }

        const el = findEditor(document);
        if (!el) {
            console.warn("MyChat: no supported chat editor found");
            return;
        }

        el.focus();

        /*
         * Expected logs:
         *
         * ChatGPT:
         *   MyChat: inserted line by line 44 characters, 7 lines, for DIV
         *
         * Copilot:
         *   MyChat: dispatched beforeinput 44 characters for SPAN
         *
         * Google:
         *   MyChat: embedded 44 characters for TEXTAREA
         *
         * Gemini:
         *   MyChat: inserted line by line 44 characters, 7 lines, for DIV
         *
         * Claude:
         *   MyChat: inserted line by line 44 characters, 7 lines, for DIV
         *
         * Perplexity:
         *   MyChat: dispatched beforeinput 44 characters for DIV
         *
         * Grok:
         *   MyChat: inserted line by line 44 characters, 7 lines, for DIV
         *
         * Duck:
         *   MyChat: embedded 44 characters for TEXTAREA
         */

        const elementWindow = el.ownerDocument.defaultView;
        if (!elementWindow) {
            console.warn("MyChat: unable to obtain the editor window");
            return;
        }

        const isTextArea = el instanceof elementWindow.HTMLTextAreaElement;
        const isInput = el instanceof elementWindow.HTMLInputElement;

        if (isTextArea || isInput) {
            // Standard input controls used by Google and Duck.

            /**
             * IntelliJ's JavaScript analyzer may not narrow an HTMLElement based only
             * on tagName. The runtime instanceof checks above ensure that this element
             * is a textarea or input control.
             *
             * @type {HTMLTextAreaElement|HTMLInputElement}
             */
            const textControl = el;

            /*
             * Obtain the native value setter for the actual control type.
             * React-controlled fields may require the native setter rather than direct
             * assignment to textControl.value.
             */
            const valuePrototype = isTextArea
                ? elementWindow.HTMLTextAreaElement.prototype
                : elementWindow.HTMLInputElement.prototype;

            const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                valuePrototype,
                'value'
            )?.set;

            if (!nativeInputValueSetter) {
                console.warn(
                    "MyChat: unable to obtain the native value setter for",
                    textControl.tagName
                );
                return;
            }

            /*
             * Insert at the current selection. Both selectionStart and selectionEnd
             * can be null for input types that do not support text selection, so fall
             * back to appending at the end.
             */
            const selectionStart =
                textControl.selectionStart ?? textControl.value.length;

            const selectionEnd =
                textControl.selectionEnd ?? selectionStart;

            const updatedValue =
                textControl.value.slice(0, selectionStart) +
                MARKDOWN_TEXT +
                textControl.value.slice(selectionEnd);

            nativeInputValueSetter.call(textControl, updatedValue);

            /*
             * Leave the caret immediately after the inserted Markdown. Some input
             * types do not support setSelectionRange(), although the method exists on
             * HTMLInputElement, so protect the call against unsupported input types.
             */
            const newCaretPosition = selectionStart + MARKDOWN_TEXT.length;

            try {
                textControl.setSelectionRange(
                    newCaretPosition,
                    newCaretPosition
                );
            } catch (error) {
                console.warn(
                    "MyChat: unable to update the text-control selection",
                    error
                );
            }
            // Notify React or another controlling framework about the value change.
            textControl.dispatchEvent(new Event('input', {bubbles: true}));

            console.log(
                "MyChat: embedded",
                MARKDOWN_TEXT.length,
                "characters for",
                textControl.tagName
            );

            return;
        }

        /*
         * Process either a standard contenteditable editor or a Lexical editor.
         * Copilot and Perplexity currently use Lexical-based contenteditable roots.
         */
        const isLexical = el.hasAttribute('data-lexical-editor');

        if (isLexical) {
            /*
             * Lexical needs both DOM focus and a valid selection range.
             * Focus alone may leave the editor without an insertion point,
             * particularly when existing content is already present.
             */
            el.focus();

            if (!placeCaretAtEnd(el)) {
                console.warn(
                    "MyChat: unable to place the caret in the Lexical editor"
                );
                return;
            }

            /*
             * Dispatch beforeinput after establishing the selection. Lexical uses
             * the current DOM selection to determine the insertion position.
             */
            const wasNotCanceled = el.dispatchEvent(new InputEvent('beforeinput', {
                bubbles: true,
                cancelable: true,
                inputType: 'insertText',
                data: MARKDOWN_TEXT
            }));

            console.log(
                "MyChat: dispatched beforeinput",
                MARKDOWN_TEXT.length,
                "characters for",
                el.tagName,
                "default prevented:",
                !wasNotCanceled
            );

            return;
        }

        /*
         * Standard contenteditable editor: insert each line separately and create
         * paragraph boundaries between lines.
         *
         * execCommand() is deprecated, but remains a compatibility fallback for
         * framework-managed editors. Direct DOM manipulation could leave the state
         * of the framework's internal-editor out of sync with the displayed content.
         */
        const editorDocument = el.ownerDocument;

        if (typeof editorDocument.execCommand !== 'function') {
            console.warn(
                "MyChat: this browser does not support contenteditable insertion"
            );
            return;
        }

        const lines = MARKDOWN_TEXT.split('\n');

        for (let i = 0; i < lines.length; i++) {
            // noinspection JSDeprecatedSymbols
            const textInserted = editorDocument.execCommand(
                'insertText',
                false,
                lines[i]
            );

            if (!textInserted && lines[i].length > 0) {
                console.warn(
                    "MyChat: insertText was not handled for line",
                    i + 1
                );
            }

            if (i < lines.length - 1) {
                // noinspection JSDeprecatedSymbols
                const paragraphInserted = editorDocument.execCommand(
                    'insertParagraph',
                    false
                );

                if (!paragraphInserted) {
                    console.warn(
                        "MyChat: insertParagraph was not handled after line",
                        i + 1
                    );
                }
            }
        }

        console.log(
            "MyChat: inserted line by line",
            MARKDOWN_TEXT.length,
            "characters,",
            lines.length,
            "lines, for",
            el.tagName
        );
    }
)();