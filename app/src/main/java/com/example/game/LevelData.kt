package com.example.game

import com.example.model.Bubble
import com.example.model.BubbleColor
import com.example.model.DifficultyTier
import kotlin.random.Random

data class LevelConfig(
    val levelNumber: Int,
    val name: String,
    val description: String,
    val moves: Int,
    val isBoss: Boolean = false
)

object LevelData {

    val LEVELS = listOf(
        LevelConfig(1, "Gentle Waves", "Learn the basics with simple color groups", 30),
        LevelConfig(2, "Twin Stripes", "Alternating color rows. Plan your bank shots!", 28),
        LevelConfig(3, "Diamond Citadel", "Pierce the diamond core to drop floating wings", 26),
        LevelConfig(4, "Rainbow Zigzag", "Staggered diagonal waves test your accuracy", 25),
        LevelConfig(5, "Honeycomb Hive", "Clusters of sweet combos waiting to burst", 25),
        LevelConfig(6, "Double Spiral", "Interlocking arms. Pop the anchors to cause massive drops", 24),
        LevelConfig(7, "The Fortress", "Heavy reinforced barricade of bubbles", 23),
        LevelConfig(8, "Hourglass Prism", "Narrow neck leads to huge upper rewards", 22),
        LevelConfig(9, "Crystal Cross", "Multi-colored geometric challenge", 22),
        LevelConfig(10, "Castle Ramparts", "The final fortress before the Overlord's chamber", 21),
        LevelConfig(11, "THE BUBBLE OVERLORD", "Epic Boss Battle! Pop adjacent bubbles to shatter its armor!", 28, isBoss = true)
    )

    fun getLevelConfig(level: Int): LevelConfig {
        return LEVELS.firstOrNull { it.levelNumber == level } ?: LEVELS.first()
    }

    fun generateLevel(
        level: Int,
        gridLogic: BubbleGridLogic,
        tier: DifficultyTier = DifficultyTier.NOVICE
    ): Map<Pair<Int, Int>, Bubble> {
        val grid = mutableMapOf<Pair<Int, Int>, Bubble>()
        val colors = tier.colors

        when (level) {
            1 -> {
                // Gentle Waves: 5 rows of 2-color pairs
                for (r in 0 until 5) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val color = colors[(c / 2 + r % 2) % colors.size]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            2 -> {
                // Twin Stripes: horizontal rows alternating cleanly
                for (r in 0 until 6) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val color = colors[r % colors.size]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            3 -> {
                // Diamond Citadel: Center diamond with dangling wings
                for (r in 0 until 6) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val isWing = (c == 0 || c == cols - 1)
                        val color = if (isWing) colors[0] else colors[(r + c) % colors.size]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            4 -> {
                // Rainbow Zigzag: diagonal wave pattern
                for (r in 0 until 6) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val color = colors[(r * 2 + c) % colors.size]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            5 -> {
                // Honeycomb Hive: Honeycomb clusters of 3
                for (r in 0 until 6) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val cluster = (c / 3 + r / 2) % colors.size
                        val color = colors[cluster]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            6 -> {
                // Double Spiral: Center anchor with hanging coils
                for (r in 0 until 7) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val color = if (r % 2 == 0) colors[c % colors.size] else colors[(cols - 1 - c) % colors.size]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            7 -> {
                // The Fortress: Thick outer wall, high-value core
                for (r in 0 until 7) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val isWall = (r == 0 || c == 0 || c == cols - 1)
                        val color = if (isWall) colors[1] else colors[(r + c * 2) % colors.size]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            8 -> {
                // Hourglass Prism: Wide top, narrow neck, flared bottom
                for (r in 0 until 7) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    val skipStart = when (r) {
                        2, 3 -> 2
                        else -> 0
                    }
                    val skipEnd = when (r) {
                        2, 3 -> cols - 2
                        else -> cols
                    }
                    for (c in 0 until cols) {
                        if (c in skipStart until skipEnd) {
                            val color = colors[(r * 3 + c) % colors.size]
                            val center = gridLogic.getBubbleCenter(r, c, 0)
                            grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                        }
                    }
                }
            }
            9 -> {
                // Crystal Cross
                for (r in 0 until 7) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val isCrossArm = (r == 3 || c == cols / 2)
                        val color = if (isCrossArm) colors[0] else colors[(r + c) % colors.size]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            10 -> {
                // Castle Ramparts: Pre-Boss Final Defense
                for (r in 0 until 7) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val color = when {
                            r == 0 -> colors[0]
                            r == 1 -> colors[1 % colors.size]
                            c % 2 == 0 -> colors[(r + 2) % colors.size]
                            else -> colors[(r + 3) % colors.size]
                        }
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            11 -> {
                // BOSS LEVEL: The Overlord Chamber!
                // Dense armor shielding surrounding the center boss bubble
                for (r in 0 until 6) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        // Center is Boss zone (row 2, col 3-4)
                        val color = if (r in 1..3 && c in 2..4) {
                            BubbleColor.PURPLE // Boss core shield
                        } else {
                            colors[(r + c) % colors.size]
                        }
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
            else -> {
                // Default fallback
                for (r in 0 until 6) {
                    val cols = gridLogic.getColsForRow(r, 0)
                    for (c in 0 until cols) {
                        val color = colors[(r + c) % colors.size]
                        val center = gridLogic.getBubbleCenter(r, c, 0)
                        grid[Pair(r, c)] = Bubble(row = r, col = c, color = color, x = center.x, y = center.y)
                    }
                }
            }
        }
        return grid
    }

    fun getMovesForLevel(level: Int): Int {
        val config = getLevelConfig(level)
        return config.moves
    }
}
