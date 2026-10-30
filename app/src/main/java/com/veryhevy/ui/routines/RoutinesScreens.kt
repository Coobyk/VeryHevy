package com.veryhevy.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import com.veryhevy.data.db.RoutineSetEntity
import com.veryhevy.ui.Format
import com.veryhevy.ui.exercises.ExercisePickerSheet

@Composable
fun RoutinesListScreen(vm: RoutinesViewModel, onOpenRoutine: (Long) -> Unit) {
    val routines by vm.routines.collectAsState()
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
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (routines.isEmpty()) {
                item {
                    Text(
                        "No routines yet. Create one to plan your workouts ahead of time.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(routines, key = { it.routine.id }) { r ->
                Card(
                    Modifier.fillMaxWidth().clickable { onOpenRoutine(r.routine.id) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(r.routine.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${r.exercises.size} exercises · updated ${Format.date(r.routine.updatedAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { vm.duplicateRoutine(r.routine.id) }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Duplicate")
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        androidx.compose.material3.AlertDialog(
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
    val routine by vm.current.collectAsState()
    val unit by vm.unit.collectAsState()
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
            Row(Modifier.padding(horizontal = 16.dp)) {
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
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(r.exercises.sortedBy { it.exercise.orderIndex }, key = { it.exercise.id }) { item ->
                    RoutineExerciseCard(
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
            searchFlow = vm.exerciseRepository::search,
            onDismiss = { showPicker = false },
            onPick = { vm.addExercise(routineId, it.id) },
        )
    }
}

@Composable
private fun RoutineExerciseCard(
    item: com.veryhevy.data.db.RoutineExerciseWithSets,
    unit: WeightUnit,
    vm: RoutinesViewModel,
) {
    var showMenu by remember { mutableStateOf(false) }
    val sets = item.sets.sortedBy { it.setIndex }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.exerciseDetails.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${item.exerciseDetails.muscleGroup.replaceFirstChar { it.uppercase() }} · ${item.exerciseDetails.equipment.replace('_', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { showMenu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "Options") }
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
                        text = { Text("Rest ${item.exercise.restSeconds ?: 90}s") },
                        onClick = {
                            showMenu = false
                            vm.updateRestSeconds(
                                item.exercise.id,
                                item.exercise.routineId,
                                (item.exercise.restSeconds ?: 90).let { if (it >= 240) 90 else it + 30 },
                            )
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Remove exercise") },
                        onClick = { showMenu = false; vm.removeExercise(item.exercise.routineId, item.exercise.orderIndex) },
                    )
                }
            }

            Row(Modifier.padding(vertical = 4.dp)) {
                Text("SET", Modifier.width(36.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("KG" + if (unit == WeightUnit.LB) "/LB" else "", Modifier.weight(1.4f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("REPS", Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("RPE", Modifier.weight(0.9f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("", Modifier.weight(0.7f))
            }

            sets.forEach { set ->
                RoutineSetRow(
                    set = set,
                    index = sets.indexOf(set) + 1,
                    unit = unit,
                    onWeightReps = { w, r -> vm.updateSetValues(set, w, r) },
                    onRpe = { vm.setRpe(set, it) },
                    onWarmup = { vm.toggleWarmup(set) },
                    onDelete = { vm.deleteSet(set) },
                )
            }

            FilledTonalButton(
                onClick = { vm.addSet(item.exercise.id, sets.lastOrNull()) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }
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
    var weightText by remember(set.id) { mutableStateOf(if (set.weightKg == 0.0) "" else Format.weight(set.weightKg, unit)) }
    var repsText by remember(set.id) { mutableStateOf(if (set.reps == 0) "" else set.reps.toString()) }
    var showRpeMenu by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    var weightWasFocused by remember { mutableStateOf(false) }
    var repsWasFocused by remember { mutableStateOf(false) }

    fun commit() {
        val kg = Format.displayToKg(weightText.toDoubleOrNull() ?: 0.0, unit)
        val reps = repsText.toIntOrNull() ?: 0
        if (kg != set.weightKg || reps != set.reps) onWeightReps(kg, reps)
    }

    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(if (set.isWarmup) "W$index" else "$index", Modifier.width(36.dp), style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' }; commit() },
            modifier = Modifier.weight(1.4f).padding(end = 4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = repsText,
            onValueChange = { repsText = it.filter { c -> c.isDigit() }; commit() },
            modifier = Modifier.weight(1.2f).padding(end = 4.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyMedium,
        )
        androidx.compose.foundation.layout.Box(Modifier.weight(0.9f)) {
            TextButton(onClick = { showRpeMenu = true }) {
                Text(set.rpe?.let { "%.1f".format(it) } ?: "-", style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(expanded = showRpeMenu, onDismissRequest = { showRpeMenu = false }) {
                listOf(null, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0).forEach { rpe ->
                    DropdownMenuItem(
                        text = { Text(rpe?.let { "%.1f".format(it) } ?: "None") },
                        onClick = { showRpeMenu = false; onRpe(rpe) },
                    )
                }
            }
        }
        IconButton(onClick = onWarmup, Modifier.size(24.dp)) {
            Icon(Icons.Filled.Edit, contentDescription = "Toggle warmup", Modifier.size(14.dp))
        }
        IconButton(onClick = onDelete, Modifier.size(24.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Delete set", Modifier.size(14.dp))
        }
    }
}
