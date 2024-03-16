package com.bignerdranch.android.springbreak


import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    private lateinit var languageSpinner: Spinner
    private lateinit var sayPhraseButton: Button
    private lateinit var phraseEditText: EditText

    private val languages = listOf("Spanish", "French", "Chinese") // Add more languages as needed

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
    }

    private fun promptSpeechInput(language: String) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
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
            else -> "en" // Default to English
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

    companion object {
        private const val REQUEST_CODE_SPEECH_INPUT = 100
    }
}