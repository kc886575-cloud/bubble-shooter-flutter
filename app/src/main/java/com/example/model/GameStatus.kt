package com.example.model

enum class GameStatus {
    PLAYING,
    PAUSED,
    LEVEL_CLEARED,
    GAME_OVER
}

data class AimTrajectory(
    val points: List<androidx.compose.ui.geometry.Offset>,
    val targetHitPoint: androidx.compose.ui.geometry.Offset? = null,
    val targetHex: Pair<Int, Int>? = null
)
