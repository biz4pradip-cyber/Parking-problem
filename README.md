# ParkSpot — find my car

An offline Android app that remembers where you parked and walks you back to it.

You tap **Park here** when you get out of the car. The app takes a GPS fix and (optionally) a
photo, the level and bay number, and a note. When you come back, **Find my car** shows a compass
pointer to the car and the distance left, updating live as you walk.

Everything is stored on the device. There is no account, no network call and no analytics — the
app has no internet permission at all.

## What it does

| | |
|---|---|
| **Save a spot** | One tap takes a high-accuracy fix, falling back to the last known position when the sky is blocked (underground garages). Saving a new spot archives the old one. |
| **Find the car** | A dial points at the car and shows the remaining distance. The bearing is computed against **true** north: the compass reading is corrected by the local magnetic declination, which is over 20° in parts of the world. |
| **Last few metres** | GPS is worth ±5–20 m at best, so the details you saved — level, bay, note and photo — are shown right on the find screen, which is what actually gets you to the car. |
| **Hands-free parking** | Pick your car's Bluetooth device once, and losing that connection — the moment you step out of the car — saves the spot by itself. Reconnecting clears it again, because you are driving. |
| **Parking reminder** | Set 30 min / 1 h / 2 h and get a notification before the meter runs out. Re-armed after a reboot. |
| **Open in Maps / Share** | Hands the coordinates to any maps app, or shares a link so someone else can find the car. |
| **History** | Past spots with the date, how long you were parked, and the photo. Deletable individually or all at once. |

Niceties: the screen stays awake while you are walking to the car, the pointer always turns the
short way round instead of unwinding through 359°, and the app tells you to calibrate the compass
when the sensor reports low accuracy.

## Getting the APK

Every push runs [`.github/workflows/build-apk.yml`](.github/workflows/build-apk.yml), which runs
the unit tests and builds the APK on a GitHub runner. Open the run under the repository's
**Actions** tab and download **parkspot-debug-apk** from its Artifacts section. It is signed with
the standard debug key, so it installs on a device as-is:

```bash
adb install -r parkspot-debug.apk      # or just open the file on the phone
```

Sideloading needs "install unknown apps" enabled for whichever app opens the file. For a
Play-signable build, `./gradlew assembleRelease` with your own signing config in `app/build.gradle.kts`.

## How the automatic mode works

Losing the car stereo is the most reliable "I have just parked" signal a phone gets, so that is
what the app listens for:

1. A manifest-declared receiver watches `ACTION_ACL_CONNECTED` / `ACTION_ACL_DISCONNECTED`. Both
   are exempt from Android's implicit-broadcast restrictions, so they still arrive with the app
   closed.
2. On a disconnect from *your* chosen device it starts a short foreground service. Receiving a
   Bluetooth broadcast that requires `BLUETOOTH_CONNECT` is one of the documented exemptions from
   the Android 12 background-start restrictions, and a `location`-typed foreground service is what
   lets the fix be taken at all with the app in the background — no background-location permission
   needed.
3. The service takes one fix, stores the spot, posts a quiet notification and stops. It runs for
   seconds, not for the time you are parked.
4. Reconnecting to the same device archives the spot: you are in the car, so there is nothing to
   walk back to.

A stereo that drops and reconnects mid-drive will not litter the history — a disconnect within
three minutes of the last save is ignored.

Worth knowing: some manufacturers' battery optimisation kills manifest receivers for apps they
consider idle. If automatic saves stop happening, exclude ParkSpot from battery optimisation. The
manual **Park here** button always works regardless.

## Building it

The project is a standard Gradle/Android Studio project — no API keys, no Google Maps SDK, no
`local.properties` edits needed.

```bash
# In Android Studio: File ▸ Open ▸ this directory, then Run.
# From a shell with the Android SDK installed (ANDROID_HOME set):
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/
./gradlew installDebug         # build and install on a connected device
./gradlew test                 # JVM unit tests (geo maths and formatting)
```

Requirements: JDK 17, Android SDK 35, `minSdk 26` (Android 8.0). A physical device is
strongly recommended — the emulator has no magnetometer, so the pointer stays grey there while
the distance still works.

## How it is put together

```
app/src/main/java/com/parkspot/app/
├── ParkSpotApplication.kt      AppContainer — hand-rolled DI, no framework
├── MainActivity.kt             single activity, Compose only
├── data/                       Room entity + DAO + repository, photo file store
├── location/
│   ├── LocationClient.kt       fused provider wrapped in coroutines/Flow
│   └── CompassClient.kt        rotation-vector sensor → smoothed heading Flow
├── reminder/                   AlarmManager scheduling, notification, boot re-arm
├── ui/
│   ├── ParkSpotApp.kt          navigation host
│   ├── ParkingViewModel.kt     all screen state, derived in one place
│   ├── components/             CompassDial, details sheet, permission card
│   └── screens/                Home, Find, History
└── util/                       GeoUtils (haversine, bearing) + Formatters
```

Kotlin, Jetpack Compose (Material 3), Room, Play Services Location, Coil. State flows from Room
through the repository into one `ParkingViewModel`; the two screen states (`HomeUiState`,
`FindUiState`) derive everything else — distance, bearing, arrow rotation, "you have arrived" —
so the composables only draw.

`GeoUtils` and `Formatters` deliberately avoid Android types so the maths is covered by plain JVM
unit tests (`./gradlew test`).

## Permissions, and why

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Save and find the spot. Coarse alone works, just less precisely. |
| `POST_NOTIFICATIONS` | Only requested when you set a parking reminder. |
| `RECEIVE_BOOT_COMPLETED` | Re-arms a pending reminder after a reboot. |
| `VIBRATE` | The reminder notification. |

No camera permission is declared: photos are taken by handing a `FileProvider` URI to the camera
app, so the picture lands in app-private storage without any storage access.

## Known limits

- GPS does not work well under concrete. The first fix in a garage can be poor or unavailable —
  which is exactly why the photo and the level/bay fields are there, and why the accuracy of the
  saved fix is always shown rather than hidden.
- The pointer assumes you are holding the phone reasonably flat, like a map. Held vertically, the
  heading gets noisy; the distance stays correct either way.
- There is no map view in the app itself; **Open in Maps** hands off to whichever maps app you
  already use, which keeps the app free of API keys and of tracking.
