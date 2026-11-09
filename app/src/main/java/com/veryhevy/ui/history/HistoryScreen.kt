package com.veryhevy.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.veryhevy.data.WeightUnit
import com.veryhevy.ui.Format
import com.veryhevy.ui.LedgerCard
import com.veryhevy.ui.SectionLabel
import com.veryhevy.ui.VhDivider
import com.veryhevy.ui.theme.numeric
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    onOpenWorkout: (Long) -> Unit,
) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val unit by vm.unit.collectAsStateWithLifecycle()

    val workoutDays by vm.workoutDays.collectAsStateWithLifecycle()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { CalendarSection(workoutDays) }
        item { SectionLabel("Workouts") }
        if (entries.isEmpty()) {
            item {
                Text(
                    "No workouts yet. Start training from the Training tab.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        items(entries, key = { it.workoutId }, contentType = { "workout" }) { entry ->
            HistoryRow(entry, unit, onClick = { onOpenWorkout(entry.workoutId) })
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun CalendarSection(workoutDays: Set<LocalDate>) {
    val today = remember { LocalDate.now() }
    val month = remember { YearMonth.from(today) }
    val firstDayOffset = month.atDay(1).dayOfWeek.value % 7 // Monday-based grid starting Sunday
    val daysInMonth = month.lengthOfMonth()

    LedgerCard(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                "${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}",
                Modifier.weight(1f),
            )
            Text(
                "${workoutDays.count { it.month == month.month }} sessions",
                style = MaterialTheme.typography.labelSmall.numeric(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                Text(
                    d,
                    Modifier.width(36.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                repeat(7) { col ->
                    val cell = row * 7 + col
                    val day = cell - firstDayOffset + 1
                    Box(Modifier.width(36.dp).height(30.dp), contentAlignment = Alignment.Center) {
                        if (day in 1..daysInMonth) {
                            val date = month.atDay(day)
                            val hasWorkout = date in workoutDays
                            val isToday = date == today
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        when {
                                            hasWorkout -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .then(
                                        if (isToday && !hasWorkout) Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline,
                                            RoundedCornerShape(5.dp),
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$day",
                                    style = MaterialTheme.typography.labelSmall.numeric(),
                                    color = if (hasWorkout) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry, unit: WeightUnit, onClick: () -> Unit) {
    LedgerCard(Modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    Format.dateTime(entry.startedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                InlineStat("${entry.exerciseCount}", "exercises")
                InlineStat("${entry.setCount}", "sets")
                InlineStat("${Format.volume(entry.totalVolumeKg, unit)} ${Format.unitLabel(unit)}", "volume")
                InlineStat(Format.duration(entry.startedAt, entry.endedAt), "time")
            }
        }
    }
}

@Composable
private fun InlineStat(value: String, label: String) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(value, style = MaterialTheme.typography.bodyMedium.numeric(), fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
