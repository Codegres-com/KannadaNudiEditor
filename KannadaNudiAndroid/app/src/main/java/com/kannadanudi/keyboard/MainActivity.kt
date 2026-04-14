package com.kannadanudi.keyboard

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), LanguageManager.OnLanguageChangeListener {

    private lateinit var keyboardFragment: Fragment
    private lateinit var speechFragment: Fragment
    private lateinit var editorFragment: Fragment
    private lateinit var translateFragment: Fragment
    private lateinit var activeFragment: Fragment
    private lateinit var btnLanguageToggle: TextView
    private lateinit var tvAppTitle: TextView
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize language manager
        LanguageManager.init(this)
        LanguageManager.addListener(this)

        btnLanguageToggle = findViewById(R.id.btnLanguageToggle)
        tvAppTitle = findViewById(R.id.tvAppTitle)
        bottomNav = findViewById(R.id.bottom_navigation)

        btnLanguageToggle.setOnClickListener {
            LanguageManager.toggleLanguage()
        }

        if (savedInstanceState == null) {
            keyboardFragment = KeyboardFragment()
            speechFragment = SpeechFragment()
            editorFragment = EditorFragment()
            translateFragment = TranslateFragment()

            // Set KeyboardFragment as default
            activeFragment = keyboardFragment

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, speechFragment, "SPEECH").hide(speechFragment)
                add(R.id.fragment_container, editorFragment, "EDITOR").hide(editorFragment)
                add(R.id.fragment_container, translateFragment, "TRANSLATE").hide(translateFragment)
                add(R.id.fragment_container, keyboardFragment, "KEYBOARD")
                commit()
            }
            bottomNav.selectedItemId = R.id.navigation_keyboard
        } else {
            keyboardFragment = supportFragmentManager.findFragmentByTag("KEYBOARD")!!
            speechFragment = supportFragmentManager.findFragmentByTag("SPEECH")!!
            editorFragment = supportFragmentManager.findFragmentByTag("EDITOR")!!
            translateFragment = supportFragmentManager.findFragmentByTag("TRANSLATE")!!

            // Determine active fragment
            activeFragment = if (!keyboardFragment.isHidden) keyboardFragment
                             else if (!speechFragment.isHidden) speechFragment
                             else if (!translateFragment.isHidden) translateFragment
                             else editorFragment
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_keyboard -> {
                    showFragment(keyboardFragment)
                    true
                }
                R.id.navigation_speech -> {
                    showFragment(speechFragment)
                    true
                }
                R.id.navigation_editor -> {
                    showFragment(editorFragment)
                    true
                }
                R.id.navigation_translate -> {
                    showFragment(translateFragment)
                    true
                }
                else -> false
            }
        }

        // Apply initial language
        updateLanguageUI(LanguageManager.getCurrentLanguage())
    }

    override fun onDestroy() {
        super.onDestroy()
        LanguageManager.removeListener(this)
    }

    override fun onLanguageChanged(language: String) {
        updateLanguageUI(language)
    }

    private fun updateLanguageUI(language: String) {
        val isKannada = language == LanguageManager.KANNADA

        // Toggle button shows the OTHER language (what you can switch TO)
        btnLanguageToggle.text = if (isKannada) "English" else "ಕನ್ನಡ"
        tvAppTitle.text = if (isKannada) "ಕನ್ನಡ ನುಡಿ" else "Kannada Nudi"

        // Update bottom nav labels
        val menu = bottomNav.menu
        menu.findItem(R.id.navigation_keyboard).title = if (isKannada) "ಕೀಬೋರ್ಡ್" else "Keyboard"
        menu.findItem(R.id.navigation_speech).title = if (isKannada) "ಧ್ವನಿ" else "Speech"
        menu.findItem(R.id.navigation_editor).title = if (isKannada) "ಸಂಪಾದಕ" else "Editor"
        menu.findItem(R.id.navigation_translate).title = if (isKannada) "ಅನುವಾದ" else "Translate"
    }

    private fun showFragment(fragment: Fragment) {
        if (fragment == activeFragment) return

        supportFragmentManager.beginTransaction().apply {
            hide(activeFragment)
            show(fragment)
            commit()
        }
        activeFragment = fragment
    }
}
