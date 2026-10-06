package com.notivault.service

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Opens the system settings pages the user needs. Each tries the most specific page first and falls back. */
object SystemScreens {

    fun openListenerSettings(context: Context) {
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                AppStatus.listenerComponent(context).flattenToString(),
            )
        } else null
        context.startFirst(detail, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS), appDetails(context))
    }

    /** App info page: on Xiaomi this is where Autostart, Battery saver and "Allow restricted settings" live. */
    fun openAppDetails(context: Context) {
        context.startFirst(appDetails(context))
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations(context: Context) {
        context.startFirst(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")),
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
            appDetails(context),
        )
    }

    private fun appDetails(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    private fun Context.startFirst(vararg intents: Intent?) {
        for (intent in intents) {
            if (intent == null) continue
            try {
                startActivity(intent)
                return
            } catch (_: ActivityNotFoundException) {
            } catch (_: SecurityException) {
            }
        }
    }
}
