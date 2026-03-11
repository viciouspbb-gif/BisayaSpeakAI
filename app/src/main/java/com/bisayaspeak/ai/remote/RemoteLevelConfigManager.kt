package com.bisayaspeak.ai.remote

import android.util.Log
import com.bisayaspeak.ai.data.repository.LevelConfigRepository
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

class RemoteLevelConfigManager(
    private val levelConfigRepository: LevelConfigRepository
) {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    fun initialize() {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(MIN_FETCH_INTERVAL_SECONDS)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf(KEY_LATEST_MAX_LEVEL to LevelConfigRepository.DEFAULT_MAX_LEVEL.toLong())
        )
    }

    suspend fun fetchAndActivate() {
        runCatching {
            remoteConfig.fetchAndActivate().await()
            val latestMaxLevel = remoteConfig.getLong(KEY_LATEST_MAX_LEVEL).toInt()
            val updated = levelConfigRepository.updateMaxLevelIfHigher(latestMaxLevel)
            if (updated) {
                Log.d(TAG, "RemoteConfig pushed latest_max_level=$latestMaxLevel")
            }
        }.onFailure { throwable ->
            Log.e(TAG, "Failed to fetch Remote Config", throwable)
        }
    }

    companion object {
        private const val TAG = "RemoteLevelConfig"
        private const val KEY_LATEST_MAX_LEVEL = "latest_max_level"
        private const val MIN_FETCH_INTERVAL_SECONDS = 60L
    }
}
