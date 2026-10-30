package com.veryhevy.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvParserTest {

    @Test
    fun parse_basicQuotedFields() {
        val rows = CsvParser.parse(
            "Date,\"Workout Name\",Weight\n2024-03-05,\"Bench Press, heavy\",100\n2024-03-05,Squat,80"
        )
        assertEquals(3, rows.size)
        assertEquals(listOf("Date", "Workout Name", "Weight"), rows[0])
        assertEquals("Bench Press, heavy", rows[1][1])
        assertEquals("Squat", rows[2][1])
        assertEquals("100", rows[1][2])
    }

    @Test
    fun parse_escapedQuotesAndNewlines() {
        val rows = CsvParser.parse("a,\"say \"\"hi\"\"\",c\nd,\"multi\nline\",f")
        assertEquals("say \"hi\"", rows[0][1])
        assertEquals("multi\nline", rows[1][1])
    }

    @Test
    fun parseDate_iso() {
        assertNotNull(CsvParser.parseDate("2024-03-05 18:32:00"))
        assertNotNull(CsvParser.parseDate("2024-03-05"))
        assertNotNull(CsvParser.parseDate("05/03/2024"))
        assertNull(CsvParser.parseDate("not a date"))
    }

    @Test
    fun matchExercise_exactAndFuzzy() {
        val lib = listOf(
            com.veryhevy.data.db.ExerciseEntity(id = "bench", name = "Barbell Bench Press", muscleGroup = "chest", equipment = "barbell"),
            com.veryhevy.data.db.ExerciseEntity(id = "squat", name = "Barbell Back Squat", muscleGroup = "quads", equipment = "barbell"),
        )
        assertEquals("bench", CsvParser.matchExercise("Barbell Bench Press", lib))
        assertEquals("bench", CsvParser.matchExercise("bench press", lib))
        assertEquals("squat", CsvParser.matchExercise("Back Squat (Barbell)", lib))
        assertNull(CsvParser.matchExercise("Totally Unrelated Zzz", lib))
        assertTrue(CsvParser.similarity(CsvParser.normalize("bench press"), CsvParser.normalize("Bench Press")) > 0.9)
    }
}
