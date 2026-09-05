package com.example.timetable.util

import com.example.timetable.data.db.CoursePhaseEntity
import com.example.timetable.data.db.EndCondition
import com.example.timetable.data.db.RepeatType

object RepeatExpander {
    fun expandWeeks(phase: CoursePhaseEntity, totalWeeks: Int): Set<Int> {
        val result = mutableSetOf<Int>()
        var w = phase.startWeek
        var count = 0
        val maxWeek = if (phase.endCondition == EndCondition.UNTIL_WEEK) (phase.endWeek ?: totalWeeks) else Int.MAX_VALUE
        val maxCount = if (phase.endCondition == EndCondition.REPEAT_COUNT) (phase.repeatCount ?: Int.MAX_VALUE) else Int.MAX_VALUE
        // safety guard
        var iter = 0
        while (w <= maxWeek && count < maxCount && w <= totalWeeks && iter < 1000) {
            iter++
            when (phase.repeatType) {
                RepeatType.EVERY_WEEK -> {
                    result.add(w); w += 1; count += 1
                }
                RepeatType.ODD_WEEK, RepeatType.EVEN_WEEK -> {
                    val isOdd = w % 2 == 1
                    val wantOdd = phase.repeatType == RepeatType.ODD_WEEK
                    if (isOdd == wantOdd) { result.add(w); count += 1 }
                    w += 1
                }
                RepeatType.INTERVAL -> {
                    result.add(w); count += 1; w += phase.intervalWeeks.coerceAtLeast(1)
                }
            }
        }
        return result
    }

    fun isActiveInWeek(phase: CoursePhaseEntity, week: Int, totalWeeks: Int): Boolean {
        return week in expandWeeks(phase, totalWeeks)
    }

    fun description(phase: CoursePhaseEntity): String {
        val dayMap = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        val time = "%02d:%02d-%02d:%02d".format(phase.startMin/60, phase.startMin%60, phase.endMin/60, phase.endMin%60)
        val repeat = when (phase.repeatType) {
            RepeatType.EVERY_WEEK -> "每周"
            RepeatType.ODD_WEEK -> "单周"
            RepeatType.EVEN_WEEK -> "双周"
            RepeatType.INTERVAL -> "每${phase.intervalWeeks}周"
        }
        val end = when (phase.endCondition) {
            EndCondition.UNTIL_WEEK -> "1-${phase.endWeek}周".let { "第${phase.startWeek}-${phase.endWeek}周" }
            EndCondition.REPEAT_COUNT -> "从第${phase.startWeek}周起 共${phase.repeatCount}次"
        }
        // expanded preview
        return "$repeat ${dayMap[phase.dayOfWeek]} $time $end"
    }

    fun expandedWeeksText(phase: CoursePhaseEntity, totalWeeks: Int): String {
        val set = expandWeeks(phase, totalWeeks).sorted()
        return if (set.size <= 12) set.joinToString(",") else set.take(12).joinToString(",") + "...共${set.size}次"
    }
}
