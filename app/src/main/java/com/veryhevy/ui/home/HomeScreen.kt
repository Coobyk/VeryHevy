package com.veryhevy.ui.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veryhevy.data.WeightUnit
import com.veryhevy.ui.Format
import com.veryhevy.ui.LedgerCard
import com.veryhevy.ui.NumStat
import com.veryhevy.ui.SectionLabel
import com.veryhevy.ui.VhDivider
import com.veryhevy.ui.theme.numeric

@Composable
fun HomeScreen(vm: HomeViewModel) {
    val unit by vm.unit.collectAsStateWithLifecycle()
    val weekly by vm.weeklyVolume.collectAsStateWithLifecycle()
    val muscles by vm.muscleVolume.collectAsStateWithLifecycle()
    val records by vm.personalRecords.collectAsStateWithLifecycle()
    val names by vm.exerciseNames.collectAsStateWithLifecycle()
    val workoutCount by vm.workoutCount.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val measurements by vm.measurements.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
    ) {
        item {
            LedgerCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp)) {
                    NumStat("$workoutCount", "Workouts", Modifier.weight(1f))
                    NumStat("$streak", "Week streak", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        item { VolumeSection(weekly, unit) }
        item { MuscleSplitSection(muscles, unit) }
        item {
            MeasurementsSection(
                measurements = measurements,
                unit = unit,
                onAdd = { weightKg -> vm.addMeasurement(weightKg) },
                onDelete = { vm.deleteMeasurement(it) },
            )
        }
        item { SectionLabel("Personal records", Modifier.padding(top = 8.dp)) }
        if (records.isEmpty()) {
            item {
                Text(
                    "Complete sets in your workouts to see records here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        items(records, key = { it.exerciseId }) { pr ->
            PRRow(pr, names[pr.exerciseId] ?: pr.exerciseId, unit)
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun VolumeSection(weekly: List<com.veryhevy.domain.WeeklyVolume>, unit: WeightUnit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SectionLabel("Weekly volume")
        Spacer(Modifier.height(4.dp))
        Text(
            "Last ${weekly.size} weeks · ${Format.unitLabel(unit)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        val max = weekly.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0
        val barColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val slot = size.width / weekly.size
            val barWidth = slot * 0.55f
            weekly.forEachIndexed { i, week ->
                val h = (week.volumeKg / max * size.height).toFloat().coerceAtLeast(3f)
                val x = slot * i + (slot - barWidth) / 2f
                drawRect(color = trackColor, topLeft = Offset(x, 0f), size = Size(barWidth, size.height))
                drawRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - h),
                    size = Size(barWidth, h),
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            weekly.take(3).forEach { week ->
                Text(
                    "${week.weekStart.monthValue}/${week.weekStart.dayOfMonth}",
                    style = MaterialTheme.typography.labelSmall.numeric(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MuscleSplitSection(muscles: List<com.veryhevy.domain.MuscleVolume>, unit: WeightUnit) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        SectionLabel("Volume by muscle")
        Spacer(Modifier.height(12.dp))
        if (muscles.isEmpty()) {
            Text(
                "No training volume recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }
        val max = muscles.maxOfOrNull { it.volumeKg }?.takeIf { it > 0 } ?: 1.0
        val barColor = MaterialTheme.colorScheme.primary
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        muscles.forEachIndexed { i, m ->
            if (i > 0) Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    m.muscleGroup.replaceFirstChar { it.uppercase() },
                    Modifier.width(84.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                Box(Modifier.weight(1f).height(6.dp)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(color = trackColor)
                        drawRect(
                            color = barColor,
                            size = Size(
                                (m.volumeKg / max * size.width).toFloat().coerceAtLeast(3f),
                                size.height,
                            ),
                        )
                    }
                }
                Text(
                    Format.volume(m.volumeKg, unit),
                    Modifier.width(56.dp),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.bodySmall.numeric(),
                )
            }
        }
    }
}

@Composable
private fun MeasurementsSection(
    measurements: List<com.veryhevy.data.db.MeasurementEntity>,
    unit: WeightUnit,
    onAdd: (Double) -> Unit,
    onDelete: (com.veryhevy.data.db.MeasurementEntity) -> Unit,
) {
    var showAdd by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Body weight", Modifier.weight(1f))
            TextButton(onClick = { showAdd = true }) { Text("Add") }
        }
        Spacer(Modifier.height(4.dp))
        val latest = measurements.firstOrNull()
        if (latest == null) {
            Text(
                "Log your body weight to track progress.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    Format.weight(latest.weightKg, unit),
                    style = MaterialTheme.typography.displaySmall.numeric(),
                )
                Text(
                    " ${Format.unitLabel(unit)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    Format.date(latest.dateEpochDay * 86_400_000L),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            MiniWeightChart(measurements.takeLast(12).map { Format.weightKgToDisplay(it.weightKg, unit) })
            Spacer(Modifier.height(8.dp))
            measurements.take(5).forEach { m ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                    Text(
                        "${Format.weight(m.weightKg, unit)} ${Format.unitLabel(unit)}",
                        style = MaterialTheme.typography.bodySmall.numeric(),
                    )
                    Text(
                        "  ·  ${Format.date(m.dateEpochDay * 86_400_000L)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onDelete(m) }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = "Delete",
                            Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
    val dotColor = MaterialTheme.colorScheme.primary
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
                    strokeWidth = 5f,
                )
            }
            drawCircle(color = dotColor, radius = 5f, center = Offset(x, y))
        }
    }
}

@Composable
private fun PRRow(pr: com.veryhevy.domain.PersonalRecord, exerciseName: String, unit: WeightUnit) {
    LedgerCard(Modifier.padding(top = 10.dp).fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(exerciseName, style = MaterialTheme.typography.titleSmall)
                Text(
                    Format.date(pr.achievedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Format.weight(pr.bestOneRepMaxKg, unit),
                    style = MaterialTheme.typography.titleLarge.numeric(),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "est. 1RM · ${Format.weight(pr.bestWeightKg, unit)} ${Format.unitLabel(unit)} × ${pr.bestReps}",
                    style = MaterialTheme.typography.labelSmall.numeric(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
