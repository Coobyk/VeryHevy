package com.veryhevy.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.veryhevy.data.db.AppDatabase
import com.veryhevy.data.db.ExerciseEntity
import com.veryhevy.data.db.MeasurementEntity
import com.veryhevy.data.db.RoutineEntity
import com.veryhevy.data.db.RoutineExerciseEntity
import com.veryhevy.data.db.RoutineSetEntity
import com.veryhevy.data.db.SetEntity
import com.veryhevy.data.db.WorkoutEntity
import com.veryhevy.data.db.WorkoutExerciseEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manual JSON backups of everything the user created: workouts, routines,
 * measurements and custom exercises. Weights are always serialized in kg.
 */
class BackupManager(private val context: Context, private val db: AppDatabase) {

    private data class ExerciseJson(
        val exerciseId: String,
        val orderIndex: Int,
        val notes: String,
        val supersetId: Int?,
        val restSeconds: Int?,
        val sets: List<SetJson>,
    )

    private data class SetJson(
        val setIndex: Int,
        val weightKg: Double,
        val reps: Int,
        val rpe: Double?,
        val isWarmup: Boolean,
        val isDone: Boolean,
    )

    private fun setToJson(s: SetJson) = JSONObject()
        .put("setIndex", s.setIndex)
        .put("weightKg", s.weightKg)
        .put("reps", s.reps)
        .put("rpe", s.rpe ?: JSONObject.NULL)
        .put("isWarmup", s.isWarmup)
        .put("isDone", s.isDone)

    private fun exerciseToJson(e: ExerciseJson) = JSONObject()
        .put("exerciseId", e.exerciseId)
        .put("orderIndex", e.orderIndex)
        .put("notes", e.notes)
        .put("supersetId", e.supersetId ?: JSONObject.NULL)
        .put("restSeconds", e.restSeconds ?: JSONObject.NULL)
        .put("sets", JSONArray().apply { e.sets.forEach { put(setToJson(it)) } })

    private fun JSONObject.toSetJson() = SetJson(
        setIndex = getInt("setIndex"),
        weightKg = getDouble("weightKg"),
        reps = getInt("reps"),
        rpe = if (isNull("rpe")) null else getDouble("rpe"),
        isWarmup = optBoolean("isWarmup", false),
        isDone = optBoolean("isDone", true),
    )

    private fun JSONObject.toExerciseJson() = ExerciseJson(
        exerciseId = getString("exerciseId"),
        orderIndex = getInt("orderIndex"),
        notes = optString("notes", ""),
        supersetId = if (isNull("supersetId")) null else getInt("supersetId"),
        restSeconds = if (isNull("restSeconds")) null else getInt("restSeconds"),
        sets = getJSONArray("sets").toList { it.toSetJson() },
    )

    private inline fun <T> JSONArray.toList(map: (JSONObject) -> T): List<T> =
        (0 until length()).map { map(getJSONObject(it)) }

    suspend fun exportTo(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            val allWorkouts = db.workoutDao().observeFinished().first()
            val workouts = JSONArray()
            for (w in allWorkouts) {
                workouts.put(
                    JSONObject()
                        .put("id", w.workout.id)
                        .put("name", w.workout.name)
                        .put("startedAt", w.workout.startedAt)
                        .put("endedAt", w.workout.endedAt ?: JSONObject.NULL)
                        .put("notes", w.workout.notes)
                        .put(
                            "exercises",
                            JSONArray().apply {
                                w.exercises.forEach { we ->
                                    put(
                                        exerciseToJson(
                                            ExerciseJson(
                                                exerciseId = we.exercise.exerciseId,
                                                orderIndex = we.exercise.orderIndex,
                                                notes = we.exercise.notes,
                                                supersetId = we.exercise.supersetId,
                                                restSeconds = we.exercise.restSeconds,
                                                sets = we.sets.sortedBy { it.setIndex }.map {
                                                    SetJson(it.setIndex, it.weightKg, it.reps, it.rpe, it.isWarmup, it.isDone)
                                                },
                                            )
                                        )
                                    )
                                }
                            }
                        )
                )
            }
            root.put("workouts", workouts)

            val routines = JSONArray()
            for (r in db.routineDao().observeAll().first()) {
                routines.put(
                    JSONObject()
                        .put("id", r.routine.id)
                        .put("name", r.routine.name)
                        .put("notes", r.routine.notes)
                        .put("createdAt", r.routine.createdAt)
                        .put("updatedAt", r.routine.updatedAt)
                        .put(
                            "exercises",
                            JSONArray().apply {
                                r.exercises.forEach { re ->
                                    put(
                                        exerciseToJson(
                                            ExerciseJson(
                                                exerciseId = re.exercise.exerciseId,
                                                orderIndex = re.exercise.orderIndex,
                                                notes = re.exercise.notes,
                                                supersetId = re.exercise.supersetId,
                                                restSeconds = re.exercise.restSeconds,
                                                sets = re.sets.sortedBy { it.setIndex }.map {
                                                    SetJson(it.setIndex, it.weightKg, it.reps, it.rpe, it.isWarmup, true)
                                                },
                                            )
                                        )
                                    )
                                }
                            }
                        )
                )
            }
            root.put("routines", routines)

            val measurements = JSONArray()
            for (m in db.measurementDao().all()) {
                measurements.put(
                    JSONObject()
                        .put("dateEpochDay", m.dateEpochDay)
                        .put("weightKg", m.weightKg)
                        .put("notes", m.notes)
                )
            }
            root.put("measurements", measurements)

            val custom = JSONArray()
            for (e in db.exerciseDao().all().filter { it.isCustom }) {
                custom.put(
                    JSONObject()
                        .put("id", e.id)
                        .put("name", e.name)
                        .put("muscleGroup", e.muscleGroup)
                        .put("secondaryMuscles", e.secondaryMuscles)
                        .put("equipment", e.equipment)
                        .put("instructions", e.instructions)
                )
            }
            root.put("customExercises", custom)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            } ?: error("Could not open output stream")
            allWorkouts.size
        }
    }

    /** Imports a backup, replacing existing data (including any in-progress workout). */
    suspend fun importFrom(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.readUriText(uri) ?: error("Could not open input stream")
            val root = JSONObject(text)

            db.withTransaction {
                db.workoutDao().deleteAll()
                db.routineDao().deleteAll()
                db.measurementDao().deleteAll()

                val workouts = root.getJSONArray("workouts")
                for (i in 0 until workouts.length()) {
                    val wObj = workouts.getJSONObject(i)
                    val workoutId = db.workoutDao().insert(
                        WorkoutEntity(
                            name = wObj.getString("name"),
                            startedAt = wObj.getLong("startedAt"),
                            endedAt = wObj.optLong("endedAt", -1L).takeIf { it > 0 },
                            notes = wObj.optString("notes", ""),
                        )
                    )
                    wObj.getJSONArray("exercises").toList { it.toExerciseJson() }.forEach { e ->
                        val weId = db.workoutDao().insertExercise(
                            WorkoutExerciseEntity(
                                workoutId = workoutId,
                                exerciseId = e.exerciseId,
                                orderIndex = e.orderIndex,
                                notes = e.notes,
                                supersetId = e.supersetId,
                                restSeconds = e.restSeconds,
                            )
                        )
                        db.workoutDao().insertSets(
                            e.sets.map {
                                SetEntity(
                                    workoutExerciseId = weId,
                                    setIndex = it.setIndex,
                                    weightKg = it.weightKg,
                                    reps = it.reps,
                                    rpe = it.rpe,
                                    isWarmup = it.isWarmup,
                                    isDone = it.isDone,
                                )
                            }
                        )
                    }
                }

                val routines = root.getJSONArray("routines")
                for (i in 0 until routines.length()) {
                    val rObj = routines.getJSONObject(i)
                    val routineId = db.routineDao().insert(
                        RoutineEntity(
                            name = rObj.getString("name"),
                            notes = rObj.optString("notes", ""),
                            createdAt = rObj.getLong("createdAt"),
                            updatedAt = rObj.getLong("updatedAt"),
                        )
                    )
                    rObj.getJSONArray("exercises").toList { it.toExerciseJson() }.forEach { e ->
                        val reId = db.routineDao().insertExercise(
                            RoutineExerciseEntity(
                                routineId = routineId,
                                exerciseId = e.exerciseId,
                                orderIndex = e.orderIndex,
                                notes = e.notes,
                                supersetId = e.supersetId,
                                restSeconds = e.restSeconds,
                            )
                        )
                        db.routineDao().insertSets(
                            e.sets.map {
                                RoutineSetEntity(
                                    routineExerciseId = reId,
                                    setIndex = it.setIndex,
                                    weightKg = it.weightKg,
                                    reps = it.reps,
                                    rpe = it.rpe,
                                    isWarmup = it.isWarmup,
                                )
                            }
                        )
                    }
                }

                val measurements = root.getJSONArray("measurements")
                for (i in 0 until measurements.length()) {
                    val m = measurements.getJSONObject(i)
                    db.measurementDao().insert(
                        MeasurementEntity(
                            dateEpochDay = m.getLong("dateEpochDay"),
                            weightKg = m.getDouble("weightKg"),
                            notes = m.optString("notes", ""),
                        )
                    )
                }

                val custom = root.optJSONArray("customExercises") ?: JSONArray()
                for (i in 0 until custom.length()) {
                    val e = custom.getJSONObject(i)
                    db.exerciseDao().upsert(
                        ExerciseEntity(
                            id = e.getString("id"),
                            name = e.getString("name"),
                            muscleGroup = e.getString("muscleGroup"),
                            secondaryMuscles = e.optString("secondaryMuscles", ""),
                            equipment = e.optString("equipment", "other"),
                            instructions = e.optString("instructions", ""),
                            isCustom = true,
                        )
                    )
                }

                workouts.length()
            }
        }
    }
}
