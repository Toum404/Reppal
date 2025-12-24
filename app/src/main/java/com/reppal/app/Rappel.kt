package com.reppal.app

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "table_rappels")
data class Rappel(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val titre: String,
    val description: String,
    val timestamp: Long
)