package com.veryhevy.data

import android.content.Context
import com.veryhevy.data.db.AppDatabase
import com.veryhevy.data.db.ExerciseEntity
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ExerciseRepository(
    private val db: AppDatabase,
    private val context: Context,
) {
    fun observeAll() = db.exerciseDao().observeAll()
    fun search(query: String) = db.exerciseDao().search(query)
    suspend fun byId(id: String) = db.exerciseDao().byId(id)
    suspend fun all(): List<ExerciseEntity> = db.exerciseDao().all()

    suspend fun addCustom(name: String, muscleGroup: String, equipment: String) {
        val id = "custom-${System.currentTimeMillis()}"
        db.exerciseDao().upsert(
            ExerciseEntity(
                id = id,
                name = name,
                muscleGroup = muscleGroup,
                equipment = equipment,
                isCustom = true,
            )
        )
    }

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (db.exerciseDao().count() > 0) return@withContext
        val json = context.assets.open("exercises.json").bufferedReader().use { it.readText() }
        val array = JSONObject("{items: $json}").getJSONArray("items")
        val exercises = (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            ExerciseEntity(
                id = o.getString("id"),
                name = o.getString("name"),
                muscleGroup = o.getString("muscleGroup"),
                secondaryMuscles = o.optString("secondaryMuscles", ""),
                equipment = o.getString("equipment"),
                instructions = o.optString("instructions", ""),
            )
        }
        db.exerciseDao().insertAll(exercises)
    }
}
