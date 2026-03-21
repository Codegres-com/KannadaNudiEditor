package com.kannadanudi.keyboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var keyboardFragment: Fragment
    private lateinit var speechFragment: Fragment
    private lateinit var editorFragment: Fragment
    private lateinit var translateFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

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
