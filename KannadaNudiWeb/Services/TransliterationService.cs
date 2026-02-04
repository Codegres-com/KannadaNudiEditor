using System.Text;
using KannadaNudiEditor.Helpers.Conversion;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;

namespace KannadaNudiWeb.Services
{
    public enum KeyboardLayout
    {
        Nudi,
        Baraha
    }

    public enum IndianLanguage
    {
        Kannada,
        Hindi,      // Devanagari
        Telugu,
        Tamil,
        Malayalam,
        Bengali,
        Gujarati,
        Odia,
        Punjabi,    // Gurmukhi
        Assamese,   // Bengali Script
        Sanskrit,   // Devanagari
        Marathi,    // Devanagari
        Nepali,     // Devanagari
        Konkani,    // Devanagari
        Bodo,       // Devanagari
        Dogri,      // Devanagari
        Maithili,   // Devanagari
        Sindhi,     // Devanagari
        Kashmiri,   // Devanagari
        Manipuri    // Bengali Script
    }

    public class TransliterationService
    {
        private readonly FileConversionService _conversionService;
        private readonly StringBuilder _buffer = new StringBuilder();

        public KeyboardLayout CurrentLayout { get; private set; } = KeyboardLayout.Nudi;
        public IndianLanguage CurrentLanguage { get; set; } = IndianLanguage.Kannada;

        // Nudi 6.1 Maps
        private readonly Dictionary<string, string> _nudiConsonants = new Dictionary<string, string>
        {
            { "k", "ಕ" }, { "K", "ಖ" }, { "g", "ಗ" }, { "G", "ಘ" }, { "Z", "ಙ" },
            { "c", "ಚ" }, { "C", "ಛ" }, { "j", "ಜ" }, { "z", "ಞ" },
            { "q", "ಟ" }, { "Q", "ಠ" }, { "w", "ಡ" }, { "W", "ಢ" }, { "N", "ಣ" },
            { "t", "ತ" }, { "T", "ಥ" }, { "d", "ದ" }, { "D", "ಧ" }, { "n", "ನ" },
            { "p", "ಪ" }, { "P", "ಫ" }, { "b", "ಬ" }, { "B", "ಭ" }, { "m", "ಮ" },
            { "y", "ಯ" }, { "r", "ರ" }, { "l", "ಲ" }, { "v", "ವ" },
            { "S", "ಶ್" }, { "x", "ಷ" }, { "s", "ಸ" }, { "h", "ಹ" }, { "L", "ಳ" }
        };

        private readonly Dictionary<string, string> _nudiVowels = new Dictionary<string, string>
        {
            { "a", "ಅ" }, { "A", "ಆ" },
            { "i", "ಇ" }, { "I", "ಈ" },
            { "u", "ಉ" }, { "U", "ಊ" },
            { "R", "ಋ" }, { "e", "ಎ" }, { "E", "ಏ" },
            { "Y", "ಐ" }, { "o", "ಒ" }, { "O", "ಓ" },
            { "V", "ಔ" },
            { "M", "ಂ" }, { "H", "ಃ" }
        };

        private readonly Dictionary<string, string> _nudiMatras = new Dictionary<string, string>
        {
            { "a", "" }, // Inherent vowel (no matra needed for Base Consonant)
            { "A", "ಾ" },
            { "i", "ಿ" }, { "I", "ೀ" },
            { "u", "ು" }, { "U", "ೂ" },
            { "R", "ೃ" },
            { "e", "ೆ" }, { "E", "ೇ" },
            { "Y", "ೈ" },
            { "o", "ೊ" }, { "O", "ೋ" },
            { "V", "ೌ" },
            { "M", "ಂ" }, { "H", "ಃ" }
        };

        private readonly Dictionary<string, string> _nudiSpecials = new Dictionary<string, string>
        {
            { "rX", "ಱ" },
            { "LX", "ೞ" },
            { "RX", "ೠ" },
            { "jX", "ಜ಼" },
            { "PX", "ಫ಼" },
            { "KX", "ಖ಼" }
        };

        private readonly Dictionary<string, string> _nudiSpecialMatras = new Dictionary<string, string>
        {
            { "RX", "ೄ" }
        };

        private readonly Dictionary<string, string> _diacritics = new Dictionary<string, string>
        {
            { "!", "\u0306" }, // Laghu (Breve)
            { "@", "\u0304" }, // Guru (Macron)
            { "#", "\u0333" }, // Double low line
            { "$", "₹" },      // Rupee (Caps+4 mapped to Shift+4 here as closest approximation)
            { "%", "\u0951" }, // Svarita
            { "^", "\u1CDA" }, // Dirgha Svarita
            { "&", "\u093C" }, // Nukta
            { "*", "\u0307" }, // Dot above
            { "(", "\u0308" }, // Two dots above
            { ")", "\u0C81" }  // Chandrabindu
        };

        private readonly Dictionary<string, string> _symbolDiacritics = new Dictionary<string, string>
        {
            { ".", "\u0324" }, // Two dots below
            { "-", "\u0332" }, // One line below
            { "'", "\u0301" }, // Acute
            { ",", "\u0327" }  // Cedilla
        };

        // Baraha Phonetic Map
        private readonly Dictionary<string, string> _barahaMap = new Dictionary<string, string>
        {
            // Consonants (Halant default)
            { "k", "ಕ್" }, { "K", "ಖ್" }, { "g", "ಗ್" }, { "G", "ಘ್" }, { "ng", "ಂಗ್" },
            { "c", "ಚ್" }, { "ch", "ಚ್" }, { "C", "ಛ್" }, { "Ch", "ಛ್" }, { "j", "ಜ್" }, { "J", "ಝ್" }, { "nj", "ಞ್" },
            { "T", "ಟ್" }, { "Th", "ಠ್" }, { "D", "ಡ್" }, { "Dh", "ಢ್" }, { "N", "ಣ್" },
            { "t", "ತ್" }, { "th", "ಥ್" }, { "d", "ದ್" }, { "dh", "ಧ್" }, { "n", "ನ್" },
            { "p", "ಪ್" }, { "P", "ಫ್" }, { "f", "ಫ್" }, { "b", "ಬ್" }, { "B", "ಭ್" }, { "m", "ಮ್" },
            { "y", "ಯ್" }, { "r", "ರ್" }, { "l", "ಲ್" }, { "v", "ವ್" }, { "w", "ವ್" },
            { "S", "ಶ್" }, { "sh", "ಷ್" }, { "s", "ಸ್" }, { "h", "ಹ್" },
            { "L", "ಳ್" },

            // Vowels (Independent)
            { "a", "ಅ" }, { "aa", "ಆ" }, { "A", "ಆ" },
            { "i", "ಇ" }, { "ii", "ಈ" }, { "I", "ಈ" },
            { "u", "ಉ" }, { "uu", "ಊ" }, { "U", "ಊ" },
            { "R", "ಋ" }, { "Ru", "ಋ" },
            { "e", "ಎ" }, { "ee", "ಏ" }, { "E", "ಏ" },
            { "ai", "ಐ" },
            { "o", "ಒ" }, { "oo", "ಓ" }, { "O", "ಓ" },
            { "au", "ಔ" }, { "ou", "ಔ" },

            // Modifiers
            { "M", "ಂ" }, { "H", "ಃ" }
        };

        // Baraha Vowel Signs (Matras)
        private readonly Dictionary<string, string> _barahaVowelSigns = new Dictionary<string, string>
        {
            { "a", "" }, // Removes Halant
            { "aa", "ಾ" }, { "A", "ಾ" },
            { "i", "ಿ" },
            { "ii", "ೀ" }, { "I", "ೀ" },
            { "u", "ು" },
            { "uu", "ೂ" }, { "U", "ೂ" },
            { "R", "ೃ" }, { "Ru", "ೃ" },
            { "e", "ೆ" },
            { "ee", "ೇ" }, { "E", "ೇ" },
            { "ai", "ೈ" },
            { "o", "ೊ" },
            { "oo", "ೋ" }, { "O", "ೋ" },
            { "au", "ೌ" }, { "ou", "ೌ" }
        };


        public TransliterationService(FileConversionService conversionService)
        {
            _conversionService = conversionService;
        }

        public async Task InitializeAsync()
        {
            await _conversionService.InitializeAsync();
        }

        public void SetLayout(KeyboardLayout layout)
        {
            CurrentLayout = layout;
            ClearBuffer();
        }

        public (string text, int backspaceCount) GetTransliteration(string key)
        {
            if (string.IsNullOrEmpty(key)) return ("", 0);

            (string text, int backspaceCount) result;

            if (CurrentLayout == KeyboardLayout.Nudi)
            {
                result = GetNudiTransliteration(key);
            }
            else
            {
                result = GetBarahaTransliteration(key);
            }

            // Convert result text to target language
            if (CurrentLanguage != IndianLanguage.Kannada && !string.IsNullOrEmpty(result.text))
            {
                result.text = ConvertFromKannada(result.text);
            }

            return result;
        }

        private string ConvertFromKannada(string text)
        {
            if (string.IsNullOrEmpty(text)) return text;

            var sb = new StringBuilder();
            int offset = GetOffsetForLanguage(CurrentLanguage);

            foreach (char c in text)
            {
                // Check if character is in Kannada block (0C80 - 0CFF)
                // Note: Generic diacritics (0300-036F) should NOT be shifted.
                if (c >= 0x0C80 && c <= 0x0CFF)
                {
                    // Apply offset
                    int newCharCode = c + offset;
                    sb.Append((char)newCharCode);
                }
                else
                {
                    sb.Append(c);
                }
            }
            return sb.ToString();
        }

        private int GetOffsetForLanguage(IndianLanguage lang)
        {
            // Kannada Base: 0x0C80
            // Devanagari Base: 0x0900 (Diff: -0x380)
            // Bengali Base: 0x0980 (Diff: -0x300)
            // Gurmukhi Base: 0x0A00 (Diff: -0x280)
            // Gujarati Base: 0x0A80 (Diff: -0x200)
            // Oriya Base: 0x0B00 (Diff: -0x180)
            // Tamil Base: 0x0B80 (Diff: -0x100)
            // Telugu Base: 0x0C00 (Diff: -0x080)
            // Malayalam Base: 0x0D00 (Diff: +0x080)

            switch (lang)
            {
                case IndianLanguage.Hindi:
                case IndianLanguage.Sanskrit:
                case IndianLanguage.Marathi:
                case IndianLanguage.Nepali:
                case IndianLanguage.Konkani:
                case IndianLanguage.Bodo:
                case IndianLanguage.Dogri:
                case IndianLanguage.Maithili:
                case IndianLanguage.Sindhi:
                case IndianLanguage.Kashmiri:
                    return -0x380; // All Devanagari

                case IndianLanguage.Bengali:
                case IndianLanguage.Assamese:
                case IndianLanguage.Manipuri:
                    return -0x300; // Bengali Script

                case IndianLanguage.Punjabi: return -0x280;
                case IndianLanguage.Gujarati: return -0x200;
                case IndianLanguage.Odia: return -0x180;
                case IndianLanguage.Tamil: return -0x100;
                case IndianLanguage.Telugu: return -0x080;
                case IndianLanguage.Malayalam: return 0x080;
                case IndianLanguage.Kannada:
                default: return 0;
            }
        }

        private (string text, int backspaceCount) GetNudiTransliteration(string key)
        {
            string combined = _buffer.ToString() + key;

            // 1. Check for 2-char specials (e.g. rX, RX, LX)
            if (combined.Length >= 2)
            {
                string lastTwo = combined.Substring(combined.Length - 2);

                // We need to know if the last keystroke (before this one) was effectively a Matra or Base context
                // to decide how to handle cases like RX (ೠ vs ೄ).
                // However, RX is usually entered after a consonant for Matra ೄ, or standalone for ೠ.

                if (_buffer.Length > 0)
                {
                    string prevKey = _buffer[_buffer.Length - 1].ToString();
                    string seq = prevKey + key;

                    if (_nudiSpecials.ContainsKey(seq))
                    {
                        // Determine if we are in a Matra context (previous char was a consonant base)
                        // If so, and if seq is RX, we might want 'ೄ' instead of 'ೠ'
                        bool prevWasMatraContext = IsNudiMatraContext(_buffer.ToString().Substring(0, _buffer.Length - 1), prevKey);

                        _buffer.Append(key);

                        if (prevWasMatraContext && _nudiSpecialMatras.ContainsKey(seq))
                        {
                            return (_nudiSpecialMatras[seq], 1); // Replace 'ೃ' with 'ೄ'
                        }
                        else
                        {
                            return (_nudiSpecials[seq], 1); // Replace Base 'ಋ' with 'ೠ' OR 'ರ' with 'ಱ'
                        }
                    }
                }
            }

            // 2. Check for Consonant Modifiers (f, F)
            if (key == "f" || key == "F")
            {
                if (_buffer.Length > 0)
                {
                    char lastKeyChar = _buffer[_buffer.Length - 1];
                    string lastKey = lastKeyChar.ToString();

                    // If last key produced a Base Consonant (which ends in 'a' implicit), we add Halant.
                    // If last key produced 'ಕ' (Base), we want 'ಕ್' (Halant).
                    // This is done by appending '್'. We do NOT backspace 'ಕ'.
                    if (_nudiConsonants.ContainsKey(lastKey))
                    {
                        _buffer.Append(key);
                        string modifier = (key == "f") ? "್" : "್\u200D";
                        return (modifier, 0);
                    }
                }
            }

            // 3. Check for Vowels (Matra vs Independent)
            if (_nudiVowels.ContainsKey(key))
            {
                // Check context
                if (IsNudiMatraContext(_buffer.ToString(), key))
                {
                    _buffer.Append(key);
                    string matra = _nudiMatras[key];
                    // If matra is empty (key 'a'), we assume user wants to confirm Base Consonant.
                    // We return empty string, 0 backspace.
                    // But effectively we consumed the key.
                    return (matra, 0);
                }
                else
                {
                    _buffer.Append(key);
                    return (_nudiVowels[key], 0);
                }
            }

            // 4. Check for Consonants
            if (_nudiConsonants.ContainsKey(key))
            {
                _buffer.Append(key);
                return (_nudiConsonants[key], 0);
            }

            // 5. Diacritics
            if (_diacritics.ContainsKey(key))
            {
                _buffer.Append(key);
                return (_diacritics[key], 0);
            }

            if (_symbolDiacritics.ContainsKey(key))
            {
                _buffer.Append(key);
                return (_symbolDiacritics[key], 0);
            }

            // Default
            _buffer.Append(key);
            return (key, 0);
        }

        private bool IsNudiMatraContext(string buffer, string currentKey)
        {
            if (string.IsNullOrEmpty(buffer)) return false;

            char lastChar = buffer[buffer.Length - 1];
            string lastKey = lastChar.ToString();

            if (_nudiConsonants.ContainsKey(lastKey))
            {
                // Last key was a consonant.
                // It is in Base form (e.g. 'ಕ').
                // It can accept a matra.
                return true;
            }

            // If last key was f/F, then it's Halant form. Cannot accept Matra.
            if (lastKey == "f" || lastKey == "F") return false;

            // If last key was a Vowel/Matra, then it cannot accept another Matra (usually).
            if (_nudiVowels.ContainsKey(lastKey)) return false;

            // If last key sequence was a special Consonant (rX, LX, etc)
            // We need to check the last 2 chars of buffer.
            if (buffer.Length >= 2)
            {
                 string suffix = buffer.Substring(buffer.Length-2);
                 if (_nudiSpecials.ContainsKey(suffix))
                 {
                     string val = _nudiSpecials[suffix];
                     // If val is a consonant (ಱ, ೞ, etc), it can accept Matra.
                     // Consonant checks: ಱ(rX), ೞ(LX), ಜ಼(jX), ಫ಼(PX), ಖ಼(KX)
                     if (val == "ಱ" || val == "ೞ" || val == "ಜ಼" || val == "ಫ಼" || val == "ಖ಼") return true;
                 }
            }

            return false;
        }

        private (string text, int backspaceCount) GetBarahaTransliteration(string key)
        {
            string combinedKey = (_buffer.ToString() + key);

            if (_buffer.Length > 0)
            {
                // 1. Try to match longest sequence backwards for VOWEL MODIFIERS on CONSONANTS
                for (int i = combinedKey.Length - 1; i >= 0; i--)
                {
                    string potentialConsonantToken = combinedKey.Substring(0, i); // e.g. "k" or "kh"
                    string potentialVowelToken = combinedKey.Substring(i); // e.g. "a" or "aa"

                    if (_barahaMap.ContainsKey(potentialConsonantToken) && IsBarahaConsonant(potentialConsonantToken))
                    {
                        if (_barahaVowelSigns.ContainsKey(potentialVowelToken))
                        {
                            // Found a valid C+V combo!
                            // e.g. "kh" + "aa"

                            string previousOutput = RecalculateOutput(_buffer.ToString());
                            if (string.IsNullOrEmpty(previousOutput)) previousOutput = "";

                            string consChar = _barahaMap[potentialConsonantToken];
                            string baseChar = consChar.TrimEnd('\u0CCD');
                            string matra = _barahaVowelSigns[potentialVowelToken];

                            string replacement = baseChar + matra;

                            _buffer.Append(key);
                            return (replacement, previousOutput.Length);
                        }
                    }
                }

                // 2. Check if combined key is a valid Consonant or Vowel (Extension)
                // e.g. Buffer="t", Key="h" -> "th" -> "ಥ್".
                // Previous was "t" -> "ಟ್" (2 chars).
                // We replace "ಟ್" with "ಥ್".
                if (_barahaMap.ContainsKey(combinedKey))
                {
                    string previousOutput = RecalculateOutput(_buffer.ToString());

                    _buffer.Clear();
                    _buffer.Append(combinedKey);
                    return (_barahaMap[combinedKey], previousOutput.Length);
                }
            }

            // Standard processing (Independent Char)
            if (_barahaMap.ContainsKey(key))
            {
                _buffer.Clear();
                _buffer.Append(key);
                return (_barahaMap[key], 0);
            }

            // Unmapped
            _buffer.Clear();
            _buffer.Append(key);
            return (key, 0);
        }

        // Helper to determine what string a given key buffer would have produced.
        // This effectively simulates the logic.
        private string RecalculateOutput(string bufferKeys)
        {
            if (string.IsNullOrEmpty(bufferKeys)) return "";

            // Try to match C+V
            for (int i = bufferKeys.Length - 1; i >= 0; i--)
            {
                string c = bufferKeys.Substring(0, i);
                string v = bufferKeys.Substring(i);

                if (_barahaMap.ContainsKey(c) && IsBarahaConsonant(c) && _barahaVowelSigns.ContainsKey(v))
                {
                    string cons = _barahaMap[c];
                    string baseChar = cons.TrimEnd('\u0CCD');
                    string matra = _barahaVowelSigns[v];
                    return baseChar + matra;
                }
            }

            // Try match full key
            if (_barahaMap.ContainsKey(bufferKeys))
            {
                return _barahaMap[bufferKeys];
            }

            // Fallback (likely shouldn't happen for valid sequences in buffer, but...)
            // Just return the last key's mapping if possible, or ?
            // In our logic, the buffer is cleared if not matched.
            // So if we are here, the buffer *should* be a valid sequence.
            return "";
        }

        public void RemoveLast()
        {
            if (_buffer.Length > 0)
            {
                _buffer.Length--;
            }
        }

        private bool IsBarahaConsonant(string k)
        {
            if (!_barahaMap.ContainsKey(k)) return false;
            string val = _barahaMap[k];
            // Check for Halant at end
            return val.EndsWith("\u0CCD");
        }

        private string GetLastTokenFromBuffer(string buffer, Dictionary<string, string> map)
        {
             for (int i = 0; i < buffer.Length; i++)
             {
                 string sub = buffer.Substring(i);
                 if (map.ContainsKey(sub)) return sub;
             }
             return "";
        }

        public void ClearBuffer()
        {
            _buffer.Clear();
        }
    }
}
