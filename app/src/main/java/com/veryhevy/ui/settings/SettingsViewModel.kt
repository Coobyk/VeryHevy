package com.veryhevy.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veryhevy.VeryHevyApp
import com.veryhevy.data.BackupManager
import com.veryhevy.data.CsvImporter
import com.veryhevy.data.RestSeconds
import com.veryhevy.data.WeightUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as VeryHevyApp
    private val settings = container.settingsRepository
    private val backup = BackupManager(container, container.database)
    private val csv = CsvImporter(container, container.database)

    val unit: StateFlow<WeightUnit> = settings.settings
        .map { it.unit }
        .stateIn(viewModelScope, SharingStarted.Eagerly, WeightUnit.KG)

    val defaultRest: StateFlow<Int> = settings.settings
        .map { it.defaultRestSeconds }
        .stateIn(viewModelScope, SharingStarted.Eagerly, RestSeconds.DEFAULT)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setUnit(unit: WeightUnit) {
        viewModelScope.launch { settings.setUnit(unit) }
    }

    fun setDefaultRest(seconds: Int) {
        viewModelScope.launch { settings.setDefaultRestSeconds(seconds) }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            backup.exportTo(uri)
                .onSuccess { _message.value = "Backup exported ($it workouts)." }
                .onFailure { _message.value = "Export failed: ${it.message}" }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            backup.importFrom(uri)
                .onSuccess { _message.value = "Backup imported ($it workouts)." }
                .onFailure { _message.value = "Import failed: ${it.message}" }
        }
    }

    fun importCsv(uri: Uri, unit: WeightUnit) {
        viewModelScope.launch {
            csv.importFrom(uri, unit)
                .onSuccess { _message.value = "Hevy CSV imported ($it workouts)." }
                .onFailure { _message.value = "CSV import failed: ${it.message}" }
        }
    }
}
