package com.example.game

import android.app.Application
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffect
import com.example.audio.SoundManager
import com.example.data.AppDatabase
import com.example.data.HighScoreEntity
import com.example.data.HighScoreRepository
import com.example.data.LevelProgressEntity
import com.example.model.AimTrajectory
import com.example.model.AppScreen
import com.example.model.BossState
import com.example.model.Bubble
import com.example.model.BubbleColor
import com.example.model.DifficultyTier
import com.example.model.FloatingText
import com.example.model.FlyingBubble
import com.example.model.GameStatus
import com.example.model.Particle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class GameUiState(
    val currentScreen: AppScreen = AppScreen.HOME,
    val selectedLevel: Int = 1,
    val levelProgressList: List<LevelProgressEntity> = emptyList(),
    val bossState: BossState? = null,
    val score: Int = 0,
    val highScore: Int = 0,
    val level: Int = 1,
    val movesLeft: Int = 30,
    val totalMoves: Int = 30,
    val difficultyTier: DifficultyTier = DifficultyTier.NOVICE,
    val shotsUntilCeilingDescent: Int = 6,
    val maxShotsForDescent: Int = 6,
    val secondsUntilCeilingDescent: Int = 30,
    val totalDescents: Int = 0,
    val ceilingWarningAlert: Boolean = false,
    val tierUnlockAlert: String? = null,
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val currentBubble: BubbleColor = BubbleColor.RED,
    val nextBubble: BubbleColor = BubbleColor.BLUE,
    val grid: Map<Pair<Int, Int>, Bubble> = emptyMap(),
    val flyingBubble: FlyingBubble? = null,
    val aimTrajectory: AimTrajectory? = null,
    val particles: List<Particle> = emptyList(),
    val floatingTexts: List<FloatingText> = emptyList(),
    val comboCount: Int = 0,
    val isSoundEnabled: Boolean = true,
    val isVibrationEnabled: Boolean = true,
    val topScores: List<HighScoreEntity> = emptyList(),
    val isReadyToShoot: Boolean = true
)

class BubbleShooterViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = HighScoreRepository(database.highScoreDao(), database.levelProgressDao())
    val soundManager = SoundManager(application)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val gridLogic = BubbleGridLogic(numColsEven = 8, maxRows = 13)

    var canvasWidth: Float = 1080f
    var canvasHeight: Float = 1920f
    var shooterCenter: Offset = Offset(540f, 1700f)

    private var gameLoopJob: Job? = null
    private var totalPoppedInGame = 0
    private var lastAimOffset: Offset? = null

    private var descentTimerAccumulator: Float = 0f
    private var alertTimerAccumulator: Float = 0f
    private var warningTimerAccumulator: Float = 0f

    init {
        viewModelScope.launch {
            repository.initDefaultLevels()
        }
        viewModelScope.launch {
            repository.highestScore.collect { maxScore ->
                _uiState.update { it.copy(highScore = maxScore ?: 0) }
            }
        }
        viewModelScope.launch {
            repository.topScores.collect { scores ->
                _uiState.update { it.copy(topScores = scores) }
            }
        }
        viewModelScope.launch {
            repository.allLevelProgress.collect { progressList ->
                _uiState.update { it.copy(levelProgressList = progressList) }
            }
        }
        startGame(1)
        startGameLoop()
    }

    fun navigateTo(screen: AppScreen) {
        _uiState.update { it.copy(currentScreen = screen) }
    }

    fun navigateBack() {
        when (_uiState.value.currentScreen) {
            AppScreen.GAMEPLAY -> navigateTo(AppScreen.LEVEL_SELECT)
            AppScreen.LEVEL_SELECT -> navigateTo(AppScreen.HOME)
            AppScreen.HOME -> {}
        }
    }

    fun startLevel(levelNumber: Int) {
        _uiState.update {
            it.copy(
                selectedLevel = levelNumber,
                currentScreen = AppScreen.GAMEPLAY
            )
        }
        startGame(levelNumber)
    }

    fun updateScreenDimensions(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        val changed = canvasWidth != width || canvasHeight != height
        canvasWidth = width
        canvasHeight = height

        val availableWidth = width * 0.96f
        val bubbleRadius = (availableWidth / (gridLogic.numColsEven * 2)).coerceIn(24f, 48f)
        val startX = (width - (gridLogic.numColsEven * 2 * bubbleRadius)) / 2f
        val startY = height * 0.08f

        gridLogic.bubbleRadius = bubbleRadius
        gridLogic.startX = startX
        gridLogic.startY = startY

        shooterCenter = Offset(width / 2f, height - bubbleRadius * 3.6f)

        if (changed) {
            val currentGrid = _uiState.value.grid
            if (currentGrid.isNotEmpty()) {
                val updatedGrid = currentGrid.mapValues { (_, bubble) ->
                    val newCenter = gridLogic.getBubbleCenter(bubble.row, bubble.col, _uiState.value.totalDescents)
                    bubble.copy(x = newCenter.x, y = newCenter.y)
                }
                _uiState.update { it.copy(grid = updatedGrid) }
            } else {
                startGame(_uiState.value.level)
            }
        }
    }

    fun startGame(level: Int = 1) {
        val tier = DifficultyTier.fromScore(_uiState.value.score)
        val initialGrid = LevelData.generateLevel(level, gridLogic, tier)
        val moves = LevelData.getMovesForLevel(level)
        val availableColors = getActiveColors(initialGrid, tier)

        descentTimerAccumulator = 0f
        alertTimerAccumulator = 0f
        warningTimerAccumulator = 0f

        val boss = if (level == 11) BossState(maxHp = 6, currentHp = 6) else null

        _uiState.update {
            it.copy(
                level = level,
                selectedLevel = level,
                movesLeft = moves,
                totalMoves = moves,
                bossState = boss,
                difficultyTier = tier,
                shotsUntilCeilingDescent = tier.maxShotsBeforeDescent,
                maxShotsForDescent = tier.maxShotsBeforeDescent,
                secondsUntilCeilingDescent = tier.descentTimerSeconds,
                totalDescents = 0,
                ceilingWarningAlert = false,
                tierUnlockAlert = null,
                gameStatus = GameStatus.PLAYING,
                grid = initialGrid,
                currentBubble = availableColors.randomOrNull() ?: BubbleColor.RED,
                nextBubble = availableColors.randomOrNull() ?: BubbleColor.BLUE,
                flyingBubble = null,
                aimTrajectory = null,
                comboCount = 0,
                isReadyToShoot = true
            )
        }
    }

    fun restartGame() {
        startGame(_uiState.value.level)
    }

    fun nextLevel() {
        val currentLvl = _uiState.value.level
        val nextLvl = if (currentLvl < 11) currentLvl + 1 else 1
        startGame(nextLvl)
    }

    private fun getActiveColors(grid: Map<Pair<Int, Int>, Bubble>, tier: DifficultyTier): List<BubbleColor> {
        val colors = grid.values
            .filter { !it.isPopping && !it.isFalling }
            .map { it.color }
            .distinct()
        return if (colors.isNotEmpty()) colors else tier.colors
    }

    fun onAim(touchOffset: Offset) {
        val state = _uiState.value
        if (state.currentScreen != AppScreen.GAMEPLAY) return
        if (state.gameStatus != GameStatus.PLAYING || !state.isReadyToShoot || state.flyingBubble != null) return

        val dx = touchOffset.x - shooterCenter.x
        val dy = touchOffset.y - shooterCenter.y

        if (dy >= -20f) {
            if (state.aimTrajectory != null) {
                _uiState.update { it.copy(aimTrajectory = null) }
            }
            return
        }

        lastAimOffset?.let { last ->
            val moveDistSq = (touchOffset.x - last.x) * (touchOffset.x - last.x) +
                    (touchOffset.y - last.y) * (touchOffset.y - last.y)
            if (moveDistSq < 16f) return
        }
        lastAimOffset = touchOffset

        val trajectory = calculateAimTrajectory(shooterCenter, dx, dy, state.totalDescents)
        _uiState.update { it.copy(aimTrajectory = trajectory) }
    }

    fun onReleaseAim() {
        lastAimOffset = null
        val state = _uiState.value
        val trajectory = state.aimTrajectory
        if (trajectory != null && trajectory.points.size >= 2 && state.isReadyToShoot && state.flyingBubble == null) {
            shootBubble(trajectory)
        }
        _uiState.update { it.copy(aimTrajectory = null) }
    }

    fun swapBubbles() {
        val state = _uiState.value
        if (state.flyingBubble != null || !state.isReadyToShoot) return
        _uiState.update {
            it.copy(
                currentBubble = state.nextBubble,
                nextBubble = state.currentBubble
            )
        }
        soundManager.vibrate(15)
        soundManager.play(SoundEffect.BOUNCE)
    }

    fun toggleSound() {
        soundManager.isSoundEnabled = !soundManager.isSoundEnabled
        _uiState.update { it.copy(isSoundEnabled = soundManager.isSoundEnabled) }
    }

    fun toggleVibration() {
        soundManager.isHapticsEnabled = !soundManager.isHapticsEnabled
        _uiState.update { it.copy(isVibrationEnabled = soundManager.isHapticsEnabled) }
    }

    fun pauseGame() {
        if (_uiState.value.gameStatus == GameStatus.PLAYING) {
            _uiState.update { it.copy(gameStatus = GameStatus.PAUSED) }
        }
    }

    fun resumeGame() {
        if (_uiState.value.gameStatus == GameStatus.PAUSED) {
            _uiState.update { it.copy(gameStatus = GameStatus.PLAYING) }
        }
    }

    private fun shootBubble(trajectory: AimTrajectory) {
        val p0 = trajectory.points[0]
        val p1 = trajectory.points[1]
        val dx = p1.x - p0.x
        val dy = p1.y - p0.y
        val len = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
        val speed = 2000f

        val flying = FlyingBubble(
            x = shooterCenter.x,
            y = shooterCenter.y,
            vx = (dx / len) * speed,
            vy = (dy / len) * speed,
            color = _uiState.value.currentBubble,
            radius = gridLogic.bubbleRadius
        )

        val newMoves = _uiState.value.movesLeft - 1

        _uiState.update {
            it.copy(
                flyingBubble = flying,
                movesLeft = newMoves,
                isReadyToShoot = false
            )
        }

        soundManager.play(SoundEffect.SHOOT)
        soundManager.vibrate(20)
    }

    private fun calculateAimTrajectory(
        start: Offset,
        dirX: Float,
        dirY: Float,
        totalDescents: Int
    ): AimTrajectory {
        val points = ArrayList<Offset>(6)
        points.add(start)

        val r = gridLogic.bubbleRadius
        val left = gridLogic.startX + r
        val right = gridLogic.startX + (gridLogic.numColsEven * 2 * r) - r
        val ceiling = gridLogic.startY + r
        val collisionDistSq = (2 * r * 0.95f) * (2 * r * 0.95f)
        val doubleR = 2 * r

        var currX = start.x
        var currY = start.y
        val len = sqrt(dirX * dirX + dirY * dirY).coerceAtLeast(0.001f)
        var vx = (dirX / len)
        var vy = (dirY / len)

        val grid = _uiState.value.grid
        var hitPoint: Offset? = null
        val step = 16f

        for (bounce in 0..2) {
            var hit = false
            for (i in 0 until 180) {
                currX += vx * step
                currY += vy * step

                // Wall bounces
                if (currX <= left) {
                    currX = left
                    points.add(Offset(currX, currY))
                    vx = -vx
                    break
                } else if (currX >= right) {
                    currX = right
                    points.add(Offset(currX, currY))
                    vx = -vx
                    break
                }

                // Ceiling hit
                if (currY <= ceiling) {
                    currY = ceiling
                    points.add(Offset(currX, currY))
                    hitPoint = Offset(currX, currY)
                    hit = true
                    break
                }

                val collisionBubble = grid.values.firstOrNull { bubble ->
                    !bubble.isPopping && !bubble.isFalling &&
                            abs(bubble.y - currY) <= doubleR &&
                            distanceSq(currX, currY, bubble.x, bubble.y) <= collisionDistSq
                }

                if (collisionBubble != null) {
                    points.add(Offset(currX, currY))
                    hitPoint = Offset(currX, currY)
                    hit = true
                    break
                }
            }
            if (hit) break
        }

        val targetHex = if (hitPoint != null) {
            gridLogic.findBestSnapSlot(grid, hitPoint.x, hitPoint.y, totalDescents)
        } else null

        return AimTrajectory(points = points, targetHitPoint = hitPoint, targetHex = targetHex)
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.nanoTime()
            while (isActive) {
                val now = System.nanoTime()
                val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.008f, 0.033f)
                lastTime = now

                if (_uiState.value.currentScreen == AppScreen.GAMEPLAY && _uiState.value.gameStatus == GameStatus.PLAYING) {
                    updatePhysics(dt)
                }
                delay(16)
            }
        }
    }

    private fun updatePhysics(dt: Float) {
        val state = _uiState.value
        val flying = state.flyingBubble
        val hasParticles = state.particles.isNotEmpty()
        val hasTexts = state.floatingTexts.isNotEmpty()
        val hasPoppingOrFalling = state.grid.values.any { it.isPopping || it.isFalling }

        descentTimerAccumulator += dt
        var newSecondsUntilDescent = state.secondsUntilCeilingDescent
        var shouldTriggerTimeDescent = false

        if (descentTimerAccumulator >= 1.0f) {
            descentTimerAccumulator -= 1.0f
            if (newSecondsUntilDescent > 0) {
                newSecondsUntilDescent--
                if (newSecondsUntilDescent == 0 && flying == null && state.isReadyToShoot) {
                    shouldTriggerTimeDescent = true
                }
            }
        }

        var currentUnlockAlert = state.tierUnlockAlert
        if (currentUnlockAlert != null) {
            alertTimerAccumulator += dt
            if (alertTimerAccumulator >= 3.0f) {
                alertTimerAccumulator = 0f
                currentUnlockAlert = null
            }
        }

        var isCeilingWarning = state.ceilingWarningAlert
        if (isCeilingWarning) {
            warningTimerAccumulator += dt
            if (warningTimerAccumulator >= 1.5f) {
                warningTimerAccumulator = 0f
                isCeilingWarning = false
            }
        }

        if (shouldTriggerTimeDescent) {
            descendCeiling()
            return
        }

        if (flying == null && !hasParticles && !hasTexts && !hasPoppingOrFalling &&
            newSecondsUntilDescent == state.secondsUntilCeilingDescent &&
            currentUnlockAlert == state.tierUnlockAlert &&
            isCeilingWarning == state.ceilingWarningAlert
        ) {
            return
        }

        val r = gridLogic.bubbleRadius
        val left = gridLogic.startX + r
        val right = gridLogic.startX + (gridLogic.numColsEven * 2 * r) - r
        val ceiling = gridLogic.startY + r
        val collisionDistSq = (2 * r * 0.92f) * (2 * r * 0.92f)
        val doubleR = 2 * r

        var updatedFlying = flying
        var needSnap = false
        var snapX = 0f
        var snapY = 0f
        var snapColor = BubbleColor.RED

        if (flying != null) {
            val totalDistance = sqrt(flying.vx * flying.vx + flying.vy * flying.vy) * dt
            val steps = (totalDistance / 12f).toInt().coerceAtLeast(1)
            val subDt = dt / steps

            var curX = flying.x
            var curY = flying.y
            var curVx = flying.vx
            var curVy = flying.vy

            for (s in 0 until steps) {
                curX += curVx * subDt
                curY += curVy * subDt

                if (curX <= left) {
                    curX = left
                    curVx = -curVx
                    soundManager.play(SoundEffect.BOUNCE)
                } else if (curX >= right) {
                    curX = right
                    curVx = -curVx
                    soundManager.play(SoundEffect.BOUNCE)
                }

                if (curY <= ceiling) {
                    needSnap = true
                    snapX = curX
                    snapY = curY
                    snapColor = flying.color
                    break
                }

                val hitBubble = state.grid.values.firstOrNull { b ->
                    !b.isPopping && !b.isFalling &&
                            abs(b.y - curY) <= doubleR &&
                            distanceSq(curX, curY, b.x, b.y) <= collisionDistSq
                }

                if (hitBubble != null) {
                    needSnap = true
                    snapX = curX
                    snapY = curY
                    snapColor = flying.color
                    break
                }
            }

            if (needSnap) {
                updatedFlying = null
            } else {
                updatedFlying = flying.copy(x = curX, y = curY, vx = curVx, vy = curVy)
            }
        }

        val updatedParticles = if (hasParticles) {
            state.particles.mapNotNull { p ->
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.vy += 450f * dt
                p.life -= p.decay
                if (p.life > 0) p else null
            }
        } else emptyList()

        val updatedTexts = if (hasTexts) {
            state.floatingTexts.mapNotNull { t ->
                t.y -= 75f * dt
                t.alpha -= 0.028f
                if (t.alpha > 0f) t else null
            }
        } else emptyList()

        var updatedGrid = state.grid
        if (hasPoppingOrFalling) {
            val modGrid = state.grid.toMutableMap()
            val toRemove = mutableListOf<Pair<Int, Int>>()

            for ((pos, bubble) in modGrid) {
                if (bubble.isPopping) {
                    bubble.popProgress += dt * 5.5f
                    if (bubble.popProgress >= 1f) {
                        toRemove.add(pos)
                    }
                } else if (bubble.isFalling) {
                    bubble.fallVy += 1700f * dt
                    bubble.x += bubble.fallVx * dt
                    bubble.y += bubble.fallVy * dt
                    bubble.fallAlpha -= dt * 1.6f
                    if (bubble.y > canvasHeight || bubble.fallAlpha <= 0f) {
                        toRemove.add(pos)
                    }
                }
            }

            if (toRemove.isNotEmpty()) {
                toRemove.forEach { modGrid.remove(it) }
            }
            updatedGrid = modGrid
        }

        if (needSnap) {
            snapBubbleToGrid(snapX, snapY, snapColor, updatedParticles.toMutableList(), updatedTexts.toMutableList())
        } else {
            _uiState.update {
                it.copy(
                    flyingBubble = updatedFlying,
                    particles = updatedParticles,
                    floatingTexts = updatedTexts,
                    grid = updatedGrid,
                    secondsUntilCeilingDescent = newSecondsUntilDescent,
                    tierUnlockAlert = currentUnlockAlert,
                    ceilingWarningAlert = isCeilingWarning
                )
            }
        }
    }

    private fun snapBubbleToGrid(
        hitX: Float,
        hitY: Float,
        color: BubbleColor,
        currentParticles: MutableList<Particle>,
        currentTexts: MutableList<FloatingText>
    ) {
        val state = _uiState.value
        val slot = gridLogic.findBestSnapSlot(state.grid, hitX, hitY, state.totalDescents)

        if (slot == null) {
            _uiState.update {
                it.copy(
                    flyingBubble = null,
                    particles = currentParticles,
                    floatingTexts = currentTexts,
                    isReadyToShoot = true
                )
            }
            return
        }

        val center = gridLogic.getBubbleCenter(slot.first, slot.second, state.totalDescents)
        val newBubble = Bubble(
            row = slot.first,
            col = slot.second,
            color = color,
            x = center.x,
            y = center.y
        )

        val newGrid = state.grid.toMutableMap()
        newGrid[slot] = newBubble

        val matches = gridLogic.findMatches(newGrid, slot.first, slot.second, state.totalDescents)
        var newScore = state.score
        var newCombo = state.comboCount
        var shotsRemainingForDescent = state.shotsUntilCeilingDescent
        var shouldTriggerCeilingDrop = false
        var updatedBossState = state.bossState

        if (matches.size >= 3) {
            newCombo += 1
            val comboBonus = if (newCombo > 1) 1.5f * newCombo else 1f
            val matchPoints = (matches.size * 100 * comboBonus).toInt()
            newScore += matchPoints
            totalPoppedInGame += matches.size

            soundManager.play(SoundEffect.POP)
            soundManager.vibrate(25)

            matches.forEach { pos ->
                val b = newGrid[pos]
                if (b != null) {
                    b.isPopping = true
                    spawnBurstParticles(b.x, b.y, b.color.primary, currentParticles)
                }
            }

            // Boss damage handling (Level 11)
            if (updatedBossState != null) {
                val newHp = (updatedBossState.currentHp - 1).coerceAtLeast(0)
                updatedBossState = updatedBossState.copy(
                    currentHp = newHp,
                    isEnraged = newHp <= 2,
                    isDefeated = newHp == 0
                )
                currentTexts.add(
                    FloatingText(
                        x = center.x,
                        y = center.y - 70f,
                        text = if (newHp == 0) "💥 BOSS DEFEATED!" else "💥 BOSS HIT! -1 HP ($newHp/6)",
                        color = Color(0xFFEF4444),
                        isCombo = true
                    )
                )
            }

            currentTexts.add(
                FloatingText(
                    x = center.x,
                    y = center.y - 30f,
                    text = if (newCombo > 1) "+$matchPoints (x$newCombo!)" else "+$matchPoints",
                    color = color.primary,
                    isCombo = newCombo > 1
                )
            )

            val orphans = gridLogic.findFloatingBubbles(newGrid, excluding = matches, totalDescents = state.totalDescents)
            if (orphans.isNotEmpty()) {
                val orphanPoints = orphans.size * 250
                newScore += orphanPoints
                totalPoppedInGame += orphans.size

                orphans.forEach { pos ->
                    val ob = newGrid[pos]
                    if (ob != null) {
                        ob.isFalling = true
                        ob.fallVx = Random.nextFloat() * 220f - 110f
                        ob.fallVy = Random.nextFloat() * 120f
                    }
                }

                currentTexts.add(
                    FloatingText(
                        x = center.x,
                        y = center.y + 40f,
                        text = "Drop +$orphanPoints!",
                        color = Color(0xFFFFD700)
                    )
                )
                soundManager.play(SoundEffect.COMBO)
            }
        } else {
            newCombo = 0
            soundManager.play(SoundEffect.BOUNCE)
            shotsRemainingForDescent--
            if (shotsRemainingForDescent <= 0) {
                shouldTriggerCeilingDrop = true
            }
        }

        val previousTier = state.difficultyTier
        val newTier = DifficultyTier.fromScore(newScore)
        var newTierAlert = state.tierUnlockAlert

        if (newTier != previousTier) {
            soundManager.play(SoundEffect.TIER_UP)
            soundManager.vibrate(50)
            val newlyUnlockedColor = newTier.colors.last().displayName
            newTierAlert = "TIER UP: ${newTier.title}! $newlyUnlockedColor UNLOCKED!"
            alertTimerAccumulator = 0f
        }

        val remainingColors = getActiveColors(newGrid, newTier)
        val nextCurrent = state.nextBubble
        val newNextBubble = remainingColors.randomOrNull() ?: newTier.colors.random()

        val activeCount = newGrid.values.count { !it.isPopping && !it.isFalling }
        var status = state.gameStatus

        // Victory condition: all active bubbles cleared OR boss HP reached 0
        val isBossCleared = updatedBossState != null && updatedBossState.currentHp == 0
        if (activeCount == 0 || isBossCleared) {
            status = GameStatus.LEVEL_CLEARED
            val clearBonus = state.movesLeft * 300 + (if (isBossCleared) 5000 else 0)
            newScore += clearBonus
            currentTexts.add(
                FloatingText(
                    x = canvasWidth / 2f,
                    y = canvasHeight / 2f,
                    text = if (isBossCleared) "VICTORY! OVERLORD DEFEATED! +$clearBonus" else "LEVEL ${state.level} CLEAR! +$clearBonus",
                    color = Color(0xFF10B981)
                )
            )
            soundManager.play(SoundEffect.WIN)
            saveScore(newScore)

            // Calculate stars
            val starsEarned = when {
                state.movesLeft >= 10 -> 3
                state.movesLeft >= 4 -> 2
                else -> 1
            }
            viewModelScope.launch {
                repository.completeLevel(state.level, starsEarned, newScore)
            }
        } else {
            val lowestRow = newGrid.values.filter { !it.isPopping && !it.isFalling }.maxOfOrNull { it.row } ?: 0
            if (state.movesLeft <= 0 || lowestRow >= 11) {
                status = GameStatus.GAME_OVER
                soundManager.play(SoundEffect.LOSE)
                saveScore(newScore)
            }
        }

        _uiState.update {
            it.copy(
                grid = newGrid,
                flyingBubble = null,
                score = newScore,
                bossState = updatedBossState,
                highScore = maxOf(newScore, state.highScore),
                difficultyTier = newTier,
                shotsUntilCeilingDescent = if (shouldTriggerCeilingDrop) newTier.maxShotsBeforeDescent else shotsRemainingForDescent,
                maxShotsForDescent = newTier.maxShotsBeforeDescent,
                tierUnlockAlert = newTierAlert,
                comboCount = newCombo,
                currentBubble = nextCurrent,
                nextBubble = newNextBubble,
                particles = currentParticles,
                floatingTexts = currentTexts,
                gameStatus = status,
                isReadyToShoot = true
            )
        }

        if (shouldTriggerCeilingDrop && status == GameStatus.PLAYING) {
            descendCeiling()
        }
    }

    fun descendCeiling() {
        val state = _uiState.value
        if (state.gameStatus != GameStatus.PLAYING) return

        val newTotalDescents = state.totalDescents + 1
        val tier = state.difficultyTier
        val newGrid = mutableMapOf<Pair<Int, Int>, Bubble>()
        var hitDangerFloor = false

        for ((pos, bubble) in state.grid) {
            val newRow = bubble.row + 1
            if (newRow >= 11 && !bubble.isPopping && !bubble.isFalling) {
                hitDangerFloor = true
            }
            val center = gridLogic.getBubbleCenter(newRow, bubble.col, newTotalDescents)
            newGrid[Pair(newRow, bubble.col)] = bubble.copy(
                row = newRow,
                x = center.x,
                y = center.y
            )
        }

        val colsInRowZero = gridLogic.getColsForRow(0, newTotalDescents)
        for (c in 0 until colsInRowZero) {
            val color = tier.colors.random()
            val center = gridLogic.getBubbleCenter(0, c, newTotalDescents)
            newGrid[Pair(0, c)] = Bubble(
                row = 0,
                col = c,
                color = color,
                x = center.x,
                y = center.y
            )
        }

        soundManager.play(SoundEffect.CLANK)
        soundManager.vibrate(40)
        warningTimerAccumulator = 0f

        val updatedTexts = state.floatingTexts.toMutableList()
        updatedTexts.add(
            FloatingText(
                x = canvasWidth / 2f,
                y = gridLogic.startY + gridLogic.bubbleRadius * 2f,
                text = "⚠️ CEILING DESCENDED!",
                color = Color(0xFFEF4444),
                isCombo = true
            )
        )

        var newStatus = state.gameStatus
        if (hitDangerFloor) {
            newStatus = GameStatus.GAME_OVER
            soundManager.play(SoundEffect.LOSE)
            saveScore(state.score)
        }

        _uiState.update {
            it.copy(
                grid = newGrid,
                totalDescents = newTotalDescents,
                shotsUntilCeilingDescent = tier.maxShotsBeforeDescent,
                secondsUntilCeilingDescent = tier.descentTimerSeconds,
                ceilingWarningAlert = true,
                floatingTexts = updatedTexts,
                gameStatus = newStatus
            )
        }
    }

    private fun spawnBurstParticles(x: Float, y: Float, color: Color, list: MutableList<Particle>) {
        val count = 10
        for (i in 0 until count) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 280f + 100f
            list.add(
                Particle(
                    x = x,
                    y = y,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    color = color,
                    radius = Random.nextFloat() * 3.5f + 2.5f
                )
            )
        }
    }

    private fun saveScore(score: Int) {
        viewModelScope.launch {
            repository.saveScore(
                score = score,
                bubblesPopped = totalPoppedInGame,
                level = _uiState.value.level
            )
        }
    }

    private fun distanceSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return dx * dx + dy * dy
    }
}
