package com.bisayaspeak.ai.data.model.mock

import com.bisayaspeak.ai.data.model.LearningLevel

data class MockRolePlayScenario(
    val id: String,
    val titleJa: String,
    val level: LearningLevel,
    val npcName: String,
    val npcIcon: String,
    val steps: List<MockRolePlayStep>
)

data class MockRolePlayStep(
    val id: String,
    val aiLineVisayan: String,
    val aiLineJa: String,
    val choices: List<MockRolePlayChoice>,
    val isFinal: Boolean = false
)

data class MockRolePlayChoice(
    val textVisayan: String,
    val textJa: String,
    val isCorrect: Boolean
)
