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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class TranslateFragment : Fragment() {

    private lateinit var spinnerSourceLang: Spinner
    private lateinit var etInputText: EditText
    private lateinit var etOutputText: EditText
    private lateinit var btnTranslate: Button
    private lateinit var btnCopyTranslation: Button

    // Language display names paired with their MyMemory language codes
    private val languages = listOf(
        "Auto Detect"   to "autodetect",
        "English"       to "en",
        "Hindi"         to "hi",
        "Tamil"         to "ta",
        "Telugu"        to "te",
        "Malayalam"     to "ml",
        "Marathi"       to "mr",
        "Gujarati"      to "gu",
        "Bengali"       to "bn",
        "Punjabi"       to "pa",
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

        val languageNames = languages.map { it.first }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, languageNames)
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

    private fun performTranslation(text: String, sourceLangCode: String) {
        btnTranslate.isEnabled = false
        btnTranslate.text = "Translating..."
        etOutputText.setText("")

        viewLifecycleOwner.lifecycleScope.launch {
            val result = translateText(text, sourceLangCode)
            btnTranslate.isEnabled = true
            btnTranslate.text = "Translate"
            if (result != null) {
                etOutputText.setText(result)
            } else {
                Toast.makeText(context, "Translation failed. Please check your internet connection.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun translateText(text: String, sourceLangCode: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(text, "UTF-8")
                val langPair = "$sourceLangCode|kn"
                val urlString = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPair"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("User-Agent", "KannadaNudiApp/1.0")

                try {
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                        val json = JSONObject(response)
                        val status = json.getInt("responseStatus")
                        if (status == 200) {
                            json.getJSONObject("responseData").getString("translatedText")
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
