package com.example.timetable

import android.app.Application
import androidx.room.Room
import com.example.timetable.data.db.AppDatabase
import com.example.timetable.data.prefs.Prefs
import com.example.timetable.data.repo.TimetableRepository

class TimetableApp : Application() {
    lateinit var db: AppDatabase private set
    lateinit var prefs: Prefs private set
    lateinit var repo: TimetableRepository private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "timetable.db")
            .fallbackToDestructiveMigration()
            .build()
        prefs = Prefs(this)
        repo = TimetableRepository(db, prefs)
    }
}
