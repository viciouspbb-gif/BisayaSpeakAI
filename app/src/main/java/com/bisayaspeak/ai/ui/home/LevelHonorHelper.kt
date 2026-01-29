package com.bisayaspeak.ai.ui.home

import android.content.Context
import androidx.annotation.StringRes
import com.bisayaspeak.ai.R
import kotlin.math.max

data class LevelHonorStrings(
    val level: Int,
    val title: String,
    val nickname: String
)

private data class LevelHonorEntry(
    val level: Int,
    @StringRes val titleRes: Int,
    @StringRes val nicknameRes: Int
)

object LevelHonorHelper {

    private val honorTable: List<LevelHonorEntry> = listOf(
        LevelHonorEntry(1, R.string.honor_title_1, R.string.honor_nickname_1),
        LevelHonorEntry(2, R.string.honor_title_2, R.string.honor_nickname_2),
        LevelHonorEntry(3, R.string.honor_title_3, R.string.honor_nickname_3),
        LevelHonorEntry(4, R.string.honor_title_4, R.string.honor_nickname_4),
        LevelHonorEntry(5, R.string.honor_title_5, R.string.honor_nickname_5),
        LevelHonorEntry(6, R.string.honor_title_6, R.string.honor_nickname_6),
        LevelHonorEntry(7, R.string.honor_title_7, R.string.honor_nickname_7),
        LevelHonorEntry(8, R.string.honor_title_8, R.string.honor_nickname_8),
        LevelHonorEntry(9, R.string.honor_title_9, R.string.honor_nickname_9),
        LevelHonorEntry(10, R.string.honor_title_10, R.string.honor_nickname_10),
        LevelHonorEntry(11, R.string.honor_title_11, R.string.honor_nickname_11),
        LevelHonorEntry(12, R.string.honor_title_12, R.string.honor_nickname_12),
        LevelHonorEntry(13, R.string.honor_title_13, R.string.honor_nickname_13),
        LevelHonorEntry(14, R.string.honor_title_14, R.string.honor_nickname_14),
        LevelHonorEntry(15, R.string.honor_title_15, R.string.honor_nickname_15),
        LevelHonorEntry(16, R.string.honor_title_16, R.string.honor_nickname_16),
        LevelHonorEntry(17, R.string.honor_title_17, R.string.honor_nickname_17),
        LevelHonorEntry(18, R.string.honor_title_18, R.string.honor_nickname_18),
        LevelHonorEntry(19, R.string.honor_title_19, R.string.honor_nickname_19),
        LevelHonorEntry(20, R.string.honor_title_20, R.string.honor_nickname_20),
        LevelHonorEntry(21, R.string.honor_title_21, R.string.honor_nickname_21),
        LevelHonorEntry(22, R.string.honor_title_22, R.string.honor_nickname_22),
        LevelHonorEntry(23, R.string.honor_title_23, R.string.honor_nickname_23),
        LevelHonorEntry(24, R.string.honor_title_24, R.string.honor_nickname_24),
        LevelHonorEntry(25, R.string.honor_title_25, R.string.honor_nickname_25),
        LevelHonorEntry(26, R.string.honor_title_26, R.string.honor_nickname_26),
        LevelHonorEntry(27, R.string.honor_title_27, R.string.honor_nickname_27),
        LevelHonorEntry(28, R.string.honor_title_28, R.string.honor_nickname_28),
        LevelHonorEntry(29, R.string.honor_title_29, R.string.honor_nickname_29),
        LevelHonorEntry(30, R.string.honor_title_30, R.string.honor_nickname_30)
    )

    fun getHonorInfo(context: Context, rawLevel: Int): LevelHonorStrings {
        val level = max(1, rawLevel).coerceAtMost(honorTable.last().level)
        val entry = honorTable.getOrNull(level - 1)
            ?: LevelHonorEntry(level, R.string.honor_title_default, R.string.honor_nickname_default)
        return LevelHonorStrings(
            level = level,
            title = context.getString(entry.titleRes),
            nickname = context.getString(entry.nicknameRes)
        )
    }
}
