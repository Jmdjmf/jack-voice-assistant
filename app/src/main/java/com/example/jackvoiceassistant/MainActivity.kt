package com.example.jackvoiceassistant

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private val requiredPermissions = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS
    )
    private val permissionRequestCode = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        val startButton: Button = findViewById(R.id.startButton)
        val stopButton: Button = findViewById(R.id.stopButton)

        startButton.setOnClickListener {
            if (hasAllPermissions()) {
                startVoiceService()
            } else {
                requestNeededPermissions()
            }
        }

        stopButton.setOnClickListener {
            stopVoiceService()
        }
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestNeededPermissions() {
        val toRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions
        } else {
            arrayOf(Manifest.permission.RECORD_AUDIO)
        }
        ActivityCompat.requestPermissions(this, toRequest, permissionRequestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (hasAllPermissions()) {
                startVoiceService()
            } else {
                Toast.makeText(this, "Jack needs microphone permission to work", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startVoiceService() {
        val serviceIntent = Intent(this, VoiceAssistantService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        statusText.text = "Status: Listening..."
    }

    private fun stopVoiceService() {
        val serviceIntent = Intent(this, VoiceAssistantService::class.java)
        stopService(serviceIntent)
        statusText.text = "Status: Stopped"
    }
}
