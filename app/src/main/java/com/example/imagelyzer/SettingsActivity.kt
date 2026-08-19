package com.example.imagelyzer

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.imagelyzer.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "image_location_ai_prefs"
        private const val KEY_API_KEY = "api_key"

        fun getSavedApiKey(context: Context): String {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(KEY_API_KEY, "") ?: ""
        }
    }

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editApiKey.setText(getSavedApiKey(this))

        binding.btnSaveKey.setOnClickListener {
            val key = binding.editApiKey.text.toString().trim()
            getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_API_KEY, key)
                .apply()
            Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}