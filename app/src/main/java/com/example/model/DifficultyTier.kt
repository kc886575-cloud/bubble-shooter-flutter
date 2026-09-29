package com.example.model

import androidx.compose.ui.graphics.Color

enum class DifficultyTier(
    val title: String,
    val minScore: Int,
    val maxShotsBeforeDescent: Int,
    val colors: List<BubbleColor>,
    val badgeColor: Color,
    val speedLabel: String,
    val descentTimerSeconds: Int
) {
    NOVICE(
        title = "NOVICE",
        minScore = 0,
        maxShotsBeforeDescent = 6,
        colors = listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW),
        badgeColor = Color(0xFF10B981), // Emerald
        speedLabel = "1.0x",
        descentTimerSeconds = 30
    ),
    ADEPT(
        title = "ADEPT",
        minScore = 1500,
        maxShotsBeforeDescent = 5,
        colors = listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW, BubbleColor.PURPLE),
        badgeColor = Color(0xFF3B82F6), // Blue
        speedLabel = "1.3x",
        descentTimerSeconds = 24
    ),
    EXPERT(
        title = "EXPERT",
        minScore = 3500,
        maxShotsBeforeDescent = 4,
        colors = listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW, BubbleColor.PURPLE, BubbleColor.CYAN),
        badgeColor = Color(0xFF8B5CF6), // Purple
        speedLabel = "1.6x",
        descentTimerSeconds = 18
    ),
    MASTER(
        title = "MASTER",
        minScore = 7000,
        maxShotsBeforeDescent = 3,
        colors = listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW, BubbleColor.PURPLE, BubbleColor.CYAN, BubbleColor.ORANGE),
        badgeColor = Color(0xFFF59E0B), // Amber
        speedLabel = "2.0x",
        descentTimerSeconds = 14
    ),
    INFERNO(
        title = "INFERNO",
        minScore = 12000,
        maxShotsBeforeDescent = 2,
        colors = listOf(BubbleColor.RED, BubbleColor.BLUE, BubbleColor.GREEN, BubbleColor.YELLOW, BubbleColor.PURPLE, BubbleColor.CYAN, BubbleColor.ORANGE),
        badgeColor = Color(0xFFEF4444), // Crimson
        speedLabel = "2.5x MAX",
        descentTimerSeconds = 10
    );

    companion object {
        fun fromScore(score: Int): DifficultyTier {
            val tiers = entries
            for (i in tiers.indices.reversed()) {
                if (score >= tiers[i].minScore) {
                    return tiers[i]
                }
            }
            return NOVICE
        }
    }
}
