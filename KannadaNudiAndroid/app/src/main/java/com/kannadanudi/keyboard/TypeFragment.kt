package com.kannadanudi.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlin.math.max
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import java.util.Locale
import java.util.Random

class TypeFragment : Fragment(), LanguageManager.OnLanguageChangeListener, KeyboardView.OnKeyboardActionListener {

    private lateinit var etTypeArea: EditText
    private lateinit var btnCopy: View
    private lateinit var btnSpeak: View
    private lateinit var btnToggleKeyboard: View
    private lateinit var btnMicCircle: View
    private lateinit var btnMicIcon: ImageView
    private lateinit var tvStatusLabel: TextView
    private lateinit var tvMicInstruction: TextView
    private lateinit var tvMicSubInstruction: TextView
    private lateinit var keyboardContainer: View
    private lateinit var waveContainer: ViewGroup
    private lateinit var waveBars: List<View>
    
    private lateinit var keyboardView: KeyboardView
    private lateinit var qwertyKeyboard: Keyboard
    private lateinit var nudiKeyboard: Keyboard
    private lateinit var nudiShiftedKeyboard: Keyboard
    private lateinit var numberpadKeyboard: Keyboard
    private var isCaps = false

    private val transliterationEngine = TransliterationEngine()
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private var textToSpeech: TextToSpeech? = null
    private val fragmentScope = MainScope()
    private var waveAnimators = mutableListOf<android.animation.ValueAnimator>()
    private var micAnimator: android.animation.ObjectAnimator? = null

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
        
        // Find Views
        etTypeArea = view.findViewById(R.id.etTypeArea)
        btnCopy = view.findViewById(R.id.btnCopy)
        btnSpeak = view.findViewById(R.id.btnSpeak)
        btnToggleKeyboard = view.findViewById(R.id.btnToggleKeyboard)
        btnMicCircle = view.findViewById(R.id.btnMicCircle)
        btnMicIcon = view.findViewById(R.id.btnMicIcon)
        tvStatusLabel = view.findViewById(R.id.tvStatusLabel)
        tvMicInstruction = view.findViewById(R.id.tvMicInstruction)
        tvMicSubInstruction = view.findViewById(R.id.tvMicSubInstruction)
        keyboardContainer = view.findViewById(R.id.keyboard_container)
        waveContainer = view.findViewById(R.id.wave_container)
        
        waveBars = listOf(
            view.findViewById(R.id.wave_bar_1),
            view.findViewById(R.id.wave_bar_2),
            view.findViewById(R.id.wave_bar_3),
            view.findViewById(R.id.wave_bar_4),
            view.findViewById(R.id.wave_bar_5)
        )

        // Disable system keyboard soft input focus popup
        etTypeArea.showSoftInputOnFocus = false

        // Custom keyboard triggers (Requirement: touch or click text area should spawn the keyboard)
        etTypeArea.setOnClickListener {
            keyboardContainer.visibility = View.VISIBLE
        }
        
        etTypeArea.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                keyboardContainer.visibility = View.VISIBLE
            }
        }

        // Initialize custom keyboard layout
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
        
        // Hide candidates view by default
        val candidatesView = view.findViewById<View>(R.id.rv_candidates)
        candidatesView?.visibility = View.GONE

        // Initialize Text-To-Speech
        textToSpeech = TextToSpeech(requireContext()) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("kn", "IN")
            }
        }

        // Initialize Speech Recognizer
        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}

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
                        activity?.runOnUiThread {
                            stopMicPulse()
                            stopWaveAnimation()
                            resetStatusLabels()
                        }
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
                    } else {
                        activity?.runOnUiThread {
                            stopMicPulse()
                            stopWaveAnimation()
                            resetStatusLabels()
                        }
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } catch (e: Exception) {
            speechRecognizer = null
        }

        // Action Listeners
        btnMicCircle.setOnClickListener {
            checkAudioPermissionAndListen()
        }

        btnSpeak.setOnClickListener {
            speakText()
        }

        btnToggleKeyboard.setOnClickListener {
            toggleKeyboardVisibility()
        }

        btnCopy.setOnClickListener {
            copyTextToClipboard()
        }

        LanguageManager.addListener(this)
        applyLanguage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LanguageManager.removeListener(this)
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        fragmentScope.cancel()
        stopMicPulse()
        stopWaveAnimation()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            if (isListening) {
                isListening = false
                speechRecognizer?.stopListening()
                stopMicPulse()
                stopWaveAnimation()
                resetStatusLabels()
            }
        }
    }

    override fun onLanguageChanged(language: String) {
        if (isAdded) {
            applyLanguage()
        }
    }

    private fun applyLanguage() {
        val isKn = LanguageManager.isKannada()

        etTypeArea.hint = if (isKn) "ಏನನ್ನಾದರೂ ಟೈಪ್ ಮಾಡಿ..." else "Type something..."
        
        if (!isListening) {
            resetStatusLabels()
        }
    }

    private fun resetStatusLabels() {
        val isKn = LanguageManager.isKannada()
        tvStatusLabel.text = if (isKn) "ಮಾತನಾಡಲು ಮೈಕ್ರೊಫೋನ್ ಟ್ಯಾಪ್ ಮಾಡಿ" else "TAP MICROPHONE TO SPEAK"
        tvMicInstruction.text = if (isKn) "ಟ್ಯಾಪ್ ಮಾಡಿ ಮತ್ತು ಮಾತನಾಡಿ" else "TAP AND SPEAK"
        tvMicSubInstruction.text = if (isKn) "ಮಾತನಾಡಿ" else "TAP TO SPEAK"
    }

    // Keyboard Key Action Listeners
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
            -102 -> { // MIC Code on custom keyboard
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

    // Audio Continuous Recording handlers
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
            stopMicPulse()
            stopWaveAnimation()
            resetStatusLabels()
            val stoppedMsg = if (LanguageManager.isKannada()) "ಧ್ವನಿ ಗ್ರಹಿಕೆ ನಿಲ್ಲಿಸಲಾಗಿದೆ" else "Stopped Listening"
            Toast.makeText(requireContext(), stoppedMsg, Toast.LENGTH_SHORT).show()
        } else {
            isListening = true
            startVoiceRecognitionInternal()
            startMicPulse()
            startWaveAnimation()
            
            val activeLabel = if (LanguageManager.isKannada()) "ಆಲಿಸಲಾಗುತ್ತಿದೆ..." else "LISTENING..."
            val activeInstruction = if (LanguageManager.isKannada()) "ನಡು ಮಾತನಾಡುತ್ತಿದೆ" else "SPEAK NOW"
            
            tvStatusLabel.text = activeLabel
            tvMicInstruction.text = activeInstruction
            tvMicSubInstruction.text = activeLabel
            
            val startedMsg = if (LanguageManager.isKannada()) "ಆಲಿಸಲಾಗುತ್ತಿದೆ..." else "Listening..."
            Toast.makeText(requireContext(), startedMsg, Toast.LENGTH_SHORT).show()
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

    // Dynamic scale pulse on the mic button
    private fun startMicPulse() {
        btnMicCircle.setBackgroundResource(R.drawable.mic_background_circle_active)
        btnMicIcon.setColorFilter(Color.WHITE)
        
        val scaleX = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1.0f, 1.1f, 1.0f)
        val scaleY = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.0f, 1.1f, 1.0f)
        
        micAnimator = android.animation.ObjectAnimator.ofPropertyValuesHolder(btnMicCircle, scaleX, scaleY).apply {
            duration = 1800L
            repeatCount = android.animation.ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopMicPulse() {
        micAnimator?.cancel()
        micAnimator = null
        btnMicCircle.scaleX = 1.0f
        btnMicCircle.scaleY = 1.0f
        btnMicCircle.setBackgroundResource(R.drawable.mic_background_circle)
        btnMicIcon.setColorFilter(Color.parseColor("#BB001E"))
    }

    // Dynamic wave height scale animators
    private fun startWaveAnimation() {
        stopWaveAnimation()
        val random = Random()
        val dpToPx = resources.displayMetrics.density
        
        waveBars.forEachIndexed { index, bar ->
            val startHeight = 10f
            val maxHeight = 30f
            val animator = android.animation.ValueAnimator.ofFloat(startHeight, maxHeight).apply {
                duration = 320L + random.nextInt(180)
                repeatCount = android.animation.ValueAnimator.INFINITE
                repeatMode = android.animation.ValueAnimator.REVERSE
                startDelay = index * 70L
                addUpdateListener { animation ->
                    val value = animation.animatedValue as Float
                    val params = bar.layoutParams
                    params.height = (value * dpToPx).toInt()
                    bar.layoutParams = params
                }
            }
            waveAnimators.add(animator)
            animator.start()
        }
    }

    private fun stopWaveAnimation() {
        waveAnimators.forEach { it.cancel() }
        waveAnimators.clear()
        
        val dpToPx = resources.displayMetrics.density
        waveBars.forEach { bar ->
            val params = bar.layoutParams
            params.height = (10 * dpToPx).toInt()
            bar.layoutParams = params
        }
    }

    // Toggle Custom Keyboard visibility drawer
    private fun toggleKeyboardVisibility() {
        val isVisible = keyboardContainer.visibility == View.VISIBLE
        if (isVisible) {
            keyboardContainer.visibility = View.GONE
        } else {
            keyboardContainer.visibility = View.VISIBLE
            etTypeArea.requestFocus()
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(etTypeArea.windowToken, 0)
        }
    }

    // Copy to clipboard
    private fun copyTextToClipboard() {
        val textToCopy = etTypeArea.text.toString()
        if (textToCopy.isNotEmpty()) {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Kannada Typed Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            
            val toastMsg = if (LanguageManager.isKannada()) "ಪಠ್ಯವನ್ನು ನಕಲಿಸಲಾಗಿದೆ" else "Text copied to clipboard"
            Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()
        } else {
            val emptyMsg = if (LanguageManager.isKannada()) "ನಕಲಿಸಲು ಪಠ್ಯವಿಲ್ಲ" else "No text to copy"
            Toast.makeText(requireContext(), emptyMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // TTS Reader
    private fun speakText() {
        val text = etTypeArea.text.toString().trim()
        if (text.isNotEmpty()) {
            val isEng = text.firstOrNull()?.let { it in 'A'..'Z' || it in 'a'..'z' } ?: false
            textToSpeech?.language = if (isEng) Locale.US else Locale("kn", "IN")
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            val speakEmptyMsg = if (LanguageManager.isKannada()) "ಓದಲು ಪಠ್ಯವಿಲ್ಲ" else "Text area is empty"
            Toast.makeText(requireContext(), speakEmptyMsg, Toast.LENGTH_SHORT).show()
        }
    }
}
