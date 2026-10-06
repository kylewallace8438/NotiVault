package com.notivault.service

import android.content.ComponentName
import android.content.Context
import android.os.PowerManager
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow

/** Permission / health state shown in the UI. Refreshed every time the app comes to the foreground. */
object AppStatus {

    data class State(
        val listenerGranted: Boolean = false,
        val batteryUnrestricted: Boolean = false,
        val canPostNotifications: Boolean = true,
    )

    val state = MutableStateFlow(State())

    /** True while Android has our listener bound (i.e. it is actually receiving notifications right now). */
    val listenerConnected = MutableStateFlow(false)

    fun listenerComponent(context: Context) = ComponentName(context, BankNotificationListener::class.java)

    fun refresh(context: Context) {
        val power = context.getSystemService(PowerManager::class.java)
        state.value = State(
            listenerGranted = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName),
            batteryUnrestricted = power?.isIgnoringBatteryOptimizations(context.packageName) == true,
            canPostNotifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
        )
    }

    fun requestRebind(context: Context) {
        runCatching { NotificationListenerService.requestRebind(listenerComponent(context)) }
    }
}
