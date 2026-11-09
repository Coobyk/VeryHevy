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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.veryhevy.ui.LedgerCard
import com.veryhevy.ui.SectionLabel
import com.veryhevy.ui.theme.numeric
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch


@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val unit by vm.unit.collectAsStateWithLifecycle()
    val defaultRest by vm.defaultRest.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
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
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        message?.let { msg ->
            LedgerCard(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(msg, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = vm::clearMessage) { Text("Dismiss") }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Units")
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
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Default rest timer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(60, 90, 120, 180).forEach { seconds ->
                    val selected = defaultRest == seconds
                    OutlinedButton(
                        onClick = { vm.setDefaultRest(seconds) },
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                    ) {
                        Text(
                            "${seconds}s",
                            style = MaterialTheme.typography.bodyMedium.numeric(),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Data")
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
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Import from Hevy")
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

        Text(
            "VeryHevy · free and open source workout tracker",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }
}
