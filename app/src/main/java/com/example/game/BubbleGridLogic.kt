package com.example.game

import androidx.compose.ui.geometry.Offset
import com.example.model.Bubble
import com.example.model.BubbleColor
import kotlin.math.sqrt

class BubbleGridLogic(
    val numColsEven: Int = 8,
    val maxRows: Int = 14,
    var bubbleRadius: Float = 36f,
    var startX: Float = 0f,
    var startY: Float = 0f
) {
    val numColsOdd: Int = numColsEven - 1
    val rowHeight: Float get() = bubbleRadius * sqrt(3f)

    fun getColsForRow(row: Int, totalDescents: Int = 0): Int =
        if ((row + totalDescents) % 2 == 0) numColsEven else numColsOdd

    fun getBubbleCenter(row: Int, col: Int, totalDescents: Int = 0): Offset {
        val isEven = (row + totalDescents) % 2 == 0
        val x = if (isEven) {
            startX + (col * 2 + 1) * bubbleRadius
        } else {
            startX + (col * 2 + 2) * bubbleRadius
        }
        val y = startY + bubbleRadius + (row * rowHeight)
        return Offset(x, y)
    }

    fun getNeighbors(row: Int, col: Int, totalDescents: Int = 0): List<Pair<Int, Int>> {
        val neighbors = mutableListOf<Pair<Int, Int>>()
        val isEven = (row + totalDescents) % 2 == 0

        // Same row neighbors (left, right)
        neighbors.add(Pair(row, col - 1))
        neighbors.add(Pair(row, col + 1))

        if (isEven) {
            // Even row neighbors
            neighbors.add(Pair(row - 1, col - 1))
            neighbors.add(Pair(row - 1, col))
            neighbors.add(Pair(row + 1, col - 1))
            neighbors.add(Pair(row + 1, col))
        } else {
            // Odd row neighbors
            neighbors.add(Pair(row - 1, col))
            neighbors.add(Pair(row - 1, col + 1))
            neighbors.add(Pair(row + 1, col))
            neighbors.add(Pair(row + 1, col + 1))
        }

        return neighbors.filter { (r, c) ->
            r in 0 until maxRows && c in 0 until getColsForRow(r, totalDescents)
        }
    }

    fun findMatches(
        grid: Map<Pair<Int, Int>, Bubble>,
        startRow: Int,
        startCol: Int,
        totalDescents: Int = 0
    ): Set<Pair<Int, Int>> {
        val startBubble = grid[Pair(startRow, startCol)] ?: return emptySet()
        val targetColor = startBubble.color
        val matched = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        val startPos = Pair(startRow, startCol)
        queue.add(startPos)
        matched.add(startPos)

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            for (neighbor in getNeighbors(curr.first, curr.second, totalDescents)) {
                if (neighbor !in matched) {
                    val bubble = grid[neighbor]
                    if (bubble != null && !bubble.isPopping && !bubble.isFalling && bubble.color == targetColor) {
                        matched.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }
        }

        return if (matched.size >= 3) matched else emptySet()
    }

    fun findFloatingBubbles(
        grid: Map<Pair<Int, Int>, Bubble>,
        excluding: Set<Pair<Int, Int>> = emptySet(),
        totalDescents: Int = 0
    ): Set<Pair<Int, Int>> {
        val attached = mutableSetOf<Pair<Int, Int>>()
        val queue = ArrayDeque<Pair<Int, Int>>()

        // Bubbles in row 0 are anchored to ceiling
        for (col in 0 until getColsForRow(0, totalDescents)) {
            val pos = Pair(0, col)
            val bubble = grid[pos]
            if (bubble != null && pos !in excluding && !bubble.isPopping && !bubble.isFalling) {
                attached.add(pos)
                queue.add(pos)
            }
        }

        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            for (neighbor in getNeighbors(curr.first, curr.second, totalDescents)) {
                if (neighbor !in attached && neighbor !in excluding) {
                    val bubble = grid[neighbor]
                    if (bubble != null && !bubble.isPopping && !bubble.isFalling) {
                        attached.add(neighbor)
                        queue.add(neighbor)
                    }
                }
            }
        }

        val floating = mutableSetOf<Pair<Int, Int>>()
        for ((pos, bubble) in grid) {
            if (pos !in attached && pos !in excluding && !bubble.isPopping && !bubble.isFalling) {
                floating.add(pos)
            }
        }
        return floating
    }

    fun findBestSnapSlot(
        grid: Map<Pair<Int, Int>, Bubble>,
        hitX: Float,
        hitY: Float,
        totalDescents: Int = 0
    ): Pair<Int, Int>? {
        var bestSlot: Pair<Int, Int>? = null
        var minDistance = Float.MAX_VALUE

        for (r in 0 until maxRows) {
            val cols = getColsForRow(r, totalDescents)
            for (c in 0 until cols) {
                val pos = Pair(r, c)
                if (!grid.containsKey(pos) || grid[pos]?.isPopping == true || grid[pos]?.isFalling == true) {
                    val hasAnchor = (r == 0) || getNeighbors(r, c, totalDescents).any { n ->
                        grid.containsKey(n) && grid[n]?.isPopping == false && grid[n]?.isFalling == false
                    }
                    if (hasAnchor) {
                        val center = getBubbleCenter(r, c, totalDescents)
                        val dx = center.x - hitX
                        val dy = center.y - hitY
                        val distSq = dx * dx + dy * dy
                        if (distSq < minDistance) {
                            minDistance = distSq
                            bestSlot = pos
                        }
                    }
                }
            }
        }
        return bestSlot
    }
}
