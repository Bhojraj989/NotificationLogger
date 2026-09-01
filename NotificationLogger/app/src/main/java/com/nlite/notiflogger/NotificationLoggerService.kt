package com.nlite.notiflogger

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Event-driven notification capture. No polling, no wake locks — the system
 * calls onNotificationPosted() only when a new notification actually arrives.
 */
class NotificationLoggerService : NotificationListenerService() {

    companion object {
        const val KEY_EXCLUDED_PACKAGES = "excluded_packages"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        try {
            handleNotification(sbn)
        } catch (e: Exception) {
            // Never crash the listener service — just skip this entry.
        }
    }

    private fun handleNotification(sbn: StatusBarNotification) {
        val prefs = applicationContext.getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)

        // Respect the on/off toggle in the app UI
        if (!prefs.getBoolean(MainActivity.KEY_ENABLED, true)) return

        val pkg = sbn.packageName

        // Don't log our own notifications (we don't post any, but just in case)
        if (pkg == packageName) return

        // Respect the user's exclude list
        val excluded = prefs.getStringSet(KEY_EXCLUDED_PACKAGES, emptySet()) ?: emptySet()
        if (excluded.contains(pkg)) return

        // Skip pure group-summary stubs (they carry no real content of their own —
        // the individual notifications in the group are posted separately anyway).
        // But if this app ONLY ever posts group-summary notifications (some apps do),
        // logging nothing would mean we never see them at all — so we still record it,
        // just tagged clearly, instead of silently dropping it.
        val isGroupSummary = (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        val extras = sbn.notification.extras
        var title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        var body = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()

        // Fallback chain for apps that don't use plain EXTRA_TEXT
        if (body.isBlank()) {
            body = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        }
        if (body.isBlank()) {
            body = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString().orEmpty()
        }
        if (body.isBlank()) {
            body = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString().orEmpty()
        }
        if (body.isBlank()) {
            body = extras.getCharSequence(Notification.EXTRA_INFO_TEXT)?.toString().orEmpty()
        }

        // InboxStyle notifications (e.g. stacked alerts) store lines separately.
        if (body.isBlank()) {
            @Suppress("DEPRECATION")
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            if (!lines.isNullOrEmpty()) {
                body = lines.joinToString(" | ") { it.toString() }
            }
        }

        // WhatsApp/Telegram/Signal-style chat notifications (MessagingStyle) store the
        // actual message text inside EXTRA_MESSAGES, not EXTRA_TEXT — grab the latest one.
        if (body.isBlank()) {
            @Suppress("DEPRECATION")
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            val lastMessage = messages?.lastOrNull() as? android.os.Bundle
            val msgText = lastMessage?.getCharSequence("text")?.toString().orEmpty()
            val msgSender = lastMessage?.getCharSequence("sender")?.toString().orEmpty()
            if (msgText.isNotBlank()) {
                body = msgText
                if (title.isBlank() && msgSender.isNotBlank()) title = msgSender
            }
        }

        // Some apps (trading/banking apps especially) use a fully custom notification
        // layout (RemoteViews) with NO standard title/text fields at all — there is
        // genuinely no text to extract in that case. Rather than silently dropping the
        // notification (which made it look like it was never captured), we still log
        // an entry so you can see it arrived, just with a placeholder note.
        if (title.isBlank() && body.isBlank()) {
            if (isGroupSummary) return // pure summary + no content at all — safe to skip
            body = "(no readable text — app uses a custom notification layout)"
        }

        val appLabel = getAppLabel(pkg)
        FileLogger.appendEntry(applicationContext, appLabel, title, body)
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val pm = applicationContext.packageManager
            val appInfo: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
