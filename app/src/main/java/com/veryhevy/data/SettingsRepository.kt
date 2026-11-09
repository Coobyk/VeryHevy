package com.veryhevy.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.veryhevy.domain.Calculators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Storage unit is always kg; the enum owns display conversion so callers never branch on it. */
enum class WeightUnit(val label: String) {
    KG("kg") {
        override fun displayToKg(value: Double) = value
        override fun kgToDisplay(kg: Double) = kg
    },
    LB("lb") {
        override fun displayToKg(value: Double) = Calculators.lbToKg(value)
        override fun kgToDisplay(kg: Double) = Calculators.kgToLb(kg)
    };

    abstract fun displayToKg(value: Double): Double
    abstract fun kgToDisplay(kg: Double): Double

    companion object {
        fun fromStorage(name: String?): WeightUnit =
            entries.firstOrNull { it.name == name } ?: KG
    }
}

object RestSeconds {
    const val DEFAULT = 90
    const val MAX = 240
    const val STEP = 30
    const val TIMER_ADD = 15

    fun nextAfter(current: Int): Int = if (current >= MAX) DEFAULT else current + STEP
}

data class AppSettings(
    val unit: WeightUnit = WeightUnit.KG,
    val defaultRestSeconds: Int = RestSeconds.DEFAULT,
)

class SettingsRepository(private val context: Context) {

    private val keyUnit = stringPreferencesKey("weight_unit")
    private val keyDefaultRest = intPreferencesKey("default_rest_seconds")

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            unit = WeightUnit.fromStorage(prefs[keyUnit]),
            defaultRestSeconds = prefs[keyDefaultRest] ?: RestSeconds.DEFAULT,
        )
    }

    suspend fun setUnit(unit: WeightUnit) {
        context.dataStore.edit { it[keyUnit] = unit.name }
    }

    suspend fun setDefaultRestSeconds(seconds: Int) {
        context.dataStore.edit { it[keyDefaultRest] = seconds }
    }
}
