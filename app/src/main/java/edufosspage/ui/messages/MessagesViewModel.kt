package org.edufosspage.ui.messages

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.edufosspage.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class MessagesViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _allMessages = MutableStateFlow<List<EduMessage>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery          = _searchQuery.asStateFlow()

    /** Filtered when a search query is active, full list otherwise. */
    val messages: StateFlow<List<EduMessage>?> = combine(_allMessages, _searchQuery) { all, q ->
        if (q.isBlank()) all else all.filter { matches(it, q) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True while the very first load hasn't returned yet. */
    private val _initialLoading = MutableStateFlow(true)
    val initialLoading           = _initialLoading.asStateFlow()

    /** True while refreshing the recent 30 days in the background. */
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing           = _isRefreshing.asStateFlow()

    /** True while loading an older chunk (infinite scroll). */
    private val _isFetchingMore = MutableStateFlow(false)
    val isFetchingMore           = _isFetchingMore.asStateFlow()

    /** Progress label shown while loading older chunks or searching history. Null when idle. */
    private val _fetchProgress = MutableStateFlow<String?>(null)
    val fetchProgress           = _fetchProgress.asStateFlow()

    /** False once the API has returned an empty result set - no more history. */
    private val _hasMore = MutableStateFlow(true)
    val hasMore           = _hasMore.asStateFlow()

    private val _error = MutableStateFlow(false)
    val error           = _error.asStateFlow()

    private val _refreshError = MutableStateFlow(false)
    val refreshError           = _refreshError.asStateFlow()

    private var markedCached    = false
    private var searchFetchJob: Job? = null

    init { loadInitial() }

    private fun loadInitial() {
        viewModelScope.launch {
            // Serve the full history cache instantly so the list populates before any network call
            val cached = MessageHistoryCache.loadAll(app)
            if (cached.isNotEmpty()) {
                _allMessages.value    = cached
                _initialLoading.value = false
                AppCacheState.markCached()
                markedCached = true
            }

            // Also check the legacy 30-day cache in case history cache is still cold
            val legacyCached = AppCache.loadMessages(app)
            if (!legacyCached.isNullOrEmpty()) {
                mergeIn(legacyCached)
                if (_initialLoading.value) {
                    _initialLoading.value = false
                    if (!markedCached) { AppCacheState.markCached(); markedCached = true }
                }
            }

            val meta = MessageHistoryCache.loadMeta(app)
            _hasMore.value = !meta.reachedBeginning

            refreshRecent()
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshRecent() }
    }

    private suspend fun refreshRecent() {
        _isRefreshing.value = true
        _refreshError.value = false

        val to    = LocalDate.now()
        val from  = to.minusDays(30)
        val fresh = MessagesApi.fetchMessages(from, to)

        if (fresh != null) {
            mergeIn(fresh)
            _initialLoading.value = false
            MessageHistoryCache.saveBatch(app, fresh, from)
            AppCache.saveMessages(app, _allMessages.value.take(300))
            if (markedCached) { AppCacheState.markFresh(); markedCached = false }
        } else {
            AppCacheState.markOffline()
            if (_allMessages.value.isEmpty()) _error.value = true
            else                               _refreshError.value = true
        }

        _isRefreshing.value = false
    }

    fun loadMore() {
        if (_isFetchingMore.value || !_hasMore.value) return
        viewModelScope.launch {
            _isFetchingMore.value = true

            val meta = MessageHistoryCache.loadMeta(app)
            val to   = meta.oldestFetchedDate.minusDays(1)
            val from = to.minusDays(30)

            _fetchProgress.value = "Loading ${monthLabel(from)}..."
            Log.d("MsgVM", "loadMore: $from -> $to")

            val chunk = MessagesApi.fetchMessages(from, to)
            if (chunk != null) {
                val isLast = chunk.isEmpty()
                MessageHistoryCache.saveBatch(app, chunk, from, isLastChunk = isLast)
                mergeIn(chunk)
                _hasMore.value = !isLast
            }

            _fetchProgress.value  = null
            _isFetchingMore.value = false
        }
    }

    fun setSearch(query: String) {
        _searchQuery.value = query
        searchFetchJob?.cancel()
        searchFetchJob = null

        if (query.isBlank()) return

        val currentResults = _allMessages.value.filter { matches(it, query) }
        if (currentResults.isNotEmpty()) return

        val meta = MessageHistoryCache.loadMeta(app)
        if (meta.reachedBeginning) return

        searchFetchJob = viewModelScope.launch { fetchAllRemainingHistory(query) }
    }

    /** Fetches history month by month until a match for [query] is found or we reach the end. */
    private suspend fun fetchAllRemainingHistory(query: String) {
        var meta = MessageHistoryCache.loadMeta(app)

        while (!meta.reachedBeginning) {
            val to   = meta.oldestFetchedDate.minusDays(1)
            val from = to.minusDays(30)

            _fetchProgress.value = "Searching ${monthLabel(from)}..."
            Log.d("MsgVM", "searchFetch: $from -> $to")

            val chunk  = MessagesApi.fetchMessages(from, to) ?: break
            val isLast = chunk.isEmpty()
            MessageHistoryCache.saveBatch(app, chunk, from, isLastChunk = isLast)
            mergeIn(chunk)

            meta           = MessageHistoryCache.loadMeta(app)
            _hasMore.value = !meta.reachedBeginning

            if (chunk.any { matches(it, query) }) break
            if (isLast) break
        }

        _fetchProgress.value = null
    }

    /** Merges [incoming] into [_allMessages], deduplicating by ID and keeping newest-first order. */
    private fun mergeIn(incoming: List<EduMessage>) {
        val current = _allMessages.value
        val ids     = current.map { it.id }.toHashSet()
        val novel   = incoming.filter { it.id !in ids }
        if (novel.isEmpty()) return
        _allMessages.value = (current + novel).sortedByDescending { it.timestamp }
    }

    private fun matches(msg: EduMessage, query: String): Boolean {
        val q = query.trim().lowercase()
        return msg.sender.lowercase().contains(q) || msg.preview.lowercase().contains(q)
    }

    private fun monthLabel(date: LocalDate): String =
        date.format(DateTimeFormatter.ofPattern("MMM yyyy"))

    fun clearRefreshError() { _refreshError.value = false }

    override fun onCleared() {
        super.onCleared()
        if (markedCached) AppCacheState.markFresh()
    }

    companion object {
        fun relativeDate(dateStr: String): String = runCatching {
            val date  = LocalDate.parse(dateStr)
            val today = LocalDate.now()
            val days  = ChronoUnit.DAYS.between(date, today)
            when {
                days == 0L -> "Today"
                days == 1L -> "Yesterday"
                days < 7L  -> "${days}d ago"
                else       -> date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
            }
        }.getOrElse { dateStr }
    }
}
