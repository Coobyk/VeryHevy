package com.veryhevy.ui.exercises

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.veryhevy.data.db.ExerciseEntity
import com.veryhevy.ui.LedgerCard
import com.veryhevy.ui.VhDivider
import com.veryhevy.ui.VhTag
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    searchFlow: (String) -> Flow<List<ExerciseEntity>>,
    onDismiss: () -> Unit,
    onPick: (ExerciseEntity) -> Unit,
    title: String = "Add exercise",
) {
    var query by remember { mutableStateOf("") }
    val results by remember(query) { searchFlow(query) }.collectAsStateWithLifecycle(initialValue = emptyList())

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search exercises") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
            )
            LazyColumn(Modifier.padding(vertical = 8.dp)) {
                items(results, key = { it.id }) { exercise ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onPick(exercise)
                                onDismiss()
                            },
                    ) {
                        VhDivider()
                        Column(Modifier.padding(vertical = 16.dp)) {
                            Text(exercise.name, style = MaterialTheme.typography.titleSmall)
                            Row(
                                Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                VhTag(exercise.muscleGroup)
                                VhTag(exercise.equipment.replace('_', ' '))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseLibraryCard(exercise: ExerciseEntity) {
    LedgerCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                VhTag(exercise.muscleGroup)
                VhTag(exercise.equipment.replace('_', ' '))
            }
            if (exercise.instructions.isNotBlank()) {
                Text(
                    exercise.instructions,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
