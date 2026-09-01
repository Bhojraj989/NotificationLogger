package com.nlite.notiflogger

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ListView
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

data class AppInfo(val label: String, val packageName: String, val icon: Drawable?)

class AppListActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var excludedSet: MutableSet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        prefs = getSharedPreferences(MainActivity.PREFS_NAME, MODE_PRIVATE)
        excludedSet = (prefs.getStringSet(
            NotificationLoggerService.KEY_EXCLUDED_PACKAGES,
            emptySet()
        ) ?: emptySet()).toMutableSet()

        val listView = findViewById<ListView>(R.id.appListView)
        val apps = loadInstalledApps()

        listView.adapter = AppListAdapter(this, apps, excludedSet) { pkg, isOn ->
            if (isOn) excludedSet.remove(pkg) else excludedSet.add(pkg)
            prefs.edit()
                .putStringSet(NotificationLoggerService.KEY_EXCLUDED_PACKAGES, excludedSet)
                .apply()
        }
    }

    private fun loadInstalledApps(): List<AppInfo> {
        val pm = packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { AppInfo(pm.getApplicationLabel(it).toString(), it.packageName, pm.getApplicationIcon(it)) }
            .sortedBy { it.label.lowercase() }
    }
}

/** Simple, dependency-free list adapter — keeps the app lightweight (no RecyclerView needed). */
class AppListAdapter(
    private val context: Context,
    private val apps: List<AppInfo>,
    private val excludedSet: Set<String>,
    private val onToggle: (packageName: String, isOn: Boolean) -> Unit
) : BaseAdapter() {

    override fun getCount() = apps.size
    override fun getItem(position: Int): AppInfo = apps[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_app, parent, false)

        val app = apps[position]

        view.findViewById<ImageView>(R.id.appIcon).setImageDrawable(app.icon)
        view.findViewById<TextView>(R.id.appName).text = app.label

        val toggle = view.findViewById<Switch>(R.id.appToggle)
        // Clear any listener left over from a recycled row before setting state,
        // so we don't accidentally fire onToggle for the wrong app.
        toggle.setOnCheckedChangeListener(null)
        toggle.isChecked = !excludedSet.contains(app.packageName)
        toggle.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            onToggle(app.packageName, isChecked)
        }

        return view
    }
}
