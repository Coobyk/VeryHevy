package com.veryhevy.data

import com.veryhevy.data.db.AppDatabase
import com.veryhevy.data.db.RoutineEntity
import com.veryhevy.data.db.RoutineExerciseEntity
import com.veryhevy.data.db.RoutineSetEntity
import com.veryhevy.data.db.RoutineWithExercises
import com.veryhevy.data.db.SetEntity
import com.veryhevy.data.db.WorkoutEntity
import com.veryhevy.data.db.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

class RoutineRepository(private val db: AppDatabase) {

    private val dao get() = db.routineDao()
    private val workoutDao get() = db.workoutDao()

    fun observeAll(): Flow<List<RoutineWithExercises>> = dao.observeAll()
    fun observeById(id: Long): Flow<RoutineWithExercises?> = dao.observeById(id)
    suspend fun byId(id: Long): RoutineWithExercises? = dao.byId(id)

    suspend fun create(name: String): Long =
        dao.insert(RoutineEntity(name = name, createdAt = now(), updatedAt = now()))

    suspend fun rename(id: Long, name: String) {
        val r = dao.byId(id) ?: return
        dao.update(r.routine.copy(name = name, updatedAt = now()))
    }

    suspend fun delete(id: Long) {
        val r = dao.byId(id) ?: return
        dao.delete(r.routine)
    }

    suspend fun duplicate(id: Long): Long {
        val r = dao.byId(id) ?: return -1
        val newId = dao.insert(r.routine.copy(id = 0, name = "${r.routine.name} (copy)", createdAt = now(), updatedAt = now()))
        r.exercises.sortedBy { it.exercise.orderIndex }.forEach { e ->
            val neId = dao.insertExercise(e.exercise.copy(id = 0, routineId = newId))
            dao.insertSets(e.sets.map { it.copy(id = 0, routineExerciseId = neId) })
        }
        return newId
    }

    suspend fun addExercise(routineId: Long, exerciseId: String): Long {
        val r = dao.byId(routineId) ?: return -1
        val reId = dao.insertExercise(
            RoutineExerciseEntity(routineId = routineId, exerciseId = exerciseId, orderIndex = r.exercises.size)
        )
        dao.insertSets(listOf(RoutineSetEntity(routineExerciseId = reId, setIndex = 0)))
        touch(routineId)
        return reId
    }

    suspend fun removeExercise(routineId: Long, orderIndex: Int) {
        val r = dao.byId(routineId) ?: return
        val target = r.exercises.firstOrNull { it.exercise.orderIndex == orderIndex } ?: return
        dao.deleteExercise(target.exercise)
        val shifts = PlanOps.afterRemoval(
            r.exercises.map { it.exercise.orderIndex }, orderIndex
        ).toMap()
        r.exercises.forEach { e ->
            shifts[e.exercise.orderIndex]?.let { dao.updateExercise(e.exercise.copy(orderIndex = it)) }
        }
        touch(routineId)
    }

    suspend fun moveExercise(routineId: Long, orderIndex: Int, direction: Int) {
        val r = dao.byId(routineId) ?: return
        val targetIndex = orderIndex + direction
        val a = r.exercises.firstOrNull { it.exercise.orderIndex == orderIndex } ?: return
        val b = r.exercises.firstOrNull { it.exercise.orderIndex == targetIndex } ?: return
        dao.updateExercise(a.exercise.copy(orderIndex = targetIndex))
        dao.updateExercise(b.exercise.copy(orderIndex = orderIndex))
        touch(routineId)
    }

    suspend fun setRestSeconds(reId: Long, seconds: Int?) {
        val e = dao.exerciseById(reId) ?: return
        dao.updateExercise(e.copy(restSeconds = seconds))
        touch(e.routineId)
    }

    suspend fun addSet(reId: Long, template: RoutineSetEntity?) {
        val spec = template.specOrEmpty()
        dao.insertSets(
            listOf(
                RoutineSetEntity(
                    routineExerciseId = reId,
                    setIndex = dao.setsFor(reId).size,
                    weightKg = spec.weightKg,
                    reps = spec.reps,
                    rpe = spec.rpe,
                    isWarmup = spec.isWarmup,
                )
            )
        )
        touchForExercise(reId)
    }

    suspend fun deleteSet(set: RoutineSetEntity) {
        dao.deleteSets(listOf(set))
        val remaining = dao.setsFor(set.routineExerciseId)
        val shifts = PlanOps.compacted(remaining.map { it.setIndex }).toMap()
        val updated = remaining.mapNotNull { s ->
            shifts[s.setIndex]?.let { s.copy(setIndex = it) }
        }
        if (updated.isNotEmpty()) dao.insertSets(updated)
        touchForExercise(set.routineExerciseId)
    }

    suspend fun updateSetValues(set: RoutineSetEntity, weightKg: Double, reps: Int) {
        dao.insertSets(listOf(set.copy(weightKg = weightKg, reps = reps)))
        touchForExercise(set.routineExerciseId)
    }

    suspend fun setSetRpe(set: RoutineSetEntity, rpe: Double?) {
        dao.insertSets(listOf(set.copy(rpe = rpe)))
        touchForExercise(set.routineExerciseId)
    }

    suspend fun toggleSetWarmup(set: RoutineSetEntity) {
        dao.insertSets(listOf(set.copy(isWarmup = !set.isWarmup)))
        touchForExercise(set.routineExerciseId)
    }

    /** Copies the routine into a fresh in-progress workout and returns its id. */
    suspend fun startWorkoutFromRoutine(routineId: Long): Long? {
        val r = dao.byId(routineId) ?: return null
        val workoutId = workoutDao.insert(
            WorkoutEntity(name = r.routine.name, startedAt = now())
        )
        r.exercises.sortedBy { it.exercise.orderIndex }.forEach { e ->
            val weId = workoutDao.insertExercise(
                WorkoutExerciseEntity(
                    workoutId = workoutId,
                    exerciseId = e.exercise.exerciseId,
                    orderIndex = e.exercise.orderIndex,
                    notes = e.exercise.notes,
                    supersetId = e.exercise.supersetId,
                    restSeconds = e.exercise.restSeconds,
                )
            )
            workoutDao.insertSets(
                e.sets.sortedBy { it.setIndex }.map {
                    SetEntity(
                        workoutExerciseId = weId,
                        setIndex = it.setIndex,
                        weightKg = it.weightKg,
                        reps = it.reps,
                        rpe = it.rpe,
                        isWarmup = it.isWarmup,
                    )
                }
            )
        }
        return workoutId
    }

    private suspend fun touch(routineId: Long) {
        val r = dao.byId(id = routineId) ?: return
        dao.update(r.routine.copy(updatedAt = now()))
    }

    private suspend fun touchForExercise(reId: Long) {
        dao.exerciseById(reId)?.let { touch(it.routineId) }
    }

    private fun now(): Long = System.currentTimeMillis()
}
