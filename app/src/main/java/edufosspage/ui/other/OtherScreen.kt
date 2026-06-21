package org.edufosspage.ui.other

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun OtherScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAbout:    () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp)
        ) {
            Text("Other", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TileCard {
                TileRow(
                    icon     = Icons.Outlined.Settings,
                    label    = "Settings",
                    subtitle = "Theme, privacy, notifications",
                    onClick  = onNavigateToSettings
                )
                TileDivider()
                TileRow(
                    icon     = Icons.Outlined.Info,
                    label    = "About",
                    subtitle = "App info, licenses, version",
                    onClick  = onNavigateToAbout
                )
            }
        }
    }
}

@Composable
internal fun TileCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
internal fun TileDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(start = 56.dp))
}

@Composable
internal fun TileRow(
    icon:         ImageVector,
    label:        String,
    subtitle:     String?      = null,
    trailingIcon: ImageVector? = null,
    enabled:      Boolean      = true,
    onClick:      () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null, Modifier.size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Column(Modifier.weight(1f)) {
            Text(
                label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.8f else 0.4f))
            }
        }
        if (trailingIcon != null) {
            Icon(trailingIcon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        } else if (enabled) {
            Icon(Icons.Default.ChevronRight, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        }
    }
}
