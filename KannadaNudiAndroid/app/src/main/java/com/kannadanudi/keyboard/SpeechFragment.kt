package com.kannadanudi.keyboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class SpeechFragment : Fragment() {

    private lateinit var btnMic: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var etSpeechResult: EditText
    private lateinit var btnCopy: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    // Request permission using ActivityResultContracts
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, start listening
            startListening()
        } else {
            // Permission denied, show a message
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

        // Setup Speech Recognizer
        setupSpeechRecognizer()

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

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    tvStatus.text = "Listening..."
                    btnMic.setBackgroundResource(R.drawable.mic_background_circle_active)
                }

                override fun onBeginningOfSpeech() {
                    tvStatus.text = "Listening..."
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // Visual feedback for volume could go here
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    tvStatus.text = "Processing..."
                    isListening = false
                    btnMic.setBackgroundResource(R.drawable.mic_background_circle)
                }

                override fun onError(error: Int) {
                    isListening = false
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                        SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Service busy"
                        SpeechRecognizer.ERROR_SERVER -> "Server error"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                        else -> "Error occurred: $error"
                    }
                    tvStatus.text = message
                    btnMic.setBackgroundResource(R.drawable.mic_background_circle)
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    btnMic.setBackgroundResource(R.drawable.mic_background_circle)

                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        // Append to existing text with a space
                        val currentText = etSpeechResult.text.toString()
                        val newText = if (currentText.isEmpty()) text else "$currentText $text"

                        etSpeechResult.setText(newText)
                        etSpeechResult.setSelection(newText.length) // Move cursor to end
                        tvStatus.text = "Tap microphone to speak"
                    } else {
                        tvStatus.text = "No text recognized"
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    // Handle partial results if needed
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            tvStatus.text = "Speech Recognition not available on this device"
            btnMic.isEnabled = false
            btnMic.alpha = 0.5f
        }
    }

    private fun checkPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            // Request permission
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startListening() {
        if (speechRecognizer == null) {
            Toast.makeText(context, "Speech recognizer not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "kn-IN") // Kannada India
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
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
