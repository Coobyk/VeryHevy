package com.veryhevy.ui

import com.veryhevy.data.WeightUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.floor
import kotlin.math.round

object Format {
    private val dateTimeFormat = SimpleDateFormat("EEE, MMM d · HH:mm", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun weight(kg: Double, unit: WeightUnit): String =
        formatWeight(unit.kgToDisplay(kg))

    /** Aggregate volumes are shown as whole numbers in the display unit. */
    fun volume(kg: Double, unit: WeightUnit): String =
        round(unit.kgToDisplay(kg)).toLong().toString()

    fun unitLabel(unit: WeightUnit): String = unit.label

    fun dateTime(millis: Long): String = dateTimeFormat.format(Date(millis))
    fun date(millis: Long): String = dateFormat.format(Date(millis))
    fun epochDate(epochDay: Long): String = date(epochDay * 86_400_000L)

    fun duration(startedAt: Long, endedAt: Long): String =
        formatSeconds(((endedAt - startedAt) / 1000).coerceAtLeast(0))

    fun durationSince(startedAt: Long): String =
        duration(startedAt, System.currentTimeMillis())

    fun clock(totalSeconds: Int): String = formatSeconds(totalSeconds.toLong())

    fun rpe(rpe: Double): String = "%.1f".format(rpe)

    fun equipment(raw: String): String = raw.replace('_', ' ')

    fun supersetLabel(supersetId: Int): String = "Superset ${'A' + supersetId - 1}"

    fun decimalFilter(input: String): String = input.filter { it.isDigit() || it == '.' }

    private fun formatSeconds(totalSeconds: Long): String {
        val s = totalSeconds.coerceAtLeast(0)
        val minutes = s / 60
        val seconds = s % 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "%d:%02d:%02d".format(hours, minutes % 60, seconds)
            else -> "%d:%02d".format(minutes, seconds)
        }
    }

    /** Format a weight trimming trailing zeros ("82.5", not "82.50"). */
    private fun formatWeight(weight: Double): String {
        val rounded = round(weight * 100) / 100
        if (rounded == floor(rounded)) return rounded.toLong().toString()
        return rounded.toString()
    }
}
