package com.veryhevy.data

import com.veryhevy.data.db.RoutineSetEntity
import com.veryhevy.data.db.SetEntity

/** The editable fields of a set, shared by workout and routine set entities. */
internal data class SetSpec(
    val weightKg: Double = 0.0,
    val reps: Int = 0,
    val rpe: Double? = null,
    val isWarmup: Boolean = false,
)

internal fun SetEntity.toSpec() = SetSpec(weightKg, reps, rpe, isWarmup)
internal fun RoutineSetEntity.toSpec() = SetSpec(weightKg, reps, rpe, isWarmup)
internal fun SetEntity?.specOrEmpty() = this?.toSpec() ?: SetSpec()
internal fun RoutineSetEntity?.specOrEmpty() = this?.toSpec() ?: SetSpec()

/**
 * Index bookkeeping for ordered exercise/set lists. Workout and routine
 * entities carry their own `orderIndex`/`setIndex` but compact the same way;
 * these return (old index, new index) pairs the caller applies to its entities.
 */
internal object PlanOps {

    /** Items above [removedIndex] shift down by one. */
    fun afterRemoval(indices: List<Int>, removedIndex: Int): List<Pair<Int, Int>> =
        indices.filter { it > removedIndex }.sorted().map { it to it - 1 }

    /** Indices that differ once re-compacted to 0..n-1. */
    fun compacted(indices: List<Int>): List<Pair<Int, Int>> =
        indices.sorted().mapIndexedNotNull { i, idx -> if (idx != i) idx to i else null }
}
