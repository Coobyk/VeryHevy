package com.veryhevy.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veryhevy.data.RestSeconds
import com.veryhevy.data.WeightUnit
import com.veryhevy.data.db.RoutineSetEntity
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
fun RoutinesListScreen(vm: RoutinesViewModel, onOpenRoutine: (Long) -> Unit) {
    val routines by vm.routines.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("New routine")
            }
        },
    ) { padding ->
        if (routines.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.EventNote,
                title = "No routines yet",
                body = "Create a routine to plan your workouts ahead of time and start them with one tap.",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(routines, key = { it.routine.id }) { r ->
                    LedgerCard(Modifier.fillMaxWidth().clickable { onOpenRoutine(r.routine.id) }) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(r.routine.name, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    "${r.exercises.size} exercises · updated ${Format.date(r.routine.updatedAt)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { vm.duplicateRoutine(r.routine.id) }) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Duplicate",
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { vm.deleteRoutine(r.routine.id) }) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = "Delete",
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New routine") },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        showCreate = false
                        vm.createRoutine(newName.trim()) { id ->
                            newName = ""
                            onOpenRoutine(id)
                        }
                    }
                }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineEditScreen(
    routineId: Long,
    vm: RoutinesViewModel,
    onBack: () -> Unit,
    onWorkoutStarted: (Long) -> Unit,
) {
    val routine by vm.current.collectAsStateWithLifecycle()
    val unit by vm.unit.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    var editingName by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(routineId) {
        vm.loadRoutine(routineId)
    }

    val r = routine
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (editingName) {
                        OutlinedTextField(
                            value = r?.routine?.name ?: "",
                            onValueChange = { vm.renameRoutine(routineId, it) },
                            singleLine = true,
                        )
                    } else {
                        Text(
                            r?.routine?.name ?: "",
                            modifier = Modifier.clickable { editingName = true },
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (editingName) {
                        IconButton(onClick = { editingName = false }) { Icon(Icons.Filled.Check, contentDescription = "Done") }
                    }
                },
            )
        },
    ) { padding ->
        if (r == null) return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Button(
                    onClick = { vm.startWorkout(routineId, onWorkoutStarted) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Start workout")
                }
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(r.exercises.sortedBy { it.exercise.orderIndex }, key = { it.exercise.id }) { item ->
                    RoutineExerciseBlock(
                        item = item,
                        unit = unit,
                        vm = vm,
                    )
                }
                item {
                    OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add exercise")
                    }
                }
            }
        }
    }

    if (showPicker) {
        ExercisePickerSheet(
            searchFlow = vm::searchExercises,
            onDismiss = { showPicker = false },
            onPick = { vm.addExercise(routineId, it.id) },
        )
    }
}

@Composable
private fun RoutineExerciseBlock(
    item: com.veryhevy.data.db.RoutineExerciseWithSets,
    unit: WeightUnit,
    vm: RoutinesViewModel,
) {
    var showMenu by remember { mutableStateOf(false) }
    val sets = remember(item.sets) { item.sets.sortedBy { it.setIndex } }

    LedgerCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.exerciseDetails.name, style = MaterialTheme.typography.titleMedium)
                    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VhTag(item.exerciseDetails.muscleGroup)
                        VhTag(item.exerciseDetails.equipment.replace('_', ' '))
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
                            text = { Text("Move up") },
                            onClick = { showMenu = false; vm.moveExercise(item.exercise.routineId, item.exercise.orderIndex, -1) },
                        )
                        DropdownMenuItem(
                            text = { Text("Move down") },
                            onClick = { showMenu = false; vm.moveExercise(item.exercise.routineId, item.exercise.orderIndex, 1) },
                        )
                        DropdownMenuItem(
                            text = { Text("Rest ${item.exercise.restSeconds ?: RestSeconds.DEFAULT}s") },
                            onClick = {
                                showMenu = false
                                vm.updateRestSeconds(
                                    item.exercise.id,
                                    RestSeconds.nextAfter(item.exercise.restSeconds ?: RestSeconds.DEFAULT),
                                )
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove exercise") },
                            onClick = { showMenu = false; vm.removeExercise(item.exercise.routineId, item.exercise.orderIndex) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(Modifier.padding(bottom = 6.dp)) {
                RoutineTableLabel("SET", Modifier.width(32.dp))
                RoutineTableLabel(Format.unitLabel(unit).uppercase(), Modifier.weight(1.3f))
                RoutineTableLabel("REPS", Modifier.weight(1.1f))
                RoutineTableLabel("RPE", Modifier.weight(0.9f))
                Spacer(Modifier.width(46.dp))
            }

            sets.forEachIndexed { i, set ->
                RoutineSetRow(
                    set = set,
                    index = i + 1,
                    unit = unit,
                    onWeightReps = { w, r -> vm.updateSetValues(set, w, r) },
                    onRpe = { vm.setRpe(set, it) },
                    onWarmup = { vm.toggleWarmup(set) },
                    onDelete = { vm.deleteSet(set) },
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
private fun RoutineTableLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun RoutineSetRow(
    set: RoutineSetEntity,
    index: Int,
    unit: WeightUnit,
    onWeightReps: (Double, Int) -> Unit,
    onRpe: (Double?) -> Unit,
    onWarmup: () -> Unit,
    onDelete: () -> Unit,
) {
    var weightText by remember(set.id, unit) { mutableStateOf(if (set.weightKg == 0.0) "" else Format.weight(set.weightKg, unit)) }
    var repsText by remember(set.id, unit) { mutableStateOf(if (set.reps == 0) "" else set.reps.toString()) }
    var showRpeMenu by remember { mutableStateOf(false) }

    fun commit() {
        val kg = unit.displayToKg(weightText.toDoubleOrNull() ?: 0.0)
        val reps = repsText.toIntOrNull() ?: 0
        if (kg != set.weightKg || reps != set.reps) onWeightReps(kg, reps)
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(32.dp), contentAlignment = Alignment.CenterStart) {
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
        SetField(
            value = weightText,
            onValueChange = { weightText = Format.decimalFilter(it); commit() },
            onCommit = ::commit,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1.3f).padding(horizontal = 4.dp),
        )
        SetField(
            value = repsText,
            onValueChange = { repsText = it.filter { c -> c.isDigit() }; commit() },
            onCommit = ::commit,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1.1f).padding(horizontal = 4.dp),
        )
        Box(Modifier.weight(0.9f), contentAlignment = Alignment.Center) {
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
                        onClick = { showRpeMenu = false; onRpe(rpe) },
                    )
                }
            }
        }
        Row(Modifier.width(46.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onWarmup, Modifier.size(24.dp)) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Toggle warmup",
                    Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete, Modifier.size(22.dp)) {
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
