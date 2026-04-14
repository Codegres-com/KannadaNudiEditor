package com.kannadanudi.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.content.ComponentName
import android.net.Uri
import android.provider.Settings

class SpeechFragment : Fragment(), LanguageManager.OnLanguageChangeListener {

    private lateinit var tvSpeechTitle: TextView
    private lateinit var tvOfflineLabel: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var etSpeechResult: EditText
    private lateinit var btnCopy: Button
    private lateinit var switchOffline: SwitchCompat
    private lateinit var tvOfflineStatus: TextView
    private lateinit var btnDownloadOfflinePack: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isOfflineMode = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startListening()
        } else {
            val msg = if (LanguageManager.isKannada())
                "ಧ್ವನಿ ಗುರುತಿಸುವಿಕೆಗೆ ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ಅಗತ್ಯವಿದೆ"
            else
                "Microphone permission is required for speech recognition"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_speech, container, false)

        tvSpeechTitle = view.findViewById(R.id.tvSpeechTitle)
        tvOfflineLabel = view.findViewById(R.id.tvOfflineLabel)
        btnMic = view.findViewById(R.id.btnMic)
        tvStatus = view.findViewById(R.id.tvStatus)
        etSpeechResult = view.findViewById(R.id.etSpeechResult)
        btnCopy = view.findViewById(R.id.btnCopy)
        switchOffline = view.findViewById(R.id.switchOffline)
        tvOfflineStatus = view.findViewById(R.id.tvOfflineStatus)
        btnDownloadOfflinePack = view.findViewById(R.id.btnDownloadOfflinePack)

        btnDownloadOfflinePack.setOnClickListener { openOfflineSpeechSettings() }

        updateOfflineStatusText()
        setupSpeechRecognizer()

        switchOffline.setOnCheckedChangeListener { _, checked ->
            isOfflineMode = checked
            updateOfflineStatusText()
            setupSpeechRecognizer()
        }

        btnMic.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                checkPermissionAndStart()
            }
        }

        btnCopy.setOnClickListener {
            val text = etSpeechResult.text.toString()
            if (text.isNotEmpty()) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Kannada Speech", text)
                clipboard.setPrimaryClip(clip)
                val msg = if (LanguageManager.isKannada()) "ಕ್ಲಿಪ್‌ಬೋರ್ಡ್‌ಗೆ ನಕಲಿಸಲಾಗಿದೆ" else "Copied to clipboard"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            } else {
                val msg = if (LanguageManager.isKannada()) "ನಕಲಿಸಲು ಪಠ್ಯವಿಲ್ಲ" else "No text to copy"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        LanguageManager.addListener(this)
        applyLanguage()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LanguageManager.removeListener(this)
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    override fun onLanguageChanged(language: String) {
        if (isAdded) {
            applyLanguage()
        }
    }

    private fun applyLanguage() {
        val isKn = LanguageManager.isKannada()

        tvSpeechTitle.text = if (isKn) "ಕನ್ನಡ ಧ್ವನಿಯಿಂದ ಪಠ್ಯ" else "Kannada Speech to Text"
        tvOfflineLabel.text = if (isKn) "ಆಫ್‌ಲೈನ್ ಮೋಡ್" else "Offline Mode"
        btnCopy.text = if (isKn) "ಪಠ್ಯ ನಕಲಿಸಿ" else "Copy Text"
        etSpeechResult.hint = if (isKn) "ನಿಮ್ಮ ಮಾತು ಇಲ್ಲಿ ಕಾಣಿಸುತ್ತದೆ..." else "Your speech will appear here..."
        btnDownloadOfflinePack.text = if (isKn) "ಕನ್ನಡ ಆಫ್‌ಲೈನ್ ಪ್ಯಾಕ್ ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ" else "Download Kannada Offline Pack"

        if (!isListening) {
            tvStatus.text = if (isKn) "ಮಾತನಾಡಲು ಮೈಕ್ರೋಫೋನ್ ಒತ್ತಿ" else "Tap microphone to speak"
        }

        updateOfflineStatusText()
    }

    private fun updateOfflineStatusText() {
        val isKn = LanguageManager.isKannada()

        if (!isOfflineMode) {
            tvOfflineStatus.text = if (isKn) "ಆನ್‌ಲೈನ್ ಮೋಡ್ — ಇಂಟರ್ನೆಟ್ ಸಂಪರ್ಕ ಅಗತ್ಯ" else "Online mode — requires internet connection"
            tvOfflineStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            btnDownloadOfflinePack.visibility = View.GONE
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (SpeechRecognizer.isOnDeviceRecognitionAvailable(requireContext())) {
                tvOfflineStatus.text = if (isKn)
                    "ಆಫ್‌ಲೈನ್ ಮೋಡ್ — ಸಾಧನದಲ್ಲಿ ಕನ್ನಡ ಗುರುತಿಸುವಿಕೆ ಸಕ್ರಿಯ"
                else
                    "Offline mode — on-device Kannada recognition active"
                tvOfflineStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                btnDownloadOfflinePack.visibility = View.GONE
            } else {
                tvOfflineStatus.text = if (isKn)
                    "ಕನ್ನಡ ಆಫ್‌ಲೈನ್ ಪ್ಯಾಕ್ ಸ್ಥಾಪಿಸಲಾಗಿಲ್ಲ. ಆಫ್‌ಲೈನ್ ಧ್ವನಿ ಬಳಸಲು ಡೌನ್‌ಲೋಡ್ ಮಾಡಿ."
                else
                    "Kannada offline pack not installed. Download it to use offline speech."
                tvOfflineStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                btnDownloadOfflinePack.visibility = View.VISIBLE
            }
        } else {
            tvOfflineStatus.text = if (isKn)
                "ಆಫ್‌ಲೈನ್ ಮೋಡ್ — ಸಾಧನದಲ್ಲಿ ಗುರುತಿಸುವಿಕೆಗೆ ಆದ್ಯತೆ (ಸಂಪೂರ್ಣ ಆಫ್‌ಲೈನ್ ಬೆಂಬಲಕ್ಕೆ Android 12+)"
            else
                "Offline mode — prefers on-device recognition (Android 12+ for full offline support)"
            tvOfflineStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            btnDownloadOfflinePack.visibility = View.VISIBLE
        }
    }

    private fun openOfflineSpeechSettings() {
        // 1. Google app — direct Offline Language Picker (most reliable on stock Android)
        val offlinePicker = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.voicesearch.settings.OfflineLangPickerActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 2. Google app — Voice Search preferences page
        val voicePrefs = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(
                "com.google.android.googlequicksearchbox",
                "com.google.android.apps.gsa.velvet.ui.settings.PublicSettingsActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // 3. Android System Intelligence app details (Android 11+ on-device STT manager)
        val asiDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:com.google.android.as")
        }

        // 4. Manage voice input data (generic Android speech intent)
        val manageVoice = Intent("android.speech.action.MANAGE_VOICE_INPUT_DATA")

        // Use try/startActivity directly — resolveActivity is unreliable on Android 11+
        // even with <queries> declared, due to package visibility policy changes in API 33.
        for (intent in listOf(offlinePicker, voicePrefs, manageVoice, asiDetails)) {
            try {
                startActivity(intent)
                return
            } catch (_: Exception) {}
        }

        // Final fallback — Language & Input settings
        startActivity(Intent(Settings.ACTION_LOCALE_SETTINGS))
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        val isKn = LanguageManager.isKannada()

        if (isOfflineMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(requireContext())
        ) {
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(requireContext())
        } else {
            if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
                tvStatus.text = if (isKn)
                    "ಈ ಸಾಧನದಲ್ಲಿ ಧ್ವನಿ ಗುರುತಿಸುವಿಕೆ ಲಭ್ಯವಿಲ್ಲ"
                else
                    "Speech Recognition not available on this device"
                btnMic.isEnabled = false
                btnMic.alpha = 0.5f
                return
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        }

        btnMic.isEnabled = true
        btnMic.alpha = 1.0f

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                tvStatus.text = if (LanguageManager.isKannada()) "ಕೇಳುತ್ತಿದೆ..." else "Listening..."
                btnMic.setBackgroundResource(R.drawable.mic_background_circle_active)
            }

            override fun onBeginningOfSpeech() {
                tvStatus.text = if (LanguageManager.isKannada()) "ಕೇಳುತ್ತಿದೆ..." else "Listening..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                tvStatus.text = if (LanguageManager.isKannada()) "ಸಂಸ್ಕರಿಸಲಾಗುತ್ತಿದೆ..." else "Processing..."
                isListening = false
                btnMic.setBackgroundResource(R.drawable.mic_background_circle)
            }

            override fun onError(error: Int) {
                isListening = false
                btnMic.setBackgroundResource(R.drawable.mic_background_circle)
                val isKn2 = LanguageManager.isKannada()

                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> if (isKn2) "ಯಾವುದೇ ಮಾತು ಪತ್ತೆಯಾಗಿಲ್ಲ" else "No speech detected"
                    SpeechRecognizer.ERROR_NETWORK -> if (isKn2) "ನೆಟ್‌ವರ್ಕ್ ದೋಷ. ನಿಮ್ಮ ಸಂಪರ್ಕ ಪರಿಶೀಲಿಸಿ." else "Network error. Check your connection and try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> if (isKn2) "ಅನುಮತಿ ನಿರಾಕರಿಸಲಾಗಿದೆ" else "Permission denied"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> if (isKn2) "ಗುರುತಿಸುವಿಕೆ ಕಾರ್ಯನಿರತ, ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ" else "Recognizer busy, please try again"
                    SpeechRecognizer.ERROR_SERVER -> if (isKn2) "ಸರ್ವರ್ ದೋಷ" else "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (isKn2) "ಯಾವುದೇ ಮಾತು ಪತ್ತೆಯಾಗಿಲ್ಲ" else "No speech detected"
                    else -> if (isKn2) "ದೋಷ ಸಂಭವಿಸಿದೆ ($error)" else "Error occurred ($error)"
                }
                tvStatus.text = message

                // Recreate recognizer — it gets stuck after errors on many devices
                setupSpeechRecognizer()
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                btnMic.setBackgroundResource(R.drawable.mic_background_circle)

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    val currentText = etSpeechResult.text.toString()
                    val newText = if (currentText.isEmpty()) text else "$currentText $text"
                    etSpeechResult.setText(newText)
                    etSpeechResult.setSelection(newText.length)
                    tvStatus.text = if (LanguageManager.isKannada()) "ಮಾತನಾಡಲು ಮೈಕ್ರೋಫೋನ್ ಒತ್ತಿ" else "Tap microphone to speak"
                } else {
                    tvStatus.text = if (LanguageManager.isKannada()) "ಯಾವುದೇ ಪಠ್ಯ ಗುರುತಿಸಲಾಗಿಲ್ಲ" else "No text recognized"
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        if (speechRecognizer == null) {
            setupSpeechRecognizer()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kn-IN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            if (isOfflineMode) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            tvStatus.text = if (LanguageManager.isKannada()) "ಪ್ರಾರಂಭಿಸಲಾಗುತ್ತಿದೆ..." else "Initializing..."
        } catch (e: Exception) {
            val msg = if (LanguageManager.isKannada())
                "ಧ್ವನಿ ಗುರುತಿಸುವಿಕೆ ಪ್ರಾರಂಭಿಸುವಲ್ಲಿ ದೋಷ: ${e.message}"
            else
                "Error starting speech recognition: ${e.message}"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            isListening = false
        }
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        tvStatus.text = if (LanguageManager.isKannada()) "ಮಾತನಾಡಲು ಮೈಕ್ರೋಫೋನ್ ಒತ್ತಿ" else "Tap microphone to speak"
        btnMic.setBackgroundResource(R.drawable.mic_background_circle)
    }
}
