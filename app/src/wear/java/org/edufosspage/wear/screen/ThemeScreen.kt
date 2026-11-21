package org.edufosspage.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import kotlinx.coroutines.launch
import org.edufosspage.R
import org.edufosspage.data.AppTheme
import org.edufosspage.data.ThemeStore
import org.edufosspage.wear.SectionHeader

/** Theme picker, sharing the phone app's stored preference. */
@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val theme by ThemeStore.themeFlow(ctx).collectAsState(initial = AppTheme.SYSTEM)

    val options = listOf(
        AppTheme.SYSTEM to (stringResource(R.string.wearset_theme_system) to stringResource(R.string.wearset_theme_system_desc)),
        AppTheme.LIGHT to (stringResource(R.string.wearset_theme_light) to stringResource(R.string.wearset_theme_light_desc)),
        AppTheme.DARK to (stringResource(R.string.wearset_theme_dark) to stringResource(R.string.wearset_theme_dark_desc)),
        AppTheme.AMOLED to (stringResource(R.string.wearset_theme_amoled) to stringResource(R.string.wearset_theme_amoled_desc)),
    )

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
            item { SectionHeader(stringResource(R.string.wearset_theme_title), onBack) }

            item {
                Text(
                    stringResource(R.string.wearset_app_theme),
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.primary,
                )
            }

            items(options.size) { index ->
                val (value, info) = options[index]
                val (label, description) = info
                Chip(
                    label          = { Text(label) },
                    secondaryLabel = {
                        Text(
                            description,
                            style = MaterialTheme.typography.caption2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                        )
                    },
                    icon = {
                        if (theme == value) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colors.primary)
                        }
                    },
                    onClick  = { scope.launch { ThemeStore.setTheme(ctx, value) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
