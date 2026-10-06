package com.notivault.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** The set of package names the user wants to capture. */
class WatchedAppsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun get(): Set<String> = prefs.getStringSet(KEY, emptySet())?.toSet() ?: emptySet()

    fun set(packages: Set<String>) {
        prefs.edit().putStringSet(KEY, HashSet(packages)).apply()
    }

    fun toggle(packageName: String) {
        val current = get().toMutableSet()
        if (!current.add(packageName)) current.remove(packageName)
        set(current)
    }

    val flow: Flow<Set<String>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY) trySend(get())
        }
        trySend(get())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private companion object {
        const val KEY = "watched_packages"
    }
}
