@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.veryhevy.data

import com.veryhevy.data.db.AppDatabase
import com.veryhevy.data.db.SetEntity
import com.veryhevy.data.db.WorkoutEntity
import com.veryhevy.data.db.WorkoutExerciseEntity
import com.veryhevy.data.db.WorkoutWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

class WorkoutRepository(private val db: AppDatabase) {

    private val dao get() = db.workoutDao()

    fun observeInProgress(): Flow<WorkoutEntity?> = dao.observeInProgress()

    fun observeInProgressWithDetails(): Flow<WorkoutWithExercises?> =
        dao.observeInProgress().flatMapLatest { w ->
            if (w == null) flowOf(null) else dao.observeWithExercises(w.id)
        }
    fun observeFinished(): Flow<List<WorkoutWithExercises>> = dao.observeFinished()
    fun observeById(id: Long): Flow<WorkoutWithExercises?> = dao.observeWithExercises(id)
    suspend fun byId(id: Long): WorkoutWithExercises? = dao.withExercises(id)
    suspend fun inProgress(): WorkoutEntity? = dao.inProgress()
    suspend fun setHistoryForExercise(exerciseId: String) = dao.setHistoryForExercise(exerciseId)

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

    suspend fun updateNotes(id: Long, notes: String) {
        val w = dao.withExercises(id) ?: return
        dao.update(w.workout.copy(notes = notes))
    }

    suspend fun addExercise(workoutId: Long, exerciseId: String): Long {
        val w = dao.withExercises(workoutId) ?: return -1
        val order = w.exercises.size
        val weId = dao.insertExercise(
            WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, orderIndex = order)
        )
        // A fresh exercise starts with one empty working set, matching what a lifter expects.
        dao.insertSets(listOf(SetEntity(workoutExerciseId = weId, setIndex = 0)))
        return weId
    }

    suspend fun removeExercise(workoutId: Long, orderIndex: Int) {
        val w = dao.withExercises(workoutId) ?: return
        val toRemove = w.exercises.firstOrNull { it.exercise.orderIndex == orderIndex } ?: return
        dao.deleteExercise(toRemove.exercise)
        // Re-index the remaining exercises.
        w.exercises.filter { it.exercise.orderIndex > orderIndex }
            .sortedBy { it.exercise.orderIndex }
            .forEach { dao.updateExercise(it.exercise.copy(orderIndex = it.exercise.orderIndex - 1)) }
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
        val w = allWorkoutExercise(weId) ?: return
        dao.updateExercise(w.copy(notes = notes))
    }

    suspend fun updateRestSeconds(weId: Long, restSeconds: Int?) {
        val w = allWorkoutExercise(weId) ?: return
        dao.updateExercise(w.copy(restSeconds = restSeconds))
    }

    suspend fun setSuperset(workoutId: Long, orderIndex: Int, supersetId: Int?) {
        val w = dao.withExercises(workoutId) ?: return
        val e = w.exercises.firstOrNull { it.exercise.orderIndex == orderIndex } ?: return
        dao.updateExercise(e.exercise.copy(supersetId = supersetId))
    }

    suspend fun addSet(weId: Long, template: SetEntity? = null): Long {
        val existing = dao.setsFor(weId)
        val next = SetEntity(
            workoutExerciseId = weId,
            setIndex = existing.size,
            weightKg = template?.weightKg ?: 0.0,
            reps = template?.reps ?: 0,
            rpe = template?.rpe,
            isWarmup = template?.isWarmup ?: false,
        )
        dao.insertSets(listOf(next))
        return existing.size.toLong()
    }

    suspend fun updateSet(set: SetEntity) = dao.upsertSet(set)

    suspend fun deleteSet(set: SetEntity) {
        dao.deleteSet(set)
        val remaining = dao.setsFor(set.workoutExerciseId).sortedBy { it.setIndex }
        remaining.forEachIndexed { i, s ->
            if (s.setIndex != i) dao.upsertSet(s.copy(setIndex = i))
        }
    }

    private suspend fun allWorkoutExercise(weId: Long): WorkoutExerciseEntity? =
        dao.exerciseById(weId)
}
