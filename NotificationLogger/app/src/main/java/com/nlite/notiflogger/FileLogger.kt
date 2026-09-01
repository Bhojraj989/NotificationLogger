package com.nlite.notiflogger

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles all reading/writing of the local notification log file.
 * Everything stays on-device — no network calls of any kind.
 */
object FileLogger {

    private const val FILE_PREFIX = "notifications_"
    private const val FILE_SUFFIX = ".txt"

    /**
     * App-specific storage directory. Falls back to internal storage
     * if external storage isn't available — either way it's private
     * to this app and needs no runtime storage permission on modern Android.
     */
    private fun getLogDir(context: Context): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Log file rotates daily, e.g. notifications_2026-08-20.txt */
    fun getTodayLogFile(context: Context): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return File(getLogDir(context), "$FILE_PREFIX$dateStr$FILE_SUFFIX")
    }

    fun listLogFiles(context: Context): List<File> {
        return getLogDir(context)
            .listFiles { f -> f.name.startsWith(FILE_PREFIX) && f.name.endsWith(FILE_SUFFIX) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    @Synchronized
    fun appendEntry(context: Context, appLabel: String, title: String, text: String) {
        val file = getTodayLogFile(context)
        val timestamp = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.US).format(Date())
        val safeTitle = title.trim()
        val safeText = text.trim()

        // Multi-line, phone-notification-style block instead of one long line.
        val line = buildString {
            append("App: $appLabel\n")
            append("Time: $timestamp\n")
            if (safeTitle.isNotEmpty()) append("Title: $safeTitle\n")
            if (safeText.isNotEmpty()) append("Message: $safeText\n")
            append("----------------------------------------\n\n")
        }
        // Every entry is appended on top of the existing (decrypted) content,
        // then the whole file is re-encrypted. The file on disk is never
        // plain text — only this app, with the correct passcode, can read it.
        val existing = CryptoUtils.decryptFile(file)
        CryptoUtils.encryptToFile(file, existing + line)
    }

    fun clearLogs(context: Context) {
        listLogFiles(context).forEach { it.delete() }
    }

    fun totalSizeBytes(context: Context): Long {
        return listLogFiles(context).sumOf { it.length() }
    }

    /** Returns the decrypted plain text, or "" if the file is empty/unreadable. */
    fun readFile(file: File): String {
        return CryptoUtils.decryptFile(file)
    }

    /** Encrypts [content] and overwrites the file with it. */
    fun writeFile(file: File, content: String) {
        CryptoUtils.encryptToFile(file, content)
    }
}
