package com.veryhevy.ui.tracker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.veryhevy.data.RestSeconds
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.SetEntity
import com.veryhevy.data.db.WorkoutExerciseWithSets
import com.veryhevy.domain.Calculators
import com.veryhevy.ui.EmptyState
import com.veryhevy.ui.Format
import com.veryhevy.ui.LedgerCard
import com.veryhevy.ui.RPE_OPTIONS
import com.veryhevy.ui.SetField
import com.veryhevy.ui.VhDivider
import com.veryhevy.ui.VhTag
import com.veryhevy.ui.exercises.ExercisePickerSheet
import com.veryhevy.ui.theme.numeric

@Composable
fun TrackerScreen(vm: TrackerViewModel) {
    val workout by vm.workout.collectAsStateWithLifecycle()
    val unit by vm.unit.collectAsStateWithLifecycle()
    val previousSets by vm.previousSets.collectAsStateWithLifecycle()
    // Read only through derivedStateOf below so the once-per-second countdown
    // doesn't recompose the whole screen — just the timer bar.
    val restTimer by vm.restTimer.collectAsStateWithLifecycle()
    val restVisible by remember { derivedStateOf { restTimer.isActive } }
    var showPicker by remember { mutableStateOf(false) }

    val w = workout
    if (w == null) {
        EmptyState(
            icon = Icons.Filled.FitnessCenter,
            title = "No workout in progress",
            body = "Start a new workout to log your sets, reps and weights.",
            modifier = Modifier.fillMaxSize(),
            action = {
                Button(onClick = { vm.startEmptyWorkout() }) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start empty workout")
                }
            },
        )
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
            val exercises = remember(w.exercises) { w.exercises.sortedBy { it.exercise.orderIndex } }
            VhDivider()
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(exercises, key = { it.exercise.id }) { item ->
                    ExerciseBlock(
                        modifier = Modifier.animateItem(),
                        item = item,
                        unit = unit,
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
            AnimatedVisibility(
                visible = restVisible,
                enter = slideInVertically(
                    spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                ) { it } + fadeIn(),
                exit = slideOutVertically(
                    spring(stiffness = Spring.StiffnessMedium),
                ) { it } + fadeOut(),
            ) {
                RestTimerBar(
                    stateFlow = vm.restTimer,
                    onAddTime = { vm.addRestTime() },
                    onSkip = { vm.skipRest() },
                )
            }
        }
        if (showPicker) {
            ExercisePickerSheet(
                searchFlow = vm::searchExercises,
                onDismiss = { showPicker = false },
                onPick = { vm.addExercise(it.id) },
            )
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

    Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
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
                    Text(name, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        Format.dateTime(startedAt),
                        style = MaterialTheme.typography.bodySmall.numeric(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { editing = true }) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Rename",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onFinish,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text("Finish")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Discard workout") },
                        onClick = {
                            showMenu = false
                            confirmDiscard = true
                        },
                    )
                }
            }
        }
        if (confirmDiscard) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Discard this workout?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { confirmDiscard = false; onDiscard() }) {
                    Text("Discard", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun ExerciseBlock(
    modifier: Modifier = Modifier,
    item: WorkoutExerciseWithSets,
    unit: WeightUnit,
    vm: TrackerViewModel,
    previous: PreviousSet?,
) {
    androidx.compose.runtime.LaunchedEffect(item.exerciseDetails.id) {
        vm.loadPrevious(item.exerciseDetails.id)
    }
    var showMenu by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(item.exercise.notes.isNotBlank()) }
    var showPlateCalc by remember { mutableStateOf(false) }
    val sets = remember(item.sets) { item.sets.sortedBy { it.setIndex } }

    LedgerCard(modifier = modifier.fillMaxWidth()) {
        Column(
            Modifier
                .padding(18.dp)
                .animateContentSize(
                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                ),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.exerciseDetails.name, style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        VhTag(item.exerciseDetails.muscleGroup)
                        item.exercise.supersetId?.let { VhTag(Format.supersetLabel(it), accent = true) }
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                            text = { Text("Rest ${item.exercise.restSeconds ?: RestSeconds.DEFAULT}s") },
                            onClick = {
                                showMenu = false
                                vm.setRestSeconds(item.exercise.id, (item.exercise.restSeconds ?: RestSeconds.DEFAULT).let { if (it >= 240) 90 else it + 30 })
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
            }

            if (showNotes) {
                OutlinedTextField(
                    value = item.exercise.notes,
                    onValueChange = { vm.updateExerciseNotes(item.exercise.id, it) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    placeholder = { Text("Notes") },
                    minLines = 1,
                    maxLines = 3,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(bottom = 6.dp)) {
                TableLabel("SET", Modifier.width(32.dp))
                TableLabel("PREV", Modifier.weight(1.1f))
                TableLabel(Format.unitLabel(unit).uppercase(), Modifier.weight(1.15f))
                TableLabel("REPS", Modifier.weight(1f))
                TableLabel("RPE", Modifier.weight(0.85f))
                Spacer(Modifier.width(58.dp))
            }

            sets.forEachIndexed { i, set ->
                SetRow(
                    set = set,
                    index = i + 1,
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

            FilledTonalButton(
                onClick = { vm.addSet(item.exercise.id, sets.lastOrNull()) },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
}

@Composable
private fun TableLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
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
    var weightText by remember(set.id, unit) { mutableStateOf(if (set.weightKg == 0.0) "" else Format.weight(set.weightKg, unit)) }
    var repsText by remember(set.id, unit) { mutableStateOf(if (set.reps == 0) "" else set.reps.toString()) }
    var showRpeMenu by remember { mutableStateOf(false) }

    fun commit() {
        val displayWeight = weightText.toDoubleOrNull() ?: 0.0
        val kg = unit.displayToKg(displayWeight)
        val reps = repsText.toIntOrNull() ?: 0
        if (kg != set.weightKg || reps != set.reps) onWeightReps(kg, reps)
    }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(32.dp).clickable { onToggleWarmup() }, contentAlignment = Alignment.CenterStart) {
            if (set.isWarmup) {
                VhTag("W")
            } else {
                Text(
                    "$index",
                    style = MaterialTheme.typography.bodyMedium.numeric(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            if (previous == null || (previous.weightKg <= 0.0 && previous.reps <= 0)) "—"
            else "${Format.weight(previous.weightKg, unit)}×${previous.reps}",
            Modifier.weight(1.1f),
            style = MaterialTheme.typography.bodySmall.numeric(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        SetField(
            value = weightText,
            onValueChange = { weightText = Format.decimalFilter(it) },
            onCommit = ::commit,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1.15f).padding(horizontal = 4.dp),
        )
        SetField(
            value = repsText,
            onValueChange = { repsText = it.filter { c -> c.isDigit() } },
            onCommit = ::commit,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        )
        Box(Modifier.weight(0.85f), contentAlignment = Alignment.Center) {
            TextButton(onClick = { showRpeMenu = true }, contentPadding = PaddingValues(0.dp)) {
                Text(
                    set.rpe?.let { Format.rpe(it) } ?: "-",
                    style = MaterialTheme.typography.bodyMedium.numeric(),
                    color = if (set.rpe != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = showRpeMenu, onDismissRequest = { showRpeMenu = false }) {
                RPE_OPTIONS.forEach { rpe ->
                    DropdownMenuItem(
                        text = { Text(rpe?.let { Format.rpe(it) } ?: "None", style = MaterialTheme.typography.bodyMedium.numeric()) },
                        onClick = {
                            showRpeMenu = false
                            onSetRpe(rpe)
                        },
                    )
                }
            }
        }
        Row(Modifier.width(58.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            val checkScale by animateFloatAsState(
                targetValue = if (set.isDone) 1f else 0.86f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                label = "check-scale",
            )
            val checkColor by animateColorAsState(
                targetValue = if (set.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(200),
                label = "check-color",
            )
            IconButton(
                onClick = {
                    commit()
                    onToggleDone()
                },
                modifier = Modifier
                    .size(40.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(checkColor),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = if (set.isDone) "Mark not done" else "Mark done",
                    Modifier.size(16.dp),
                    tint = if (set.isDone) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(22.dp)) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Delete set",
                    Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun RestTimerBar(stateFlow: kotlinx.coroutines.flow.StateFlow<RestTimerState>, onAddTime: () -> Unit, onSkip: () -> Unit) {
    val state by stateFlow.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "REST",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(12.dp))
            AnimatedContent(
                targetState = state.remainingSeconds,
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "rest-countdown",
            ) { seconds ->
                Text(
                    Format.clock(seconds),
                    style = MaterialTheme.typography.headlineMedium.numeric(),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAddTime) { Text("+${RestSeconds.TIMER_ADD}s") }
            TextButton(onClick = onSkip) { Text("Skip") }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = {
                if (state.totalSeconds == 0) 0f
                else 1f - state.remainingSeconds.toFloat() / state.totalSeconds
            },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}


@Composable
private fun PlateCalculatorDialog(unit: WeightUnit, onDismiss: () -> Unit) {
    var targetText by remember { mutableStateOf("60") }
    val barKg = Calculators.BAR_KG
    val platesKg = Calculators.PLATES_KG

    val displayTarget = targetText.toDoubleOrNull() ?: 0.0
    val targetKg = unit.displayToKg(displayTarget)
    val plates = Calculators.platesFor(targetKg, barKg, platesKg)
    val loadableKg = Calculators.closestLoadable(targetKg, barKg, platesKg)

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Plate calculator") },
        text = {
            Column {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = Format.decimalFilter(it) },
                    label = { Text("Target weight (${Format.unitLabel(unit)})") },
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                if (targetKg <= barKg) {
                    Text("Target must be above the empty ${Format.weight(barKg, unit)} ${Format.unitLabel(unit)} bar.")
                } else {
                    Text(
                        "Per side: " + if (plates.isEmpty()) "nothing to add" else plates.joinToString(" + ") { Format.weight(it, unit) },
                        style = MaterialTheme.typography.bodyMedium.numeric(),
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
