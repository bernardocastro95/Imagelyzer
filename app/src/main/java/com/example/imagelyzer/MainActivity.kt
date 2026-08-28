package com.example.imagelyzer

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.example.imagelyzer.databinding.ActivityMainBinding
import com.example.imagelyzer.AnalysisResult
import kotlinx.coroutines.launch
import kotlinx.coroutines.MainScope
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateint var binding: ActivityMainBinding

    private val scope = MainScope()

    private var currentBitmap: Bitmap? = null

    private var cameraPhotoUri: Uri? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { loadBitmapFromUri(it) }
    }


    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            cameraPhotoUri?.let { loadBitmapFromUri(it) }
        }
    }



    private val cameraLaunch = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {granted: Boolean ->
        if(granted) launchCamera() else Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickImage.setOnClickListener { galleryLaucher.launch("image/*") }

        binding.btnTakePhoto.setOnClickListener { requestCameraAndLaunch() }

        binding.btnAnalyse.setOnClickListener { runAnalysis() }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun requestCameraAndLaunch() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera(){
        val photoFile = File.createTempFile("photo_", ".jpg", cacheDir.also{File(it, "images").mkdirs()}.let {File(it, "images")})
        cameraPhotoUri = FileProvider.getUriForFile((this), "$packageName.fileprovider", photoFile)
        cameraLauncher.launch(cameraPhotoUri)
    }

    private fun loadBitmapFromUri(uri: Uri){
        val bitmap = contentResolver.openInputStream(uri)?.use {
            android.graphics.BitmapFactory.decodeStream(it)
        } ?: return

        currentBitmap = bitmap
        binding.imagePreview.setImageBitmap(bitmap)
        binding.placeholderText.visibility = View.GONE
        binding.btnAnalyse.isEnabled = true

        hideResults()
        binding.errorText.visibility = View.GONE
    }

}