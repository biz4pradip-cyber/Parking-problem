package com.parkspot.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ParkingSpot::class], version = 1, exportSchema = true)
abstract class ParkingDatabase : RoomDatabase() {

    abstract fun parkingDao(): ParkingDao

    companion object {
        @Volatile
        private var instance: ParkingDatabase? = null

        fun getInstance(context: Context): ParkingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ParkingDatabase::class.java,
                    "parkspot.db",
                ).build().also { instance = it }
            }
    }
}
