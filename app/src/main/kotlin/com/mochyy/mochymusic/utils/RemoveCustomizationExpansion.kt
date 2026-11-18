package com.mochyy.mochymusic.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit

private val CustomizationExpansionRemoved = booleanPreferencesKey("pippo.migrations.customizationExpansionRemoved.v1")

/** Remove only the withdrawn expansion's private settings; original settings and user data survive. */
suspend fun Context.removeCustomizationExpansionPreferences() {
    dataStore.edit { preferences ->
        if (preferences[CustomizationExpansionRemoved] == true) return@edit
        val removedKeys = preferences.asMap().keys.filter { it.name.startsWith("pippo.ui.") }
        removedKeys.forEach { preferences.remove(it) }
        preferences[CustomizationExpansionRemoved] = true
    }
}
