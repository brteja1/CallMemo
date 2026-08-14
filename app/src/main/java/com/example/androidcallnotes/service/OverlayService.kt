package com.example.androidcallnotes.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.androidcallnotes.CallNotesApplication
import com.example.androidcallnotes.CallNotesContract
import com.example.androidcallnotes.ContactUtils
import com.example.androidcallnotes.MainActivity
import com.example.androidcallnotes.data.CallNote
import com.example.androidcallnotes.ui.theme.CallMemoTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class OverlayService : LifecycleService(), SavedStateRegistryOwner {
    companion object {
        private const val TAG = "OverlayService"

        fun createIntent(context: Context, phoneNumber: String): Intent {
            return Intent(context, OverlayService::class.java).apply {
                putExtra(CallNotesContract.EXTRA_PHONE_NUMBER, phoneNumber)
            }
        }
    }

    private val windowManager by lazy { getSystemService<WindowManager>() ?: error("WindowManager unavailable") }
    private val repository by lazy { (application as CallNotesApplication).repository }

    private var overlayView: FrameLayout? = null
    private var currentPhoneNumber by mutableStateOf(CallNotesContract.UNKNOWN_PHONE_NUMBER)
    private var isExpanded by mutableStateOf(value = false)
    private var isMinimized by mutableStateOf(value = false)
    private var isCallActive by mutableStateOf(value = true)
    private var sidebarOffsetY by mutableStateOf(-100f)
    private var autoDismissJob: Job? = null

    private val viewModelStore = ViewModelStore()
    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore: ViewModelStore = this@OverlayService.viewModelStore
    }

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val newPhoneNumber = intent?.getStringExtra(CallNotesContract.EXTRA_PHONE_NUMBER)
            ?.takeIf { it.isNotBlank() }
            ?: CallNotesContract.UNKNOWN_PHONE_NUMBER

        val callEnded = intent?.getBooleanExtra(CallNotesContract.EXTRA_CALL_ENDED, false) ?: false

        Log.d(TAG, "onStartCommand: phoneNumber=$newPhoneNumber, callEnded=$callEnded")

        val isUnknown = newPhoneNumber == CallNotesContract.UNKNOWN_PHONE_NUMBER
        val currentIsUnknown = currentPhoneNumber == CallNotesContract.UNKNOWN_PHONE_NUMBER

        if (newPhoneNumber != currentPhoneNumber) {
            // Only update if the new number is not unknown, or if we currently have an unknown number
            if (!isUnknown || currentIsUnknown) {
                currentPhoneNumber = newPhoneNumber
                isExpanded = false
                autoDismissJob?.cancel()
                updateWindowParams()
            }
        }

        isCallActive = !callEnded

        if (callEnded && !isExpanded) {
            startAutoDismissTimer()
        }

        startForeground(CallNotesContract.NOTIFICATION_ID, buildNotification())
        showOverlay()
        return START_NOT_STICKY
    }

    private fun startAutoDismissTimer() {
        Log.d(TAG, "startAutoDismissTimer: starting 5s delay")
        autoDismissJob?.cancel()
        autoDismissJob = lifecycleScope.launch {
            delay(5000)
            Log.d(TAG, "autoDismissTimer triggered: isExpanded=$isExpanded")
            if (!isExpanded) {
                stopSelf()
            }
        }
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
        container.setViewTreeLifecycleOwner(this)
        container.setViewTreeViewModelStoreOwner(viewModelStoreOwner)
        container.setViewTreeSavedStateRegistryOwner(this)

        val composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                CallMemoTheme {
                    val phoneNumber = currentPhoneNumber
                    val notesState by remember(phoneNumber) {
                        repository.getNotesForNumber(phoneNumber)
                    }.collectAsState(initial = emptyList())

                    val contactName = remember(phoneNumber) {
                        ContactUtils.getContactName(context, phoneNumber)
                    }

                    if (isExpanded) {
                        OverlayContent(
                            phoneNumber = currentPhoneNumber,
                            contactName = contactName,
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
                                    if (!isCallActive) {
                                        stopSelf()
                                    } else {
                                        isExpanded = false
                                        updateWindowParams()
                                    }
                                }
                            },
                            onDismiss = {
                                if (!isCallActive) {
                                    stopSelf()
                                } else {
                                    isExpanded = false
                                    updateWindowParams()
                                }
                            },
                            onShowAllNotes = {
                                val intent =
                                    Intent(this@OverlayService, MainActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        putExtra(
                                            CallNotesContract.EXTRA_PHONE_NUMBER,
                                            currentPhoneNumber
                                        )
                                    }
                                startActivity(intent)

                                if (!isCallActive) {
                                    stopSelf()
                                } else {
                                    isExpanded = false
                                    updateWindowParams()
                                }
                            }
                        )
                    } else {
                        SidebarContent(
                            isMinimized = isMinimized,
                            onClick = {
                                if (isMinimized) {
                                    isMinimized = false
                                } else {
                                    autoDismissJob?.cancel()
                                    isExpanded = true
                                }
                                updateWindowParams()
                            },
                            onMinimize = {
                                isMinimized = true
                                updateWindowParams()
                            },
                            onMaximize = {
                                isMinimized = false
                                updateWindowParams()
                            },
                            onVerticalDrag = { deltaY ->
                                sidebarOffsetY += deltaY
                                updateWindowParams()
                            }
                        )
                    }
                }
            }
        }

        container.addView(
            composeView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        )

        val params = getWindowParams()

        windowManager.addView(container, params)
        overlayView = container
    }

    private fun updateWindowParams() {
        overlayView?.let { view ->
            windowManager.updateViewLayout(view, getWindowParams())
        }
    }

    private fun getWindowParams(): WindowManager.LayoutParams {
        val width = if (isExpanded) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT
        val height = if (isExpanded) WindowManager.LayoutParams.MATCH_PARENT else WindowManager.LayoutParams.WRAP_CONTENT
        
        var flags = if (isExpanded) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_DIM_BEHIND
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        
        // Allow the window to be shown over the lock screen
        @Suppress("DEPRECATION")
        flags = flags or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            flags,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = if (isExpanded) Gravity.CENTER else Gravity.CENTER_VERTICAL or Gravity.END
            if (!isExpanded) {
                x = 0 // Anchored flush to the right edge
                y = sidebarOffsetY.toInt()
            }
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
            dimAmount = if (isExpanded) 0.45f else 0f
        }
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
            overlayView = null
        }
    }

    private fun ensureNotificationChannel() {
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
        return PendingIntent.FLAG_IMMUTABLE
    }

}
