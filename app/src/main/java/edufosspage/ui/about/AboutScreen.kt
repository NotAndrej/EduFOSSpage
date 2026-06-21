package org.edufosspage.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.SystemUpdate
import org.edufosspage.BuildConfig
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.edufosspage.ui.other.TileCard
import org.edufosspage.ui.other.TileDivider
import org.edufosspage.ui.other.TileRow
import org.edufosspage.ui.settings.SectionLabel

@Composable
fun AboutScreen(onBack: () -> Unit = {}) {
    val ctx          = LocalContext.current
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    var updateInfo     by remember { mutableStateOf<org.edufosspage.data.UpdateInfo?>(null) }
    var checkingUpdate by remember { mutableStateOf(false) }

    updateInfo?.let { info ->
        org.edufosspage.ui.UpdateDialog(
            info      = info,
            onDismiss = { updateInfo = null },
        )
    }

    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    fun showSnackbar(msg: String) {
        snackbarJob?.cancel()
        snackbarHost.currentSnackbarData?.dismiss()
        snackbarJob = scope.launch {
            snackbarHost.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Text("About", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionLabel("App info")

                TileCard {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("EduFOSSpage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("A free and open-source EduPage client", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text  = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    TileRow(icon = Icons.Outlined.Person, label = "Developer", subtitle = "Not Andrej", onClick = {})
                    TileDivider()
                    TileRow(
                        icon     = Icons.Outlined.SystemUpdate,
                        label    = "Check for updates",
                        subtitle = if (checkingUpdate) "Checking…" else "Version ${BuildConfig.VERSION_NAME}",
                        onClick  = {
                            if (!checkingUpdate) {
                                checkingUpdate = true
                                scope.launch {
                                    val info = org.edufosspage.data.UpdateChecker.check()
                                    if (info != null) updateInfo = info
                                    else showSnackbar("You're on the latest version")
                                    checkingUpdate = false
                                }
                            }
                        },
                    )
                }

                SectionLabel("Legal")

                TileCard {
                    TileRow(
                        icon         = Icons.Outlined.Code,
                        label        = "Source Code",
                        subtitle     = "github.com/NotAndrej/EduFOSSpage",
                        trailingIcon = Icons.Filled.OpenInNew,
                        onClick      = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/NotAndrej/EduFOSSpage"))) }
                    )
                    TileDivider()
                    TileRow(
                        icon         = Icons.Outlined.Gavel,
                        label        = "License",
                        subtitle     = "GNU General Public License v3.0",
                        trailingIcon = Icons.Filled.OpenInNew,
                        onClick      = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"))) }
                    )
                    TileDivider()
                    TileRow(
                        icon     = Icons.Outlined.Info,
                        label    = "Disclaimer",
                        subtitle = "Not a product of aSc Applied Software Consultants",
                        trailingIcon = Icons.Filled.OpenInNew,
                        onClick  = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ascacademic.com/"))) }
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
