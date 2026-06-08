package com.example.androidcallnotes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.example.androidcallnotes.CallNotesContract
import com.example.androidcallnotes.service.OverlayService

class CallReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "CallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: action=${intent.action}")
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(CallNotesContract.PREFS_NAME, Context.MODE_PRIVATE)

        when (intent.action) {
            TelephonyManager.ACTION_PHONE_STATE_CHANGED -> handlePhoneStateChanged(appContext, intent, prefs)
            Intent.ACTION_NEW_OUTGOING_CALL -> handleOutgoingCall(appContext, intent, prefs)
        }
    }

    private fun handlePhoneStateChanged(
        context: Context,
        intent: Intent,
        prefs: android.content.SharedPreferences,
    ) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)?.trim().orEmpty()
        Log.d(TAG, "handlePhoneStateChanged: state=$state, number=$number")
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
                var phoneNumber = number
                if (phoneNumber.isEmpty()) {
                    phoneNumber = prefs.getString(CallNotesContract.PREF_LAST_NUMBER, null).orEmpty()
                }
                
                if (phoneNumber.isEmpty()) {
                    phoneNumber = getLatestCallNumber(context)
                }
                
                if (phoneNumber.isNotEmpty()) {
                    editor.putString(CallNotesContract.PREF_LAST_NUMBER, phoneNumber)
                    editor.apply()
                }

                Log.d(TAG, "OFFHOOK: final number=$phoneNumber")
                launchOverlay(context, phoneNumber)
                
                editor.putInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_OFFHOOK)
                editor.apply()
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val previousState = prefs.getInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_IDLE)
                var phoneNumber = number.ifEmpty {
                    prefs.getString(CallNotesContract.PREF_LAST_NUMBER, null).orEmpty()
                }
                
                if (phoneNumber.isEmpty()) {
                    phoneNumber = getLatestCallNumber(context)
                }

                Log.d(TAG, "IDLE: previousState=$previousState, phoneNumber=$phoneNumber")

                editor.putInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_IDLE)
                editor.remove(CallNotesContract.PREF_LAST_NUMBER)
                editor.apply()

                if (previousState == TelephonyManager.CALL_STATE_RINGING ||
                    previousState == TelephonyManager.CALL_STATE_OFFHOOK
                ) {
                    val serviceIntent = OverlayService.createIntent(context, phoneNumber).apply {
                        putExtra(CallNotesContract.EXTRA_CALL_ENDED, true)
                    }
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            }
        }
    }

    private fun handleOutgoingCall(
        context: Context,
        intent: Intent,
        prefs: android.content.SharedPreferences,
    ) {
        val number = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)?.trim().orEmpty()
        Log.d(TAG, "handleOutgoingCall: number=$number")
        if (number.isNotEmpty()) {
            prefs.edit()
                .putString(CallNotesContract.PREF_LAST_NUMBER, number)
                .putInt(CallNotesContract.PREF_LAST_STATE, TelephonyManager.CALL_STATE_OFFHOOK)
                .apply()
            launchOverlay(context, number)
        }
    }

    private fun launchOverlay(context: Context, phoneNumber: String) {
        Log.d(TAG, "launchOverlay: phoneNumber=$phoneNumber")
        val serviceIntent = OverlayService.createIntent(
            context,
            phoneNumber.ifBlank { CallNotesContract.UNKNOWN_PHONE_NUMBER },
        )
        ContextCompat.startForegroundService(context, serviceIntent)
    }

    private fun getLatestCallNumber(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "getLatestCallNumber: READ_CALL_LOG permission not granted")
            return ""
        }

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(CallLog.Calls.NUMBER),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 1"
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    return it.getString(0).orEmpty()
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing READ_CALL_LOG permission", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying call log", e)
        }
        return ""
    }
}

