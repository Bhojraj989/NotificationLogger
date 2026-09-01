package com.nlite.notiflogger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import java.io.File

/**
 * Lets the user view, edit, save, share, or delete a log file — all inside
 * this app. No other app is needed to inspect or modify the log.
 */
class LogViewerActivity : AppCompatActivity() {

    private lateinit var spinner: Spinner
    private lateinit var editText: EditText
    private var logFiles: List<File> = emptyList()
    private var currentFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_viewer)

        spinner = findViewById(R.id.fileSpinner)
        editText = findViewById(R.id.logEditText)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveCurrentFile() }
        findViewById<Button>(R.id.btnShare).setOnClickListener { shareCurrentFile() }
        findViewById<Button>(R.id.btnDelete).setOnClickListener { confirmDeleteCurrentFile() }

        loadFileList()
    }

    private fun loadFileList() {
        logFiles = FileLogger.listLogFiles(this)

        if (logFiles.isEmpty()) {
            editText.setText("")
            currentFile = null
            spinner.adapter = ArrayAdapter(
                this, android.R.layout.simple_spinner_item, listOf("No log files yet")
            )
            Toast.makeText(this, "No log files yet — trigger a notification first", Toast.LENGTH_SHORT).show()
            return
        }

        val names = logFiles.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                loadFile(logFiles[position])
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadFile(logFiles[0])
    }

    private fun loadFile(file: File) {
        currentFile = file
        editText.setText(FileLogger.readFile(file))
    }

    private fun saveCurrentFile() {
        val file = currentFile ?: run {
            Toast.makeText(this, "No file selected", Toast.LENGTH_SHORT).show()
            return
        }
        FileLogger.writeFile(file, editText.text.toString())
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
    }

    private fun shareCurrentFile() {
        val file = currentFile ?: return
        if (!file.exists()) {
            Toast.makeText(this, "Nothing to share yet", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Share encrypted log file"))
    }

    private fun confirmDeleteCurrentFile() {
        val file = currentFile ?: return
        AlertDialog.Builder(this)
            .setTitle("Delete this file?")
            .setMessage("This permanently deletes ${file.name} from this device.")
            .setPositiveButton("Delete") { _, _ ->
                file.delete()
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                loadFileList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
