(
    // used in ComponentUtils.kt, see MARKDOWN_JS_TEMPLATE
    function() {
        const MARKDOWN_TEXT = `__ESCAPED_MARKDOWN_TEXT__`;

        function findEditor(doc) {
            // Try the stable ID first — Copilot
            const byId = doc.getElementById('m365-chat-editor-target-element');
            if (byId) return byId;

            // Try Lexical attribute as fallback
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
            for (const sel of SELECTORS) {
                const el = doc.querySelector(sel);
                if (el) return el;
            }
            for (const frame of doc.querySelectorAll('iframe')) {
                try {
                    const el = findEditor(frame.contentDocument);
                    if (el) return el;
                } catch (_) {}
            }
            return null;
        }
        let el = findEditor(document);
        if (!el) { console.warn("MyChat: no textarea found"); return; }

        el.focus();

        /*
         * logs:
         *   ChatGPT:
         *   	MyChat: insert line by line 44 characters, 7 lines, for DIV
         *   Copilot:
         *   	MyChat: dispatchEvent beforeinput 44 characters for SPAN
         *   Google:
         *   	MyChat: embedded 44 characters for TEXTAREA
         *   Gemini:
         *   	MyChat: insert line by line 44 characters, 7 lines, for DIV
         *   Claude:
         *   	MyChat: insert line by line 44 characters, 7 lines, for DIV
         *   Perplexity:
         *   	MyChat: dispatchEvent beforeinput 44 characters for DIV
         *   Grok:
         *   	MyChat: insert line by line 44 characters, 7 lines, for DIV
         *   Duck:
         *   	MyChat: embedded 44 characters for TEXTAREA
         */

        if (el.tagName === 'TEXTAREA' || el.tagName === 'INPUT') {
            // for Google and Duck
            const nativeInputValueSetter = Object.getOwnPropertyDescriptor(
                window.HTMLTextAreaElement.prototype, 'value').set;
            nativeInputValueSetter.call(el, MARKDOWN_TEXT);
            el.dispatchEvent(new Event('input', { bubbles: true }));
            console.log("MyChat: embedded", MARKDOWN_TEXT.length, "characters for", el.tagName, "");

        } else {
            // contenteditable (ChatGPT, Gemini, Claude, Grok) or Lexical (Copilot, Perplexity)
            const isLexical = el.hasAttribute('data-lexical-editor');

            if (isLexical) {
                // Dispatch beforeinput — Lexical's native insertion mechanism (for Copilot and Perplexity)
                el.dispatchEvent(new InputEvent('beforeinput', {
                    bubbles: true,
                    cancelable: true,
                    inputType: 'insertText',
                    data: MARKDOWN_TEXT
                }));
                console.log("MyChat: dispatchEvent beforeinput", MARKDOWN_TEXT.length, "characters for", el.tagName, "");
            } else {
                // Standard contenteditable: insert line by line (for ChatGPT, Gemini, Claude and Grok)
                const lines = MARKDOWN_TEXT.split('\n');
                for (let i = 0; i < lines.length; i++) {
                    document.execCommand('insertText', false, lines[i]);
                    if (i < lines.length - 1) {
                        document.execCommand('insertParagraph', false);
                    }
                }
                console.log("MyChat: insert line by line", MARKDOWN_TEXT.length, "characters,", lines.length, "lines, for", el.tagName, "");
            }
        }

    }
)();
