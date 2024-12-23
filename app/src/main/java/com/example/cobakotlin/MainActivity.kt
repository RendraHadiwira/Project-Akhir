package com.example.cobakotlin

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*

class MainActivity : AppCompatActivity() {

    private val RESULT_SPEECH = 1
    private lateinit var tvVoice: TextView
    private lateinit var tvChat: TextView
    private lateinit var btnVoice: Button
    private val ID_BahasaIndonesia = "id"
    private var textToSpeech: TextToSpeech? = null
    private lateinit var requestQueue: RequestQueue

    private val API_URL = "https://api.openai.com/v1/chat/completions"
    private val API_KEY = "Bearer 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"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvChat = findViewById(R.id.tvChat)
        tvVoice = findViewById(R.id.tvVoice)
        btnVoice = findViewById(R.id.btnVoice)
        requestQueue = Volley.newRequestQueue(this)

        btnVoice.setOnClickListener {
            val micGoogle = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, ID_BahasaIndonesia)
            }

            try {
                startActivityForResult(micGoogle, RESULT_SPEECH)
                tvVoice.text = ""
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(applicationContext, "Maaf, Device Kamu Tidak Support Speech To Text", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RESULT_SPEECH && resultCode == RESULT_OK && data != null) {
            val text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val userInput = text?.get(0) ?: ""
            tvVoice.text = userInput

            sendToChatGPT(userInput)
        }
    }

    // Fungsi untuk mengirimkan input ke ChatGPT API
    private fun sendToChatGPT(userInput: String) {
        try {
            val requestBody = JSONObject().apply {
                put("model", "gpt-3.5-turbo")
                val messages = JSONArray().apply {
                    val userMessage = JSONObject().apply {
                        put("role", "user")
                        put("content", userInput)
                    }
                    put(userMessage)
                }
                put("messages", messages)
            }

            val jsonObjectRequest = object : JsonObjectRequest(
                Request.Method.POST,
                API_URL,
                requestBody,
                Response.Listener { response ->
                    try {
                        Log.d("API Response", response.toString())

                        val choices = response.getJSONArray("choices")
                        val chatGPTResponse = choices.getJSONObject(0).getJSONObject("message").getString("content")

                        Toast.makeText(this@MainActivity, chatGPTResponse, Toast.LENGTH_SHORT).show()

                        tvChat.text = chatGPTResponse

                        // Konversi teks ke suara
                        speak(chatGPTResponse)

                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Toast.makeText(this@MainActivity, "Error parsing response", Toast.LENGTH_SHORT).show()
                    }
                },
                Response.ErrorListener { error ->
                    error.printStackTrace()
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            ) {
                override fun getHeaders(): MutableMap<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Authorization"] = API_KEY
                    headers["Content-Type"] = "application/json"
                    return headers
                }
            }

            requestQueue.add(jsonObjectRequest)

        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi untuk mengonversi teks ke suara
    private fun speak(text: String) {
        if (textToSpeech == null) {
            textToSpeech = TextToSpeech(this) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    textToSpeech?.language = Locale.getDefault()
                }
            }
        }
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onDestroy() {
        textToSpeech?.apply {
            stop()
            shutdown()
        }
        super.onDestroy()
    }
}