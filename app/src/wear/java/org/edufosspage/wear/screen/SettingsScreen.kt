package org.edufosspage.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.rememberScalingLazyListState
import org.edufosspage.R
import org.edufosspage.wear.SectionHeader

/** Watch settings hub, mirroring the phone app's Preferences section. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText          = { TimeText() },
        positionIndicator = { PositionIndicator(listState) },
    ) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { SectionHeader(stringResource(R.string.wearset_settings), onBack) }

            item {
                SettingsChip(
                    label    = stringResource(R.string.wearset_theme_title),
                    subtitle = stringResource(R.string.wearset_theme_sub),
                    icon     = Icons.Filled.Settings,
                    onClick  = onOpenTheme,
                )
            }
            item {
                SettingsChip(
                    label    = stringResource(R.string.wearset_privacy_title),
                    subtitle = stringResource(R.string.wearset_privacy_sub),
                    icon     = Icons.Filled.Lock,
                    onClick  = onOpenPrivacy,
                )
            }
            item {
                SettingsChip(
                    label    = stringResource(R.string.wearset_notif_title),
                    subtitle = stringResource(R.string.wearset_notif_sub),
                    icon     = Icons.Filled.Notifications,
                    onClick  = onOpenNotifications,
                )
            }
            item {
                SettingsChip(
                    label    = stringResource(R.string.wearset_about_title),
                    subtitle = stringResource(R.string.wearset_about_sub),
                    icon     = Icons.Filled.Info,
                    onClick  = onOpenAbout,
                )
            }
        }
    }
}

@Composable
internal fun SettingsChip(
    label: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Chip(
        label          = { Text(label) },
        secondaryLabel = if (subtitle != null) {
            { Text(subtitle, style = MaterialTheme.typography.caption2, color = MaterialTheme.colors.onSurfaceVariant, maxLines = 1) }
        } else null,
        icon     = { Icon(icon, contentDescription = null) },
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}
