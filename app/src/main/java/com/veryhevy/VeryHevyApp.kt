package com.veryhevy

import android.app.Application
import com.veryhevy.data.ExerciseRepository
import com.veryhevy.data.SettingsRepository
import com.veryhevy.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VeryHevyApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var exerciseRepository: ExerciseRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
        settingsRepository = SettingsRepository(this)
        exerciseRepository = ExerciseRepository(database, this)
        CoroutineScope(Dispatchers.Default).launch {
            exerciseRepository.seedIfEmpty()
        }
    }
}
