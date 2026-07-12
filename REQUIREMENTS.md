# CallMemo Requirements

## Core Features
- **Phone State Monitoring**: 
    - Detect incoming and outgoing cellular calls to trigger the overlay UI.
    - **VoIP Support**: Detect WhatsApp and WhatsApp Business calls using a `NotificationListenerService`.
    - Robust number/contact detection: Use intent extras, shared preferences, call log fallbacks, and notification extras to ensure the correct identifier is displayed.
- **Overlay UI**: 
    - Display a Compose-based floating overlay during or immediately after a call to allow quick note-taking.
    - Dismiss button on the left and Save button on the right for standard action placement.
- **Contact Integration**: Automatically retrieve contact names from the system contacts based on the phone number.
- **Note Management**: Store and retrieve call notes using a local Room database.
- **Search**: Search through saved notes by contact name, phone number, or note content.

## User Interface Requirements
- **Overlay Sidebar**: 
    - A glossy vertical sidebar handle (64dp x 70dp) anchored flush to the right edge.
    - Includes the text "CallMemo" in a small, bold, and italicized font below the icon.
    - Has a slight transparency (85% opacity) to be less intrusive.
    - **Minimize Gesture**: Users can swipe right on the sidebar to minimize it to a tiny 12dp wide handle of the same height. 
    - **Maximize Gesture**: Users can swipe left (pull from the edge) on the minimized handle to restore the sidebar to its full width. Clicking the handle also restores it.
    - **Lock Screen Support**: Displays over the system lock screen to allow note access during active calls even when the device is locked.
    - Can be expanded to a full note-taking interface.
- **Auto-Dismiss**: The overlay should automatically dismiss after a 5-second period of inactivity once a call ends, unless the user is actively editing.
- **Main Activity Listing**:
    - Display a list of all saved notes.
    - For known contacts: Show the contact name as the title with the phone number in a smaller font below it.
    - For unknown contacts: Show the phone number as the main title.
    - Each note item shows the timestamp of the call and a delete option.
- **Navigation & Filtering**:
    - Clicking "Show all notes" in the overlay navigates to the Main Activity.
    - The Main Activity automatically filters the notes list for that specific contact using their phone number as the initial search query.
    - The overlay bubble must remain visible during an active call, even after navigating to the Main Activity.

## Branding & Iconography
- **App Icon**: 
    - Design: A white notes symbol with black lines and a black phone symbol on top.
    - Placement: Centered and scaled proportionately to fit completely within the Android circular launcher mask.
    - Background: Teal theme color.

## Technical & Security Requirements
- **Offline First**: The app must remain entirely offline with no network permissions.
- **Permissions**:
    - `READ_PHONE_STATE`: To detect call state changes.
    - `READ_CALL_LOG`: To retrieve the phone number for outgoing calls and identify contacts reliably.
    - `READ_CONTACTS`: To display contact names.
    - `BIND_NOTIFICATION_LISTENER_SERVICE`: To detect VoIP calls (WhatsApp) from system notifications.
    - `SYSTEM_ALERT_WINDOW`: To display the overlay over other apps.
    - `POST_NOTIFICATIONS`: For the required foreground service notification (Android 13+).
- **Foreground Service**: Use a Foreground Service to ensure the overlay remains active and reliable during the call lifecycle.
- **Architecture**: Follow modern Android practices using Jetpack Compose, Room, and Coroutines.
- **Theming**:
    - Support both Light and Dark modes based on system settings.
    - Implement Dynamic Color (Material You) on Android 12+ to align with the phone's wallpaper and system theme.
    - Ensure consistency between the main application UI and the floating overlay.
