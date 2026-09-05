package com.example.timetable.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WeekCalculator {
    fun weekIndexForDate(startMonday: LocalDate, date: LocalDate): Int {
        val days = ChronoUnit.DAYS.between(startMonday, date)
        return (days / 7).toInt() + 1
    }

    fun dateForWeekAndDay(startMonday: LocalDate, week: Int, dayOfWeek: Int): LocalDate {
        // dayOfWeek 1..7 Mon..Sun
        return startMonday.plusDays(((week - 1) * 7 + (dayOfWeek - 1)).toLong())
    }

    fun currentWeek(startMonday: LocalDate, totalWeeks: Int, today: LocalDate = LocalDate.now()): Int {
        val w = weekIndexForDate(startMonday, today)
        return w.coerceIn(1, totalWeeks)
    }

    fun formatWeekRange(startMonday: LocalDate, week: Int): String {
        val monday = dateForWeekAndDay(startMonday, week, 1)
        val sunday = dateForWeekAndDay(startMonday, week, 7)
        return "${monday.monthValue}/${monday.dayOfMonth}-${sunday.monthValue}/${sunday.dayOfMonth}"
    }
}
