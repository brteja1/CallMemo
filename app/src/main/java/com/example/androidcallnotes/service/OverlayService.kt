package com.example.androidcallnotes.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.androidcallnotes.CallNotesApplication
import com.example.androidcallnotes.CallNotesContract
import com.example.androidcallnotes.MainActivity
import com.example.androidcallnotes.data.CallNote
import kotlinx.coroutines.launch

class OverlayService : LifecycleService() {
    private val windowManager by lazy { getSystemService<WindowManager>() ?: error("WindowManager unavailable") }
    private val repository by lazy { (application as CallNotesApplication).repository }

    private var overlayView: FrameLayout? = null
    private var currentPhoneNumber: String = CallNotesContract.UNKNOWN_PHONE_NUMBER

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        currentPhoneNumber = intent?.getStringExtra(CallNotesContract.EXTRA_PHONE_NUMBER)
            ?.takeIf { it.isNotBlank() }
            ?: CallNotesContract.UNKNOWN_PHONE_NUMBER

        startForeground(CallNotesContract.NOTIFICATION_ID, buildNotification())
        showOverlay()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    private fun showOverlay() {
        if (overlayView != null) {
            return
        }

        if (!Settings.canDrawOverlays(this)) {
            return
        }

        val container = FrameLayout(this)
        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val notesState by repository.getNotesForNumber(currentPhoneNumber)
                    .collectAsState(initial = emptyList())

                OverlayContent(
                    phoneNumber = currentPhoneNumber,
                    recentNotes = notesState,
                    onSave = { noteText ->
                        lifecycleScope.launch {
                            repository.insertNote(
                                CallNote(
                                    phoneNumber = currentPhoneNumber,
                                    timestamp = System.currentTimeMillis(),
                                    noteText = noteText,
                                )
                            )
                            stopSelf()
                        }
                    },
                    onDismiss = {
                        stopSelf()
                    },
                )
            }
        }

        container.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_DIM_BEHIND,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            dimAmount = 0.45f
        }

        windowManager.addView(container, params)
        overlayView = container
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
            overlayView = null
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CallNotesContract.NOTIFICATION_CHANNEL_ID,
            CallNotesContract.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = CallNotesContract.NOTIFICATION_CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlags(),
        )

        return NotificationCompat.Builder(this, CallNotesContract.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(com.example.androidcallnotes.R.drawable.ic_call_note)
            .setContentTitle(getString(com.example.androidcallnotes.R.string.foreground_notification_title))
            .setContentText(getString(com.example.androidcallnotes.R.string.foreground_notification_text))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun pendingIntentFlags(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
    }

    companion object {
        fun createIntent(context: Context, phoneNumber: String): Intent {
            return Intent(context, OverlayService::class.java).apply {
                putExtra(CallNotesContract.EXTRA_PHONE_NUMBER, phoneNumber)
            }
        }
    }
}
