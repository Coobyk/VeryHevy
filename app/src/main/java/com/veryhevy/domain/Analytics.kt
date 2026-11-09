package com.veryhevy.domain

import androidx.compose.runtime.Immutable

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Immutable
data class SetRecord(
    val exerciseId: String,
    val muscleGroup: String,
    val weightKg: Double,
    val reps: Int,
    val isWarmup: Boolean,
    val startedAt: Long,
    val workoutId: Long,
)

@Immutable
data class WeeklyVolume(val weekStart: LocalDate, val volumeKg: Double, val workoutCount: Int)

@Immutable
data class MuscleVolume(val muscleGroup: String, val volumeKg: Double)

@Immutable
data class PersonalRecord(
    val exerciseId: String,
    val bestOneRepMaxKg: Double,
    val bestWeightKg: Double,
    val bestReps: Int,
    val achievedAt: Long,
)

object Analytics {

    /** e1RM across all sets per exercise; every record field comes from the same best set. */
    fun personalRecords(sets: List<SetRecord>): List<PersonalRecord> =
        sets.filter { !it.isWarmup && it.reps > 0 }
            .groupBy { it.exerciseId }
            .map { (exerciseId, list) ->
                val best = list.maxBy { Calculators.oneRepMax(it.weightKg, it.reps) }
                PersonalRecord(
                    exerciseId = exerciseId,
                    bestOneRepMaxKg = Calculators.oneRepMax(best.weightKg, best.reps),
                    bestWeightKg = best.weightKg,
                    bestReps = best.reps,
                    achievedAt = best.startedAt,
                )
            }
            .sortedByDescending { it.bestOneRepMaxKg }

    private fun weekStartOf(date: LocalDate): LocalDate =
        date.minusDays((date.dayOfWeek.value % 7).toLong())

    fun weeklyVolume(sets: List<SetRecord>, weeks: Int = 8, zone: ZoneId = ZoneId.systemDefault()): List<WeeklyVolume> {
        val currentWeekStart = weekStartOf(LocalDate.now(zone))
        return (weeks - 1 downTo 0).map { back ->
            val weekStart = currentWeekStart.minusWeeks(back.toLong())
            val weekEndExclusive = weekStart.plusWeeks(1)
            val weekSets = sets.filter {
                val d = Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
                d >= weekStart && d < weekEndExclusive
            }.filter { !it.isWarmup }
            WeeklyVolume(
                weekStart = weekStart,
                volumeKg = weekSets.sumOf { Calculators.setVolume(it.weightKg, it.reps) },
                workoutCount = weekSets.map { it.workoutId }.distinct().size,
            )
        }
    }

    fun volumeByMuscle(sets: List<SetRecord>): List<MuscleVolume> =
        sets.filter { !it.isWarmup }
            .groupBy { it.muscleGroup }
            .map { (muscle, list) -> MuscleVolume(muscle, list.sumOf { Calculators.setVolume(it.weightKg, it.reps) }) }
            .sortedByDescending { it.volumeKg }

    /** Consecutive weeks with at least one workout, counting back from this week
     *  (or last week if the current one hasn't seen a session yet). */
    fun weeklyStreak(sets: List<SetRecord>, zone: ZoneId = ZoneId.systemDefault()): Int {
        val weeksWithWorkouts = sets
            .map { weekStartOf(Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()) }
            .toSet()
        var week = weekStartOf(LocalDate.now(zone))
        if (week !in weeksWithWorkouts) week = week.minusWeeks(1)
        var streak = 0
        while (week in weeksWithWorkouts) {
            streak++
            week = week.minusWeeks(1)
        }
        return streak
    }
}
