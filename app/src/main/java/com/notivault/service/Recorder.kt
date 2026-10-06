package com.notivault.service

import android.content.Context
import android.service.notification.StatusBarNotification
import com.notivault.data.NotificationDao
import com.notivault.parsing.ParserRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Maps -> parses -> saves. */
class Recorder(private val context: Context, private val dao: NotificationDao) {

    /** Returns true if a new row was stored. */
    suspend fun record(sbn: StatusBarNotification): Boolean {
        val entity = NotificationMapper.map(context, sbn) ?: return false
        return dao.insert(ParserRegistry.apply(entity)) > 0
    }

    /** Runs the current parsers over every stored row. Returns how many rows matched a parser. */
    suspend fun reparseAll(): Int = withContext(Dispatchers.IO) {
        val updated = dao.getAll().map { ParserRegistry.apply(it) }
        dao.updateAll(updated)
        updated.count { it.parserId != null }
    }
}
