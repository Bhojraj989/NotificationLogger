# Notification Logger

A lightweight, fully offline Android app that captures system notifications
and appends them to a local `.txt` file on your device.

- **No internet permission** — check `AndroidManifest.xml`, `INTERNET` is not
  even declared, so the app is physically incapable of sending data anywhere.
- **AES-encrypted log file** — the `.txt` file on disk is never plain text.
  Opening it with any other app (file manager, text editor) shows unreadable
  bytes. Only this app, with the correct passcode, can decrypt it.
- **Passcode-protected in-app viewer/editor** — default passcode is `3232`
  (change it in `CryptoUtils.kt` if you want a different one).
- **No ads, analytics, or third-party SDKs.**
- **Event-driven** — uses `NotificationListenerService.onNotificationPosted()`,
  no polling loops, no wake locks.
- **Minimal dependencies** — only `androidx.core` and `androidx.appcompat`.

## Project structure

```
NotificationLogger/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/nlite/notiflogger/
│       │   ├── MainActivity.kt              # UI: toggle, buttons, status
│       │   ├── NotificationLoggerService.kt # captures notifications
│       │   ├── FileLogger.kt                # writes/reads the .txt file
│       │   └── BootReceiver.kt              # helps resume logging after reboot
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/ (strings, colors, themes)
│           ├── xml/file_paths.xml           # FileProvider config for sharing
│           └── drawable/ic_launcher.xml     # vector icon, no PNG assets
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## How to open and build

1. Open **Android Studio** (Giraffe/Koala or newer recommended).
2. **File → Open** and select the `NotificationLogger` folder.
3. Let Gradle sync (it will download the Android Gradle Plugin + Kotlin
   plugin the first time — this is the only network activity involved, and
   it's build-tooling only, not app behavior).
4. Connect a device or start an emulator, then click **Run ▶**.

To build a release APK: **Build → Generate Signed Bundle / APK**, choose APK,
and follow the signing wizard. `minifyEnabled` and `shrinkResources` are
already turned on for release builds to keep the APK small.

## How to test it

1. Launch the app. It opens a single screen with a status box and buttons.
2. Tap **Grant Notification Access** — this opens the system
   *Settings → Notification Access* screen (this permission cannot be
   requested as a normal runtime permission, Android requires the manual
   Settings toggle).
3. Find **Notification Logger** in that list and enable it.
4. Go back to the app — the status box should now show
   "✅ Notification access granted".
5. Trigger a few notifications (e.g. send yourself a WhatsApp/Telegram
   message, get an email, etc.).
6. Reopen the app — the status box shows the current log file name and size.
7. Tap **View / Share Log File** to open or share the plain-text log, e.g.:

   ```
   [2026-08-20 14:32:11] WhatsApp - John Doe: Hey, are you free later?
   [2026-08-20 14:35:02] Gmail - Team standup: Meeting moved to 3 PM
   ```

8. Tap **Manage Excluded Apps** to pick specific apps you don't want logged
   (e.g. banking apps, OTP apps).
9. Tap **Clear All Logs** to permanently delete all saved log files.

## Where the log file is stored

`context.getExternalFilesDir(null)` — this resolves to a path like:

```
/storage/emulated/0/Android/data/com.nlite.notiflogger/files/notifications_2026-08-20.txt
```

This is **app-specific storage**: private to the app, requires no broad
storage permission on Android 10+ (scoped storage), and is automatically
deleted if the app is uninstalled. A new file is created each day
(`notifications_YYYY-MM-DD.txt`), and entries are appended, never
overwritten.

If external storage isn't available for some reason, it automatically falls
back to internal storage (`context.filesDir`), which is even more private
but not visible via a file manager.

## Notes and limitations

- Some OEM Android skins (Xiaomi/MIUI, Oppo, Vivo, etc.) aggressively kill
  background services. If logging stops working after a while, check the
  battery-optimization settings for the app and set it to "unrestricted" /
  "no restrictions."
- Notifications that only contain images (no title/text) or empty group
  summaries are skipped.
- The `BootReceiver` doesn't itself "start" the listener — Android already
  rebinds a granted `NotificationListenerService` automatically after boot.
  It just toggles the service component off/on as a nudge for OEMs that are
  slow or unreliable about rebinding.
- This app stores potentially sensitive notification content in plain text
  on your device. Treat the log file with the same care as your messages —
  anyone with file access to your device (or an unencrypted backup) could
  read it.
