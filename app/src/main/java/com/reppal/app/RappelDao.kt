package com.reppal.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RappelDao {
    @Query("SELECT * FROM table_rappels ORDER BY id DESC")
    fun getAllRappels(): Flow<List<Rappel>>

    @Query("SELECT * FROM table_rappels")
    suspend fun getAllRappelsSync(): List<Rappel>

    @Query("SELECT * FROM table_rappels WHERE id = :id LIMIT 1")
    suspend fun getRappelById(id: Int): Rappel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRappel(rappel: Rappel): Long

    @Delete
    suspend fun deleteRappel(rappel: Rappel)
}