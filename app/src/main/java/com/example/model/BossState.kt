package com.example.model

data class BossState(
    val name: String = "THE BUBBLE OVERLORD",
    val title: String = "Ancient Realm Destroyer",
    val maxHp: Int = 6,
    val currentHp: Int = 6,
    val isEnraged: Boolean = false,
    val isDefeated: Boolean = false
) {
    val hpPercent: Float get() = (currentHp.toFloat() / maxHp.toFloat()).coerceIn(0f, 1f)
}
