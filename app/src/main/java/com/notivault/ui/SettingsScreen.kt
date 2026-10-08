@file:OptIn(ExperimentalMaterial3Api::class)

package com.notivault.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notivault.data.AppDatabase
import com.notivault.data.ExportFormat
import com.notivault.service.AppStatus
import com.notivault.service.SystemScreens
import com.notivault.service.TestNotification
import com.notivault.ui.theme.MoneyColors
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.launch
import java.time.LocalDate

import androidx.compose.material.icons.filled.Menu

@Composable
fun SettingsScreen(
    vm: MainViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val status by AppStatus.state.collectAsStateWithLifecycle()
    val connected by AppStatus.listenerConnected.collectAsStateWithLifecycle()
    val total by vm.totalCount.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val show: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    var confirmClear by remember { mutableStateOf(false) }
    var lastRequestedTestWasPositive by remember { mutableStateOf(true) }

    fun sendTest(positive: Boolean) {
        if (!AppStatus.state.value.listenerGranted) {
            show("Turn on notification access first")
            return
        }
        TestNotification.send(context, positive)
        show("Test sent. Go back to see it in the list.")
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        AppStatus.refresh(context)
        if (granted) sendTest(lastRequestedTestWasPositive) else show("Notification permission was denied")
    }

    val exportJson = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.JSON.mime)
    ) { uri -> if (uri != null) vm.export(uri, ExportFormat.JSON, show) }
    val exportCsv = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.CSV.mime)
    ) { uri -> if (uri != null) vm.export(uri, ExportFormat.CSV, show) }
    val exportDb = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(ExportFormat.DATABASE.mime)
    ) { uri -> if (uri != null) vm.export(uri, ExportFormat.DATABASE, show) }
    fun fileName(f: ExportFormat) = "notivault-${LocalDate.now()}.${f.extension}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Group("Status") {
                StatusRow(
                    label = "Notification access",
                    ok = status.listenerGranted,
                    detail = if (status.listenerGranted) "Granted" else "Off. Nothing can be captured.",
                    action = "Open",
                    onAction = { SystemScreens.openListenerSettings(context) },
                )
                StatusRow(
                    label = "Listener running",
                    ok = connected,
                    detail = if (connected) "Receiving notifications now"
                    else "Not connected. Check access, Autostart and Battery saver.",
                )
                StatusRow(
                    label = "Background activity",
                    ok = status.batteryUnrestricted,
                    detail = if (status.batteryUnrestricted) "Not restricted by battery optimization"
                    else "May be killed in the background",
                    action = if (status.batteryUnrestricted) null else "Allow",
                    onAction = { SystemScreens.requestIgnoreBatteryOptimizations(context) },
                )
            }

            Group("Xiaomi / HyperOS checklist") {
                Text(
                    "HyperOS kills background apps aggressively. Do these once, in App info:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Bullet("Turn on Autostart.")
                Bullet("Battery saver: choose \"No restrictions\".")
                Bullet("Open the Recents screen, long-press NotiVault and tap the lock, so clearing Recents won't stop it.")
                Bullet("If the notification access switch is greyed out: tap ⋮ and choose \"Allow restricted settings\".")
                OutlinedButton(onClick = { SystemScreens.openAppDetails(context) }) { Text("Open App info") }
            }

            Group("Test") {
                Text(
                    "Posts a fake bank notification. If it shows up in the list with a green or red amount, " +
                        "the listener, database and parser all work.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilledTonalButton(onClick = {
                        lastRequestedTestWasPositive = true
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.canPostNotifications) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            sendTest(positive = true)
                        }
                    }, modifier = Modifier.weight(1f)) { Text("+ Positive") }
                    
                    FilledTonalButton(onClick = {
                        lastRequestedTestWasPositive = false
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.canPostNotifications) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            sendTest(positive = false)
                        }
                    }, modifier = Modifier.weight(1f)) { Text("- Negative") }
                }
            }

            Group("Data") {
                Text(
                    "$total notifications stored privately on this phone in " +
                        "databases/${AppDatabase.NAME}. Exports go wherever you choose (e.g. Downloads).",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exportJson.launch(fileName(ExportFormat.JSON)) }) { Text("JSON") }
                    OutlinedButton(onClick = { exportCsv.launch(fileName(ExportFormat.CSV)) }) { Text("CSV") }
                    OutlinedButton(onClick = { exportDb.launch(fileName(ExportFormat.DATABASE)) }) { Text("SQLite .db") }
                }
                Text(
                    "Changed a parser? Re-run them over everything you've stored:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                FilledTonalButton(onClick = { vm.reparseAll(show) }) { Text("Re-run parsers") }
                TextButton(onClick = { confirmClear = true }) {
                    Text("Delete all notifications", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Delete everything?") },
            text = { Text("All $total stored notifications will be removed. Export first if you want a copy.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearAll(show)
                }) { Text("Delete all", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content,
            )
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    ok: Boolean,
    detail: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val dark = isSystemInDarkTheme()
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (ok) MoneyColors.income(dark) else MaterialTheme.colorScheme.error)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row {
        Text("•", modifier = Modifier.width(16.dp), style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
