package com.example.timetable.ui.overview

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timetable.data.db.CoursePhaseEntity
import com.example.timetable.ui.theme.colorForIndex
import com.example.timetable.util.RepeatExpander
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp

@Composable
fun OverviewScreen(
    distinctNames: List<String>,
    phases: List<CoursePhaseEntity>,
    totalWeeks: Int,
    onEditPhase: (Long) -> Unit
) {
    // group by courseName
    val grouped = phases.groupBy { it.courseName }
    var selectedCourse by remember { mutableStateOf<String?>(null) }

    if (selectedCourse == null) {
        LazyColumn(Modifier.padding(12.dp)) {
            items(distinctNames) { name ->
                val list = grouped[name] ?: emptyList()
                val color = if (list.isNotEmpty()) colorForIndex(list.first().colorIndex) else MaterialTheme.colorScheme.primary
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedCourse = name },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(12.dp)) {
                        androidx.compose.foundation.layout.Box(Modifier.size(16.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.titleMedium)
                            Text("${list.size} 个阶段 · ${list.firstOrNull()?.teacher ?: "无教师"}", style = MaterialTheme.typography.bodySmall)
                        }
                        Text(">", modifier = Modifier.padding(start=8.dp))
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)); Text("共 ${distinctNames.size} 门课程，${phases.size} 个阶段", style = MaterialTheme.typography.bodySmall) }
        }
    } else {
        val name = selectedCourse!!
        val list = grouped[name] ?: emptyList()
        Column(Modifier.padding(12.dp)) {
            androidx.compose.material3.TextButton(onClick = { selectedCourse = null }) { Text("← 返回全部") }
            Text(name, style = MaterialTheme.typography.titleLarge)
            Text("${list.size} 个阶段", style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.padding(top=8.dp)) {
                items(list) { p ->
                    Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable { onEditPhase(p.id) }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(RepeatExpander.description(p), fontSize = 13.sp)
                            Text(RepeatExpander.expandedWeeksText(p, totalWeeks), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            Row(Modifier.padding(top=4.dp)) {
                                Text("${p.classroom ?: "无教室"}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text("${p.teacher ?: ""}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (p.note != null) Text("备注: ${p.note}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
