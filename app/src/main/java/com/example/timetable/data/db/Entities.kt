package com.example.timetable.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class SemesterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startMondayMillis: Long, // LocalDate epochDay * 86400000 or millis at 00:00
    val totalWeeks: Int,
    val displayStartMin: Int = 8 * 60, // 08:00
    val displayEndMin: Int = 22 * 60,  // 22:00
    val isCurrent: Boolean = false
)

enum class RepeatType { EVERY_WEEK, ODD_WEEK, EVEN_WEEK, INTERVAL }
enum class EndCondition { UNTIL_WEEK, REPEAT_COUNT }

@Entity(
    tableName = "phases",
    foreignKeys = [ForeignKey(entity = SemesterEntity::class, parentColumns = ["id"], childColumns = ["semesterId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("semesterId"), Index("courseName")]
)
data class CoursePhaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterId: Long,
    val courseName: String,
    val colorIndex: Int, // 0..11
    val dayOfWeek: Int, // 1..7 (Mon..Sun)
    val startMin: Int,
    val endMin: Int,
    val teacher: String? = null,
    val classroom: String? = null,
    val note: String? = null,
    val repeatType: RepeatType = RepeatType.EVERY_WEEK,
    val intervalWeeks: Int = 1,
    val startWeek: Int = 1,
    val endCondition: EndCondition = EndCondition.UNTIL_WEEK,
    val endWeek: Int? = null,
    val repeatCount: Int? = null
)
