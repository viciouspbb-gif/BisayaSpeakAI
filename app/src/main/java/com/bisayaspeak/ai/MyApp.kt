package com.bisayaspeak.ai

import android.app.Application
import android.util.Log
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.data.local.AppDatabase
import com.bisayaspeak.ai.data.repository.QuestionRepository
import com.bisayaspeak.ai.data.repository.UserProgressRepository
import com.bisayaspeak.ai.ui.ads.AdsPolicy

class MyApp : Application() {

    lateinit var questionRepository: QuestionRepository
        private set

    lateinit var userProgressRepository: UserProgressRepository
        private set

    override fun onCreate() {
        super.onCreate()

        Log.d("MyApp", "Application started")

        val database = AppDatabase.getInstance(this)
        questionRepository = QuestionRepository(database.questionDao())
        userProgressRepository = UserProgressRepository(database.userProgressDao())

        if (AdsPolicy.areAdsEnabled) {
            // 広告システムの準備スタート！
            AdManager.initialize(this)
        }
    }
}
