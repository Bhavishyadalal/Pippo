package com.mochyy.mochymusic.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val UiAccentKey = stringPreferencesKey("pippo.ui.accent")
val UiCustomAccentKey = intPreferencesKey("pippo.ui.customAccent")
val UiContrastKey = booleanPreferencesKey("pippo.ui.highContrast")
val UiTextScaleKey = floatPreferencesKey("pippo.ui.textScale")
val UiFontKey = stringPreferencesKey("pippo.ui.font")
val UiCornersKey = stringPreferencesKey("pippo.ui.corners")
val UiSpacingKey = stringPreferencesKey("pippo.ui.spacing")
val UiNavStyleKey = stringPreferencesKey("pippo.ui.navStyle")
val UiNavLabelsKey = stringPreferencesKey("pippo.ui.navLabels")
val UiNavIndicatorKey = stringPreferencesKey("pippo.ui.navIndicator")
val UiNavOrderKey = stringPreferencesKey("pippo.ui.navOrder")
val UiNavIconSizeKey = stringPreferencesKey("pippo.ui.navIconSize")
val UiNavSwipeKey = booleanPreferencesKey("pippo.ui.navSwipe")
val UiTransitionKey = stringPreferencesKey("pippo.ui.transition")
val UiTransitionDurationKey = intPreferencesKey("pippo.ui.transitionDuration")
val UiPressEffectsKey = booleanPreferencesKey("pippo.ui.pressEffects")
val UiHapticsKey = booleanPreferencesKey("pippo.ui.haptics")
val UiLoadingMotionKey = booleanPreferencesKey("pippo.ui.loadingMotion")
val UiCharacterKey = stringPreferencesKey("pippo.ui.character")
val UiSavedProfilesKey = stringPreferencesKey("pippo.ui.savedProfiles")

enum class UiAccent { CORAL, SAGE, LAVENDER, ICE, GOLD, MONOCHROME, CUSTOM }
enum class UiFont { INTER, SYSTEM, SERIF, MONOSPACE }
enum class UiCorners { SOFT, SQUARE, ROUND }
enum class UiSpacing { COMPACT, COMFORTABLE, ROOMY }
enum class UiNavStyle { CURRENT, FLAT, OUTLINED, FULL_WIDTH }
enum class UiNavLabels { OFF, SELECTED, ALL }
enum class UiNavIndicator { PILL, DOT, NONE }
enum class UiNavIconSize { SMALL, NORMAL, LARGE }
enum class UiTransition { DEFAULT, FADE, SCALE, NONE }
enum class UiCharacter { RANDOM, FEMALE, MALE, HIDDEN }

val UiHomeOrderKey = stringPreferencesKey("pippo.ui.homeOrder")
val UiHomeHiddenKey = stringPreferencesKey("pippo.ui.homeHidden")
val UiLibraryRowsKey = stringPreferencesKey("pippo.ui.libraryRows")
