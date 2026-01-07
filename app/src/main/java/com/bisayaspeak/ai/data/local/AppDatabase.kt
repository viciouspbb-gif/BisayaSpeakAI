package com.bisayaspeak.ai.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Question::class, UserProgress::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questionDao(): QuestionDao
    abstract fun userProgressDao(): UserProgressDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            val seedCallback = SeedCallback(context)
            val instance = Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "bisaya_speak_ai.db"
            )
                .fallbackToDestructiveMigration()
                .addCallback(seedCallback)
                .build()
            seedCallback.database = instance
            return instance
        }
    }

    private class SeedCallback(
        private val context: Context
    ) : RoomDatabase.Callback() {
        lateinit var database: AppDatabase

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            Log.d("AppDatabase", "Database created, initializing seed data")
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseInitializer.initialize(
                    context = context,
                    questionDao = database.questionDao(),
                    userProgressDao = database.userProgressDao()
                )
            }
        }
        
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            Log.d("AppDatabase", "Database opened, checking seed data")
            CoroutineScope(Dispatchers.IO).launch {
                DatabaseInitializer.initialize(
                    context = context,
                    questionDao = database.questionDao(),
                    userProgressDao = database.userProgressDao()
                )
            }
        }
    }
}
