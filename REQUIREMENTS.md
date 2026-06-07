# CallMemo Requirements

## Objective

Build an Android app that detects when a phone call ends, immediately opens a floating Jetpack Compose overlay, and lets the user record a note tied to that caller's phone number. All data must remain local to the device and no network permissions should be added.

## Phase 1: Call Detection

- Implement a `BroadcastReceiver` that listens for `TelephonyManager.ACTION_PHONE_STATE_CHANGED`.
- Also handle `Intent.ACTION_NEW_OUTGOING_CALL` so outgoing numbers can be captured when available.
- Track call-state transitions between `CALL_STATE_RINGING` or `CALL_STATE_OFFHOOK` and `CALL_STATE_IDLE`.
- Trigger `OverlayService` only when the call transitions to `CALL_STATE_IDLE`, which indicates the call has ended.
- Capture the incoming or outgoing phone number associated with the call event.
- For Android 12 and later, launch the overlay through an explicit foreground service from the receiver to comply with broadcast restrictions.
- The foreground service should remain offline-only and use a local notification channel.

## Phase 2: Local Database

Use Room to persist call notes in a lightweight SQLite database.

### Entity: `CallNote`

Required fields:

- `id` - `Int`, primary key, auto-increment
- `phoneNumber` - `String`, indexed
- `timestamp` - `Long`
- `noteText` - `String`

### DAO Operations

Implement the following operations:

- `insertNote()`
- `getNotesForNumber(phoneNumber: String)` - Returns the latest 3 notes for the number.
- `deleteNote()`

### Data Access Rules

- Expose notes as a `Flow<List<CallNote>>` for the overlay history preview.
- Run inserts and deletes on a background dispatcher such as `Dispatchers.IO`.
- Do not block the main thread with database work.

## Project Structure

Keep the implementation organized by responsibility:

- `CallNotesApplication` for database and repository initialization
- `MainActivity` for permissions and entry-point UI
- `CallReceiver` for phone-state broadcasts
- `OverlayService` for the foreground overlay window and Compose host
- `OverlayContent` for the Compose note UI
- `data/` for Room entity, DAO, database, and repository classes
- `ui/theme/` for Material 3 design tokens
- `CallNotesContract` for shared intent extras and notification constants

## Data Flow

The implementation should follow this path:

```text
Phone state change
    -> CallReceiver
    -> OverlayService
    -> OverlayContent
    -> CallNoteRepository
    -> Room database
```

## Phase 3: Overlay UI

Extend the overlay into a compact notepad interface.

### Required UI Behavior

- Display a clear title and show the captured phone number, such as `Number: +1 234-567-890`.
- Include a focusable `TextField` or `OutlinedTextField` that automatically requests keyboard focus when the overlay appears.
- Provide a `Save Note` button that writes the note to Room and calls `stopSelf()` to dismiss the overlay.
- Provide a `Dismiss` button that closes the overlay without saving.

### Optional History Preview

- Show a small scrollable section with the most recent notes saved for the same phone number.

## Technical and Security Requirements

- Perform all Room operations on a background dispatcher such as `Dispatchers.IO`.
- Do not block the main UI thread.
- Keep the app fully offline. Do not add any external network permissions.
- The overlay service hosts Compose directly and sets lifecycle, ViewModel, and saved-state owners manually.
- Configure the floating window with keyboard-friendly layout flags:

```kotlin
flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_DIM_BEHIND
```

- Set a nonzero `dimAmount` (e.g., 0.45f) so the overlay stands out above the dimmed background.
- Configure `softInputMode` so the keyboard can appear and resize the overlay content.

## Current Status

- The app handles `READ_PHONE_STATE`, `POST_NOTIFICATIONS`, `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, and `PROCESS_OUTGOING_CALLS` permissions.
- The history preview shows the latest three notes for the selected phone number.
