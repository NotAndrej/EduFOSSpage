package org.edufosspage.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.edufosspage.ui.login.LoginViewModel
import org.edufosspage.ui.other.TileCard
import org.edufosspage.ui.other.TileDivider
import org.edufosspage.ui.other.TileRow

@Composable
fun SettingsScreen(
    loginVm:             LoginViewModel = viewModel(),
    onBack:              () -> Unit     = {},
    onLoggedOut:         () -> Unit     = {},
    onOpenThemes:        () -> Unit     = {},
    onOpenPrivacy:       () -> Unit     = {},
    onOpenNotifications: () -> Unit     = {},
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title            = { Text("Log out?") },
            text             = { Text("You'll need to sign in again.") },
            confirmButton    = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    loginVm.logout()
                    onLoggedOut()
                }) { Text("Log out", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("Preferences")

            TileCard {
                TileRow(
                    icon     = Icons.Outlined.Palette,
                    label    = "Themes",
                    subtitle = "Light, dark, AMOLED",
                    onClick  = onOpenThemes
                )
                TileDivider()
                TileRow(
                    icon     = Icons.Outlined.Security,
                    label    = "Privacy",
                    subtitle = "Cache, app lock",
                    onClick  = onOpenPrivacy
                )
                TileDivider()
                TileRow(
                    icon     = Icons.Outlined.Notifications,
                    label    = "Notifications",
                    subtitle = "Messages, grades, timetable alerts",
                    onClick  = onOpenNotifications
                )
            }

            SectionLabel("Account")

            TileCard {
                Row(
                    modifier              = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Logout, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.error)
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Log out",
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color      = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Clears saved credentials and cache",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        colors  = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("Log out") }
                }
            }
        }
    }
}

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        style      = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
internal fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape    = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}
