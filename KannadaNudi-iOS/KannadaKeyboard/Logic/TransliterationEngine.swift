import Foundation

enum KeyboardLayout {
    case nudi
    case baraha
    case english
}

struct TransliterationResult {
    let text: String
    let backspaceCount: Int
}

class TransliterationEngine {
    private var buffer = ""
    var currentLayout: KeyboardLayout = .baraha

    // MARK: - Maps

    // Nudi Map (Direct Layout) - Independent Chars
    private let nudiMap: [String: String] = [
        // Top Row
        "q": "ಟ", "Q": "ಠ",
        "w": "ಡ", "W": "ಢ",
        "e": "ಎ", "E": "ಏ",
        "r": "ರ", "R": "ಋ",
        "t": "ತ", "T": "ಥ",
        "y": "ಯ", "Y": "ಐ",
        "u": "ಉ", "U": "ಊ",
        "i": "ಇ", "I": "ಈ",
        "o": "ಒ", "O": "ಓ",
        "p": "ಪ", "P": "ಫ",

        // Middle Row
        "a": "ಅ", "A": "ಆ",
        "s": "ಸ", "S": "ಶ",
        "d": "ದ", "D": "ಧ",
        "f": "್", "F": "್",
        "g": "ಗ", "G": "ಘ",
        "h": "ಹ", "H": "ಃ", // Visarga
        "j": "ಜ", "J": "ಝ",
        "k": "ಕ", "K": "ಖ",
        "l": "ಲ", "L": "ಳ",

        // Bottom Row
        "z": "ಞ", "Z": "ಙ",
        "x": "ಷ", "X": "ಷ",
        "c": "ಚ", "C": "ಛ",
        "v": "ವ", "V": "ಔ",
        "b": "ಬ", "B" : "ಭ",
        "n": "ನ", "N": "ಣ",
        "m": "ಮ", "M": "ಂ"
    ]

    // Matra Map (Vowel Signs)
    private let nudiVowelSigns: [String: String] = [
        "A": "ಾ",
        "i": "ಿ", "I": "ೀ",
        "u": "ು", "U": "ೂ",
        "R": "ೃ",
        "e": "ೆ", "E": "ೇ",
        "Y": "ೈ", // Shift+y = I -> Matra ai
        "o": "ೊ", "O": "ೋ",
        "V": "ೌ" // Shift+v = au Matra
        // 'a' has no matra (implicit)
    ]

    // Baraha Map (Phonetic)
    private let barahaMap: [String: String] = [
        // Consonants (Halant default)
        "k": "ಕ್", "K": "ಖ್", "g": "ಗ್", "G": "ಘ್", "ng": "ಂಗ್",
        "c": "ಚ್", "ch": "ಚ್", "C": "ಛ್", "Ch": "ಛ್", "j": "ಜ್", "J": "ಝ್", "nj": "ಞ್",
        "T": "ಟ್", "Th": "ಠ್", "D": "ಡ್", "Dh": "ಢ್", "N": "ಣ್",
        "t": "ತ್", "th": "ಥ್", "d": "ದ್", "dh": "ಧ್", "n": "ನ್",
        "p": "ಪ್", "P": "ಫ್", "f": "ಫ್", "b": "ಬ್", "B": "ಭ್", "m": "ಮ್",
        "y": "ಯ್", "r": "ರ್", "l": "ಲ್", "v": "ವ್", "w": "ವ್",
        "S": "ಶ್", "sh": "ಷ್", "s": "ಸ್", "h": "ಹ್",
        "L": "ಳ್",

        // Vowels (Independent)
        "a": "ಅ", "aa": "ಆ", "A": "ಆ",
        "i": "ಇ", "ii": "ಈ", "I": "ಈ",
        "u": "ಉ", "uu": "ಊ", "U": "ಊ",
        "R": "ಋ", "Ru": "ಋ",
        "e": "ಎ", "ee": "ಏ", "E": "ಏ",
        "ai": "ಐ",
        "o": "ಒ", "oo": "ಓ", "O": "ಓ",
        "au": "ಔ", "ou": "ಔ",

        // Modifiers
        "M": "ಂ", "H": "ಃ"
    ]

    private let barahaVowelSigns: [String: String] = [
        "a": "",
        "aa": "ಾ", "A": "ಾ",
        "i": "ಿ",
        "ii": "ೀ", "I": "ೀ",
        "u": "ು",
        "uu": "ೂ", "U": "ೂ",
        "R": "ೃ", "Ru": "ೃ",
        "e": "ೆ",
        "ee": "ೇ", "E": "ೇ",
        "ai": "ೈ",
        "o": "ೊ",
        "oo": "ೋ", "O": "ೋ",
        "au": "ೌ", "ou": "ೌ"
    ]

    // MARK: - Methods

    func setLayout(_ layout: KeyboardLayout) {
        currentLayout = layout
        clearBuffer()
    }

    func clearBuffer() {
        buffer = ""
    }

    func removeLast() {
        if !buffer.isEmpty {
            buffer.removeLast()
        }
    }

    func getTransliteration(key: String, lastCommittedChar: Character? = nil) -> TransliterationResult {
        if key.isEmpty {
          return TransliterationResult(text: "", backspaceCount: 0)
        }

        switch currentLayout {
        case .nudi:
            return getNudiTransliteration(key: key, lastCommittedChar: lastCommittedChar)
        case .baraha:
            return getBarahaTransliteration(key: key)
        case .english:
            buffer = ""
            return TransliterationResult(text: key, backspaceCount: 0)
        }
    }

    private func getNudiTransliteration(key: String, lastCommittedChar: Character?) -> TransliterationResult {
        // Direct Mapping with Matra Composition Context

        // 1. Check if key is a vowel that should become a Matra
        if let last = lastCommittedChar, isKannadaConsonant(last), let matra = nudiVowelSigns[key] {
            return TransliterationResult(text: matra, backspaceCount: 0)
        }

        // 2. Default: Map to Independent Char
        if let val = nudiMap[key] {
            buffer = ""
            return TransliterationResult(text: val, backspaceCount: 0)
        }

        // Pass through if not found
        buffer = ""
        return TransliterationResult(text: key, backspaceCount: 0)
    }

    private func isKannadaConsonant(_ c: Character) -> Bool {
        // Range for Kannada Consonants: 0x0C95 (ka) to 0x0CB9 (ha)
        guard let scalar = c.unicodeScalars.first else { return false }
        let code = scalar.value
        return (code >= 0x0C95 && code <= 0x0CB9)
    }

    private func getBarahaTransliteration(key: String) -> TransliterationResult {
        let combinedKey = buffer + key

        if !buffer.isEmpty {
            // 1. Try to match longest sequence backwards for VOWEL MODIFIERS on CONSONANTS
            let combinedCount = combinedKey.count

            for i in (0..<combinedCount).reversed() {
                let index = combinedKey.index(combinedKey.startIndex, offsetBy: i)
                let potentialConsonantToken = String(combinedKey[..<index])
                let potentialVowelToken = String(combinedKey[index...])

                if let consChar = barahaMap[potentialConsonantToken], isBarahaConsonant(potentialConsonantToken) {
                    if let matra = barahaVowelSigns[potentialVowelToken] {
                        // Found C+V combo
                        let previousOutput = recalculateOutput(bufferKeys: buffer)
                        let baseChar = consChar.trimmingCharacters(in: CharacterSet(charactersIn: "\u{0CCD}"))
                        let replacement = baseChar + matra

                        buffer.append(key)
                        return TransliterationResult(text: replacement, backspaceCount: previousOutput.count)
                    }
                }
            }

            // 2. Check if combined key is a valid Consonant or Vowel (Extension)
            if let val = barahaMap[combinedKey] {
                let previousOutput = recalculateOutput(bufferKeys: buffer)
                buffer = ""
                buffer.append(combinedKey)
                return TransliterationResult(text: val, backspaceCount: previousOutput.count)
            }
        }

        // Standard processing
        if let val = barahaMap[key] {
            buffer = ""
            buffer.append(key)
            return TransliterationResult(text: val, backspaceCount: 0)
        }

        // Unmapped
        buffer = ""
        buffer.append(key)
        return TransliterationResult(text: key, backspaceCount: 0)
    }

    private func recalculateOutput(bufferKeys: String) -> String {
        if bufferKeys.isEmpty { return "" }

        let count = bufferKeys.count
        for i in (0..<count).reversed() {
            let index = bufferKeys.index(bufferKeys.startIndex, offsetBy: i)
            let c = String(bufferKeys[..<index])
            let v = String(bufferKeys[index...])

            if let cons = barahaMap[c], isBarahaConsonant(c), let matra = barahaVowelSigns[v] {
                let baseChar = cons.trimmingCharacters(in: CharacterSet(charactersIn: "\u{0CCD}"))
                return baseChar + matra
            }
        }

        if let val = barahaMap[bufferKeys] {
            return val
        }

        return ""
    }

    private func isBarahaConsonant(_ k: String) -> Bool {
        guard let value = barahaMap[k] else { return false }
        return value.hasSuffix("\u{0CCD}")
    }
}

