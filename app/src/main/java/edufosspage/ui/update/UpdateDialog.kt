package org.edufosspage.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.edufosspage.BuildConfig
import org.edufosspage.data.UpdateChecker
import org.edufosspage.data.UpdateInfo
/**
 * Shows when [UpdateChecker.check] finds a newer version.
 * Handles the full download → install flow inline.
 */
@Composable
fun UpdateDialog(
    info:        UpdateInfo,
    onDismiss:   () -> Unit,
) {
    val ctx   = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloading by remember { mutableStateOf(false) }
    var progress    by remember { mutableStateOf(0f) }
    var etaLabel    by remember { mutableStateOf("") }
    var failed      by remember { mutableStateOf(false) }

    // Rolling speed window: list of (epochMs, bytesSoFar) samples
    val speedSamples = remember { ArrayDeque<Pair<Long, Long>>(10) }

    fun updateEta(bytesDownloaded: Long, totalBytes: Long) {
        val now = System.currentTimeMillis()
        speedSamples.addLast(now to bytesDownloaded)
        // Keep only the last 5 samples (~last ~2-3 seconds of data)
        while (speedSamples.size > 5) speedSamples.removeFirst()

        if (speedSamples.size < 2 || totalBytes <= 0) { etaLabel = ""; return }

        val oldest   = speedSamples.first()
        val newest   = speedSamples.last()
        val elapsed  = (newest.first - oldest.first).coerceAtLeast(1L)
        val bytesDelta = newest.second - oldest.second
        if (bytesDelta <= 0) { etaLabel = ""; return }

        val bytesPerMs  = bytesDelta.toDouble() / elapsed
        val remaining   = totalBytes - bytesDownloaded
        val etaMs       = (remaining / bytesPerMs).toLong()

        etaLabel = when {
            etaMs < 5_000  -> "almost done"
            etaMs < 60_000 -> "~${etaMs / 1000}s remaining"
            else           -> "~${etaMs / 60_000}m remaining"
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        label       = "dl_progress",
    )

    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        icon  = {
            Icon(
                Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text(
                "Update available",
                fontWeight = FontWeight.Bold,
            )
        },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                ) {
                    Surface(
                        color  = MaterialTheme.colorScheme.surfaceVariant,
                        shape  = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            BuildConfig.VERSION_NAME,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelMedium,
                            color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text("→", style = MaterialTheme.typography.bodyMedium)
                    Surface(
                        color  = MaterialTheme.colorScheme.primaryContainer,
                        shape  = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            info.latestVersion,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style    = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                if (downloading) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(
                            progress    = { animatedProgress },
                            modifier    = Modifier.fillMaxWidth(),
                        )
                        Text(
                            when {
                                progress >= 1f   -> "Installing…"
                                etaLabel.isBlank() -> "Downloading… ${(progress * 100).toInt()}%"
                                else             -> "Downloading… ${(progress * 100).toInt()}%  •  $etaLabel"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                if (failed) {
                    Text(
                        "Download failed - check your connection and try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !downloading,
                onClick = {
                    failed         = false
                    downloading    = true
                    speedSamples.clear()
                    scope.launch {
                        val apk = UpdateChecker.download(
                            ctx         = ctx,
                            url         = info.downloadUrl,
                            checksumUrl = info.checksumUrl,
                            onProgress  = { downloaded, total ->
                                progress = if (total > 0) downloaded.toFloat() / total else 0f
                                updateEta(downloaded, total)
                            },
                        )
                        if (apk != null) {
                            UpdateChecker.install(ctx, apk)
                            onDismiss()
                        } else {
                            failed      = true
                            downloading = false
                            etaLabel    = ""
                        }
                    }
                },
            ) {
                if (downloading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (downloading) "Downloading…" else "Update now")
            }
        },
        dismissButton = {
            if (!downloading) {
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        },
    )
}
