package com.example.timetable.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timetable.data.db.CoursePhaseEntity
import com.example.timetable.data.repo.TimetableRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class EditViewModel(private val repo: TimetableRepository) : ViewModel() {
    fun phaseFlow(id: Long?): Flow<CoursePhaseEntity?> {
        return if (id == null) kotlinx.coroutines.flow.flowOf(null) else kotlinx.coroutines.flow.flow {
            val p = repo.db.phaseDao().getById(id)
            emit(p)
        }
    }
    suspend fun getPhasesByName(semesterId: Long, name: String): List<CoursePhaseEntity> {
        return repo.getAllPhasesSync(semesterId).filter { it.courseName == name }
    }
    fun save(entity: CoursePhaseEntity, existing: CoursePhaseEntity?, onSaved: () -> Unit) {
        viewModelScope.launch {
            val oldName = existing?.courseName
            val newName = entity.courseName
            // if rename and existing not null, that phase becomes new name (detach). No extra handling needed except color sync.
            if (existing == null) {
                repo.insertPhase(entity)
            } else {
                repo.db.phaseDao().update(entity)
                // if renamed, don't affect other phases' color; if color changed and name unchanged, sync color to same-name phases
                if (oldName == newName && existing.colorIndex != entity.colorIndex) {
                    repo.updateCourseColor(entity.semesterId, newName, entity.colorIndex)
                }
            }
            // ensure color consistency for new phase: if same name exists, force color to match group? already inherited UI, but sync after insert
            if (existing == null) {
                val group = repo.getAllPhasesSync(entity.semesterId).filter { it.courseName == newName }
                if (group.size > 1) {
                    // sync to first color
                    val canonicalColor = group.first().colorIndex
                    if (entity.colorIndex != canonicalColor) {
                        repo.updateCourseColor(entity.semesterId, newName, canonicalColor)
                    }
                }
            }
            onSaved()
        }
    }
}
