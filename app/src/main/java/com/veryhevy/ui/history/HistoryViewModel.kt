package com.veryhevy.ui.history

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veryhevy.VeryHevyApp
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.WorkoutWithExercises
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HistoryEntry(
    val workoutId: Long,
    val name: String,
    val startedAt: Long,
    val endedAt: Long,
    val totalVolumeKg: Double,
    val exerciseCount: Int,
    val setCount: Int,
)

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = com.veryhevy.data.WorkoutRepository((app as VeryHevyApp).database)

    val unit: StateFlow<WeightUnit> =
        (app as VeryHevyApp).settingsRepository.settings
            .map { it.unit }
            .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)

    val entries: StateFlow<List<HistoryEntry>> = repo.observeFinished()
        .map { list ->
            list.map { w ->
                val doneSets = w.exercises.flatMap { it.sets }.filter { it.isDone }
                HistoryEntry(
                    workoutId = w.workout.id,
                    name = w.workout.name,
                    startedAt = w.workout.startedAt,
                    endedAt = w.workout.endedAt ?: w.workout.startedAt,
                    totalVolumeKg = doneSets.sumOf { it.weightKg * it.reps },
                    exerciseCount = w.exercises.size,
                    setCount = doneSets.size,
                )
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun workout(id: Long): WorkoutWithExercises? = repo.byId(id)

    fun deleteWorkout(id: Long) {
        viewModelScope.launch {
            repo.deleteWorkout(id)
        }
    }
}
