package com.veryhevy.domain

import kotlin.math.floor
import kotlin.math.roundToInt

object Calculators {

    /**
     * Estimated one-rep max using the Epley formula.
     * A set at 1 rep is its own 1RM; zero/negative reps yield 0.
     */
    fun oneRepMax(weight: Double, reps: Int): Double {
        if (reps <= 0 || weight <= 0.0) return 0.0
        if (reps == 1) return weight
        return weight * (1.0 + reps / 30.0)
    }

    /** Training volume: weight x reps, summed per set by the caller. */
    fun setVolume(weight: Double, reps: Int): Double {
        if (weight <= 0.0 || reps <= 0) return 0.0
        return weight * reps
    }

    /**
     * Pick the plates per side for [targetWeight] given a [barWeight] and an
     * ordered (heaviest first) list of available plate weights.
     */
    fun platesFor(
        targetWeight: Double,
        barWeight: Double,
        availablePlates: List<Double>,
    ): List<Double> {
        if (targetWeight <= barWeight) return emptyList()
        var remaining = targetWeight - barWeight
        val plates = mutableListOf<Double>()
        for (plate in availablePlates.sortedDescending()) {
            while (remaining >= plate * 2 - 1e-9) {
                plates.add(plate)
                remaining -= plate * 2
            }
        }
        return plates
    }

    /** Largest weight (per the same plate set) that can actually be loaded, at or below [target]. */
    fun closestLoadable(
        targetWeight: Double,
        barWeight: Double,
        availablePlates: List<Double>,
    ): Double {
        val plates = platesFor(targetWeight, barWeight, availablePlates)
        return barWeight + plates.sumOf { it * 2 }
    }

    /** Format a weight in the given unit, trimming trailing zeros ("82.5", not "82.50"). */
    fun formatWeight(weight: Double, decimals: Int = 2): String {
        val scaled = Math.pow(10.0, decimals.toDouble())
        val rounded = kotlin.math.round(weight * scaled) / scaled
        if (rounded == floor(rounded)) return rounded.toLong().toString()
        return rounded.toString()
    }

    /** Convert kg to lb. */
    fun kgToLb(kg: Double): Double = kg * 2.20462262185

    /** Convert lb to kg. */
    fun lbToKg(lb: Double): Double = lb / 2.20462262185

    /** Round a weight to the nearest increment (e.g. 2.5 kg plates). */
    fun roundToIncrement(weight: Double, increment: Double): Double {
        if (increment <= 0.0) return weight
        return (kotlin.math.round(weight / increment) * increment * 100).roundToInt() / 100.0
    }
}
