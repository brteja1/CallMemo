# CallMemo Requirements

## Objective

Build an Android app that detects when a phone call starts (incoming or outgoing), displays a floating bubble on the screen, and allows the user to expand it into a Jetpack Compose overlay to record notes tied to the caller's phone number and contact name. All data remains local to the device.

## Phase 1: Call Detection & Bubble Display

- Implement a `BroadcastReceiver` (`CallReceiver`) that listens for `TelephonyManager.ACTION_PHONE_STATE_CHANGED` and `Intent.ACTION_NEW_OUTGOING_CALL`.
- **Trigger**: Launch the `OverlayService` immediately when a call is initiated (`OFFHOOK`) or received (`RINGING`).
- **Bubble UI**:
    - Display a small floating bubble during the call.
    - **Positioning**: The bubble must be centered vertically on the right edge of the screen, with a small buffer margin (e.g., 16px).
    - **Interaction**: Tapping the bubble expands it into the full Note Overlay.
- **Service Type**: Use a foreground service with `specialUse` type (for Android 14+ compatibility) and a local notification.

## Phase 2: Local Database (Room)

Persist call notes locally using Room.

### Entity: `CallNote`
- `id`: `Int` (Primary Key, Auto-increment)
- `phoneNumber`: `String` (Indexed)
- `timestamp`: `Long`
- `noteText`: `String`

### DAO Operations
- `insertNote()`: Save a new note.
- `getNotesForNumber(phoneNumber: String)`: Retrieve notes for a specific number, ordered by most recent.
- `deleteNote()`: Remove a note.

### Technical Implementation
- Use **KSP** (Kotlin Symbol Processing) for Room annotation processing.
- Expose data as `Flow<List<CallNote>>`.
- Ensure all database operations run on `Dispatchers.IO`.

## Phase 3: Note Overlay UI

The expanded overlay provides the interface for note capture and history review.

### Features
- **Contact Integration**: Resolve and display the **Contact Name** from the phone number using `ContactsContract`.
- **Identity**: Show the **Phone Number** clearly.
- **Note Capture**: 
    - A multi-line `OutlinedTextField` for typing the note.
    - Requests focus automatically when expanded.
- **Recent History**: 
    - Display a section showing the **most recent note** captured for this contact/number.
- **Navigation**:
    - Provide a "Show all notes" link/button that opens the main application to view the full history for that contact.
- **Actions**:
    - `Save Note`: Save the text and collapse the overlay back to a bubble or dismiss.
    - `Dismiss`: Collapse the overlay or close the service.

## Project Structure

- `receiver/CallReceiver`: Handles telephony broadcasts.
- `service/OverlayService`: Manages the floating window, View Tree Owners, and state (Bubble vs. Expanded).
- `service/BubbleContent`: Compose UI for the floating bubble.
- `service/OverlayContent`: Compose UI for the note-taking card.
- `data/`: Room entities, DAO, and Repository.
- `ContactUtils`: Helper for resolving contact names.
- `MainActivity`: Permission management and full notes history view.

## Security & Permissions

- **Offline-only**: No network permissions allowed.
- **Required Permissions**:
    - `READ_PHONE_STATE`: Detect call state.
    - `READ_CONTACTS`: Resolve phone numbers to names.
    - `SYSTEM_ALERT_WINDOW`: Draw the floating bubble and overlay.
    - `POST_NOTIFICATIONS`: Show the mandatory foreground service notification.
    - `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`: Run the overlay in the background.

## Technical Requirements
- Use **Jetpack Compose** for all UI components.
- Manually attach `LifecycleOwner`, `ViewModelStoreOwner`, and `SavedStateRegistryOwner` to the `WindowManager` view to support Compose in a Service.
- Use `WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE` for the bubble to allow background interaction, and `FLAG_NOT_TOUCH_MODAL` with `FLAG_DIM_BEHIND` for the expanded overlay.
