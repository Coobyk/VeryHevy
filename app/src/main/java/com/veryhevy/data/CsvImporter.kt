package com.veryhevy.data

import android.content.Context
import android.net.Uri
import com.veryhevy.data.db.SetEntity
import com.veryhevy.domain.Calculators
import com.veryhevy.data.db.WorkoutEntity
import com.veryhevy.data.db.WorkoutExerciseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Imports Hevy's CSV export. Rows are grouped into workouts by
 * (date, workout name); weights are assumed to be in the given [unit].
 */
class CsvImporter(private val context: Context, private val db: com.veryhevy.data.db.AppDatabase) {

    suspend fun importFrom(uri: Uri, unit: WeightUnit): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use {
                it.bufferedReader().readText()
            } ?: error("Could not open CSV file")

            val lines = CsvParser.parse(text)
            if (lines.isEmpty()) error("CSV file is empty")
            val header = lines.first()
            fun col(name: String): Int =
                header.indexOfFirst { it.trim().equals(name, ignoreCase = true) }

            val iDate = col("Date").also { if (it < 0) error("Missing 'Date' column — is this a Hevy export?") }
            val iName = col("Workout Name")
            val iExercise = col("Exercise Name").also { if (it < 0) error("Missing 'Exercise Name' column") }
            val iSetOrder = col("Set Order")
            val iWeight = col("Weight")
            val iReps = col("Reps")
            val iRpe = col("RPE")
            val iNotes = col("Notes")
            val iWorkoutNotes = col("Workout Notes")

            val workoutsByKey = LinkedHashMap<Pair<String, String>, MutableList<CsvRow>>()
            for (line in lines.drop(1)) {
                val date = line.getOrNull(iDate)?.trim() ?: continue
                val workoutName = line.getOrNull(iName)?.trim().orEmpty()
                val key = date to workoutName
                workoutsByKey.getOrPut(key) { mutableListOf() }
                    .add(CsvRow(line, iExercise, iSetOrder, iWeight, iReps, iRpe, iNotes, iWorkoutNotes))
            }

            var imported = 0
            for ((key, rows) in workoutsByKey) {
                val (dateText, name) = key
                val startedAt = CsvParser.parseDate(dateText) ?: System.currentTimeMillis()
                val workoutNotes = rows.firstNotNullOfOrNull { it.workoutNotes() }.orEmpty()
                val workoutId = db.workoutDao().insert(
                    WorkoutEntity(name = name.ifBlank { "Imported workout" }, startedAt = startedAt, endedAt = startedAt, notes = workoutNotes)
                )
                var order = 0
                for ((exerciseName, exerciseRows) in rows.groupBy { it.exerciseName() }) {
                    val exerciseId = CsvParser.matchExercise(exerciseName, db.exerciseDao().all())
                        ?: "custom-csv-${exerciseName.hashCode().toLong() and 0x7FFFFFFFL}"
                    if (exerciseId.startsWith("custom-csv-")) {
                        db.exerciseDao().upsert(
                            com.veryhevy.data.db.ExerciseEntity(
                                id = exerciseId,
                                name = exerciseName,
                                muscleGroup = "unknown",
                                equipment = "other",
                                isCustom = true,
                            )
                        )
                    }
                    val weId = db.workoutDao().insertExercise(
                        WorkoutExerciseEntity(workoutId = workoutId, exerciseId = exerciseId, orderIndex = order++)
                    )
                    db.workoutDao().insertSets(
                        exerciseRows.mapIndexedNotNull { index, row ->
                            val reps = row.reps()?.toIntOrNull() ?: return@mapIndexedNotNull null
                            val weightDisplay = row.weight()?.toDoubleOrNull() ?: 0.0
                            val kg = if (unit == WeightUnit.LB) Calculators.lbToKg(weightDisplay) else weightDisplay
                            SetEntity(
                                workoutExerciseId = weId,
                                setIndex = row.setOrder()?.toIntOrNull() ?: index,
                                weightKg = kg,
                                reps = reps,
                                rpe = row.rpe()?.toDoubleOrNull(),
                                isDone = true,
                                completedAt = startedAt,
                            )
                        }
                    )
                }
                imported++
            }
            imported
        }
    }
}

private data class CsvRow(
    val cells: List<String>,
    val iExercise: Int,
    val iSetOrder: Int,
    val iWeight: Int,
    val iReps: Int,
    val iRpe: Int,
    val iNotes: Int,
    val iWorkoutNotes: Int,
) {
    fun exerciseName() = cells.getOrNull(iExercise)?.trim().orEmpty()
    fun setOrder() = cells.getOrNull(iSetOrder)?.trim()?.ifBlank { null }
    fun weight() = cells.getOrNull(iWeight)?.trim()?.ifBlank { null }
    fun reps() = cells.getOrNull(iReps)?.trim()?.ifBlank { null }
    fun rpe() = cells.getOrNull(iRpe)?.trim()?.ifBlank { null }
    fun notes() = cells.getOrNull(iNotes)?.trim()?.ifBlank { null }
    fun workoutNotes() = cells.getOrNull(iWorkoutNotes)?.trim()?.ifBlank { null }
}

object CsvParser {
    /** RFC-4180-ish CSV split handling quoted fields with commas, newlines and escaped quotes. */
    fun parse(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var field = StringBuilder()
        var row = mutableListOf<String>()
        var inQuotes = false
        var i = 0
        val n = text.length
        while (i < n) {
            val c = text[i]
            when {
                inQuotes -> {
                    if (c == '"') {
                        if (i + 1 < n && text[i + 1] == '"') {
                            field.append('"')
                            i++
                        } else {
                            inQuotes = false
                        }
                    } else {
                        field.append(c)
                    }
                }
                c == '"' -> inQuotes = true
                c == ',' -> {
                    row.add(field.toString())
                    field = StringBuilder()
                }
                c == '\n' || c == '\r' -> {
                    if (field.isNotEmpty() || row.isNotEmpty()) {
                        row.add(field.toString())
                        rows.add(row)
                        row = mutableListOf()
                        field = StringBuilder()
                    }
                    if (c == '\r' && i + 1 < n && text[i + 1] == '\n') i++
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row.add(field.toString())
            rows.add(row)
        }
        return rows
    }

    /** Parses Hevy's date format, e.g. "2024-03-05 18:32:00" or "2024-03-05" or "03/05/2024". */
    fun parseDate(value: String): Long? {
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
            "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm", "dd/MM/yyyy",
            "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm", "MM/dd/yyyy",
        )
        for (pattern in formats) {
            try {
                val fmt = java.text.SimpleDateFormat(pattern, java.util.Locale.US)
                fmt.isLenient = false
                val date = fmt.parse(value.trim()) ?: continue
                return date.time
            } catch (_: Exception) {
            }
        }
        return null
    }

    /** Finds the closest matching library exercise by name (case/punctuation insensitive). */
    fun matchExercise(name: String, library: List<com.veryhevy.data.db.ExerciseEntity>): String? {
        val norm = normalize(name)
        if (norm.isEmpty()) return null
        library.firstOrNull { normalize(it.name) == norm }?.let { return it.id }
        return library
            .map { it to similarity(norm, normalize(it.name)) }
            .maxByOrNull { it.second }
            ?.takeIf { it.second >= 0.6 }
            ?.first?.id
    }

    fun normalize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    /** Simple bigram similarity in [0, 1]. */
    fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length < 2 || b.length < 2) return 0.0
        val bigramsA = a.windowed(2).groupBy { it }.mapValues { it.value.size }
        val bigramsB = b.windowed(2).groupBy { it }.mapValues { it.value.size }
        val intersection = bigramsA.entries.sumOf { (k, v) -> minOf(v, bigramsB.getOrDefault(k, 0)) }
        val total = bigramsA.values.sum() + bigramsB.values.sum()
        return if (total == 0) 0.0 else 2.0 * intersection / total
    }
}
