package com.example.timetable

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.timetable.data.db.EndCondition
import com.example.timetable.data.db.RepeatType
import com.example.timetable.data.db.SemesterEntity
import com.example.timetable.ui.edit.EditPhaseScreen
import com.example.timetable.ui.edit.EditViewModel
import com.example.timetable.ui.overview.OverviewScreen
import com.example.timetable.ui.settings.SettingsScreen
import com.example.timetable.ui.theme.TimetableTheme
import com.example.timetable.ui.week.WeekViewModel
import com.example.timetable.ui.week.WeekViewScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TimetableApp
        val repo = app.repo

        setContent {
            TimetableTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val nav = rememberNavController()
                    val scope = rememberCoroutineScope()
                    val semesters by repo.flowSemesters().collectAsState(initial = emptyList())
                    val currentId by repo.currentSemesterIdFlow.collectAsState(initial = null)
                    val hide by repo.hideNotThisWeekFlow.collectAsState(initial = false)
                    val currentSemester = semesters.find { it.id == currentId } ?: semesters.firstOrNull()

                    // week VM
                    val weekVm = remember { WeekViewModel(repo) }
                    val editVm = remember { EditViewModel(repo) }

                    // phases for overview/edit
                    val phases by (currentSemester?.let { repo.flowPhases(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
                    val distinctNames by (currentSemester?.let { repo.flowDistinctNames(it.id) } ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

                    // edit navigation args via state
                    var editPhaseId by remember { mutableStateOf<Long?>(null) }
                    var editInitialDay by remember { mutableStateOf(1) }
                    var editInitialStartMin by remember { mutableStateOf(8*60) }

                    NavHost(navController = nav, startDestination = "week") {
                        composable("week") {
                            // keep week sync with current semester
                            currentSemester?.let {
                                val today = LocalDate.now()
                                val startMonday = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.startMondayMillis), ZoneId.systemDefault())
                                val curWeek = com.example.timetable.util.WeekCalculator.currentWeek(startMonday, it.totalWeeks, today)
                                // set only once when semester changes? We'll launch effect
                                androidx.compose.runtime.LaunchedEffect(it.id) {
                                    weekVm.setWeek(curWeek)
                                }
                            }
                            WeekViewScreen(
                                vm = weekVm,
                                onAdd = { day, startMin ->
                                    editPhaseId = null
                                    editInitialDay = day
                                    editInitialStartMin = startMin
                                    nav.navigate("edit")
                                },
                                onEdit = { id ->
                                    editPhaseId = id
                                    nav.navigate("edit")
                                },
                                onOverview = { nav.navigate("overview") },
                                onSettings = { nav.navigate("settings") },
                                repoDeletePhase = { p -> repo.deletePhase(p) },
                                repoDeleteCourse = { name -> currentSemester?.let { repo.deleteByCourseName(it.id, name) } }
                            )
                        }
                        composable("edit") {
                            if (currentSemester == null) {
                                Box(Modifier.fillMaxSize()) { androidx.compose.material3.Text("请先创建学期") }
                            } else {
                                EditPhaseScreen(
                                    vm = editVm,
                                    phaseId = editPhaseId,
                                    initialDay = editInitialDay,
                                    initialStartMin = editInitialStartMin,
                                    semesterId = currentSemester.id,
                                    distinctNames = distinctNames,
                                    onSaved = {
                                        // update widget
                                        scope.launch { updateWidget() }
                                        nav.popBackStack()
                                    },
                                    onCancel = { nav.popBackStack() }
                                )
                            }
                        }
                        composable("overview") {
                            if (currentSemester == null) {
                                androidx.compose.material3.Text("暂无学期")
                            } else {
                                OverviewScreen(
                                    distinctNames = distinctNames,
                                    phases = phases,
                                    totalWeeks = currentSemester.totalWeeks,
                                    onEditPhase = { id -> editPhaseId = id; nav.navigate("edit") }
                                )
                            }
                        }
                        composable("settings") {
                            SettingsScreen(
                                repo = repo,
                                semesters = semesters,
                                hideNotThisWeek = hide,
                                currentSemesterId = currentId,
                                onSemesterChanged = { scope.launch { updateWidget() } },
                                onExportJson = { uri -> scope.launch { doExport(uri, repo) } },
                                onImportJson = { uri -> scope.launch { doImport(uri, repo) } }
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun updateWidget() {
        try {
            val manager = androidx.glance.appwidget.GlanceAppWidgetManager(this)
            val widget = com.example.timetable.widget.TimetableWidget()
            val ids = manager.getGlanceIds(widget::class.java)
            ids.forEach { widget.update(this, it) }
        } catch (_: Exception) {}
    }

    private suspend fun doExport(uri: Uri, repo: com.example.timetable.data.repo.TimetableRepository) {
        try {
            val semesters = kotlinx.coroutines.flow.first(repo.flowSemesters())
            val allPhases = mutableListOf<com.example.timetable.data.db.CoursePhaseEntity>()
            semesters.forEach { s -> allPhases.addAll(repo.getAllPhasesSync(s.id)) }
            val export = ExportDto(
                exportedAt = java.time.Instant.now().toString(),
                semesters = semesters.map {
                    val start = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(it.startMondayMillis), ZoneId.systemDefault()).toString()
                    SemesterDto(it.name, start, it.totalWeeks, it.displayStartMin, it.displayEndMin)
                },
                phases = allPhases.map { p ->
                    val sName = semesters.find { it.id == p.semesterId }?.name ?: ""
                    PhaseDto(sName, p.courseName, p.colorIndex, p.dayOfWeek, p.startMin, p.endMin, p.teacher, p.classroom, p.note, p.repeatType.name, p.intervalWeeks, p.startWeek, p.endCondition.name, p.endWeek, p.repeatCount)
                }
            )
            val json = Json { prettyPrint = true; encodeDefaults = true }.encodeToString(ExportDto.serializer(), export)
            contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private suspend fun doImport(uri: Uri, repo: com.example.timetable.data.repo.TimetableRepository) {
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return
            val dto = Json { ignoreUnknownKeys = true }.decodeFromString(ExportDto.serializer(), text)
            // clear or merge? For now append: create semesters if not exists by name
            dto.semesters.forEach { sDto ->
                val start = LocalDate.parse(sDto.startMonday)
                val millis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val existing = kotlinx.coroutines.flow.first(repo.flowSemesters()).find { it.name == sDto.name }
                val sid = if (existing != null) existing.id else {
                    val e = SemesterEntity(name=sDto.name, startMondayMillis=millis, totalWeeks=sDto.totalWeeks, displayStartMin=sDto.displayStartMin, displayEndMin=sDto.displayEndMin, isCurrent=false)
                    repo.db.semesterDao().insert(e)
                }
                dto.phases.filter { it.semesterName == sDto.name }.forEach { pDto ->
                    val entity = com.example.timetable.data.db.CoursePhaseEntity(
                        semesterId = sid,
                        courseName = pDto.courseName,
                        colorIndex = pDto.colorIndex,
                        dayOfWeek = pDto.dayOfWeek,
                        startMin = pDto.startMin, endMin = pDto.endMin,
                        teacher = pDto.teacher, classroom = pDto.classroom, note = pDto.note,
                        repeatType = try { RepeatType.valueOf(pDto.repeatType) } catch(_:Exception){ RepeatType.EVERY_WEEK },
                        intervalWeeks = pDto.intervalWeeks,
                        startWeek = pDto.startWeek,
                        endCondition = try { EndCondition.valueOf(pDto.endCondition) } catch(_:Exception){ EndCondition.UNTIL_WEEK },
                        endWeek = pDto.endWeek, repeatCount = pDto.repeatCount
                    )
                    repo.db.phaseDao().insert(entity)
                }
            }
            // if no current, set first
            val cur = kotlinx.coroutines.flow.first(repo.currentSemesterIdFlow)
            if (cur == null) {
                val first = kotlinx.coroutines.flow.first(repo.flowSemesters()).firstOrNull()
                if (first != null) repo.setCurrentSemesterId(first.id)
            }
            updateWidget()
        } catch (e: Exception) { e.printStackTrace() }
    }
}

@Serializable
data class ExportDto(val exportVersion: Int = 1, val exportedAt: String, val semesters: List<SemesterDto>, val phases: List<PhaseDto>)
@Serializable
data class SemesterDto(val name: String, val startMonday: String, val totalWeeks: Int, val displayStartMin: Int, val displayEndMin: Int)
@Serializable
data class PhaseDto(val semesterName: String, val courseName: String, val colorIndex: Int, val dayOfWeek: Int, val startMin: Int, val endMin: Int, val teacher: String?, val classroom: String?, val note: String?, val repeatType: String, val intervalWeeks: Int, val startWeek: Int, val endCondition: String, val endWeek: Int?, val repeatCount: Int?)

private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.first(): T {
    var result: T? = null
    collect { result = it; throw kotlinx.coroutines.CancellationException("first") }
    @Suppress("UNCHECKED_CAST") return result as T
}
