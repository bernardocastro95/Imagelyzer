package com.example.imagelyzer

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
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
    private lateinit var binding: ActivityMainBinding

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

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) launchCamera() else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPickImage.setOnClickListener { galleryLauncher.launch("image/*") }

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

    private fun launchCamera() {
        val photoFile = File.createTempFile(
            "photo_", ".jpg",
            cacheDir.also { File(it, "images").mkdirs() }.let { File(it, "images") }
        )
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", photoFile)
        cameraPhotoUri = uri
        cameraLauncher.launch(uri)
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
    
    private fun runAnalysis(){
        val bitmap = currentBitmap ?: return
        val apiKey = SettingsActivity.getSavedApiKey(this)

        if(apiKey.isBlank()){
            Toast.makeText(this, "Please add your API key in Settings first", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        setLoading(true)
        hideResults()

        scope.launch {
            try {
                val result = NetworkService.analyzeImage(apiKey, bitmap)
                showResults(result)
            } catch (e: Exception) {
                showError(e.message ?: "Something went wrong while analyzing the image")
            } finally {
                setLoading(false)
            }
        }

    }

    private fun setLoading(loading: Boolean){
        binding.progressBar.visibility = if(loading) View.VISIBLE else View.GONE
        binding.btnAnalyse.isEnabled = !loading
    }

    private fun showResults(result: AnalysisResult){
        binding.errorText.visibility = View.GONE

        binding.textLocation.text = result.location
        binding.cardLocation.visibility = View.VISIBLE

        binding.textHistory.text = result.history
        binding.cardHistory.visibility = View.VISIBLE

        binding.curiositiesContainer.removeAllViews()
        result.curiosities.forEachIndexed { index, fact ->
            val textView = TextView(this)
            textView.text = "${index + 1}. $fact"
            textView.setTextColor(getColor(R.color.text_primary))
            textView.setPadding(0, 8, 0, 8)
            binding.curiositiesContainer.addView(textView)
        }
        binding.cardCuriosities.visibility = View.VISIBLE

    }

    private fun hideResults(){
        binding.cardLocation.visibility = View.GONE
        binding.cardHistory.visibility = View.GONE
        binding.cardCuriosities.visibility = View.GONE
    }
    private fun showError(message: String){
        binding.errorText.text = message
        binding.errorText.visibility = View.VISIBLE
    }



}