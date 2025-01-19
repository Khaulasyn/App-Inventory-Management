package com.project.inventorymanagement

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MovingActivity : AppCompatActivity() {

    private lateinit var code: String
    private lateinit var noInventarisTextView: TextView
    private lateinit var moveByEditText: EditText
    private lateinit var moveToEditText: EditText
    private lateinit var btnSave: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_moving)

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        code = intent.getStringExtra("code") ?: ""
        noInventarisTextView = findViewById(R.id.tv_no_inventaris)
        noInventarisTextView.text = "No Inventaris : ".plus(code)

        moveByEditText = findViewById(R.id.move_by)
        moveToEditText = findViewById(R.id.move_to)
        btnSave = findViewById(R.id.save_moving)

        btnSave.setOnClickListener {
            performMove()
        }
    }

    private fun performMove() {
        val serverAddress = sharedPreferences.getString("server_address", "")
        if (serverAddress.isNullOrEmpty()) {
            Toast.makeText(this, "Please set the server address in settings", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val moveBy = moveByEditText.text.toString()
        val moveTo = moveToEditText.text.toString()

        if (moveBy.isEmpty() || moveTo.isEmpty()) {
            Toast.makeText(this, "Move By and Move To cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        // Create request body with JSON payload
        val json = JSONObject().apply {
            put("no_inventaris", code)
            put("move_to", moveTo)
            put("move_by", moveBy)
        }

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$serverAddress/api/moving/")
            .post(requestBody)
            .build()

        val token = sharedPreferences.getString("auth_token", null)
        val client = token?.let { getAuthenticatedClient(it) }

        // Execute the request asynchronously
        if (client != null) {
            client.newCall(request).enqueue(object : Callback {

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@MovingActivity, "Success!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@MovingActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MovingActivity, "Failed: ${response.message}\nResponse: $responseBody", Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Handle request failure
                    runOnUiThread {
                        Toast.makeText(this@MovingActivity, "Request failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun getAuthenticatedClient(token: String): OkHttpClient {
        val sessionCookie = sharedPreferences.getString("session_cookie", null)

        return OkHttpClient.Builder()
            .cookieJar(MyCookieJar())
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .method(original.method, original.body)

                // Add session cookie to request if it exists
                sessionCookie?.let {
                    requestBuilder.header("Cookie", it)
                    Log.d(TAG, "Cookie $it")
                }

                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .build()
    }
}