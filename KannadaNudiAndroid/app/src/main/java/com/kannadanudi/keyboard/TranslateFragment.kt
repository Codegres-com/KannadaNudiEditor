package com.kannadanudi.keyboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslateFragment : Fragment() {

    private lateinit var spinnerSourceLang: Spinner
    private lateinit var etInputText: EditText
    private lateinit var etOutputText: EditText
    private lateinit var btnTranslate: Button
    private lateinit var btnCopyTranslation: Button

    // Language display names paired with their BCP-47 language codes (ML Kit compatible)
    // Note: Malayalam (ml) and Punjabi (pa) are not supported by ML Kit Translate offline
    private val languages = listOf(
        "English"       to "en",
        "Hindi"         to "hi",
        "Tamil"         to "ta",
        "Telugu"        to "te",
        "Marathi"       to "mr",
        "Gujarati"      to "gu",
        "Bengali"       to "bn",
        "Urdu"          to "ur",
        "French"        to "fr",
        "German"        to "de",
        "Spanish"       to "es",
        "Arabic"        to "ar",
        "Chinese"       to "zh",
        "Japanese"      to "ja",
        "Russian"       to "ru",
        "Portuguese"    to "pt",
        "Italian"       to "it"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_translate, container, false)

        spinnerSourceLang = view.findViewById(R.id.spinnerSourceLang)
        etInputText = view.findViewById(R.id.etInputText)
        etOutputText = view.findViewById(R.id.etOutputText)
        btnTranslate = view.findViewById(R.id.btnTranslate)
        btnCopyTranslation = view.findViewById(R.id.btnCopyTranslation)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languages.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSourceLang.adapter = adapter

        btnTranslate.setOnClickListener {
            val inputText = etInputText.text.toString().trim()
            if (inputText.isEmpty()) {
                Toast.makeText(context, "Please enter text to translate", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val sourceLangCode = languages[spinnerSourceLang.selectedItemPosition].second
            performTranslation(inputText, sourceLangCode)
        }

        preDownloadModels()

        btnCopyTranslation.setOnClickListener {
            val text = etOutputText.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Kannada Translation", text)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No translation to copy", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    // Silently pre-download the Kannada model (target) and the default source language
    // so translation is ready immediately when the user needs it.
    private fun preDownloadModels() {
        val conditions = DownloadConditions.Builder().build()
        val modelManager = RemoteModelManager.getInstance()

        listOf(TranslateLanguage.KANNADA, languages[0].second).forEach { langCode ->
            val model = TranslateRemoteModel.Builder(langCode).build()
            modelManager.isModelDownloaded(model)
                .addOnSuccessListener { downloaded ->
                    if (!downloaded) {
                        modelManager.download(model, conditions)
                    }
                }
        }
    }

    private fun performTranslation(text: String, sourceLangCode: String) {
        btnTranslate.isEnabled = false
        btnTranslate.text = "Downloading model..."
        etOutputText.setText("")

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLangCode)
            .setTargetLanguage(TranslateLanguage.KANNADA)
            .build()
        val translator = Translation.getClient(options)

        // Download language models if not already on device (requires internet first time only)
        val conditions = DownloadConditions.Builder().build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                if (!isAdded) { translator.close(); return@addOnSuccessListener }
                btnTranslate.text = "Translating..."
                translator.translate(text)
                    .addOnSuccessListener { translatedText ->
                        if (isAdded) {
                            etOutputText.setText(translatedText)
                            btnTranslate.isEnabled = true
                            btnTranslate.text = "Translate"
                        }
                        translator.close()
                    }
                    .addOnFailureListener {
                        if (isAdded) {
                            btnTranslate.isEnabled = true
                            btnTranslate.text = "Translate"
                            Toast.makeText(context, "Translation failed. Please try again.", Toast.LENGTH_LONG).show()
                        }
                        translator.close()
                    }
            }
            .addOnFailureListener {
                if (isAdded) {
                    btnTranslate.isEnabled = true
                    btnTranslate.text = "Translate"
                    Toast.makeText(
                        context,
                        "Model download failed. Internet is required the first time to download the offline model.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                translator.close()
            }
    }
}
