package com.veryhevy.ui.routines

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veryhevy.VeryHevyApp
import com.veryhevy.data.RoutineRepository
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.RoutineSetEntity
import com.veryhevy.data.db.RoutineWithExercises
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RoutinesViewModel(app: Application) : AndroidViewModel(app) {

    val repo = RoutineRepository((app as VeryHevyApp).database)
    val exerciseRepository = (app as VeryHevyApp).exerciseRepository

    val unit: StateFlow<WeightUnit> =
        (app as VeryHevyApp).settingsRepository.settings
            .map { it.unit }
            .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)

    val routines: StateFlow<List<RoutineWithExercises>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _current = MutableStateFlow<RoutineWithExercises?>(null)
    val current: StateFlow<RoutineWithExercises?> = _current.asStateFlow()

    fun loadRoutine(id: Long) {
        viewModelScope.launch { _current.value = repo.byId(id) }
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
        viewModelScope.launch {
            repo.rename(id, name)
            _current.value = repo.byId(id)
        }
    }

    fun updateNotes(id: Long, notes: String) {
        viewModelScope.launch { repo.updateNotes(id, notes) }
    }

    fun addExercise(routineId: Long, exerciseId: String) {
        viewModelScope.launch {
            repo.addExercise(routineId, exerciseId)
            _current.value = repo.byId(routineId)
        }
    }

    fun removeExercise(routineId: Long, orderIndex: Int) {
        viewModelScope.launch {
            repo.removeExercise(routineId, orderIndex)
            _current.value = repo.byId(routineId)
        }
    }

    fun moveExercise(routineId: Long, orderIndex: Int, direction: Int) {
        viewModelScope.launch {
            repo.moveExercise(routineId, orderIndex, direction)
            _current.value = repo.byId(routineId)
        }
    }

    fun updateRestSeconds(reId: Long, routineId: Long, seconds: Int) {
        viewModelScope.launch {
            repo.setRestSeconds(reId, seconds)
            _current.value = repo.byId(routineId)
        }
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

    fun startWorkout(routineId: Long, onStarted: (Long) -> Unit) {
        viewModelScope.launch {
            repo.startWorkoutFromRoutine(routineId)?.let(onStarted)
        }
    }
}
