package com.bisayaspeak.ai.data.repository

import android.content.Context
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.domain.xp.XpRewards

class MissionRepository(private val context: Context) {

    fun describeMissions(
        progress: DailyMissionProgress?,
        translatorAvailable: Boolean,
        isProUser: Boolean
    ): List<MissionDescriptor> {
        progress ?: return emptyList()
        return if (isProUser) {
            buildProMissions(progress)
        } else {
            buildLiteMissions(progress, translatorAvailable)
        }
    }

    private fun buildLiteMissions(
        progress: DailyMissionProgress,
        translatorAvailable: Boolean
    ): List<MissionDescriptor> {
        val missions = mutableListOf<MissionDescriptor>()
        missions += buildDescriptor(
            type = DailyMissionType.LISTENING,
            title = context.getString(R.string.home_daily_mission_listening_title),
            subtitle = context.getString(
                R.string.home_daily_mission_listening_subtitle,
                UsageRepository.LISTENING_TARGET
            ),
            current = progress.listeningCount,
            required = UsageRepository.LISTENING_TARGET,
            xpReward = XpRewards.LISTENING
        )

        if (translatorAvailable) {
            val completedViaFallback = progress.translatorFallbackGranted &&
                progress.translatorCount >= UsageRepository.TRANSLATOR_TARGET
            missions += buildDescriptor(
                type = DailyMissionType.TRANSLATOR,
                title = context.getString(R.string.home_daily_mission_translator_title),
                subtitle = context.getString(
                    R.string.home_daily_mission_translator_subtitle,
                    UsageRepository.TRANSLATOR_TARGET
                ),
                current = progress.translatorCount,
                required = UsageRepository.TRANSLATOR_TARGET,
                xpReward = XpRewards.TRANSLATOR,
                note = if (completedViaFallback) {
                    context.getString(R.string.home_daily_mission_translator_fallback)
                } else {
                    null
                }
            )
        } else {
            missions += buildDescriptor(
                type = DailyMissionType.LISTENING,
                title = context.getString(R.string.home_daily_mission_listening_fallback_title),
                subtitle = context.getString(
                    R.string.home_daily_mission_listening_fallback_subtitle,
                    UsageRepository.LISTENING_FALLBACK_THRESHOLD
                ),
                current = progress.listeningCount,
                required = UsageRepository.LISTENING_FALLBACK_THRESHOLD,
                xpReward = XpRewards.LISTENING_FALLBACK,
                note = context.getString(R.string.home_daily_mission_listening_fallback_note)
            )
        }
        return missions
    }

    private fun buildProMissions(progress: DailyMissionProgress): List<MissionDescriptor> {
        return listOf(
            buildDescriptor(
                type = DailyMissionType.SANPO,
                title = context.getString(R.string.home_daily_mission_sanpo_title),
                subtitle = context.getString(R.string.home_daily_mission_sanpo_subtitle),
                current = if (progress.sanpoCompleted) 1 else 0,
                required = 1,
                xpReward = XpRewards.SANPO
            ),
            buildDescriptor(
                type = DailyMissionType.LISTENING,
                title = context.getString(R.string.home_daily_mission_listening_title),
                subtitle = context.getString(
                    R.string.home_daily_mission_listening_subtitle,
                    UsageRepository.LISTENING_TARGET
                ),
                current = progress.listeningCount,
                required = UsageRepository.LISTENING_TARGET,
                xpReward = XpRewards.LISTENING
            )
        )
    }

    private fun buildDescriptor(
        type: DailyMissionType,
        title: String,
        subtitle: String,
        current: Int,
        required: Int,
        xpReward: Int,
        note: String? = null
    ): MissionDescriptor {
        val clippedCurrent = current.coerceIn(0, required)
        val fraction = if (required == 0) 1f else clippedCurrent.toFloat() / required
        val progressLabel = if (clippedCurrent >= required) {
            context.getString(R.string.home_daily_mission_progress_done)
        } else {
            context.getString(R.string.home_daily_mission_progress, clippedCurrent, required)
        }
        return MissionDescriptor(
            type = type,
            title = title,
            subtitle = subtitle,
            progressFraction = fraction.coerceIn(0f, 1f),
            progressLabel = progressLabel,
            isCompleted = clippedCurrent >= required,
            note = note,
            xpReward = xpReward
        )
    }
}

data class MissionDescriptor(
    val type: DailyMissionType?,
    val title: String,
    val subtitle: String,
    val progressFraction: Float,
    val progressLabel: String,
    val isCompleted: Boolean,
    val note: String?,
    val xpReward: Int
)
