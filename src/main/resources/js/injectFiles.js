(
    // used in ComponentUtils.kt, see INJECT_FILES_JS_TEMPLATE
    function () {
        const SELECTORS_JS_ARRAY = __SELECTORS_JS_ARRAY__;
        const ATTACH_DELAY_MS = Number(`__ATTACH_DELAY_MS__`);

        try {
            const dataTransfer = new DataTransfer();

            __FILE_BLOBS__

            setTimeout(function () {
                const selectors = SELECTORS_JS_ARRAY;
                let input = document.querySelector('input[type="file"]');

                if (!input) {
                    let attachBtn = null;
                    console.log("MyChat: selectors", selectors);
                    for (const sel of selectors) {
                        try {
                            const el = document.querySelector(sel);
                            if (el) {
                                console.info("MyChat: found selector & el:", sel, el);
                                attachBtn = el.closest("button") || el;
                                break;
                            }
                        } catch (e) {
                            console.error("MyChat: invalid selector:", sel, e);
                        }
                    }

                    if (attachBtn) {
                        attachBtn.click();
                        setTimeout(function () {
                            input = document.querySelector('input[type="file"]');
                            injectFiles(input);
                        }, ATTACH_DELAY_MS);
                    } else {
                        console.error("MyChat: no attachBtn found");
                    }
                } else {
                    injectFiles(input);
                }

                function injectFiles(inputEl) {
                    if (!inputEl) return;
                    try {
                        inputEl.files = dataTransfer.files;
                        inputEl.dispatchEvent(new Event('change', { bubbles: true }));
                        console.log("MyChat: injected " + dataTransfer.files.length + " file(s)");
                    } catch (e) {
                        console.error("MyChat: file injection failed", e);
                    }
                }
            }, ATTACH_DELAY_MS);

        } catch (e) {
            console.error("MyChat: DnD injection failed", e);
        }
    }
)();
