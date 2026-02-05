package com.kannadanudi.keyboard

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var keyboardFragment: Fragment
    private lateinit var editorFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        if (savedInstanceState == null) {
            keyboardFragment = KeyboardFragment()
            editorFragment = EditorFragment()
            activeFragment = editorFragment

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, keyboardFragment, "KEYBOARD").hide(keyboardFragment)
                add(R.id.fragment_container, editorFragment, "EDITOR")
                commit()
            }
            bottomNav.selectedItemId = R.id.navigation_editor
        } else {
            keyboardFragment = supportFragmentManager.findFragmentByTag("KEYBOARD")!!
            editorFragment = supportFragmentManager.findFragmentByTag("EDITOR")!!
            activeFragment = if (editorFragment.isHidden) keyboardFragment else editorFragment
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
