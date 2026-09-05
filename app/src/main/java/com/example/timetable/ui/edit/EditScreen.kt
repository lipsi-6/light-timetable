package com.example.timetable.ui.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timetable.data.db.CoursePhaseEntity
import com.example.timetable.data.db.EndCondition
import com.example.timetable.data.db.RepeatType
import com.example.timetable.ui.theme.CoursePalette
import com.example.timetable.ui.theme.colorForIndex
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhaseScreen(
    vm: EditViewModel,
    phaseId: Long?, // null = new
    initialDay: Int = 1,
    initialStartMin: Int = 8*60,
    semesterId: Long,
    distinctNames: List<String>,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val existing by vm.phaseFlow(phaseId).collectAsState(initial = null)
    var courseName by remember { mutableStateOf("") }
    var colorIndex by remember { mutableStateOf(0) }
    var dayOfWeek by remember { mutableStateOf(initialDay) }
    var startH by remember { mutableStateOf(initialStartMin/60) }
    var startM by remember { mutableStateOf(initialStartMin%60) }
    var endH by remember { mutableStateOf((initialStartMin+90)/60) }
    var endM by remember { mutableStateOf((initialStartMin+90)%60) }
    var teacher by remember { mutableStateOf("") }
    var classroom by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var repeatType by remember { mutableStateOf(RepeatType.EVERY_WEEK) }
    var interval by remember { mutableStateOf(2) }
    var startWeek by remember { mutableStateOf(1) }
    var endCondition by remember { mutableStateOf(EndCondition.UNTIL_WEEK) }
    var endWeek by remember { mutableStateOf(16) }
    var repeatCount by remember { mutableStateOf(8) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(existing) {
        existing?.let { p ->
            courseName = p.courseName; colorIndex = p.colorIndex; dayOfWeek = p.dayOfWeek
            startH = p.startMin/60; startM = p.startMin%60; endH = p.endMin/60; endM = p.endMin%60
            teacher = p.teacher ?: ""; classroom = p.classroom ?: ""; note = p.note ?: ""
            repeatType = p.repeatType; interval = p.intervalWeeks; startWeek = p.startWeek
            endCondition = p.endCondition; endWeek = p.endWeek ?: 16; repeatCount = p.repeatCount ?: 8
        }
    }
    // auto-fill when picking existing name for new phase
    LaunchedEffect(courseName) {
        if (phaseId == null && courseName.isNotBlank()) {
            val same = vm.getPhasesByName(semesterId, courseName)
            if (same.isNotEmpty()) {
                val last = same.last()
                colorIndex = last.colorIndex
                teacher = last.teacher ?: teacher
                classroom = last.classroom ?: classroom
            }
        }
    }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(if (phaseId==null) "添加课程阶段" else "编辑阶段", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        // course name with dropdown
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = courseName, onValueChange = { courseName = it; expanded = true },
                label = { Text("课程名（同名即同课）") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                distinctNames.filter { it.contains(courseName, ignoreCase = true) || courseName.isBlank() }.take(8).forEach { name ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { courseName = name; expanded = false })
                }
            }
        }
        Text("选择后将作为该课程的新阶段，默认继承教师/教室/颜色", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Text("颜色（8-12 预设，同名课程同步）")
        Row(Modifier.padding(top=4.dp)) {
            CoursePalette.forEachIndexed { idx, c ->
                Box(Modifier.size(28.dp).padding(2.dp).clip(CircleShape).background(c).clickable { colorIndex = idx }
                    .then(if (idx==colorIndex) Modifier else Modifier)) {
                }
            }
        }
        // selected indicator
        Text("已选: ${colorIndex}", style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Row {
            OutlinedTextField(value = teacher, onValueChange = { teacher = it }, label = { Text("教师") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = classroom, onValueChange = { classroom = it }, label = { Text("教室") }, modifier = Modifier.weight(1f))
        }
        OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text("星期")
        Row {
            (1..7).forEach { d ->
                val label = listOf("","一","二","三","四","五","六","日")[d]
                androidx.compose.material3.FilterChip(selected = dayOfWeek==d, onClick = { dayOfWeek=d }, label = { Text(label) }, modifier = Modifier.padding(2.dp))
            }
        }
        Row {
            OutlinedTextField(value = startH.toString(), onValueChange = { startH = it.toIntOrNull() ?: startH }, label = { Text("开始时") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(value = startM.toString(), onValueChange = { startM = it.toIntOrNull() ?: startM }, label = { Text("分") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(value = endH.toString(), onValueChange = { endH = it.toIntOrNull() ?: endH }, label = { Text("结束时") }, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(4.dp))
            OutlinedTextField(value = endM.toString(), onValueChange = { endM = it.toIntOrNull() ?: endM }, label = { Text("分") }, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Text("重复规则")
        Row {
            listOf(RepeatType.EVERY_WEEK to "每周", RepeatType.ODD_WEEK to "单周", RepeatType.EVEN_WEEK to "双周", RepeatType.INTERVAL to "间隔").forEach { (t,l) ->
                androidx.compose.material3.FilterChip(selected = repeatType==t, onClick = { repeatType=t }, label={ Text(l)}, modifier=Modifier.padding(2.dp))
            }
        }
        if (repeatType== RepeatType.INTERVAL) {
            OutlinedTextField(value = interval.toString(), onValueChange = { interval = it.toIntOrNull()?.coerceIn(1,8) ?: interval }, label={ Text("间隔周数 N") }, modifier=Modifier.width(140.dp))
        }
        Row {
            OutlinedTextField(value = startWeek.toString(), onValueChange = { startWeek = it.toIntOrNull() ?: startWeek }, label={ Text("起始周") }, modifier=Modifier.weight(1f))
        }
        Row(Modifier.padding(top=8.dp)) {
            Row(Modifier.clickable{ endCondition = EndCondition.UNTIL_WEEK }.padding(4.dp)) { RadioButton(selected = endCondition== EndCondition.UNTIL_WEEK, onClick={endCondition= EndCondition.UNTIL_WEEK}); Text("截止到第 X 周") }
            Row(Modifier.clickable{ endCondition = EndCondition.REPEAT_COUNT }.padding(4.dp)) { RadioButton(selected = endCondition== EndCondition.REPEAT_COUNT, onClick={endCondition= EndCondition.REPEAT_COUNT}); Text("重复 N 次") }
        }
        if (endCondition== EndCondition.UNTIL_WEEK) {
            OutlinedTextField(value = endWeek.toString(), onValueChange={ endWeek = it.toIntOrNull()?: endWeek }, label={ Text("截止周") }, modifier=Modifier.fillMaxWidth())
        } else {
            OutlinedTextField(value = repeatCount.toString(), onValueChange={ repeatCount = it.toIntOrNull()?: repeatCount }, label={ Text("重复次数") }, modifier=Modifier.fillMaxWidth())
        }
        val startMin = startH*60+startM
        val endMin = endH*60+endM
        val preview = buildString {
            append(if (repeatType== RepeatType.INTERVAL) "每${interval}周 " else when(repeatType){ RepeatType.EVERY_WEEK->"每周 "; RepeatType.ODD_WEEK->"单周 "; RepeatType.EVEN_WEEK->"双周 "; else->""})
            append("周${dayOfWeek} %02d:%02d-%02d:%02d ".format(startH,startM,endH,endM))
            append(if (endCondition== EndCondition.UNTIL_WEEK) "第${startWeek}-${endWeek}周" else "从第${startWeek}周起 共${repeatCount}次")
        }
        Text("预览: $preview", style=MaterialTheme.typography.bodySmall, modifier=Modifier.padding(top=8.dp))
        if (endMin <= startMin) Text("结束时间必须晚于开始时间", color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = {
                if (courseName.isBlank() || endMin <= startMin) return@Button
                val entity = CoursePhaseEntity(
                    id = existing?.id ?: 0,
                    semesterId = semesterId,
                    courseName = courseName.trim(),
                    colorIndex = colorIndex,
                    dayOfWeek = dayOfWeek,
                    startMin = startMin, endMin = endMin,
                    teacher = teacher.ifBlank{ null }, classroom = classroom.ifBlank{ null }, note = note.ifBlank{ null },
                    repeatType = repeatType, intervalWeeks = interval, startWeek = startWeek,
                    endCondition = endCondition, endWeek = if (endCondition== EndCondition.UNTIL_WEEK) endWeek else null,
                    repeatCount = if (endCondition== EndCondition.REPEAT_COUNT) repeatCount else null
                )
                vm.save(entity, existing, onSaved)
            }, modifier=Modifier.weight(1f)) { Text("保存") }
            Spacer(Modifier.width(8.dp))
            androidx.compose.material3.OutlinedButton(onClick = onCancel, modifier=Modifier.weight(1f)) { Text("取消") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
