package com.android.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage


@Suppress("DUPLICATE_LABEL_IN_WHEN")
class MainActivity : AppCompatActivity() {

    private lateinit var camerBtn: MaterialButton
    private lateinit var galleryBtn: MaterialButton
    private lateinit var imageIv: ImageView
    private lateinit var scanBtn: MaterialButton
    private lateinit var resultTv: TextView
    private lateinit var Datatv: TextView
    private lateinit var finddataBtn: MaterialButton
    private lateinit var database: DatabaseReference
//    public lateinit var rawValue: string


    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 100
        private const val TAG = "MAIN_TAG"
    }

    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePerssion: Array<String>

    private var imageUri: Uri? = null

    private var barcodeScannerOptions: BarcodeScannerOptions? = null
    private var barcodeScanner: BarcodeScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        camerBtn = findViewById(R.id.camerBtn)
        galleryBtn = findViewById(R.id.galleryBtn)
        imageIv = findViewById(R.id.imageIv)
        resultTv = findViewById(R.id.resultTv)
        scanBtn = findViewById(R.id.scanBtn)
        Datatv = findViewById(R.id.DataTv)
        finddataBtn = findViewById(R.id.finddataBtn)

        database = Firebase.database.reference

        cameraPermission = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        storagePerssion = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)


        barcodeScannerOptions = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()

        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions!!)


        camerBtn.setOnClickListener {

            if (checkCameraPermission()) {
                pickImageCamera()
            } else {
                requestStoragePermission()
            }
        }

        galleryBtn.setOnClickListener {

            pickImageGallery()

        }

        scanBtn.setOnClickListener {
            if (imageUri == null) {
                showToast("Pick image first")
            } else {
                detectResultFromImage()
            }

        }
        finddataBtn.setOnClickListener {
            if (imageUri == null){
                showToast("Pick image first")
            } else {
                Toast.makeText(baseContext,"ejf",Toast.LENGTH_LONG).show()
                detectDataFromRawValue()


            }
        }
    }

    private fun detectDataFromRawValue() {
        database.child("sumit").setValue(mapOf(
            "1" to "1"
        ))
        Toast.makeText(baseContext,"hello",Toast.LENGTH_SHORT).show()
        database.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawValue = snapshot.value
                rawValue?.let {
                    Log.d("Firebase", "Raw Value: $it")
                    Datatv.text = "Raw Value: ${it.toString()}"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("Firebase", "Error reading raw value", databaseError.toException())
            }
        })
    }

    private fun detectResultFromImage() {
        Log.d(TAG, "detectResultFromImage: ")
        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)

            barcodeScanner!!.process(inputImage)
                .addOnSuccessListener { barcodes ->

                    extractBarcodeQrCodeInfo(barcodes)
                }
                .addOnFailureListener { e ->
                    Log.d(TAG, "detectResultFromImage: ", e)
                    showToast("Failed scanning due to ${e.message}")
                }
        } catch (e: Exception) {
            Log.d(TAG, "detectResultFromImage: ", e)
            showToast("Failed due to ${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun extractBarcodeQrCodeInfo(barcodes: List<Barcode>) {

        for (barcode in barcodes) {
            barcode.boundingBox
            barcode.cornerPoints
            val rawValue = barcode.rawValue
            Log.d(TAG, "extractBarcodeQrCodeInfo: rawValue $rawValue")

            val valueType = barcode.valueType
            when (valueType) {
                Barcode.TYPE_WIFI -> {

                    val typeWiFi = barcode.wifi
                    val ssid = "${typeWiFi?.ssid}"
                    val password = "${typeWiFi?.password}"
                    var encryptionType = "${typeWiFi?.encryptionType}"

                    if (encryptionType == "1") {
                        encryptionType = "open"
                    } else if (encryptionType == "2") {
                        encryptionType == "WPA"
                    } else if (encryptionType == "3") {
                        encryptionType == "WEP"
                    }
                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_WIFI")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: ssid: $ssid")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: password: $password")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: encryptionType: $encryptionType")

                    resultTv.text =
                        "TYPE_WIFI\n ssid:$ssid\n password:$password\n encryptionType: $encryptionType\n rawValue:$rawValue" +
                                ""
                }

                Barcode.TYPE_URL -> {
                    val typeUrl = barcode.url
                    val title = "${typeUrl?.title}"
                    val url = "${typeUrl?.url}"
                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_URL")

                    Log.d(TAG, "extractBarcodeQrCodeInfo: title: $title")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: url: $url")

                    resultTv.text = "TYPE_URL \ntitle: $title\n url: $url\n rawValue: $rawValue "
                }

                Barcode.TYPE_EMAIL -> {
                    val typeEmail = barcode.email

                    val address = "${typeEmail?.address}"

                    val body = "${typeEmail?.body}"
                    val subject = "${typeEmail?.subject}"

                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_EMAIL")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: address: $address")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: body: $body")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: subject: $subject")

                    resultTv.text = "TYPE_EMAIL \nEMAIL: $address \nbody: $body \n subject: $subject \n rawValue: $rawValue"
                }
                Barcode.TYPE_CONTACT_INFO -> {
                    val typeContact = barcode.contactInfo

                    val title = "${typeContact?.title}"
                    val organization = "${typeContact?.organization}"
                    val name = "${typeContact?.name?.first} ${typeContact?.name?.last}"
                    val phone = "${typeContact?.name?.first} ${typeContact?.phones?.get(0)?.number}"

                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_CONTACT_INFO")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: $title")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: organization: $organization")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: name: $name")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: phone: $phone")
                    resultTv.text ="TYPE_CONTACT_INFO \n title: $title \n organization: $organization \n name: $name \n phone: $phone \n rawValue: $rawValue"
                }
                else -> {
                    resultTv.text = "rawValue: $rawValue"
                }
            }
        }
    }
    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)

        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            imageUri = data?.data
            Log.d(TAG, "galleryActivityResultLauncher: imageUri: $imageUri")

            imageIv.setImageURI(imageUri)
        } else {
            showToast("Cancelled.....!")
        }
    }

    private fun pickImageCamera() {

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Tmage")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample Tmage Description")


        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }


    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {

            result.data

            Log.d(TAG, "cameraActivityResultLauncher: imageUri: $imageUri")

            imageIv.setImageURI(imageUri)
        }
    }

    private fun checkStoragePermission(): Boolean {

        val result = (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
        return result
    }

    private fun requestStoragePermission() {

        ActivityCompat.requestPermissions(this, storagePerssion, STORAGE_REQUEST_CODE)
    }

    private fun checkCameraPermission(): Boolean {

        val resultCamera = (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED)
        val resultStorage = (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)

        return resultCamera && resultStorage
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_REQUEST_CODE -> {

                if (grantResults.isNotEmpty()) {

                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && storageAccepted) {

                        pickImageCamera()
                    } else {

                        showToast("Camera & Storage permission are required")
                    }
                }
            }
            STORAGE_REQUEST_CODE -> {

                if (grantResults.isNotEmpty()) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (storageAccepted) {
                        pickImageGallery()
                    } else  {
                        showToast("Storage permission is required..")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}