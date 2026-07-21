package com.dapa1.utils

/**
 * Normalizes a URL for reliable prefix matching, stripping scheme, and trailing slash.
 *
 * Without normalization, these would all fail to match "https://chatgpt.com":
 *   "https://chatgpt.com/" (trailing slash)
 *   "HTTPS://ChatGPT.com" (case difference)
 *   "http://chatgpt.com"  (different scheme)
 *
 * Note: query strings and paths (e.g. "?model=4") are intentionally left intact —
 * startsWith() matching in findMatchingSite() still works because the configured
 * site URL (without a path) will be a prefix of the full current URL.
 *
 * Extracted from MyChatWindow so it can be tested independently and reused
 * by any other part of the plugin that needs URL comparison.
 */
@Suppress("HttpUrlsUsage")  // we handle both schemes intentionally
fun normalizeUrl(url: String): String =
    url.lowercase()
        .removePrefix("https://")
        .removePrefix("http://")
        .removeSuffix("/")
