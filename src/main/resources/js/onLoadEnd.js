(
    // used in ComponentUtils.kt, see ON_LOAD_END_JS_TEMPLATE
    function() {
        const PLUS_BTN_SELECTOR = `__PLUS_BTN_SELECTOR__`;
        const OBSERVER_TIMEOUT_MS = Number(`__OBSERVER_TIMEOUT_MS__`);

        const selector = `[data-testid="${PLUS_BTN_SELECTOR}"]`;
        console.log("MyChat: onLoadEnd script injected for selector", selector);

        function tryApply() {
            const btn = document.querySelector(selector);
            if (btn) {
                console.log("MyChat: found button for selector", selector);
                return true;
            }
            return false;
        }

        if (tryApply()) return;

        const observer = new MutationObserver(function () {
            if (tryApply()) observer.disconnect();
        });

        observer.observe(document.body, { childList: true, subtree: true });

        // Safety timeout: disconnect if the selector never matches (e.g., unsupported site).
        setTimeout(function () {
            observer.disconnect();
        }, OBSERVER_TIMEOUT_MS);
    }
)();
