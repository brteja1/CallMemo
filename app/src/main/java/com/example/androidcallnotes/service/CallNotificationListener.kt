package com.example.androidcallnotes.service

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.androidcallnotes.CallNotesContract

class CallNotificationListener : NotificationListenerService() {
    companion object {
        private const val TAG = "CallNotificationListener"
        private val VOIP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in VOIP_PACKAGES) return

        val notification = sbn.notification
        val extras = notification.extras
        
        // Detect if it's a call. WhatsApp uses CATEGORY_CALL for active/incoming calls.
        val isCall = notification.category == Notification.CATEGORY_CALL || 
                     extras.getString(Notification.EXTRA_TITLE)?.contains("Incoming call") == true ||
                     extras.getString(Notification.EXTRA_TITLE)?.contains("Ongoing call") == true

        if (isCall) {
            val contactName = extras.getString(Notification.EXTRA_TITLE)
                ?.replace("Incoming call", "")
                ?.replace("Ongoing call", "")
                ?.trim()
                ?: CallNotesContract.UNKNOWN_PHONE_NUMBER

            Log.d(TAG, "VoIP Call Detected: $contactName from ${sbn.packageName}")
            
            // Save as last number for fallback
            val prefs = getSharedPreferences(CallNotesContract.PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(CallNotesContract.PREF_LAST_NUMBER, contactName).apply()

            val serviceIntent = OverlayService.createIntent(this, contactName)
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName !in VOIP_PACKAGES) return

        val notification = sbn.notification
        val isCall = notification.category == Notification.CATEGORY_CALL

        if (isCall) {
            val contactName = notification.extras.getString(Notification.EXTRA_TITLE)
                ?.replace("Incoming call", "")
                ?.replace("Ongoing call", "")
                ?.trim()
                ?: getSharedPreferences(CallNotesContract.PREFS_NAME, Context.MODE_PRIVATE)
                    .getString(CallNotesContract.PREF_LAST_NUMBER, null)
                ?: return

            Log.d(TAG, "VoIP Call Ended: $contactName")

            val serviceIntent = OverlayService.createIntent(this, contactName).apply {
                putExtra(CallNotesContract.EXTRA_CALL_ENDED, true)
            }
            ContextCompat.startForegroundService(this, serviceIntent)
        }
    }
}
