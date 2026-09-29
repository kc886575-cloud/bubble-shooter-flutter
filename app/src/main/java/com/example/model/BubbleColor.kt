package com.example.model

import androidx.compose.ui.graphics.Color

enum class BubbleColor(
    val primary: Color,
    val light: Color,
    val dark: Color,
    val glow: Color,
    val displayName: String
) {
    RED(
        primary = Color(0xFFEF4444),
        light = Color(0xFFFCA5A5),
        dark = Color(0xFF991B1B),
        glow = Color(0xFFFF6B6B),
        displayName = "Red"
    ),
    BLUE(
        primary = Color(0xFF3B82F6),
        light = Color(0xFF93C5FD),
        dark = Color(0xFF1E40AF),
        glow = Color(0xFF60A5FA),
        displayName = "Blue"
    ),
    GREEN(
        primary = Color(0xFF10B981),
        light = Color(0xFF6EE7B7),
        dark = Color(0xFF065F46),
        glow = Color(0xFF34D399),
        displayName = "Green"
    ),
    YELLOW(
        primary = Color(0xFFF59E0B),
        light = Color(0xFFFDE68A),
        dark = Color(0xFFB45309),
        glow = Color(0xFFFBBF24),
        displayName = "Yellow"
    ),
    PURPLE(
        primary = Color(0xFF8B5CF6),
        light = Color(0xFFC4B5FD),
        dark = Color(0xFF5B21B6),
        glow = Color(0xFFA78BFA),
        displayName = "Purple"
    ),
    CYAN(
        primary = Color(0xFF06B6D4),
        light = Color(0xFFA5F3FC),
        dark = Color(0xFF0E7490),
        glow = Color(0xFF22D3EE),
        displayName = "Cyan"
    ),
    ORANGE(
        primary = Color(0xFFF97316),
        light = Color(0xFFFDBA74),
        dark = Color(0xFFC2410C),
        glow = Color(0xFFFB923C),
        displayName = "Orange"
    );

    companion object {
        fun standardColors(): List<BubbleColor> = listOf(RED, BLUE, GREEN, YELLOW, PURPLE)
    }
}
