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

enum class AppIconFamily(@StringRes val title: Int) {
    Brand(R.string.pippo_master_group_brand),
    Lettering(R.string.pippo_master_group_lettering),
    Illustrations(R.string.pippo_master_group_illustrations),
    Glass(R.string.pippo_master_group_glass),
    Anime(R.string.pippo_icons_portraits),
    Named(R.string.pippo_icons_named),
}

class AppIcon private constructor(
    val name: String,
    val alias: String,
    val family: AppIconFamily,
    @DrawableRes val foreground: Int,
    @ColorRes val background: Int?,
    @StringRes val label: Int,
    @StyleRes val splashTheme: Int,
) {
    companion object {
        val entries: List<AppIcon> = listOf(
            AppIcon("Default", "IconDefault", AppIconFamily.Brand, R.drawable.pippo_master_black_ribbon_p, R.color.pippo_master_black_ribbon_p_bg, R.string.pippo_master_black_ribbon_p_label, R.style.Theme_App_Starting),
            AppIcon("Master_01_cobalt_wax", "PippoMaster01CobaltWax", AppIconFamily.Lettering, R.drawable.pippo_master_01_cobalt_wax, R.color.pippo_master_01_cobalt_wax_bg, R.string.pippo_master_01_cobalt_wax_label, R.style.Theme_App_Starting_Master01CobaltWax),
            AppIcon("Master_02_graphite_script", "PippoMaster02GraphiteScript", AppIconFamily.Lettering, R.drawable.pippo_master_02_graphite_script, R.color.pippo_master_02_graphite_script_bg, R.string.pippo_master_02_graphite_script_label, R.style.Theme_App_Starting_Master02GraphiteScript),
            AppIcon("Master_03_pink_pastel", "PippoMaster03PinkPastel", AppIconFamily.Lettering, R.drawable.pippo_master_03_pink_pastel, R.color.pippo_master_03_pink_pastel_bg, R.string.pippo_master_03_pink_pastel_label, R.style.Theme_App_Starting_Master03PinkPastel),
            AppIcon("Master_04_black_ink_brush", "PippoMaster04BlackInkBrush", AppIconFamily.Lettering, R.drawable.pippo_master_04_black_ink_brush, R.color.pippo_master_04_black_ink_brush_bg, R.string.pippo_master_04_black_ink_brush_label, R.style.Theme_App_Starting_Master04BlackInkBrush),
            AppIcon("Master_05_mint_pencil", "PippoMaster05MintPencil", AppIconFamily.Lettering, R.drawable.pippo_master_05_mint_pencil, R.color.pippo_master_05_mint_pencil_bg, R.string.pippo_master_05_mint_pencil_label, R.style.Theme_App_Starting_Master05MintPencil),
            AppIcon("Master_06_yellow_retro_bubble", "PippoMaster06YellowRetroBubble", AppIconFamily.Lettering, R.drawable.pippo_master_06_yellow_retro_bubble, R.color.pippo_master_06_yellow_retro_bubble_bg, R.string.pippo_master_06_yellow_retro_bubble_label, R.style.Theme_App_Starting_Master06YellowRetroBubble),
            AppIcon("Master_07_teal_peach_marker", "PippoMaster07TealPeachMarker", AppIconFamily.Lettering, R.drawable.pippo_master_07_teal_peach_marker, R.color.pippo_master_07_teal_peach_marker_bg, R.string.pippo_master_07_teal_peach_marker_label, R.style.Theme_App_Starting_Master07TealPeachMarker),
            AppIcon("Master_08_lavender_paper_cut", "PippoMaster08LavenderPaperCut", AppIconFamily.Lettering, R.drawable.pippo_master_08_lavender_paper_cut, R.color.pippo_master_08_lavender_paper_cut_bg, R.string.pippo_master_08_lavender_paper_cut_label, R.style.Theme_App_Starting_Master08LavenderPaperCut),
            AppIcon("Master_09_peach_dry_brush", "PippoMaster09PeachDryBrush", AppIconFamily.Lettering, R.drawable.pippo_master_09_peach_dry_brush, R.color.pippo_master_09_peach_dry_brush_bg, R.string.pippo_master_09_peach_dry_brush_label, R.style.Theme_App_Starting_Master09PeachDryBrush),
            AppIcon("Master_10_sage_woodcut", "PippoMaster10SageWoodcut", AppIconFamily.Lettering, R.drawable.pippo_master_10_sage_woodcut, R.color.pippo_master_10_sage_woodcut_bg, R.string.pippo_master_10_sage_woodcut_label, R.style.Theme_App_Starting_Master10SageWoodcut),
            AppIcon("Master_11_approved_blue_scrapbook", "PippoMaster11ApprovedBlueScrapbook", AppIconFamily.Lettering, R.drawable.pippo_master_11_approved_blue_scrapbook, R.color.pippo_master_11_approved_blue_scrapbook_bg, R.string.pippo_master_11_approved_blue_scrapbook_label, R.style.Theme_App_Starting_Master11ApprovedBlueScrapbook),
            AppIcon("Master_blue_crayon_original", "PippoMasterBlueCrayonOriginal", AppIconFamily.Lettering, R.drawable.pippo_master_blue_crayon_original, R.color.pippo_master_blue_crayon_original_bg, R.string.pippo_master_blue_crayon_original_label, R.style.Theme_App_Starting_MasterBlueCrayonOriginal),
            AppIcon("Master_01_spider_red", "PippoMaster01SpiderRed", AppIconFamily.Illustrations, R.drawable.pippo_master_01_spider_red, R.color.pippo_master_01_spider_red_bg, R.string.pippo_master_01_spider_red_label, R.style.Theme_App_Starting_Master01SpiderRed),
            AppIcon("Master_02_sage_doodle", "PippoMaster02SageDoodle", AppIconFamily.Illustrations, R.drawable.pippo_master_02_sage_doodle, R.color.pippo_master_02_sage_doodle_bg, R.string.pippo_master_02_sage_doodle_label, R.style.Theme_App_Starting_Master02SageDoodle),
            AppIcon("Master_03_vintage_sepia", "PippoMaster03VintageSepia", AppIconFamily.Illustrations, R.drawable.pippo_master_03_vintage_sepia, R.color.pippo_master_03_vintage_sepia_bg, R.string.pippo_master_03_vintage_sepia_label, R.style.Theme_App_Starting_Master03VintageSepia),
            AppIcon("Master_04_burgundy_gothic", "PippoMaster04BurgundyGothic", AppIconFamily.Illustrations, R.drawable.pippo_master_04_burgundy_gothic, R.color.pippo_master_04_burgundy_gothic_bg, R.string.pippo_master_04_burgundy_gothic_label, R.style.Theme_App_Starting_Master04BurgundyGothic),
            AppIcon("Master_05_sakura_night", "PippoMaster05SakuraNight", AppIconFamily.Illustrations, R.drawable.pippo_master_05_sakura_night, R.color.pippo_master_05_sakura_night_bg, R.string.pippo_master_05_sakura_night_label, R.style.Theme_App_Starting_Master05SakuraNight),
            AppIcon("Master_06_rainy_blue", "PippoMaster06RainyBlue", AppIconFamily.Illustrations, R.drawable.pippo_master_06_rainy_blue, R.color.pippo_master_06_rainy_blue_bg, R.string.pippo_master_06_rainy_blue_label, R.style.Theme_App_Starting_Master06RainyBlue),
            AppIcon("Master_07_scrapbook", "PippoMaster07Scrapbook", AppIconFamily.Illustrations, R.drawable.pippo_master_07_scrapbook, R.color.pippo_master_07_scrapbook_bg, R.string.pippo_master_07_scrapbook_label, R.style.Theme_App_Starting_Master07Scrapbook),
            AppIcon("Master_08_noir_moon", "PippoMaster08NoirMoon", AppIconFamily.Illustrations, R.drawable.pippo_master_08_noir_moon, R.color.pippo_master_08_noir_moon_bg, R.string.pippo_master_08_noir_moon_label, R.style.Theme_App_Starting_Master08NoirMoon),
            AppIcon("Master_09_emerald_floral", "PippoMaster09EmeraldFloral", AppIconFamily.Illustrations, R.drawable.pippo_master_09_emerald_floral, R.color.pippo_master_09_emerald_floral_bg, R.string.pippo_master_09_emerald_floral_label, R.style.Theme_App_Starting_Master09EmeraldFloral),
            AppIcon("Master_10_pastel_sunset", "PippoMaster10PastelSunset", AppIconFamily.Illustrations, R.drawable.pippo_master_10_pastel_sunset, R.color.pippo_master_10_pastel_sunset_bg, R.string.pippo_master_10_pastel_sunset_label, R.style.Theme_App_Starting_Master10PastelSunset),
            AppIcon("Master_anime_duo", "PippoMasterAnimeDuo", AppIconFamily.Illustrations, R.drawable.pippo_master_anime_duo, R.color.pippo_master_anime_duo_bg, R.string.pippo_master_anime_duo_label, R.style.Theme_App_Starting_MasterAnimeDuo),
            AppIcon("Master_anime_girl", "PippoMasterAnimeGirl", AppIconFamily.Illustrations, R.drawable.pippo_master_anime_girl, R.color.pippo_master_anime_girl_bg, R.string.pippo_master_anime_girl_label, R.style.Theme_App_Starting_MasterAnimeGirl),
            AppIcon("Master_11_original_sakura_cat", "PippoMaster11OriginalSakuraCat", AppIconFamily.Glass, R.drawable.pippo_master_11_original_sakura_cat, R.color.pippo_master_11_original_sakura_cat_bg, R.string.pippo_master_11_original_sakura_cat_label, R.style.Theme_App_Starting_Master11OriginalSakuraCat),
            AppIcon("Master_12_crimson_web_glass", "PippoMaster12CrimsonWebGlass", AppIconFamily.Glass, R.drawable.pippo_master_12_crimson_web_glass, R.color.pippo_master_12_crimson_web_glass_bg, R.string.pippo_master_12_crimson_web_glass_label, R.style.Theme_App_Starting_Master12CrimsonWebGlass),
            AppIcon("Master_13_sage_glass", "PippoMaster13SageGlass", AppIconFamily.Glass, R.drawable.pippo_master_13_sage_glass, R.color.pippo_master_13_sage_glass_bg, R.string.pippo_master_13_sage_glass_label, R.style.Theme_App_Starting_Master13SageGlass),
            AppIcon("Master_14_antique_gold_glass", "PippoMaster14AntiqueGoldGlass", AppIconFamily.Glass, R.drawable.pippo_master_14_antique_gold_glass, R.color.pippo_master_14_antique_gold_glass_bg, R.string.pippo_master_14_antique_gold_glass_label, R.style.Theme_App_Starting_Master14AntiqueGoldGlass),
            AppIcon("Master_15_neon_rain_glass", "PippoMaster15NeonRainGlass", AppIconFamily.Glass, R.drawable.pippo_master_15_neon_rain_glass, R.color.pippo_master_15_neon_rain_glass_bg, R.string.pippo_master_15_neon_rain_glass_label, R.style.Theme_App_Starting_Master15NeonRainGlass),
            AppIcon("Master_16_emerald_celestial_glass", "PippoMaster16EmeraldCelestialGlass", AppIconFamily.Glass, R.drawable.pippo_master_16_emerald_celestial_glass, R.color.pippo_master_16_emerald_celestial_glass_bg, R.string.pippo_master_16_emerald_celestial_glass_label, R.style.Theme_App_Starting_Master16EmeraldCelestialGlass),
            AppIcon("Master_17_sunset_glass", "PippoMaster17SunsetGlass", AppIconFamily.Glass, R.drawable.pippo_master_17_sunset_glass, R.color.pippo_master_17_sunset_glass_bg, R.string.pippo_master_17_sunset_glass_label, R.style.Theme_App_Starting_Master17SunsetGlass),
            AppIcon("Master_monochrome_cat", "PippoMasterMonochromeCat", AppIconFamily.Glass, R.drawable.pippo_master_monochrome_cat, R.color.pippo_master_monochrome_cat_bg, R.string.pippo_master_monochrome_cat_label, R.style.Theme_App_Starting_MasterMonochromeCat),
            AppIcon("Pippo01", "PippoIcon01", AppIconFamily.Anime, R.drawable.pippo_icon_01, R.color.pippo_icon_bg_01, R.string.pippo_icon_label_01, R.style.Theme_App_Starting_Pippo01),
            AppIcon("Pippo02", "PippoIcon02", AppIconFamily.Anime, R.drawable.pippo_icon_02, R.color.pippo_icon_bg_02, R.string.pippo_icon_label_02, R.style.Theme_App_Starting_Pippo02),
            AppIcon("Pippo03", "PippoIcon03", AppIconFamily.Anime, R.drawable.pippo_icon_03, R.color.pippo_icon_bg_03, R.string.pippo_icon_label_03, R.style.Theme_App_Starting_Pippo03),
            AppIcon("Pippo04", "PippoIcon04", AppIconFamily.Anime, R.drawable.pippo_icon_04, R.color.pippo_icon_bg_04, R.string.pippo_icon_label_04, R.style.Theme_App_Starting_Pippo04),
            AppIcon("Pippo05", "PippoIcon05", AppIconFamily.Anime, R.drawable.pippo_icon_05, R.color.pippo_icon_bg_05, R.string.pippo_icon_label_05, R.style.Theme_App_Starting_Pippo05),
            AppIcon("Pippo06", "PippoIcon06", AppIconFamily.Anime, R.drawable.pippo_icon_06, R.color.pippo_icon_bg_06, R.string.pippo_icon_label_06, R.style.Theme_App_Starting_Pippo06),
            AppIcon("Pippo07", "PippoIcon07", AppIconFamily.Anime, R.drawable.pippo_icon_07, R.color.pippo_icon_bg_07, R.string.pippo_icon_label_07, R.style.Theme_App_Starting_Pippo07),
            AppIcon("Pippo08", "PippoIcon08", AppIconFamily.Anime, R.drawable.pippo_icon_08, R.color.pippo_icon_bg_08, R.string.pippo_icon_label_08, R.style.Theme_App_Starting_Pippo08),
            AppIcon("Pippo09", "PippoIcon09", AppIconFamily.Anime, R.drawable.pippo_icon_09, R.color.pippo_icon_bg_09, R.string.pippo_icon_label_09, R.style.Theme_App_Starting_Pippo09),
            AppIcon("Pippo10", "PippoIcon10", AppIconFamily.Anime, R.drawable.pippo_icon_10, R.color.pippo_icon_bg_10, R.string.pippo_icon_label_10, R.style.Theme_App_Starting_Pippo10),
            AppIcon("Pippo11", "PippoIcon11", AppIconFamily.Named, R.drawable.pippo_icon_11, R.color.pippo_icon_bg_11, R.string.pippo_icon_label_11, R.style.Theme_App_Starting_Pippo11),
            AppIcon("Pippo12", "PippoIcon12", AppIconFamily.Named, R.drawable.pippo_icon_12, R.color.pippo_icon_bg_12, R.string.pippo_icon_label_12, R.style.Theme_App_Starting_Pippo12),
            AppIcon("Pippo13", "PippoIcon13", AppIconFamily.Named, R.drawable.pippo_icon_13, R.color.pippo_icon_bg_13, R.string.pippo_icon_label_13, R.style.Theme_App_Starting_Pippo13),
            AppIcon("Pippo14", "PippoIcon14", AppIconFamily.Named, R.drawable.pippo_icon_14, R.color.pippo_icon_bg_14, R.string.pippo_icon_label_14, R.style.Theme_App_Starting_Pippo14),
            AppIcon("Pippo15", "PippoIcon15", AppIconFamily.Named, R.drawable.pippo_icon_15, R.color.pippo_icon_bg_15, R.string.pippo_icon_label_15, R.style.Theme_App_Starting_Pippo15),
            AppIcon("Pippo16", "PippoIcon16", AppIconFamily.Named, R.drawable.pippo_icon_16, R.color.pippo_icon_bg_16, R.string.pippo_icon_label_16, R.style.Theme_App_Starting_Pippo16),
            AppIcon("Pippo17", "PippoIcon17", AppIconFamily.Named, R.drawable.pippo_icon_17, R.color.pippo_icon_bg_17, R.string.pippo_icon_label_17, R.style.Theme_App_Starting_Pippo17),
            AppIcon("Pippo18", "PippoIcon18", AppIconFamily.Named, R.drawable.pippo_icon_18, R.color.pippo_icon_bg_18, R.string.pippo_icon_label_18, R.style.Theme_App_Starting_Pippo18),
            AppIcon("Pippo19", "PippoIcon19", AppIconFamily.Named, R.drawable.pippo_icon_19, R.color.pippo_icon_bg_19, R.string.pippo_icon_label_19, R.style.Theme_App_Starting_Pippo19),
            AppIcon("Pippo20", "PippoIcon20", AppIconFamily.Named, R.drawable.pippo_icon_20, R.color.pippo_icon_bg_20, R.string.pippo_icon_label_20, R.style.Theme_App_Starting_Pippo20),
        )
        val Default: AppIcon = entries.first()
        val presets: List<AppIcon> = entries
    }
}

object AppIcons {
    fun current(context: Context): AppIcon =
        prefs(context).getString(Key, null)?.let { name -> AppIcon.entries.firstOrNull { it.name == name } }
            ?: AppIcon.Default

    fun launchedFrom(intent: Intent?): AppIcon? {
        val started = intent?.component?.className ?: return null
        return AppIcon.entries.firstOrNull { started.endsWith(".${it.alias}") }
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

        fun set(entry: AppIcon, enabled: Boolean) {
            val component = componentOf(context, entry)
            val previousState = pm.getComponentEnabledSetting(component)
            val currentlyEnabled = when (previousState) {
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> entry == AppIcon.Default
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
            set(icon, true)
            AppIcon.entries.forEach { entry -> if (entry != icon) set(entry, false) }
            prefs(context).edit { putString(Key, icon.name) }
        } catch (error: Exception) {
            changed.asReversed().forEach { (component, state) ->
                runCatching { pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP) }
            }
            throw error
        }
    }

    private fun componentOf(context: Context, icon: AppIcon) =
        ComponentName(context, "com.mochyy.mochymusic.${icon.alias}")
    private fun prefs(context: Context) = context.getSharedPreferences("app_icon", Context.MODE_PRIVATE)
    private const val Key = "icon"
}
