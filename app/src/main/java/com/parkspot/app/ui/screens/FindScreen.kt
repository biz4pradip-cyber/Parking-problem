package com.parkspot.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.parkspot.app.R
import com.parkspot.app.ui.MapsLauncher
import com.parkspot.app.ui.ParkingViewModel
import com.parkspot.app.ui.components.CompassDial
import com.parkspot.app.ui.components.Hairline
import com.parkspot.app.ui.components.MinimalTopBar
import com.parkspot.app.ui.components.PermissionNotice
import com.parkspot.app.ui.components.PrimaryAction
import com.parkspot.app.ui.components.QuietAction
import com.parkspot.app.ui.components.ScreenPadding
import com.parkspot.app.ui.components.SectionLabel
import com.parkspot.app.util.Formatters
import com.parkspot.app.util.GeoUtils

/**
 * Live "walk this way": a pointer, the distance left, and the notes and photo from when the car
 * was parked — which is what actually finds it once GPS gives up.
 */
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
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MinimalTopBar(title = stringResource(R.string.find_title), onBack = onBack)
        },
        bottomBar = {
            if (spot != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = ScreenPadding),
                ) {
                    PrimaryAction(
                        text = stringResource(R.string.found_my_car),
                        onClick = {
                            viewModel.markFound()
                            onBack()
                        },
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        QuietAction(
                            text = stringResource(R.string.open_in_maps),
                            onClick = {
                                if (!MapsLauncher.openInMaps(context, spot)) {
                                    viewModel.show(R.string.error_no_maps_app)
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (spot == null) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding, vertical = 56.dp),
            ) {
                Text(
                    text = stringResource(R.string.no_spot_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
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
                .padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!state.locationPermissionGranted) {
                Spacer(Modifier.height(16.dp))
                PermissionNotice(onRequestPermission = onRequestPermission)
            }

            Spacer(Modifier.height(24.dp))
            CompassDial(
                rotationDegrees = state.arrowRotation,
                headline = when {
                    distance == null -> "—"
                    state.hasArrived -> "0 m"
                    else -> Formatters.distance(distance)
                },
                subline = when {
                    distance == null -> stringResource(R.string.waiting_for_gps)
                    state.hasArrived -> stringResource(R.string.you_are_here)
                    state.arrowRotation != null -> stringResource(R.string.walk_this_way)
                    bearing != null -> stringResource(
                        R.string.direction_of_you,
                        GeoUtils.compassPoint(bearing),
                    )
                    else -> stringResource(R.string.waiting_for_gps)
                },
                active = !state.hasArrived,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            state.currentLocation?.let { here ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.accuracy_format, "${here.accuracy.toInt()} m"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val hint = when {
                !state.hasCompass -> stringResource(R.string.no_compass)
                state.compass?.needsCalibration == true -> stringResource(R.string.calibrate_compass)
                else -> null
            }
            if (hint != null) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (spot.hasDetails || spot.photoUri != null) {
                Spacer(Modifier.height(28.dp))
                Hairline()
                Spacer(Modifier.height(20.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(stringResource(R.string.where_exactly))
                    Spacer(Modifier.height(10.dp))
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
                                .clip(MaterialTheme.shapes.medium),
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
