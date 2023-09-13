package com.example.hotelvendor

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

data class User(
    val username: String = "",
    val email: String = "",
    val password: String = ""
)

class Signup : AppCompatActivity() {
    private var isPasswordVisible = false
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Hotels")

        val loginAccountTextView = findViewById<TextView>(R.id.etnewact)
        loginAccountTextView.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val username = findViewById<EditText>(R.id.etname)
        val email = findViewById<EditText>(R.id.etemail)
        val password = findViewById<EditText>(R.id.etpassword)
        val confirmPassword = findViewById<EditText>(R.id.etconfirmpassword)

        val btnsignup = findViewById<Button>(R.id.btnOrder)
        btnsignup.setOnClickListener {
            val usernameText = username.text.toString().trim()
            val emailText = email.text.toString().trim()
            val passwordText = password.text.toString().trim()
            val confirmPasswordText = confirmPassword.text.toString().trim()

            if (usernameText.isNotEmpty() && emailText.isNotEmpty() && passwordText.isNotEmpty() && confirmPasswordText.isNotEmpty()) {
                if (passwordText == confirmPasswordText) {
                    if (isValidPassword(passwordText)) {
                        // Create a new user account with Firebase Authentication
                        auth.createUserWithEmailAndPassword(emailText, passwordText)
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    // User registration success
                                    val user = auth.currentUser
                                    val uid = user?.uid

                                    // Store additional user data in Realtime Database
                                    uid?.let {
                                        val userData = User(usernameText, emailText, passwordText)
                                        database.child(it).setValue(userData)
                                    }

                                    val intent = Intent(this,HotelInfoBottomSheetFragment::class.java)
                                    startActivity(intent)
                                    Toast.makeText(this, "SIGNUP Successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    // User registration failed
                                    Toast.makeText(
                                        this,
                                        "User registration failed: ${task.exception?.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                    } else {
                        Toast.makeText(
                            this,
                            "Password must have at least 8 characters and contain a special character.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Confirm Password does not match, please try again", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }

        val showPasswordButton = findViewById<ImageButton>(R.id.btnShowPassword)

        showPasswordButton.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            togglePasswordVisibility(password, isPasswordVisible)
            toggleEyeIcon(showPasswordButton, isPasswordVisible)
        }
    }

    private fun togglePasswordVisibility(editText: EditText, isVisible: Boolean) {
        if (isVisible) {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
        } else {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
        }
        editText.setSelection(editText.text.length)
    }

    private fun toggleEyeIcon(imageButton: ImageButton, isVisible: Boolean) {
        val iconResource = if (isVisible) R.drawable.ic_eye else R.drawable.ic_eye
        imageButton.setImageResource(iconResource)
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 && password.contains(Regex("[^a-zA-Z0-9]"))
    }
}
