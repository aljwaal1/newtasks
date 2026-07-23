package com.aljwaal.newtasks

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLog {
    private const val TAG = "SmartTasksAlarm"
    private const val MAX_LOG_BYTES = 512 * 1024L
    private val lock = Any()

    fun write(context: Context, event: String, details: String = "") {
        val safeDetails = details.replace('\n', ' ').replace('\r', ' ').trim()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = buildString {
            append('[').append(timestamp).append("] ")
            append(event)
            if (safeDetails.isNotEmpty()) append(" | ").append(safeDetails)
        }
        Log.i(TAG, line)

        synchronized(lock) {
            runCatching {
                val file = logFile(context)
                if (file.exists() && file.length() > MAX_LOG_BYTES) {
                    val backup = File(file.parentFile, "smart_tasks.previous.log")
                    if (backup.exists()) backup.delete()
                    file.renameTo(backup)
                }
                file.appendText(line + System.lineSeparator(), Charsets.UTF_8)
            }.onFailure {
                Log.e(TAG, "LOG_WRITE_FAILED", it)
            }
        }
    }

    fun readTail(context: Context, maxChars: Int = 24_000): String = synchronized(lock) {
        runCatching {
            val file = logFile(context)
            if (!file.exists()) return@synchronized "لا يوجد سجل حتى الآن."
            val text = file.readText(Charsets.UTF_8)
            if (text.length <= maxChars) text else "…\n" + text.takeLast(maxChars)
        }.getOrElse { "تعذر قراءة السجل: ${it.message}" }
    }

    fun clear(context: Context) = synchronized(lock) {
        runCatching { logFile(context).writeText("", Charsets.UTF_8) }
    }

    fun logFile(context: Context): File {
        val directory = File(context.filesDir, "logs")
        if (!directory.exists()) directory.mkdirs()
        return File(directory, "smart_tasks.log")
    }
}
