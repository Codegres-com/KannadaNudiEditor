package com.kannadanudi.keyboard

import java.lang.StringBuilder

enum class KeyboardLayout {
    Nudi,
    Baraha,
    English
}

data class TransliterationResult(val text: String, val backspaceCount: Int)

class TransliterationEngine {
    private val buffer = StringBuilder()
    var currentLayout = KeyboardLayout.Baraha

    // Nudi Map (Direct Layout) - Independent Chars
    private val nudiMap = mapOf(
        // Top Row
        "q" to "ಟ", "Q" to "ಠ",
        "w" to "ಡ", "W" to "ಢ",
        "e" to "ಎ", "E" to "ಏ",
        "r" to "ರ", "R" to "ಋ",
        "t" to "ತ", "T" to "ಥ",
        "y" to "ಯ", "Y" to "ಐ",
        "u" to "ಉ", "U" to "ಊ",
        "i" to "ಇ", "I" to "ಈ",
        "o" to "ಒ", "O" to "ಓ",
        "p" to "ಪ", "P" to "ಫ",

        // Middle Row
        "a" to "ಅ", "A" to "ಆ",
        "s" to "ಸ", "S" to "ಶ",
        "d" to "ದ", "D" to "ಧ",
        "f" to "್", "F" to "್",
        "g" to "ಗ", "G" to "ಘ",
        "h" to "ಹ", "H" to "ಃ", // Visarga
        "j" to "ಜ್", "J" to "ಝ್",
        // User list J=ಝ (Direct). Wait, my previous write said "j" to "ಜ್" (Halant)?
        // I will fix it to "ಜ" and "ಝ" (Direct) as per list.
        "j" to "ಜ", "J" to "ಝ",
        "k" to "ಕ", "K" to "ಖ",
        "l" to "ಲ", "L" to "ಳ",

        // Bottom Row
        "z" to "ಞ", "Z" to "ಙ",
        "x" to "ಷ", "X" to "ಷ",
        "c" to "ಚ", "C" to "ಛ",
        "v" to "ವ", "V" to "ಔ",
        "b" to "ಬ", "B" to "ಭ",
        "n" to "ನ", "N" to "ಣ",
        "m" to "ಮ", "M" to "ಂ"
    )

    // Matra Map (Vowel Signs)
    private val nudiMatras = mapOf(
        "A" to "ಾ",
        "i" to "ಿ", "I" to "ೀ",
        "u" to "ು", "U" to "ೂ",
        "R" to "ೃ",
        "e" to "ೆ", "E" to "ೇ",
        "Y" to "ೈ", // Shift+y = I -> Matra ai
        "o" to "ೊ", "O" to "ೋ",
        "V" to "ೌ" // Shift+v = au Matra
        // 'a' has no matra (implicit)
    )

    // Baraha Map (Phonetic)
    private val barahaMap = mapOf(
        // Consonants (Halant default)
        "k" to "ಕ್", "K" to "ಖ್", "g" to "ಗ್", "G" to "ಘ್", "ng" to "ಂಗ್",
        "c" to "ಚ್", "ch" to "ಚ್", "C" to "ಛ್", "Ch" to "ಛ್", "j" to "ಜ್", "J" to "ಝ್", "nj" to "ಞ್",
        "T" to "ಟ್", "Th" to "ಠ್", "D" to "ಡ್", "Dh" to "ಢ್", "N" to "ಣ್",
        "t" to "ತ್", "th" to "ಥ್", "d" to "ದ್", "dh" to "ಧ್", "n" to "ನ್",
        "p" to "ಪ್", "P" to "ಫ್", "f" to "ಫ್", "b" to "ಬ್", "B" to "ಭ್", "m" to "ಮ್",
        "y" to "ಯ್", "r" to "ರ್", "l" to "ಲ್", "v" to "ವ್", "w" to "ವ್",
        "S" to "ಶ್", "sh" to "ಷ್", "s" to "ಸ್", "h" to "ಹ್",
        "L" to "ಳ್",

        // Vowels (Independent)
        "a" to "ಅ", "aa" to "ಆ", "A" to "ಆ",
        "i" to "ಇ", "ii" to "ಈ", "I" to "ಈ",
        "u" to "ಉ", "uu" to "ಊ", "U" to "ಊ",
        "R" to "ಋ", "Ru" to "ಋ",
        "e" to "ಎ", "ee" to "ಏ", "E" to "ಏ",
        "ai" to "ಐ",
        "o" to "ಒ", "oo" to "ಓ", "O" to "ಓ",
        "au" to "ಔ", "ou" to "ಔ",

        // Modifiers
        "M" to "ಂ", "H" to "ಃ"
    )

    private val barahaVowelSigns = mapOf(
        "a" to "",
        "aa" to "ಾ", "A" to "ಾ",
        "i" to "ಿ",
        "ii" to "ೀ", "I" to "ೀ",
        "u" to "ು",
        "uu" to "ೂ", "U" to "ೂ",
        "R" to "ೃ", "Ru" to "ೃ",
        "e" to "ೆ",
        "ee" to "ೇ", "E" to "ೇ",
        "ai" to "ೈ",
        "o" to "ೊ",
        "oo" to "ೋ", "O" to "ೋ",
        "au" to "ೌ", "ou" to "ೌ"
    )

    fun setLayout(layout: KeyboardLayout) {
        currentLayout = layout
        clearBuffer()
    }

    fun clearBuffer() {
        buffer.setLength(0)
    }

    fun removeLast() {
        if (buffer.isNotEmpty()) {
            buffer.setLength(buffer.length - 1)
        }
    }

    fun getTransliteration(key: String, lastCommittedChar: Char? = null): TransliterationResult {
        if (key.isEmpty()) return TransliterationResult("", 0)

        return when (currentLayout) {
            KeyboardLayout.Nudi -> getNudiTransliteration(key, lastCommittedChar)
            KeyboardLayout.Baraha -> getBarahaTransliteration(key)
            KeyboardLayout.English -> {
                buffer.setLength(0)
                TransliterationResult(key, 0)
            }
        }
    }

    private fun getNudiTransliteration(key: String, lastCommittedChar: Char?): TransliterationResult {
        // Direct Mapping with Matra Composition Context

        // 1. Check if key is a vowel that should become a Matra
        if (lastCommittedChar != null && isKannadaConsonant(lastCommittedChar) && nudiMatras.containsKey(key)) {
            // Return Matra
            return TransliterationResult(nudiMatras[key]!!, 0)
        }

        // 2. Default: Map to Independent Char
        if (nudiMap.containsKey(key)) {
            buffer.setLength(0)
            return TransliterationResult(nudiMap[key]!!, 0)
        }

        // Pass through if not found
        buffer.setLength(0)
        return TransliterationResult(key, 0)
    }

    private fun isKannadaConsonant(c: Char): Boolean {
        // Range for Kannada Consonants: 0x0C95 (ka) to 0x0CB9 (ha)
        // This covers most consonants.
        // Also includes 0x0CB3 (Lla)
        // Excludes 0x0C82..0x0C94 (Vowels, signs)
        // Excludes 0x0CBE.. (Matras)

        val code = c.code
        return (code in 0x0C95..0x0CB9)
    }

    private fun getBarahaTransliteration(key: String): TransliterationResult {
        val combinedKey = buffer.toString() + key

        if (buffer.isNotEmpty()) {
            // 1. Try to match longest sequence backwards for VOWEL MODIFIERS on CONSONANTS
            for (i in combinedKey.length - 1 downTo 0) {
                val potentialConsonantToken = combinedKey.substring(0, i)
                val potentialVowelToken = combinedKey.substring(i)

                if (barahaMap.containsKey(potentialConsonantToken) && isBarahaConsonant(potentialConsonantToken)) {
                    if (barahaVowelSigns.containsKey(potentialVowelToken)) {
                        // Found C+V combo
                        val previousOutput = recalculateOutput(buffer.toString())

                        val consChar = barahaMap[potentialConsonantToken]!!
                        val baseChar = consChar.trimEnd('\u0CCD')
                        val matra = barahaVowelSigns[potentialVowelToken]!!

                        val replacement = baseChar + matra

                        buffer.append(key)
                        return TransliterationResult(replacement, previousOutput.length)
                    }
                }
            }

            // 2. Check if combined key is a valid Consonant or Vowel (Extension)
            if (barahaMap.containsKey(combinedKey)) {
                val previousOutput = recalculateOutput(buffer.toString())
                buffer.setLength(0)
                buffer.append(combinedKey)
                return TransliterationResult(barahaMap[combinedKey]!!, previousOutput.length)
            }
        }

        // Standard processing
        if (barahaMap.containsKey(key)) {
            buffer.setLength(0)
            buffer.append(key)
            return TransliterationResult(barahaMap[key]!!, 0)
        }

        // Unmapped
        buffer.setLength(0)
        buffer.append(key)
        return TransliterationResult(key, 0)
    }

    private fun recalculateOutput(bufferKeys: String): String {
        if (bufferKeys.isEmpty()) return ""

        // Try to match C+V
        for (i in bufferKeys.length - 1 downTo 0) {
            val c = bufferKeys.substring(0, i)
            val v = bufferKeys.substring(i)

            if (barahaMap.containsKey(c) && isBarahaConsonant(c) && barahaVowelSigns.containsKey(v)) {
                val cons = barahaMap[c]!!
                val baseChar = cons.trimEnd('\u0CCD')
                val matra = barahaVowelSigns[v]!!
                return baseChar + matra
            }
        }

        // Try match full key
        if (barahaMap.containsKey(bufferKeys)) {
            return barahaMap[bufferKeys]!!
        }

        return ""
    }

    private fun isBarahaConsonant(k: String): Boolean {
        val value = barahaMap[k] ?: return false
        return value.endsWith("\u0CCD")
    }
}
