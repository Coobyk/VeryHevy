package com.veryhevy.ui.tracker

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veryhevy.VeryHevyApp
import com.veryhevy.data.RestSeconds
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.ExerciseEntity
import com.veryhevy.data.db.SetEntity
import com.veryhevy.data.db.WorkoutWithExercises
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class PreviousSet(val weightKg: Double, val reps: Int)

@Immutable
data class RestTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Int = 0,
    val totalSeconds: Int = 0,
)

class TrackerViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as VeryHevyApp
    private val workoutRepo = container.workoutRepository
    private val exerciseRepo = container.exerciseRepository
    private val settings = container.settingsRepository

    val unit: StateFlow<WeightUnit> = settings.settings
        .map { it.unit }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)

    val workout: StateFlow<WorkoutWithExercises?> = workoutRepo.observeInProgressWithDetails()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _restTimer = MutableStateFlow(RestTimerState())
    val restTimer: StateFlow<RestTimerState> = _restTimer.asStateFlow()

    /** exerciseId → last-session heaviest set; null value means "none found". */
    private val _previousSets = MutableStateFlow<Map<String, PreviousSet?>>(emptyMap())
    val previousSets: StateFlow<Map<String, PreviousSet?>> = _previousSets.asStateFlow()

    private var restJob: Job? = null
    private var defaultRestSeconds = RestSeconds.DEFAULT

    init {
        viewModelScope.launch {
            settings.settings.collect { defaultRestSeconds = it.defaultRestSeconds }
        }
    }

    fun searchExercises(query: String): Flow<List<ExerciseEntity>> = exerciseRepo.search(query)

    /** The sets done for this exercise in the most recent prior workout. */
    fun loadPrevious(exerciseId: String) {
        if (_previousSets.value.containsKey(exerciseId)) return
        viewModelScope.launch {
            val currentWorkoutId = workout.value?.workout?.id
            val history = workoutRepo.setHistoryForExercise(exerciseId)
                .filter { it.workoutId != currentWorkoutId }
            val best = history.maxByOrNull { it.workoutStartedAt }
            val heaviest = best?.let { b ->
                history.filter { it.workoutId == b.workoutId }.maxByOrNull { it.set.weightKg }
            }
            _previousSets.value = _previousSets.value +
                (exerciseId to heaviest?.let { PreviousSet(it.set.weightKg, it.set.reps) })
        }
    }

    fun startEmptyWorkout() {
        viewModelScope.launch {
            workoutRepo.startWorkout("Workout")
        }
    }

    fun finishWorkout() {
        val id = workout.value?.workout?.id ?: return
        viewModelScope.launch {
            workoutRepo.finishWorkout(id)
            _restTimer.value = RestTimerState()
        }
    }

    fun discardWorkout() {
        val id = workout.value?.workout?.id ?: return
        viewModelScope.launch {
            workoutRepo.deleteWorkout(id)
            _restTimer.value = RestTimerState()
        }
    }

    fun renameWorkout(name: String) {
        val id = workout.value?.workout?.id ?: return
        viewModelScope.launch { workoutRepo.updateName(id, name) }
    }

    fun addExercise(exerciseId: String) {
        val id = workout.value?.workout?.id ?: return
        viewModelScope.launch { workoutRepo.addExercise(id, exerciseId) }
    }

    fun removeExercise(orderIndex: Int) {
        val id = workout.value?.workout?.id ?: return
        viewModelScope.launch { workoutRepo.removeExercise(id, orderIndex) }
    }

    fun moveExercise(orderIndex: Int, direction: Int) {
        val id = workout.value?.workout?.id ?: return
        viewModelScope.launch { workoutRepo.moveExercise(id, orderIndex, direction) }
    }

    /** Groups this exercise with the previous one (or the next one if it is the first). */
    fun toggleSupersetWithPrevious(orderIndex: Int) {
        val id = workout.value?.workout?.id ?: return
        viewModelScope.launch {
            val w = workoutRepo.byId(id) ?: return@launch
            val sorted = w.exercises.sortedBy { it.exercise.orderIndex }
            val current = sorted.firstOrNull { it.exercise.orderIndex == orderIndex } ?: return@launch
            val neighbor = sorted.firstOrNull { it.exercise.orderIndex == orderIndex - 1 }
                ?: sorted.firstOrNull { it.exercise.orderIndex == orderIndex + 1 }
                ?: return@launch
            val newId = when {
                current.exercise.supersetId != null -> null
                neighbor.exercise.supersetId != null -> neighbor.exercise.supersetId
                else -> (sorted.maxOfOrNull { it.exercise.supersetId ?: 0 } ?: 0) + 1
            }
            workoutRepo.setSuperset(id, orderIndex, newId)
        }
    }

    fun updateExerciseNotes(weId: Long, notes: String) {
        viewModelScope.launch { workoutRepo.updateExerciseNotes(weId, notes) }
    }

    fun setRestSeconds(weId: Long, seconds: Int?) {
        viewModelScope.launch { workoutRepo.updateRestSeconds(weId, seconds) }
    }

    fun addSet(weId: Long, template: SetEntity?) {
        viewModelScope.launch { workoutRepo.addSet(weId, template) }
    }

    fun deleteSet(set: SetEntity) {
        viewModelScope.launch { workoutRepo.deleteSet(set) }
    }

    fun toggleWarmup(set: SetEntity) {
        viewModelScope.launch { workoutRepo.updateSet(set.copy(isWarmup = !set.isWarmup)) }
    }

    fun setRpe(set: SetEntity, rpe: Double?) {
        viewModelScope.launch { workoutRepo.updateSet(set.copy(rpe = rpe)) }
    }

    fun updateSetValues(set: SetEntity, weightKg: Double, reps: Int) {
        viewModelScope.launch { workoutRepo.updateSet(set.copy(weightKg = weightKg, reps = reps)) }
    }

    fun toggleSetDone(set: SetEntity, restSeconds: Int?) {
        viewModelScope.launch {
            val nowDone = !set.isDone
            workoutRepo.setDone(
                id = set.id,
                done = nowDone,
                completedAt = if (nowDone) System.currentTimeMillis() else null,
            )
            if (nowDone) startRest(restSeconds ?: defaultRestSeconds)
        }
    }

    // ----- Rest timer -----

    fun startRest(seconds: Int) {
        restJob?.cancel()
        _restTimer.value = RestTimerState(isActive = true, remainingSeconds = seconds, totalSeconds = seconds)
        restJob = viewModelScope.launch {
            while (_restTimer.value.remainingSeconds > 0) {
                delay(1000)
                _restTimer.value = _restTimer.value.copy(remainingSeconds = _restTimer.value.remainingSeconds - 1)
            }
            _restTimer.value = _restTimer.value.copy(isActive = false)
            RestAlarm.notify(getApplication())
        }
    }

    fun addRestTime(seconds: Int = RestSeconds.TIMER_ADD) {
        if (!_restTimer.value.isActive) return
        _restTimer.value = _restTimer.value.copy(
            remainingSeconds = _restTimer.value.remainingSeconds + seconds,
            totalSeconds = _restTimer.value.totalSeconds + seconds,
        )
    }

    fun skipRest() {
        restJob?.cancel()
        _restTimer.value = RestTimerState()
    }
}
