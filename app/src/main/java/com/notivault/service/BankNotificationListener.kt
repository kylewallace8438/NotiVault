package com.notivault.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.notivault.NotiVaultApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Android binds this service once the user enables Notification access for NotiVault.
 * It then gets a callback for every notification on the phone; we keep only the watched apps.
 */
class BankNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        AppStatus.listenerConnected.value = true
        // Catch up on anything still in the notification shade (posted while we were disconnected).
        val active = try { activeNotifications } catch (_: Throwable) { null } ?: return
        active.forEach { handle(it) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        AppStatus.listenerConnected.value = false
        // Ask Android to bind us again (HyperOS sometimes drops the binding).
        AppStatus.requestRebind(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn?.let { handle(it) }
    }

    override fun onDestroy() {
        AppStatus.listenerConnected.value = false
        scope.cancel()
        super.onDestroy()
    }

    private fun handle(sbn: StatusBarNotification) {
        val app = applicationContext as NotiVaultApp
        val isTest = sbn.packageName == packageName &&
            sbn.notification?.channelId == TestNotification.CHANNEL_ID
        if (!isTest && sbn.packageName !in app.watchedApps.get()) return
        scope.launch {
            runCatching { app.recorder.record(sbn) }
        }
    }
}
