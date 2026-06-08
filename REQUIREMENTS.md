# CallMemo Requirements

## Core Features
- **Phone State Monitoring**: Detect incoming and outgoing calls to trigger the overlay UI.
- **Overlay UI**: Display a Compose-based floating overlay during or immediately after a call to allow quick note-taking.
- **Contact Integration**: Automatically retrieve contact names from the system contacts based on the phone number.
- **Note Management**: Store and retrieve call notes using a local Room database.
- **Search**: Search through saved notes by contact name, phone number, or note content.

## User Interface Requirements
- **Overlay Bubble**: A compact bubble UI that appears during calls, which can be expanded to a full note-taking interface.
- **Auto-Dismiss**: The overlay should automatically dismiss after a period of inactivity once a call ends, unless the user is actively editing.
- **Main Activity Listing**:
    - Display a list of all saved notes.
    - For known contacts: Show the contact name as the title with the phone number in a smaller font below it.
    - For unknown contacts: Show the phone number as the main title.
    - Each note item shows the timestamp of the call.
- **Navigation & Filtering**:
    - Clicking "Show all notes" in the overlay should navigate to the Main Activity.
    - The Main Activity should automatically filter the notes list for that specific contact using their phone number as the initial search query.
    - The overlay bubble must remain visible during an active call, even after navigating to the Main Activity.

## Technical & Security Requirements
- **Offline First**: The app must remain entirely offline with no network permissions.
- **Permissions**:
    - `READ_PHONE_STATE`: To detect call state changes.
    - `READ_CALL_LOG`: To retrieve the phone number for outgoing calls where not immediately available.
    - `READ_CONTACTS`: To display contact names.
    - `SYSTEM_ALERT_WINDOW`: To display the overlay over other apps.
    - `POST_NOTIFICATIONS`: For the required foreground service notification (Android 13+).
- **Foreground Service**: Use a Foreground Service to ensure the overlay remains active and reliable during the call lifecycle.
- **Architecture**: Follow modern Android practices using Jetpack Compose, Room, and Coroutines.
- **Theming**:
    - Support both Light and Dark modes based on system settings.
    - Implement Dynamic Color (Material You) on Android 12+ to align with the phone's wallpaper and system theme.
    - Ensure consistency between the main application UI and the floating overlay.
