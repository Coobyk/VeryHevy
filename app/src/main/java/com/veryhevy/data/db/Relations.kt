package com.veryhevy.data.db

import androidx.compose.runtime.Immutable

import androidx.room.Embedded
import androidx.room.Relation

@Immutable
data class WorkoutExerciseWithSets(
    @Embedded val exercise: WorkoutExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exerciseDetails: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "workoutExerciseId")
    val sets: List<SetEntity>,
)

@Immutable
data class WorkoutWithExercises(
    @Embedded val workout: WorkoutEntity,
    @Relation(entity = WorkoutExerciseEntity::class, parentColumn = "id", entityColumn = "workoutId")
    val exercises: List<WorkoutExerciseWithSets>,
)

@Immutable
data class RoutineExerciseWithSets(
    @Embedded val exercise: RoutineExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exerciseDetails: ExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "routineExerciseId")
    val sets: List<RoutineSetEntity>,
)

@Immutable
data class RoutineWithExercises(
    @Embedded val routine: RoutineEntity,
    @Relation(entity = RoutineExerciseEntity::class, parentColumn = "id", entityColumn = "routineId")
    val exercises: List<RoutineExerciseWithSets>,
)
