package com.veryhevy.ui.home

import android.app.Application
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as VeryHevyApp
    private val workoutRepo = com.veryhevy.data.WorkoutRepository(container.database)
    private val measurementDao = container.database.measurementDao()
    private val exerciseDao = container.database.exerciseDao()

    val unit: StateFlow<WeightUnit> = container.settingsRepository.settings
        .map { it.unit }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)

    private val setRecords = workoutRepo.observeFinished()
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

    val weeklyVolume: StateFlow<List<WeeklyVolume>> = setRecords
        .map { Analytics.weeklyVolume(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val muscleVolume: StateFlow<List<MuscleVolume>> = setRecords
        .map { Analytics.volumeByMuscle(it).take(8) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val personalRecords: StateFlow<List<PersonalRecord>> = setRecords
        .map { Analytics.personalRecords(it).take(20) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workoutCount: StateFlow<Int> = setRecords
        .map { it.map { r -> r.workoutId }.distinct().size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streak: StateFlow<Int> = setRecords
        .map { Analytics.weeklyStreak(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val measurements: StateFlow<List<MeasurementEntity>> = measurementDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _exerciseNames = MutableStateFlow<Map<String, String>>(emptyMap())
    val exerciseNames: StateFlow<Map<String, String>> = _exerciseNames.asStateFlow()

    init {
        viewModelScope.launch {
            personalRecords.collect { records ->
                val missing = records.map { it.exerciseId }.filter { it !in _exerciseNames.value }
                if (missing.isEmpty()) return@collect
                val names = missing.mapNotNull { id -> exerciseDao.byId(id)?.let { it.id to it.name } }
                _exerciseNames.value = _exerciseNames.value + names
            }
        }
    }

    fun addMeasurement(weightKg: Double) {
        viewModelScope.launch {
            measurementDao.insert(
                MeasurementEntity(dateEpochDay = java.time.LocalDate.now().toEpochDay(), weightKg = weightKg)
            )
        }
    }

    fun deleteMeasurement(m: MeasurementEntity) {
        viewModelScope.launch { measurementDao.delete(m) }
    }

    companion object {
        fun weeklyVolumeOf(records: List<SetRecord>) = Analytics.weeklyVolume(records)
    }
}
