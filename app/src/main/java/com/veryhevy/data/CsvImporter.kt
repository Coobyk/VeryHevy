package com.veryhevy.data

import android.content.Context
import android.net.Uri
import com.veryhevy.data.db.AppDatabase
import com.veryhevy.data.db.ExerciseEntity
import com.veryhevy.data.db.SetEntity
import com.veryhevy.data.db.WorkoutEntity
import com.veryhevy.data.db.WorkoutExerciseEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Reads a content URI into a String, or null if it can't be opened. */
internal fun Context.readUriText(uri: Uri): String? =
    contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }

/**
 * Imports Hevy's CSV export. Rows are grouped into workouts by
 * (date, workout name); legacy exports' weights are read in [unit],
 * the current format always exports kilograms.
 */
class CsvImporter(private val context: Context, private val db: AppDatabase) {

    private val workoutDao get() = db.workoutDao()
    private val exerciseDao get() = db.exerciseDao()

    suspend fun importFrom(uri: Uri, unit: WeightUnit): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.readUriText(uri) ?: error("Could not open CSV file")
            val lines = CsvParser.parse(text)
            if (lines.isEmpty()) error("CSV file is empty")
            val header = lines.first()
            fun col(name: String): Int =
                header.indexOfFirst { it.trim().equals(name, ignoreCase = true) }

            // The exercise library is fetched once and grows as imports create customs.
            val library = exerciseDao.all().toMutableList()

            // Hevy's current export uses exercise_title/weight_kg/set_type;
            // older exports use Exercise Name/Weight/Set Order.
            if (col("exercise_title") >= 0) importV2(lines, ::col, library)
            else importLegacy(lines, ::col, unit, library)
        }
    }

    /** Match a name to the library, creating and registering a custom exercise on no match. */
    private suspend fun resolveExerciseId(name: String, library: MutableList<ExerciseEntity>): String {
        CsvParser.matchExercise(name, library)?.let { return it }
        val id = "custom-csv-${name.hashCode().toLong() and 0x7FFFFFFFL}"
        if (library.none { it.id == id }) {
            val entity = ExerciseEntity(
                id = id,
                name = name,
                muscleGroup = "unknown",
                equipment = "other",
                isCustom = true,
            )
            exerciseDao.upsert(entity)
            library += entity
        }
        return id
    }

    private fun importedSet(
        weId: Long,
        setIndex: Int,
        weightKg: Double,
        reps: Int,
        rpe: Double?,
        isWarmup: Boolean,
        completedAt: Long,
    ) = SetEntity(
        workoutExerciseId = weId,
        setIndex = setIndex,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        isWarmup = isWarmup,
        isDone = true,
        completedAt = completedAt,
    )

    /** Legacy export: Date,Workout Name,Exercise Name,Set Order,Weight,Reps,RPE,Notes,... */
    private suspend fun importLegacy(
        lines: List<List<String>>,
        col: (String) -> Int,
        unit: WeightUnit,
        library: MutableList<ExerciseEntity>,
    ): Int {
        val iDate = col("Date").also { if (it < 0) error("Missing 'Date' column — is this a Hevy export?") }
        val iName = col("Workout Name")
        val iExercise = col("Exercise Name").also { if (it < 0) error("Missing 'Exercise Name' column") }
        val iSetOrder = col("Set Order")
        val iWeight = col("Weight")
        val iReps = col("Reps")
        val iRpe = col("RPE")
        val iNotes = col("Notes")
        val iWorkoutNotes = col("Workout Notes")

        fun List<String>.cell(i: Int) = getOrNull(i)?.trim()?.ifBlank { null }

        val workoutsByKey = LinkedHashMap<Pair<String, String>, MutableList<List<String>>>()
        for (line in lines.drop(1)) {
            val date = line.cell(iDate) ?: continue
            workoutsByKey.getOrPut(date to line.cell(iName).orEmpty()) { mutableListOf() }.add(line)
        }

        var imported = 0
        for ((key, rows) in workoutsByKey) {
            val (dateText, name) = key
            val startedAt = CsvParser.parseDate(dateText) ?: continue // skip workouts with unparseable dates
            val workoutId = workoutDao.insert(
                WorkoutEntity(
                    name = name.ifBlank { "Imported workout" },
                    startedAt = startedAt,
                    endedAt = startedAt,
                    notes = rows.firstNotNullOfOrNull { it.cell(iWorkoutNotes) }.orEmpty(),
                )
            )
            var order = 0
            for ((exerciseName, exerciseRows) in rows.groupBy { it.cell(iExercise).orEmpty() }) {
                if (exerciseName.isBlank()) continue
                val exerciseId = resolveExerciseId(exerciseName, library)
                val weId = workoutDao.insertExercise(
                    WorkoutExerciseEntity(
                        workoutId = workoutId,
                        exerciseId = exerciseId,
                        orderIndex = order++,
                        notes = exerciseRows.firstNotNullOfOrNull { it.cell(iNotes) }.orEmpty(),
                    )
                )
                workoutDao.insertSets(
                    exerciseRows.mapIndexedNotNull { index, row ->
                        val reps = row.cell(iReps)?.toIntOrNull() ?: return@mapIndexedNotNull null
                        importedSet(
                            weId = weId,
                            setIndex = row.cell(iSetOrder)?.toIntOrNull() ?: index,
                            weightKg = unit.displayToKg(row.cell(iWeight)?.toDoubleOrNull() ?: 0.0),
                            reps = reps,
                            rpe = row.cell(iRpe)?.toDoubleOrNull(),
                            isWarmup = false,
                            completedAt = startedAt,
                        )
                    }
                )
            }
            imported++
        }
        return imported
    }

    /** Current export: title,start_time,end_time,description,exercise_title,
     *  superset_id,exercise_notes,set_index,set_type,weight_kg,reps,distance_km,
     *  duration_seconds,rpe. Weights are always kilograms. */
    private suspend fun importV2(
        lines: List<List<String>>,
        col: (String) -> Int,
        library: MutableList<ExerciseEntity>,
    ): Int {
        val iTitle = col("title")
        val iStart = col("start_time")
        val iEnd = col("end_time")
        val iDesc = col("description")
        val iExercise = col("exercise_title")
        val iSuperset = col("superset_id")
        val iExerciseNotes = col("exercise_notes")
        val iSetIndex = col("set_index")
        val iSetType = col("set_type")
        val iWeight = col("weight_kg")
        val iReps = col("reps")
        val iRpe = col("rpe")

        fun List<String>.cell(i: Int) = getOrNull(i)?.trim()?.ifBlank { null }

        val workoutsByKey = LinkedHashMap<Pair<String, String>, MutableList<List<String>>>()
        for (line in lines.drop(1)) {
            val start = line.cell(iStart) ?: continue
            workoutsByKey.getOrPut(start to line.cell(iTitle).orEmpty()) { mutableListOf() }.add(line)
        }

        var imported = 0
        for ((key, rows) in workoutsByKey) {
            val (startText, name) = key
            val startedAt = CsvParser.parseDate(startText) ?: continue // skip workouts with unparseable dates
            val endedAt = rows.firstNotNullOfOrNull { it.cell(iEnd) }
                ?.let { CsvParser.parseDate(it) } ?: startedAt
            val workoutId = workoutDao.insert(
                WorkoutEntity(
                    name = name.ifBlank { "Imported workout" },
                    startedAt = startedAt,
                    endedAt = endedAt,
                    notes = rows.firstNotNullOfOrNull { it.cell(iDesc) }.orEmpty(),
                )
            )

            val supersetIds = rows.mapNotNull { it.cell(iSuperset) }.distinct()
            var order = 0
            for ((exerciseName, exerciseRows) in rows.groupBy { it.cell(iExercise).orEmpty() }) {
                if (exerciseName.isBlank()) continue
                val exerciseId = resolveExerciseId(exerciseName, library)
                val superset = exerciseRows.firstNotNullOfOrNull { it.cell(iSuperset) }
                    ?.let { supersetIds.indexOf(it) + 1 }
                val weId = workoutDao.insertExercise(
                    WorkoutExerciseEntity(
                        workoutId = workoutId,
                        exerciseId = exerciseId,
                        orderIndex = order++,
                        notes = exerciseRows.firstNotNullOfOrNull { it.cell(iExerciseNotes) }.orEmpty(),
                        supersetId = superset,
                    )
                )
                workoutDao.insertSets(
                    exerciseRows.mapIndexedNotNull { index, row ->
                        val reps = row.cell(iReps)?.toIntOrNull() ?: return@mapIndexedNotNull null
                        importedSet(
                            weId = weId,
                            setIndex = row.cell(iSetIndex)?.toIntOrNull() ?: index,
                            weightKg = row.cell(iWeight)?.toDoubleOrNull() ?: 0.0,
                            reps = reps,
                            rpe = row.cell(iRpe)?.toDoubleOrNull(),
                            isWarmup = row.cell(iSetType).equals("warmup", ignoreCase = true),
                            completedAt = startedAt,
                        )
                    }
                )
            }
            imported++
        }
        return imported
    }
}

object CsvParser {

    private const val MATCH_THRESHOLD = 0.6

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

    private val dateFormats = listOf(
        "d MMM yyyy, HH:mm", "d MMM yyyy, H:mm", "d MMM yyyy",
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd",
        "dd/MM/yyyy HH:mm:ss", "dd/MM/yyyy HH:mm", "dd/MM/yyyy",
        "MM/dd/yyyy HH:mm:ss", "MM/dd/yyyy HH:mm", "MM/dd/yyyy",
    ).map {
        java.text.SimpleDateFormat(it, java.util.Locale.US).apply { isLenient = false }
    }

    /** Parses Hevy's date formats, e.g. "7 Nov 2026, 16:37" or "2024-03-05 18:32:00". */
    fun parseDate(value: String): Long? {
        val trimmed = value.trim()
        for (fmt in dateFormats) {
            try {
                return fmt.parse(trimmed)?.time ?: continue
            } catch (_: Exception) {
            }
        }
        return null
    }

    private val equipmentWords = setOf(
        "machine", "cable", "dumbbell", "barbell", "smith", "band", "kettlebell",
    )
    private val synonyms = mapOf("overhead" to "shoulder")

    /** Significant words of an exercise name — drops equipment modifiers and stopwords. */
    private fun contentTokens(s: String): Set<String> =
        normalize(s).split(" ")
            .filter { it.length > 1 }
            .map { synonyms[it] ?: it }
            .filter { it !in equipmentWords }
            .toSet()

    /** Finds the closest matching library exercise by name (case/punctuation insensitive).
     *  A fuzzy match is only accepted when every content word of the input appears in the
     *  candidate — otherwise the name becomes a custom exercise instead of being attached
     *  to the wrong library entry (e.g. "Calf Press" → "Machine Chest Press"). */
    fun matchExercise(name: String, library: List<ExerciseEntity>): String? {
        val norm = normalize(name)
        if (norm.isEmpty()) return null
        library.firstOrNull { normalize(it.name) == norm }?.let { return it.id }
        val queryTokens = contentTokens(name)
        if (queryTokens.isEmpty()) return null
        return library
            .map { it to similarity(norm, normalize(it.name)) }
            .filter { (e, score) -> score >= MATCH_THRESHOLD && contentTokens(e.name).containsAll(queryTokens) }
            .maxByOrNull { it.second }
            ?.first?.id
    }

    private fun normalize(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    /** Simple bigram similarity in [0, 1]. */
    private fun similarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.length < 2 || b.length < 2) return 0.0
        val bigramsA = a.windowed(2).groupBy { it }.mapValues { it.value.size }
        val bigramsB = b.windowed(2).groupBy { it }.mapValues { it.value.size }
        val intersection = bigramsA.entries.sumOf { (k, v) -> minOf(v, bigramsB.getOrDefault(k, 0)) }
        val total = bigramsA.values.sum() + bigramsB.values.sum()
        return if (total == 0) 0.0 else 2.0 * intersection / total
    }
}
