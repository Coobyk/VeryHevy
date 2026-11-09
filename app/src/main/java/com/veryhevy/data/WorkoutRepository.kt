@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.veryhevy.data

import com.veryhevy.data.db.AppDatabase
import com.veryhevy.data.db.SetEntity
import com.veryhevy.data.db.SetWithWorkout
import com.veryhevy.data.db.WorkoutEntity
import com.veryhevy.data.db.WorkoutExerciseEntity
import com.veryhevy.data.db.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class WorkoutRepository(private val db: AppDatabase) {

    private val dao get() = db.workoutDao()

    fun observeInProgressWithDetails(): Flow<WorkoutWithExercises?> =
        dao.observeInProgress().flatMapLatest { w ->
            if (w == null) flowOf(null) else dao.observeWithExercises(w.id)
        }

    fun observeFinished(): Flow<List<WorkoutWithExercises>> = dao.observeFinished()
    suspend fun byId(id: Long): WorkoutWithExercises? = dao.withExercises(id)
    suspend fun setHistoryForExercise(exerciseId: String): List<SetWithWorkout> =
        dao.setHistoryForExercise(exerciseId)

    suspend fun startWorkout(name: String): Long {
        val existing = dao.inProgress()
        if (existing != null) return existing.id
        return dao.insert(WorkoutEntity(name = name, startedAt = System.currentTimeMillis()))
    }

    suspend fun finishWorkout(id: Long) {
        val w = dao.withExercises(id) ?: return
        dao.update(w.workout.copy(endedAt = System.currentTimeMillis()))
    }

    suspend fun deleteWorkout(id: Long) {
        val w = dao.withExercises(id) ?: return
        dao.delete(w.workout)
    }

    suspend fun updateName(id: Long, name: String) {
        val w = dao.withExercises(id) ?: return
        dao.update(w.workout.copy(name = name))
    }

    suspend fun addExercise(workoutId: Long, exerciseId: String): Long {
        val w = dao.withExercises(workoutId) ?: return -1
        val weId = dao.insertExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, orderIndex = w.exercises.size)
        )
        // A fresh exercise starts with one empty working set, matching what a lifter expects.
        dao.insertSets(listOf(SetEntity(workoutExerciseId = weId, setIndex = 0)))
        return weId
    }

    suspend fun removeExercise(workoutId: Long, orderIndex: Int) {
        val w = dao.withExercises(workoutId) ?: return
        val toRemove = w.exercises.firstOrNull { it.exercise.orderIndex == orderIndex } ?: return
        dao.deleteExercise(toRemove.exercise)
        val shifts = PlanOps.afterRemoval(
            w.exercises.map { it.exercise.orderIndex }, orderIndex
        ).toMap()
        w.exercises.forEach { e ->
            shifts[e.exercise.orderIndex]?.let { dao.updateExercise(e.exercise.copy(orderIndex = it)) }
        }
    }

    suspend fun moveExercise(workoutId: Long, orderIndex: Int, direction: Int) {
        val w = dao.withExercises(workoutId) ?: return
        val target = orderIndex + direction
        val a = w.exercises.firstOrNull { it.exercise.orderIndex == orderIndex } ?: return
        val b = w.exercises.firstOrNull { it.exercise.orderIndex == target } ?: return
        dao.updateExercise(a.exercise.copy(orderIndex = target))
        dao.updateExercise(b.exercise.copy(orderIndex = orderIndex))
    }

    suspend fun updateExerciseNotes(weId: Long, notes: String) {
        val e = dao.exerciseById(weId) ?: return
        dao.updateExercise(e.copy(notes = notes))
    }

    suspend fun updateRestSeconds(weId: Long, restSeconds: Int?) {
        val e = dao.exerciseById(weId) ?: return
        dao.updateExercise(e.copy(restSeconds = restSeconds))
    }

    suspend fun setSuperset(workoutId: Long, orderIndex: Int, supersetId: Int?) {
        val w = dao.withExercises(workoutId) ?: return
        val e = w.exercises.firstOrNull { it.exercise.orderIndex == orderIndex } ?: return
        dao.updateExercise(e.exercise.copy(supersetId = supersetId))
    }

    suspend fun addSet(weId: Long, template: SetEntity? = null) {
        val spec = template.specOrEmpty()
        dao.insertSets(
            listOf(
                SetEntity(
                    workoutExerciseId = weId,
                    setIndex = dao.setsFor(weId).size,
                    weightKg = spec.weightKg,
                    reps = spec.reps,
                    rpe = spec.rpe,
                    isWarmup = spec.isWarmup,
                )
            )
        )
    }

    suspend fun updateSet(set: SetEntity) = dao.upsertSet(set)

    suspend fun setDone(id: Long, done: Boolean, completedAt: Long?) =
        dao.setDone(id, done, completedAt)

    suspend fun deleteSet(set: SetEntity) {
        dao.deleteSet(set)
        val remaining = dao.setsFor(set.workoutExerciseId)
        val shifts = PlanOps.compacted(remaining.map { it.setIndex }).toMap()
        remaining.forEach { s ->
            shifts[s.setIndex]?.let { dao.upsertSet(s.copy(setIndex = it)) }
        }
    }
}
