package com.example.timetable.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CoursePalette = listOf(
    Color(0xFF4A90E2), Color(0xFF50C878), Color(0xFFFF6B6B), Color(0xFFFFD93D),
    Color(0xFF9B59B6), Color(0xFF1ABC9C), Color(0xFFFF8A65), Color(0xFF64B5F6),
    Color(0xFF81C784), Color(0xFFE57373), Color(0xFFBA68C8), Color(0xFFFFB74D)
)

fun colorForIndex(index: Int): Color = CoursePalette[index.mod(CoursePalette.size)]
fun fadedColor(c: Color): Color = c.copy(alpha = 0.32f)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF4A90E2),
    secondary = Color(0xFF50C878)
)

@Composable
fun TimetableTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightScheme, content = content)
}
