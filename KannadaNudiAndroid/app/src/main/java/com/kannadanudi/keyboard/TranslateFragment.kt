package com.kannadanudi.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.Random

class TranslateFragment : Fragment(), LanguageManager.OnLanguageChangeListener {

    private lateinit var btnLeftLangSelector: View
    private lateinit var btnRightLangSelector: View
    private lateinit var tvLeftLangLabel: TextView
    private lateinit var tvRightLangLabel: TextView
    private lateinit var ivLeftDropdownArrow: ImageView
    private lateinit var ivRightDropdownArrow: ImageView
    private lateinit var btnSwapLanguages: ImageButton
    
    private lateinit var etSourceArea: EditText
    private lateinit var tvTargetArea: TextView
    private lateinit var btnTranslate: View
    private lateinit var btnSpeakSource: View
    private lateinit var btnCopySource: View
    private lateinit var btnSpeakTarget: View
    private lateinit var btnCopyTarget: View
    
    private lateinit var btnMicCircle: View
    private lateinit var btnMicIcon: ImageView
    private lateinit var tvStatusLabel: TextView
    private lateinit var tvMicInstruction: TextView
    private lateinit var tvMicSubInstruction: TextView
    
    private lateinit var waveContainer: ViewGroup
    private lateinit var waveBars: List<View>

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var textToSpeech: TextToSpeech? = null
    private val fragmentScope = MainScope()
    private var waveAnimators = mutableListOf<android.animation.ValueAnimator>()
    private var micAnimator: android.animation.ObjectAnimator? = null

    // Target translation languages map
    private val languages = listOf(
        Language("English", "en", "en-US", "ಇಂಗ್ಲಿಷ್"),
        Language("Hindi", "hi", "hi-IN", "ಹಿಂದಿ"),
        Language("Tamil", "ta", "ta-IN", "ತಮಿಳು"),
        Language("Telugu", "te", "te-IN", "ತೆಲುಗು"),
        Language("Malayalam", "ml", "ml-IN", "ಮಲಯಾಳಂ"),
        Language("Spanish", "es", "es-ES", "ಸ್ಪ್ಯಾನಿಷ್"),
        Language("French", "fr", "fr-FR", "ಫ್ರೆಂಚ್"),
        Language("German", "de", "de-DE", "ಜರ್ಮನ್"),
        Language("Chinese", "zh", "zh-CN", "ಚೈನೀಸ್")
    )
    
    private var selectedLanguage = languages[0] // Default: English
    private var isSourceKannada = true // Default: Kannada Input -> English Output

    // Bi-directional local offline fallback database
    private val localPhrasesToEnglish = mapOf(
        "ನಮಸ್ಕಾರ, ಹೇಗಿದ್ದೀರಿ?" to "Hello, how are you?",
        "ನಮಸ್ಕಾರ" to "Hello / Welcome",
        "ಹೇಗಿದ್ದೀರಿ?" to "How are you?",
        "ನನಗೆ ಕನ್ನಡ ಗೊತ್ತು" to "I know Kannada",
        "ಧನ್ಯವಾದಗಳು" to "Thank you",
        "ಶುಭೋದಯ" to "Good morning",
        "ಶುಭ ರಾತ್ರಿ" to "Good night",
        "ನಿಮ್ಮ ಹೆಸರೇನು?" to "What is your name?",
        "ನನ್ನ ಹೆಸರು" to "My name is"
    )

    private val localPhrasesToKannada = mapOf(
        "hello, how are you?" to "ನಮಸ್ಕಾರ, ಹೇಗಿದ್ದೀರಿ?",
        "hello" to "ನಮಸ್ಕಾರ",
        "how are you?" to "ಹೇಗಿದ್ದೀರಿ?",
        "i know kannada" to "ನನಗೆ ಕನ್ನಡ ಗೊತ್ತು",
        "thank you" to "ಧನ್ಯವಾದಗಳು",
        "good morning" to "ಶುಭೋದಯ",
        "good night" to "ಶುಭ ರಾತ್ರಿ",
        "what is your name?" to "ನಿಮ್ಮ ಹೆಸರೇನು?",
        "my name is" to "ನನ್ನ ಹೆಸರು"
    )

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
        return inflater.inflate(R.layout.fragment_translate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Find Views
        btnLeftLangSelector = view.findViewById(R.id.btnLeftLangSelector)
        btnRightLangSelector = view.findViewById(R.id.btnRightLangSelector)
        tvLeftLangLabel = view.findViewById(R.id.tvLeftLangLabel)
        tvRightLangLabel = view.findViewById(R.id.tvRightLangLabel)
        ivLeftDropdownArrow = view.findViewById(R.id.ivLeftDropdownArrow)
        ivRightDropdownArrow = view.findViewById(R.id.ivRightDropdownArrow)
        btnSwapLanguages = view.findViewById(R.id.btnSwapLanguages)
        
        etSourceArea = view.findViewById(R.id.etSourceArea)
        tvTargetArea = view.findViewById(R.id.tvTargetArea)
        btnTranslate = view.findViewById(R.id.btnTranslate)
        btnSpeakSource = view.findViewById(R.id.btnSpeakSource)
        btnCopySource = view.findViewById(R.id.btnCopySource)
        btnSpeakTarget = view.findViewById(R.id.btnSpeakTarget)
        btnCopyTarget = view.findViewById(R.id.btnCopyTarget)
        
        btnMicCircle = view.findViewById(R.id.btnMicCircle)
        btnMicIcon = view.findViewById(R.id.btnMicIcon)
        tvStatusLabel = view.findViewById(R.id.tvStatusLabel)
        tvMicInstruction = view.findViewById(R.id.tvMicInstruction)
        tvMicSubInstruction = view.findViewById(R.id.tvMicSubInstruction)
        
        waveContainer = view.findViewById(R.id.wave_container)
        waveBars = listOf(
            view.findViewById(R.id.wave_bar_1),
            view.findViewById(R.id.wave_bar_2),
            view.findViewById(R.id.wave_bar_3),
            view.findViewById(R.id.wave_bar_4),
            view.findViewById(R.id.wave_bar_5)
        )

        // Make source input standard editable/pasteable focusing standard keyboard soft input
        etSourceArea.showSoftInputOnFocus = true

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
                        etSourceArea.editableText.insert(etSourceArea.selectionStart, text + " ")
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

        btnSwapLanguages.setOnClickListener {
            swapTranslationDirection()
        }

        val selectLanguageListener = View.OnClickListener {
            showLanguageSelectorDialog()
        }
        btnLeftLangSelector.setOnClickListener(selectLanguageListener)
        btnRightLangSelector.setOnClickListener(selectLanguageListener)

        btnTranslate.setOnClickListener {
            translateText()
        }

        btnSpeakSource.setOnClickListener {
            speakText(isSource = true)
        }

        btnCopySource.setOnClickListener {
            copyText(isSource = true)
        }

        btnSpeakTarget.setOnClickListener {
            speakText(isSource = false)
        }

        btnCopyTarget.setOnClickListener {
            copyText(isSource = false)
        }

        LanguageManager.addListener(this)
        updateLanguageSelectorUI()
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

    // dynamic voice deactivation when switching tabs (fragment gets hidden)
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
            updateLanguageSelectorUI()
        }
    }

    private fun applyLanguage() {
        val isKn = LanguageManager.isKannada()
        etSourceArea.hint = if (isKn) "ಇಲ್ಲಿ ಬರೆಯಿರಿ ಅಥವಾ ಮಾತನಾಡಿ..." else "Type or paste here to translate..."
        tvTargetArea.hint = if (isKn) "ಅನುವಾದವು ಇಲ್ಲಿ ಗೋಚರಿಸುತ್ತದೆ..." else "Translation will appear here..."
        
        if (!isListening) {
            resetStatusLabels()
        }
    }

    private fun resetStatusLabels() {
        val isKn = LanguageManager.isKannada()
        tvStatusLabel.text = if (isKn) "ಅನುವಾದಿಸಲು ಮೈಕ್ರೊಫೋನ್ ಟ್ಯಾಪ್ ಮಾಡಿ" else "TAP MICROPHONE TO TRANSLATE"
        tvMicInstruction.text = if (isKn) "ಟ್ಯಾಪ್ ಮಾಡಿ ಮತ್ತು ಮಾತನಾಡಿ" else "TAP AND SPEAK"
        tvMicSubInstruction.text = if (isKn) "ಮಾತನಾಡಿ" else "TAP TO SPEAK"
    }

    // Toggle swap translation direction (Top Kannada Input -> English Output vs Top English Input -> Kannada Output)
    private fun swapTranslationDirection() {
        isSourceKannada = !isSourceKannada
        // Swap contents
        val tempSrc = etSourceArea.text.toString()
        val tempTgt = tvTargetArea.text.toString()
        
        etSourceArea.setText(tempTgt)
        tvTargetArea.text = tempSrc

        updateLanguageSelectorUI()
        applyLanguage()
        
        val directionMsg = if (isSourceKannada) {
            if (LanguageManager.isKannada()) "ಕನ್ನಡದಿಂದ ಅನುವಾದ" else "Translating from Kannada"
        } else {
            val langName = if (LanguageManager.isKannada()) selectedLanguage.knName else selectedLanguage.name
            if (LanguageManager.isKannada()) "${langName}ದಿಂದ ಅನುವಾದ" else "Translating from ${selectedLanguage.name}"
        }
        Toast.makeText(requireContext(), directionMsg, Toast.LENGTH_SHORT).show()
    }

    // Update target language selection UI indicators dynamically swapping arrow indicators and container click properties
    private fun updateLanguageSelectorUI() {
        val isKn = LanguageManager.isKannada()
        val targetName = if (isKn) selectedLanguage.knName else selectedLanguage.name

        if (isSourceKannada) {
            // Left is Kannada: static, arrow hidden, click disabled
            tvLeftLangLabel.text = if (isKn) "ಕನ್ನಡ" else "KANNADA"
            ivLeftDropdownArrow.visibility = View.GONE
            btnLeftLangSelector.isClickable = false
            btnLeftLangSelector.isFocusable = false

            // Right is Other Language: active dropdown selector, arrow shown, click enabled
            tvRightLangLabel.text = targetName.uppercase(Locale.getDefault())
            ivRightDropdownArrow.visibility = View.VISIBLE
            btnRightLangSelector.isClickable = true
            btnRightLangSelector.isFocusable = true
        } else {
            // Left is Other Language: active dropdown selector, arrow shown, click enabled
            tvLeftLangLabel.text = targetName.uppercase(Locale.getDefault())
            ivLeftDropdownArrow.visibility = View.VISIBLE
            btnLeftLangSelector.isClickable = true
            btnLeftLangSelector.isFocusable = true

            // Right is Kannada: static, arrow hidden, click disabled
            tvRightLangLabel.text = if (isKn) "ಕನ್ನಡ" else "KANNADA"
            ivRightDropdownArrow.visibility = View.GONE
            btnRightLangSelector.isClickable = false
            btnRightLangSelector.isFocusable = false
        }
    }

    // Displays target languages dropdown list
    private fun showLanguageSelectorDialog() {
        val items = languages.map { if (LanguageManager.isKannada()) it.knName else it.name }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(if (LanguageManager.isKannada()) "ಭಾಷೆಯನ್ನು ಆರಿಸಿ" else "Select Language")
            .setItems(items) { _, which ->
                selectedLanguage = languages[which]
                updateLanguageSelectorUI()
                applyLanguage()
            }
            .show()
    }

    // Voice recognition toggles
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
            val activeInstruction = if (LanguageManager.isKannada()) "ಮಾತನಾಡಿ..." else "SPEAK NOW"
            
            tvStatusLabel.text = activeLabel
            tvMicInstruction.text = activeInstruction
            tvMicSubInstruction.text = activeLabel
            
            val startedMsg = if (LanguageManager.isKannada()) "ಆಲಿಸಲಾಗುತ್ತಿದೆ..." else "Listening..."
            Toast.makeText(requireContext(), startedMsg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun startVoiceRecognitionInternal() {
        val localeStr = if (isSourceKannada) "kn-IN" else selectedLanguage.locale
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeStr)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isListening = false
            Toast.makeText(requireContext(), "Voice typing unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    // ObjectAnimator mic pulsing scale animation
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

    // ValueAnimator asynchronous wave scaling animations
    private fun startWaveAnimation() {
        stopWaveAnimation()
        val random = Random()
        val dpToPx = resources.displayMetrics.density
        
        waveBars.forEachIndexed { index, bar ->
            val startHeight = 10f
            val maxHeight = 28f
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

    // Copy action for both Source and Translation Target text
    private fun copyText(isSource: Boolean) {
        val textToCopy = if (isSource) etSourceArea.text.toString() else tvTargetArea.text.toString()
        if (textToCopy.isNotEmpty()) {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Translate Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            
            val toastMsg = if (LanguageManager.isKannada()) "ಪಠ್ಯವನ್ನು ನಕಲಿಸಲಾಗಿದೆ" else "Text copied to clipboard"
            Toast.makeText(requireContext(), toastMsg, Toast.LENGTH_SHORT).show()
        } else {
            val emptyMsg = if (LanguageManager.isKannada()) "ನಕಲಿಸಲು ಪಠ್ಯವಿಲ್ಲ" else "No text to copy"
            Toast.makeText(requireContext(), emptyMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // TTS Reader with locale mapping
    private fun speakText(isSource: Boolean) {
        val text = if (isSource) etSourceArea.text.toString().trim() else tvTargetArea.text.toString().trim()
        if (text.isNotEmpty()) {
            val speakInKannada = (isSource && isSourceKannada) || (!isSource && !isSourceKannada)
            
            textToSpeech?.language = if (speakInKannada) {
                Locale("kn", "IN")
            } else {
                Locale(selectedLanguage.code)
            }
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            val speakEmptyMsg = if (LanguageManager.isKannada()) "ಓದಲು ಪಠ್ಯವಿಲ್ಲ" else "Text is empty"
            Toast.makeText(requireContext(), speakEmptyMsg, Toast.LENGTH_SHORT).show()
        }
    }

    // Coroutine HTTP Translation with offline local phrase maps
    private fun translateText() {
        val text = etSourceArea.text.toString().trim()
        if (text.isEmpty()) {
            tvTargetArea.text = ""
            val transEmptyMsg = if (LanguageManager.isKannada()) "ಅನುವಾದಿಸಲು ಪಠ್ಯವನ್ನು ಬರೆಯಿರಿ" else "Write text to translate"
            Toast.makeText(requireContext(), transEmptyMsg, Toast.LENGTH_SHORT).show()
            return
        }

        tvTargetArea.text = if (LanguageManager.isKannada()) "ಅನುವಾದಿಸಲಾಗುತ್ತಿದೆ..." else "Translating..."

        fragmentScope.launch {
            var translation: String? = null

            // 1. Check local fast phrase lookup
            if (isSourceKannada) {
                for ((kn, en) in localPhrasesToEnglish) {
                    if (text.equals(kn, ignoreCase = true)) {
                        translation = en
                        break
                    }
                }
            } else {
                for ((en, kn) in localPhrasesToKannada) {
                    if (text.equals(en, ignoreCase = true)) {
                        translation = kn
                        break
                    }
                }
            }

            // 2. Query HTTP MyMemory API in IO Thread
            if (translation == null) {
                translation = withContext(Dispatchers.IO) {
                    try {
                        val encoded = URLEncoder.encode(text, "UTF-8")
                        val langPairStr = if (isSourceKannada) "kn|${selectedLanguage.code}" else "${selectedLanguage.code}|kn"
                        
                        val url = URL("https://api.mymemory.translated.net/get?q=$encoded&langpair=$langPairStr")
                        val conn = url.openConnection() as java.net.HttpURLConnection
                        conn.connectTimeout = 3000
                        conn.readTimeout = 3000
                        conn.requestMethod = "GET"
                        
                        if (conn.responseCode == 200) {
                            val res = conn.inputStream.bufferedReader().use { it.readText() }
                            val key = "\"translatedText\":\""
                            val startIdx = res.indexOf(key)
                            if (startIdx != -1) {
                                val start = startIdx + key.length
                                val end = res.indexOf("\"", start)
                                if (end != -1) {
                                    val rawTrans = res.substring(start, end)
                                    unescapeJson(rawTrans)
                                } else null
                            } else null
                        } else null
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            // 3. Update Output UI
            if (translation != null) {
                tvTargetArea.text = translation
            } else {
                val failMsg = if (LanguageManager.isKannada()) "ಅನುವಾದ ವಿಫಲವಾಗಿದೆ (ನೆಟ್‌ವರ್ಕ್ ಪರಿಶೀಲಿಸಿ)" else "Translation failed (Check network)"
                tvTargetArea.text = failMsg
                Toast.makeText(requireContext(), failMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun unescapeJson(str: String): String {
        var res = str
        val matcher = java.util.regex.Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(res)
        val sb = java.lang.StringBuffer()
        while (matcher.find()) {
            val hex = matcher.group(1)
            val char = hex.toInt(16).toChar()
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(char.toString()))
        }
        matcher.appendTail(sb)
        res = sb.toString()
        return res.replace("\\\"", "\"").replace("\\/", "/")
    }

    data class Language(val name: String, val code: String, val locale: String, val knName: String)
}
