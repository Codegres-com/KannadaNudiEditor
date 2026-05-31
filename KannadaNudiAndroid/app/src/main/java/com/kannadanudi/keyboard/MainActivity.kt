package com.kannadanudi.keyboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity(), LanguageManager.OnLanguageChangeListener {

    private lateinit var keyboardFragment: Fragment
    private lateinit var typeFragment: Fragment
    private lateinit var translateFragment: Fragment
    private lateinit var activeFragment: Fragment
    private lateinit var btnLanguageToggle: TextView
    private lateinit var tvAppTitle: TextView
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var topBarLayout: FrameLayout
    private lateinit var btnMenu: ImageButton

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
        topBarLayout = findViewById(R.id.top_bar_layout)
        btnMenu = findViewById(R.id.btnMenu)

        btnLanguageToggle.setOnClickListener {
            LanguageManager.toggleLanguage()
        }

        if (savedInstanceState == null) {
            keyboardFragment = KeyboardFragment()
            typeFragment = TypeFragment()
            translateFragment = TranslateFragment()

            // Set TypeFragment as default
            activeFragment = typeFragment

            supportFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container, keyboardFragment, "KEYBOARD").hide(keyboardFragment)
                add(R.id.fragment_container, translateFragment, "TRANSLATE").hide(translateFragment)
                add(R.id.fragment_container, typeFragment, "TYPE")
                commit()
            }
            bottomNav.selectedItemId = R.id.navigation_type
        } else {
            keyboardFragment = supportFragmentManager.findFragmentByTag("KEYBOARD")!!
            typeFragment = supportFragmentManager.findFragmentByTag("TYPE")!!
            translateFragment = supportFragmentManager.findFragmentByTag("TRANSLATE")!!

            // Determine active fragment
            activeFragment = if (!typeFragment.isHidden) typeFragment
                             else if (!translateFragment.isHidden) translateFragment
                             else keyboardFragment
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_keyboard -> {
                    showFragment(keyboardFragment)
                    true
                }
                R.id.navigation_type -> {
                    showFragment(typeFragment)
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
        updateBarStyles(activeFragment)
    }

    override fun onDestroy() {
        super.onDestroy()
        LanguageManager.removeListener(this)
    }

    override fun onLanguageChanged(language: String) {
        updateLanguageUI(language)
        updateBarStyles(activeFragment)
    }

    private fun updateLanguageUI(language: String) {
        val isKannada = language == LanguageManager.KANNADA

        // Toggle button shows the OTHER language (what you can switch TO)
        btnLanguageToggle.text = if (isKannada) "English" else "ಕನ್ನಡ"

        // Update bottom nav labels
        val menu = bottomNav.menu
        menu.findItem(R.id.navigation_keyboard).title = if (isKannada) "ಕೀಬೋರ್ಡ್" else "Keyboard"
        menu.findItem(R.id.navigation_type).title = if (isKannada) "ಟೈಪ್" else "Type"
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
        updateBarStyles(fragment)
    }

    /**
     * Update top app bar and bottom nav styles dynamically based on the active screen.
     * Applies the premium transparent top bar and red bottom navigation app-wide.
     */
    private fun updateBarStyles(fragment: Fragment) {
        // Transparent top bar for yellow/red split screen across all screens
        topBarLayout.setBackgroundColor(Color.TRANSPARENT)
        topBarLayout.elevation = 0f
        topBarLayout.translationZ = 0f

        // Set Title text and Menu button colors to match the design (Dark Gold/Grey)
        tvAppTitle.setTextColor(Color.parseColor("#191C1E"))
        
        when (fragment) {
            keyboardFragment -> {
                tvAppTitle.text = if (LanguageManager.isKannada()) "ಕೀಬೋರ್ಡ್ ಸೆಟಪ್" else "Keyboard Setup"
            }
            typeFragment -> {
                tvAppTitle.text = if (LanguageManager.isKannada()) "ಕನ್ನಡ ಧ್ವನಿ" else "Kannada Voice"
            }
            translateFragment -> {
                tvAppTitle.text = if (LanguageManager.isKannada()) "ಅನುವಾದ" else "Translate"
            }
        }
        
        btnMenu.setColorFilter(Color.parseColor("#191C1E"))

        // Style Language Toggle
        btnLanguageToggle.setTextColor(Color.parseColor("#735C00"))
        btnLanguageToggle.setBackgroundResource(R.drawable.shape_round_pill)

        // Red bottom navigation that blends into the red half of the screen
        bottomNav.setBackgroundColor(Color.parseColor("#BB001E"))

        // Dynamic White & Yellow item state tint for navigation on red background
        val colors = intArrayOf(
            Color.parseColor("#CCCCCC"), // Unselected: light grey/white
            Color.parseColor("#FFCD00")  // Selected: Karnataka yellow
        )
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_checked),
            intArrayOf(android.R.attr.state_checked)
        )
        val colorStateList = ColorStateList(states, colors)
        bottomNav.itemIconTintList = colorStateList
        bottomNav.itemTextColor = colorStateList
    }
}

