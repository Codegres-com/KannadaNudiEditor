package com.kannadanudi.keyboard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.kannadanudi.keyboard.prediction.DictionaryLoader
import com.kannadanudi.keyboard.ui.CandidatesAdapter
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.ArrayList

class KannadaIME : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var nudiKeyboard: Keyboard
    private lateinit var nudiShiftedKeyboard: Keyboard
    private lateinit var numberpadKeyboard: Keyboard
    private var isCaps = false
    private val transliterationEngine = TransliterationEngine()
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private lateinit var dictionaryLoader: DictionaryLoader
    private lateinit var candidatesAdapter: CandidatesAdapter
    private lateinit var candidatesView: RecyclerView

    private val mainScope = MainScope()

    override fun onCreate() {
        super.onCreate()
        dictionaryLoader = DictionaryLoader(this)
        dictionaryLoader.loadDictionary()
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    isListening = false
                }

                override fun onError(error: Int) {
                    isListening = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                        else -> "Error: $error"
                    }
                    Toast.makeText(this@KannadaIME, message, Toast.LENGTH_SHORT).show()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        currentInputConnection.commitText(text, 1)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } catch (e: Exception) {
            // Speech recognition not supported or initialization failed
            speechRecognizer = null
        }
    }

    override fun onCreateInputView(): View {
        val rootView = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = rootView.findViewById(R.id.keyboard)
        candidatesView = rootView.findViewById(R.id.rv_candidates)
        


        qwertyKeyboard = Keyboard(this, R.xml.qwerty)
        nudiKeyboard = Keyboard(this, R.xml.nudi_layout)
        nudiShiftedKeyboard = Keyboard(this, R.xml.nudi_layout_shifted)
        numberpadKeyboard = Keyboard(this, R.xml.numberpad_layout)

        val switchButton = rootView.findViewById<TextView>(R.id.tv_switch_keyboard)
        switchButton.setOnClickListener {
            val imeManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imeManager.showInputMethodPicker()
        }

        // Setup Candidates View
        candidatesAdapter = CandidatesAdapter { word ->
            pickCandidate(word)
        }
        candidatesView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        candidatesView.adapter = candidatesAdapter

        // Default to Nudi
        keyboardView.keyboard = nudiKeyboard
        keyboardView.setOnKeyboardActionListener(this)

        return rootView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        transliterationEngine.clearBuffer()
        // Default to Nudi (Direct)
        transliterationEngine.setLayout(KeyboardLayout.Nudi)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)

        keyboardView.visibility = View.VISIBLE
        keyboardView.keyboard = nudiKeyboard
        keyboardView.invalidateAllKeys()
        updateCandidates()
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val inputConnection = currentInputConnection ?: return

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                handleBackspace(inputConnection)
            }
            Keyboard.KEYCODE_SHIFT -> {
                isCaps = !isCaps
                if (keyboardView.keyboard == qwertyKeyboard || keyboardView.keyboard == numberpadKeyboard) {
                    keyboardView.isShifted = isCaps
                } else {
                    keyboardView.keyboard = if (isCaps) nudiShiftedKeyboard else nudiKeyboard
                }
                keyboardView.invalidateAllKeys()
            }
            Keyboard.KEYCODE_DONE -> {
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            10 -> { // Custom Enter Key (Numberpad)
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                inputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            -102 -> { // MIC Code
                checkAudioPermissionAndListen()
            }
            -200 -> { // Switch to Nudi
                isCaps = false
                transliterationEngine.setLayout(KeyboardLayout.Nudi)
                keyboardView.keyboard = nudiKeyboard
                keyboardView.invalidateAllKeys()
            }
            -201 -> { // Switch to Qwerty (English)
                isCaps = false
                keyboardView.isShifted = false
                transliterationEngine.setLayout(KeyboardLayout.English)
                keyboardView.keyboard = qwertyKeyboard
                keyboardView.invalidateAllKeys()
            }
            -2 -> { // Switch to Numberpad
                keyboardView.keyboard = numberpadKeyboard
                keyboardView.invalidateAllKeys()
            }
            else -> {
                var code = primaryCode.toChar()
                if (Character.isLetter(code) && isCaps) {
                    code = Character.toUpperCase(code)
                }

                // Process through Transliteration Engine
                val keyString = code.toString()

                // Get context for Matra handling
                val textBefore = inputConnection.getTextBeforeCursor(1, 0)
                val lastChar = if (!textBefore.isNullOrEmpty()) textBefore[0] else null

                val result = transliterationEngine.getTransliteration(keyString, lastChar)

                if (result.backspaceCount > 0) {
                    inputConnection.deleteSurroundingText(result.backspaceCount, 0)
                }

                inputConnection.commitText(result.text, 1)
            }
        }
        updateCandidates()
    }

    private fun handleBackspace(inputConnection: InputConnection) {
        // Clear buffer on backspace to prevent state desynchronization
        // This treats backspace as "commit current and delete previous char"
        transliterationEngine.clearBuffer()
        inputConnection.deleteSurroundingText(1, 0)
    }

    private fun updateCandidates() {
        mainScope.launch {
            val inputConnection = currentInputConnection ?: return@launch
            val textBeforeCursor = inputConnection.getTextBeforeCursor(50, 0)?.toString() ?: ""

            // Simple tokenization: take last word
            val lastWord = textBeforeCursor.split(Regex("\\s|\\p{Punct}")).lastOrNull() ?: ""

            if (lastWord.isNotEmpty()) {
                val suggestions = dictionaryLoader.getSuggestions(lastWord)
                if (suggestions.isNotEmpty()) {
                    candidatesAdapter.submitList(suggestions)
                    candidatesView.visibility = View.VISIBLE
                } else {
                    candidatesView.visibility = View.GONE
                }
            } else {
                candidatesView.visibility = View.GONE
            }
        }
    }

    private fun pickCandidate(word: String) {
        val inputConnection = currentInputConnection ?: return
        val textBeforeCursor = inputConnection.getTextBeforeCursor(50, 0)?.toString() ?: ""
        val lastWord = textBeforeCursor.split(Regex("\\s|\\p{Punct}")).lastOrNull() ?: ""

        if (lastWord.isNotEmpty()) {
            inputConnection.deleteSurroundingText(lastWord.length, 0)
            inputConnection.commitText("$word ", 1)
        }
        transliterationEngine.clearBuffer()
        candidatesView.visibility = View.GONE
    }

    private fun checkAudioPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition()
        } else {
            Toast.makeText(this, "Microphone permission required for Voice Typing", Toast.LENGTH_LONG).show()
            // Direct user to Settings to grant permission
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun startVoiceRecognition() {
        if (isListening) {
            speechRecognizer?.stopListening()
            isListening = false
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kn-IN")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            try {
                speechRecognizer?.startListening(intent)
                isListening = true
                Toast.makeText(this, "Listening...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Voice typing unavailable", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
        speechRecognizer?.destroy()
    }
}
