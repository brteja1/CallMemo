package com.example.androidcallnotes

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.androidcallnotes.data.CallNote
import com.example.androidcallnotes.ui.theme.CallMemoTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallMemoTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        onOpenOverlaySettings = ::openOverlaySettings,
                        onOpenAppSettings = ::openAppSettings,
                        onOpenNotificationSettings = ::openNotificationSettings,
                    )
                }
            }
        }
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        startActivity(intent)
    }
}

@Composable
private fun MainScreen(
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val context = LocalContext.current
    val repository = (context.applicationContext as CallNotesApplication).repository
    val allNotes by repository.getAllNotes().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var refreshToken by remember { mutableIntStateOf(0) }
    
    // Refresh permission status whenever the activity is resumed
    LifecycleResumeEffect(Unit) {
        refreshToken++
        onPauseOrDispose { }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshToken++
    }
    
    val permissions = remember(refreshToken) { PermissionStatus.from(context) }
    val allPermissionsGranted = permissions.hasPhoneState && permissions.hasCallLog && permissions.hasContacts && permissions.canDrawOverlays && permissions.hasNotificationAccess

    var searchQuery by remember { 
        mutableStateOf(
            (context as? android.app.Activity)?.intent?.getStringExtra(CallNotesContract.EXTRA_PHONE_NUMBER) ?: ""
        )
    }

    val filteredNotes = remember(allNotes, searchQuery) {
        if (searchQuery.isBlank()) {
            allNotes
        } else {
            val query = searchQuery.lowercase()
            allNotes.filter { note ->
                val contactName = ContactUtils.getContactName(context, note.phoneNumber)?.lowercase().orEmpty()
                note.noteText.lowercase().contains(query) ||
                        note.phoneNumber.lowercase().contains(query) ||
                        contactName.contains(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.main_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.main_subtitle),
                style = MaterialTheme.typography.bodyMedium,
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (!allPermissionsGranted) {
                PermissionsCard(
                    permissions = permissions,
                    onGrantPermissions = {
                        val permissionsToRequest = buildList {
                            add(Manifest.permission.READ_PHONE_STATE)
                            add(Manifest.permission.READ_CALL_LOG)
                            add(Manifest.permission.READ_CONTACTS)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    },
                    onOpenOverlaySettings = onOpenOverlaySettings,
                    onOpenAppSettings = onOpenAppSettings,
                    onOpenNotificationSettings = onOpenNotificationSettings
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search name, number or notes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notes List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (filteredNotes.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (searchQuery.isEmpty()) "No notes captured yet." else "No matching notes found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredNotes, key = { it.id }) { note ->
                    NoteItem(
                        note = note,
                        onDelete = {
                            scope.launch {
                                repository.deleteNote(note)
                            }
                        }
                    )
                }
            }
        }
    }

    // Keep tracking permission refreshes
    if (refreshToken == Int.MIN_VALUE) Text("")
}

@Composable
private fun PermissionsCard(
    permissions: PermissionStatus,
    onGrantPermissions: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                PermissionStatusText("Phone State", permissions.hasPhoneState)
                PermissionStatusText("Call Log", permissions.hasCallLog)
                PermissionStatusText("Contacts", permissions.hasContacts)
                PermissionStatusText("Overlay", permissions.canDrawOverlays)
                PermissionStatusText("Notification Access (for WhatsApp)", permissions.hasNotificationAccess)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onGrantPermissions, modifier = Modifier.weight(1f)) {
                        Text("Grant", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(onClick = onOpenOverlaySettings, modifier = Modifier.weight(1f)) {
                        Text("Overlay", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenNotificationSettings, modifier = Modifier.weight(1f)) {
                        Text("Notifications", style = MaterialTheme.typography.labelLarge)
                    }
                    OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.weight(1f)) {
                        Text("App Settings", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusText(label: String, granted: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
        val color = if (granted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
        val text = if (granted) "✓" else "✗"
        Text(text = "$text $label", color = color, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NoteItem(
    note: CallNote,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val contactName = remember(note.phoneNumber) {
        ContactUtils.getContactName(context, note.phoneNumber)
    }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contactName ?: note.phoneNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (contactName != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (contactName != null) {
                        Text(
                            text = note.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = dateFormatter.format(Date(note.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Note",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            
            Text(
                text = note.noteText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private data class PermissionStatus(
    val hasPhoneState: Boolean,
    val hasCallLog: Boolean,
    val hasContacts: Boolean,
    val notificationStatus: Boolean,
    val canDrawOverlays: Boolean,
    val hasNotificationAccess: Boolean,
) {
    companion object {
        fun from(context: android.content.Context): PermissionStatus {
            val hasPhoneState = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasCallLog = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALL_LOG
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val hasContacts = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val notificationStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
            val canDrawOverlays = Settings.canDrawOverlays(context)
            
            val hasNotificationAccess = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)

            return PermissionStatus(
                hasPhoneState = hasPhoneState,
                hasCallLog = hasCallLog,
                hasContacts = hasContacts,
                notificationStatus = notificationStatus,
                canDrawOverlays = canDrawOverlays,
                hasNotificationAccess = hasNotificationAccess,
            )
        }
    }
}
