package com.parkspot.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.parkspot.app.R

/** Shown when location access is missing. A rule and some text, rather than a coloured box. */
@Composable
fun PermissionNotice(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Hairline()
        Spacer(Modifier.height(16.dp))
        SectionLabel(stringResource(R.string.permission_title))
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        QuietAction(
            text = stringResource(R.string.grant_permission),
            onClick = onRequestPermission,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Hairline()
    }
}
