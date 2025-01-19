package com.project.inventorymanagement

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class AddInventoryActivity : AppCompatActivity() {

    private lateinit var noEditText: EditText
    private lateinit var itemEditText: EditText
    private lateinit var specificationEditText: EditText
    private lateinit var locationEditText: EditText
    private lateinit var userEditText: EditText
    private lateinit var userIdEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var code: String
    private lateinit var progressBar: ProgressBar
    private lateinit var condition: String

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var selectPhotoButton: Button
    private lateinit var takePhotoButton: Button
    private lateinit var photoImageView: ImageView
    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_IMAGE_CAPTURE = 2
    private val REQUEST_PERMISSIONS = 3
    private var photoUri: Uri? = null
    private lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_inventory)

        sharedPreferences = getSharedPreferences("MyAppPrefs", MODE_PRIVATE)
        code = intent.getStringExtra("code") ?: ""

        val conditionSpinner: Spinner = findViewById(R.id.condition_spinner)
        // Array for display values
        val conditionDisplayValues = arrayOf("1: ok", "2: not ok", "3: afkir")
        // Array for actual values to send to API
        val conditionActualValues = arrayOf("1", "2", "3")
        // Set up the ArrayAdapter for the Spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conditionDisplayValues)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        conditionSpinner.adapter = adapter
        conditionSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                condition = conditionActualValues[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle no selection case if needed
            }
        }

        noEditText = findViewById(R.id.no)
        noEditText.setText(code)
        itemEditText = findViewById(R.id.item)
        specificationEditText = findViewById(R.id.specification)
        locationEditText = findViewById(R.id.location)
        userIdEditText = findViewById(R.id.user_id)
        val user_id = sharedPreferences.getString("user_id", "")
        userIdEditText.setText(user_id)
        userEditText = findViewById(R.id.user)
        submitButton = findViewById(R.id.submit)
        progressBar = findViewById(R.id.loading_add_inventory)

        // Set up submit button
        submitButton.setOnClickListener {
            submitInventoryData()
        }

        selectPhotoButton = findViewById(R.id.selectPhotoButton)
        takePhotoButton = findViewById(R.id.takePhotoButton)
        photoImageView = findViewById(R.id.photo)

        selectPhotoButton.setOnClickListener {
            openGallery()
        }

        takePhotoButton.setOnClickListener {
            if (checkPermissions()) {
                openCamera()
            }
        }
    }

    private fun submitInventoryData() {
        progressBar.visibility = View.VISIBLE
        submitButton.visibility = View.GONE
        val serverAddress = sharedPreferences.getString("server_address", "")
        if (serverAddress.isNullOrEmpty()) {
            Toast.makeText(this, "Please set the server address in settings", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val no = noEditText.text.toString()
        val item = itemEditText.text.toString()
        val specification = specificationEditText.text.toString()
        val location = locationEditText.text.toString()
        val user = userEditText.text.toString()

        val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
        requestBodyBuilder.addFormDataPart("no", no)
        requestBodyBuilder.addFormDataPart("name", item)
        requestBodyBuilder.addFormDataPart("specifications", specification)
        requestBodyBuilder.addFormDataPart("location", location)
        requestBodyBuilder.addFormDataPart("user", user)
        requestBodyBuilder.addFormDataPart("condition", condition)

        photoUri?.let {
            val file = File(currentPhotoPath)
            val photoRequestBody = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            requestBodyBuilder.addFormDataPart("photo", file.name, photoRequestBody)
        }

        val requestBody = requestBodyBuilder.build()

        val request = Request.Builder()
            .url("$serverAddress/api/add_inventory/")
            .post(requestBody)
            .build()

        val token = sharedPreferences.getString("auth_token", null)
        val client = token?.let { getAuthenticatedClient(it) }

        if (client != null) {
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@AddInventoryActivity, "Gagal Menyimpan Data!", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Error during submission", e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            submitButton.visibility = View.VISIBLE
                            Toast.makeText(this@AddInventoryActivity, "Inventory Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this@AddInventoryActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            submitButton.visibility = View.VISIBLE
                            Toast.makeText(this@AddInventoryActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                null
            }
            photoFile?.also {
                photoUri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.fileprovider",
                    it
                )
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }


    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    val selectedImage: Uri? = data?.data
                    selectedImage?.let {
                        photoImageView.setImageURI(it)
                        photoUri = it
                        currentPhotoPath = getRealPathFromURI(it).toString()
                    }
                }
                REQUEST_IMAGE_CAPTURE -> {
                    photoUri?.let {
                        photoImageView.setImageURI(it)
                    }
                }
            }
        }
    }

    private fun getRealPathFromURI(contentUri: Uri): String? {
        var result: String? = null
        val proj = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(contentUri, proj, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                result = it.getString(columnIndex)
            }
        }
        return result
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_PERMISSIONS)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS && grantResults.isNotEmpty()) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                openCamera()
            }
        }
    }
}