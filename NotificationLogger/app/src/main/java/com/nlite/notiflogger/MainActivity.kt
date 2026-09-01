package com.nlite.notiflogger

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var toggleSwitch: Switch

    companion object {
        const val PREFS_NAME = "notif_logger_prefs"
        const val KEY_ENABLED = "logging_enabled"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        statusText = findViewById(R.id.statusText)
        toggleSwitch = findViewById(R.id.toggleSwitch)

        toggleSwitch.isChecked = prefs.getBoolean(KEY_ENABLED, true)
        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(KEY_ENABLED, isChecked).apply()
            Toast.makeText(
                this,
                if (isChecked) "Logging enabled" else "Logging paused",
                Toast.LENGTH_SHORT
            ).show()
        }

        findViewById<Button>(R.id.btnGrantAccess).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        findViewById<Button>(R.id.btnViewLog).setOnClickListener {
            startActivity(Intent(this, PasscodeActivity::class.java))
        }

        findViewById<Button>(R.id.btnClearLog).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear all logs?")
                .setMessage("This permanently deletes all saved notification log files from this device.")
                .setPositiveButton("Clear") { _, _ ->
                    FileLogger.clearLogs(this)
                    Toast.makeText(this, "Logs cleared", Toast.LENGTH_SHORT).show()
                    updateStatus()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        findViewById<Button>(R.id.btnExcludeApps).setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val granted = isNotificationAccessGranted()
        val file = FileLogger.getTodayLogFile(this)
        val totalKb = FileLogger.totalSizeBytes(this) / 1024

        statusText.text = buildString {
            append(if (granted) "\u2705 Notification access granted\n" else "\u274C Notification access NOT granted\n")
            append("Today's file: ${file.name}\n")
            append("Total log size: $totalKb KB\n")
            append("Location: ${file.parentFile?.absolutePath}")
        }
    }

    private fun isNotificationAccessGranted(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners?.contains(packageName) == true
    }
}
