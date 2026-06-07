package com.example.androidcallnotes

object CallNotesContract {
    const val EXTRA_PHONE_NUMBER = "com.example.androidcallnotes.extra.PHONE_NUMBER"
    const val EXTRA_CALL_ENDED = "com.example.androidcallnotes.extra.CALL_ENDED"

    const val PREFS_NAME = "call_notes_prefs"
    const val PREF_LAST_STATE = "pref_last_state"
    const val PREF_LAST_NUMBER = "pref_last_number"

    const val UNKNOWN_PHONE_NUMBER = "Unknown number"

    const val NOTIFICATION_CHANNEL_ID = "callmemo_overlay_channel"
    const val NOTIFICATION_CHANNEL_NAME = "CallMemo overlay"
    const val NOTIFICATION_CHANNEL_DESCRIPTION = "Shows the floating note overlay service status."
    const val NOTIFICATION_ID = 1001
}

