package com.project.inventorymanagement

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ReviewActivity : AppCompatActivity() {

    private lateinit var imageViewCropped: ImageView
    private lateinit var readableNo: TextView
    private lateinit var noInventaris: TextView
    private lateinit var item: TextView
    private lateinit var spesifikasi: TextView
    private lateinit var lokasi: TextView
//    private lateinit var department: TextView
    private lateinit var user: TextView
    private lateinit var kondisi: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnTambahInventaris: Button
    private lateinit var btnChangeStatus: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var no_inventaris_cleaned: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_review)

        imageViewCropped = findViewById(R.id.imageViewCropped)
        readableNo = findViewById(R.id.readableNo)
        noInventaris = findViewById(R.id.no_inventaris)
        item = findViewById(R.id.item)
        spesifikasi = findViewById(R.id.spesifikasi)
//        department = findViewById(R.id.tv_department)
        lokasi = findViewById(R.id.lokasi)
        user = findViewById(R.id.user)
        kondisi = findViewById(R.id.kondisi)
        scrollView = findViewById(R.id.scrollview)
        btnTambahInventaris = findViewById(R.id.btnTambahInventaris)
        btnChangeStatus = findViewById(R.id.btnChangeStatus)
        progressBar = findViewById(R.id.loading)
        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)

        val imagePath = intent.getStringExtra("imagePath") ?: return

        val bitmap = BitmapFactory.decodeFile(imagePath)
        imageViewCropped.setImageBitmap(bitmap)

//        cropAndShowImage(bitmap)
        runTextRecognition(bitmap)
    }

    private fun runTextRecognition(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                processTextRecognitionResult(visionText)
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun processTextRecognitionResult(text: com.google.mlkit.vision.text.Text) {
        val resultText = text.text
        // Menghilangkan karakter selain angka dan titik
        val filteredText = resultText.replace(Regex("[^0-9.]"), "")
        readableNo.text = "Hasil scan : $filteredText"
        no_inventaris_cleaned = filteredText
        checkInventory(filteredText)
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

    private fun checkInventory(code: String) {
        val serverAddress = sharedPreferences.getString("server_address", "")
        if (serverAddress.isNullOrEmpty()) {
            Toast.makeText(this, "Please set the server address in settings", Toast.LENGTH_SHORT)
                .show()
            return
        }

        // Create request body with JSON payload
        val json = JSONObject().apply {
            put("inventory_no", code)
        }
        val requestBody =
            json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val token = sharedPreferences.getString("auth_token", null)
        val client = token?.let { getAuthenticatedClient(it) }

        // Create request to post data to the server
        val request = Request.Builder()
            .url("$serverAddress/api/check_inventory/") // Change to your check_inventory URL
            .post(requestBody)
            .build()

        // Execute the request asynchronously
        if (client != null) {
            client.newCall(request).enqueue(object : Callback {

                override fun onResponse(call: Call, response: Response) {
                    val responseBody = response.body?.string()

                    if (!response.isSuccessful || responseBody == null) {
                        runOnUiThread {
                            showPopup("Inventory check failed")
                        }
                        return
                    }

                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.has("error")) {
                        runOnUiThread {
                            showPopup("Inventory Tidak Ditemukan!")
                            showAddInventoryButton()
                        }
                    } else {
                        val itemJson = jsonResponse.getJSONObject("item")
                        runOnUiThread {
                            showPopup("Inventory Ditemukan!")
                            populateInventoryDetails(itemJson)
                        }
                    }
                }

                override fun onFailure(call: Call, e: IOException) {
                    // Handle request failure
                    runOnUiThread {
                        showPopup("Request failed: ${e.message}")
                    }
                }
            })
        }
    }
    private fun populateInventoryDetails(itemJson: JSONObject ) {
        noInventaris.text = itemJson.optString("no", "-")
        item.text = itemJson.optString("name", "-")
        spesifikasi.text = itemJson.optString("specifications", "-")
//        department.text = itemJson.optString("department", "-")
        lokasi.text = itemJson.optString("location", "-")
        user.text = itemJson.optString("pic", "-")
        kondisi.text = itemJson.optString("condition", "-")
        readableNo.text = readableNo.text.toString().plus(" (ditemukan)")
        progressBar.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
        btnTambahInventaris.visibility = View.GONE
        btnChangeStatus.setOnClickListener {
            val intent = Intent(this, SelectChangeStatusActivity::class.java)
            intent.putExtra("code", no_inventaris_cleaned)
            startActivity(intent)
        }
    }

    private fun showAddInventoryButton() {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.GONE
        btnTambahInventaris.visibility = View.VISIBLE
        readableNo.text = readableNo.text.toString().plus(" (tidak ditemukan)")
        btnTambahInventaris.setOnClickListener {
            val intent = Intent(this, AddInventoryActivity::class.java)
            intent.putExtra("code", no_inventaris_cleaned)
            startActivity(intent)
        }
    }

    private fun showPopup(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
