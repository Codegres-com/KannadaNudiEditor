package com.kannadanudi.keyboard

import android.content.Context
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment

class EditorFragment : Fragment(), LanguageManager.OnLanguageChangeListener {

    private lateinit var webView: WebView
    private lateinit var btnSwitchKeyboard: Button
    private lateinit var overlaySwitch: LinearLayout
    private lateinit var tvOverlayTitle: TextView
    private lateinit var tvOverlayMessage: TextView
    private lateinit var tvOverlayHint: TextView

    // Handler for periodic keyboard check
    private val handler = Handler(Looper.getMainLooper())
    private val keyboardCheckRunnable = object : Runnable {
        override fun run() {
            checkKeyboardStatus()
            handler.postDelayed(this, 1500) // Check every 1.5 seconds
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_editor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        webView = view.findViewById(R.id.webView)
        overlaySwitch = view.findViewById(R.id.overlaySwitch)
        tvOverlayTitle = view.findViewById(R.id.tvOverlayTitle)
        tvOverlayMessage = view.findViewById(R.id.tvOverlayMessage)
        tvOverlayHint = view.findViewById(R.id.tvOverlayHint)

        // Configure WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true

        // Enable Zoom (Touch Controls)
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false // Hide the old zoom buttons

        // Improve Layout for Mobile
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true

        // Enable Scrollbars (enabled by default, but ensuring settings)
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if (url.contains("nudiweb.com")) {
                    return false
                }
                return true
            }
        }

        if (webView.url == null) {
            webView.loadUrl("https://nudiweb.com")
        }

        btnSwitchKeyboard = view.findViewById(R.id.btnSwitchKeyboard)
        btnSwitchKeyboard.setOnClickListener {
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showInputMethodPicker()
        }

        LanguageManager.addListener(this)
        applyLanguage()
    }

    override fun onResume() {
        super.onResume()
        handler.post(keyboardCheckRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(keyboardCheckRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LanguageManager.removeListener(this)
    }

    override fun onLanguageChanged(language: String) {
        if (isAdded) {
            applyLanguage()
        }
    }

    private fun applyLanguage() {
        val isKn = LanguageManager.isKannada()

        tvOverlayTitle.text = if (isKn)
            "ಕನ್ನಡ ನುಡಿ ಕೀಬೋರ್ಡ್ ಪತ್ತೆಯಾಗಿದೆ"
        else
            "Kannada Nudi Keyboard Detected"

        tvOverlayMessage.text = if (isKn)
            "ಸಂಪಾದಕ (WebView) ಇಂಗ್ಲಿಷ್ ಇನ್\u200Cಪುಟ್ ಮಾತ್ರ ಸ್ವೀಕರಿಸುತ್ತದೆ.\nಟೈಪ್ ಮಾಡಲು ಇಂಗ್ಲಿಷ್ ಕೀಬೋರ್ಡ್\u200Cಗೆ (ಉದಾ. Gboard) ಬದಲಾಯಿಸಿ."
        else
            "The Editor (WebView) only accepts English input.\nPlease switch to an English keyboard (e.g. Gboard) to continue typing."

        btnSwitchKeyboard.text = if (isKn)
            "🔄  ಇನ್\u200Cಪುಟ್ ವಿಧಾನ ಬದಲಾಯಿಸಿ"
        else
            "🔄  Switch Input Method"

        tvOverlayHint.text = if (isKn)
            "ಪಿಕರ್\u200Cನಿಂದ Gboard ಅಥವಾ ಯಾವುದೇ ಇಂಗ್ಲಿಷ್ ಕೀಬೋರ್ಡ್ ಆಯ್ಕೆ ಮಾಡಿ"
        else
            "Select Gboard or any English keyboard from the picker"
    }

    private fun checkKeyboardStatus() {
        if (!isAdded) return
        
        val context = context ?: return
        val currentId = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        
        val isNudiKeyboardActive = currentId != null && currentId.contains(context.packageName)

        if (isNudiKeyboardActive) {
            // Kannada Nudi Keyboard is active — block WebView and prompt to switch
            if (overlaySwitch.visibility != View.VISIBLE) {
                overlaySwitch.visibility = View.VISIBLE
                // Auto-open the input method picker
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        } else {
            // English / other keyboard is active — allow normal usage
            if (overlaySwitch.visibility != View.GONE) {
                overlaySwitch.visibility = View.GONE
            }
        }
    }
}
