package com.veryhevy.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.veryhevy.data.WeightUnit
import com.veryhevy.ui.Format

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val unit by vm.unit.collectAsState()
    val weekly by vm.weeklyVolume.collectAsState()
    val muscles by vm.muscleVolume.collectAsState()
    val records by vm.personalRecords.collectAsState()
    val names by vm.exerciseNames.collectAsState()
    val workoutCount by vm.workoutCount.collectAsState()
    val streak by vm.streak.collectAsState()
    val measurements by vm.measurements.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Workouts", "$workoutCount", Modifier.weight(1f))
                StatCard("Weekly streak", "$streak wk", Modifier.weight(1f))
            }
        }
        item { VolumeChartCard(weekly, unit) }
        item { MuscleSplitCard(muscles, unit) }
        item { MeasurementsCard(measurements.map { it.weightKg }, unit, onAdd = { weightKg -> vm.addMeasurement(weightKg) }, measurements = measurements, onDelete = { vm.deleteMeasurement(it) }, vm = vm) }
        item {
            Text("Personal records", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        if (records.isEmpty()) {
            item {
                Text(
                    "Complete sets in your workouts to see records here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        items(records, key = { it.exerciseId }) { pr ->
            PRCard(pr, names[pr.exerciseId] ?: pr.exerciseId, unit)
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun VolumeChartCard(weekly: List<com.veryhevy.domain.WeeklyVolume>, unit: WeightUnit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text("Weekly volume", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Last ${weekly.size} weeks · ${Format.unitLabel(unit)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            val max = weekly.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0
            val barColor = MaterialTheme.colorScheme.primary
            Canvas(Modifier.fillMaxWidth().height(120.dp)) {
                val barWidth = size.width / (weekly.size * 2f)
                weekly.forEachIndexed { i, week ->
                    val h = (week.volumeKg / max * size.height).toFloat()
                    val x = barWidth * (2 * i + 0.5f)
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                    )
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekly.take(3).forEach { week ->
                    Text(
                        "${week.weekStart.monthValue}/${week.weekStart.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleSplitCard(muscles: List<com.veryhevy.domain.MuscleVolume>, unit: WeightUnit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Text("Volume by muscle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            if (muscles.isEmpty()) {
                Text(
                    "No training volume recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val max = muscles.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0
            val barColor = MaterialTheme.colorScheme.primary
            muscles.forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        m.muscleGroup.replaceFirstChar { it.uppercase() },
                        Modifier.width(90.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Canvas(Modifier.weight(1f).height(14.dp)) {
                        drawRoundRect(
                            color = barColor,
                            size = Size((m.volumeKg / max * size.width).toFloat().coerceAtLeast(4f), size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f),
                        )
                    }
                    Text(
                        Format.volume(m.volumeKg, unit),
                        Modifier.width(56.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun MeasurementsCard(
    weightsKg: List<Double>,
    unit: WeightUnit,
    measurements: List<com.veryhevy.data.db.MeasurementEntity>,
    onAdd: (Double) -> Unit,
    onDelete: (com.veryhevy.data.db.MeasurementEntity) -> Unit,
    vm: HomeViewModel,
) {
    var showAdd by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Body weight",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { showAdd = true }) { Text("Add") }
            }
            val latest = measurements.firstOrNull()
            if (latest == null) {
                Text(
                    "Log your body weight to track progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "Latest: ${Format.weight(latest.weightKg, unit)} ${Format.unitLabel(unit)} · ${Format.date(latest.dateEpochDay * 86_400_000L)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                MiniWeightChart(measurements.takeLast(12).map { Format.weightKgToDisplay(it.weightKg, unit) })
                Spacer(Modifier.height(8.dp))
                measurements.take(5).forEach { m ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${Format.weight(m.weightKg, unit)} ${Format.unitLabel(unit)} · ${Format.date(m.dateEpochDay * 86_400_000L)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { onDelete(m) }, modifier = Modifier.height(24.dp)) {
                            Icon(Icons.Filled.Close, contentDescription = "Delete", Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Add body weight (${Format.unitLabel(unit)})") },
            text = {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() || c == '.' } },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    input.toDoubleOrNull()?.let { v ->
                        onAdd(Format.displayToKg(v, unit))
                        input = ""
                        showAdd = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MiniWeightChart(values: List<Double>) {
    if (values.size < 2) return
    val min = values.min()
    val max = values.max()
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxWidth().height(48.dp)) {
        val range = (max - min).takeIf { it > 0 } ?: 1.0
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1f)
            val y = (size.height * (1 - (v - min) / range)).toFloat()
            if (i > 0) {
                val prevX = size.width * (i - 1) / (values.size - 1f)
                val prevY = (size.height * (1 - (values[i - 1] - min) / range)).toFloat()
                drawLine(
                    color = lineColor,
                    start = Offset(prevX, prevY),
                    end = Offset(x, y),
                    strokeWidth = 6f,
                )
            }
        }
    }
}

@Composable
private fun PRCard(pr: com.veryhevy.domain.PersonalRecord, exerciseName: String, unit: WeightUnit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exerciseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "Achieved ${Format.date(pr.achievedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${Format.weight(pr.bestOneRepMaxKg, unit)} ${Format.unitLabel(unit)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "est. 1RM · ${Format.weight(pr.bestWeightKg, unit)} × ${pr.bestReps}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
