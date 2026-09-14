package com.parkspot.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ParkingSpot::class], version = 2, exportSchema = true)
abstract class ParkingDatabase : RoomDatabase() {

    abstract fun parkingDao(): ParkingDao

    companion object {
        @Volatile
        private var instance: ParkingDatabase? = null

        /** Adds the "this spot was saved automatically" flag. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE parking_spots ADD COLUMN savedAutomatically INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun getInstance(context: Context): ParkingDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ParkingDatabase::class.java,
                    "parkspot.db",
                ).addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}
