package com.example.timetable.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SemesterEntity::class, CoursePhaseEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun semesterDao(): SemesterDao
    abstract fun phaseDao(): PhaseDao
}
