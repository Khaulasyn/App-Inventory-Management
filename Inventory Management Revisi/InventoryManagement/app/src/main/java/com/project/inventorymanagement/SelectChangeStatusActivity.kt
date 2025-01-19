package com.project.inventorymanagement

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

class SelectChangeStatusActivity : AppCompatActivity() {

    private lateinit var code: String
    private lateinit var noInventarisTextView: TextView
    private lateinit var btnMoving: Button
    private lateinit var btnService: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_select_change_status)


        code = intent.getStringExtra("code") ?: ""

        noInventarisTextView = findViewById(R.id.tv_no_inventaris)
        noInventarisTextView.text = "No Inventaris : ".plus(code)

        btnMoving = findViewById(R.id.btn_moving)
        btnService = findViewById(R.id.btn_service)

        btnMoving.setOnClickListener {
            val intent = Intent(this, MovingActivity::class.java)
            intent.putExtra("code", code)
            startActivity(intent)
        }
        btnService.setOnClickListener {
            val intent = Intent(this, ServiceActivity::class.java)
            intent.putExtra("code", code)
            startActivity(intent)
        }
    }
}