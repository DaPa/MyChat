(
    // used in ComponentUtils.kt, see FILE_BLOB_JS_TEMPLATE
    function (dataTransfer) {
        const BASE64_DATA = `__BASE64_DATA__`;
        const MIME_TYPE = `__MIME_TYPE__`;
        const SAFE_NAME = `__SAFE_NAME__`;

        const byteCharacters = atob(BASE64_DATA);
        const byteNumbers = new Array(byteCharacters.length);

        for (let i = 0; i < byteCharacters.length; i++) {
            byteNumbers[i] = byteCharacters.charCodeAt(i);
        }

        const blob = new Blob([new Uint8Array(byteNumbers)], {type: MIME_TYPE});
        // dataTransfer is defined as DataTransfer in injectFiles.js
        dataTransfer.items.add(new File([blob], SAFE_NAME, {type: MIME_TYPE}));
    }
)(dataTransfer);
