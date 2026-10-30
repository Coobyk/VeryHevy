package com.veryhevy.data

import android.content.Context
import android.net.Uri
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
class BackupManager(private val context: Context, private val db: com.veryhevy.data.db.AppDatabase) {

    suspend fun exportTo(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject()
            root.put("version", 1)
            root.put("exportedAt", System.currentTimeMillis())

            val workouts = JSONArray()
            val workoutDao = db.workoutDao()
            val allWorkouts = workoutDao.observeFinished().first()
            for (w in allWorkouts) {
                val wObj = JSONObject()
                wObj.put("id", w.workout.id)
                wObj.put("name", w.workout.name)
                wObj.put("startedAt", w.workout.startedAt)
                wObj.put("endedAt", w.workout.endedAt ?: JSONObject.NULL)
                wObj.put("notes", w.workout.notes)
                val exercises = JSONArray()
                for (we in w.exercises) {
                    val weObj = JSONObject()
                    weObj.put("exerciseId", we.exercise.exerciseId)
                    weObj.put("orderIndex", we.exercise.orderIndex)
                    weObj.put("notes", we.exercise.notes)
                    weObj.put("supersetId", we.exercise.supersetId ?: JSONObject.NULL)
                    weObj.put("restSeconds", we.exercise.restSeconds ?: JSONObject.NULL)
                    val sets = JSONArray()
                    for (s in we.sets.sortedBy { it.setIndex }) {
                        sets.put(
                            JSONObject()
                                .put("setIndex", s.setIndex)
                                .put("weightKg", s.weightKg)
                                .put("reps", s.reps)
                                .put("rpe", s.rpe ?: JSONObject.NULL)
                                .put("isWarmup", s.isWarmup)
                                .put("isDone", s.isDone)
                        )
                    }
                    weObj.put("sets", sets)
                    exercises.put(weObj)
                }
                wObj.put("exercises", exercises)
                workouts.put(wObj)
            }
            root.put("workouts", workouts)

            val routines = JSONArray()
            val allRoutines = db.routineDao().observeAll().first()
            for (r in allRoutines) {
                val rObj = JSONObject()
                rObj.put("id", r.routine.id)
                rObj.put("name", r.routine.name)
                rObj.put("notes", r.routine.notes)
                rObj.put("createdAt", r.routine.createdAt)
                rObj.put("updatedAt", r.routine.updatedAt)
                val exercises = JSONArray()
                for (re in r.exercises) {
                    val reObj = JSONObject()
                    reObj.put("exerciseId", re.exercise.exerciseId)
                    reObj.put("orderIndex", re.exercise.orderIndex)
                    reObj.put("notes", re.exercise.notes)
                    reObj.put("supersetId", re.exercise.supersetId ?: JSONObject.NULL)
                    reObj.put("restSeconds", re.exercise.restSeconds ?: JSONObject.NULL)
                    val sets = JSONArray()
                    for (s in re.sets.sortedBy { it.setIndex }) {
                        sets.put(
                            JSONObject()
                                .put("setIndex", s.setIndex)
                                .put("weightKg", s.weightKg)
                                .put("reps", s.reps)
                                .put("rpe", s.rpe ?: JSONObject.NULL)
                                .put("isWarmup", s.isWarmup)
                        )
                    }
                    reObj.put("sets", sets)
                    exercises.put(reObj)
                }
                rObj.put("exercises", exercises)
                routines.put(rObj)
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

    /** Imports a backup, replacing existing data. Returns the number of workouts imported. */
    suspend fun importFrom(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: error("Could not open input stream")
            val root = JSONObject(text)

            db.workoutDao().observeFinished().first().forEach { db.workoutDao().delete(it.workout) }
            db.routineDao().observeAll().first().forEach { db.routineDao().delete(it.routine) }
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
                val exercises = wObj.getJSONArray("exercises")
                for (j in 0 until exercises.length()) {
                    val weObj = exercises.getJSONObject(j)
                    val weId = db.workoutDao().insertExercise(
                        WorkoutExerciseEntity(
                            workoutId = workoutId,
                            exerciseId = weObj.getString("exerciseId"),
                            orderIndex = weObj.getInt("orderIndex"),
                            notes = weObj.optString("notes", ""),
                            supersetId = if (weObj.isNull("supersetId")) null else weObj.getInt("supersetId"),
                            restSeconds = if (weObj.isNull("restSeconds")) null else weObj.getInt("restSeconds"),
                        )
                    )
                    val sets = weObj.getJSONArray("sets")
                    db.workoutDao().insertSets((0 until sets.length()).map { k ->
                        val s = sets.getJSONObject(k)
                        SetEntity(
                            workoutExerciseId = weId,
                            setIndex = s.getInt("setIndex"),
                            weightKg = s.getDouble("weightKg"),
                            reps = s.getInt("reps"),
                            rpe = if (s.isNull("rpe")) null else s.getDouble("rpe"),
                            isWarmup = s.optBoolean("isWarmup", false),
                            isDone = s.optBoolean("isDone", true),
                        )
                    })
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
                val exercises = rObj.getJSONArray("exercises")
                for (j in 0 until exercises.length()) {
                    val reObj = exercises.getJSONObject(j)
                    val reId = db.routineDao().insertExercise(
                        RoutineExerciseEntity(
                            routineId = routineId,
                            exerciseId = reObj.getString("exerciseId"),
                            orderIndex = reObj.getInt("orderIndex"),
                            notes = reObj.optString("notes", ""),
                            supersetId = if (reObj.isNull("supersetId")) null else reObj.getInt("supersetId"),
                            restSeconds = if (reObj.isNull("restSeconds")) null else reObj.getInt("restSeconds"),
                        )
                    )
                    val sets = reObj.getJSONArray("sets")
                    db.routineDao().insertSets((0 until sets.length()).map { k ->
                        val s = sets.getJSONObject(k)
                        RoutineSetEntity(
                            routineExerciseId = reId,
                            setIndex = s.getInt("setIndex"),
                            weightKg = s.getDouble("weightKg"),
                            reps = s.getInt("reps"),
                            rpe = if (s.isNull("rpe")) null else s.getDouble("rpe"),
                            isWarmup = s.optBoolean("isWarmup", false),
                        )
                    })
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