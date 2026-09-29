package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapCalls
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.BubbleShooterViewModel
import com.example.model.AppScreen
import com.example.model.BossState
import com.example.model.BubbleColor
import com.example.model.DifficultyTier
import com.example.model.GameStatus

@Composable
fun BubbleShooterScreen(
    viewModel: BubbleShooterViewModel,
    modifier: Modifier = Modifier
) {
    // Android hardware/gesture back button support
    BackHandler {
        viewModel.navigateBack()
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF172554)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        LaunchedEffect(width, height) {
            viewModel.updateScreenDimensions(width, height)
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Top HUD Bar with Score, Shots, Difficulty Tier, and Ceiling Descent Countdown
            GameHudBar(
                score = state.score,
                highScore = state.highScore,
                level = state.level,
                movesLeft = state.movesLeft,
                difficultyTier = state.difficultyTier,
                shotsUntilDescent = state.shotsUntilCeilingDescent,
                maxShotsForDescent = state.maxShotsForDescent,
                secondsUntilDescent = state.secondsUntilCeilingDescent,
                isSoundEnabled = state.isSoundEnabled,
                onPause = { viewModel.pauseGame() },
                onToggleSound = { viewModel.toggleSound() },
                onExitToHome = { viewModel.navigateTo(AppScreen.HOME) },
                onExitToLevels = { viewModel.navigateTo(AppScreen.LEVEL_SELECT) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            )

            // Boss Health Bar (when playing Boss Battle: Level 11)
            state.bossState?.let { boss ->
                BossHealthBar(
                    boss = boss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Main Playfield Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PlayfieldCanvas(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )

                // Combo indicator banner
                if (state.comboCount >= 2) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF59E0B).copy(alpha = 0.92f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = "STREAK x${state.comboCount}!",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                // New Tier / New Color Unlocked Floating Alert
                state.tierUnlockAlert?.let { alertText ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = state.difficultyTier.badgeColor.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.linearGradient(listOf(Color.White, state.difficultyTier.badgeColor))
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = alertText,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                // Ceiling Warning Flash Banner
                if (state.ceilingWarningAlert) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 46.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ROOF DROPPED!",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Bottom Pedestal & Controls
            ShooterControls(
                currentBubble = state.currentBubble,
                nextBubble = state.nextBubble,
                onSwap = { viewModel.swapBubbles() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            )
        }

        // Dialogs
        when (state.gameStatus) {
            GameStatus.GAME_OVER -> {
                GameOverDialog(
                    score = state.score,
                    highScore = state.highScore,
                    tier = state.difficultyTier,
                    onRestart = { viewModel.restartGame() },
                    onExitToLevels = { viewModel.navigateTo(AppScreen.LEVEL_SELECT) },
                    onExitToHome = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
            GameStatus.LEVEL_CLEARED -> {
                LevelClearDialog(
                    level = state.level,
                    score = state.score,
                    movesLeft = state.movesLeft,
                    tier = state.difficultyTier,
                    isBoss = state.level == 11,
                    onNextLevel = { viewModel.nextLevel() },
                    onRestart = { viewModel.restartGame() },
                    onExitToLevels = { viewModel.navigateTo(AppScreen.LEVEL_SELECT) },
                    onExitToHome = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
            GameStatus.PAUSED -> {
                PauseDialog(
                    score = state.score,
                    highScore = state.highScore,
                    tier = state.difficultyTier,
                    isSoundEnabled = state.isSoundEnabled,
                    onResume = { viewModel.resumeGame() },
                    onRestart = {
                        viewModel.restartGame()
                        viewModel.resumeGame()
                    },
                    onToggleSound = { viewModel.toggleSound() },
                    onExitToLevels = { viewModel.navigateTo(AppScreen.LEVEL_SELECT) },
                    onExitToHome = { viewModel.navigateTo(AppScreen.HOME) }
                )
            }
            GameStatus.PLAYING -> {}
        }
    }
}

@Composable
private fun BossHealthBar(
    boss: BossState,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0xFFFFD700)))
        ),
        modifier = modifier.testTag("boss_health_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = boss.name,
                        color = Color(0xFFFFD700),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = "${boss.currentHp} / ${boss.maxHp} HP",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { boss.hpPercent },
                color = if (boss.isEnraged) Color(0xFFEF4444) else Color(0xFFF59E0B),
                trackColor = Color(0xFF1E1B4B),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun GameHudBar(
    score: Int,
    highScore: Int,
    level: Int,
    movesLeft: Int,
    difficultyTier: DifficultyTier,
    shotsUntilDescent: Int,
    maxShotsForDescent: Int,
    secondsUntilDescent: Int,
    isSoundEnabled: Boolean,
    onPause: () -> Unit,
    onToggleSound: () -> Unit,
    onExitToHome: () -> Unit,
    onExitToLevels: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.88f)
        ),
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.testTag("game_hud_bar")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Row 1: Home, Level Select, Score, Moves Left, Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Navigation buttons: Home & Levels
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onExitToHome,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("exit_to_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onExitToLevels,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("exit_to_levels_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Levels",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Level & Score
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (level == 11) "👑 BOSS" else "LVL $level",
                        color = if (level == 11) Color(0xFFFFD700) else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$score",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Moves Left Pill Badge
                val movesColor = when {
                    movesLeft > 10 -> Color(0xFF10B981)
                    movesLeft > 5 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = movesColor.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(movesColor, movesColor.copy(alpha = 0.5f)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "SHOTS",
                            color = movesColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "$movesLeft",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // High Score
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Trophy",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "BEST",
                            color = Color(0xFFFFD700),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "$highScore",
                        color = Color(0xFFE2E8F0),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Audio and Pause icons
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    IconButton(
                        onClick = onToggleSound,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                            contentDescription = "Toggle Sound",
                            tint = if (isSoundEnabled) Color(0xFF38BDF8) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onPause,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("pause_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Row 2: Progressive Difficulty Tier Badge & Ceiling Descent Meter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = difficultyTier.badgeColor.copy(alpha = 0.18f)),
                    shape = RoundedCornerShape(8.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.linearGradient(listOf(difficultyTier.badgeColor, difficultyTier.badgeColor.copy(alpha = 0.4f)))
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(difficultyTier.badgeColor)
                        )
                        Text(
                            text = "${difficultyTier.title} • ${difficultyTier.colors.size} COLORS (${difficultyTier.speedLabel})",
                            color = difficultyTier.badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                val isUrgent = shotsUntilDescent <= 1
                val roofWarningColor = if (isUrgent) Color(0xFFEF4444) else Color(0xFFF59E0B)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = roofWarningColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (isUrgent) "ROOF DROP: 1 SHOT!" else "ROOF: $shotsUntilDescent SHOTS ($secondsUntilDescent" + "s)",
                        color = roofWarningColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        for (i in 1..maxShotsForDescent) {
                            val active = i <= shotsUntilDescent
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) roofWarningColor else Color(0xFF475569)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayfieldCanvas(
    viewModel: BubbleShooterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Canvas(
        modifier = modifier
            .testTag("game_canvas")
            .pointerInput(state.gameStatus, state.isReadyToShoot) {
                detectDragGestures(
                    onDragStart = { offset ->
                        viewModel.onAim(offset)
                    },
                    onDrag = { change, _ ->
                        viewModel.onAim(change.position)
                    },
                    onDragEnd = {
                        viewModel.onReleaseAim()
                    },
                    onDragCancel = {
                        viewModel.onReleaseAim()
                    }
                )
            }
    ) {
        val r = viewModel.gridLogic.bubbleRadius
        val left = viewModel.gridLogic.startX
        val right = viewModel.gridLogic.startX + (viewModel.gridLogic.numColsEven * 2 * r)

        BubblePainter.drawCeilingBar(
            drawScope = this,
            startX = left,
            endX = right,
            y = viewModel.gridLogic.startY + r,
            isWarning = state.ceilingWarningAlert,
            shotsUntilDescent = state.shotsUntilCeilingDescent
        )

        val isNearDanger = state.grid.values.any { it.row >= 9 && !it.isPopping && !it.isFalling }
        val dangerY = viewModel.gridLogic.startY + (11 * viewModel.gridLogic.rowHeight)
        BubblePainter.drawDangerLine(
            drawScope = this,
            startX = left,
            endX = right,
            y = dangerY,
            isNearDanger = isNearDanger
        )

        state.grid.values.forEach { bubble ->
            val scale = if (bubble.isPopping) (1f + bubble.popProgress * 0.4f) else 1f
            val alpha = if (bubble.isPopping) (1f - bubble.popProgress) else if (bubble.isFalling) bubble.fallAlpha else 1f

            BubblePainter.drawGlossyBubble(
                drawScope = this,
                center = Offset(bubble.x, bubble.y),
                radius = r,
                color = bubble.color,
                alpha = alpha.coerceIn(0f, 1f),
                scale = scale
            )
        }

        state.aimTrajectory?.let { trajectory ->
            BubblePainter.drawAimTrajectory(
                drawScope = this,
                trajectory = trajectory,
                bubbleColor = state.currentBubble,
                bubbleRadius = r
            )
        }

        state.flyingBubble?.let { flying ->
            BubblePainter.drawGlossyBubble(
                drawScope = this,
                center = Offset(flying.x, flying.y),
                radius = r,
                color = flying.color
            )
        }

        BubblePainter.drawParticles(
            drawScope = this,
            particles = state.particles
        )

        BubblePainter.drawFloatingTexts(
            drawScope = this,
            floatingTexts = state.floatingTexts
        )
    }
}

@Composable
private fun ShooterControls(
    currentBubble: BubbleColor,
    nextBubble: BubbleColor,
    onSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.testTag("shooter_controls")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSwap() }
                    .padding(4.dp)
                    .testTag("swap_bubble_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "NEXT",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A))
                            .border(2.dp, Color(0xFF334155), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(34.dp)) {
                            BubblePainter.drawGlossyBubble(
                                drawScope = this,
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.width / 2f,
                                color = nextBubble
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF334155)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapCalls,
                        contentDescription = "Swap Bubbles",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = "Drag to Aim\nRelease to Shoot",
                color = Color(0xFFCBD5E1),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "READY",
                    color = Color(0xFF38BDF8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A))
                        .border(2.5.dp, Color(0xFF38BDF8), CircleShape)
                        .shadow(8.dp, CircleShape, spotColor = currentBubble.glow),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(42.dp)) {
                        BubblePainter.drawGlossyBubble(
                            drawScope = this,
                            center = Offset(size.width / 2f, size.height / 2f),
                            radius = size.width / 2f,
                            color = currentBubble
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameOverDialog(
    score: Int,
    highScore: Int,
    tier: DifficultyTier,
    onRestart: () -> Unit,
    onExitToLevels: () -> Unit,
    onExitToHome: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME OVER",
                    color = Color(0xFFEF4444),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Reached ${tier.title} Tier (${tier.colors.size} colors)",
                    color = tier.badgeColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FINAL SCORE",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$score",
                            color = Color.White,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Best: $highScore",
                                color = Color(0xFFFFD700),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TRY AGAIN",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExitToLevels,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("game_over_levels_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LEVELS",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onExitToHome,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("game_over_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HOME",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelClearDialog(
    level: Int,
    score: Int,
    movesLeft: Int,
    tier: DifficultyTier,
    isBoss: Boolean,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onExitToLevels: () -> Unit,
    onExitToHome: () -> Unit
) {
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("level_clear_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isBoss) "👑 BOSS VANQUISHED!" else "LEVEL $level CLEARED!",
                    color = Color(0xFF10B981),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isBoss) "🎉 CONGRATULATIONS! ALL 10 LEVELS & BOSS COMPLETED!" else "Current Tier: ${tier.title} (${tier.colors.size} Colors)",
                    color = if (isBoss) Color(0xFFFFD700) else tier.badgeColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3 Stars rating
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(3) { idx ->
                        val earned = when (idx) {
                            0 -> true
                            1 -> movesLeft >= 5
                            2 -> movesLeft >= 12
                            else -> false
                        }
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (earned) Color(0xFFFFD700) else Color(0xFF475569),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Shots Remaining: $movesLeft (+${movesLeft * 300} bonus)",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TOTAL SCORE",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$score",
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                if (level < 11) {
                    Button(
                        onClick = onNextLevel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("next_level_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (level == 10) "FIGHT BOSS LEVEL 11!" else "NEXT LEVEL",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExitToLevels,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("level_clear_levels_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LEVELS",
                            color = Color(0xFFCBD5E1),
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onExitToHome,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("level_clear_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HOME",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseDialog(
    score: Int,
    highScore: Int,
    tier: DifficultyTier,
    isSoundEnabled: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    onExitToLevels: () -> Unit,
    onExitToHome: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("pause_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "GAME PAUSED",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tier: ${tier.title} (${tier.colors.size} Colors • ${tier.speedLabel})",
                    color = tier.badgeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Current Score", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("$score", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Best Score", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("$highScore", color = Color(0xFFFFD700), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button")
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RESUME", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onToggleSound,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isSoundEnabled) "Sound: ON" else "Sound: OFF")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExitToLevels,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("pause_levels_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("LEVELS", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onExitToHome,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A8A)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("pause_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("HOME", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
