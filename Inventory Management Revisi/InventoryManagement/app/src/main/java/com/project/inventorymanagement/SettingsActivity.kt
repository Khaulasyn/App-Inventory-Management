package com.project.inventorymanagement

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var serverAddressEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)

        serverAddressEditText = findViewById(R.id.server_address_edit_text)
        saveButton = findViewById(R.id.save_button)
        sharedPreferences = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Load saved server address if it exists
        val savedServerAddress = sharedPreferences.getString("server_address", "")
        serverAddressEditText.setText(savedServerAddress)

        saveButton.setOnClickListener {
            val serverAddress = serverAddressEditText.text.toString()
            if (serverAddress.isNotEmpty()) {
                // Save server address to SharedPreferences
                val editor = sharedPreferences.edit()
                editor.putString("server_address", serverAddress)
                editor.apply()
                Toast.makeText(this, "Server address saved", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please enter a valid server address", Toast.LENGTH_SHORT).show()
            }
        }
    }
}