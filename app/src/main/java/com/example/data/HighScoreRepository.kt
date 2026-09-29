package com.example.data

import kotlinx.coroutines.flow.Flow

class HighScoreRepository(
    private val dao: HighScoreDao,
    private val levelDao: LevelProgressDao
) {
    val topScores: Flow<List<HighScoreEntity>> = dao.getTopScores()
    val highestScore: Flow<Int?> = dao.getHighestScore()
    val allLevelProgress: Flow<List<LevelProgressEntity>> = levelDao.getAllProgress()

    suspend fun initDefaultLevels() {
        val initialList = mutableListOf<LevelProgressEntity>()
        // 10 Regular levels + 1 Boss level (Level 11)
        for (i in 1..11) {
            initialList.add(
                LevelProgressEntity(
                    levelNumber = i,
                    isUnlocked = (i == 1), // Only Level 1 unlocked initially
                    stars = 0,
                    bestScore = 0,
                    isCompleted = false
                )
            )
        }
        levelDao.insertAll(initialList)
    }

    suspend fun saveScore(score: Int, bubblesPopped: Int, level: Int) {
        if (score > 0) {
            dao.insertScore(
                HighScoreEntity(
                    score = score,
                    bubblesPopped = bubblesPopped,
                    level = level
                )
            )
        }
    }

    suspend fun completeLevel(level: Int, stars: Int, score: Int) {
        val current = levelDao.getProgressForLevel(level)
        val bestScore = maxOf(score, current?.bestScore ?: 0)
        val bestStars = maxOf(stars, current?.stars ?: 0)

        levelDao.insertOrUpdate(
            LevelProgressEntity(
                levelNumber = level,
                isUnlocked = true,
                stars = bestStars,
                bestScore = bestScore,
                isCompleted = true
            )
        )

        // Unlock next level (including Boss Level 11!)
        if (level < 11) {
            val next = levelDao.getProgressForLevel(level + 1)
            levelDao.insertOrUpdate(
                LevelProgressEntity(
                    levelNumber = level + 1,
                    isUnlocked = true,
                    stars = next?.stars ?: 0,
                    bestScore = next?.bestScore ?: 0,
                    isCompleted = next?.isCompleted ?: false
                )
            )
        }
    }
}
