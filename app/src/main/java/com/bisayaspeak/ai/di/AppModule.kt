package com.bisayaspeak.ai.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.bisayaspeak.ai.data.local.AppDatabase
import com.bisayaspeak.ai.data.local.QuestionDao
import com.bisayaspeak.ai.data.local.UserProgressDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
        return application.applicationContext
    }

    @Provides
    @Singleton
    fun provideAppDatabase(context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "bisayaspeak_db"
        )
        // 必要であればマイグレーション設定を追加
        // .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideQuestionDao(database: AppDatabase): QuestionDao {
        return database.questionDao()
    }

    @Provides
    fun provideUserProgressDao(database: AppDatabase): UserProgressDao {
        return database.userProgressDao()
    }
}
