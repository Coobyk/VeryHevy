@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.veryhevy.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veryhevy.VeryHevyApp
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.ExerciseEntity
import com.veryhevy.data.db.RoutineSetEntity
import com.veryhevy.data.db.RoutineWithExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as VeryHevyApp
    private val repo = container.routineRepository
    private val exerciseRepo = container.exerciseRepository

    val unit: StateFlow<WeightUnit> = container.settingsRepository.settings
        .map { it.unit }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)

    val routines: StateFlow<List<RoutineWithExercises>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** The routine open in the editor; observed, so DB writes refresh it automatically. */
    private val openRoutineId = MutableStateFlow<Long?>(null)
    val current: StateFlow<RoutineWithExercises?> = openRoutineId
        .flatMapLatest { id -> if (id == null) flowOf(null) else repo.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun loadRoutine(id: Long) {
        openRoutineId.value = id
    }

    fun createRoutine(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(repo.create(name)) }
    }

    fun deleteRoutine(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }

    fun duplicateRoutine(id: Long) {
        viewModelScope.launch { repo.duplicate(id) }
    }

    fun renameRoutine(id: Long, name: String) {
        viewModelScope.launch { repo.rename(id, name) }
    }

    fun addExercise(routineId: Long, exerciseId: String) {
        viewModelScope.launch { repo.addExercise(routineId, exerciseId) }
    }

    fun removeExercise(routineId: Long, orderIndex: Int) {
        viewModelScope.launch { repo.removeExercise(routineId, orderIndex) }
    }

    fun moveExercise(routineId: Long, orderIndex: Int, direction: Int) {
        viewModelScope.launch { repo.moveExercise(routineId, orderIndex, direction) }
    }

    fun updateRestSeconds(reId: Long, seconds: Int) {
        viewModelScope.launch { repo.setRestSeconds(reId, seconds) }
    }

    fun addSet(reId: Long, template: RoutineSetEntity?) {
        viewModelScope.launch { repo.addSet(reId, template) }
    }

    fun deleteSet(set: RoutineSetEntity) {
        viewModelScope.launch { repo.deleteSet(set) }
    }

    fun updateSetValues(set: RoutineSetEntity, weightKg: Double, reps: Int) {
        viewModelScope.launch { repo.updateSetValues(set, weightKg, reps) }
    }

    fun setRpe(set: RoutineSetEntity, rpe: Double?) {
        viewModelScope.launch { repo.setSetRpe(set, rpe) }
    }

    fun toggleWarmup(set: RoutineSetEntity) {
        viewModelScope.launch { repo.toggleSetWarmup(set) }
    }

    fun searchExercises(query: String): Flow<List<ExerciseEntity>> = exerciseRepo.search(query)

    fun startWorkout(routineId: Long, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            repo.startWorkoutFromRoutine(routineId)?.let(onStarted)
        }
    }
}
