@file:OptIn(ExperimentalMaterial3Api::class)

package com.notivault.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notivault.service.AppStatus
import com.notivault.service.SystemScreens

@Composable
fun HomeScreen(
    vm: MainViewModel,
    onOpenDetail: (Long) -> Unit,
    onOpenApps: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val status by AppStatus.state.collectAsStateWithLifecycle()
    val watched by vm.watched.collectAsStateWithLifecycle()
    val rows by vm.notifications.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()
    val selected by vm.selectedPackage.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val total by vm.totalCount.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NotiVault") },
                actions = {
                    TextButton(onClick = onOpenApps) { Text("Apps (${watched.size})") }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!status.listenerGranted) {
                item(key = "setup-access") {
                    SetupCard(
                        title = "Notification access is off",
                        body = "NotiVault can't read anything until you switch it on. " +
                            "If the switch is greyed out, open App info, tap ⋮ and choose " +
                            "\"Allow restricted settings\", then try again.",
                        primaryLabel = "Grant access",
                        onPrimary = { SystemScreens.openListenerSettings(context) },
                        secondaryLabel = "App info",
                        onSecondary = { SystemScreens.openAppDetails(context) },
                    )
                }
            }
            if (watched.isEmpty()) {
                item(key = "setup-apps") {
                    SetupCard(
                        title = "No apps selected",
                        body = "Pick the banking apps whose notifications you want to keep.",
                        primaryLabel = "Choose apps",
                        onPrimary = onOpenApps,
                    )
                }
            }

            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search text") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.query.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
            }

            if (sources.isNotEmpty()) {
                item(key = "chips") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item(key = "all") {
                            FilterChip(
                                selected = selected == null,
                                onClick = { vm.selectedPackage.value = null },
                                label = { Text("All ($total)") },
                            )
                        }
                        items(sources, key = { it.packageName }) { s ->
                            FilterChip(
                                selected = selected == s.packageName,
                                onClick = {
                                    vm.selectedPackage.value =
                                        if (selected == s.packageName) null else s.packageName
                                },
                                label = { Text("${s.appLabel} (${s.count})") },
                            )
                        }
                    }
                }
            }

            val list = rows
            when {
                list == null -> item(key = "loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                }
                list.isEmpty() -> item(key = "empty") { EmptyState(hasAny = total > 0) }
                else -> items(list, key = { it.id }) { n ->
                    NotificationCard(n, onClick = { onOpenDetail(n.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(hasAny: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (hasAny) "Nothing matches" else "Nothing captured yet",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (hasAny) "Try a different search or filter."
            else "New notifications from your selected apps will appear here. " +
                "To check the setup, use Settings > Send test notification.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
