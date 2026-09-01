package com.nlite.notiflogger

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PasscodeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passcode)

        val input = findViewById<EditText>(R.id.passcodeInput)

        findViewById<Button>(R.id.btnUnlock).setOnClickListener {
            if (CryptoUtils.verifyPasscode(input.text.toString().trim())) {
                startActivity(Intent(this, LogViewerActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, getString(R.string.wrong_passcode), Toast.LENGTH_SHORT).show()
                input.setText("")
            }
        }

        findViewById<Button>(R.id.btnCancel).setOnClickListener { finish() }
    }
}
