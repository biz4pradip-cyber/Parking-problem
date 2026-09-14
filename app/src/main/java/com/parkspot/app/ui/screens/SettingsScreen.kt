package com.parkspot.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkspot.app.R
import com.parkspot.app.bluetooth.PairedDevice
import com.parkspot.app.ui.ParkingViewModel
import com.parkspot.app.ui.components.Hairline
import com.parkspot.app.ui.components.MinimalTopBar
import com.parkspot.app.ui.components.QuietAction
import com.parkspot.app.ui.components.ScreenPadding
import com.parkspot.app.ui.components.SectionLabel
import com.parkspot.app.util.BackgroundAccess

/**
 * Hands-free setup: pick the car's Bluetooth device once, and the app saves the spot by itself
 * every time that device disconnects.
 */
@Composable
fun SettingsScreen(
    viewModel: ParkingViewModel,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
) {
    val config by viewModel.autoPark.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-checked on resume, since the exemption is granted in system settings and returned from.
    var unrestricted by remember { mutableStateOf(BackgroundAccess.isUnrestricted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                unrestricted = BackgroundAccess.isUnrestricted(context)
                viewModel.refreshPairedDevices()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.refreshPairedDevices()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* The spot is still saved if denied; you just do not get told about it. */ }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MinimalTopBar(title = stringResource(R.string.settings), onBack = onBack)
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.auto_park_title),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.auto_park_summary),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(28.dp))
            }

            Hairline(modifier = Modifier.padding(horizontal = ScreenPadding))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.auto_park_enable),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = config.enabled,
                    onCheckedChange = { enabled ->
                        viewModel.setAutoParkEnabled(enabled)
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS,
                                )
                            }
                            viewModel.refreshPairedDevices()
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
            }

            Hairline(modifier = Modifier.padding(horizontal = ScreenPadding))

            if (config.enabled) {
                Section(title = stringResource(R.string.background_title)) {
                    Text(
                        text = stringResource(
                            if (unrestricted) R.string.background_ok else R.string.background_restricted,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!unrestricted) {
                        QuietAction(
                            text = stringResource(R.string.background_open_settings),
                            onClick = {
                                if (!BackgroundAccess.openBatteryOptimizationSettings(context)) {
                                    viewModel.show(R.string.background_no_settings)
                                }
                            },
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Hairline(modifier = Modifier.padding(horizontal = ScreenPadding))

                CarPicker(
                    devices = devices,
                    selectedAddress = config.deviceAddress,
                    hasPermission = viewModel.hasBluetoothPermission(),
                    bluetoothAvailable = viewModel.isBluetoothAvailable(),
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    },
                    onSelect = viewModel::setCar,
                )

                Column(modifier = Modifier.padding(horizontal = ScreenPadding)) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.auto_park_reconnect_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 20.dp),
    ) {
        SectionLabel(title)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@Composable
private fun CarPicker(
    devices: List<PairedDevice>,
    selectedAddress: String?,
    hasPermission: Boolean,
    bluetoothAvailable: Boolean,
    onRequestPermission: () -> Unit,
    onSelect: (PairedDevice) -> Unit,
) {
    Section(title = stringResource(R.string.auto_park_pick_car)) {
        when {
            !hasPermission -> {
                Text(
                    text = stringResource(R.string.auto_park_permission),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                QuietAction(
                    text = stringResource(R.string.auto_park_grant_bluetooth),
                    onClick = onRequestPermission,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            !bluetoothAvailable || devices.isEmpty() -> {
                Text(
                    text = stringResource(
                        if (bluetoothAvailable) {
                            R.string.auto_park_no_devices
                        } else {
                            R.string.auto_park_bluetooth_off
                        },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(device) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = device.address.equals(selectedAddress, ignoreCase = true),
                            onClick = { onSelect(device) },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = device.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            if (device.looksLikeCar) {
                                Text(
                                    text = stringResource(R.string.auto_park_likely_car),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
