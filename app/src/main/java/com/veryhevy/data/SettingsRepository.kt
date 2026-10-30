package com.veryhevy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

enum class WeightUnit { KG, LB }

data class AppSettings(
    val unit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = 90,
)

class SettingsRepository(private val context: Context) {

    private val keyUnit = stringPreferencesKey("weight_unit")
    private val keyDefaultRest = intPreferencesKey("default_rest_seconds")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            unit = prefs[keyUnit]?.let { WeightUnit.valueOf(it) } ?: WeightUnit.KG,
            defaultRestSeconds = prefs[keyDefaultRest] ?: 90,
        )
    }

    suspend fun setUnit(unit: WeightUnit) {
        context.dataStore.edit { it[keyUnit] = unit.name }
    }

    suspend fun setDefaultRestSeconds(seconds: Int) {
        context.dataStore.edit { it[keyDefaultRest] = seconds }
    }
}
