package com.bisayaspeak.ai.ui.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bisayaspeak.ai.BuildConfig
import com.bisayaspeak.ai.R
import com.bisayaspeak.ai.ads.AdManager
import com.bisayaspeak.ai.data.model.TranslationDirection
import com.bisayaspeak.ai.data.repository.FreeUsageManager
import com.bisayaspeak.ai.data.repository.FreeUsageRepository
import com.bisayaspeak.ai.data.repository.OpenAiChatRepository
import com.bisayaspeak.ai.data.repository.PromptProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

sealed interface TranslatorUiState {
    object Idle : TranslatorUiState
    object Loading : TranslatorUiState
    data class Error(val message: String) : TranslatorUiState
    object Success : TranslatorUiState
}

data class TranslatorUsageStatus(
    val dayKey: String,
    val usedCount: Int,
    val maxCount: Int,
    val canUse: Boolean
)

data class TranslatorCandidate(
    val bisaya: String,
    val japanese: String,
    val english: String,
    val politeness: String,
    val situation: String,
    val nuance: String,
    val tip: String
)

data class TranslatorExplanation(
    val summary: String,
    val usage: String,
    val relatedPhrases: List<String>
)

data class TranslatorCandidateDisplay(
    val candidate: TranslatorCandidate,
    val politenessLabel: String?,
    val situationLabel: String?
)

private data class TranslatorPrompt(
    val systemPrompt: String,
    val userPrompt: String
)

data class TranslatorDebugState(
    val isPremiumUser: Boolean = false,
    val dayKey: String = FreeUsageManager.currentDayKey(),
    val translateCount: Int = 0,
    val translateLimit: Int = FreeUsageRepository.MAX_TRANSLATE_PER_DAY,
    val shouldShowAdNow: Boolean = false,
    val adAttempted: Boolean = false,
    val adResult: String = "idle",
    val skipReason: String? = null,
    val lastUpdatedAt: String = ""
)

sealed interface TranslatorEvent {
    data class RequireAd(val translateCount: Int) : TranslatorEvent
    data class ShowToast(@StringRes val messageResId: Int) : TranslatorEvent
    data object ShowUpsell : TranslatorEvent
}

class AiTranslatorViewModel(
    private val repository: OpenAiChatRepository = OpenAiChatRepository(),
    private val promptProvider: PromptProvider? = null
) : ViewModel() {

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    private val _candidates = MutableStateFlow<List<TranslatorCandidate>>(emptyList())
    val candidates: StateFlow<List<TranslatorCandidate>> = _candidates.asStateFlow()

    private val _explanation = MutableStateFlow<TranslatorExplanation?>(null)
    val explanation: StateFlow<TranslatorExplanation?> = _explanation.asStateFlow()

    private val _direction = MutableStateFlow(TranslationDirection.JA_TO_CEB)
    val direction: StateFlow<TranslationDirection> = _direction.asStateFlow()

    private val _uiState = MutableStateFlow<TranslatorUiState>(TranslatorUiState.Idle)
    val uiState: StateFlow<TranslatorUiState> = _uiState.asStateFlow()

    private val _usageStatus = MutableStateFlow<TranslatorUsageStatus?>(null)
    val usageStatus: StateFlow<TranslatorUsageStatus?> = _usageStatus.asStateFlow()

    private val _events = MutableSharedFlow<TranslatorEvent>()
    val events: SharedFlow<TranslatorEvent> = _events.asSharedFlow()

    private val _debugState = MutableStateFlow(TranslatorDebugState())
    val debugState: StateFlow<TranslatorDebugState> = _debugState.asStateFlow()

    private var pendingAdLog: PendingAdLog? = null

    private val debugZoneId: ZoneId = ZoneId.systemDefault()
    private val debugTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    init {
        viewModelScope.launch {
            FreeUsageManager.resetIfNewDay()
            refreshUsageStatus()
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun swapDirection() {
        _direction.value = when (_direction.value) {
            TranslationDirection.JA_TO_CEB -> TranslationDirection.CEB_TO_JA
            TranslationDirection.CEB_TO_JA -> TranslationDirection.JA_TO_CEB
        }
        _translatedText.value = ""
        _uiState.value = TranslatorUiState.Idle
    }

    fun clearAll() {
        _inputText.value = ""
        _translatedText.value = ""
        _uiState.value = TranslatorUiState.Idle
    }

    fun translate(isPremiumUser: Boolean) {
        val text = _inputText.value.trim()
        if (text.isEmpty()) {
            _uiState.value = TranslatorUiState.Error("Please enter text to translate")
            return
        }

        // Prevent concurrent translation requests
        if (_uiState.value is TranslatorUiState.Loading) {
            Log.d(LOG_TAG, "translate skipped - already loading")
            return
        }

        _direction.value = determineDirectionFromInput(text)
        Log.d(LOG_TAG, "translate start premium=$isPremiumUser direction=${_direction.value}")

        viewModelScope.launch {
            // 全ユーザーをprepareTranslateAttempt経由に統一
            val attemptContext = prepareTranslateAttempt(isPremiumUser) ?: return@launch

            _uiState.value = TranslatorUiState.Loading
            var translationFailed = false
            try {
                val direction = _direction.value
                val raw = translateWithOpenAi(text, direction)
                val payload = parseTranslatorPayload(raw)
                _candidates.value = payload.candidates
                _explanation.value = payload.explanation
                val primary = payload.candidates.firstOrNull()?.bisaya.orEmpty()
                _translatedText.value = if (primary.isNotBlank()) primary else sanitizeTranslation(raw, direction)
                _uiState.value = TranslatorUiState.Success
                Log.d(LOG_TAG, "translate success candidates=${payload.candidates.size}")
            } catch (e: Exception) {
                translationFailed = true
                Log.e(LOG_TAG, "translate failed", e)
                _uiState.value = TranslatorUiState.Error(e.message ?: "Translation failed")
            }

            attemptContext.let {
                handleTranslateAttemptCompletion(
                    attempt = it,
                    translationFailed = translationFailed
                )
            }
        }
    }

    fun onAdResult(result: AdManager.InterstitialAttemptResult) {
        val pending = pendingAdLog
        if (pending != null && !pending.deferred.isCompleted) {
            pending.deferred.complete(result)
            val snapshot = UsageSnapshot(
                dayKey = pending.context.dayKey,
                count = pending.context.countAfter,
                limit = pending.context.maxCount
            )
            val (resultLabel, skipReason) = when (result) {
                AdManager.InterstitialAttemptResult.SHOWN -> "shown" to null
                AdManager.InterstitialAttemptResult.NOT_READY -> "not_ready" to "ad_not_ready"
                AdManager.InterstitialAttemptResult.FAILED -> "failed" to "show_failed"
                AdManager.InterstitialAttemptResult.SKIPPED_BY_POLICY -> "skipped" to "ads_disabled"
            }
            Log.d(LOG_TAG, "ad show result=${result.name} count=${pending.context.countAfter}")
            publishDebugState(
                snapshot = snapshot,
                isPremiumUser = pending.context.isPremiumUser,
                shouldShowAd = pending.context.shouldShowAd,
                adAttempted = true,
                adResult = resultLabel,
                skipReason = skipReason
            )
        }
    }

    private suspend fun refreshUsageStatus() {
        val day = FreeUsageManager.dayKey() ?: FreeUsageManager.currentDayKey()
        val used = FreeUsageManager.translateCount()
        val max = FreeUsageRepository.MAX_TRANSLATE_PER_DAY
        val canUse = used < max
        _usageStatus.value = TranslatorUsageStatus(
            dayKey = day,
            usedCount = used,
            maxCount = max,
            canUse = canUse
        )
    }

    private suspend fun translateWithOpenAi(
        text: String,
        direction: TranslationDirection
    ): String {
        val prompt = buildTranslatorPrompt(text, direction)
        return repository.generateJsonResponse(
            userPrompt = prompt.userPrompt,
            temperature = 0.35,
            systemPrompt = prompt.systemPrompt
        )
    }

    private fun buildTranslatorPrompt(
        text: String,
        direction: TranslationDirection
    ): TranslatorPrompt {
        val deviceLanguage = Locale.getDefault().language.lowercase(Locale.ROOT)
        val uiLangCode = if (deviceLanguage == "ja") "ja" else "en"
        val uiLangName = if (uiLangCode == "ja") "Japanese" else "English"
        val basePrompt = promptProvider?.getSystemPrompt().orEmpty()
        val directionInstruction = when (direction) {
            TranslationDirection.JA_TO_CEB -> "Convert the user's Japanese or English text into rich Cebuano (Bisaya) expressions."
            TranslationDirection.CEB_TO_JA -> "Review the user's Cebuano text and produce natural Japanese and refined Bisaya variations."
        }

        val systemPrompt = buildString {
            if (basePrompt.isNotBlank()) {
                appendLine(basePrompt)
                appendLine()
            }
            appendLine("You are an AI translator and nuance dictionary for Japanese, English, and Cebuano (Bisaya).")
            appendLine(directionInstruction)
            appendLine("Output ONLY valid JSON.")
            appendLine("Language rule: All explanation-related fields (politeness, situation, nuance, tip, summary, usage, related) MUST be written strictly in $uiLangName only.")
            appendLine("Keep every explanation field short (max 2-3 sentences).")
            appendLine("If any explanation field is not written in $uiLangName, you must regenerate it in the correct language before returning the JSON response. Never output mixed-language explanations.")
            appendLine("Never include narrative text outside of the JSON object.")
            appendLine("Schema (keys must match exactly):")
            appendLine("{")
            appendLine("  \"detectedLanguage\": \"ja\"|\"ceb\"|\"en\",")
            appendLine("  \"candidates\": [")
            appendLine("    {")
            appendLine("      \"bisaya\": string,")
            appendLine("      \"japanese\": string,")
            appendLine("      \"english\": string,")
            appendLine("      \"politeness\": string,")
            appendLine("      \"situation\": string,")
            appendLine("      \"nuance\": string,")
            appendLine("      \"tip\": string")
            appendLine("    }...")
            appendLine("  ],")
            appendLine("  \"explanation\": {")
            appendLine("    \"summary\": string,")
            appendLine("    \"usage\": string,")
            appendLine("    \"related\": [string]")
            appendLine("  }")
            appendLine("}")
            appendLine("Candidates may include multi-language translations as usual, but explanation-related fields must follow the $uiLangName rule.")
        }

        val userPrompt = buildString {
            appendLine("ui_lang: \"$uiLangCode\"")
            appendLine("translation_direction: \"${direction.name}\"")
            appendLine("detect_input_language: true")
            appendLine("Return translations and explanations using the schema above. Do not emit any keys outside this structure.")
            appendLine("User text: ${JSONObject.quote(text)}")
        }

        return TranslatorPrompt(systemPrompt = systemPrompt.trim(), userPrompt = userPrompt.trim())
    }

    private fun sanitizeTranslation(
        raw: String,
        direction: TranslationDirection
    ): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return trimmed

        return when (direction) {
            TranslationDirection.JA_TO_CEB -> {
                val firstJapaneseIndex = trimmed.indexOfFirst { it.isJapaneseGlyph() }
                val bisayaOnly = if (firstJapaneseIndex >= 0) {
                    trimmed.substring(0, firstJapaneseIndex).trim().trimEnd { it == '-' || it == '–' }
                } else {
                    trimmed
                }
                if (bisayaOnly.isNotBlank()) bisayaOnly else trimmed
            }

            TranslationDirection.CEB_TO_JA -> {
                val firstJapaneseIndex = trimmed.indexOfFirst { it.isJapaneseGlyph() }
                if (firstJapaneseIndex >= 0) {
                    trimmed.substring(firstJapaneseIndex).trim()
                } else {
                    trimmed
                }
            }
        }
    }

    private fun determineDirectionFromInput(text: String): TranslationDirection {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return TranslationDirection.JA_TO_CEB
        val lower = trimmed.lowercase()
        val hasJapaneseScript = trimmed.any { it.isJapaneseGlyph() }
        val hasBisayaHints = BISAYA_HINTS.any { lower.contains(it) }
        return when {
            hasBisayaHints -> TranslationDirection.CEB_TO_JA
            hasJapaneseScript -> TranslationDirection.JA_TO_CEB
            else -> TranslationDirection.JA_TO_CEB
        }
    }

    private fun Char.isJapaneseGlyph(): Boolean {
        val block = Character.UnicodeBlock.of(this)
        return block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
    }

    private fun parseTranslatorPayload(raw: String): TranslatorPayload {
        val cleaned = raw.trim().removePrefix("```json").removeSuffix("```")
        val json = JSONObject(cleaned)
        val candidatesArray = json.optJSONArray("candidates") ?: JSONArray()
        val candidates = buildList {
            for (i in 0 until candidatesArray.length()) {
                val item = candidatesArray.optJSONObject(i) ?: continue
                add(
                    TranslatorCandidate(
                        bisaya = item.optString("bisaya"),
                        japanese = item.optString("japanese"),
                        english = item.optString("english"),
                        politeness = item.optString("politeness"),
                        situation = item.optString("situation"),
                        nuance = item.optString("nuance"),
                        tip = item.optString("tip")
                    )
                )
            }
        }
        val explanationObj = json.optJSONObject("explanation")
        val explanation = explanationObj?.let {
            TranslatorExplanation(
                summary = it.optString("summary"),
                usage = it.optString("usage"),
                relatedPhrases = it.optJSONArray("related")?.let { arr ->
                    buildList {
                        for (i in 0 until arr.length()) {
                            add(arr.optString(i))
                        }
                    }
                } ?: emptyList()
            )
        }
        return TranslatorPayload(candidates = candidates, explanation = explanation)
    }

    fun buildCandidateDisplayList(candidates: List<TranslatorCandidate>): List<TranslatorCandidateDisplay> {
        return candidates.map { candidate ->
            val polite = candidate.politeness.trim().takeIf { it.isNotEmpty() }
            val situation = candidate.situation.trim().takeIf { it.isNotEmpty() }
            TranslatorCandidateDisplay(
                candidate = candidate,
                politenessLabel = polite?.let { shortenLabel(it, 12) },
                situationLabel = situation?.let { shortenLabel(it, 16) }
            )
        }
    }

    private fun shortenLabel(text: String, maxLength: Int): String {
        val trimmed = text.trim()
        if (trimmed.length <= maxLength) return trimmed
        if (maxLength <= 1) return trimmed.first().toString()
        val slice = trimmed.take(maxLength - 1).trimEnd()
        return "$slice…"
    }

    private fun logTranslationUsage(
        context: TranslateAdLogContext,
        adAttempted: Boolean,
        adResult: String
    ) {
        FreeUsageManager.logUsage(
            "free_limit_check feature=translate isPremium=${context.isPremiumUser} dayKey=${context.dayKey} " +
                "countBefore=${context.countBefore} countAfter=${context.countAfter}/${context.maxCount} " +
                "adShouldShow=${context.shouldShowAd} adAttempted=$adAttempted adResult=$adResult"
        )
    }

    private data class TranslateAdLogContext(
        val isPremiumUser: Boolean,
        val dayKey: String,
        val countBefore: Int,
        val countAfter: Int,
        val maxCount: Int,
        val shouldShowAd: Boolean
    )

    private data class PendingAdLog(
        val context: TranslateAdLogContext,
        val deferred: CompletableDeferred<AdManager.InterstitialAttemptResult>
    )

    private data class UsageSnapshot(
        val dayKey: String,
        val count: Int,
        val limit: Int
    )

    private fun usageSnapshot(
        overrideCount: Int? = null,
        overrideDayKey: String? = null,
        overrideLimit: Int? = null
    ): UsageSnapshot {
        val status = _usageStatus.value
        return UsageSnapshot(
            dayKey = overrideDayKey ?: status?.dayKey ?: FreeUsageManager.currentDayKey(),
            count = overrideCount ?: status?.usedCount ?: 0,
            limit = overrideLimit ?: status?.maxCount ?: FreeUsageRepository.MAX_TRANSLATE_PER_DAY
        )
    }

    private fun publishDebugState(
        snapshot: UsageSnapshot,
        isPremiumUser: Boolean,
        shouldShowAd: Boolean,
        adAttempted: Boolean,
        adResult: String,
        skipReason: String?
    ) {
        val timestamp = LocalDateTime.now(debugZoneId).format(debugTimeFormatter)
        _debugState.value = TranslatorDebugState(
            isPremiumUser = isPremiumUser,
            dayKey = snapshot.dayKey,
            translateCount = snapshot.count,
            translateLimit = snapshot.limit,
            shouldShowAdNow = shouldShowAd,
            adAttempted = adAttempted,
            adResult = adResult,
            skipReason = skipReason,
            lastUpdatedAt = timestamp
        )
    }

    private suspend fun prepareTranslateAttempt(isPremiumUser: Boolean): TranslateAttemptContext? {
        FreeUsageManager.resetIfNewDay()
        refreshUsageStatus()
        val statusBefore = _usageStatus.value
        val snapshotBefore = usageSnapshot()
        publishDebugState(
            snapshot = snapshotBefore,
            isPremiumUser = isPremiumUser,
            shouldShowAd = false,
            adAttempted = false,
            adResult = "pending",
            skipReason = null
        )

        // プロユーザーは制限なし
        if (isPremiumUser) {
            val dayKey = FreeUsageManager.currentDayKey()
            val maxCount = FreeUsageRepository.MAX_TRANSLATE_PER_DAY
            return TranslateAttemptContext(
                snapshotBefore = snapshotBefore,
                snapshotAfter = UsageSnapshot(dayKey, 0, maxCount),
                logContext = TranslateAdLogContext(
                    isPremiumUser = true,
                    dayKey = dayKey,
                    countBefore = 0,
                    countAfter = 0,
                    maxCount = maxCount,
                    shouldShowAd = false
                ),
                shouldShowAd = false,
                reachedLimitAfterAttempt = false
            )
        }

        // 無料ユーザーの制限処理
        if (statusBefore != null && !statusBefore.canUse) {
            publishDebugState(
                snapshot = snapshotBefore.copy(count = statusBefore.usedCount),
                isPremiumUser = false,
                shouldShowAd = false,
                adAttempted = false,
                adResult = "limit_reached",
                skipReason = "limit_reached"
            )
            FreeUsageManager.logUsage(
                "free_limit_check feature=translate event=limit_reached dayKey=${statusBefore.dayKey} count=${statusBefore.usedCount}"
            )
            _uiState.value = TranslatorUiState.Error("本日の無料翻訳は上限に達しました")
            _events.emit(TranslatorEvent.ShowUpsell)
            return null
        }

        val dayKeyBefore = statusBefore?.dayKey ?: FreeUsageManager.dayKey() ?: FreeUsageManager.currentDayKey()
        val countBefore = statusBefore?.usedCount ?: 0

        // 先行消費の徹底：消費直後に保存完了
        FreeUsageManager.consumeTranslate()
        refreshUsageStatus() // 即時更新
        
        val afterStatus = _usageStatus.value
        val dayKeyAfter = afterStatus?.dayKey ?: dayKeyBefore
        val countAfter = afterStatus?.usedCount ?: (countBefore + 1)
        val maxCount = afterStatus?.maxCount ?: FreeUsageRepository.MAX_TRANSLATE_PER_DAY
        val shouldShowAd = countAfter in listOf(2, 4)
        val isBeyondLimit = countAfter > maxCount

        if (isBeyondLimit) {
            publishDebugState(
                snapshot = UsageSnapshot(dayKeyAfter, countAfter, maxCount),
                isPremiumUser = false,
                shouldShowAd = false,
                adAttempted = false,
                adResult = "beyond_limit",
                skipReason = "beyond_limit"
            )
            _uiState.value = TranslatorUiState.Error("本日の無料翻訳は上限に達しました")
            _events.emit(TranslatorEvent.ShowUpsell)
            return null
        }

        val logContext = TranslateAdLogContext(
            isPremiumUser = false,
            dayKey = dayKeyAfter,
            countBefore = countBefore,
            countAfter = countAfter,
            maxCount = maxCount,
            shouldShowAd = shouldShowAd
        )

        val snapshotAfter = usageSnapshot(
            overrideCount = countAfter,
            overrideDayKey = dayKeyAfter,
            overrideLimit = maxCount
        )

        return TranslateAttemptContext(
            snapshotBefore = snapshotBefore,
            snapshotAfter = snapshotAfter,
            logContext = logContext,
            shouldShowAd = shouldShowAd,
            reachedLimitAfterAttempt = countAfter >= maxCount
        )
    }

    private suspend fun handleTranslateAttemptCompletion(
        attempt: TranslateAttemptContext,
        translationFailed: Boolean
    ) {
        val attemptCount = attempt.logContext.countAfter
        // ShowUpsellは上限（5回目）に達した時だけに変更 - ゴミ仕様を排除
        val shouldPromptUpsell = attempt.reachedLimitAfterAttempt && !attempt.logContext.isPremiumUser
        
        if (attempt.shouldShowAd && !attempt.logContext.isPremiumUser) {
            Log.d(LOG_TAG, "ad show requested count=${attempt.logContext.countAfter}")
            val deferred = CompletableDeferred<AdManager.InterstitialAttemptResult>()
            pendingAdLog?.deferred?.complete(AdManager.InterstitialAttemptResult.FAILED)
            pendingAdLog = PendingAdLog(attempt.logContext, deferred)
            publishDebugState(
                snapshot = attempt.snapshotAfter,
                isPremiumUser = attempt.logContext.isPremiumUser,
                shouldShowAd = true,
                adAttempted = true,
                adResult = "awaiting_show",
                skipReason = null
            )
            _events.emit(TranslatorEvent.RequireAd(attempt.logContext.countAfter))
            val adResult = try {
                deferred.await()
            } catch (_: Exception) {
                AdManager.InterstitialAttemptResult.FAILED
            }
            logTranslationUsage(attempt.logContext, adAttempted = true, adResult = adResult.name)
            pendingAdLog = null
        } else {
            val reachedLimit = attempt.reachedLimitAfterAttempt
            val adResultLabel = if (reachedLimit) "LIMIT_TRIGGERED" else "SKIPPED_NOT_REQUIRED"
            logTranslationUsage(attempt.logContext, adAttempted = false, adResult = adResultLabel)
            publishDebugState(
                snapshot = attempt.snapshotAfter,
                isPremiumUser = attempt.logContext.isPremiumUser,
                shouldShowAd = false,
                adAttempted = false,
                adResult = if (translationFailed) "failed_early" else "skipped",
                skipReason = if (reachedLimit) "limit_reached" else "not_eligible"
            )
            if (reachedLimit) {
                FreeUsageManager.logUsage(
                    "free_limit_check feature=translate event=limit_reached dayKey=${attempt.logContext.dayKey} count=${attempt.logContext.countAfter}"
                )
            }
        }

        if (shouldPromptUpsell) {
            _events.emit(TranslatorEvent.ShowUpsell)
        }
    }

    private data class TranslateAttemptContext(
        val snapshotBefore: UsageSnapshot,
        val snapshotAfter: UsageSnapshot,
        val logContext: TranslateAdLogContext,
        val shouldShowAd: Boolean,
        val reachedLimitAfterAttempt: Boolean
    )

    private data class TranslatorPayload(
        val candidates: List<TranslatorCandidate>,
        val explanation: TranslatorExplanation?
    )

    companion object {
        private const val LOG_TAG = "LearnBisaya"
        private val BISAYA_HINTS = setOf(
            "ako", "ikaw", "siya", "kami", "kita", "sila", "palihug", "salamat",
            "maayong", "unsa", "asa", "pila", "gani", "nimo", "karon", "adto",
            "laag", "gikan", "ganiha", "puhon", "mag", "amping", "gani", "wala",
            "naa", "kaayo", "gihapon"
        )
    }
}
