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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.parkspot.app.ui.components.Hairline
import com.parkspot.app.ui.components.MinimalTopBar
import com.parkspot.app.ui.components.PermissionNotice
import com.parkspot.app.ui.components.PrimaryAction
import com.parkspot.app.ui.components.QuietAction
import com.parkspot.app.ui.components.ScreenPadding
import com.parkspot.app.ui.components.SectionLabel
import com.parkspot.app.ui.components.SpotDetailsSheet
import com.parkspot.app.util.Formatters

@Composable
fun HomeScreen(
    viewModel: ParkingViewModel,
    snackbarHostState: SnackbarHostState,
    onFindMyCar: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
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

    val park = {
        if (state.locationPermissionGranted) viewModel.saveCurrentSpot() else requestLocation()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MinimalTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            imageVector = Icons.Rounded.History,
                            contentDescription = stringResource(R.string.history),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
        bottomBar = {
            HomeActions(
                isParked = spot != null,
                isSaving = state.isSaving,
                onPark = park,
                onFindMyCar = onFindMyCar,
                onOpenInMaps = {
                    if (spot != null && !MapsLauncher.openInMaps(context, spot)) {
                        viewModel.show(R.string.error_no_maps_app)
                    }
                },
                onShare = {
                    if (spot != null && !MapsLauncher.share(context, spot)) {
                        viewModel.show(R.string.error_no_maps_app)
                    }
                },
                onEdit = { showDetailsSheet = true },
                onFound = viewModel::markFound,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
        ) {
            if (!state.locationPermissionGranted) {
                Spacer(Modifier.height(16.dp))
                PermissionNotice(onRequestPermission = requestLocation)
            }

            if (spot == null) {
                EmptyState()
            } else {
                ParkedState(
                    spot = spot,
                    parkedFor = state.parkedFor ?: 0L,
                    reminderIn = state.reminderIn,
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

            Spacer(Modifier.height(32.dp))
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
    Column(modifier = Modifier.padding(top = 56.dp)) {
        Text(
            text = stringResource(R.string.no_spot_title),
            style = MaterialTheme.typography.displayMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_spot_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ParkedState(
    spot: ParkingSpot,
    parkedFor: Long,
    reminderIn: Long?,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit,
    onOpenPhoto: () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 40.dp)) {
        SectionLabel(stringResource(R.string.parked_title))
        Spacer(Modifier.height(10.dp))
        Text(
            text = Formatters.duration(parkedFor),
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(
                R.string.parked_since,
                Formatters.clockTime(spot.savedAt),
                Formatters.accuracy(spot.accuracyMeters),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = Formatters.coordinates(spot.latitude, spot.longitude),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (spot.savedAutomatically) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DirectionsCar,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.saved_automatically),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (spot.hasDetails || spot.photoUri != null) {
            Spacer(Modifier.height(28.dp))
            Hairline()
            Spacer(Modifier.height(20.dp))
            if (spot.label.isNotBlank()) {
                Text(
                    text = spot.label,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(6.dp))
            }
            if (spot.note.isNotBlank()) {
                Text(
                    text = spot.note,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            spot.photoUri?.let { uri ->
                Spacer(Modifier.height(16.dp))
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClick = onOpenPhoto),
                )
            }
        }

        Spacer(Modifier.height(28.dp))
        Hairline()
        Spacer(Modifier.height(20.dp))
        SectionLabel(stringResource(R.string.reminder))
        Spacer(Modifier.height(4.dp))
        if (reminderIn != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = Formatters.countdown(reminderIn),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                QuietAction(
                    text = stringResource(R.string.reminder_clear),
                    onClick = onClearReminder,
                )
            }
        } else {
            Row(modifier = Modifier.padding(start = 0.dp)) {
                listOf(30L to "30 min", 60L to "1 h", 120L to "2 h").forEach { (minutes, label) ->
                    QuietAction(
                        text = label,
                        onClick = { onSetReminder(minutes) },
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

/**
 * The action area. One filled button for the thing you came to do, everything else as plain text
 * so nothing competes with it.
 */
@Composable
private fun HomeActions(
    isParked: Boolean,
    isSaving: Boolean,
    onPark: () -> Unit,
    onFindMyCar: () -> Unit,
    onOpenInMaps: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onFound: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = ScreenPadding),
    ) {
        Hairline()
        Spacer(Modifier.height(16.dp))

        if (isParked) {
            PrimaryAction(
                text = stringResource(R.string.find_my_car),
                onClick = onFindMyCar,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuietAction(stringResource(R.string.open_in_maps_short), onOpenInMaps)
                QuietAction(stringResource(R.string.share_location), onShare)
                QuietAction(stringResource(R.string.edit_details_short), onEdit)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                QuietAction(stringResource(R.string.found_my_car), onFound)
                QuietAction(
                    text = stringResource(R.string.save_spot_again),
                    onClick = onPark,
                    enabled = !isSaving,
                )
            }
        } else {
            PrimaryAction(
                text = stringResource(
                    if (isSaving) R.string.saving_spot else R.string.save_spot,
                ),
                onClick = onPark,
                enabled = !isSaving,
                loading = isSaving,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PhotoViewerDialog(photoUri: String, onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
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
