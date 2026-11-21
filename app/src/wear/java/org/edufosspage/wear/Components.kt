package org.edufosspage.wear

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/** Full-screen "please wait" state, centred for the round display. */
@Composable
fun LoadingState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            CircularProgressIndicator()
            Text(message, textAlign = TextAlign.Center)
        }
    }
}

/** Compact empty/error block that fits inside a ScalingLazyColumn item. */
@Composable
fun InfoBlock(
    title: String,
    detail: String? = null,
    isError: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Text(
            title,
            textAlign = TextAlign.Center,
            color = if (isError) MaterialTheme.colors.error else MaterialTheme.colors.onBackground,
        )
        detail?.let {
            Text(
                it,
                style = MaterialTheme.typography.caption1,
                color = MaterialTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Small inline loading row for list headers/footers. */
@Composable
fun LoadingRow(message: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.padding(vertical = 8.dp),
    ) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(message, style = MaterialTheme.typography.caption1, color = MaterialTheme.colors.onSurfaceVariant)
    }
}

/**
 * Round-friendly screen header: a back chip on the left and the title centred.
 * The trailing slot keeps the title optically centred whether or not an action is present.
 */
@Composable
fun SectionHeader(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompactChip(
            onClick = onBack,
            icon = { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") },
        )
        Text(
            title,
            style = MaterialTheme.typography.title3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) {
            trailing()
        } else {
            Spacer(Modifier.width(40.dp))
        }
    }
}

/** Refresh chip that swaps its icon for a spinner while a fetch is running. */
@Composable
fun RefreshChip(refreshing: Boolean, onClick: () -> Unit) {
    CompactChip(
        onClick = onClick,
        icon = {
            if (refreshing) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        },
    )
}
