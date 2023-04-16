package com.dylan.ivy2

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

const val PERMISSION_REQUEST_NETWORK = 0
const val PERMISSION_REQUEST_BLUETOOTH = 1

data class Permission(val name: String, val display: String, var isGranted: Boolean = false)

class MainActivity : AppCompatActivity() {
    private lateinit var imageInputView: ImageView
    private lateinit var imageInputLayout: ConstraintLayout
    private lateinit var imageButtonLayout: ConstraintLayout

    private var imageInput: Bitmap? = null

    private val permissionsMap = mapOf(
        PERMISSION_REQUEST_NETWORK to Permission(
            "android.permission.ACCESS_NETWORK_STATE",
            "Network"
        ),
        PERMISSION_REQUEST_BLUETOOTH to Permission("android.permission.BLUETOOTH", "Bluetooth"),
    )

    private val activityImageIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Get the image bitmap
                val selectedImage = if (Build.VERSION.SDK_INT >= 33) {
                    result.data?.getParcelableExtra("data", Bitmap::class.java)
                } else {
                    result.data?.getParcelableExtra("data")
                }

                updateInputImage(selectedImage)
//            val root = Environment.getExternalStorageDirectory().absolutePath
//            val imgFile = File(root, "${System.currentTimeMillis()}.jpg")
//            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(imgFile))
            }
        }

    private val activityGalleryIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Get the image path
                val imagePath = result.data?.dataString

                if(imagePath != null) {
                    val path = Uri.parse(result.data?.dataString)
                    val inputStream = contentResolver.openInputStream(path)

                    val selectedImage = BitmapFactory.decodeStream(inputStream)

                    updateInputImage(selectedImage)
                }
            }
        }

    private val cameraClickListener = View.OnClickListener {
        activityImageIntent.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
    }

    private val browseClickListener = View.OnClickListener {
        activityGalleryIntent.launch(
            Intent(
                Intent.ACTION_GET_CONTENT,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
        )
    }

    private val removeClickListener = View.OnClickListener {
        updateInputImage(null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add camera button click listener
        val cameraButton: Button = findViewById(R.id.open_camera_button)
        cameraButton.setOnClickListener(cameraClickListener)

        // Add browse files button click listener
        val browseButton: Button = findViewById(R.id.browse_files_button)
        browseButton.setOnClickListener(browseClickListener)

        // Add remove image button click listener
        val removeButton: Button = findViewById(R.id.remove_image_button)
        removeButton.setOnClickListener(removeClickListener)

        // Gather view references
        imageInputView = findViewById(R.id.print_image_input)
        imageInputLayout = findViewById(R.id.image_input_layout)
        imageButtonLayout = findViewById(R.id.image_button_layout)

        // Set initial visibility
        imageInputLayout.visibility = View.INVISIBLE
        imageButtonLayout.visibility = View.VISIBLE

        // Check and request permissions
        checkPermissions()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionsMap.containsKey(requestCode)) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                permissionsMap[requestCode]?.isGranted = true
            } else {
                val display = permissionsMap[requestCode]?.name
                Toast.makeText(this, "$display permission not granted", Toast.LENGTH_SHORT).show()
            }
            return
        }
    }

    private fun updateInputImage(image: Bitmap?) {
        // Update the input image view
        imageInputView.setImageBitmap(image)
        imageInput = image

        // Toggle visibility of the image input layouts
        if (image != null) {
            imageInputLayout.visibility = View.VISIBLE
            imageButtonLayout.visibility = View.INVISIBLE
        } else {
            imageInputLayout.visibility = View.INVISIBLE
            imageButtonLayout.visibility = View.VISIBLE
        }
    }

    private fun checkPermissions() {
        for ((requestCode, permission) in permissionsMap) {
            val currentStatus = ContextCompat.checkSelfPermission(this, permission.name)
            if (currentStatus == PackageManager.PERMISSION_GRANTED) {
                permissionsMap[requestCode]?.isGranted = true
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(permission.name), requestCode)
            }
        }
    }
}