package com.veryhevy.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.WorkoutWithExercises
import com.veryhevy.ui.Format
import com.veryhevy.ui.LedgerCard
import com.veryhevy.ui.VhDivider
import com.veryhevy.ui.VhTag
import com.veryhevy.ui.theme.numeric

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    workoutId: Long,
    vm: HistoryViewModel,
    onBack: () -> Unit,
) {
    val unit by vm.unit.collectAsStateWithLifecycle()
    var workout by remember { mutableStateOf<WorkoutWithExercises?>(null) }

    LaunchedEffect(workoutId) {
        workout = vm.workout(workoutId)
    }

    val w = workout
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(w?.workout?.name ?: "Workout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        vm.deleteWorkout(workoutId)
                        onBack()
                    }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (w == null) return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    InlineStat(Format.dateTime(w.workout.startedAt), "date")
                    InlineStat(w.workout.endedAt?.let { Format.duration(w.workout.startedAt, it) } ?: Format.durationSince(w.workout.startedAt), "duration")
                    InlineStat("${Format.volume(doneVolumeKg(w), unit)} ${Format.unitLabel(unit)}", "volume")
                }
                Spacer(Modifier.height(4.dp))
            }
            itemsIndexed(w.exercises.sortedBy { it.exercise.orderIndex }, key = { _, it -> it.exercise.id }) { _, item ->
                LedgerCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                item.exerciseDetails.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            item.exercise.supersetId?.let {
                                VhTag(Format.supersetLabel(it), accent = true)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        item.sets.sortedBy { it.setIndex }.forEachIndexed { i, set ->
                            VhDivider()
                            Row(Modifier.padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                if (set.isWarmup) {
                                    VhTag("W", Modifier.width(32.dp))
                                } else {
                                    Text(
                                        "${i + 1}",
                                        Modifier.width(32.dp),
                                        style = MaterialTheme.typography.bodySmall.numeric(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "${Format.weight(set.weightKg, unit)} ${Format.unitLabel(unit)} × ${set.reps}",
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium.numeric(),
                                    fontWeight = if (set.isDone) FontWeight.Medium else FontWeight.Normal,
                                    color = if (set.isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                set.rpe?.let {
                                    Text(
                                        "RPE ${Format.rpe(it)}",
                                        style = MaterialTheme.typography.bodySmall.numeric(),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        if (item.exercise.notes.isNotBlank()) {
                            Text(
                                item.exercise.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
            if (w.workout.notes.isNotBlank()) {
                item {
                    LedgerCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text("Workout notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(w.workout.notes, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineStat(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.bodyMedium.numeric(), fontWeight = FontWeight.Medium)
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun doneVolumeKg(w: WorkoutWithExercises): Double =
    w.exercises.flatMap { it.sets }.filter { it.isDone }.sumOf { it.weightKg * it.reps }
