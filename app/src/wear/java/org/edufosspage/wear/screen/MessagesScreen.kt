package org.edufosspage.wear.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
            contentPadding      = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { SectionHeader("Messages", onBack, trailing = { RefreshChip(refreshing) { vm.refresh() } }) }

            when {
                list == null -> item { LoadingRow(if (loading) "Loading messages…" else "Refreshing…") }

                list.isEmpty() -> item {
                    InfoBlock(
                        title   = if (error) "Couldn't load messages" else "No messages",
                        detail  = if (error) "Check your connection and refresh" else "Nothing new right now",
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
                "${MessagesViewModel.relativeDate(message.date)} · ${message.preview}",
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

    Scaffold(
        timeText          = { TimeText() },
        positionIndicator = { PositionIndicator(listState) },
    ) {
        ScalingLazyColumn(
            state               = listState,
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(horizontal = 8.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item { SectionHeader("Message", onBack) }

            if (message == null) {
                item { InfoBlock("Message unavailable") }
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
                        "${MessagesViewModel.relativeDate(message.date)}" +
                            if (message.time.isNotBlank()) " · ${message.time}" else "",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                    )
                }
                item {
                    Text(
                        message.preview,
                        style = MaterialTheme.typography.body2,
                    )
                }
            }
        }
    }
}
