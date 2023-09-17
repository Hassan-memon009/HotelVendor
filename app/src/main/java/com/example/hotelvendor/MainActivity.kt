package com.example.hotelvendor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.*
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var showPasswordButton: ImageButton
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_main)

        emailEditText = findViewById(R.id.etusername)
        passwordEditText = findViewById(R.id.etpassword)
        showPasswordButton = findViewById(R.id.showPasswordButton)

        showPasswordButton.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility()
        }

        val btnregister = findViewById<TextView>(R.id.etnewact)

        btnregister.setOnClickListener {
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
        }

        val btnLogin = findViewById<Button>(R.id.btnlogin)

        btnLogin.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                readData(email, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readData(email: String, password: String) {
        database = FirebaseDatabase.getInstance().getReference("Hotels")
        database.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    var isPasswordCorrect = false
                    for (userSnapshot in dataSnapshot.children) {
                        val user = userSnapshot.getValue(HotelUser::class.java)
                        if (user != null && user.password == password) {
                            // User exists and the password matches, allow login
                            isPasswordCorrect = true
                            val intent = Intent(this@MainActivity, Order::class.java)
                            startActivity(intent)
                            Toast.makeText(this@MainActivity, "Login Successful", Toast.LENGTH_SHORT).show()
                            return
                        }
                    }
                    // User exists but password doesn't match
                    if (!isPasswordCorrect) {
                        Toast.makeText(this@MainActivity, "Incorrect Password", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // User doesn't exist
                    Toast.makeText(this@MainActivity, "User doesn't exist", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to read data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
            showPasswordButton.setImageResource(R.drawable.ic_eye) // Change to your icon when password is hidden
        } else {
            passwordEditText.transformationMethod = null
            showPasswordButton.setImageResource(R.drawable.ic_eye) // Change to your icon when password is visible
        }
        passwordEditText.setSelection(passwordEditText.text.length)
    }
}
