package org.edufosspage.ui.messages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import org.edufosspage.R
import org.edufosspage.data.EduMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(vm: MessagesViewModel = viewModel()) {
    val messages       by vm.messages.collectAsState()
    val searchQuery    by vm.searchQuery.collectAsState()
    val isRefreshing   by vm.isRefreshing.collectAsState()
    val initialLoading by vm.initialLoading.collectAsState()
    val isFetchingMore by vm.isFetchingMore.collectAsState()
    val fetchProgress  by vm.fetchProgress.collectAsState()
    val hasMore        by vm.hasMore.collectAsState()
    val error          by vm.error.collectAsState()
    val refreshError   by vm.refreshError.collectAsState()

    val snackbarHost      = remember { SnackbarHostState() }
    val refreshErrorMsg   = stringResource(R.string.msg_messages_refresh_error)
    var selectedMessageId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedMessage   = messages?.firstOrNull { it.id == selectedMessageId }

    var searchActive by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = selectedMessageId != null) { selectedMessageId = null }
    BackHandler(enabled = searchActive && selectedMessageId == null) {
        searchActive = false
        vm.setSearch("")
    }

    LaunchedEffect(refreshError) {
        if (refreshError) {
            snackbarHost.showSnackbar(refreshErrorMsg,
                duration = SnackbarDuration.Short)
            vm.clearRefreshError()
        }
    }

    // Detail view
    selectedMessage?.let { msg ->
        MessageDetailScreen(msg = msg, onBack = { selectedMessageId = null })
        return
    }

    // List view
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            // Header + search bar
            AnimatedContent(
                targetState = searchActive,
                transitionSpec = {
                    fadeIn() + slideInVertically() togetherWith fadeOut() + slideOutVertically()
                },
                label = "search_header"
            ) { inSearch ->
                if (inSearch) {
                    SearchBar(
                        query    = searchQuery,
                        onQuery  = { vm.setSearch(it) },
                        onClose  = { searchActive = false; vm.setSearch("") },
                    )
                } else {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 12.dp),
                        verticalAlignment      = Alignment.CenterVertically,
                        horizontalArrangement  = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.msg_messages_title),
                            style      = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        IconButton(onClick = { searchActive = true }) {
                            Icon(Icons.Outlined.Search, contentDescription = stringResource(R.string.msg_search))
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Fetch progress banner
            AnimatedVisibility(visible = fetchProgress != null) {
                fetchProgress?.let { progress ->
                    Surface(
                        color    = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment      = Alignment.CenterVertically,
                            horizontalArrangement  = Arrangement.spacedBy(10.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            Text(
                                progress,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                }
            }

            // Content area
            when {
                initialLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }

                error -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            stringResource(R.string.msg_messages_load_error),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilledTonalButton(onClick = { vm.refresh() }) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.msg_retry))
                        }
                    }
                }

                else -> {
                    val currentMessages = messages ?: emptyList()

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh    = { vm.refresh() },
                        modifier     = Modifier.fillMaxSize(),
                    ) {
                        if (currentMessages.isEmpty() && searchQuery.isBlank()) {
                            // True empty state
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("📭", style = MaterialTheme.typography.displayMedium)
                                    Text(
                                        stringResource(R.string.msg_no_messages),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else if (currentMessages.isEmpty() && searchQuery.isNotBlank()) {
                            // Search returned nothing yet
                            Box(Modifier.fillMaxSize(), Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    if (fetchProgress != null) {
                                        CircularProgressIndicator()
                                        Text(
                                            stringResource(R.string.msg_searching_older),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    } else {
                                        Text("🔍", style = MaterialTheme.typography.displayMedium)
                                        Text(
                                            stringResource(R.string.msg_no_messages_found),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        } else {
                            MessageList(
                                messages       = currentMessages,
                                hasMore        = hasMore && searchQuery.isBlank(),
                                isFetchingMore = isFetchingMore,
                                onLoadMore     = { vm.loadMore() },
                                onMessageClick = { selectedMessageId = it.id },
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// Search bar

@Composable
private fun SearchBar(
    query:   String,
    onQuery: (String) -> Unit,
    onClose: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard       = LocalSoftwareKeyboardController.current
    val focusMgr       = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value           = query,
            onValueChange   = onQuery,
            placeholder     = { Text(stringResource(R.string.msg_search_messages_hint)) },
            singleLine      = true,
            leadingIcon     = { Icon(Icons.Outlined.Search, null) },
            trailingIcon    = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Default.Close, stringResource(R.string.msg_clear))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide(); focusMgr.clearFocus()
            }),
            shape    = RoundedCornerShape(28.dp),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        TextButton(onClick = onClose) { Text(stringResource(R.string.msg_cancel)) }
    }
}

// Message list with infinite scroll

@Composable
private fun MessageList(
    messages:       List<EduMessage>,
    hasMore:        Boolean,
    isFetchingMore: Boolean,
    onLoadMore:     () -> Unit,
    onMessageClick: (EduMessage) -> Unit,
) {
    val ctx = LocalContext.current
    val listState = rememberLazyListState()

    // Trigger loadMore when we're 5 items from the bottom
    LaunchedEffect(listState) {
        snapshotFlow {
            val info      = listState.layoutInfo
            val lastIndex = info.visibleItemsInfo.lastOrNull()?.index ?: return@snapshotFlow false
            lastIndex >= info.totalItemsCount - 5
        }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd && hasMore && !isFetchingMore) onLoadMore()
            }
    }

    val grouped = messages
        .groupBy { MessagesViewModel.relativeDate(ctx, it.date) }
        .entries
        .toList()

    LazyColumn(
        state               = listState,
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier            = Modifier.fillMaxSize(),
    ) {
        grouped.forEach { (dateLabel, msgs) ->
            item(key = "header_$dateLabel") { DateSeparator(dateLabel) }
            items(msgs, key = { it.id }) { msg ->
                MessageCard(msg, onClick = { onMessageClick(msg) })
            }
            item(key = "spacer_$dateLabel") { Spacer(Modifier.height(8.dp)) }
        }

        item(key = "footer") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                Alignment.Center,
            ) {
                when {
                    isFetchingMore -> CircularProgressIndicator(
                        modifier    = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                    )
                    !hasMore -> Text(
                        stringResource(R.string.msg_all_messages_loaded),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// Sub-composables

@Composable
private fun DateSeparator(label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            label,
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color      = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun MessageCard(msg: EduMessage, onClick: () -> Unit) {
    Surface(
        shape    = RoundedCornerShape(16.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape    = CircleShape,
                color    = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp),
            ) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        msg.sender.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    msg.sender,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    msg.preview,
                    style    = MaterialTheme.typography.bodySmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                msg.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            )
        }
    }
}
