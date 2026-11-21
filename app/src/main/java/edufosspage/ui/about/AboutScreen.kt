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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.edufosspage.R
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

    val latestVersionMsg = stringResource(R.string.set_latest_version)

    Box(Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.set_back))
                }
                Text(stringResource(R.string.set_about), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionLabel(stringResource(R.string.set_app_info))

                TileCard {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("EduFOSSpage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.set_app_tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text  = stringResource(R.string.set_version_full, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    TileRow(icon = Icons.Outlined.Person, label = stringResource(R.string.set_developer), subtitle = stringResource(R.string.set_developer_subtitle), onClick = {})
                    TileDivider()
                    TileRow(
                        icon     = Icons.Outlined.SystemUpdate,
                        label    = stringResource(R.string.set_check_updates),
                        subtitle = if (checkingUpdate) stringResource(R.string.set_checking) else stringResource(R.string.set_version_short, BuildConfig.VERSION_NAME),
                        onClick  = {
                            if (!checkingUpdate) {
                                checkingUpdate = true
                                scope.launch {
                                    val info = org.edufosspage.data.UpdateChecker.check()
                                    if (info != null) updateInfo = info
                                    else showSnackbar(latestVersionMsg)
                                    checkingUpdate = false
                                }
                            }
                        },
                    )
                }

                SectionLabel(stringResource(R.string.set_legal))

                TileCard {
                    TileRow(
                        icon         = Icons.Outlined.Code,
                        label        = stringResource(R.string.set_source_code),
                        subtitle     = "github.com/NotAndrej/EduFOSSpage",
                        trailingIcon = Icons.Filled.OpenInNew,
                        onClick      = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/NotAndrej/EduFOSSpage"))) }
                    )
                    TileDivider()
                    TileRow(
                        icon         = Icons.Outlined.Gavel,
                        label        = stringResource(R.string.set_license),
                        subtitle     = stringResource(R.string.set_license_name),
                        trailingIcon = Icons.Filled.OpenInNew,
                        onClick      = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.gnu.org/licenses/gpl-3.0.html"))) }
                    )
                    TileDivider()
                    TileRow(
                        icon     = Icons.Outlined.Info,
                        label    = stringResource(R.string.set_disclaimer),
                        subtitle = stringResource(R.string.set_disclaimer_subtitle),
                        trailingIcon = Icons.Filled.OpenInNew,
                        onClick  = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ascacademic.com/"))) }
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHost, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
