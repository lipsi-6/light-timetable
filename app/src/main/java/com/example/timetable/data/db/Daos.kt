package com.example.timetable.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SemesterDao {
    @Query("SELECT * FROM semesters ORDER BY startMondayMillis DESC")
    fun flowAll(): Flow<List<SemesterEntity>>

    @Query("SELECT * FROM semesters WHERE isCurrent = 1 LIMIT 1")
    fun flowCurrent(): Flow<SemesterEntity?>

    @Query("SELECT * FROM semesters WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): SemesterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: SemesterEntity): Long

    @Update
    suspend fun update(e: SemesterEntity)

    @Query("UPDATE semesters SET isCurrent = 0")
    suspend fun clearCurrent()

    @Query("DELETE FROM semesters WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface PhaseDao {
    @Query("SELECT * FROM phases WHERE semesterId = :semesterId")
    fun flowBySemester(semesterId: Long): Flow<List<CoursePhaseEntity>>

    @Query("SELECT DISTINCT courseName FROM phases WHERE semesterId = :semesterId ORDER BY courseName")
    fun flowDistinctNames(semesterId: Long): Flow<List<String>>

    @Query("SELECT * FROM phases WHERE semesterId = :semesterId AND courseName = :name")
    fun flowByCourseName(semesterId: Long, name: String): Flow<List<CoursePhaseEntity>>

    @Query("SELECT * FROM phases WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CoursePhaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(e: CoursePhaseEntity): Long

    @Update
    suspend fun update(e: CoursePhaseEntity)

    @Delete
    suspend fun delete(e: CoursePhaseEntity)

    @Query("DELETE FROM phases WHERE semesterId = :sid AND courseName = :name")
    suspend fun deleteByCourseName(sid: Long, name: String)

    @Query("SELECT * FROM phases WHERE semesterId = :sid")
    suspend fun getAllBySemesterSync(sid: Long): List<CoursePhaseEntity>
}
