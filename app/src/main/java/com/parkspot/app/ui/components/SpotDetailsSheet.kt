package com.parkspot.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.parkspot.app.R
import com.parkspot.app.data.ParkingSpot

/** Bottom sheet for the "where exactly did I leave it" details: level, bay, note and photo. */
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

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.edit_details),
                style = MaterialTheme.typography.headlineSmall,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text(stringResource(R.string.level_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = spotLabel,
                    onValueChange = { spotLabel = it },
                    label = { Text(stringResource(R.string.spot_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.note_hint)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )

            if (spot.photoUri != null) {
                AsyncImage(
                    model = spot.photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.PhotoCamera, contentDescription = null)
                        Text(
                            text = stringResource(R.string.retake_photo),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                    TextButton(onClick = onRemovePhoto) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                        Text(
                            text = stringResource(R.string.remove_photo),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            } else {
                OutlinedButton(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.AddAPhoto, contentDescription = null)
                    Text(
                        text = stringResource(R.string.add_photo),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                Button(
                    onClick = { onSave(level, spotLabel, note) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
