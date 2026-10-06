package com.notivault.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.notivault.R

/** Posts a fake bank notification so you can check the whole pipeline (listener -> DB -> parser -> UI). */
object TestNotification {

    const val CHANNEL_ID = "notivault_test"

    private val samples = listOf(
        "+1,250,000 VND from NGUYEN VAN A. Balance: 8,430,000 VND",
        "-89,000 VND paid to GRAB FOOD. Balance: 8,341,000 VND",
        "TK 1234xxx789|GD: +2,500,000VND|SD: 12,345,678VND|ND: TRAN THI B chuyen tien",
    )

    fun send(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Test notifications", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val text = samples.random()
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Test bank")
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .build()
        nm.notify((System.currentTimeMillis() % 1_000_000).toInt(), notification)
    }
}
