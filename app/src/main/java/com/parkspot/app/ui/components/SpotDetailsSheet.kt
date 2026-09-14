package com.parkspot.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.parkspot.app.R
import com.parkspot.app.data.ParkingSpot

/** Level, bay, note and photo — the things that actually find the car on the last few metres. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotDetailsSheet(
    spot: ParkingSpot,
    onDismiss: () -> Unit,
    onSave: (level: String, spotLabel: String, note: String) -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var level by rememberSaveable(spot.id) { mutableStateOf(spot.level) }
    var spotLabel by rememberSaveable(spot.id) { mutableStateOf(spot.spotLabel) }
    var note by rememberSaveable(spot.id) { mutableStateOf(spot.note) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPadding)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            SectionLabel(stringResource(R.string.edit_details))
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text(stringResource(R.string.level_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = quietFieldColors(),
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = spotLabel,
                    onValueChange = { spotLabel = it },
                    label = { Text(stringResource(R.string.spot_hint)) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    colors = quietFieldColors(),
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_hint)) },
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
                colors = quietFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            if (spot.photoUri != null) {
                Spacer(Modifier.height(16.dp))
                AsyncImage(
                    model = spot.photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(MaterialTheme.shapes.medium),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    QuietAction(stringResource(R.string.retake_photo), onTakePhoto)
                    QuietAction(stringResource(R.string.remove_photo), onRemovePhoto)
                }
            } else {
                QuietAction(
                    text = stringResource(R.string.add_photo),
                    onClick = onTakePhoto,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(20.dp))
            PrimaryAction(
                text = stringResource(R.string.save),
                onClick = { onSave(level, spotLabel, note) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                QuietAction(stringResource(R.string.cancel), onDismiss)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun quietFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
