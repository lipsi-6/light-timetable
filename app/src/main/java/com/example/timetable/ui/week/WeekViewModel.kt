package com.example.timetable.ui.week

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.db.CoursePhaseEntity
import com.example.timetable.data.db.SemesterEntity
import com.example.timetable.data.repo.TimetableRepository
import com.example.timetable.util.RepeatExpander
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeekUiState(
    val semester: SemesterEntity? = null,
    val weekIndex: Int = 1,
    val hideNotThisWeek: Boolean = false,
    val phases: List<CoursePhaseEntity> = emptyList(),
    val visiblePhases: List<PhaseWithActive> = emptyList(),
    val distinctNames: List<String> = emptyList()
)

data class PhaseWithActive(val phase: CoursePhaseEntity, val isActive: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
class WeekViewModel(private val repo: TimetableRepository) : ViewModel() {
    val currentWeek = MutableStateFlow(1)
    private val semesterIdFlow = repo.currentSemesterIdFlow

    private val semesterFlow = semesterIdFlow.flatMapLatest { id ->
        if (id == null) flowOf(null) else {
            repo.flowSemesters().map { list -> list.find { it.id == id } }
        }
    }

    val uiState: StateFlow<WeekUiState> = combine(
        semesterFlow,
        currentWeek,
        repo.hideNotThisWeekFlow,
        semesterIdFlow.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.flowPhases(id) },
        semesterIdFlow.flatMapLatest { id -> if (id == null) flowOf(emptyList()) else repo.flowDistinctNames(id) }
    ) { semester, week, hide, phases, names ->
        val totalWeeks = semester?.totalWeeks ?: 20
        val visible = phases.map { p ->
            PhaseWithActive(p, RepeatExpander.isActiveInWeek(p, week, totalWeeks))
        }.let { list ->
            if (hide) list.filter { it.isActive } else list
        }
        WeekUiState(semester as SemesterEntity?, week as Int, hide as Boolean, phases as List<CoursePhaseEntity>, visible, names as List<String>)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), WeekUiState())

    fun setWeek(w: Int) { currentWeek.value = w }
    fun setHide(h: Boolean) { viewModelScope.launch { repo.setHideNotThisWeek(h) } }
}
