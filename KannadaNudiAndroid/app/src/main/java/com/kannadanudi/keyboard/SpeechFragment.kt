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
        speechRecognizer?.destroy()
        speechRecognizer = null

        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            tvStatus.text = "Speech Recognition not available on this device"
            btnMic.isEnabled = false
            btnMic.alpha = 0.5f
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
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
