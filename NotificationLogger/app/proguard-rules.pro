# Add project specific ProGuard rules here.
# Keep the NotificationListenerService so it isn't stripped/renamed in a way
# that breaks system binding.
-keep class com.nlite.notiflogger.NotificationLoggerService { *; }
-keep class com.nlite.notiflogger.BootReceiver { *; }
