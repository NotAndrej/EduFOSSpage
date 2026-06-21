package org.edufosspage.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.edufosspage.data.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

class HomeViewModel(private val app: Application) : AndroidViewModel(app) {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _todayLessons  = MutableStateFlow<List<Lesson>?>(null)
    val todayLessons = _todayLessons.asStateFlow()

    private val _displayDate = MutableStateFlow(LocalDate.now())
    val displayDate = _displayDate.asStateFlow()

    private val _messages = MutableStateFlow<List<DashMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _grades = MutableStateFlow<List<DashGrade>>(emptyList())
    val grades = _grades.asStateFlow()

    private val _loading = MutableStateFlow(true)
    val loading = _loading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _timetables = MutableStateFlow<Map<String, List<Lesson>?>>(emptyMap())
    val timetables = _timetables.asStateFlow()

    private val _failedDates = MutableStateFlow<Set<String>>(emptySet())
    val failedDates = _failedDates.asStateFlow()

    private val _timetableSelectedDate = MutableStateFlow(LocalDate.now())
    val timetableSelectedDate = _timetableSelectedDate.asStateFlow()

    private val _timetableWeekStart = MutableStateFlow(
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    )
    val timetableWeekStart = _timetableWeekStart.asStateFlow()

    fun setTimetableSelectedDate(date: LocalDate) {
        _timetableSelectedDate.value = date
        _timetableWeekStart.value    = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    fun setTimetableWeekStart(weekStart: LocalDate) {
        _timetableWeekStart.value = weekStart
        val dayOffset = (_timetableSelectedDate.value.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong()
        _timetableSelectedDate.value = weekStart.plusDays(dayOffset)
    }

    private val inflightDates          = mutableSetOf<String>()
    private var dashboardLoaded        = false
    private var dashboardMarkedCached  = false

    fun loadDashboard(force: Boolean = false) {
        if (dashboardLoaded && !force) return
        viewModelScope.launch {
            if (!dashboardLoaded) {
                val cached = AppCache.loadDashboard(app)
                if (cached != null) {
                    val cachedDate = runCatching { LocalDate.parse(cached.displayDate) }.getOrElse { LocalDate.now() }
                    _displayDate.value  = cachedDate
                    _todayLessons.value = cached.lessons
                    _messages.value     = cached.messages
                    _grades.value       = cached.grades
                    _timetables.value   = _timetables.value + (cached.displayDate to cached.lessons)
                    _loading.value      = false
                    AppCacheState.markCached()
                    dashboardMarkedCached = true
                } else {
                    _loading.value = true
                }
            }

            if (dashboardLoaded) _isRefreshing.value = true

            var targetDate = LocalDate.now()
            var lessons    = EduApi.fetchTimetable(targetDate.format(fmt))

            if (lessons == null) {
                AppCacheState.markOffline()
                _loading.value      = false
                _isRefreshing.value = false
                dashboardLoaded     = true
                return@launch
            }

            if (lessons.isEmpty()) {
                for (i in 1..7) {
                    val next        = LocalDate.now().plusDays(i.toLong())
                    val nextLessons = EduApi.fetchTimetable(next.format(fmt))
                    if (!nextLessons.isNullOrEmpty()) { targetDate = next; lessons = nextLessons; break }
                }
            }

            _displayDate.value  = targetDate
            _todayLessons.value = lessons ?: emptyList()

            val key = targetDate.format(fmt)
            _timetables.value = _timetables.value + (key to (lessons ?: emptyList()))
            if (lessons != null) AppCache.saveTimetable(app, key, lessons)

            val freshMessages = EduApi.fetchRecentMessages()
            val freshGrades   = EduApi.fetchRecentGrades()
            _messages.value   = freshMessages
            _grades.value     = freshGrades

            AppCache.saveDashboard(app, lessons ?: emptyList(), key, freshMessages, freshGrades)

            dashboardLoaded     = true
            _loading.value      = false
            _isRefreshing.value = false
            if (dashboardMarkedCached) {
                AppCacheState.markFresh()
                dashboardMarkedCached = false
            }

            // Auto-switch to next day when last lesson ends
            scheduleNextDaySwitch(lessons ?: emptyList(), targetDate)
        }
    }

    // Waits until the last lesson ends then advances the display to the next day
    private fun scheduleNextDaySwitch(lessons: List<Lesson>, date: LocalDate) {
        if (date != LocalDate.now()) return
        if (lessons.isEmpty()) return

        val lastEndTime = lessons
            .mapNotNull { it.endTime.toLocalTimeOrNull() }
            .maxOrNull() ?: return

        viewModelScope.launch {
            val now     = LocalTime.now()
            val millis  = java.time.Duration.between(now, lastEndTime).toMillis()
            if (millis <= 0) {
                // Last lesson already ended, switch immediately
                advanceToNextDay(date)
                return@launch
            }
            delay(millis)
            advanceToNextDay(date)
        }
    }

    private fun advanceToNextDay(currentDate: LocalDate) {
        val next = currentDate.plusDays(1)
        _displayDate.value = next
        setTimetableSelectedDate(next)

        viewModelScope.launch {
            val key = next.format(fmt)
            val cachedLessons = AppCache.loadTimetable(app, key)
            if (cachedLessons != null) {
                _timetables.value   = _timetables.value + (key to cachedLessons)
                _todayLessons.value = cachedLessons
            }
            val fetched = EduApi.fetchTimetable(key)
            if (fetched != null) {
                _timetables.value   = _timetables.value + (key to fetched)
                _todayLessons.value = fetched
                AppCache.saveTimetable(app, key, fetched)
                // If next day also has no lessons (weekend / holiday), keep advancing
                scheduleNextDaySwitch(fetched, next)
            }
        }
    }

    fun refreshDashboard() = loadDashboard(force = true)

    fun loadDate(date: LocalDate) {
        val key = date.format(fmt)
        if (_timetables.value.containsKey(key)) return
        if (inflightDates.contains(key)) return
        inflightDates.add(key)
        viewModelScope.launch {
            val cached = AppCache.loadTimetable(app, key)
            if (cached != null) {
                _timetables.value  = _timetables.value + (key to cached)
                _failedDates.value = _failedDates.value - key
                inflightDates.remove(key)
                return@launch
            }
            val result = EduApi.fetchTimetable(key)
            if (result != null) {
                _timetables.value  = _timetables.value + (key to result)
                _failedDates.value = _failedDates.value - key
                AppCache.saveTimetable(app, key, result)
            } else {
                _failedDates.value = _failedDates.value + key
            }
            inflightDates.remove(key)
        }
    }

    fun retryDate(date: LocalDate) {
        _failedDates.value = _failedDates.value - date.format(fmt)
        loadDate(date)
    }

    override fun onCleared() {
        super.onCleared()
        if (dashboardMarkedCached) AppCacheState.markFresh()
    }
}

private fun String.toLocalTimeOrNull(): LocalTime? = runCatching {
    val p = split(":"); LocalTime.of(p[0].toInt(), p[1].toInt())
}.getOrNull()
