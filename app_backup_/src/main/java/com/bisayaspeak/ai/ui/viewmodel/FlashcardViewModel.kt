package com.bisayaspeak.ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.bisayaspeak.ai.data.model.PracticeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

data class FlashcardUiState(
    val items: List<PracticeItem> = emptyList(),
    val currentIndex: Int = 0,
    val showMeaning: Boolean = false,
    val isComplete: Boolean = false
)

class FlashcardViewModel : ViewModel() {

    companion object {
        private const val LITE_CARD_COUNT = 11
    }

    private val _uiState = MutableStateFlow(FlashcardUiState())
    val uiState: StateFlow<FlashcardUiState> = _uiState.asStateFlow()

    fun startLiteSession(allItems: List<PracticeItem>) {
        val nonPremiumItems = allItems.filterNot { it.isPremium }
        if (nonPremiumItems.isEmpty()) {
            _uiState.value = FlashcardUiState(items = emptyList())
            return
        }
        val sessionItems = buildLiteFlashcards(nonPremiumItems, LITE_CARD_COUNT)
        _uiState.value = FlashcardUiState(
            items = sessionItems,
            currentIndex = 0,
            showMeaning = false,
            isComplete = false
        )
    }

    fun toggleMeaning() {
        _uiState.update { state ->
            if (state.items.isEmpty() || state.isComplete) {
                state
            } else {
                state.copy(showMeaning = !state.showMeaning)
            }
        }
    }

    fun goToNextCard() {
        _uiState.update { state ->
            if (state.items.isEmpty() || state.isComplete) {
                state
            } else {
                val nextIndex = state.currentIndex + 1
                if (nextIndex >= state.items.size) {
                    state.copy(isComplete = true, showMeaning = false)
                } else {
                    state.copy(currentIndex = nextIndex, showMeaning = false)
                }
            }
        }
    }
}

private fun buildLiteFlashcards(
    items: List<PracticeItem>,
    totalCards: Int,
    hardCountRange: IntRange = 1..2
): List<PracticeItem> {
    if (items.isEmpty() || totalCards <= 0) return emptyList()

    val beginnerPool = items.filter { it.difficulty <= 1 }.shuffled().toMutableList()
    val intermediatePool = items.filter { it.difficulty == 2 }.shuffled().toMutableList()
    val advancedPool = items.filter { it.difficulty >= 3 }.shuffled().toMutableList()

    val hardSource = when {
        advancedPool.isNotEmpty() -> advancedPool
        intermediatePool.isNotEmpty() -> intermediatePool
        else -> beginnerPool
    }

    val requestedHardCount = if (hardCountRange.first == hardCountRange.last) {
        hardCountRange.first
    } else {
        Random.nextInt(hardCountRange.first, hardCountRange.last + 1)
    }
    val hardCount = requestedHardCount
        .coerceAtLeast(1)
        .coerceAtMost(hardSource.size.coerceAtLeast(1))
    val hardItems = hardSource.take(hardCount)
    beginnerPool.removeAll(hardItems)
    intermediatePool.removeAll(hardItems)
    advancedPool.removeAll(hardItems)

    val remainingNeeded = (totalCards - hardItems.size).coerceAtLeast(0)
    val beginnerTarget = minOf(remainingNeeded, 7, beginnerPool.size)
    val beginnerItems = beginnerPool.take(beginnerTarget)
    beginnerPool.removeAll(beginnerItems)

    val remainingAfterBeginner = remainingNeeded - beginnerItems.size
    val intermediateItems =
        intermediatePool.take(remainingAfterBeginner.coerceAtMost(intermediatePool.size))
    intermediatePool.removeAll(intermediateItems)

    val combined = mutableListOf<PracticeItem>().apply {
        addAll(beginnerItems)
        addAll(intermediateItems)
        addAll(hardItems)
    }

    if (combined.size < totalCards) {
        val fallbackPool = (beginnerPool + intermediatePool + advancedPool).filter { it !in combined }
        combined += fallbackPool.shuffled().take(totalCards - combined.size)
    }

    return combined.take(totalCards).shuffled()
}
