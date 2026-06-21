package org.edufosspage.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object DeepLinkState {
    const val TAB_MESSAGES  = 3
    const val TAB_GRADES    = 2
    const val TAB_TIMETABLE = 1

    private val _pendingTab = MutableStateFlow<Int?>(null)
    val pendingTab = _pendingTab.asStateFlow()

    /** Post a tab to open. The UI collects this and navigates, then calls [consume]. */
    fun request(tab: Int) { _pendingTab.value = tab }

    /** Call after the UI has navigated so the event isn't re-consumed on recomposition. */
    fun consume() { _pendingTab.value = null }
}
