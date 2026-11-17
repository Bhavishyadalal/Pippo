package com.mochyy.mochymusic.utils

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import com.mochyy.mochymusic.R

enum class AppIconFamily(@StringRes val title: Int) {
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
) {
    companion object {
        val entries: List<AppIcon> = listOf(
            AppIcon("Default", "IconDefault", AppIconFamily.Anime, R.drawable.pippo_icon_01, R.color.pippo_icon_bg_01, R.string.pippo_icon_label_01),
            AppIcon("Pippo02", "PippoIcon02", AppIconFamily.Anime, R.drawable.pippo_icon_02, R.color.pippo_icon_bg_02, R.string.pippo_icon_label_02),
            AppIcon("Pippo03", "PippoIcon03", AppIconFamily.Anime, R.drawable.pippo_icon_03, R.color.pippo_icon_bg_03, R.string.pippo_icon_label_03),
            AppIcon("Pippo04", "PippoIcon04", AppIconFamily.Anime, R.drawable.pippo_icon_04, R.color.pippo_icon_bg_04, R.string.pippo_icon_label_04),
            AppIcon("Pippo05", "PippoIcon05", AppIconFamily.Anime, R.drawable.pippo_icon_05, R.color.pippo_icon_bg_05, R.string.pippo_icon_label_05),
            AppIcon("Pippo06", "PippoIcon06", AppIconFamily.Anime, R.drawable.pippo_icon_06, R.color.pippo_icon_bg_06, R.string.pippo_icon_label_06),
            AppIcon("Pippo07", "PippoIcon07", AppIconFamily.Anime, R.drawable.pippo_icon_07, R.color.pippo_icon_bg_07, R.string.pippo_icon_label_07),
            AppIcon("Pippo08", "PippoIcon08", AppIconFamily.Anime, R.drawable.pippo_icon_08, R.color.pippo_icon_bg_08, R.string.pippo_icon_label_08),
            AppIcon("Pippo09", "PippoIcon09", AppIconFamily.Anime, R.drawable.pippo_icon_09, R.color.pippo_icon_bg_09, R.string.pippo_icon_label_09),
            AppIcon("Pippo10", "PippoIcon10", AppIconFamily.Anime, R.drawable.pippo_icon_10, R.color.pippo_icon_bg_10, R.string.pippo_icon_label_10),
            AppIcon("Pippo11", "PippoIcon11", AppIconFamily.Named, R.drawable.pippo_icon_11, R.color.pippo_icon_bg_11, R.string.pippo_icon_label_11),
            AppIcon("Pippo12", "PippoIcon12", AppIconFamily.Named, R.drawable.pippo_icon_12, R.color.pippo_icon_bg_12, R.string.pippo_icon_label_12),
            AppIcon("Pippo13", "PippoIcon13", AppIconFamily.Named, R.drawable.pippo_icon_13, R.color.pippo_icon_bg_13, R.string.pippo_icon_label_13),
            AppIcon("Pippo14", "PippoIcon14", AppIconFamily.Named, R.drawable.pippo_icon_14, R.color.pippo_icon_bg_14, R.string.pippo_icon_label_14),
            AppIcon("Pippo15", "PippoIcon15", AppIconFamily.Named, R.drawable.pippo_icon_15, R.color.pippo_icon_bg_15, R.string.pippo_icon_label_15),
            AppIcon("Pippo16", "PippoIcon16", AppIconFamily.Named, R.drawable.pippo_icon_16, R.color.pippo_icon_bg_16, R.string.pippo_icon_label_16),
            AppIcon("Pippo17", "PippoIcon17", AppIconFamily.Named, R.drawable.pippo_icon_17, R.color.pippo_icon_bg_17, R.string.pippo_icon_label_17),
            AppIcon("Pippo18", "PippoIcon18", AppIconFamily.Named, R.drawable.pippo_icon_18, R.color.pippo_icon_bg_18, R.string.pippo_icon_label_18),
            AppIcon("Pippo19", "PippoIcon19", AppIconFamily.Named, R.drawable.pippo_icon_19, R.color.pippo_icon_bg_19, R.string.pippo_icon_label_19),
            AppIcon("Pippo20", "PippoIcon20", AppIconFamily.Named, R.drawable.pippo_icon_20, R.color.pippo_icon_bg_20, R.string.pippo_icon_label_20),
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

    // Keep future system splashes aligned with the main-icon Compose handoff.
    fun syncSplash(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runCatching { activity.splashScreen.setSplashScreenTheme(R.style.Theme_App_Starting) }
        }
    }

    @Synchronized
    fun apply(context: Context, icon: AppIcon) {
        require(icon in AppIcon.entries)
        val previous = current(context)
        val pm = context.packageManager
        fun set(entry: AppIcon, enabled: Boolean) = pm.setComponentEnabledSetting(
            componentOf(context, entry),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        try {
            // Enable the new alias before retiring the old one, including on Android 7–12.
            set(icon, true)
            AppIcon.entries.filter { it != icon }.forEach { set(it, false) }
            prefs(context).edit { putString(Key, icon.name) }
        } catch (error: Exception) {
            runCatching { set(previous, true) }
            if (previous != icon) runCatching { set(icon, false) }
            throw error
        }
    }

    private fun componentOf(context: Context, icon: AppIcon) =
        ComponentName(context, "com.mochyy.mochymusic.${icon.alias}")
    private fun prefs(context: Context) = context.getSharedPreferences("app_icon", Context.MODE_PRIVATE)
    private const val Key = "icon"
}
