package com.mochyy.mochymusic.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.annotation.StyleRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.mochyy.mochymusic.R
import timber.log.Timber

private const val AdaptiveArtworkScale = 1.5f // 108 dp layer / 72 dp visible viewport

enum class AppIconFamily(@StringRes val title: Int) {
    Brand(R.string.pippo_master_group_brand),
    PinArtwork(R.string.pippo_pin_group),
    Lettering(R.string.pippo_master_group_lettering),
    Anime(R.string.pippo_icons_portraits),
}

class AppIcon private constructor(
    val name: String,
    val alias: String,
    val family: AppIconFamily,
    @DrawableRes val foreground: Int,
    @ColorRes val background: Int?,
    @StringRes val label: Int,
    @StyleRes val splashTheme: Int,
    // Match the 72 dp launcher viewport after its adaptive layer's inset. The picker,
    // startup animation and Home badge all use these same framing proportions.
    internal val foregroundScale: Float = .96f,
) {
    companion object {
        private fun pin(
            name: String,
            alias: String,
            @DrawableRes artwork: Int,
            @StringRes label: Int,
            @StyleRes splashTheme: Int,
        ) = AppIcon(name, alias, AppIconFamily.PinArtwork, artwork, null, label, splashTheme, foregroundScale = AdaptiveArtworkScale)

        val entries: List<AppIcon> = listOf(
            AppIcon("Default", "IconDefault", AppIconFamily.Brand, R.drawable.pippo_master_black_ribbon_p, R.color.pippo_master_black_ribbon_p_bg, R.string.pippo_master_black_ribbon_p_label, R.style.Theme_App_Starting, foregroundScale = 1.38f),
            AppIcon(
                name = "SilverHeadphones",
                alias = "PippoSilverHeadphones",
                family = AppIconFamily.Brand,
                foreground = R.drawable.pippo_silver_headphones_adaptive,
                background = R.color.pippo_master_black_ribbon_p_bg,
                label = R.string.pippo_silver_headphones_label,
                splashTheme = R.style.Theme_App_Starting_SilverHeadphones,
                foregroundScale = AdaptiveArtworkScale,
            ),
            pin("Pin01", "PippoPin01", R.drawable.pippo_pin_01_midnight_stars_adaptive, R.string.pippo_pin_01_midnight_stars_label, R.style.Theme_App_Starting_Pin01),
            pin("Pin02", "PippoPin02", R.drawable.pippo_pin_02_sky_headphones_adaptive, R.string.pippo_pin_02_sky_headphones_label, R.style.Theme_App_Starting_Pin02),
            pin("Pin03", "PippoPin03", R.drawable.pippo_pin_03_mint_hearts_adaptive, R.string.pippo_pin_03_mint_hearts_label, R.style.Theme_App_Starting_Pin03),
            pin("Pin04", "PippoPin04", R.drawable.pippo_pin_04_mocha_leaves_adaptive, R.string.pippo_pin_04_mocha_leaves_label, R.style.Theme_App_Starting_Pin04),
            pin("Pin05", "PippoPin05", R.drawable.pippo_pin_05_cherry_float_adaptive, R.string.pippo_pin_05_cherry_float_label, R.style.Theme_App_Starting_Pin05),
            pin("Pin06", "PippoPin06", R.drawable.pippo_pin_06_woodland_sketch_adaptive, R.string.pippo_pin_06_woodland_sketch_label, R.style.Theme_App_Starting_Pin06),
            pin("Pin07", "PippoPin07", R.drawable.pippo_pin_07_red_crayon_adaptive, R.string.pippo_pin_07_red_crayon_label, R.style.Theme_App_Starting_Pin07),
            pin("Pin08", "PippoPin08", R.drawable.pippo_pin_08_blue_sketch_note_adaptive, R.string.pippo_pin_08_blue_sketch_note_label, R.style.Theme_App_Starting_Pin08),
            pin("Pin09", "PippoPin09", R.drawable.pippo_pin_09_lilac_note_adaptive, R.string.pippo_pin_09_lilac_note_label, R.style.Theme_App_Starting_Pin09),
            pin("Pin10", "PippoPin10", R.drawable.pippo_pin_10_lilac_stars_adaptive, R.string.pippo_pin_10_lilac_stars_label, R.style.Theme_App_Starting_Pin10),
            pin("Pin11", "PippoPin11", R.drawable.pippo_pin_11_strawberry_green_adaptive, R.string.pippo_pin_11_strawberry_green_label, R.style.Theme_App_Starting_Pin11),
            pin("Pin12", "PippoPin12", R.drawable.pippo_pin_12_woodland_note_adaptive, R.string.pippo_pin_12_woodland_note_label, R.style.Theme_App_Starting_Pin12),
            pin("Pin13", "PippoPin13", R.drawable.pippo_pin_13_stardust_cat_adaptive, R.string.pippo_pin_13_stardust_cat_label, R.style.Theme_App_Starting_Pin13),
            pin("Pin14", "PippoPin14", R.drawable.pippo_pin_14_coral_headphones_adaptive, R.string.pippo_pin_14_coral_headphones_label, R.style.Theme_App_Starting_Pin14),
            pin("Pin15", "PippoPin15", R.drawable.pippo_pin_15_neon_shades_adaptive, R.string.pippo_pin_15_neon_shades_label, R.style.Theme_App_Starting_Pin15),
            pin("Pin16", "PippoPin16", R.drawable.pippo_pin_16_green_crayon_adaptive, R.string.pippo_pin_16_green_crayon_label, R.style.Theme_App_Starting_Pin16),
            pin("Pin17", "PippoPin17", R.drawable.pippo_pin_17_midnight_headphones_adaptive, R.string.pippo_pin_17_midnight_headphones_label, R.style.Theme_App_Starting_Pin17),
            pin("Pin18", "PippoPin18", R.drawable.pippo_pin_18_mossy_cat_adaptive, R.string.pippo_pin_18_mossy_cat_label, R.style.Theme_App_Starting_Pin18),
            pin("Pin19", "PippoPin19", R.drawable.pippo_pin_19_cloud_headphones_adaptive, R.string.pippo_pin_19_cloud_headphones_label, R.style.Theme_App_Starting_Pin19),
            pin("Pin20", "PippoPin20", R.drawable.pippo_pin_20_ink_headphones_adaptive, R.string.pippo_pin_20_ink_headphones_label, R.style.Theme_App_Starting_Pin20),
            pin("Pin21", "PippoPin21", R.drawable.pippo_pin_21_candy_web_adaptive, R.string.pippo_pin_21_candy_web_label, R.style.Theme_App_Starting_Pin21),
            pin("Pin22", "PippoPin22", R.drawable.pippo_pin_22_suit_and_shades_adaptive, R.string.pippo_pin_22_suit_and_shades_label, R.style.Theme_App_Starting_Pin22),
            pin("Pin23", "PippoPin23", R.drawable.pippo_pin_23_arcade_vampire_adaptive, R.string.pippo_pin_23_arcade_vampire_label, R.style.Theme_App_Starting_Pin23),
            pin("Pin24", "PippoPin24", R.drawable.pippo_pin_24_rooftop_toast_adaptive, R.string.pippo_pin_24_rooftop_toast_label, R.style.Theme_App_Starting_Pin24),
            AppIcon("Master_01_cobalt_wax", "PippoMaster01CobaltWax", AppIconFamily.Lettering, R.drawable.pippo_master_01_cobalt_wax, R.color.pippo_master_01_cobalt_wax_bg, R.string.pippo_master_01_cobalt_wax_label, R.style.Theme_App_Starting_Master01CobaltWax, foregroundScale = 1.11f),
            AppIcon("Master_02_graphite_script", "PippoMaster02GraphiteScript", AppIconFamily.Lettering, R.drawable.pippo_master_02_graphite_script, R.color.pippo_master_02_graphite_script_bg, R.string.pippo_master_02_graphite_script_label, R.style.Theme_App_Starting_Master02GraphiteScript, foregroundScale = 1.11f),
            AppIcon("Master_03_pink_pastel", "PippoMaster03PinkPastel", AppIconFamily.Lettering, R.drawable.pippo_master_03_pink_pastel, R.color.pippo_master_03_pink_pastel_bg, R.string.pippo_master_03_pink_pastel_label, R.style.Theme_App_Starting_Master03PinkPastel, foregroundScale = 1.11f),
            AppIcon("Master_04_black_ink_brush", "PippoMaster04BlackInkBrush", AppIconFamily.Lettering, R.drawable.pippo_master_04_black_ink_brush, R.color.pippo_master_04_black_ink_brush_bg, R.string.pippo_master_04_black_ink_brush_label, R.style.Theme_App_Starting_Master04BlackInkBrush, foregroundScale = 1.05f),
            AppIcon("Master_05_mint_pencil", "PippoMaster05MintPencil", AppIconFamily.Lettering, R.drawable.pippo_master_05_mint_pencil, R.color.pippo_master_05_mint_pencil_bg, R.string.pippo_master_05_mint_pencil_label, R.style.Theme_App_Starting_Master05MintPencil, foregroundScale = 1.11f),
            AppIcon("Master_06_yellow_retro_bubble", "PippoMaster06YellowRetroBubble", AppIconFamily.Lettering, R.drawable.pippo_master_06_yellow_retro_bubble, R.color.pippo_master_06_yellow_retro_bubble_bg, R.string.pippo_master_06_yellow_retro_bubble_label, R.style.Theme_App_Starting_Master06YellowRetroBubble, foregroundScale = 1.11f),
            AppIcon("Master_07_teal_peach_marker", "PippoMaster07TealPeachMarker", AppIconFamily.Lettering, R.drawable.pippo_master_07_teal_peach_marker, R.color.pippo_master_07_teal_peach_marker_bg, R.string.pippo_master_07_teal_peach_marker_label, R.style.Theme_App_Starting_Master07TealPeachMarker, foregroundScale = 1.11f),
            AppIcon("Master_08_lavender_paper_cut", "PippoMaster08LavenderPaperCut", AppIconFamily.Lettering, R.drawable.pippo_master_08_lavender_paper_cut, R.color.pippo_master_08_lavender_paper_cut_bg, R.string.pippo_master_08_lavender_paper_cut_label, R.style.Theme_App_Starting_Master08LavenderPaperCut, foregroundScale = 1.05f),
            AppIcon("Master_09_peach_dry_brush", "PippoMaster09PeachDryBrush", AppIconFamily.Lettering, R.drawable.pippo_master_09_peach_dry_brush, R.color.pippo_master_09_peach_dry_brush_bg, R.string.pippo_master_09_peach_dry_brush_label, R.style.Theme_App_Starting_Master09PeachDryBrush, foregroundScale = 1.11f),
            AppIcon("Master_10_sage_woodcut", "PippoMaster10SageWoodcut", AppIconFamily.Lettering, R.drawable.pippo_master_10_sage_woodcut, R.color.pippo_master_10_sage_woodcut_bg, R.string.pippo_master_10_sage_woodcut_label, R.style.Theme_App_Starting_Master10SageWoodcut, foregroundScale = 1.11f),
            AppIcon("Master_11_approved_blue_scrapbook", "PippoMaster11ApprovedBlueScrapbook", AppIconFamily.Lettering, R.drawable.pippo_master_11_approved_blue_scrapbook, R.color.pippo_master_11_approved_blue_scrapbook_bg, R.string.pippo_master_11_approved_blue_scrapbook_label, R.style.Theme_App_Starting_Master11ApprovedBlueScrapbook, foregroundScale = 1.08f),
            AppIcon("Master_blue_crayon_original", "PippoMasterBlueCrayonOriginal", AppIconFamily.Lettering, R.drawable.pippo_master_blue_crayon_original, R.color.pippo_master_blue_crayon_original_bg, R.string.pippo_master_blue_crayon_original_label, R.style.Theme_App_Starting_MasterBlueCrayonOriginal, foregroundScale = 1.08f),
            AppIcon("Pippo01", "PippoIcon01", AppIconFamily.Anime, R.drawable.pippo_icon_01, R.color.pippo_icon_bg_01, R.string.pippo_icon_label_01, R.style.Theme_App_Starting_Pippo01),
            AppIcon("Pippo02", "PippoIcon02", AppIconFamily.Anime, R.drawable.pippo_icon_02, R.color.pippo_icon_bg_02, R.string.pippo_icon_label_02, R.style.Theme_App_Starting_Pippo02),
        )
        val Default: AppIcon = entries.first()
        val presets: List<AppIcon> = entries
    }
}

object AppIcons {
    // These disabled aliases keep the launcher reachable across upgrades from an older selection.
    private val retiredAliases = setOf(
        "PippoMaster01SpiderRed",
        "PippoMaster02SageDoodle",
        "PippoMaster03VintageSepia",
        "PippoMaster04BurgundyGothic",
        "PippoMaster05SakuraNight",
        "PippoMaster06RainyBlue",
        "PippoMaster07Scrapbook",
        "PippoMaster08NoirMoon",
        "PippoMaster09EmeraldFloral",
        "PippoMaster10PastelSunset",
        "PippoMaster11OriginalSakuraCat",
        "PippoMaster12CrimsonWebGlass",
        "PippoMaster13SageGlass",
        "PippoMaster14AntiqueGoldGlass",
        "PippoMaster15NeonRainGlass",
        "PippoMaster16EmeraldCelestialGlass",
        "PippoMaster17SunsetGlass",
        "PippoMasterAnimeDuo",
        "PippoMasterAnimeGirl",
        "PippoMasterMonochromeCat",
    ) + (3..20).map { "PippoIcon${it.toString().padStart(2, '0')}" }

    fun current(context: Context): AppIcon =
        prefs(context).getString(Key, null)?.let { name -> AppIcon.entries.firstOrNull { it.name == name } }
            ?: AppIcon.Default

    fun launchedFrom(intent: Intent?): AppIcon? {
        val started = intent?.component?.className ?: return null
        return AppIcon.entries.firstOrNull { started.endsWith(".${it.alias}") }
    }

    fun migrateRetiredSelection(context: Context, intent: Intent?) {
        val saved = prefs(context).getString(Key, null)
        val startedFromRetired = intent?.component?.className?.substringAfterLast('.')?.let { it in retiredAliases } == true
        if (!startedFromRetired && (saved == null || AppIcon.entries.any { it.name == saved })) return
        runCatching { apply(context, current(context)) }
            .onFailure { Timber.w(it, "Couldn't migrate the launcher icon selection") }
    }

    // Android 13+ can persist the selected theme for the next system-created splash.
    fun syncSplash(context: Context) {
        var owner = context
        while (owner is ContextWrapper && owner !is Activity) {
            val base = owner.baseContext
            if (base === owner) return
            owner = base
        }
        val activity = owner as? Activity ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { activity.splashScreen.setSplashScreenTheme(current(activity).splashTheme) }
        }
    }

    @Composable
    fun rememberCurrent(): AppIcon {
        val context = LocalContext.current.applicationContext
        val preferences = remember(context) { prefs(context) }
        var icon by remember(preferences) { mutableStateOf(current(context)) }
        DisposableEffect(preferences) {
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == Key || key == null) icon = current(context)
            }
            preferences.registerOnSharedPreferenceChangeListener(listener)
            // Close the gap between composition and listener registration.
            icon = current(context)
            onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }
        return icon
    }

    @Synchronized
    fun apply(context: Context, icon: AppIcon) {
        require(icon in AppIcon.entries)
        val pm = context.packageManager
        val changed = mutableListOf<Pair<ComponentName, Int>>()

        fun set(alias: String, enabled: Boolean) {
            val component = componentOf(context, alias)
            val previousState = pm.getComponentEnabledSetting(component)
            val currentlyEnabled = when (previousState) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> alias == AppIcon.Default.alias
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                else -> false
            }
            if (enabled == currentlyEnabled) return
            val state = if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            changed += component to previousState
        }

        try {
            // Enable the new alias before retiring any active one, including on Android 7–12.
            set(icon.alias, true)
            AppIcon.entries.forEach { entry -> if (entry != icon) set(entry.alias, false) }
            retiredAliases.forEach { set(it, false) }
            prefs(context).edit { putString(Key, icon.name) }
        } catch (error: Exception) {
            changed.asReversed().forEach { (component, state) ->
                runCatching { pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP) }
            }
            throw error
        }
    }

    private fun componentOf(context: Context, alias: String) =
        ComponentName(context, "com.mochyy.mochymusic.$alias")
    private fun prefs(context: Context) = context.getSharedPreferences("app_icon", Context.MODE_PRIVATE)
    private const val Key = "icon"
}
