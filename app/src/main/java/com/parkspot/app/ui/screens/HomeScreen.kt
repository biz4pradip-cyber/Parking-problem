package com.parkspot.app.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalParking
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.parkspot.app.R
import com.parkspot.app.data.ParkingSpot
import com.parkspot.app.ui.CapturePhotoContract
import com.parkspot.app.ui.MapsLauncher
import com.parkspot.app.ui.ParkingViewModel
import com.parkspot.app.ui.components.PermissionCard
import com.parkspot.app.ui.components.SpotDetailsSheet
import com.parkspot.app.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ParkingViewModel,
    snackbarHostState: SnackbarHostState,
    onFindMyCar: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val state by viewModel.homeState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spot = state.activeSpot

    var showDetailsSheet by rememberSaveable { mutableStateOf(false) }
    var showPhotoViewer by rememberSaveable { mutableStateOf(false) }
    var pendingPhotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionChanged(granted)
        if (granted) viewModel.saveCurrentSpot()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* The reminder is stored either way; it just stays silent if denied. */ }

    val takePictureLauncher = rememberLauncherForActivityResult(
        CapturePhotoContract(),
    ) { success ->
        val uri = pendingPhotoUri
        if (success && uri != null && spot != null) {
            viewModel.onPhotoCaptured(spot, uri)
        } else {
            viewModel.onPhotoCaptureCancelled(uri)
        }
        pendingPhotoUri = null
    }

    val requestLocation = {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )
    }

    val takePhoto = {
        val uri = viewModel.newPhotoUri()
        pendingPhotoUri = uri
        try {
            takePictureLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingPhotoUri = null
            viewModel.show(R.string.error_no_camera_app)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = stringResource(R.string.history),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    Button(
                        onClick = {
                            if (state.locationPermissionGranted) {
                                viewModel.saveCurrentSpot()
                            } else {
                                requestLocation()
                            }
                        },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Text(
                                text = stringResource(R.string.saving_spot),
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        } else {
                            Icon(Icons.Rounded.LocalParking, contentDescription = null)
                            Text(
                                text = stringResource(
                                    if (spot == null) R.string.save_spot else R.string.save_spot_again,
                                ),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.locationPermissionGranted) {
                PermissionCard(onRequestPermission = requestLocation)
            }

            if (spot == null) {
                EmptyState()
            } else {
                ParkedCard(
                    spot = spot,
                    parkedFor = state.parkedFor ?: 0L,
                    reminderIn = state.reminderIn,
                    onFindMyCar = onFindMyCar,
                    onOpenInMaps = {
                        if (!MapsLauncher.openInMaps(context, spot)) {
                            viewModel.show(R.string.error_no_maps_app)
                        }
                    },
                    onShare = {
                        if (!MapsLauncher.share(context, spot)) {
                            viewModel.show(R.string.error_no_maps_app)
                        }
                    },
                    onEdit = { showDetailsSheet = true },
                    onFound = viewModel::markFound,
                    onSetReminder = { minutes ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(
                                Manifest.permission.POST_NOTIFICATIONS,
                            )
                        }
                        viewModel.setReminderInMinutes(spot, minutes)
                    },
                    onClearReminder = { viewModel.clearReminder(spot) },
                    onOpenPhoto = { showPhotoViewer = true },
                )
            }
        }
    }

    if (showDetailsSheet && spot != null) {
        SpotDetailsSheet(
            spot = spot,
            onDismiss = { showDetailsSheet = false },
            onSave = { level, label, note ->
                viewModel.saveDetails(spot, level, label, note)
                showDetailsSheet = false
            },
            onTakePhoto = takePhoto,
            onRemovePhoto = { viewModel.removePhoto(spot) },
        )
    }

    val activePhotoUri = spot?.photoUri
    if (showPhotoViewer && activePhotoUri != null) {
        PhotoViewerDialog(
            photoUri = activePhotoUri,
            onDismiss = { showPhotoViewer = false },
        )
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocalParking,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(R.string.no_spot_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.no_spot_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParkedCard(
    spot: ParkingSpot,
    parkedFor: Long,
    reminderIn: Long?,
    onFindMyCar: () -> Unit,
    onOpenInMaps: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onFound: () -> Unit,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
    onOpenPhoto: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.parked_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = "${Formatters.clockTime(spot.savedAt)} · ${Formatters.duration(parkedFor)} ago",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (spot.label.isNotBlank()) {
                Text(text = spot.label, style = MaterialTheme.typography.titleMedium)
            }
            if (spot.note.isNotBlank()) {
                Text(text = spot.note, style = MaterialTheme.typography.bodyLarge)
            }

            spot.photoUri?.let { uri ->
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenPhoto),
                )
            }

            Text(
                text = "${Formatters.coordinates(spot.latitude, spot.longitude)} · " +
                    Formatters.accuracy(spot.accuracyMeters),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ReminderRow(
                reminderIn = reminderIn,
                onSetReminder = onSetReminder,
                onClearReminder = onClearReminder,
            )

            Button(
                onClick = onFindMyCar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Icon(Icons.Rounded.DirectionsWalk, contentDescription = null)
                Text(
                    text = stringResource(R.string.find_my_car),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenInMaps, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Map, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.open_in_maps))
                }
                OutlinedButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = stringResource(R.string.share_location),
                    )
                }
                OutlinedButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = stringResource(R.string.edit_details),
                    )
                }
            }

            TextButton(
                onClick = onFound,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(stringResource(R.string.found_my_car))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderRow(
    reminderIn: Long?,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
) {
    if (reminderIn != null) {
        AssistChip(
            onClick = onClearReminder,
            label = { Text("${stringResource(R.string.reminder)} ${Formatters.countdown(reminderIn)}") },
            leadingIcon = { Icon(Icons.Rounded.Alarm, contentDescription = null) },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.reminder_clear),
                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                )
            },
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.reminder_set),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30L to "30 min", 60L to "1 h", 120L to "2 h").forEach { (minutes, label) ->
                    SuggestionChip(
                        onClick = { onSetReminder(minutes) },
                        label = { Text(label) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoViewerDialog(photoUri: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onDismiss),
        ) {
            AsyncImage(
                model = photoUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
