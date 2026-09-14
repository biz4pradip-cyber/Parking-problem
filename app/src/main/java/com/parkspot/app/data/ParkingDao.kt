package com.parkspot.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ParkingDao {

    @Query("SELECT * FROM parking_spots WHERE isActive = 1 ORDER BY savedAt DESC LIMIT 1")
    fun observeActive(): Flow<ParkingSpot?>

    @Query("SELECT * FROM parking_spots WHERE isActive = 1 ORDER BY savedAt DESC LIMIT 1")
    suspend fun getActive(): ParkingSpot?

    @Query("SELECT * FROM parking_spots WHERE isActive = 0 ORDER BY savedAt DESC")
    fun observeHistory(): Flow<List<ParkingSpot>>

    @Query("SELECT * FROM parking_spots WHERE isActive = 0")
    suspend fun getHistory(): List<ParkingSpot>

    @Query("SELECT * FROM parking_spots WHERE id = :id")
    suspend fun getById(id: Long): ParkingSpot?

    @Query("UPDATE parking_spots SET isActive = 0, clearedAt = :clearedAt, reminderAt = NULL WHERE isActive = 1")
    suspend fun archiveActive(clearedAt: Long)

    @Insert
    suspend fun insert(spot: ParkingSpot): Long

    @Update
    suspend fun update(spot: ParkingSpot)

    @Delete
    suspend fun delete(spot: ParkingSpot)

    @Query("DELETE FROM parking_spots WHERE isActive = 0")
    suspend fun deleteHistory()
}
