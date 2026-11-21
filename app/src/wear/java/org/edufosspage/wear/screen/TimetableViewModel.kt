package org.edufosspage.wear.screen

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.edufosspage.data.AppCache
import org.edufosspage.data.EduApi
import org.edufosspage.data.Lesson
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Single-day timetable state for the watch. Mirrors the phone app's day loading
 * (cache first, then network) but without the dashboard/auto-advance machinery.
 */
class TimetableViewModel(private val app: Application) : AndroidViewModel(app) {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _date = MutableStateFlow(LocalDate.now())
    val date = _date.asStateFlow()

    /** date-key -> lessons. A null value means "still loading" for that day. */
    private val _lessons = MutableStateFlow<Map<String, List<Lesson>?>>(emptyMap())
    val lessons = _lessons.asStateFlow()

    private val _failed = MutableStateFlow<Set<String>>(emptySet())
    val failed = _failed.asStateFlow()

    private val inflight = mutableSetOf<String>()

    init { load(LocalDate.now()) }

    fun select(date: LocalDate) {
        _date.value = date
        load(date)
    }

    fun load(date: LocalDate, force: Boolean = false) {
        val key = date.format(fmt)
        if (!force && (_lessons.value.containsKey(key) || key in inflight)) return

        inflight.add(key)
        viewModelScope.launch {
            val cached = if (force) null else AppCache.loadTimetable(app, key)
            if (cached != null) _lessons.value = _lessons.value + (key to cached)

            val fresh = EduApi.fetchTimetable(key)
            if (fresh != null) {
                _lessons.value = _lessons.value + (key to fresh)
                _failed.value  = _failed.value - key
                AppCache.saveTimetable(app, key, fresh)
            } else if (cached == null) {
                _failed.value = _failed.value + key
            }
            inflight.remove(key)
        }
    }

    fun retry(date: LocalDate) {
        _failed.value = _failed.value - date.format(fmt)
        load(date, force = true)
    }
}
