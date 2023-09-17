package com.example.hotelvendor

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class menu_crud : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_crud)

        val btnOrder = findViewById<Button>(R.id.btnOrder) // Add this line
        val btnList = findViewById<Button>(R.id.btnlist) // Add this line

        btnOrder.setOnClickListener {
            val intent = Intent(this, Order::class.java)
            startActivity(intent)
        }

        btnList.setOnClickListener {
            val intent = Intent(this, List::class.java)
            startActivity(intent)
        }
    }
}
