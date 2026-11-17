package com.mochyy.mochymusic.ui.customization

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.mochyy.mochymusic.constants.*
import com.mochyy.mochymusic.ui.screens.Screens
import com.mochyy.mochymusic.utils.dataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

/** Explicit appearance-only allowlist: credentials, music, downloads and accounts cannot enter a theme file. */
object CustomizationProfiles {
    private val booleans = listOf(UiContrastKey, UiNavSwipeKey, UiPressEffectsKey, UiHapticsKey, UiLoadingMotionKey,
        PureBlackKey, DisableBlurKey, EnableProgressiveBlurKey, ElementBlurKey, EnableElementBlurAnimatorKey,
        DiscRotatingAnimationKey, CanvasThumbnailAnimationKey, HideSettingsDescriptionsKey, MiniPlayerOutlineKey,
        PureBlackMiniPlayerKey, HideAudioQualityBadgeKey, EnableHighRefreshRateKey,
        PlayerSwipeDownAnywhereKey, PlayerSwipeUpFollowsKey, PlayerStatusBarKey, PlayerNavBarKey)
    private val floats = mapOf(UiTextScaleKey to (.85f..1.25f), AnimationSpeedKey to (.5f..2f),
        ThumbnailCornerRadiusKey to (0f..45f), BlurStrengthPxKey to (0f..1500f))
    private val strings: Map<Preferences.Key<String>, Set<String>> = mapOf(
        UiAccentKey to UiAccent.entries.map { it.name }.toSet(),
        UiFontKey to UiFont.entries.map { it.name }.toSet(),
        UiCornersKey to UiCorners.entries.map { it.name }.toSet(),
        UiSpacingKey to UiSpacing.entries.map { it.name }.toSet(),
        UiLibraryRowsKey to UiSpacing.entries.map { it.name }.toSet(),
        UiNavStyleKey to UiNavStyle.entries.map { it.name }.toSet(),
        UiNavLabelsKey to UiNavLabels.entries.map { it.name }.toSet(),
        UiNavIndicatorKey to UiNavIndicator.entries.map { it.name }.toSet(),
        UiNavIconSizeKey to UiNavIconSize.entries.map { it.name }.toSet(),
        UiTransitionKey to UiTransition.entries.map { it.name }.toSet(),
        UiCharacterKey to UiCharacter.entries.map { it.name }.toSet(),
        BlurQualityKey to BlurQuality.entries.map { it.name }.toSet(),
        DefaultOpenTabKey to setOf("HOME", "SEARCH"),
        PlayerStyleKey to PlayerStyle.entries.map { it.name }.toSet(),
        PlaylistStyleKey to PlaylistStyle.entries.map { it.name }.toSet(),
        HomeStyleKey to HomeStyle.entries.map { it.name }.toSet(),
        GridItemsSizeKey to GridItemSize.entries.map { it.name }.toSet(),
    )
    fun capture(prefs: Preferences): JSONObject = JSONObject().apply {
        booleans.forEach { key -> prefs[key]?.let { put(key.name, it) } }
        floats.forEach { (key, range) -> prefs[key]?.takeIf { it.isFinite() }?.let { put(key.name, it.coerceIn(range)) } }
        strings.forEach { (key, values) -> prefs[key]?.takeIf { it in values }?.let { put(key.name, it) } }
        prefs[UiCustomAccentKey]?.let { put(UiCustomAccentKey.name, it) }
        prefs[UiTransitionDurationKey]?.let { put(UiTransitionDurationKey.name, it.coerceIn(100, 500)) }
        prefs[UiNavOrderKey]?.let { put(UiNavOrderKey.name, normalizedOrder(it)) }
        prefs[UiHomeOrderKey]?.let { put(UiHomeOrderKey.name, it.take(512)) }
        prefs[UiHomeHiddenKey]?.let { put(UiHomeHiddenKey.name, it.take(512)) }
    }
    private fun normalizedOrder(order: String): String =
        UiCustomization.ordered(Screens.MainScreens, order) { it.route }.joinToString(",") { it.route }

    /** Sanitization occurs before the transaction. Unknown keys are ignored, invalid known values are rejected. */
    private fun sanitized(settings: JSONObject): JSONObject {
        require(settings.length() <= 128) { "Too many theme settings" }
        val result = JSONObject()
        booleans.forEach { key -> if (settings.has(key.name)) {
            val value = settings.get(key.name); require(value is Boolean) { "Invalid boolean setting" }; result.put(key.name, value)
        } }
        floats.forEach { (key, range) -> if (settings.has(key.name)) {
            val value = settings.get(key.name); require(value is Number && value.toFloat().isFinite()) { "Invalid numeric setting" }
            result.put(key.name, value.toFloat().coerceIn(range))
        } }
        strings.forEach { (key, allowed) -> if (settings.has(key.name)) {
            val value = settings.get(key.name); require(value is String && value in allowed) { "Unsupported theme option" }; result.put(key.name, value)
        } }
        if (settings.has(UiCustomAccentKey.name)) {
            val value = settings.get(UiCustomAccentKey.name); require(value is Number) { "Invalid accent color" }
            result.put(UiCustomAccentKey.name, value.toInt() or 0xFF000000.toInt())
        }
        if (settings.has(UiTransitionDurationKey.name)) {
            val value = settings.get(UiTransitionDurationKey.name); require(value is Number) { "Invalid duration" }
            result.put(UiTransitionDurationKey.name, value.toInt().coerceIn(100, 500))
        }
        if (settings.has(UiNavOrderKey.name)) {
            val value = settings.get(UiNavOrderKey.name); require(value is String && value.length <= 256) { "Invalid tab order" }
            result.put(UiNavOrderKey.name, normalizedOrder(value))
        }
        for (key in listOf(UiHomeOrderKey, UiHomeHiddenKey)) if (settings.has(key.name)) {
            val value = settings.get(key.name); require(value is String && value.length <= 512) { "Invalid Home layout" }
            result.put(key.name, value.split(',').distinct().filter { it in UiCustomization.HomeShelves }.joinToString(","))
        }
        require(settings.length() == 0 || result.length() > 0) { "No supported theme settings" }
        return result
    }
    suspend fun apply(context: Context, settings: JSONObject) {
        val safe = sanitized(settings)
        context.dataStore.edit { p ->
            booleans.forEach { key -> p.remove(key); if (safe.has(key.name)) p[key] = safe.getBoolean(key.name) }
            floats.keys.forEach { key -> p.remove(key); if (safe.has(key.name)) p[key] = safe.getDouble(key.name).toFloat() }
            strings.keys.forEach { key -> p.remove(key); if (safe.has(key.name)) p[key] = safe.getString(key.name) }
            p.remove(UiCustomAccentKey); if (safe.has(UiCustomAccentKey.name)) p[UiCustomAccentKey] = safe.getInt(UiCustomAccentKey.name)
            p.remove(UiTransitionDurationKey); if (safe.has(UiTransitionDurationKey.name)) p[UiTransitionDurationKey] = safe.getInt(UiTransitionDurationKey.name)
            p.remove(UiNavOrderKey); if (safe.has(UiNavOrderKey.name)) p[UiNavOrderKey] = safe.getString(UiNavOrderKey.name)
            for (key in listOf(UiHomeOrderKey, UiHomeHiddenKey)) { p.remove(key); if (safe.has(key.name)) p[key] = safe.getString(key.name) }
        }
    }
    suspend fun reset(context: Context) = apply(context, JSONObject())
    suspend fun export(context: Context): String = JSONObject().put("schema", 1)
        .put("settings", capture(context.dataStore.data.first())).toString(2)
    suspend fun importDocument(context: Context, text: String) {
        require(text.length <= 65_536) { "Theme file is too large" }
        var depth = 0; var quoted = false; var escaped = false
        for (char in text) {
            if (quoted) {
                if (escaped) escaped = false else if (char == '\\') escaped = true else if (char == '"') quoted = false
            } else when (char) {
                '"' -> quoted = true
                '{', '[' -> { depth++; require(depth <= 8) { "Theme nesting is too deep" } }
                '}', ']' -> depth--
            }
        }
        val document = JSONObject(text)
        require(document.optInt("schema") == 1) { "Unsupported theme format" }
        apply(context, document.getJSONObject("settings"))
    }
    fun profiles(raw: String): JSONArray = runCatching { JSONArray(raw).takeIf { it.length() <= 5 } }.getOrNull() ?: JSONArray()
    suspend fun save(context: Context, name: String) {
        val label = name.trim().take(32); require(label.isNotEmpty()) { "Enter a preset name" }
        context.dataStore.edit { p ->
            val saved = profiles(p[UiSavedProfilesKey] ?: "[]")
            val next = JSONArray()
            for (i in 0 until saved.length()) {
                val entry = saved.optJSONObject(i) ?: continue
                if (!entry.optString("name").equals(label, ignoreCase = true)) next.put(entry)
            }
            require(next.length() < 5) { "You can keep up to five presets" }
            next.put(JSONObject().put("name", label).put("settings", capture(p)))
            p[UiSavedProfilesKey] = next.toString()
        }
    }
    suspend fun delete(context: Context, name: String) {
        context.dataStore.edit { p ->
            val saved = profiles(p[UiSavedProfilesKey] ?: "[]"); val next = JSONArray()
            for (i in 0 until saved.length()) saved.optJSONObject(i)?.takeIf { it.optString("name") != name }?.let { next.put(it) }
            p[UiSavedProfilesKey] = next.toString()
        }
    }
    /** Presets are applied once and do not override subsequent manual choices. */
    suspend fun performance(context: Context, level: Int) {
        context.dataStore.edit { p ->
            val light = level == 0; val expressive = level == 2
            p[DisableBlurKey] = light
            p[EnableProgressiveBlurKey] = !light
            p[BlurQualityKey] = if (expressive) BlurQuality.HIGH.name else BlurQuality.LOW.name
            p[ElementBlurKey] = expressive
            p[EnableElementBlurAnimatorKey] = true
            p[AnimationSpeedKey] = if (light) 1.25f else 1f
            p[UiPressEffectsKey] = !light
            p[UiLoadingMotionKey] = !light
            p[DiscRotatingAnimationKey] = !light
            p[CanvasThumbnailAnimationKey] = expressive
            p[UiNavStyleKey] = if (light) UiNavStyle.FLAT.name else UiNavStyle.CURRENT.name
            p[UiTransitionKey] = if (expressive) UiTransition.SCALE.name else UiTransition.FADE.name
            p[UiTransitionDurationKey] = if (light) 140 else if (expressive) 300 else 200
        }
    }
}
