package com.bignerdranch.android.springbreak


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale
import kotlin.math.abs


class MainActivity : AppCompatActivity(), SensorEventListener, OnInitListener {

    private lateinit var languageSpinner: Spinner
    private lateinit var sayPhraseButton: Button
    private lateinit var phraseEditText: EditText

    private val languages = listOf("English", "Spanish", "French", "Chinese")
    private val vacationSpots = mapOf(
        "English" to "Boston",
        "Spanish" to "Mexico City",
        "French" to "Paris",
        "Chinese" to "Beijing"
    )

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastUpdate: Long = 0
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f

    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find views by their IDs
        languageSpinner = findViewById(R.id.languageSpinner)
        sayPhraseButton = findViewById(R.id.sayPhraseButton)
        phraseEditText = findViewById(R.id.phraseEditText)

        // Setup Spinner with languages
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter

        sayPhraseButton.setOnClickListener {
            val selectedLanguage = languageSpinner.selectedItem.toString()
            promptSpeechInput(selectedLanguage)
        }

        // Initialize sensor manager and accelerometer sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Initialize TextToSpeech
        tts = TextToSpeech(this, this)
    }

    private fun promptSpeechInput(language: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLanguageCode(language))
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a phrase in $language")
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLanguageCode(language: String): String {
        return when (language) {
            "English" -> "en"
            "Spanish" -> "es"
            "French" -> "fr"
            "Chinese" -> "zh"
            else -> Locale.getDefault().language // Default to the device's default language
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = result?.get(0) ?: ""
            phraseEditText.setText(spokenText)
        }
    }

    override fun onResume() {
        super.onResume()
        // Register accelerometer sensor listener
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        // Unregister accelerometer sensor listener to prevent battery drain
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor == accelerometer) {
            val curTime = System.currentTimeMillis()
            if (curTime - lastUpdate > SHAKE_INTERVAL) {
                val diffTime = curTime - lastUpdate
                lastUpdate = curTime

                val x = event?.values?.get(0)
                val y = event?.values?.get(1)
                val z = event?.values?.get(2)

                val speed =
                    abs((x?.plus(y!!) ?: z!!) - lastX - lastY - lastZ) / diffTime * 10000

                if (speed > SHAKE_THRESHOLD) {
                    // Shake detected, open vacation spot on maps
                    val selectedLanguage = languageSpinner.selectedItem.toString()
                    openVacationSpot(selectedLanguage)
                }

                if (x != null) {
                    lastX = x
                }
                if (y != null) {
                    lastY = y
                }
                if (z != null) {
                    lastZ = z
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    private fun openVacationSpot(language: String) {
        val vacationSpot = vacationSpots[language]
        vacationSpot?.let { spot ->
            // Play greeting in the selected language
            val selectedLanguage = languageSpinner.selectedItem.toString()
            val greeting = getGreeting(selectedLanguage)
            tts.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, null)

            // Open vacation spot on maps
            val geoUri = "geo:0,0?q=$spot"
            val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
            mapIntent.setPackage("com.google.android.apps.maps")
            startActivity(mapIntent)
        }
    }

    private fun getGreeting(language: String): String {
        return when (language) {
            "English" -> "Hello"
            "Spanish" -> "Hola"
            "French" -> "Bonjour"
            "Chinese" -> "你好"
            else -> "Hello" // Default to English greeting
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(
                    applicationContext,
                    "Language not supported",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(
                applicationContext,
                "Initialization failed",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    companion object {
        private const val REQUEST_CODE_SPEECH_INPUT = 100
        private const val SHAKE_INTERVAL = 100 // Minimum time between two shakes in milliseconds
        private const val SHAKE_THRESHOLD = 800 // Minimum acceleration change to consider shake
    }
}