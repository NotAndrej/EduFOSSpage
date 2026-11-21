package org.edufosspage.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.edufosspage.R
import org.edufosspage.data.NotificationScheduler
import org.edufosspage.data.NotificationStore
import org.edufosspage.ui.other.TileCard
import org.edufosspage.ui.other.TileDivider

@Composable
fun NotificationsScreen(onBack: () -> Unit = {}) {
    val ctx = LocalContext.current

    // State
    var masterOn    by remember { mutableStateOf(NotificationStore.isMasterEnabled(ctx)) }
    var messagesOn  by remember { mutableStateOf(NotificationStore.isMessagesEnabled(ctx)) }
    var gradesOn    by remember { mutableStateOf(NotificationStore.isGradesEnabled(ctx)) }
    var timetableOn by remember { mutableStateOf(NotificationStore.isTimetableEnabled(ctx)) }

    var testRunning by remember { mutableStateOf(false) }
    var testDone    by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.set_back))
            }
            Text(
                stringResource(R.string.set_notifications),
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(
            modifier            = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // Master toggle
            SectionLabel(stringResource(R.string.set_general))
            TileCard {
                NotifToggleRow(
                    icon     = Icons.Outlined.Notifications,
                    title    = stringResource(R.string.set_enable_notifications),
                    subtitle = stringResource(R.string.set_enable_notifications_desc),
                    checked  = masterOn,
                    onCheckedChange = { v ->
                        masterOn = v
                        NotificationStore.setMaster(ctx, v)
                        if (v) NotificationScheduler.schedule(ctx)
                        else   NotificationScheduler.cancel(ctx)
                    },
                )
            }

            // Per-channel
            SectionLabel(stringResource(R.string.set_channels))
            TileCard {
                NotifToggleRow(
                    icon     = Icons.Outlined.Email,
                    title    = stringResource(R.string.set_messages),
                    subtitle = stringResource(R.string.set_messages_desc),
                    checked  = messagesOn && masterOn,
                    enabled  = masterOn,
                    onCheckedChange = { v ->
                        messagesOn = v
                        NotificationStore.setMessages(ctx, v)
                    },
                )
                TileDivider()
                NotifToggleRow(
                    icon     = Icons.Outlined.Grade,
                    title    = stringResource(R.string.set_grades),
                    subtitle = stringResource(R.string.set_grades_desc),
                    checked  = gradesOn && masterOn,
                    enabled  = masterOn,
                    onCheckedChange = { v ->
                        gradesOn = v
                        NotificationStore.setGrades(ctx, v)
                    },
                )
                TileDivider()
                NotifToggleRow(
                    icon     = Icons.Outlined.CalendarMonth,
                    title    = stringResource(R.string.set_timetable_changes),
                    subtitle = stringResource(R.string.set_timetable_changes_desc),
                    checked  = timetableOn && masterOn,
                    enabled  = masterOn,
                    onCheckedChange = { v ->
                        timetableOn = v
                        NotificationStore.setTimetable(ctx, v)
                    },
                )
            }

            // Test button
            SectionLabel(stringResource(R.string.set_debug))
            TileCard {
                Row(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.set_test_poll),
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            if (testDone) stringResource(R.string.set_test_done)
                            else stringResource(R.string.set_test_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (testDone)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    FilledTonalButton(
                        enabled = masterOn && !testRunning,
                        onClick = {
                            testRunning = true
                            testDone    = false
                            NotificationScheduler.runNow(ctx)
                            // Give WorkManager a moment to enqueue, then update UI.
                            // The actual work runs async so we can't await it here.
                            testRunning = false
                            testDone    = true
                        },
                    ) {
                        if (testRunning) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Outlined.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.set_run))
                        }
                    }
                }
            }

            // Info card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier          = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp).padding(top = 1.dp),
                    )
                    Text(
                        stringResource(R.string.set_battery_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

// Shared toggle row

@Composable
private fun NotifToggleRow(
    icon:             ImageVector,
    title:            String,
    subtitle:         String,
    checked:          Boolean,
    enabled:          Boolean = true,
    onCheckedChange:  (Boolean) -> Unit,
) {
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                                 else         MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier           = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style      = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color      = if (enabled) MaterialTheme.colorScheme.onSurface
                             else         MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
                    .copy(alpha = if (enabled) 1f else 0.4f),
            )
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            enabled         = enabled,
        )
    }
}
