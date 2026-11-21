package org.edufosspage.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

@Composable
fun MenuScreen(
    userName: String,
    userClass: String,
    onOpenTimetable: () -> Unit,
    onOpenGrades: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenSettings: () -> Unit,
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
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "EduFOSSpage",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.primary,
                    )
                    Text(
                        userName,
                        style     = MaterialTheme.typography.caption1,
                        color     = MaterialTheme.colors.onSurfaceVariant,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                    if (userClass.isNotBlank()) {
                        Text(
                            userClass,
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                        )
                    }
                }
            }

            item { MenuChip(stringResource(R.string.wear_menu_timetable), Icons.Filled.List,  onOpenTimetable) }
            item { MenuChip(stringResource(R.string.wear_menu_grades),    Icons.Filled.Star,  onOpenGrades) }
            item { MenuChip(stringResource(R.string.wear_menu_messages),  Icons.Filled.Email, onOpenMessages) }
            item { MenuChip(stringResource(R.string.wear_menu_account),   Icons.Filled.Person, onOpenAccount) }
            item { MenuChip(stringResource(R.string.wearset_menu_settings), Icons.Filled.Settings, onOpenSettings) }
        }
    }
}

@Composable
private fun MenuChip(label: String, icon: ImageVector, onClick: () -> Unit) {
    Chip(
        label  = { Text(label) },
        icon   = { Icon(icon, contentDescription = null) },
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}
