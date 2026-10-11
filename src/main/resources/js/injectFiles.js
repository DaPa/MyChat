(
    // used in ComponentUtils.kt, see INJECT_FILES_JS_TEMPLATE
    function () {
        const SELECTORS_JS_ARRAY = __SELECTORS_JS_ARRAY__;
        const ATTACH_DELAY_MS = Number(`__ATTACH_DELAY_MS__`);

        try {
            const dataTransfer = new DataTransfer();

            __FILE_BLOBS__

            const url = location.origin;  // only for logging
            setTimeout(function () {
                const selectors = SELECTORS_JS_ARRAY;
                let input = document.querySelector('input[type="file"]');
                console.log("MyChat: initial input ", input);

                if (input) {
                    // script found the hidden "input" directly, no button click necessary
                    console.log("MYCHAT_UPLOAD_PATH: " + url + " hidden \"input\" found directly!");
                    injectFiles(input);
                } else {
                    // 1. find button
                    let attachBtn = null;
                    let matchedSelector = null;
                    console.log("MyChat: selectors ", selectors);
                    for (const sel of selectors) {
                        try {
                            const el = document.querySelector(sel);
                            if (el) {
                                console.info("MyChat: found selector & el: ", sel, el);
                                attachBtn = el.closest("button") || el;
                                matchedSelector = sel;
                                break;
                            }
                        } catch (e) {
                            console.error("MyChat: invalid selector: ", sel, e);
                            console.log("MYCHAT_UPLOAD_PATH: " + url + " invalid selector ", sel, e);
                        }
                    }

                    if (attachBtn) {
                        // 2. click button
                        attachBtn.click();
                        // 3. wait for the button click to complete
                        setTimeout(function () {
                            input = document.querySelector('input[type="file"]');
                            if (input) {
                                console.log("MYCHAT_UPLOAD_PATH: " + url + " used SELECTOR " + matchedSelector);
                                // 4. inject files
                                injectFiles(input);
                            } else {
                                console.log("MYCHAT_UPLOAD_PATH: " + url + " failed using SELECTOR " + matchedSelector);
                                console.error("MyChat: no input found after button click");
                            }
                        }, ATTACH_DELAY_MS);
                    } else {
                        console.log("MYCHAT_UPLOAD_PATH: " + url + " no attachBtn found. sel = '" + matchedSelector
                                    + "' in selectors: " + JSON.stringify(selectors));
                        console.error("MyChat: no attachBtn found");
                    }
                }

                function injectFiles(inputEl) {
                    if (!inputEl) return;
                    try {
                        inputEl.files = dataTransfer.files;
                        inputEl.dispatchEvent(new Event('change', { bubbles: true }));
                        console.log("MyChat: injected " + dataTransfer.files.length + " file(s)");
                    } catch (e) {
                        console.error("MYCHAT_UPLOAD_PATH: " + url + " file injection failed ", e);
                    }
                }
            }, ATTACH_DELAY_MS);

        } catch (e) {
            console.error("MyChat: DnD injection failed ", e);
        }
    }
)();
