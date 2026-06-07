package com.example.androidcallnotes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.androidcallnotes.CallNotesContract
import com.example.androidcallnotes.service.OverlayService

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(CallNotesContract.PREFS_NAME, Context.MODE_PRIVATE)

        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> handlePhoneStateChanged(appContext, intent, prefs)
            Intent.ACTION_NEW_OUTGOING_CALL -> handleOutgoingCall(intent, prefs)
        }
    }

    private fun handlePhoneStateChanged(
        context: Context,
        intent: Intent,
        prefs: android.content.SharedPreferences,
    ) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)?.trim().orEmpty()
        val editor = prefs.edit()

        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                if (number.isNotEmpty()) {
                    editor.putString(CallNotesContract.PREF_LAST_NUMBER, number)
                    launchOverlay(context, number)
                }
                editor.putInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_RINGING)
                editor.apply()
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (number.isNotEmpty()) {
                    editor.putString(CallNotesContract.PREF_LAST_NUMBER, number)
                    launchOverlay(context, number)
                } else {
                    // Outgoing calls might not have the number here, but handleOutgoingCall should have caught it
                    val lastNumber = prefs.getString(CallNotesContract.PREF_LAST_NUMBER, null)
                    if (lastNumber != null) {
                        launchOverlay(context, lastNumber)
                    }
                }
                editor.putInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_OFFHOOK)
                editor.apply()
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val previousState = prefs.getInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_IDLE)
                val phoneNumber = number.ifEmpty {
                    prefs.getString(CallNotesContract.PREF_LAST_NUMBER, null).orEmpty()
                }

                editor.putInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_IDLE)
                editor.remove(CallNotesContract.PREF_LAST_NUMBER)
                editor.apply()

                if (previousState == TelephonyManager.CALL_STATE_RINGING ||
                    previousState == TelephonyManager.CALL_STATE_OFFHOOK
                ) {
                    launchOverlay(context, phoneNumber)
                }
            }
        }
    }

    private fun handleOutgoingCall(
        intent: Intent,
        prefs: android.content.SharedPreferences,
    ) {
        val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)?.trim().orEmpty()
        if (number.isNotEmpty()) {
            prefs.edit()
                .putString(CallNotesContract.PREF_LAST_NUMBER, number)
                .putInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_OFFHOOK)
                .apply()
        }
    }

    private fun launchOverlay(context: Context, phoneNumber: String) {
        val serviceIntent = OverlayService.createIntent(
            context,
            phoneNumber.ifBlank { CallNotesContract.UNKNOWN_PHONE_NUMBER },
        )
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}

