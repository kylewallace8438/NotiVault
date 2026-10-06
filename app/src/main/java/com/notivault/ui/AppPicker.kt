@file:OptIn(ExperimentalMaterial3Api::class)

package com.notivault.ui

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notivault.NotiVaultApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap?)

class AppPickerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NotiVaultApp

    /** null while loading */
    val apps = MutableStateFlow<List<InstalledApp>?>(null)

    val watched: StateFlow<Set<String>> =
        app.watchedApps.flow.stateIn(viewModelScope, SharingStarted.Eagerly, app.watchedApps.get())

    init {
        viewModelScope.launch(Dispatchers.IO) { apps.value = loadApps() }
    }

    fun toggle(packageName: String) = app.watchedApps.toggle(packageName)

    @Suppress("DEPRECATION")
    private fun loadApps(): List<InstalledApp> {
        val pm = app.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val installed = pm.queryIntentActivities(launcher, 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != app.packageName }
            .map { ri ->
                InstalledApp(
                    packageName = ri.activityInfo.packageName,
                    label = ri.loadLabel(pm).toString(),
                    icon = runCatching { ri.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
                )
            }
        // Keep watched apps visible even if they no longer have a launcher icon (e.g. uninstalled).
        val known = installed.map { it.packageName }.toSet()
        val orphans = app.watchedApps.get().filter { it !in known }.map { InstalledApp(it, it, null) }
        return (installed + orphans).sortedBy { it.label.lowercase() }
    }
}

@Composable
fun AppPickerScreen(onBack: () -> Unit, vm: AppPickerViewModel = viewModel()) {
    val apps by vm.apps.collectAsStateWithLifecycle()
    val watched by vm.watched.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    // Snapshot at open: selected apps go on top, and the list doesn't jump around while you tick boxes.
    val pinned = remember { vm.watched.value }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (watched.isEmpty()) "Apps to watch" else "Apps to watch (${watched.size})")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val all = apps
        if (all == null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val q = query.trim().lowercase()
        val filtered = all.filter {
            q.isEmpty() || it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
        val top = filtered.filter { it.packageName in pinned }
        val rest = filtered.filter { it.packageName !in pinned }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item(key = "search") {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search apps") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                )
            }
            if (top.isNotEmpty()) {
                item(key = "h-selected") { SectionHeader("Selected") }
                items(top, key = { "s:" + it.packageName }) { a ->
                    AppRow(a, checked = a.packageName in watched) { vm.toggle(a.packageName) }
                }
            }
            item(key = "h-all") { SectionHeader(if (top.isEmpty()) "All apps" else "Other apps") }
            items(rest, key = { "a:" + it.packageName }) { a ->
                AppRow(a, checked = a.packageName in watched) { vm.toggle(a.packageName) }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, onToggle: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        headlineContent = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            val icon = app.icon
            if (icon != null) {
                Image(icon, contentDescription = null, modifier = Modifier.size(40.dp))
            } else {
                Box(Modifier.size(40.dp))
            }
        },
        trailingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
    )
}
