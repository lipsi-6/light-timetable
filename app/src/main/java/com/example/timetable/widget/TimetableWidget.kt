package com.example.timetable.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.timetable.TimetableApp
import com.example.timetable.util.RepeatExpander
import com.example.timetable.util.WeekCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.first

class TimetableWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TimetableApp
        val repo = app.repo
        var semester: com.example.timetable.data.db.SemesterEntity? = null
        var phases: List<com.example.timetable.data.db.CoursePhaseEntity> = emptyList()
        try {
            val currentId = repo.currentSemesterIdFlow.first()
            semester = if (currentId != null) app.db.semesterDao().getById(currentId) else null
            if (semester == null) {
                val list = repo.flowSemesters().first()
                semester = list.firstOrNull()
            }
            if (semester != null) {
                phases = app.db.phaseDao().getAllBySemesterSync(semester.id)
            }
        } catch (_: Exception) {}
        val s = semester
        val p = phases
        provideContent {
            WidgetContent(s, p)
        }
    }

    @Composable
    private fun WidgetContent(semester: com.example.timetable.data.db.SemesterEntity?, phases: List<com.example.timetable.data.db.CoursePhaseEntity>) {
        if (semester == null) {
            Column(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(androidx.compose.ui.graphics.Color(0xFFF5F5F5))).padding(12.dp)) {
                Text("课程表", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Black)))
                Text("请先创建学期", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Gray)))
            }
            return
        }
        val startMonday = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(semester.startMondayMillis), ZoneId.systemDefault())
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val weekToday = WeekCalculator.weekIndexForDate(startMonday, today).coerceIn(1, semester.totalWeeks)
        val weekTomorrow = WeekCalculator.weekIndexForDate(startMonday, tomorrow).coerceIn(1, semester.totalWeeks)
        val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute

        fun phasesFor(date: LocalDate, week: Int): List<com.example.timetable.data.db.CoursePhaseEntity> {
            val dow = date.dayOfWeek.value
            return phases.filter { it.dayOfWeek == dow && RepeatExpander.isActiveInWeek(it, week, semester.totalWeeks) }.sortedBy { it.startMin }
        }
        val todayPhases = phasesFor(today, weekToday).filter { it.endMin > nowMin }
        val tomorrowPhases = phasesFor(tomorrow, weekTomorrow)

        Column(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(androidx.compose.ui.graphics.Color.White)).padding(8.dp)) {
            Text("${semester.name} · 第${weekToday}周", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Gray)))
            Spacer(GlanceModifier.height(6.dp))
            Text("今日", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Black)))
            if (todayPhases.isEmpty()) {
                Text("今日无课", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Gray)))
            } else {
                todayPhases.take(6).forEach { ph ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Box(modifier = GlanceModifier.size(8.dp).background(colorProviderFor(ph.colorIndex))) {}
                        Spacer(GlanceModifier.width(6.dp))
                        Text("%02d:%02d-%02d:%02d %s %s".format(ph.startMin/60,ph.startMin%60,ph.endMin/60,ph.endMin%60, ph.courseName, ph.classroom ?: ""), style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Black)))
                    }
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            Text("明日", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Black)))
            if (tomorrowPhases.isEmpty()) {
                Text("明日无课", style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Gray)))
            } else {
                tomorrowPhases.take(6).forEach { ph ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Box(modifier = GlanceModifier.size(8.dp).background(colorProviderFor(ph.colorIndex))) {}
                        Spacer(GlanceModifier.width(6.dp))
                        Text("%02d:%02d-%02d:%02d %s %s".format(ph.startMin/60,ph.startMin%60,ph.endMin/60,ph.endMin%60, ph.courseName, ph.classroom ?: ""), style = TextStyle(color = ColorProvider(androidx.compose.ui.graphics.Color.Black)))
                    }
                }
            }
        }
    }

    private fun colorProviderFor(index: Int): ColorProvider {
        val palette = listOf(
            androidx.compose.ui.graphics.Color(0xFF4A90E2), androidx.compose.ui.graphics.Color(0xFF50C878),
            androidx.compose.ui.graphics.Color(0xFFFF6B6B), androidx.compose.ui.graphics.Color(0xFFFFD93D),
            androidx.compose.ui.graphics.Color(0xFF9B59B6), androidx.compose.ui.graphics.Color(0xFF1ABC9C),
            androidx.compose.ui.graphics.Color(0xFFFF8A65), androidx.compose.ui.graphics.Color(0xFF64B5F6),
            androidx.compose.ui.graphics.Color(0xFF81C784), androidx.compose.ui.graphics.Color(0xFFE57373),
            androidx.compose.ui.graphics.Color(0xFFBA68C8), androidx.compose.ui.graphics.Color(0xFFFFB74D)
        )
        val c = palette[index.mod(palette.size)]
        return ColorProvider(c)
    }
}

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}
