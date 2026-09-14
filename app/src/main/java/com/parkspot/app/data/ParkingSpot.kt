package com.parkspot.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One remembered parking position.
 *
 * At most one row has [isActive] set: the car you are currently looking for. Everything else is
 * history.
 */
@Entity(tableName = "parking_spots")
data class ParkingSpot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val latitude: Double,
    val longitude: Double,
    /** Radius of the GPS fix in metres, as reported when the spot was saved. */
    val accuracyMeters: Float,
    val savedAt: Long,
    /** Garage level / floor, e.g. `P3`. */
    val level: String = "",
    /** Row or bay label, e.g. `B-47`. */
    val spotLabel: String = "",
    val note: String = "",
    /** `content://` URI of the photo taken at the spot, if any. */
    val photoUri: String? = null,
    /** When to fire the "your parking runs out" notification. */
    val reminderAt: Long? = null,
    val isActive: Boolean = true,
    /** When the car was found again; only set on archived rows. */
    val clearedAt: Long? = null,
) {
    val hasDetails: Boolean
        get() = level.isNotBlank() || spotLabel.isNotBlank() || note.isNotBlank()

    /** Short one-line description, e.g. `P3 · B-47`. */
    val label: String
        get() = listOf(level, spotLabel).filter { it.isNotBlank() }.joinToString(" · ")
}
