using System.Text;
using System.Text.Json.Serialization;
using System.Text.RegularExpressions;

namespace KannadaNudiEditor.Helpers.Conversion
{
    internal static partial class FileConversionUtilities
    {
        // =========================================================
        // Shared / common helpers
        // =========================================================
        public static bool IsKannadaOrJoiner(char c)
            => (c >= '\u0C80' && c <= '\u0CFF') || c == '\u200C' || c == '\u200D';

        public static bool IsKannadaConsonant(char c)
            => (c >= '\u0C95' && c <= '\u0CB9') || c == '\u0CDE';

        public static bool EndsWith(string s, char c)
            => !string.IsNullOrEmpty(s) && s[^1] == c;

        public static bool IsSingleChar(string s, out char c)
        {
            c = default;
            if (string.IsNullOrEmpty(s) || s.Length != 1) return false;
            c = s[0];
            return true;
        }

        // =========================================================
        // JSON DTOs + runtime config (shared by A2U and U2A)
        // =========================================================
        internal sealed class ConversionConfig
        {
            // shared
            public required int MaxTokenLength { get; init; }
            public required char Zwj { get; init; }
            public required char Zwnj { get; init; }
            public required char Halant { get; init; }

            public required char AsciiHalantChar { get; init; }
            public char AsciiNuktaChar { get; init; } = '\u00F3';
            public required HashSet<char> AsciiConsonantStartChars { get; init; }
            public required HashSet<char> AsciiJoinCharsBeforeNoSpace { get; init; }

            public required Dictionary<string, string> Mapping { get; init; }
            public required HashSet<char> DependentVowels { get; init; }
            public required HashSet<char> IgnoreList { get; init; }

            public required Dictionary<string, string> Vattaksharagalu { get; init; }
            public required Dictionary<string, string> AsciiArkavattu { get; init; }
            public required Dictionary<string, BrokenCase> BrokenCases { get; init; }

            public required List<(string From, string To)> PostFixupsPairs { get; init; }

            // toggles
            public required bool EnableZwnjInsertion { get; init; }
            public required bool EnableZwjAfterHalant { get; init; }
            public required bool EnableKannadaClusterPostProcess { get; init; }

            // U2A extras
            public required char AsciiArkavattuChar { get; init; }
            public required HashSet<char> UnicodePrevValueChars { get; init; }
            public required HashSet<string> UnicodeVowels { get; init; }
            public required Dictionary<char, char> KannadaDigitToAscii { get; init; }

            public required Regex RegexUniVowelPlusAnusvaraVisarga { get; init; }
            public required Regex RegexUniConsonantPlusVowel { get; init; }
            public required Regex RegexUniVattakshara { get; init; }
            public required Regex RegexUniRephWithoutZwj { get; init; }
            public IReadOnlyList<string>[]? MappingKeysByLen { get; init; } // index = token length
            public int MaxKeyLenInMapping { get; init; }

            // Numbers (A2U)
            public required Dictionary<char, string> AsciiDigitToKannada { get; init; }

            public static ConversionConfig From(ConversionJson root, bool isAsciiToUnicode)
            {
                if (root.Meta == null) throw new InvalidOperationException("meta missing.");
                if (root.Mapping == null) throw new InvalidOperationException("mapping missing.");

                int maxToken = root.Meta.MaxTokenLength > 0 ? root.Meta.MaxTokenLength : 4;
                if (maxToken > 16) maxToken = 16;


                // Build mapping key buckets by token length (perf: avoids Substring allocations)
                int bucketSize = maxToken + 1;
                var temp = new List<string>[bucketSize];
                int maxKeyLen = 0;

                foreach (var key in root.Mapping.Keys)
                {
                    int len = key.Length;
                    if (len <= 0 || len > maxToken) continue;

                    if (len > maxKeyLen) maxKeyLen = len;

                    temp[len] ??= new List<string>();
                    temp[len].Add(key);
                }

                IReadOnlyList<string>[] buckets = new IReadOnlyList<string>[bucketSize];
                for (int len = 0; len < bucketSize; len++)
                {
                    buckets[len] = temp[len] ?? (IReadOnlyList<string>)Array.Empty<string>();
                }

                // assign
                // MappingKeysByLen = buckets,
                // MaxKeyLenInMapping =


                char zwj = FirstOrDefault(root.Meta.Zwj, '\u200D');
                char zwnj = FirstOrDefault(root.Meta.Zwnj, '\u200C');
                char halant = FirstOrDefault(root.Meta.Halant, '\u0CCD');
                char asciiHalantChar = FirstOrDefault(root.Meta.AsciiHalantChar, '\u00EF');

                var postFixupsPairs = (root.PostFixups ?? new List<PostFixupJson>())
                    .Where(x => x != null)
                    .Select(x => (From: x!.From ?? string.Empty, To: x!.To ?? string.Empty))
                    .ToList();

                char asciiArkavattu = FirstOrDefault(root.Meta.AsciiArkavattuChar, '\u00F0');

                var prevChars = new HashSet<char>((root.UnicodePrevValueChars ?? Array.Empty<string>())
                    .Where(s => !string.IsNullOrEmpty(s))
                    .Select(s => s[0]));
                if (prevChars.Count == 0)
                    prevChars = new HashSet<char>(DefaultPrevValueChars);

                var vowels = new HashSet<string>(root.UnicodeVowels ?? Array.Empty<string>(), StringComparer.Ordinal);

                // U2A digits (Kannada -> ASCII)
                var digitMap = new Dictionary<char, char>();
                if (root.KannadaDigits != null &&
                    root.AsciiDigits != null &&
                    root.KannadaDigits.Length == root.AsciiDigits.Length)
                {
                    for (int i = 0; i < root.KannadaDigits.Length; i++)
                    {
                        if (!string.IsNullOrEmpty(root.KannadaDigits[i]) &&
                            !string.IsNullOrEmpty(root.AsciiDigits[i]))
                        {
                            digitMap[root.KannadaDigits[i][0]] = root.AsciiDigits[i][0];
                        }
                    }
                }

                // A2U digits (ASCII -> Kannada) from JSON "numbersMapping"
                var asciiDigitToKannada = new Dictionary<char, string>();
                if (root.NumbersMapping != null)
                {
                    foreach (var kv in root.NumbersMapping)
                    {
                        if (!string.IsNullOrEmpty(kv.Key) && kv.Key.Length == 1)
                            asciiDigitToKannada[kv.Key[0]] = kv.Value ?? string.Empty;
                    }
                }

                // -------- U2A regex defaults --------
                string p1 = root.U2aRegex?.UniVowelPlusAnusvaraVisarga
                    ?? @"(?<v>[\u0C85-\u0C94\u0C8E-\u0C90\u0CE0])(?<av>[\u0C82\u0C83])";

                string p2 = root.U2aRegex?.UniConsonantPlusVowel
                    ?? @"(?<base>[\u0C95-\u0CB9\u0CDE])(?<dv>[\u0CBE-\u0CCC\u0C82\u0C83]{1,2})(?!\u0CCD|\u200D)";

                string p3 = root.U2aRegex?.UniVattakshara
                    ?? @"(?<base>[\u0C95-\u0CB9\u0CDE])(?<chain>(?:\u0CCD\u200D?[\u0C95-\u0CB9\u0CDE])+)(?<dv>[\u0CBE-\u0CCC\u0C82\u0C83]{0,2})";

                // -------- FIXED: UniRephWithoutZwj default --------
                // JS behavior: "RA + HALANT" (no ZWJ) => treat first vattakshara as base and append asciiArkavattu. [file:53]
                string p4 = root.U2aRegex?.UniRephWithoutZwj ?? string.Empty;
                if (string.IsNullOrWhiteSpace(p4) ||
                    p4.Contains("PASTE_THE_EXACT", StringComparison.OrdinalIgnoreCase))
                {
                    p4 =
                        @"\u0CB0\u0CCD" +                                        // RA + HALANT
                        @"\u0CCD" +                                              // first vattakshara halant
                        @"(?<baseVattaCons>\u200D?[\u0C95-\u0CB9\u0CDE])" +      // optional ZWJ + consonant
                        @"(?<restChain>(?:\u0CCD\u200D?[\u0C95-\u0CB9\u0CDE])*)" + // rest chain
                        @"(?<dv>[\u0CBE-\u0CCC\u0C82\u0C83]{0,2})";              // dep vowels
                }

                return new ConversionConfig
                {
                    MaxTokenLength = maxToken,
                    Zwj = zwj,
                    Zwnj = zwnj,
                    Halant = halant,

                    AsciiHalantChar = asciiHalantChar,
                    AsciiNuktaChar = FirstOrDefault(root.Meta.AsciiNuktaChar, '\u00F3'),
                    AsciiConsonantStartChars = new HashSet<char>(root.AsciiConsonantStartChars ?? string.Empty),
                    AsciiJoinCharsBeforeNoSpace = new HashSet<char>(root.AsciiJoinCharsBeforeNoSpace ?? string.Empty),

                    Mapping = root.Mapping,
                    DependentVowels = ToCharSet(root.DependentVowels),
                    IgnoreList = ToCharSet(root.IgnoreList),

                    Vattaksharagalu = root.Vattaksharagalu ?? new Dictionary<string, string>(StringComparer.Ordinal),
                    AsciiArkavattu = root.AsciiArkavattu ?? new Dictionary<string, string>(StringComparer.Ordinal),
                    BrokenCases = (root.BrokenCases ?? new Dictionary<string, BrokenCaseJson>())
                        .ToDictionary(kv => kv.Key, kv => BrokenCase.From(kv.Value), StringComparer.Ordinal),

                    PostFixupsPairs = postFixupsPairs,

                    EnableZwnjInsertion = isAsciiToUnicode,
                    EnableZwjAfterHalant = isAsciiToUnicode,
                    EnableKannadaClusterPostProcess = isAsciiToUnicode,

                    AsciiArkavattuChar = asciiArkavattu,
                    UnicodePrevValueChars = prevChars,
                    UnicodeVowels = vowels,
                    KannadaDigitToAscii = digitMap,

                    RegexUniVowelPlusAnusvaraVisarga = new Regex(p1, RegexOptions.Compiled),
                    RegexUniConsonantPlusVowel = new Regex(p2, RegexOptions.Compiled),
                    RegexUniVattakshara = new Regex(p3, RegexOptions.Compiled),
                    RegexUniRephWithoutZwj = new Regex(p4, RegexOptions.Compiled),

                    AsciiDigitToKannada = asciiDigitToKannada,

                    MappingKeysByLen = buckets,
                    MaxKeyLenInMapping = maxKeyLen,

                };
            }

            private static char FirstOrDefault(string? s, char fallback)
                => !string.IsNullOrEmpty(s) ? s[0] : fallback;

            private static HashSet<char> ToCharSet(string[]? arr)
                => new((arr ?? Array.Empty<string>())
                    .Where(s => !string.IsNullOrEmpty(s))
                    .Select(s => s[0]));
        }

        internal sealed class ConversionJson
        {
            public MetaJson? Meta { get; set; }

            public string? AsciiConsonantStartChars { get; set; }
            public string? AsciiJoinCharsBeforeNoSpace { get; set; }

            public Dictionary<string, string>? Mapping { get; set; }
            public Dictionary<string, BrokenCaseJson>? BrokenCases { get; set; }

            public string[]? DependentVowels { get; set; }
            public string[]? IgnoreList { get; set; }

            public Dictionary<string, string>? Vattaksharagalu { get; set; }
            public Dictionary<string, string>? AsciiArkavattu { get; set; }

            public List<PostFixupJson>? PostFixups { get; set; }

            // U2A
            public string[]? UnicodePrevValueChars { get; set; }
            public string[]? UnicodeVowels { get; set; }
            public string[]? KannadaDigits { get; set; }
            public string[]? AsciiDigits { get; set; }
            public U2aRegexJson? U2aRegex { get; set; }

            // Numbers (A2U)
            [JsonPropertyName("numbersMapping")]
            public Dictionary<string, string>? NumbersMapping { get; set; }
        }

        internal sealed class U2aRegexJson
        {
            public string? UniVowelPlusAnusvaraVisarga { get; set; }
            public string? UniConsonantPlusVowel { get; set; }
            public string? UniVattakshara { get; set; }
            public string? UniRephWithoutZwj { get; set; }
        }

        internal sealed class MetaJson
        {
            public int MaxTokenLength { get; set; }
            public string? Zwj { get; set; }
            public string? Zwnj { get; set; }
            public string? Halant { get; set; }
            public string? AsciiHalantChar { get; set; }
            public string? AsciiNuktaChar { get; set; }

            // U2A
            public string? AsciiArkavattuChar { get; set; }
        }

        internal sealed class PostFixupJson
        {
            public string? From { get; set; }
            public string? To { get; set; }
        }

        internal sealed class BrokenCase
        {
            public required string Value { get; init; }
            public required Dictionary<char, char> Mapping { get; init; }

            public static BrokenCase From(BrokenCaseJson json)
            {
                var map = new Dictionary<char, char>();
                if (json.Mapping != null)
                {
                    foreach (var kv in json.Mapping)
                    {
                        if (!string.IsNullOrEmpty(kv.Key) && !string.IsNullOrEmpty(kv.Value))
                            map[kv.Key[0]] = kv.Value[0];
                    }
                }

                return new BrokenCase
                {
                    Value = json.Value ?? string.Empty,
                    Mapping = map
                };
            }
        }

        internal sealed class BrokenCaseJson
        {
            public string? Value { get; set; }
            public Dictionary<string, string>? Mapping { get; set; }
        }
    }
}
