package com.veryhevy.ui.tracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.SetEntity
import com.veryhevy.data.db.WorkoutExerciseWithSets
import com.veryhevy.domain.Calculators
import com.veryhevy.ui.Format
import com.veryhevy.ui.exercises.ExercisePickerSheet
import java.time.LocalTime

@Composable
fun TrackerScreen(vm: TrackerViewModel) {
    val workout by vm.workout.collectAsState()
    val unit by vm.unit.collectAsState()
    val previousSets by vm.previousSets.collectAsState()
    val restTimer by vm.restTimer.collectAsState()
    var showPicker by remember { mutableStateOf(false) }

    val w = workout
    if (w == null) {
        EmptyTracker(onStart = { vm.startEmptyWorkout() })
    } else {
        Column(Modifier.fillMaxSize()) {
            WorkoutHeader(
                name = w.workout.name,
                startedAt = w.workout.startedAt,
                unit = unit,
                onRename = vm::renameWorkout,
                onFinish = { vm.finishWorkout() },
                onDiscard = { vm.discardWorkout() },
            )
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(w.exercises.sortedBy { it.exercise.orderIndex }, key = { _, it -> it.exercise.id }) { _, item ->
                    ExerciseCard(
                        item = item,
                        unit = unit,
                        restTimer = restTimer,
                        vm = vm,
                        previous = previousSets[item.exerciseDetails.id],
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add exercise")
                    }
                }
            }
            if (restTimer.isActive) {
                RestTimerBar(
                    state = restTimer,
                    onAddTime = { vm.addRestTime(15) },
                    onSkip = { vm.skipRest() },
                )
            }
        }
        if (showPicker) {
            ExercisePickerSheet(
                searchFlow = vm.exerciseRepository::search,
                onDismiss = { showPicker = false },
                onPick = { vm.addExercise(it.id) },
            )
        }
    }
}

@Composable
private fun EmptyTracker(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.FitnessCenter,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text("No workout in progress", style = MaterialTheme.typography.titleLarge)
        Text(
            "Start a new workout to log your sets, reps and weights.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        Button(onClick = onStart) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Start empty workout")
        }
    }
}

@Composable
private fun WorkoutHeader(
    name: String,
    startedAt: Long,
    unit: WeightUnit,
    onRename: (String) -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (editing) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onRename,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Workout name") },
                )
                IconButton(onClick = { editing = false }) {
                    Icon(Icons.Filled.Check, contentDescription = "Done")
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        Format.dateTime(startedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { editing = true }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Rename")
                }
            }
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "More")
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Finish workout") },
                    onClick = {
                        showMenu = false
                        onFinish()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Discard workout") },
                    onClick = {
                        showMenu = false
                        confirmDiscard = true
                    },
                )
            }
        }
        if (confirmDiscard) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Discard this workout?", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { confirmDiscard = false; onDiscard() }) { Text("Discard") }
                TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    item: WorkoutExerciseWithSets,
    unit: WeightUnit,
    restTimer: RestTimerState,
    vm: TrackerViewModel,
    previous: PreviousSet?,
) {
    androidx.compose.runtime.LaunchedEffect(item.exerciseDetails.id) {
        vm.loadPrevious(item.exerciseDetails.id)
    }
    var showMenu by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(item.exercise.notes.isNotBlank()) }
    var showPlateCalc by remember { mutableStateOf(false) }
    val sets = item.sets.sortedBy { it.setIndex }
    val supersetLabel = item.exercise.supersetId?.let { "Superset ${'A' + it - 1}" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        item.exerciseDetails.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        listOfNotNull(
                            supersetLabel,
                            item.exerciseDetails.muscleGroup.replaceFirstChar { it.uppercase() },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (item.exercise.supersetId == null) "Superset with neighbor" else "Remove superset") },
                        onClick = {
                            showMenu = false
                            vm.toggleSupersetWithPrevious(item.exercise.orderIndex)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Move up") },
                        onClick = { showMenu = false; vm.moveExercise(item.exercise.orderIndex, -1) },
                    )
                    DropdownMenuItem(
                        text = { Text("Move down") },
                        onClick = { showMenu = false; vm.moveExercise(item.exercise.orderIndex, 1) },
                    )
                    DropdownMenuItem(
                        text = { Text(if (showNotes) "Hide notes" else "Add notes") },
                        onClick = { showMenu = false; showNotes = !showNotes },
                    )
                    DropdownMenuItem(
                        text = { Text("Rest ${item.exercise.restSeconds ?: 90}s") },
                        onClick = {
                            showMenu = false
                            vm.setRestSeconds(item.exercise.id, (item.exercise.restSeconds ?: 90).let { if (it >= 240) 90 else it + 30 })
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Plate calculator") },
                        onClick = { showMenu = false; showPlateCalc = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Remove exercise") },
                        onClick = { showMenu = false; vm.removeExercise(item.exercise.orderIndex) },
                    )
                }
            }

            if (showNotes) {
                OutlinedTextField(
                    value = item.exercise.notes,
                    onValueChange = { vm.updateExerciseNotes(item.exercise.id, it) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    placeholder = { Text("Notes") },
                    minLines = 1,
                    maxLines = 3,
                )
            }

            // Table header
            Row(Modifier.padding(vertical = 4.dp)) {
                Text("SET", Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("PREVIOUS", Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("KG" + if (unit == WeightUnit.LB) "/LB" else "", Modifier.weight(1.4f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("REPS", Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("RPE", Modifier.weight(0.9f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("", Modifier.weight(0.9f))
            }

            sets.forEach { set ->
                SetRow(
                    set = set,
                    index = sets.indexOf(set) + 1,
                    unit = unit,
                    previous = previous,
                    onWeightReps = { w2, r -> vm.updateSetValues(set, w2, r) },
                    onToggleDone = { vm.toggleSetDone(set, item.exercise.restSeconds) },
                    onToggleWarmup = { vm.toggleWarmup(set) },
                    onSetRpe = { vm.setRpe(set, it) },
                    onDelete = { vm.deleteSet(set) },
                )
            }

            if (showPlateCalc) {
                PlateCalculatorDialog(
                    unit = unit,
                    onDismiss = { showPlateCalc = false },
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { vm.addSet(item.exercise.id, sets.lastOrNull()) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add set")
                }
                IconButton(onClick = { vm.toggleWarmup(sets.lastOrNull() ?: return@IconButton) }) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = "Warmup last set")
                }
            }
        }
    }
}

@Composable
private fun SetRow(
    set: SetEntity,
    index: Int,
    unit: WeightUnit,
    previous: PreviousSet?,
    onWeightReps: (Double, Int) -> Unit,
    onToggleDone: () -> Unit,
    onToggleWarmup: () -> Unit,
    onSetRpe: (Double?) -> Unit,
    onDelete: () -> Unit,
) {
    var weightText by remember(set.id) { mutableStateOf(if (set.weightKg == 0.0) "" else Format.weight(set.weightKg, unit)) }
    var repsText by remember(set.id) { mutableStateOf(if (set.reps == 0) "" else set.reps.toString()) }
    var showRpeMenu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    var weightWasFocused by remember { mutableStateOf(false) }
    var repsWasFocused by remember { mutableStateOf(false) }

    fun commit() {
        val displayWeight = weightText.toDoubleOrNull() ?: 0.0
        val kg = Format.displayToKg(displayWeight, unit)
        val reps = repsText.toIntOrNull() ?: 0
        if (kg != set.weightKg || reps != set.reps) onWeightReps(kg, reps)
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.width(36.dp), verticalAlignment = Alignment.CenterVertically) {
            if (set.isWarmup) {
                Icon(Icons.Filled.FitnessCenter, contentDescription = "Warmup", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
            }
            Text("$index", style = MaterialTheme.typography.bodyMedium)
        }
        PreviousSetText(previous, unit, Modifier.weight(1.2f))
        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
            modifier = Modifier
                .weight(1.4f)
                .padding(end = 4.dp)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        weightWasFocused = true
                    } else if (weightWasFocused) {
                        weightWasFocused = false
                        commit()
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = repsText,
            onValueChange = { repsText = it.filter { c -> c.isDigit() } },
            modifier = Modifier
                .weight(1.2f)
                .padding(end = 4.dp)
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        repsWasFocused = true
                    } else if (repsWasFocused) {
                        repsWasFocused = false
                        commit()
                    }
                },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        Box2(Modifier.weight(0.9f)) {
            TextButton(onClick = { showRpeMenu = true }) {
                Text(set.rpe?.let { "%.1f".format(it) } ?: "-", style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(expanded = showRpeMenu, onDismissRequest = { showRpeMenu = false }) {
                listOf(null, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0).forEach { rpe ->
                    DropdownMenuItem(
                        text = { Text(rpe?.let { "%.1f".format(it) } ?: "None") },
                        onClick = {
                            showRpeMenu = false
                            onSetRpe(rpe)
                        },
                    )
                }
            }
        }
        Row(Modifier.weight(0.9f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            if (set.isDone) {
                IconButton(onClick = onToggleWarmup, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.FitnessCenter, contentDescription = "Warmup", Modifier.size(16.dp))
                }
            }
            FilledTonalIconButton(
                onClick = {
                    commit()
                    onToggleDone()
                },
                modifier = Modifier.size(32.dp),
                colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (set.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (set.isDone) "Mark not done" else "Mark done",
                    Modifier.size(18.dp),
                    tint = if (set.isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Delete set", Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun Box2(modifier: Modifier, content: @Composable () -> Unit) =
    androidx.compose.foundation.layout.Box(modifier) { content() }

@Composable
private fun PreviousSetText(previous: PreviousSet?, unit: WeightUnit, modifier: Modifier) {
    val text = if (previous == null || (previous.weightKg <= 0.0 && previous.reps <= 0)) {
        "—"
    } else {
        "${Format.weight(previous.weightKg, unit)} × ${previous.reps}"
    }
    Text(text, modifier, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun RestTimerBar(state: RestTimerState, onAddTime: () -> Unit, onSkip: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    Format.clock(state.remainingSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onAddTime) { Text("+15s") }
                TextButton(onClick = onSkip) { Text("Skip") }
            }
            LinearProgressIndicator(
                progress = {
                    if (state.totalSeconds == 0) 0f
                    else 1f - state.remainingSeconds.toFloat() / state.totalSeconds
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}


@Composable
private fun PlateCalculatorDialog(unit: WeightUnit, onDismiss: () -> Unit) {
    var targetText by remember { mutableStateOf("60") }
    val barKg = 20.0
    val platesKg = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)

    val displayTarget = targetText.toDoubleOrNull() ?: 0.0
    val targetKg = Format.displayToKg(displayTarget, unit)
    val plates = Calculators.platesFor(targetKg, barKg, platesKg)
    val loadableKg = Calculators.closestLoadable(targetKg, barKg, platesKg)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plate calculator") },
        text = {
            Column {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target weight (${Format.unitLabel(unit)})") },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                if (targetKg <= barKg) {
                    Text("Target must be above the empty ${Format.weight(barKg, unit)} ${Format.unitLabel(unit)} bar.")
                } else {
                    Text(
                        "Per side: " + if (plates.isEmpty()) "nothing to add" else plates.joinToString(" + ") { Format.weight(it, unit) },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (loadableKg < targetKg - 0.01) {
                        Text(
                            "Closest loadable: ${Format.weight(loadableKg, unit)} ${Format.unitLabel(unit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}
