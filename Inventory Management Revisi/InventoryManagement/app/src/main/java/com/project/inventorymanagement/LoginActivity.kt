package com.project.inventorymanagement

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var errorMessageTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var settingsButton: ImageButton
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.login)
        errorMessageTextView = findViewById(R.id.error_message)
        settingsButton = findViewById(R.id.settings_button)
        progressBar = findViewById(R.id.loading)

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        loginButton.setOnClickListener {
            performLogin()
        }
    }


    @SuppressLint("SetTextI18n")
    private fun performLogin() {
        progressBar.visibility = View.VISIBLE
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        // Get server address from SharedPreferences
        val serverAddress = sharedPreferences.getString("server_address", "")
        if (serverAddress.isNullOrEmpty()) {
            Toast.makeText(this, "Please set the server address in settings", Toast.LENGTH_SHORT).show()
            return
        }

        if (username.isEmpty() || password.isEmpty()) {
            errorMessageTextView.text = "Username and password cannot be empty."
            errorMessageTextView.visibility = View.VISIBLE
            return
        }

        // Create OkHttpClient instance
        val client = OkHttpClient.Builder()
            .cookieJar(MyCookieJar())
            .build()

        // Create request body with JSON payload
        val json = JSONObject().apply {
            put("username", username)
            put("password", password)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        // Log the request details
        Log.d("LoginActivity", "Request URL: $serverAddress/api/login/")
        Log.d("LoginActivity", "Request Body: $json")
        // Create request to post data to the server
        val request = Request.Builder()
            .url("$serverAddress/api/login/") // Change to your login URL
            .post(requestBody)
            .build()

        // Execute the request asynchronously
        // Execute the request asynchronously
        client.newCall(request).enqueue(object : Callback {

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                if (!response.isSuccessful) {
                    // Handle unsuccessful response
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Login failed: ${response.message}\nResponse: $responseBody",
                            Toast.LENGTH_LONG
                        ).show()
                        progressBar.visibility = View.GONE
                    }
                    return
                }

                // Handle successful response
                val jsonResponse = responseBody?.let { JSONObject(it) }
                val token = jsonResponse?.optString("token") // Change to the actual key in your response
                val userID = jsonResponse?.optString("user_id") // Change to the actual key in your response

                if (!token.isNullOrEmpty() && !userID.isNullOrEmpty()) {
                    // Save token in SharedPreferences
                    val editor = sharedPreferences.edit()
                    editor.putString("auth_token", token)
                    editor.putString("user_id", userID)
                    editor.apply()
                    Log.d(TAG, "Authenticated with token : ")
                    Log.d(TAG, token)
                    Log.d(TAG, "User ID : ")
                    Log.d(TAG, userID)

                    // Save cookies (sessionid) from response if needed
                    val cookies = response.headers.values("Set-Cookie")
                    if (cookies.isNotEmpty()) {
                        editor.putString("session_cookie", cookies[0])
                        editor.apply()
                    }
                    Log.d(TAG, cookies.toString())
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                    }
                    // Navigate to another activity
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Optional: close the LoginActivity
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@LoginActivity,
                            "Invalid credentials",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: okhttp3.Call, e: IOException) {
                // Handle request failure
                runOnUiThread {
                    Toast.makeText(
                        this@LoginActivity,
                        "Request failed: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }
}