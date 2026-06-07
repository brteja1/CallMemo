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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.androidcallnotes.ui.theme.CallMemoTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CallMemoTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(
                        onOpenOverlaySettings = ::openOverlaySettings,
                        onOpenAppSettings = ::openAppSettings,
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
}

@Composable
private fun MainScreen(
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val context = LocalContext.current
    var refreshToken by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        refreshToken++
    }
    val permissions = PermissionStatus.from(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.main_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.main_subtitle),
            style = MaterialTheme.typography.bodyLarge,
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(R.string.permission_overview),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                PermissionRow(
                    title = stringResource(R.string.phone_permission_title),
                    description = stringResource(R.string.phone_permission_desc),
                    status = if (permissions.hasPhoneState) stringResource(R.string.permission_ready) else stringResource(R.string.permission_missing),
                )
                PermissionRow(
                    title = "Contacts",
                    description = "Needed to show contact names in the overlay.",
                    status = if (permissions.hasContacts) stringResource(R.string.permission_ready) else stringResource(R.string.permission_missing),
                )
                PermissionRow(
                    title = stringResource(R.string.notification_permission_title),
                    description = stringResource(R.string.notification_permission_desc),
                    status = if (permissions.notificationStatus) stringResource(R.string.permission_ready) else stringResource(R.string.permission_missing),
                )
                PermissionRow(
                    title = stringResource(R.string.overlay_permission_title),
                    description = stringResource(R.string.overlay_permission_desc),
                    status = if (permissions.canDrawOverlays) stringResource(R.string.permission_ready) else stringResource(R.string.permission_missing),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val permissionsToRequest = buildList {
                    add(Manifest.permission.READ_PHONE_STATE)
                    add(Manifest.permission.READ_CONTACTS)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }) {
                Text(stringResource(R.string.grant_permissions))
            }
            OutlinedButton(onClick = onOpenOverlaySettings) {
                Text(stringResource(R.string.open_overlay_settings))
            }
        }

        OutlinedButton(onClick = onOpenAppSettings) {
            Text(stringResource(R.string.open_app_settings))
        }

        Text(
            text = "Tip: after granting permissions, make a call and wait for it to end. The overlay will open automatically.",
            style = MaterialTheme.typography.bodyMedium,
        )
        // Read the state so Compose keeps tracking permission refreshes.
        if (refreshToken == Int.MIN_VALUE) {
            Text("")
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    status: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Text(text = description, style = MaterialTheme.typography.bodySmall)
        Text(text = status, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

private data class PermissionStatus(
    val hasPhoneState: Boolean,
    val hasContacts: Boolean,
    val notificationStatus: Boolean,
    val canDrawOverlays: Boolean,
) {
    companion object {
        fun from(context: android.content.Context): PermissionStatus {
            val hasPhoneState = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
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
            return PermissionStatus(
                hasPhoneState = hasPhoneState,
                hasContacts = hasContacts,
                notificationStatus = notificationStatus,
                canDrawOverlays = canDrawOverlays,
            )
        }
    }
}
