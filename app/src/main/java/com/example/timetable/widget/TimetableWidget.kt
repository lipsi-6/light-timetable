package com.example.timetable.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.example.timetable.TimetableApp
import com.example.timetable.data.db.CoursePhaseEntity
import com.example.timetable.util.RepeatExpander
import com.example.timetable.util.WeekCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class TimetableWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as TimetableApp
        val repo = app.repo
        // get current semester - need to collect first value synchronously; use db directly for widget (avoid Flow)
        val semesters = mutableListOf<com.example.timetable.data.db.SemesterEntity>()
        // fallback: try to get current via DataStore? Simplified: query db for isCurrent or first
        val all = try { app.db.semesterDao().getById(repo.prefs.currentSemesterIdFlow.let { null } ?: 0) } catch(_:Exception){null}
        // Actually load via repo.getAll? We'll try to fetch synchronously via db
        val semester = try {
            val list = app.db.semesterDao().let { dao ->
                // can't flow in widget easily, use direct query via Room allowMainThread? We'll query via db directly with suspend
                // Use repo.getSemesterSync if id known else find isCurrent
                val currentId = runCatching { kotlinx.coroutines.runBlocking { 
                    var v: Long? = null
                    kotlinx.coroutines.flow.firstOrNull(repo.currentSemesterIdFlow)?.let { v = it }
                    v
                } }.getOrNull()
                if (currentId != null) dao.getById(currentId) else null
            }
            list ?: app.db.let { 
                // fallback: any semester
                kotlinx.coroutines.runBlocking { 
                    val phases = app.db.semesterDao()
                    // brute: try to get first via flow
                    kotlinx.coroutines.flow.firstOrNull(repo.flowSemesters())?.firstOrNull()
                }
            }
        } catch (_:Exception) { null }

        provideContent {
            WidgetContent(context, semester)
        }
    }

    @Composable
    private fun WidgetContent(context: Context, semester: com.example.timetable.data.db.SemesterEntity?) {
        if (semester == null) {
            Column(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(day = androidx.compose.ui.graphics.Color(0xFFF5F5F5), night = androidx.compose.ui.graphics.Color(0xFF121212))).padding(12.dp)) {
                Text("课程表", style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Black, night = androidx.compose.ui.graphics.Color.White)))
                Text("请先创建学期", style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Gray, night = androidx.compose.ui.graphics.Color.LightGray)))
            }
            return
        }
        val app = context.applicationContext as TimetableApp
        val phases = kotlinx.coroutines.runBlocking { app.db.phaseDao().getAllBySemesterSync(semester.id) }
        val startMonday = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(semester.startMondayMillis), ZoneId.systemDefault())
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val weekToday = WeekCalculator.weekIndexForDate(startMonday, today).coerceIn(1, semester.totalWeeks)
        val weekTomorrow = WeekCalculator.weekIndexForDate(startMonday, tomorrow).coerceIn(1, semester.totalWeeks)
        val nowMin = LocalTime.now().hour*60 + LocalTime.now().minute

        fun phasesFor(date: LocalDate, week: Int): List<CoursePhaseEntity> {
            val dow = date.dayOfWeek.value // 1 Mon
            return phases.filter { it.dayOfWeek == dow && RepeatExpander.isActiveInWeek(it, week, semester.totalWeeks) }
                .sortedBy { it.startMin }
        }
        val todayPhases = phasesFor(today, weekToday).filter { it.endMin > nowMin }
        val tomorrowPhases = phasesFor(tomorrow, weekTomorrow)

        Column(modifier = GlanceModifier.fillMaxSize().background(ColorProvider(day = androidx.compose.ui.graphics.Color.White, night = androidx.compose.ui.graphics.Color(0xFF1E1E1E))).padding(8.dp)) {
            Text("${semester.name} · 第${weekToday}周", style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Gray, night = androidx.compose.ui.graphics.Color.LightGray)))
            Spacer(GlanceModifier.height(6.dp))
            // Today section
            Text("今日", style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Black, night = androidx.compose.ui.graphics.Color.White)))
            if (todayPhases.isEmpty()) {
                Text("今日无课", style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Gray, night = androidx.compose.ui.graphics.Color.LightGray)))
            } else {
                todayPhases.take(6).forEach { p ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Box(modifier = GlanceModifier.size(8.dp).background(colorProviderFor(p.colorIndex))) {}
                        Spacer(GlanceModifier.width(6.dp))
                        Text("%02d:%02d-%02d:%02d %s %s".format(p.startMin/60,p.startMin%60,p.endMin/60,p.endMin%60, p.courseName, p.classroom ?: ""), style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Black, night = androidx.compose.ui.graphics.Color.White)))
                    }
                }
            }
            Spacer(GlanceModifier.height(6.dp))
            Text("明日", style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Black, night = androidx.compose.ui.graphics.Color.White)))
            if (tomorrowPhases.isEmpty()) {
                Text("明日无课", style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Gray, night = androidx.compose.ui.graphics.Color.LightGray)))
            } else {
                tomorrowPhases.take(6).forEach { p ->
                    Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Box(modifier = GlanceModifier.size(8.dp).background(colorProviderFor(p.colorIndex))) {}
                        Spacer(GlanceModifier.width(6.dp))
                        Text("%02d:%02d-%02d:%02d %s %s".format(p.startMin/60,p.startMin%60,p.endMin/60,p.endMin%60, p.courseName, p.classroom ?: ""), style = TextStyle(color = ColorProvider(day = androidx.compose.ui.graphics.Color.Black, night = androidx.compose.ui.graphics.Color.White)))
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
        return ColorProvider(day = c, night = c)
    }
}

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}

// helper
private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstOrNull(): T? {
    var result: T? = null
    collect { result = it; throw kotlinx.coroutines.CancellationException() }
    return result
}
