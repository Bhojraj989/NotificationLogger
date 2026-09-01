package com.nlite.notiflogger

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * NotificationListenerService is automatically rebound by the system after
 * boot IF the user has already granted Notification Access. This receiver
 * adds a small safety net: toggling the service component off/on forces
 * Android to re-evaluate and rebind the listener, which helps on some OEM
 * skins that are aggressive about killing/deferring listener services.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pm = context.packageManager
        val component = ComponentName(context, NotificationLoggerService::class.java)

        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
