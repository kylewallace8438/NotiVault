package com.notivault.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notivault.NotiVaultApp
import com.notivault.data.ExportFormat
import com.notivault.data.Exporter
import com.notivault.data.NotificationEntity
import com.notivault.data.SourceCount
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NotiVaultApp
    private val dao = app.database.notificationDao()

    /** null = all apps */
    val selectedPackage = MutableStateFlow<String?>(null)
    val query = MutableStateFlow("")

    /** null while loading */
    val notifications: StateFlow<List<NotificationEntity>?> =
        combine(selectedPackage, query.debounce(200)) { pkg, q -> pkg to q.trim().ifEmpty { null } }
            .flatMapLatest { (pkg, q) -> dao.observe(pkg, q, 1000) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val sources: StateFlow<List<SourceCount>> =
        dao.observeSources().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val totalCount: StateFlow<Int> =
        dao.observeCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val watched: StateFlow<Set<String>> =
        app.watchedApps.flow.stateIn(viewModelScope, SharingStarted.Eagerly, app.watchedApps.get())

    fun observe(id: Long): Flow<NotificationEntity?> = dao.observeById(id)

    fun delete(id: Long) {
        viewModelScope.launch { dao.deleteById(id) }
    }

    fun clearAll(onDone: (String) -> Unit) {
        viewModelScope.launch {
            dao.deleteAll()
            selectedPackage.value = null
            onDone("All notifications deleted")
        }
    }

    fun reparseAll(onDone: (String) -> Unit) {
        viewModelScope.launch {
            val matched = runCatching { app.recorder.reparseAll() }
            onDone(matched.fold({ "Parsers re-run: $it rows matched" }, { "Failed: ${it.message}" }))
        }
    }

    fun export(uri: Uri, format: ExportFormat, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { Exporter.export(app, uri, format) }
            onDone(result.fold({ "Exported $it notifications" }, { "Export failed: ${it.message}" }))
        }
    }
}
