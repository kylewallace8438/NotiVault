package com.notivault

import android.app.Application
import com.notivault.data.AppDatabase
import com.notivault.data.WatchedAppsStore
import com.notivault.service.Recorder

class NotiVaultApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.build(this) }
    val watchedApps: WatchedAppsStore by lazy { WatchedAppsStore(this) }
    val recorder: Recorder by lazy { Recorder(this, database.notificationDao()) }
}
