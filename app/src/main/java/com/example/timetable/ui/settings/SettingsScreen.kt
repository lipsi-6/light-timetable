package com.example.timetable.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.timetable.data.db.SemesterEntity
import com.example.timetable.data.repo.TimetableRepository
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.ZoneId

@Serializable
data class ExportDto(
    val exportVersion: Int = 1,
    val exportedAt: String,
    val semesters: List<SemesterDto>,
    val phases: List<PhaseDto>
)
@Serializable
data class SemesterDto(val name: String, val startMonday: String, val totalWeeks: Int, val displayStartMin: Int, val displayEndMin: Int)
@Serializable
data class PhaseDto(
    val semesterName: String, val courseName: String, val colorIndex: Int, val dayOfWeek: Int,
    val startMin: Int, val endMin: Int, val teacher: String?, val classroom: String?, val note: String?,
    val repeatType: String, val intervalWeeks: Int, val startWeek: Int, val endCondition: String, val endWeek: Int?, val repeatCount: Int?
)

@Composable
fun SettingsScreen(
    repo: TimetableRepository,
    semesters: List<SemesterEntity>,
    hideNotThisWeek: Boolean,
    currentSemesterId: Long?,
    onSemesterChanged: () -> Unit,
    onExportJson: (Uri) -> Unit = {},
    onImportJson: (Uri) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var showAdd by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var startDateStr by remember { mutableStateOf(LocalDate.now().toString()) } // yyyy-MM-dd
    var totalWeeksStr by remember { mutableStateOf("20") }
    var displayStartStr by remember { mutableStateOf("08:00") }
    var displayEndStr by remember { mutableStateOf("22:00") }

    // export/import launchers delegating to MainActivity
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) onExportJson(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) onImportJson(uri)
    }

    LazyColumn(Modifier.padding(16.dp)) {
        item {
            Text("设置", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("隐藏非本周课程", modifier = Modifier.weight(1f))
                Switch(checked = hideNotThisWeek, onCheckedChange = { scope.launch { repo.setHideNotThisWeek(it) } })
            }
            Spacer(Modifier.height(16.dp))
            Text("学期管理", style = MaterialTheme.typography.titleMedium)
        }
        items(semesters.size) { idx ->
            val s = semesters[idx]
            Card(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(s.name + if (s.id == currentSemesterId) " · 当前" else "", style = MaterialTheme.typography.titleSmall)
                    val start = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(s.startMondayMillis), ZoneId.systemDefault())
                    Text("起始周一: $start  共${s.totalWeeks}周  显示 ${"%02d:%02d".format(s.displayStartMin/60,s.displayStartMin%60)}-${"%02d:%02d".format(s.displayEndMin/60,s.displayEndMin%60)}", style = MaterialTheme.typography.bodySmall)
                    Row(Modifier.padding(top=8.dp)) {
                        if (s.id != currentSemesterId) {
                            Button(onClick = { scope.launch { repo.setCurrentSemesterId(s.id); repo.db.semesterDao().clearCurrent(); repo.db.semesterDao().update(s.copy(isCurrent=true)) } }) { Text("设为当前") }
                            Spacer(Modifier.width(8.dp))
                        }
                        OutlinedButton(onClick = { scope.launch { repo.deleteSemester(s.id); if (currentSemesterId==s.id && semesters.size>1) { val next = semesters.firstOrNull{ it.id!=s.id}; if(next!=null) repo.setCurrentSemesterId(next.id) } } }) { Text("删除") }
                    }
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            if (!showAdd) {
                OutlinedButton(onClick = { showAdd = true }) { Text("新增学期") }
            } else {
                Card(Modifier.fillMaxWidth().padding(top=8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        OutlinedTextField(value = name, onValueChange={name=it}, label={ Text("学期名 如 2026春") }, modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(value = startDateStr, onValueChange={startDateStr=it}, label={ Text("第一周周一日期 yyyy-MM-dd") }, modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(value = totalWeeksStr, onValueChange={totalWeeksStr=it}, label={ Text("总周数") }, modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(value = displayStartStr, onValueChange={displayStartStr=it}, label={ Text("显示起点 HH:mm") }, modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(value = displayEndStr, onValueChange={displayEndStr=it}, label={ Text("显示终点 HH:mm") }, modifier=Modifier.fillMaxWidth())
                        Row(Modifier.padding(top=8.dp)) {
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        val start = LocalDate.parse(startDateStr)
                                        val sMin = parseHM(displayStartStr)
                                        val eMin = parseHM(displayEndStr)
                                        val millis = start.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                        val e = SemesterEntity(name=name, startMondayMillis=millis, totalWeeks=totalWeeksStr.toInt(), displayStartMin=sMin, displayEndMin=eMin, isCurrent=semesters.isEmpty())
                                        val id = repo.db.semesterDao().insert(e)
                                        if (semesters.isEmpty()) repo.setCurrentSemesterId(id)
                                        showAdd=false; name=""; onSemesterChanged()
                                    } catch (_: Exception) {}
                                }
                            }) { Text("保存") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick={showAdd=false}){ Text("取消") }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("备份与恢复", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.padding(top=8.dp)) {
                Button(onClick = { exportLauncher.launch("timetable_backup_${System.currentTimeMillis()}.json") }) { Text("导出 JSON") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) { Text("导入 JSON") }
            }
            Text("JSON 为完整备份格式（推荐），包含所有学期与阶段。若需表格编辑，可在二期扩展 CSV。", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top=4.dp))
            Spacer(Modifier.height(24.dp))
            Text("关于 · 轻量课程表 v1.0 · minSdk 31 (Android 12) · 纯离线", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun parseHM(s: String): Int {
    val p = s.split(":"); return p[0].toInt()*60 + p[1].toInt()
}

private suspend fun writeExport(repo: TimetableRepository, uri: Uri) {
    // This is called from composable scope with context; need ContentResolver - we capture via App instance? Simplified: use repo's application context via db? We'll need context param; for now no-op scaffold.
    // Actual implementation will be in MainActivity handing uri via ViewModel.
}
private suspend fun readImport(repo: TimetableRepository, uri: Uri) {
}
