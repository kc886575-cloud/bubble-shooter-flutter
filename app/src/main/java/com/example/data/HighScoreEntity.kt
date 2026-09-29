package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val score: Int,
    val bubblesPopped: Int,
    val level: Int,
    val dateTimestamp: Long = System.currentTimeMillis()
)
