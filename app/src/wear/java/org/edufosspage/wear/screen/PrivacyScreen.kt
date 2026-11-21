package org.edufosspage.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.wear.compose.material.rememberScalingLazyListState
import org.edufosspage.R
import org.edufosspage.data.AppCache
import org.edufosspage.data.PinStore
import org.edufosspage.data.TamperStore
import org.edufosspage.wear.SectionHeader

/** Privacy settings on the watch: PIN, screenshot blocking and the on-disk cache. */
@Composable
fun PrivacyScreen(
    onBack: () -> Unit,
    onSetPin: () -> Unit,
) {
    val ctx = LocalContext.current

    var pinEnabled by remember { mutableStateOf(PinStore.isEnabled(ctx)) }
    var hasPin by remember { mutableStateOf(PinStore.hasPin(ctx)) }
    var confirmRemove by remember { mutableStateOf(false) }
    var screenshotBlocked by remember { mutableStateOf(TamperStore.isScreenshotBlocked(ctx)) }
    var cacheEnabled by remember { mutableStateOf(AppCache.isCacheEnabled(ctx)) }

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
            item { SectionHeader(stringResource(R.string.wearset_privacy_title), onBack) }

            item {
                PrivacyToggleChip(
                    checked = pinEnabled,
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.wearset_pin_lock),
                    subtitle = stringResource(if (hasPin) R.string.wearset_pin_set_desc else R.string.wearset_pin_unset_desc),
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (hasPin) { pinEnabled = true; PinStore.setEnabled(ctx, true) }
                            else onSetPin()   // make the user choose a PIN first
                        } else {
                            PinStore.clearPin(ctx)
                            pinEnabled = false
                            hasPin = false
                        }
                    },
                )
            }

            if (pinEnabled) {
                item {
                    Chip(
                        label = {
                            Text(stringResource(if (confirmRemove) R.string.wear_pin_remove_confirm else R.string.wear_pin_remove))
                        },
                        icon    = { Icon(Icons.Filled.Delete, contentDescription = null) },
                        onClick = {
                            if (confirmRemove) {
                                PinStore.clearPin(ctx)
                                pinEnabled = false
                                hasPin = false
                                confirmRemove = false
                            } else {
                                confirmRemove = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            item {
                PrivacyToggleChip(
                    checked = screenshotBlocked,
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.wearset_block_screenshots),
                    subtitle = stringResource(R.string.wearset_block_screenshots_desc),
                    onCheckedChange = { v -> screenshotBlocked = v; TamperStore.setScreenshotBlocked(ctx, v) },
                )
            }

            item {
                PrivacyToggleChip(
                    checked = cacheEnabled,
                    icon = Icons.Filled.Refresh,
                    title = stringResource(R.string.wearset_local_cache),
                    subtitle = stringResource(if (cacheEnabled) R.string.wearset_cache_on else R.string.wearset_cache_off),
                    onCheckedChange = { v -> cacheEnabled = v; AppCache.setCacheEnabled(ctx, v) },
                )
            }
        }
    }
}

/** Round-screen on/off row with a descriptive secondary label. */
@Composable
private fun PrivacyToggleChip(
    checked: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    ToggleChip(
        checked         = checked,
        onCheckedChange = onCheckedChange,
        label           = { Text(title) },
        secondaryLabel  = {
            Text(
                subtitle,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                maxLines = 2,
                textAlign = TextAlign.Start,
            )
        },
        appIcon = { Icon(icon, contentDescription = null) },
        toggleControl = {
            Icon(
                ToggleChipDefaults.switchIcon(checked),
                contentDescription = null,
                modifier = Modifier.size(ToggleChipDefaults.IconSize),
            )
        },
        modifier = Modifier.fillMaxWidth(),
    )
}
