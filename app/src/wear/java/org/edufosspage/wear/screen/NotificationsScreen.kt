package org.edufosspage.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Today
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.rememberScalingLazyListState
import kotlinx.coroutines.launch
import org.edufosspage.R
import org.edufosspage.data.NotificationScheduler
import org.edufosspage.data.NotificationStore
import org.edufosspage.wear.SectionHeader

/** Notification settings on the watch: master switch, per-channel switches and a manual poll. */
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var masterOn    by remember { mutableStateOf(NotificationStore.isMasterEnabled(ctx)) }
    var messagesOn  by remember { mutableStateOf(NotificationStore.isMessagesEnabled(ctx)) }
    var gradesOn    by remember { mutableStateOf(NotificationStore.isGradesEnabled(ctx)) }
    var timetableOn by remember { mutableStateOf(NotificationStore.isTimetableEnabled(ctx)) }
    var testDone    by remember { mutableStateOf(false) }

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
            item { SectionHeader(stringResource(R.string.wearset_notif_title), onBack) }

            item {
                Text(
                    stringResource(R.string.wearset_notif_general),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.primary,
                )
            }

            item {
                NotifToggleChip(
                    checked  = masterOn,
                    icon     = Icons.Filled.Notifications,
                    title    = stringResource(R.string.wearset_notif_enable),
                    subtitle = stringResource(R.string.wearset_notif_enable_desc),
                    onCheckedChange = { v ->
                        masterOn = v
                        NotificationStore.setMaster(ctx, v)
                        if (v) NotificationScheduler.schedule(ctx) else NotificationScheduler.cancel(ctx)
                    },
                )
            }

            item {
                Text(
                    stringResource(R.string.wearset_notif_channels),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.primary,
                )
            }

            item {
                NotifToggleChip(
                    checked  = messagesOn && masterOn,
                    enabled  = masterOn,
                    icon     = Icons.Filled.Email,
                    title    = stringResource(R.string.wearset_notif_messages),
                    subtitle = stringResource(R.string.wearset_notif_messages_desc),
                    onCheckedChange = { messagesOn = it; NotificationStore.setMessages(ctx, it) },
                )
            }
            item {
                NotifToggleChip(
                    checked  = gradesOn && masterOn,
                    enabled  = masterOn,
                    icon     = Icons.Filled.Star,
                    title    = stringResource(R.string.wearset_notif_grades),
                    subtitle = stringResource(R.string.wearset_notif_grades_desc),
                    onCheckedChange = { gradesOn = it; NotificationStore.setGrades(ctx, it) },
                )
            }
            item {
                NotifToggleChip(
                    checked  = timetableOn && masterOn,
                    enabled  = masterOn,
                    icon     = Icons.Filled.Today,
                    title    = stringResource(R.string.wearset_notif_timetable),
                    subtitle = stringResource(R.string.wearset_notif_timetable_desc),
                    onCheckedChange = { timetableOn = it; NotificationStore.setTimetable(ctx, it) },
                )
            }

            item {
                Chip(
                    label = {
                        Text(if (testDone) stringResource(R.string.wearset_notif_test_done) else stringResource(R.string.wearset_run))
                    },
                    secondaryLabel = {
                        Text(
                            stringResource(R.string.wearset_notif_test_hint),
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            maxLines = 2,
                        )
                    },
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = null) },
                    onClick = {
                        testDone = true
                        scope.launch { NotificationScheduler.runNow(ctx) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    stringResource(R.string.wearset_notif_info),
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}

/** Round-screen on/off row: a category icon on the left and a switch glyph on the right. */
@Composable
private fun NotifToggleChip(
    checked: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    ToggleChip(
        checked         = checked,
        enabled         = enabled,
        onCheckedChange = onCheckedChange,
        label           = { Text(title) },
        secondaryLabel  = {
            Text(
                subtitle,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                maxLines = 2,
            )
        },
        appIcon = { Icon(icon, contentDescription = null) },
        toggleControl = {
            Icon(
                ToggleChipDefaults.switchIcon(checked),
                contentDescription = null,
                modifier = Modifier.size(ToggleChipDefaults.IconSize),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
