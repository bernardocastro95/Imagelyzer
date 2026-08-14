package com.example.imagelyzer

import android.graphics.Bitmap
import android.util.Base64
import com.example.imagelyzer.AnalysisResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit



//This file establishes a connection between AI and app

object NetworkService {
    private const val API_URL = "https://api.anthropic.com/v1/messages"
    private const val MODEL = "claude-sonnet-4-6"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun bitmapToBase64(bitmap: Bitmap): String{
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    suspend fun analyzeImage(apiKey: String, bitmap: Bitmap): AnalysisResult = withContext(Dispatchers.IO){
        val base64Image = bitmapToBase64(bitmap)
        val promptText = """
            Look at this image and identify the place, landmark, or object shown.
            Respond with ONLY a raw JSON object (no markdown, no code fences, no extra text) in exactly this shape:
            {
              "location": "Best guess of where this is / what it depicts, with city and country if identifiable",
              "history": "A brief 3-5 sentence history of this place or object",
              "curiosities": ["fact 1", "fact 2", "fact 3", "fact 4", "fact 5", "fact 6", "fact 7", "fact 8", "fact 9", "fact 10"]
            }
            The "curiosities" array must contain exactly 10 short, interesting, and accurate facts.
            If you cannot confidently identify the location, make your best educated guess and say so within the "location" field.
        """.trimIndent()

        val contentArray = JSONArray()
            contentArray.put(
                JSONObject()
                    .put("type", "image")
                    .put(
                        "source", JSONObject()
                            .put("type", "base64")
                            .put("media_type", "image/jpeg")
                            .put("date", base64Image)
                    )
            )
            contentArray.put(
                JSONObject()
                    .put("type", "text")
                    .put("text", promptText)
            )

        val messageArray = JSONArray()
            messageArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("content", contentArray)
            )

        val requestBodyJson = JSONObject()
            .put("model", MODEL)
            .put("max_tokens", 1024)
            .put("content", contentArray)

        val body = requestBodyJson.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMessage = try {
                    JSONObject(responseBody).optJSONObject("error")?.optString("message")
                } catch (e: Exception) {
                    null
                }
                throw Exception(errorMessage ?: "Request failed with code ${response.code}")
            }

            parseClaudeResponse(responseBody)
        }
    }
    private fun parseClaudeResponse(responseBody: String): AnalysisResult {
        val root = JSONObject(responseBody)
        val contentArray = root.getJSONArray("content")

        var rawText = ""
        for (i in 0 until contentArray.length()) {
            val block = contentArray.getJSONObject(i)
            if (block.optString("type") == "text") {
                rawText = block.getString("text")
                break
            }
        }

        val cleaned = rawText.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleaned)
        val curiositiesJsonArray = json.getJSONArray("curiosities")
        val curiosities = mutableListOf<String>()
        for (i in 0 until curiositiesJsonArray.length()) {
            curiosities.add(curiositiesJsonArray.getString(i))
        }

        return AnalysisResult(
            location = json.getString("location"),
            history = json.getString("history"),
            curiosities = curiosities
        )
    }


}
