package com.kannadanudi.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.math.max

class TypeFragment : Fragment(), LanguageManager.OnLanguageChangeListener, KeyboardView.OnKeyboardActionListener {

    private lateinit var tvTypeTitle: TextView
    private lateinit var tvTypeSubtitle: TextView
    private lateinit var etTypeArea: EditText
    private lateinit var btnCopy: ImageButton
    
    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var nudiKeyboard: Keyboard
    private lateinit var nudiShiftedKeyboard: Keyboard
    private lateinit var numberpadKeyboard: Keyboard
    private var isCaps = false

    private val transliterationEngine = TransliterationEngine()
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val requestMicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startVoiceRecognition()
        } else {
            Toast.makeText(requireContext(), "Microphone permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_type, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        tvTypeTitle = view.findViewById(R.id.tvTypeTitle)
        tvTypeSubtitle = view.findViewById(R.id.tvTypeSubtitle)
        etTypeArea = view.findViewById(R.id.etTypeArea)
        btnCopy = view.findViewById(R.id.btnCopy)

        // Disable system keyboard
        etTypeArea.showSoftInputOnFocus = false

        keyboardView = view.findViewById(R.id.keyboard)
        
        qwertyKeyboard = Keyboard(requireContext(), R.xml.qwerty)
        nudiKeyboard = Keyboard(requireContext(), R.xml.nudi_layout)
        nudiShiftedKeyboard = Keyboard(requireContext(), R.xml.nudi_layout_shifted)
        numberpadKeyboard = Keyboard(requireContext(), R.xml.numberpad_layout)

        keyboardView.keyboard = nudiKeyboard
        transliterationEngine.currentLayout = KeyboardLayout.Nudi
        keyboardView.setOnKeyboardActionListener(this)

        // Hide switch keyboard text since it's just a preview
        val switchText = view.findViewById<TextView>(R.id.tv_switch_keyboard)
        switchText?.visibility = View.GONE
        
        // Hide candidates view for now, as it requires hooking up to CandidatesAdapter which is complex
        val candidatesView = view.findViewById<View>(R.id.rv_candidates)
        candidatesView?.visibility = View.GONE

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    // Do nothing, wait for results
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                        else -> "Error: $error"
                    }
                    if (error != SpeechRecognizer.ERROR_CLIENT && isListening) {
                        startVoiceRecognitionInternal()
                    } else {
                        isListening = false
                    }
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        etTypeArea.editableText.insert(etTypeArea.selectionStart, text + " ")
                    }
                    if (isListening) {
                        startVoiceRecognitionInternal()
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } catch (e: Exception) {
            speechRecognizer = null
        }

        btnCopy.setOnClickListener {
            val textToCopy = etTypeArea.text.toString()
            if (textToCopy.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Kannada Nudi Typed Text", textToCopy)
                clipboard.setPrimaryClip(clip)
                
                val toastMsg = if (LanguageManager.isKannada()) "ಪಠ್ಯವನ್ನು ನಕಲಿಸಲಾಗಿದೆ" else "Text copied to clipboard"
                Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()
            }
        }

        LanguageManager.addListener(this)
        applyLanguage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LanguageManager.removeListener(this)
        speechRecognizer?.destroy()
    }

    override fun onLanguageChanged(language: String) {
        if (isAdded) {
            applyLanguage()
        }
    }

    private fun applyLanguage() {
        val isKn = LanguageManager.isKannada()

        tvTypeTitle.text = if (isKn) "ಕೀಬೋರ್ಡ್ ಮುನ್ನೋಟ" else "Keyboard Preview"
        tvTypeSubtitle.text = if (isKn) "ಕೀಬೋರ್ಡ್ ವಿನ್ಯಾಸ ಮತ್ತು ಲಿಪ್ಯಂತರವನ್ನು ಇಲ್ಲಿ ಪರೀಕ್ಷಿಸಿ." else "Test the keyboard layout and transliteration here."
        etTypeArea.hint = if (isKn) "ಏನನ್ನಾದರೂ ಟೈಪ್ ಮಾಡಿ..." else "Type something..."
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val start = etTypeArea.selectionStart
        val end = etTypeArea.selectionEnd
        
        if (start != end) {
            etTypeArea.editableText.delete(start, end)
        }

        val cursorPosition = etTypeArea.selectionStart

        when (primaryCode) {
            Keyboard.KEYCODE_DELETE -> {
                if (cursorPosition > 0) {
                    transliterationEngine.clearBuffer()
                    etTypeArea.editableText.delete(cursorPosition - 1, cursorPosition)
                }
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
            Keyboard.KEYCODE_DONE, 10 -> {
                etTypeArea.editableText.insert(cursorPosition, "\n")
                transliterationEngine.clearBuffer()
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

                val keyString = code.toString()
                val textBefore = if (cursorPosition > 0) etTypeArea.text[cursorPosition - 1] else null
                val result = transliterationEngine.getTransliteration(keyString, textBefore)

                if (result.backspaceCount > 0) {
                    val deleteStart = max(0, cursorPosition - result.backspaceCount)
                    etTypeArea.editableText.delete(deleteStart, cursorPosition)
                }

                etTypeArea.editableText.insert(etTypeArea.selectionStart, result.text)
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

    private fun checkAudioPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition()
        } else {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startVoiceRecognition() {
        if (isListening) {
            isListening = false
            speechRecognizer?.stopListening()
            Toast.makeText(requireContext(), "Stopped Listening", Toast.LENGTH_SHORT).show()
        } else {
            isListening = true
            startVoiceRecognitionInternal()
            Toast.makeText(requireContext(), "Listening Continuously...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVoiceRecognitionInternal() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kn-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(requireContext(), "Voice typing unavailable", Toast.LENGTH_SHORT).show()
        }
    }
}
