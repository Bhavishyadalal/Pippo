package com.mochyy.mochymusic.modules

import java.net.URI

/** Prefer the saved installation URL; built-in streaming modules can expose their server URL. */
internal fun moduleCopyLink(
    id: String,
    installationUrl: String?,
    webBase: String?,
    serverUrls: List<String>,
    projectUrl: String?,
): String? {
    if (id == "youtube" || id == "jiosaavn") return null
    return (listOf(installationUrl, webBase) + serverUrls + projectUrl).firstNotNullOfOrNull { candidate ->
        candidate?.trim()?.takeIf { value ->
            runCatching {
                val uri = URI(value)
                (uri.scheme.equals("https", true) || uri.scheme.equals("http", true)) && !uri.host.isNullOrBlank()
            }.getOrDefault(false)
        }
    }
}
