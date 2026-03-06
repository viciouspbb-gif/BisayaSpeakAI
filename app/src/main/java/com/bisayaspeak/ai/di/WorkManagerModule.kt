package com.bisayaspeak.ai.di

import com.bisayaspeak.ai.data.repository.TimeReleaseRepository
import com.bisayaspeak.ai.data.repository.FirebaseDataCleanupRepository
import com.bisayaspeak.ai.work.LevelReleaseScheduler
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * WorkManagerモジュール
 * タイムリリース関連のDI設定
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkManagerModule {
    
    @Provides
    @Singleton
    fun provideTimeReleaseRepository(): TimeReleaseRepository {
        return TimeReleaseRepository()
    }
    
    @Provides
    @Singleton
    fun provideFirebaseDataCleanupRepository(
        firebaseManager: com.bisayaspeak.ai.data.remote.FirebaseManager
    ): FirebaseDataCleanupRepository {
        return FirebaseDataCleanupRepository(firebaseManager)
    }
    
    @Provides
    @Singleton
    fun provideLevelReleaseScheduler(
        @ApplicationContext context: Context
    ): LevelReleaseScheduler {
        return LevelReleaseScheduler(context)
    }
}
