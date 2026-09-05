package com.example.timetable.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "timetable_prefs")

class Prefs(private val context: Context) {
    private val KEY_CURRENT_SEMESTER = longPreferencesKey("current_semester_id")
    private val KEY_HIDE_NOT_THIS_WEEK = booleanPreferencesKey("hide_not_this_week")

    val currentSemesterIdFlow: Flow<Long?> = context.dataStore.data.map { it[KEY_CURRENT_SEMESTER] }
    val hideNotThisWeekFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_HIDE_NOT_THIS_WEEK] ?: false }

    suspend fun setCurrentSemesterId(id: Long) {
        context.dataStore.edit { it[KEY_CURRENT_SEMESTER] = id }
    }
    suspend fun setHideNotThisWeek(hide: Boolean) {
        context.dataStore.edit { it[KEY_HIDE_NOT_THIS_WEEK] = hide }
    }
}
