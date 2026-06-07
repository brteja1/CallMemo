# CallMemo

An offline Android app that detects when a phone call ends, opens a floating Jetpack Compose overlay, and stores notes locally in Room keyed by the caller's phone number.

## Objective

Capture a note immediately after a call ends, then save it on-device with the phone number that triggered the overlay.

## Open In Android Studio

1. Launch Android Studio.
2. Choose `File > Open`.
3. Select the project root:
   `/linuxdev/localgit/CallMemo`
4. Let Gradle sync complete.
5. If Android Studio asks for a Gradle JDK, use the embedded JDK or JDK 17.

## Project Structure

```text
app/src/main/java/com/example/androidcallnotes/
├── CallNotesApplication.kt    Application container for the Room database and repository
├── MainActivity.kt            Entry screen for permissions and overlay setup
├── CallNotesContract.kt       Shared constants for the overlay flow
├── data/
│   ├── CallNote.kt            Room entity for saved call notes
│   ├── CallNoteDao.kt         Room DAO for inserts, queries, and deletes
│   ├── CallNotesDatabase.kt   Room database bootstrap
│   └── CallNoteRepository.kt  Background-safe data access layer
├── receiver/
│   └── CallReceiver.kt        Phone-state broadcast receiver
└── service/
    ├── OverlayService.kt      Foreground service that owns the floating window
    └── OverlayContent.kt      Compose UI rendered inside the overlay
```

## Data Flow

```text
Phone state change
    -> CallReceiver
    -> OverlayService (foreground launch)
    -> OverlayContent (shows phone number and note editor)
    -> CallNoteRepository
    -> Room database
```

## What The App Uses

- `BroadcastReceiver` for `TelephonyManager.ACTION_PHONE_STATE_CHANGED`
- Foreground `OverlayService` for the floating note UI
- Room database for offline persistence
- Jetpack Compose for the overlay content

## Implementation Phases

1. Call detection with a phone-state receiver and explicit foreground service launch.
2. Local Room storage for notes keyed by phone number.
3. Overlay UI with a contextual header, text input, save/cancel actions, and recent note preview.

## Required Permissions

The app does not use any network permissions.

At runtime, you may need to grant:

- `READ_PHONE_STATE` to detect call state changes
- `POST_NOTIFICATIONS` on Android 13+ so the foreground service notification can appear
- `SYSTEM_ALERT_WINDOW` so the overlay can draw above other apps

## How To Test

1. Install and run the app on a device or emulator that supports phone-state broadcasts.
2. Grant the permissions requested by the app.
3. Make or simulate a call ending.
4. The overlay should appear with the captured phone number in the header.
5. Enter a note and tap `Save` to store it in the local Room database.

## Notes

- All data stays on-device.
- The overlay uses keyboard-friendly window flags so text entry works inside the floating service window.

## Troubleshooting

- If Gradle sync fails, make sure Android Studio is using a recent embedded JDK or JDK 17.
- If the overlay does not appear, confirm `SYSTEM_ALERT_WINDOW` is enabled in system settings.
- If call-state events do not arrive, verify `READ_PHONE_STATE` is granted and the app is installed on a device that supports telephony broadcasts.
- If the foreground service notification does not show on Android 13+, grant `POST_NOTIFICATIONS`.

## Emulator Or Device

- A physical phone is the most reliable way to test call-state handling and overlay behavior.
- Some emulators do not emit real phone-state broadcasts, so the overlay flow may not trigger there.
- If you test on an emulator, you may need to simulate call-state changes manually or use an image/system image that supports telephony.
