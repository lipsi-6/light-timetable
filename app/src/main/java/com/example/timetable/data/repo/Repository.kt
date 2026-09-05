package com.example.timetable.data.repo

import com.example.timetable.data.db.AppDatabase
import com.example.timetable.data.db.CoursePhaseEntity
import com.example.timetable.data.db.SemesterEntity
import com.example.timetable.data.prefs.Prefs
import kotlinx.coroutines.flow.Flow

class TimetableRepository(val db: AppDatabase, val prefs: Prefs) {
    fun flowSemesters() = db.semesterDao().flowAll()
    fun flowCurrentSemester() = db.semesterDao().flowCurrent()
    fun flowPhases(semesterId: Long) = db.phaseDao().flowBySemester(semesterId)
    fun flowDistinctNames(semesterId: Long) = db.phaseDao().flowDistinctNames(semesterId)

    val currentSemesterIdFlow: Flow<Long?> = prefs.currentSemesterIdFlow
    val hideNotThisWeekFlow: Flow<Boolean> = prefs.hideNotThisWeekFlow

    suspend fun setCurrentSemesterId(id: Long) { prefs.setCurrentSemesterId(id) }
    suspend fun setHideNotThisWeek(h: Boolean) { prefs.setHideNotThisWeek(h) }

    suspend fun insertSemester(e: SemesterEntity): Long {
        val id = db.semesterDao().insert(e)
        // if first semester or isCurrent, set prefs
        if (e.isCurrent || id != 0L) {
            // clear others if isCurrent
            if (e.isCurrent) {
                db.semesterDao().clearCurrent()
                db.semesterDao().update(e.copy(id = id, isCurrent = true))
                prefs.setCurrentSemesterId(id)
            }
        }
        return id
    }
    suspend fun updateSemester(e: SemesterEntity) = db.semesterDao().update(e)
    suspend fun deleteSemester(id: Long) = db.semesterDao().deleteById(id)

    suspend fun insertPhase(e: CoursePhaseEntity) = db.phaseDao().insert(e)
    suspend fun updatePhase(e: CoursePhaseEntity) = db.phaseDao().update(e)
    suspend fun deletePhase(e: CoursePhaseEntity) = db.phaseDao().delete(e)
    suspend fun deleteByCourseName(sid: Long, name: String) = db.phaseDao().deleteByCourseName(sid, name)

    suspend fun getSemesterSync(id: Long) = db.semesterDao().getById(id)
    suspend fun getAllPhasesSync(sid: Long) = db.phaseDao().getAllBySemesterSync(sid)

    suspend fun updateCourseColor(semesterId: Long, courseName: String, colorIndex: Int) {
        val all = db.phaseDao().getAllBySemesterSync(semesterId).filter { it.courseName == courseName }
        all.forEach { db.phaseDao().update(it.copy(colorIndex = colorIndex)) }
    }
}
