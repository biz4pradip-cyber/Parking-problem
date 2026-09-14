package com.parkspot.app.data

import com.parkspot.app.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow

/** Single source of truth for parking spots, photos and their reminders. */
class ParkingRepository(
    private val dao: ParkingDao,
    private val photoStore: PhotoStore,
    private val reminders: ReminderScheduler,
) {

    val activeSpot: Flow<ParkingSpot?> = dao.observeActive()

    val history: Flow<List<ParkingSpot>> = dao.observeHistory()

    /** Remembers a new position, archiving whatever was parked before. */
    suspend fun saveSpot(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Float,
        savedAt: Long = System.currentTimeMillis(),
    ): ParkingSpot {
        archiveActive(savedAt)
        val spot = ParkingSpot(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            savedAt = savedAt,
            isActive = true,
        )
        val id = dao.insert(spot)
        return spot.copy(id = id)
    }

    /** The car has been found: move the active spot into history. */
    suspend fun markFound() = archiveActive(System.currentTimeMillis())

    suspend fun updateDetails(spot: ParkingSpot, level: String, spotLabel: String, note: String) {
        dao.update(
            spot.copy(
                level = level.trim(),
                spotLabel = spotLabel.trim(),
                note = note.trim(),
            ),
        )
    }

    /** Attaches (or clears) the photo of a spot, deleting the file it replaces. */
    suspend fun setPhoto(spot: ParkingSpot, photoUri: String?) {
        if (spot.photoUri != null && spot.photoUri != photoUri) {
            photoStore.delete(spot.photoUri)
        }
        dao.update(spot.copy(photoUri = photoUri))
    }

    /** Sets or clears the "parking runs out" reminder for a spot. */
    suspend fun setReminder(spot: ParkingSpot, remindAt: Long?) {
        dao.update(spot.copy(reminderAt = remindAt))
        if (remindAt == null) {
            reminders.cancel(spot.id)
        } else {
            reminders.schedule(spot.id, remindAt)
        }
    }

    suspend fun delete(spot: ParkingSpot) {
        reminders.cancel(spot.id)
        photoStore.delete(spot.photoUri)
        dao.delete(spot)
    }

    suspend fun clearHistory() {
        dao.getHistory().forEach { photoStore.delete(it.photoUri) }
        dao.deleteHistory()
    }

    /** Re-arms the pending reminder after a reboot or an app update, which clear alarms. */
    suspend fun rescheduleActiveReminder() {
        val spot = dao.getActive() ?: return
        val remindAt = spot.reminderAt ?: return
        if (remindAt > System.currentTimeMillis()) reminders.schedule(spot.id, remindAt)
    }

    suspend fun getSpot(id: Long): ParkingSpot? = dao.getById(id)

    private suspend fun archiveActive(at: Long) {
        dao.getActive()?.let { reminders.cancel(it.id) }
        dao.archiveActive(at)
    }
}
