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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.edufosspage.data.AppTheme
import org.edufosspage.data.ThemeStore

@Composable
fun ThemeScreen(onBack: () -> Unit = {}) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme by ThemeStore.themeFlow(ctx).collectAsState(initial = AppTheme.SYSTEM)

    val themes = listOf(
        AppTheme.SYSTEM to ("System default" to "Follows your device setting"),
        AppTheme.LIGHT  to ("Light"          to "Light mode"),
        AppTheme.DARK   to ("Dark"           to "Dark mode"),
        AppTheme.AMOLED to ("OLED Black"     to "Recommended for OLED displays"),
    )

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Themes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionLabel("Appearance")
            SettingsCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Palette, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(10.dp))
                        Text("App theme", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    themes.forEach { (t, info) ->
                        val (label, description) = info
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = theme == t,
                                onClick  = { scope.launch { ThemeStore.setTheme(ctx, t) } }
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
