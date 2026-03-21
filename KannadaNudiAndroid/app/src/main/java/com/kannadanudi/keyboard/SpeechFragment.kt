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

class SpeechFragment : Fragment() {

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
            Toast.makeText(context, "Microphone permission is required for speech recognition", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_speech, container, false)

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
                Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No text to copy", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun updateOfflineStatusText() {
        if (!isOfflineMode) {
            tvOfflineStatus.text = "Online mode — requires internet connection"
            tvOfflineStatus.setTextColor(resources.getColor(android.R.color.darker_gray, null))
            btnDownloadOfflinePack.visibility = View.GONE
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (SpeechRecognizer.isOnDeviceRecognitionAvailable(requireContext())) {
                tvOfflineStatus.text = "Offline mode — on-device Kannada recognition active"
                tvOfflineStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
                btnDownloadOfflinePack.visibility = View.GONE
            } else {
                tvOfflineStatus.text = "Kannada offline pack not installed. Download it to use offline speech."
                tvOfflineStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
                btnDownloadOfflinePack.visibility = View.VISIBLE
            }
        } else {
            tvOfflineStatus.text = "Offline mode — prefers on-device recognition (Android 12+ for full offline support)"
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

        if (isOfflineMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(requireContext())
        ) {
            speechRecognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(requireContext())
        } else {
            if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
                tvStatus.text = "Speech Recognition not available on this device"
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
                tvStatus.text = "Listening..."
                btnMic.setBackgroundResource(R.drawable.mic_background_circle_active)
            }

            override fun onBeginningOfSpeech() {
                tvStatus.text = "Listening..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                tvStatus.text = "Processing..."
                isListening = false
                btnMic.setBackgroundResource(R.drawable.mic_background_circle)
            }

            override fun onError(error: Int) {
                isListening = false
                btnMic.setBackgroundResource(R.drawable.mic_background_circle)

                val message = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error. Check your connection and try again."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy, please try again"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    else -> "Error occurred ($error)"
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
                    tvStatus.text = "Tap microphone to speak"
                } else {
                    tvStatus.text = "No text recognized"
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
            tvStatus.text = "Initializing..."
        } catch (e: Exception) {
            Toast.makeText(context, "Error starting speech recognition: ${e.message}", Toast.LENGTH_SHORT).show()
            isListening = false
        }
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        tvStatus.text = "Tap microphone to speak"
        btnMic.setBackgroundResource(R.drawable.mic_background_circle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
