package com.example.hotelvendor

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

data class HotelUser(
    val username: String,
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
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "username" to username,
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


class HotelInfoBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var database: DatabaseReference
    private lateinit var selectedImageUri: Uri
    private lateinit var loadingLayout: View
    private lateinit var openingTimeButton: Button
    private lateinit var closingTimeButton: Button
    private lateinit var username: String
    private lateinit var email: String
    private lateinit var password: String

    companion object {
        private const val IMAGE_PICK_REQUEST_CODE = 1001
        private const val ARG_USERNAME = "username"
        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"

        fun newInstance(username: String, email: String, password: String): HotelInfoBottomSheetFragment {
            val fragment = HotelInfoBottomSheetFragment()
            val args = Bundle()
            args.putString(ARG_USERNAME, username)
            args.putString(ARG_EMAIL, email)
            args.putString(ARG_PASSWORD, password)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_hotel_info_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve data from arguments
        username = arguments?.getString(ARG_USERNAME) ?: ""
        email = arguments?.getString(ARG_EMAIL) ?: ""
        password = arguments?.getString(ARG_PASSWORD) ?: ""

        // Initialize UI elements
        val spinnerNearestStation = view.findViewById<Spinner>(R.id.spinnerNearestStation)
        val addressEditText = view.findViewById<EditText>(R.id.etAddress)
        val phoneEditText = view.findViewById<EditText>(R.id.etPhone)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        val imageButton = view.findViewById<Button>(R.id.btnSelectImage)
        loadingLayout = view.findViewById(R.id.loadingLayout)
        openingTimeButton = view.findViewById(R.id.btnOpeningTime)
        closingTimeButton = view.findViewById(R.id.btnClosingTime)

        // Set hint for the spinner
        val spinnerHint = "Select nearest station"
        val spinnerArray = listOf(spinnerHint, "Hyderabad JN.", "Rohri JN.", "Multan Cantt", "Lahore JN.", "Rawalpindi")
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, spinnerArray)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerNearestStation.adapter = spinnerAdapter

        // Handle image selection
        // Handle image selection
        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_REQUEST_CODE)
        }

        // Handle opening time selection
        openingTimeButton.setOnClickListener {
            showTimePickerDialog(openingTimeButton)
        }

        // Handle closing time selection
        closingTimeButton.setOnClickListener {
            showTimePickerDialog(closingTimeButton)
        }

        // Handle form submission
        btnSubmit.setOnClickListener {
            val nearestStationText = spinnerNearestStation.selectedItem.toString()
            val addressText = addressEditText.text.toString().trim()
            val phoneText = phoneEditText.text.toString().trim()
            val openingTimeText = openingTimeButton.text.toString()
            val closingTimeText = closingTimeButton.text.toString()

            if (validateInput(nearestStationText, addressText, phoneText, openingTimeText, closingTimeText)) {
                // Show loading indicator
                loadingLayout.isVisible = true

                // Upload the image and store the image URL
                uploadImage(nearestStationText, addressText, phoneText, openingTimeText, closingTimeText)
            } else {
                // Handle empty or invalid fields
                Toast.makeText(requireContext(), "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == IMAGE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data!!
        }
    }

    private fun showTimePickerDialog(button: Button) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val selectedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                button.text = selectedTime
            },
            hour,
            minute,
            DateFormat.is24HourFormat(requireContext())
        )
        timePickerDialog.show()
    }

    private fun uploadImage(
        nearestStationText: String,
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
                // Here you can continue with storing the imageUrl and hotelUser in the database
                val personalizedKey = generatePersonalizedKey(nearestStationText, addressText)
                val hotelUser = HotelUser(
                    username, email, password,
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
            }
        }
    }

    private fun generatePersonalizedKey(nearestStation: String, address: String): String {
        val stationCode = nearestStation.take(3).toUpperCase()
        val addressCode = address.take(2).toUpperCase()
        val randomNumber = (100..999).random()
        return "$stationCode$addressCode$randomNumber"
    }

    private fun validateInput(
        nearestStationText: String,
        addressText: String,
        phoneText: String,
        openingTimeText: String,
        closingTimeText: String
    ): Boolean {
        return nearestStationText.isNotEmpty() &&
                addressText.isNotEmpty() &&
                phoneText.isNotEmpty() &&
                openingTimeText.isNotEmpty() &&
                closingTimeText.isNotEmpty()
    }
}
