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
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale


class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var languageSpinner: Spinner
    private lateinit var sayPhraseButton: Button
    private lateinit var phraseEditText: EditText

    private val languages = listOf("Spanish", "French", "Chinese") // Add more languages as needed
    private val vacationSpots = mapOf(
        "Spanish" to "Mexico City",
        "French" to "Paris",
        "Chinese" to "Beijing"
        // Add more vacation spots and languages as needed
    )

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lastShakeTime: Long = 0

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
            "Spanish" -> "es"
            "French" -> "fr"
            "Chinese" -> "zh"
            // Add more languages and their codes as needed
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
        event?.let {
            if (event.sensor == accelerometer) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastShakeTime > SHAKE_INTERVAL) {
                    lastShakeTime = currentTime
                    // Determine the chosen language
                    val selectedLanguage = languageSpinner.selectedItem.toString()
                    // Get the corresponding vacation spot
                    val vacationSpot = vacationSpots[selectedLanguage]
                    vacationSpot?.let { spot ->
                        // Construct the geo URI
                        val geoUri = "geo:0,0?q=$spot"
                        // Launch Google Maps with the geo URI
                        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                        mapIntent.setPackage("com.google.android.apps.maps")
                        startActivity(mapIntent)
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do nothing
    }

    companion object {
        private const val REQUEST_CODE_SPEECH_INPUT = 100
        private const val SHAKE_INTERVAL = 1000 // Minimum time between two shakes in milliseconds
    }
}