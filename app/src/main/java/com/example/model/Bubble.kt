package com.example.model

data class Bubble(
    val row: Int,
    val col: Int,
    val color: BubbleColor,
    val id: Long = System.nanoTime() + (row * 1000L) + col,
    var x: Float = 0f,
    var y: Float = 0f,
    var isPopping: Boolean = false,
    var popProgress: Float = 0f,
    var isFalling: Boolean = false,
    var fallVx: Float = 0f,
    var fallVy: Float = 0f,
    var fallAlpha: Float = 1f
)

data class FlyingBubble(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val color: BubbleColor,
    val radius: Float
)
