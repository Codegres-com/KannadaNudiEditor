package com.kannadanudi.keyboard

import android.content.Context
import android.content.SharedPreferences

/**
 * Global language manager for switching between English and Kannada UI strings.
 * Persists the selected language in SharedPreferences.
 */
object LanguageManager {

    private const val PREF_NAME = "kannada_nudi_prefs"
    private const val KEY_LANGUAGE = "app_language"
    const val ENGLISH = "en"
    const val KANNADA = "kn"

    private lateinit var prefs: SharedPreferences
    private val listeners = mutableListOf<OnLanguageChangeListener>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getCurrentLanguage(): String {
        return prefs.getString(KEY_LANGUAGE, KANNADA) ?: KANNADA
    }

    fun isKannada(): Boolean = getCurrentLanguage() == KANNADA

    fun toggleLanguage() {
        val newLang = if (isKannada()) ENGLISH else KANNADA
        prefs.edit().putString(KEY_LANGUAGE, newLang).apply()
        notifyListeners(newLang)
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply()
        notifyListeners(lang)
    }

    fun addListener(listener: OnLanguageChangeListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: OnLanguageChangeListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(language: String) {
        listeners.forEach { it.onLanguageChanged(language) }
    }

    /**
     * Get a localized string. Pass the English and Kannada versions.
     */
    fun getString(english: String, kannada: String): String {
        return if (isKannada()) kannada else english
    }

    interface OnLanguageChangeListener {
        fun onLanguageChanged(language: String)
    }
}
