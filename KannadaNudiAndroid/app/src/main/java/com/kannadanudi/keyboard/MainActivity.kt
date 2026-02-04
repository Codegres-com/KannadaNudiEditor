package com.kannadanudi.keyboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private val keyboardFragment = KeyboardFragment()
    private val editorFragment = EditorFragment()
    private var activeFragment: Fragment = keyboardFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Initial setup
        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, editorFragment, "EDITOR").hide(editorFragment)
            add(R.id.fragment_container, keyboardFragment, "KEYBOARD")
            commit()
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_keyboard -> {
                    showFragment(keyboardFragment)
                    true
                }
                R.id.navigation_editor -> {
                    showFragment(editorFragment)
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
