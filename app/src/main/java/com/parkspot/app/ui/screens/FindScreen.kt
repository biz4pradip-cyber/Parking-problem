package com.parkspot.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.parkspot.app.R
import com.parkspot.app.ui.MapsLauncher
import com.parkspot.app.ui.ParkingViewModel
import com.parkspot.app.ui.components.CompassDial
import com.parkspot.app.ui.components.PermissionCard
import com.parkspot.app.util.Formatters
import com.parkspot.app.util.GeoUtils

/**
 * Live "walk this way" screen: a pointer to the car, the distance left, and the notes and photo
 * from when it was parked — which is what actually finds the car on the last few metres, where GPS
 * gives up.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FindScreen(
    viewModel: ParkingViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    val state by viewModel.findState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val spot = state.spot

    // Walking to the car with the screen switching off every 15 seconds is miserable.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.find_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (spot == null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.no_spot_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }
            return@Scaffold
        }

        val distance = state.distanceMeters
        val bearing = state.bearingToCar

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CompassDial(
                rotationDegrees = state.arrowRotation,
                headline = when {
                    distance == null -> "–"
                    state.hasArrived -> "0 m"
                    else -> Formatters.distance(distance)
                },
                subline = when {
                    distance == null -> stringResource(R.string.waiting_for_gps)
                    state.hasArrived -> stringResource(R.string.you_are_here)
                    state.arrowRotation != null -> stringResource(R.string.walk_this_way)
                    bearing != null -> stringResource(R.string.direction_of_you, GeoUtils.compassPoint(bearing))
                    else -> stringResource(R.string.waiting_for_gps)
                },
                active = !state.hasArrived,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (!state.locationPermissionGranted) {
                PermissionCard(onRequestPermission = onRequestPermission)
            }

            state.currentLocation?.let { here ->
                Text(
                    text = stringResource(R.string.accuracy_format, "${here.accuracy.toInt()} m"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!state.hasCompass) {
                HintCard(text = stringResource(R.string.no_compass))
            } else if (state.compass?.needsCalibration == true) {
                HintCard(text = stringResource(R.string.calibrate_compass))
            }

            if (state.hasArrived) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = stringResource(R.string.you_are_here),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                }
            }

            if (spot.hasDetails || spot.photoUri != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
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
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        if (!MapsLauncher.openInMaps(context, spot)) {
                            viewModel.show(R.string.error_no_maps_app)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Map, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text(stringResource(R.string.open_in_maps))
                }
            }

            Button(
                onClick = {
                    viewModel.markFound()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                Text(stringResource(R.string.found_my_car))
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Explore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}
