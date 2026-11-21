package org.edufosspage.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.wear.compose.material.rememberScalingLazyListState
import kotlinx.coroutines.launch
import org.edufosspage.BuildConfig
import org.edufosspage.R
import org.edufosspage.data.UpdateChecker
import org.edufosspage.data.UpdateInfo
import org.edufosspage.wear.SectionHeader

/** About screen, mirroring the phone app: version, licence, source, disclaimer and self-update. */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var latest    by remember { mutableStateOf<UpdateInfo?>(null) }
    var fetching  by remember { mutableStateOf(false) }
    var upToDate  by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }

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
            item { SectionHeader(stringResource(R.string.wearset_about_title), onBack) }

            item {
                Text(
                    "EduFOSSpage",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.primary,
                )
            }
            item {
                Text(
                    stringResource(R.string.wearset_about_app_desc),
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            item {
                Text(
                    stringResource(R.string.wearset_version, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                )
            }

            // Self-update. The first tap checks GitHub; if a newer release exists a second tap
            // downloads and hands the APK to the system installer.
            item {
                Chip(
                    label = {
                        Text(
                            when {
                                updateMsg != null -> updateMsg!!
                                latest != null     -> stringResource(R.string.wearset_update_now)
                                else               -> stringResource(R.string.wearset_run)
                            }
                        )
                    },
                    secondaryLabel = {
                        val info = latest
                        val secondary = when {
                            fetching -> stringResource(R.string.wearset_update_downloading)
                            info != null -> stringResource(R.string.wearset_update_available)
                            upToDate -> stringResource(R.string.wearset_latest_version)
                            updateMsg != null -> updateMsg!!
                            else -> stringResource(R.string.wearset_notif_test_hint)
                        }
                        Text(
                            secondary,
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            maxLines = 2,
                        )
                    },
                    icon = { Icon(if (fetching) Icons.Filled.Refresh else Icons.Outlined.SystemUpdate, contentDescription = null) },
                    enabled = !fetching,
                    onClick = {
                        val info = latest
                        if (info == null) {
                            fetching = true
                            upToDate = false
                            scope.launch {
                                val found = UpdateChecker.check()
                                latest = found
                                fetching = false
                                upToDate = found == null
                            }
                        } else {
                            fetching = true
                            scope.launch {
                                val file = UpdateChecker.download(ctx, info.downloadUrl, info.checksumUrl) { _, _ -> }
                                fetching = false
                                if (file != null) {
                                    runCatching { UpdateChecker.install(ctx, file) }
                                        .onFailure { updateMsg = ctx.getString(R.string.wearset_update_failed) }
                                } else {
                                    updateMsg = ctx.getString(R.string.wearset_update_failed)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (latest != null) {
                item {
                    Chip(
                        label = { Text(stringResource(R.string.wearset_later)) },
                        icon = { Icon(Icons.Filled.Cancel, contentDescription = null) },
                        onClick = { latest = null; updateMsg = null },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                Chip(
                    label = { Text(stringResource(R.string.wearset_license)) },
                    secondaryLabel = {
                        Text(
                            stringResource(R.string.wearset_license_value),
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            maxLines = 2,
                        )
                    },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            item {
                Text(
                    stringResource(R.string.wearset_disclaimer_text),
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}
