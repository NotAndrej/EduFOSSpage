package org.edufosspage.ui.grades

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.edufosspage.data.AppCache
import org.edufosspage.data.AppCacheState
import org.edufosspage.data.GradesApi
import org.edufosspage.data.SubjectGrades
import java.time.LocalDate

class GradesViewModel(private val app: Application) : AndroidViewModel(app) {

    private val _selectedYear = MutableStateFlow(currentSchoolYear())
    val selectedYear = _selectedYear.asStateFlow()

    private val _selectedTerm = MutableStateFlow(currentTerm())
    val selectedTerm = _selectedTerm.asStateFlow()

    private val _availableYears = MutableStateFlow<List<Pair<Int, String>>>(emptyList())
    val availableYears = _availableYears.asStateFlow()

    private val _grades       = MutableStateFlow<List<SubjectGrades>?>(null)
    val grades = _grades.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _error        = MutableStateFlow(false)
    val error = _error.asStateFlow()

    private val _refreshError = MutableStateFlow(false)
    val refreshError = _refreshError.asStateFlow()

    private val cache       = mutableMapOf<String, List<SubjectGrades>>()
    private var yearsLoaded = false

    private var markedCached = false

    init { load() }

    fun load(force: Boolean = false) {
        val term = _selectedTerm.value
        val year = _selectedYear.value
        val key  = "$year-$term"

        if (!force && cache.containsKey(key)) { _grades.value = cache[key]; return }

        viewModelScope.launch {
            // Step 1: serve cache instantly
            if (_grades.value == null && !force) {
                val cached = AppCache.loadGrades(app, term, year)
                if (cached != null) {
                    cache[key]    = cached
                    _grades.value = cached
                    AppCacheState.markCached()
                    markedCached = true
                }
            }

            if (_grades.value != null) _isRefreshing.value = true
            _error.value        = false
            _refreshError.value = false

            if (!yearsLoaded) {
                _availableYears.value = GradesApi.fetchAvailableYears()
                yearsLoaded = true
            }

            val result = GradesApi.fetchGrades(term, year)
            if (result != null) {
                cache[key]    = result
                _grades.value = result
                AppCache.saveGrades(app, term, year, result)
                if (markedCached) {
                    AppCacheState.markFresh()
                    markedCached = false
                }
            } else {
                // Offline, keep cached data, signal soft error only if we had fresh before
                AppCacheState.markOffline()
                if (_grades.value != null) _refreshError.value = true
                else                       _error.value         = true
            }
            _isRefreshing.value = false
        }
    }

    fun selectYear(year: Int) {
        if (year == _selectedYear.value) return
        _selectedYear.value = year
        _grades.value = cache["$year-${_selectedTerm.value}"]
        load()
    }

    fun selectTerm(term: String) {
        if (term == _selectedTerm.value) return
        _selectedTerm.value = term
        _grades.value = cache["${_selectedYear.value}-$term"]
        load()
    }

    fun refresh() = load(force = true)
    fun clearRefreshError() { _refreshError.value = false }

    override fun onCleared() {
        super.onCleared()
        if (markedCached) AppCacheState.markFresh()
    }

    companion object {
        fun currentSchoolYear(): Int {
            val today = LocalDate.now()
            return if (today.monthValue >= 9) today.year else today.year - 1
        }
        fun currentTerm(): String {
            val month = LocalDate.now().monthValue
            return if (month in 2..8) "P2" else "P1"
        }
    }
}
