package com.example.hotelvendor

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.os.Bundle
import android.app.Activity.RESULT_OK
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

data class Hotel_User(
    val hotelName: String,
    val email: String,
    val password: String,
    val nearestStation: String,
    val address: String,
    val phone: String,
    val personalizedKey: String,
    val imageUrl: String,
    val openingTime: String,
    val closingTime: String
) {
    // Function to convert the object to a map for Firebase
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "hotelName" to hotelName,
            "email" to email,
            "password" to password,
            "nearestStation" to nearestStation,
            "address" to address,
            "phone" to phone,
            "personalizedKey" to personalizedKey,
            "imageUrl" to imageUrl,
            "openingTime" to openingTime,
            "closingTime" to closingTime
        )
    }
}

class HotelInfoBottomSheetFragment(
    private val hotelName: String,
    private val email: String,
    private val password: String
) : BottomSheetDialogFragment() {

    private lateinit var database: DatabaseReference
    private lateinit var selectedImageUri: Uri
    private lateinit var loadingLayout: View
    private lateinit var openingTimeEditText: EditText
    private lateinit var closingTimeEditText: EditText

    companion object {
        private const val IMAGE_PICK_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hotel_info_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().reference
        loadingLayout = view.findViewById(R.id.loadingLayout)

        val spinnerNearestStation = view.findViewById<Spinner>(R.id.spinnerNearestStation)
        val stationSuggestions = arrayOf("Hyderabad JN.", "Rohri JN.", "Multan Cantt", "Lahore JN.", "Rawalpindi")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, stationSuggestions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNearestStation.adapter = spinnerAdapter

        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val imageButton = view.findViewById<Button>(R.id.btnSelectImage)

        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
        }

        openingTimeEditText = view.findViewById(R.id.etOpeningTime)
        closingTimeEditText = view.findViewById(R.id.etClosingTime)

        openingTimeEditText.setOnClickListener {
            showTimePickerDialog(openingTimeEditText)
        }

        closingTimeEditText.setOnClickListener {
            showTimePickerDialog(closingTimeEditText)
        }

        btnSubmit.setOnClickListener {
            val nearestStationText = spinnerNearestStation.selectedItem.toString()
            val addressText = view.findViewById<EditText>(R.id.etAddress).text.toString().trim()
            val phoneText = view.findViewById<EditText>(R.id.etPhone).text.toString().trim()
            val openingTimeText = openingTimeEditText.text.toString()
            val closingTimeText = closingTimeEditText.text.toString()

            if (nearestStationText.isNotEmpty() && addressText.isNotEmpty() && phoneText.isNotEmpty()
                && openingTimeText.isNotEmpty() && closingTimeText.isNotEmpty()) {
                // Show loading indicator
                loadingLayout.isVisible = true

                // Upload the image and store the image URL
                uploadImage(nearestStationText, hotelName, email, password, addressText, phoneText, openingTimeText, closingTimeText)
            } else {
                // Handle empty fields
                Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.data!!
        }
    }

    private fun showTimePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                editText.setText(selectedTime)
            },
            hour,
            minute,
            DateFormat.is24HourFormat(requireContext())
        )
        timePickerDialog.show()
    }

    private fun uploadImage(
        nearestStationText: String,
        hotelName: String,
        email: String,
        password: String,
        addressText: String,
        phoneText: String,
        openingTimeText: String,
        closingTimeText: String
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${UUID.randomUUID()}")

        val uploadTask = imageRef.putFile(selectedImageUri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                throw task.exception!!
            }
            // Continue with the task to get the download URL
            imageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                val imageUrl = downloadUri.toString()
                Log.d("ImageUpload", "Image URL: $imageUrl")

                // Here you can continue with storing the imageUrl and hotelUser in the database
                val personalizedKey = generatePersonalizedKey(nearestStationText, hotelName)
                val hotelUser = Hotel_User(
                    hotelName, email, password,
                    nearestStationText, addressText, phoneText, personalizedKey, imageUrl, openingTimeText, closingTimeText
                )

                // Store hotelUser in the database
                val hotelKey = database.child("Hotels").push().key
                if (hotelKey != null) {
                    val hotelValues = hotelUser.toMap()
                    val childUpdates = hashMapOf<String, Any>(
                        "/Hotels/$hotelKey" to hotelValues
                    )

                    database.updateChildren(childUpdates).addOnSuccessListener {
                        // Dismiss the bottom sheet and hide loading indicator
                        dismiss()
                        loadingLayout.isVisible = false
                        Toast.makeText(requireContext(), "Data stored successfully", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { exception ->
                        // Handle failure and hide loading indicator
                        loadingLayout.isVisible = false
                        Toast.makeText(requireContext(), "Failed to store data: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // Handle failure to get hotelKey
                    loadingLayout.isVisible = false
                    Toast.makeText(requireContext(), "Failed to generate hotel key", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Handle image upload failure
                loadingLayout.isVisible = false
                Toast.makeText(requireContext(), "Failed to upload image", Toast.LENGTH_SHORT).show()
                Log.e("ImageUpload", "Failed to upload image: ${task.exception?.message}")

            }
        }
    }

    private fun generatePersonalizedKey(nearestStation: String, hotelName: String): String {
        val stationCode = nearestStation.take(3).toUpperCase()
        val hotelCode = hotelName.take(2).toUpperCase()
        val randomNumber = (100..999).random()
        return "$stationCode$hotelCode$randomNumber"
    }
}