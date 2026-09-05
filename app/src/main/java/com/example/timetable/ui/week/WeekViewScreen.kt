package com.example.timetable.ui.week

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.timetable.data.db.CoursePhaseEntity
import com.example.timetable.ui.theme.colorForIndex
import com.example.timetable.ui.theme.fadedColor
import com.example.timetable.util.WeekCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekViewScreen(
    vm: WeekViewModel,
    onAdd: (dayOfWeek:Int, startMin:Int) -> Unit,
    onEdit: (Long) -> Unit,
    onOverview: () -> Unit,
    onSettings: () -> Unit,
    repoDeletePhase: suspend (CoursePhaseEntity) -> Unit,
    repoDeleteCourse: suspend (String) -> Unit
) {
    val state by vm.uiState.collectAsState()
    val semester = state.semester
    if (semester == null) {
        Box(Modifier.fillMaxSize().padding(24.dp)) {
            Column {
                Text("尚未创建学期", style = MaterialTheme.typography.titleLarge)
                Text("请到设置中创建学期（填写学期名、第一周周一日期、总周数）", modifier = Modifier.padding(top=8.dp))
                androidx.compose.material3.Button(onClick = onSettings, modifier = Modifier.padding(top=16.dp)) { Text("去设置") }
            }
        }
        return
    }
    var selectedPhase by remember { mutableStateOf<CoursePhaseEntity?>(null) }
    var overlapTopId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("第 ${state.weekIndex} 周") },
                actions = {
                    IconButton(onClick = onOverview) { Text("总览", fontSize = 14.sp) }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.MoreVert, contentDescription = "设置") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAdd(1, 8*60) }) { Icon(Icons.Default.Add, contentDescription = "添加") }
        }
    ) { padding ->
        val startMonday = LocalDate.ofInstant(Instant.ofEpochMilli(semester.startMondayMillis), ZoneId.systemDefault())
        val week = state.weekIndex
        // 7 columns
        val scroll = rememberScrollState()
        val vScroll = rememberScrollState()
        val hourHeight = 64.dp
        val dayNames = listOf("周一","周二","周三","周四","周五","周六","周日")
        // compute dates for header
        val dates = (1..7).map { WeekCalculator.dateForWeekAndDay(startMonday, week, it) }

        Column(Modifier.padding(padding).fillMaxSize()) {
            // header row with dates
            Row(Modifier.fillMaxWidth().horizontalScroll(scroll).padding(start = 48.dp)) {
                dayNames.forEachIndexed { idx, name ->
                    val d = dates[idx]
                    Column(Modifier.width(90.dp).padding(4.dp)) {
                        Text(name, style = MaterialTheme.typography.labelMedium)
                        Text("${d.monthValue}/${d.dayOfMonth}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            // week pager hint
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                androidx.compose.material3.TextButton(onClick = { vm.setWeek((week-1).coerceAtLeast(1)) }) { Text("上一周") }
                Spacer(Modifier.weight(1f))
                Text(WeekCalculator.formatWeekRange(startMonday, week), modifier = Modifier.padding(top=12.dp), style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = { vm.setWeek((week+1).coerceAtMost(semester.totalWeeks)) }) { Text("下一周") }
            }
            // grid
            val totalHours = (semester.displayEndMin - semester.displayStartMin) / 60
            val gridHeight = hourHeight * totalHours
            Row(Modifier.fillMaxSize()) {
                // time axis
                Column(Modifier.width(48.dp).verticalScroll(vScroll).height(gridHeight)) {
                    for (h in 0 until totalHours) {
                        val min = semester.displayStartMin + h*60
                        Box(Modifier.height(hourHeight).padding(top=2.dp)) {
                            Text("%02d:00".format(min/60), fontSize = 10.sp)
                        }
                    }
                }
                // days grid horizontally scrollable, vertically scrollable
                Box(Modifier.horizontalScroll(scroll).verticalScroll(vScroll).width(90.dp*7).height(gridHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.2f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { offset ->
                                // estimate day and time from offset
                                val day = (offset.x / (90.dp.toPx())).toInt().coerceIn(0,6) + 1
                                val minutesFromTop = (offset.y / hourHeight.toPx() * 60).toInt()
                                val startMin = (semester.displayStartMin + minutesFromTop).coerceIn(semester.displayStartMin, semester.displayEndMin-30)
                                // snap to 30min
                                val snapped = (startMin / 30)*30
                                onAdd(day, snapped)
                            }
                        )
                    }
                ) {
                    // hour lines
                    for (h in 0..totalHours) {
                        Box(Modifier.fillMaxWidth().height(1.dp).offset(y = hourHeight * h).background(MaterialTheme.colorScheme.outline.copy(alpha=0.15f)))
                    }
                    // vertical day lines
                    for (d in 0..7) {
                        Box(Modifier.width(1.dp).height(gridHeight).offset(x = 90.dp * d).background(MaterialTheme.colorScheme.outline.copy(alpha=0.15f)))
                    }
                    // group phases by day
                    val byDay = state.visiblePhases.groupBy { it.phase.dayOfWeek }
                    // handle overlap stacking: for each day, sort by startMin
                    for (day in 1..7) {
                        val list = byDay[day] ?: emptyList()
                        // for overlap detection, we will render in order; clicking toggles top
                        val sorted = list.sortedWith(compareBy({ it.phase.startMin }, { it.phase.id }))
                        sorted.forEachIndexed { idx, pw ->
                            val p = pw.phase
                            val isActive = pw.isActive
                            val topDp = ((p.startMin - semester.displayStartMin) / 60f * 64f).dp
                            val hDp = ((p.endMin - p.startMin) / 60f * 64f).dp
                            if (hDp <= 0.dp) return@forEachIndexed
                            // overlap offset: count overlaps before
                            val overlapCount = sorted.count { other ->
                                other.phase.startMin < p.endMin && other.phase.endMin > p.startMin
                            }
                            val isOverlapped = overlapCount > 1
                            val offsetX = if (isOverlapped) ( (idx % 2) * 8).dp else 0.dp
                            val offsetY = if (isOverlapped) ( (idx % 2) * 4).dp else 0.dp
                            val z = if (overlapTopId == p.id) 10f else idx.toFloat()
                            val bg = if (isActive) colorForIndex(p.colorIndex) else fadedColor(colorForIndex(p.colorIndex))
                            val x = 90.dp * (day-1) + offsetX + 2.dp
                            Card(
                                modifier = Modifier
                                    .width(88.dp - offsetX)
                                    .height(hDp)
                                    .offset(x = x, y = topDp + offsetY)
                                    .clickable {
                                        if (isOverlapped) {
                                            // toggle stacking
                                            overlapTopId = if (overlapTopId == p.id) null else p.id
                                        } else {
                                            selectedPhase = p
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = { onEdit(p.id) },
                                            onTap = {
                                                if (isOverlapped && overlapTopId != p.id) {
                                                    overlapTopId = p.id
                                                } else {
                                                    selectedPhase = p
                                                }
                                            }
                                        )
                                    },
                                colors = CardDefaults.cardColors(containerColor = bg),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isOverlapped) 4.dp else 1.dp)
                            ) {
                                Column(Modifier.padding(4.dp)) {
                                    val prefix = if (!isActive) "非本周 " else ""
                                    Text(prefix + p.courseName, fontSize = 11.sp, maxLines = 2, color = MaterialTheme.colorScheme.onPrimary)
                                    if (hDp > 28.dp) {
                                        Text(p.classroom ?: "", fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onPrimary.copy(alpha=0.9f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (selectedPhase != null) {
            val p = selectedPhase!!
            ModalBottomSheet(onDismissRequest = { selectedPhase = null }) {
                Column(Modifier.padding(16.dp).fillMaxWidth()) {
                    Text(p.courseName, style = MaterialTheme.typography.titleMedium)
                    Text("${p.teacher ?: "无教师"} · ${p.classroom ?: "无教室"}", style = MaterialTheme.typography.bodySmall)
                    if (p.note != null) Text(p.note, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=4.dp))
                    Text("时间: 周${p.dayOfWeek} %02d:%02d-%02d:%02d".format(p.startMin/60,p.startMin%60,p.endMin/60,p.endMin%60), modifier = Modifier.padding(top=8.dp))
                    Text("重复: ${p.repeatType} 起始${p.startWeek}周 " + if (p.endCondition.toString()=="UNTIL_WEEK") "截至${p.endWeek}周" else "共${p.repeatCount}次", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.padding(top=12.dp)) {
                        androidx.compose.material3.Button(onClick = { selectedPhase=null; onEdit(p.id) }) { Text("编辑此阶段") }
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.material3.OutlinedButton(onClick = { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).let { scope ->
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) { repoDeletePhase(p); kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){ selectedPhase=null } }
                        } }) { Text("删除此阶段") }
                    }
                    androidx.compose.material3.OutlinedButton(onClick = {
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch(kotlinx.coroutines.Dispatchers.IO) {
                            repoDeleteCourse(p.courseName)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main){ selectedPhase=null }
                        }
                    }, modifier = Modifier.padding(top=8.dp)) { Text("删除整门课 (${p.courseName})") }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launch(context: kotlin.coroutines.CoroutineContext, block: suspend () -> Unit) : kotlinx.coroutines.Job {
    return kotlinx.coroutines.launch(context) { block() }
}
