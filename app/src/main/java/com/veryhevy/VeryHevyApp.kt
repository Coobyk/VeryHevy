package com.veryhevy

import android.app.Application
import com.veryhevy.data.SettingsRepository
import com.veryhevy.data.db.AppDatabase

class VeryHevyApp : Application() {

    lateinit var database: AppDatabase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.build(this)
        settingsRepository = SettingsRepository(this)
    }
}
