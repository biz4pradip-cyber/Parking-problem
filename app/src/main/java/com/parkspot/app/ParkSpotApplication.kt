package com.parkspot.app

import android.app.Application
import android.content.Context
import com.parkspot.app.bluetooth.AutoParkNotifications
import com.parkspot.app.bluetooth.PairedDevices
import com.parkspot.app.data.AutoParkSettings
import com.parkspot.app.data.ParkingDatabase
import com.parkspot.app.data.ParkingRepository
import com.parkspot.app.data.PhotoStore
import com.parkspot.app.location.CompassClient
import com.parkspot.app.location.LocationClient
import com.parkspot.app.reminder.ReminderNotifications
import com.parkspot.app.reminder.ReminderScheduler

/** Hand-rolled dependency container — the app is small enough not to need a DI framework. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val photoStore: PhotoStore by lazy { PhotoStore(appContext) }
    val locationClient: LocationClient by lazy { LocationClient(appContext) }
    val compassClient: CompassClient by lazy { CompassClient(appContext) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(appContext) }
    val autoParkSettings: AutoParkSettings by lazy { AutoParkSettings(appContext) }
    val pairedDevices: PairedDevices by lazy { PairedDevices(appContext) }

    val repository: ParkingRepository by lazy {
        ParkingRepository(
            dao = ParkingDatabase.getInstance(appContext).parkingDao(),
            photoStore = photoStore,
            reminders = reminderScheduler,
        )
    }
}

class ParkSpotApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderNotifications.createChannel(this)
        AutoParkNotifications.createChannel(this)
    }
}
