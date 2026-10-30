package com.veryhevy.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class SetWithWorkout(
    @Embedded val set: SetEntity,
    val workoutId: Long,
    val workoutStartedAt: Long,
    val workoutName: String,
)

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises ORDER BY name")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE name LIKE '%' || :query || '%' ORDER BY name LIMIT 50")
    fun search(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun byId(id: String): ExerciseEntity?

    @Query("SELECT * FROM exercises ORDER BY name")
    suspend fun all(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<ExerciseEntity>)

    @Upsert
    suspend fun upsert(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustom(id: String)
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts WHERE endedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeInProgress(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE endedAt IS NULL LIMIT 1")
    suspend fun inProgress(): WorkoutEntity?

    @Transaction
    @Query("SELECT * FROM workouts WHERE endedAt IS NOT NULL ORDER BY startedAt DESC")
    fun observeFinished(): Flow<List<WorkoutWithExercises>>

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun withExercises(id: Long): WorkoutWithExercises?

    @Transaction
    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWithExercises(id: Long): Flow<WorkoutWithExercises?>

    @Query(
        """
        SELECT ws.*, we.workoutId, w.startedAt AS workoutStartedAt, w.name AS workoutName
        FROM workout_sets ws
        JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        JOIN workouts w ON we.workoutId = w.id
        WHERE we.exerciseId = :exerciseId AND ws.isDone = 1
        ORDER BY w.startedAt, ws.setIndex
        """
    )
    suspend fun setHistoryForExercise(exerciseId: String): List<SetWithWorkout>

    @Insert
    suspend fun insert(workout: WorkoutEntity): Long

    @Update
    suspend fun update(workout: WorkoutEntity)

    @Delete
    suspend fun delete(workout: WorkoutEntity)

    @Query("SELECT * FROM workout_exercises WHERE id = :id")
    suspend fun exerciseById(id: Long): WorkoutExerciseEntity?

    @Insert
    suspend fun insertExercise(exercise: WorkoutExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: WorkoutExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: WorkoutExerciseEntity)

    @Insert
    suspend fun insertSets(sets: List<SetEntity>)

    @Upsert
    suspend fun upsertSet(set: SetEntity)

    @Query("UPDATE workout_sets SET isDone = :done, completedAt = :completedAt WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean, completedAt: Long?)

    @Delete
    suspend fun deleteSet(set: SetEntity)

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setIndex")
    suspend fun setsFor(workoutExerciseId: Long): List<SetEntity>
}

@Dao
interface RoutineDao {
    @Transaction
    @Query("SELECT * FROM routines ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<RoutineWithExercises>>

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun byId(id: Long): RoutineWithExercises?

    @Transaction
    @Query("SELECT * FROM routines WHERE id = :id")
    fun observeById(id: Long): Flow<RoutineWithExercises?>

    @Query("SELECT COUNT(*) FROM routines")
    suspend fun count(): Int

    @Insert
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Delete
    suspend fun delete(routine: RoutineEntity)

    @Insert
    suspend fun insertExercise(exercise: RoutineExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: RoutineExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: RoutineExerciseEntity)

    @Query("SELECT * FROM routine_exercises WHERE id = :id")
    suspend fun exerciseById(id: Long): RoutineExerciseEntity?

    @Query("SELECT * FROM routine_sets WHERE routineExerciseId = :routineExerciseId ORDER BY setIndex")
    suspend fun setsFor(routineExerciseId: Long): List<RoutineSetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<RoutineSetEntity>)

    @Delete
    suspend fun deleteSets(sets: List<RoutineSetEntity>)

    @Query("DELETE FROM routine_sets WHERE routineExerciseId = :routineExerciseId")
    suspend fun deleteSetsFor(routineExerciseId: Long)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY dateEpochDay DESC")
    fun observeAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements ORDER BY dateEpochDay")
    suspend fun all(): List<MeasurementEntity>

    @Insert
    suspend fun insert(measurement: MeasurementEntity): Long

    @Delete
    suspend fun delete(measurement: MeasurementEntity)

    @Query("DELETE FROM measurements")
    suspend fun deleteAll()
}
