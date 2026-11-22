package com.mochyy.mochymusic.modules

import android.content.res.Resources
import com.mochyy.mochymusic.modules.web.WebModule
import com.mochyy.mochymusic.modules.web.WebModuleManifest
import kotlinx.serialization.json.JsonPrimitive

/** Shipped metadata keeps module discovery independent of network access at startup. */
internal object BundledAudioModules {
    private data class Definition(val base: String, val manifest: WebModuleManifest) {
        val id = WebModuleManifest.idFor(base, manifest)
    }

    private val playback = listOf(JsonPrimitive("search"), JsonPrimitive("stream"))
    private val quality = WebModuleManifest.Setting(
        key = "quality",
        type = "select",
        default = JsonPrimitive("lossless"),
        options = listOf("lossless", "high", "low").map { JsonPrimitive(it) },
    )

    private val definitions = listOf(
        Definition(
            "https://flac-pocket.onrender.com/m",
            WebModuleManifest(
                id = "io.github.bhavishyadalal.flacpocket",
                name = "FLAC Pocket",
                version = "3.1.3",
                icon = "https://flac-pocket.onrender.com/pocket/mark.png",
                resources = playback,
            ),
        ),
        Definition(
            "https://flac-relay.onrender.com",
            WebModuleManifest(
                id = "com.bitchord.qobuz",
                name = "FLAC Relay",
                version = "1.0.0",
                resources = playback,
                settings = listOf(quality),
            ),
        ),
        Definition(
            "https://nct.s1ncx.site",
            WebModuleManifest(
                id = "nhaccuatui",
                name = "NhacCuaTui",
                author = "s1ncx",
                icon = "icon.png",
                resources = playback + listOf(JsonPrimitive("lyrics"), JsonPrimitive("metadata")),
            ),
        ),
        Definition(
            "https://lossless-extension-i2a2.vercel.app",
            WebModuleManifest(
                id = "com.echomusic.lossless",
                name = "Lossless Extension",
                version = "1.0.0",
                resources = playback,
                settings = listOf(quality),
            ),
        ),
    )

    val ids = definitions.map { it.id }

    fun create(resources: Resources): List<WebModule> =
        definitions.map { WebModule(it.base, it.manifest, resources) }

    fun secureBase(id: String, savedBase: String): String {
        val base = definitions.firstOrNull { it.id == id }?.base ?: return savedBase
        return if (savedBase == "http://" + base.removePrefix("https://")) base else savedBase
    }
}
