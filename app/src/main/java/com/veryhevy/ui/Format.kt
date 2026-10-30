package com.veryhevy.ui

import com.veryhevy.data.WeightUnit
import com.veryhevy.domain.Calculators
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    private val dateTimeFormat = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun weightKgToDisplay(kg: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.LB) Calculators.kgToLb(kg) else kg

    fun displayToKg(value: Double, unit: WeightUnit): Double =
        if (unit == WeightUnit.LB) Calculators.lbToKg(value) else value

    fun weight(kg: Double, unit: WeightUnit): String =
        Calculators.formatWeight(weightKgToDisplay(kg, unit))

    /** Aggregate volumes are shown as whole numbers in the display unit. */
    fun volume(kg: Double, unit: WeightUnit): String =
        kotlin.math.round(weightKgToDisplay(kg, unit)).toLong().toString()

    fun unitLabel(unit: WeightUnit): String = if (unit == WeightUnit.LB) "lb" else "kg"

    fun dateTime(millis: Long): String = dateTimeFormat.format(Date(millis))
    fun date(millis: Long): String = dateFormat.format(Date(millis))
    fun time(millis: Long): String = timeFormat.format(Date(millis))

    fun duration(startedAt: Long, endedAt: Long?): String {
        val end = endedAt ?: System.currentTimeMillis()
        val totalSeconds = ((end - startedAt) / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "%d:%02d:%02d".format(hours, minutes % 60, seconds)
            else -> "%d:%02d".format(minutes, seconds)
        }
    }

    fun clock(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%d:%02d".format(m, s)
    }
}
