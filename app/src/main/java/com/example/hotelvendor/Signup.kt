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

class Signup : AppCompatActivity() {
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val loginAccountTextView = findViewById<TextView>(R.id.etnewact)
        loginAccountTextView.setOnClickListener {
            val intent = Intent(this, menu_crud::class.java)
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
                        // Instead of using an Intent, show the bottom sheet fragment
                        val bottomSheetFragment = HotelInfoBottomSheetFragment.newInstance(usernameText, emailText, passwordText)
                        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)

                        Toast.makeText(this, "SIGNUP Successfully", Toast.LENGTH_SHORT).show()
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
