package com.nexplay

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue

object StreamFileLogger {
    private const val TAG = "StreamFileLogger"
    private const val MAX_LOG_ENTRIES = 500
    private const val LOG_FILE_PREFIX = "nexplay_stream_"
    private const val LOG_FILE_SUFFIX = ".log"

    private val logQueue = ConcurrentLinkedQueue<String>()
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
    private var currentLogFile: File? = null
    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true
        logQueue.clear()
        currentLogFile = createLogFile(context)
        Log.d(TAG, "Stream logger initialized: ${currentLogFile?.absolutePath}")
    }

    private fun createLogFile(context: Context): File {
        val downloadsDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        } else {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }

        val logDir = File(downloadsDir, "NexPlayLogs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        val timestamp = fileDateFormat.format(Date())
        return File(logDir, "${LOG_FILE_PREFIX}${timestamp}${LOG_FILE_SUFFIX}")
    }

    fun log(tag: String, message: String) {
        val timestamp = timestampFormat.format(Date())
        val logEntry = "[$timestamp] $tag: $message"
        logQueue.add(logEntry)

        while (logQueue.size > MAX_LOG_ENTRIES) {
            logQueue.poll()
        }

        Log.d(tag, message)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = timestampFormat.format(Date())
        val errorDetail = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            "\n${sw.toString()}"
        } ?: ""
        val logEntry = "[$timestamp] ERROR $tag: $message$errorDetail"
        logQueue.add(logEntry)

        while (logQueue.size > MAX_LOG_ENTRIES) {
            logQueue.poll()
        }

        Log.e(tag, message, throwable)
    }

    fun flushToFile(context: Context) {
        val file = currentLogFile ?: createLogFile(context)
        currentLogFile = file

        try {
            FileOutputStream(file, true).use { fos ->
                PrintWriter(fos).use { writer ->
                    writer.println("=== NexPlay Stream Log ===")
                    writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    writer.println("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    writer.println("App Version: ${BuildConfig.VERSION_NAME}")
                    writer.println("=== Begin Log ===")
                    writer.println()

                    for (entry in logQueue) {
                        writer.println(entry)
                    }

                    writer.println()
                    writer.println("=== End Log ===")
                    writer.flush()
                }
            }
            Log.d(TAG, "Log flushed to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to flush log to file", e)
        }
    }

    fun getLogFile(): File? = currentLogFile

    fun clearLogs() {
        logQueue.clear()
        currentLogFile = null
        isInitialized = false
    }
}
