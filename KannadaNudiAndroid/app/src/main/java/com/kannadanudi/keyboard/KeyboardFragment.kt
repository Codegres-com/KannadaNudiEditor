package com.kannadanudi.keyboard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class KeyboardFragment : Fragment(), LanguageManager.OnLanguageChangeListener {

    private lateinit var btnEnable: Button
    private lateinit var tvWelcome: TextView
    private lateinit var tvIntroMessage: TextView
    private lateinit var tvIntroAction: TextView
    private lateinit var tvInstructions: TextView
    private lateinit var tvSteps: TextView
    private lateinit var tvMicTitle: TextView
    private lateinit var tvMicStatus: TextView
    private lateinit var btnGrantMic: Button

    private val requestMicPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        updateMicPermissionUI(isGranted)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_keyboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnEnable = view.findViewById(R.id.btnEnable)
        tvWelcome = view.findViewById(R.id.tvWelcome)
        tvIntroMessage = view.findViewById(R.id.tvIntroMessage)
        tvIntroAction = view.findViewById(R.id.tvIntroAction)
        tvInstructions = view.findViewById(R.id.tvInstructions)
        tvSteps = view.findViewById(R.id.tvSteps)
        tvMicTitle = view.findViewById(R.id.tvMicTitle)
        tvMicStatus = view.findViewById(R.id.tvMicStatus)
        btnGrantMic = view.findViewById(R.id.btnGrantMic)

        btnGrantMic.setOnClickListener {
            requestMicPermission.launch(Manifest.permission.RECORD_AUDIO)
        }

        LanguageManager.addListener(this)
        applyLanguage()
    }

    override fun onResume() {
        super.onResume()
        checkKeyboardStatus()
        updateMicPermissionUI(isMicPermissionGranted())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LanguageManager.removeListener(this)
    }

    override fun onLanguageChanged(language: String) {
        if (isAdded) {
            applyLanguage()
            checkKeyboardStatus()
            updateMicPermissionUI(isMicPermissionGranted())
        }
    }

    private fun applyLanguage() {
        val isKn = LanguageManager.isKannada()

        tvWelcome.text = if (isKn)
            "ಕನ್ನಡ ನುಡಿ ಕೀಬೋರ್ಡ್\u200Cಗೆ ಸುಸ್ವಾಗತ"
        else
            "Welcome to Kannada Nudi Keyboard"

        tvIntroMessage.text = if (isKn)
            "ಎಲ್ಲಾ ಅಪ್ಲಿಕೇಶನ್\u200Cಗಳಲ್ಲಿ ಕನ್ನಡ ಕೀಬೋರ್ಡ್ ಪಡೆಯಲು,"
        else
            "In Order to get a Kannada Keyboard across all Apps,"

        tvIntroAction.text = if (isKn)
            "ಇಲ್ಲಿ ಇನ್\u200Cಪುಟ್ ವಿಧಾನವನ್ನು ಕನ್ನಡ ನುಡಿ ಕೀಬೋರ್ಡ್\u200Cಗೆ ಬದಲಾಯಿಸಿ"
        else
            "Switch Input method here to Kannada Nudi Keyboard"

        tvMicTitle.text = if (isKn) "ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ" else "Microphone Permission"
    }

    private fun isMicPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateMicPermissionUI(granted: Boolean) {
        val isKn = LanguageManager.isKannada()
        if (granted) {
            tvMicStatus.text = if (isKn)
                "ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ನೀಡಲಾಗಿದೆ — ಧ್ವನಿ ಇನ್\u200Cಪುಟ್ ಸಕ್ರಿಯವಾಗಿದೆ."
            else
                "Microphone permission granted — voice input is enabled."
            btnGrantMic.visibility = View.GONE
        } else {
            tvMicStatus.text = if (isKn)
                "ಕೀಬೋರ್ಡ್\u200Cನಲ್ಲಿ ಧ್ವನಿ ಇನ್\u200Cಪುಟ್\u200Cಗೆ ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ಅಗತ್ಯವಿದೆ."
            else
                "Microphone permission is needed for voice input on the keyboard."
            btnGrantMic.visibility = View.VISIBLE
            btnGrantMic.text = if (isKn) "ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ನೀಡಿ" else "Grant Microphone Permission"
        }
    }

    private fun checkKeyboardStatus() {
        val context = context ?: return
        val isKn = LanguageManager.isKannada()
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val list = imm.enabledInputMethodList

        val isEnabled = list.any { it.packageName == context.packageName }

        val currentId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isSelected = currentId != null && currentId.contains(context.packageName)

        if (!isEnabled) {
            tvInstructions.text = if (isKn)
                "ಈ ಕೀಬೋರ್ಡ್ ಬಳಸಲು, ನೀವು ಸಿಸ್ಟಮ್ ಸೆಟ್ಟಿಂಗ್\u200Cಗಳಲ್ಲಿ ಅದನ್ನು ಸಕ್ರಿಯಗೊಳಿಸಬೇಕು."
            else
                "To use this keyboard, you must enable it in System Settings."
            tvSteps.visibility = View.VISIBLE
            tvSteps.text = if (isKn)
                "1. ಕೆಳಗಿನ ಬಟನ್ ಕ್ಲಿಕ್ ಮಾಡಿ.\n2. 'ಕನ್ನಡ ನುಡಿ ಕೀಬೋರ್ಡ್' ಅನ್ನು ON ಮಾಡಿ.\n3. ಈ ಅಪ್ಲಿಕೇಶನ್\u200Cಗೆ ಹಿಂತಿರುಗಿ."
            else
                "1. Click the button below.\n2. Toggle 'Kannada Nudi Keyboard' to ON.\n3. Return to this app."
            btnEnable.text = if (isKn) "ಸೆಟ್ಟಿಂಗ್\u200Cಗಳಲ್ಲಿ ಕೀಬೋರ್ಡ್ ಸಕ್ರಿಯಗೊಳಿಸಿ" else "Enable Keyboard in Settings"
            btnEnable.isEnabled = true
            btnEnable.setOnClickListener {
                startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
            }
        } else if (!isSelected) {
            tvInstructions.text = if (isKn)
                "ಒಳ್ಳೆಯದು! ಈಗ ಕನ್ನಡ ನುಡಿ ಅನ್ನು ನಿಮ್ಮ ಸಕ್ರಿಯ ಕೀಬೋರ್ಡ್ ಆಗಿ ಆಯ್ಕೆ ಮಾಡಿ."
            else
                "Great! Now select Kannada Nudi as your active keyboard."
            tvSteps.visibility = View.VISIBLE
            tvSteps.text = if (isKn)
                "1. ಕೆಳಗಿನ ಬಟನ್ ಕ್ಲಿಕ್ ಮಾಡಿ.\n2. ಪಟ್ಟಿಯಿಂದ 'ಕನ್ನಡ ನುಡಿ ಕೀಬೋರ್ಡ್' ಅನ್ನು ಆಯ್ಕೆ ಮಾಡಿ."
            else
                "1. Click the button below.\n2. Select 'Kannada Nudi Keyboard' from the list."
            btnEnable.text = if (isKn) "ಇನ್\u200Cಪುಟ್ ವಿಧಾನ ಬದಲಾಯಿಸಿ" else "Switch Input Method"
            btnEnable.isEnabled = true
            btnEnable.setOnClickListener {
                imm.showInputMethodPicker()
            }
        } else {
            tvInstructions.text = if (isKn)
                "ನೀವು ಸಿದ್ಧರಾಗಿದ್ದೀರಿ! ಕೀಬೋರ್ಡ್ ಬಳಸಲು ಸಿದ್ಧವಾಗಿದೆ."
            else
                "You are all set! The keyboard is ready to use."
            tvSteps.visibility = View.GONE
            btnEnable.text = if (isKn) "ಕೀಬೋರ್ಡ್ ಸಿದ್ಧವಾಗಿದೆ" else "Keyboard is Ready"
            btnEnable.isEnabled = false
            btnEnable.setOnClickListener(null)
        }
    }
}
