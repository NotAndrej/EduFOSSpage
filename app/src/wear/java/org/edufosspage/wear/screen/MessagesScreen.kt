package org.edufosspage.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import androidx.wear.compose.material.rememberScalingLazyListState
import org.edufosspage.R
import org.edufosspage.data.EduMessage
import org.edufosspage.ui.messages.MessagesViewModel
import org.edufosspage.wear.InfoBlock
import org.edufosspage.wear.LoadingRow
import org.edufosspage.wear.RefreshChip
import org.edufosspage.wear.SectionHeader

@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    onOpenMessage: (EduMessage) -> Unit,
    vm: MessagesViewModel = viewModel(),
) {
    val messages by vm.messages.collectAsState()
    val loading by vm.initialLoading.collectAsState()
    val refreshing by vm.isRefreshing.collectAsState()
    val error by vm.error.collectAsState()

    val list = messages
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
            item { SectionHeader(stringResource(R.string.wear_title_messages), onBack, trailing = { RefreshChip(refreshing) { vm.refresh() } }) }

            when {
                list == null -> item {
                    LoadingRow(
                        if (loading) stringResource(R.string.wear_messages_loading)
                        else stringResource(R.string.wear_common_refreshing),
                    )
                }

                list.isEmpty() -> item {
                    InfoBlock(
                        title   = if (error) stringResource(R.string.wear_messages_error_title)
                                  else stringResource(R.string.wear_messages_empty_title),
                        detail  = if (error) stringResource(R.string.wear_error_check_connection)
                                  else stringResource(R.string.wear_messages_empty_detail),
                        isError = error,
                    )
                }

                else -> items(list, key = { it.id }) { message ->
                    MessageChip(message) { onOpenMessage(message) }
                }
            }
        }
    }
}

@Composable
private fun MessageChip(message: EduMessage, onClick: () -> Unit) {
    val ctx = LocalContext.current
    Chip(
        label = {
            Text(
                message.sender,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        secondaryLabel = {
            Text(
                "${MessagesViewModel.relativeDate(ctx, message.date)} · ${message.preview}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun MessageDetailScreen(message: EduMessage?, onBack: () -> Unit) {
    val listState = rememberScalingLazyListState()
    val ctx = LocalContext.current

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
            item { SectionHeader(stringResource(R.string.wear_title_message), onBack) }

            if (message == null) {
                item { InfoBlock(stringResource(R.string.wear_message_unavailable)) }
            } else {
                item {
                    Text(
                        message.sender,
                        style     = MaterialTheme.typography.title3,
                        textAlign = TextAlign.Center,
                        maxLines  = 2,
                        overflow  = TextOverflow.Ellipsis,
                    )
                }
                item {
                    Text(
                        "${MessagesViewModel.relativeDate(ctx, message.date)}" +
                            if (message.time.isNotBlank()) " · ${message.time}" else "",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
                item {
                    Text(
                        message.preview,
                        style     = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center,
                        // The screen is round, so full width is only free at the vertical
                        // middle; a paragraph that spans the width gets its first and last
                        // words cut off above and below that. A capped, centred column stays
                        // inside the bezel for the whole height of the screen.
                        modifier  = Modifier.widthIn(max = 160.dp),
                    )
                }
            }
        }
    }
}
