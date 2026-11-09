package com.veryhevy.ui.home

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veryhevy.VeryHevyApp
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.MeasurementEntity
import com.veryhevy.domain.Analytics
import com.veryhevy.domain.MuscleVolume
import com.veryhevy.domain.PersonalRecord
import com.veryhevy.domain.SetRecord
import com.veryhevy.domain.WeeklyVolume
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class RecordRow(val exerciseName: String, val record: PersonalRecord)

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as VeryHevyApp
    private val workoutRepo = container.workoutRepository
    private val measurementRepo = container.measurementRepository
    private val exerciseRepo = container.exerciseRepository

    val unit: StateFlow<WeightUnit> = container.settingsRepository.settings
        .map { it.unit }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)

    // Shared upstream: the finished-workout graph is flattened once, and the
    // analytics below map that single StateFlow instead of each re-running
    // the same Room subscription and flatten.
    private val setRecords: StateFlow<List<SetRecord>> = workoutRepo.observeFinished()
        .map { workouts ->
            workouts.flatMap { w ->
                w.exercises.flatMap { we ->
                    we.sets.filter { it.isDone }.map { s ->
                        SetRecord(
                            exerciseId = we.exercise.exerciseId,
                            muscleGroup = we.exerciseDetails.muscleGroup,
                            weightKg = s.weightKg,
                            reps = s.reps,
                            isWarmup = s.isWarmup,
                            startedAt = s.completedAt ?: w.workout.startedAt,
                            workoutId = w.workout.id,
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyVolume: StateFlow<List<WeeklyVolume>> = setRecords
        .map { Analytics.weeklyVolume(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val muscleVolume: StateFlow<List<MuscleVolume>> = setRecords
        .map { Analytics.volumeByMuscle(it).take(8) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val exerciseNames: StateFlow<Map<String, String>> = flow { emit(exerciseRepo.all()) }
        .map { list -> list.associate { it.id to it.name } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val recordRows: StateFlow<List<RecordRow>> =
        combine(setRecords, exerciseNames) { records, names ->
            Analytics.personalRecords(records)
                .take(20)
                .map { RecordRow(names[it.exerciseId] ?: it.exerciseId, it) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutCount: StateFlow<Int> = setRecords
        .map { it.map { r -> r.workoutId }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streak: StateFlow<Int> = setRecords
        .map { Analytics.weeklyStreak(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val measurements: StateFlow<List<MeasurementEntity>> = measurementRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMeasurement(weightKg: Double) {
        viewModelScope.launch { measurementRepo.add(weightKg) }
    }

    fun deleteMeasurement(m: MeasurementEntity) {
        viewModelScope.launch { measurementRepo.delete(m) }
    }
}
