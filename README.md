# CallMemo

CallMemo is an offline-first Android application designed to help you capture important notes during or immediately after phone calls. It features a floating overlay UI that appears contextually based on phone state changes and stores all data locally on your device.

## Key Features

- **Contextual Overlay**: A floating Jetpack Compose UI appears during incoming/outgoing calls or after they end.
- **Note Management**: Quickly save notes linked to phone numbers using a local Room database.
- **Contact Integration**: Automatically displays contact names for known numbers from your device's address book.
- **Smart History**: View recent notes for the current caller directly within the overlay.
- **Searchable Archives**: Browse and search through your entire history of call notes by name, number, or content in the main app.
- **Privacy Focused**: Operates entirely offline with no network permissions required.

## Project Structure

```text
app/src/main/java/com/example/androidcallnotes/
├── MainActivity.kt            Main dashboard for permissions and note history
├── ContactUtils.kt            Helper for system contact name resolution
├── data/                      Room database, entities, and repository
├── receiver/                  BroadcastReceiver for telephony events
├── service/                   Foreground Service and Overlay UI (Bubble/Content)
└── ui/theme/                  Material 3 theme definitions
```

## Data Flow

1. **Telephony Event**: `CallReceiver` detects state changes (Ringing, Offhook, Idle).
2. **Service Launch**: `OverlayService` starts as a foreground service.
3. **UI Interaction**: User interacts with a compact bubble or expands it to the full `OverlayContent`.
4. **Persistence**: Notes are saved via `CallNoteRepository` into the local SQLite/Room database.

## Technical Stack

- **UI**: Jetpack Compose for both the overlay and main application.
- **Storage**: Room Persistence Library.
- **Concurreny**: Kotlin Coroutines and Flow.
- **Service**: Foreground Service with `SYSTEM_ALERT_WINDOW` for the floating UI.

## Permissions

To function correctly, CallMemo requires the following permissions:

- `READ_PHONE_STATE`: To detect when a call starts or ends.
- `READ_CALL_LOG`: To reliably identify the phone number for all call types.
- `READ_CONTACTS`: To show contact names instead of just numbers.
- `SYSTEM_ALERT_WINDOW`: To display the floating overlay over other apps.
- `POST_NOTIFICATIONS`: For the required foreground service notification (Android 13+).

## Getting Started

1. Open the project in **Android Studio**.
2. Build and run the app on a physical device (recommended for telephony features).
3. Grant the required permissions on the first launch.
4. Try making a call or receiving one; the CallMemo bubble will appear on your screen.

## Development

- **Build**: `./gradlew assembleDebug`
- **Clean**: `./gradlew clean`
- **Check**: Documentation and requirements are maintained in `AGENTS.md` and `REQUIREMENTS.md`.
