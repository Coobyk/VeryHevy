package com.veryhevy.ui.settings

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.veryhevy.VeryHevyApp
import com.veryhevy.data.BackupManager
import com.veryhevy.data.CsvImporter
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
        .stateIn(viewModelScope, SharingStarted.Eagerly, 90)

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

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val unit by vm.unit.collectAsState()
    val defaultRest by vm.defaultRest.collectAsState()
    val message by vm.message.collectAsState()
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(vm::exportBackup) }

    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(vm::importBackup) }

    val importCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { vm.importCsv(it, unit) } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        message?.let { msg ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Row(Modifier.padding(12.dp)) {
                    Text(msg, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = vm::clearMessage) { Text("Dismiss") }
                }
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Units", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = unit == WeightUnit.KG,
                        onClick = { vm.setUnit(WeightUnit.KG) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text("Kilograms") }
                    SegmentedButton(
                        selected = unit == WeightUnit.LB,
                        onClick = { vm.setUnit(WeightUnit.LB) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text("Pounds") }
                }
                HorizontalDivider()
                Text("Default rest timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 90, 120, 180).forEach { seconds ->
                        OutlinedButton(
                            onClick = { vm.setDefaultRest(seconds) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                "${seconds}s",
                                fontWeight = if (defaultRest == seconds) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Back up everything (workouts, routines, measurements, custom exercises) or import your Hevy history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = { exportLauncher.launch("veryhevy-backup.json") },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Export backup") }
                OutlinedButton(
                    onClick = {
                        importBackupLauncher.launch(arrayOf("application/json", "application/octet-stream", "text/plain"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Import backup (replaces data)") }
                HorizontalDivider()
                Text("Import from Hevy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "In Hevy, go to Settings → Data → Export data, then pick the CSV file here. Weights are read in your selected unit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = {
                        importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain", "application/octet-stream"))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Import Hevy CSV") }
            }
        }

        Text(
            "VeryHevy · free and open source workout tracker",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
