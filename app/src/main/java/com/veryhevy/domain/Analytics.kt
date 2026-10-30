package com.veryhevy.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class SetRecord(
    val exerciseId: String,
    val muscleGroup: String,
    val weightKg: Double,
    val reps: Int,
    val isWarmup: Boolean,
    val startedAt: Long,
    val workoutId: Long,
)

data class WeeklyVolume(val weekStart: LocalDate, val volumeKg: Double, val workoutCount: Int)

data class MuscleVolume(val muscleGroup: String, val volumeKg: Double)

data class PersonalRecord(
    val exerciseId: String,
    val bestOneRepMaxKg: Double,
    val bestWeightKg: Double,
    val bestReps: Int,
    val achievedAt: Long,
)

data class WorkoutTotals(
    val workoutId: Long,
    val startedAt: Long,
    val volumeKg: Double,
    val setCount: Int,
)

object Analytics {

    /** e1RM across all sets per exercise, keeping the date it was achieved. */
    fun personalRecords(sets: List<SetRecord>): List<PersonalRecord> =
        sets.filter { !it.isWarmup && it.reps > 0 }
            .groupBy { it.exerciseId }
            .map { (exerciseId, list) ->
                val best = list.maxBy { Calculators.oneRepMax(it.weightKg, it.reps) }
                PersonalRecord(
                    exerciseId = exerciseId,
                    bestOneRepMaxKg = Calculators.oneRepMax(best.weightKg, best.reps),
                    bestWeightKg = list.maxOf { it.weightKg },
                    bestReps = best.reps,
                    achievedAt = best.startedAt,
                )
            }
            .sortedByDescending { it.bestOneRepMaxKg }

    fun weeklyVolume(sets: List<SetRecord>, weeks: Int = 8, zone: ZoneId = ZoneId.systemDefault()): List<WeeklyVolume> {
        val today = LocalDate.now(zone)
        val currentWeekStart = today.minusDays(((today.dayOfWeek.value % 7).toLong()))
        return (weeks - 1 downTo 0).map { back ->
            val weekStart = currentWeekStart.minusWeeks(back.toLong())
            val weekEndExclusive = weekStart.plusWeeks(1)
            val weekSets = sets.filter {
                val d = Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate()
                d >= weekStart && d < weekEndExclusive
            }
            WeeklyVolume(
                weekStart = weekStart,
                volumeKg = weekSets.filter { !it.isWarmup }.sumOf { Calculators.setVolume(it.weightKg, it.reps) },
                workoutCount = weekSets.map { it.workoutId }.distinct().size,
            )
        }
    }

    fun volumeByMuscle(sets: List<SetRecord>): List<MuscleVolume> =
        sets.filter { !it.isWarmup }
            .groupBy { it.muscleGroup }
            .map { (muscle, list) -> MuscleVolume(muscle, list.sumOf { Calculators.setVolume(it.weightKg, it.reps) }) }
            .sortedByDescending { it.volumeKg }

    fun totals(sets: List<SetRecord>): List<WorkoutTotals> =
        sets.groupBy { it.workoutId }.map { (id, list) ->
            WorkoutTotals(
                workoutId = id,
                startedAt = list.first().startedAt,
                volumeKg = list.filter { !it.isWarmup }.sumOf { Calculators.setVolume(it.weightKg, it.reps) },
                setCount = list.size,
            )
        }

    /** Number of consecutive 7-day windows (ending today or yesterday) containing at least one workout. */
    fun weeklyStreak(sets: List<SetRecord>, zone: ZoneId = ZoneId.systemDefault()): Int {
        val weeks = weeklyVolume(sets, weeks = 52, zone = zone).sortedByDescending { it.weekStart }
        var streak = 0
        var allowCurrentEmpty = true
        for (week in weeks) {
            if (week.workoutCount > 0) {
                streak++
            } else if (allowCurrentEmpty) {
                allowCurrentEmpty = false
            } else {
                break
            }
        }
        return streak
    }
}
