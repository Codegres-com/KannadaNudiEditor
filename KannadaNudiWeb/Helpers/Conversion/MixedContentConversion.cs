using System.IO.Compression;
using System.Text;
using System.Text.RegularExpressions;
using System.Xml.Linq;

namespace KannadaNudiEditor.Helpers.Conversion
{
    /// <summary>
    /// Conversion of documents that MIX English and legacy ASCII Kannada (Nudi).
    ///
    /// The plain <see cref="ConvertAsciiToUnicode"/> converter treats every character as
    /// Nudi ASCII, so English text in the same document is turned into garbage Kannada.
    /// This file adds two content-aware entry points:
    ///
    ///  * <see cref="ConvertDocxAsciiToUnicode"/> – works on the .docx XML. Only runs whose
    ///    effective font is a legacy Kannada font (e.g. "Nudi 01 k", "Nudi Akshara") are
    ///    converted; all other runs (Times New Roman, Arial, …) are left untouched. Bold,
    ///    italics, sizes, tables and paragraph layout are preserved.
    ///
    ///  * <see cref="ConvertMixedAsciiToUnicode"/> – for plain text (and .docx files that do
    ///    not use any legacy font), where there is no font information. Words are classified
    ///    heuristically: a word containing Latin-1 glyph characters (À Á Ä É ¸ ª « …) is Nudi
    ///    Kannada; pure-ASCII words are kept as English unless they sit between two Kannada
    ///    words.
    /// </summary>
    internal static partial class FileConversionUtilities
    {
        private static readonly XNamespace W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

        // Legacy (ASCII-encoded) Kannada font families. Matched as case-insensitive prefixes.
        private static readonly string[] DefaultLegacyFontPrefixes = { "Nudi" };

        // Fonts that start with a legacy prefix but are real Unicode fonts.
        private static readonly string[] DefaultUnicodeFontExclusions =
            { "NudiParijatha", "Nudi Parijatha", "Nudi Unicode", "NudiUni" };

        private const string DefaultUnicodeOutputFont = "Nirmala UI";

        // Pure-lowercase ASCII words of 5+ letters inside a Nudi-font run are almost always
        // English typed with the wrong font ("organism", "sample"); Nudi Kannada words of that
        // shape essentially never occur, so keep them as English.
        private static readonly Regex EnglishWordInLegacyRun =
            new(@"(?<![A-Za-z\u0080-ÿ])[a-z]{5,}(?![A-Za-z\u0080-ÿ])", RegexOptions.Compiled);

        // =========================================================
        // DOCX (font-aware)
        // =========================================================

        /// <summary>
        /// Converts legacy-font runs of a .docx to Unicode Kannada and returns the new package.
        /// <paramref name="convertedRuns"/> is 0 when the document uses no legacy Kannada font;
        /// callers can then fall back to <see cref="ConvertMixedAsciiToUnicode"/>.
        /// </summary>
        public static byte[] ConvertDocxAsciiToUnicode(byte[] docx, ConversionConfig cfg, out int convertedRuns)
        {
            convertedRuns = 0;

            using var input = new MemoryStream(docx);
            using var zin = new ZipArchive(input, ZipArchiveMode.Read);

            var styles = LoadStyles(zin);

            using var output = new MemoryStream();
            using (var zout = new ZipArchive(output, ZipArchiveMode.Create, leaveOpen: true))
            {
                foreach (var entry in zin.Entries)
                {
                    var target = zout.CreateEntry(entry.FullName, CompressionLevel.Optimal);
                    target.LastWriteTime = entry.LastWriteTime;

                    using var src = entry.Open();
                    using var dst = target.Open();

                    if (IsWordTextPart(entry.FullName))
                    {
                        var doc = XDocument.Load(src, LoadOptions.PreserveWhitespace);
                        convertedRuns += ConvertDocxPart(doc, styles, cfg);
                        doc.Save(dst, SaveOptions.DisableFormatting);
                    }
                    else
                    {
                        src.CopyTo(dst);
                    }
                }
            }

            return output.ToArray();
        }

        private static bool IsWordTextPart(string name) =>
            Regex.IsMatch(name, @"^word/(document|footnotes|endnotes|comments|header\d*|footer\d*)\.xml$",
                RegexOptions.IgnoreCase);

        private static int ConvertDocxPart(XDocument doc, StyleFonts styles, ConversionConfig cfg)
        {
            int count = 0;

            foreach (var p in doc.Descendants(W + "p").ToList())
            {
                string? paraStyle = p.Element(W + "pPr")?.Element(W + "pStyle")?.Attribute(W + "val")?.Value;

                // Runs that belong to this paragraph (not to a text box nested inside it).
                var runs = p.Descendants(W + "r")
                            .Where(r => r.Ancestors(W + "p").First() == p)
                            .ToList();

                var group = new List<XElement>();

                foreach (var r in runs)
                {
                    bool legacy = IsLegacyFont(EffectiveFont(r, paraStyle, styles));
                    string text = RunText(r);
                    bool hasBreak = r.Elements().Any(e => RunBreakTags.Contains(e.Name.LocalName));

                    if (legacy && !hasBreak)
                    {
                        if (text.Length > 0) group.Add(r);
                        continue; // empty legacy run: keep collecting
                    }

                    count += FlushGroup(group, cfg);

                    if (legacy && text.Length > 0)
                    {
                        // Legacy run that also contains tabs/breaks: convert each text node alone.
                        foreach (var t in r.Elements(W + "t"))
                            t.Value = ConvertLegacySegment(t.Value, cfg);
                        SetUnicodeFont(r);
                        count++;
                    }
                }

                count += FlushGroup(group, cfg);
            }

            return count;
        }

        private static readonly HashSet<string> RunBreakTags = new(StringComparer.Ordinal)
        {
            "tab", "br", "cr", "sym", "drawing", "pict", "object", "fldChar", "instrText", "ptab"
        };

        /// <summary>
        /// Converts a group of consecutive legacy-font runs. A syllable can be split across
        /// runs (e.g. a vowel sign in a differently formatted run), so text is only split where
        /// converting the pieces separately gives the same result as converting them together;
        /// otherwise the following run's text is merged into the previous run.
        /// </summary>
        private static int FlushGroup(List<XElement> group, ConversionConfig cfg)
        {
            if (group.Count == 0) return 0;

            var texts = group.Select(RunText).ToList();
            var segments = new List<(int First, int Last, string Text)>();

            int i = 0;
            while (i < group.Count)
            {
                string left = texts[i];
                int j = i + 1;
                while (j < group.Count)
                {
                    string rest = string.Concat(texts.Skip(j));
                    if (ConvertLegacySegment(left, cfg) + ConvertLegacySegment(rest, cfg) ==
                        ConvertLegacySegment(left + rest, cfg))
                        break;
                    left += texts[j];
                    j++;
                }
                segments.Add((i, j - 1, left));
                i = j;
            }

            var converted = segments.Select(s => ConvertLegacySegment(s.Text, cfg)).ToList();

            // A run must not start with a combining mark (anusvara, vowel sign, halant, vattakshara);
            // Word would show a dotted circle. Move such leading marks to the previous run.
            for (int k = 1; k < converted.Count; k++)
            {
                string s = converted[k];
                int n = 0;
                while (n < s.Length &&
                       (IsKannadaCombining(s[n]) ||
                        (IsKannadaConsonant(s[n]) && n > 0 && (s[n - 1] == cfg.Halant || s[n - 1] == cfg.Zwj))))
                    n++;
                if (n == 0) continue;

                int prev = k - 1;
                while (prev > 0 && converted[prev].Length == 0) prev--;
                if (converted[prev].Length == 0) continue;

                converted[prev] += s.Substring(0, n);
                converted[k] = s.Substring(n);
            }

            for (int k = 0; k < segments.Count; k++)
            {
                var (first, last, _) = segments[k];
                SetRunText(group[first], converted[k]);
                SetUnicodeFont(group[first]);
                for (int m = first + 1; m <= last; m++)
                    SetRunText(group[m], string.Empty);
            }

            int count = group.Count;
            group.Clear();
            return count;
        }

        private static bool IsKannadaCombining(char c) =>
            c == 'ಂ' || c == 'ಃ' || c == '಼' || (c >= 'ಾ' && c <= '್') ||
            c == 'ೕ' || c == 'ೖ' || c == '‌' || c == '‍';

        /// <summary>Converts text from a legacy-font run, keeping English words typed in that font.</summary>
        private static string ConvertLegacySegment(string text, ConversionConfig cfg)
        {
            if (string.IsNullOrEmpty(text)) return text;

            var sb = new StringBuilder(text.Length);
            int last = 0;
            foreach (Match m in EnglishWordInLegacyRun.Matches(text))
            {
                if (m.Index > last) sb.Append(ConvertAsciiToUnicode(text.Substring(last, m.Index - last), cfg));
                sb.Append(m.Value);
                last = m.Index + m.Length;
            }
            if (last < text.Length) sb.Append(ConvertAsciiToUnicode(text.Substring(last), cfg));
            return sb.ToString();
        }

        private static string RunText(XElement r) =>
            string.Concat(r.Elements(W + "t").Select(t => t.Value));

        private static void SetRunText(XElement r, string text)
        {
            var ts = r.Elements(W + "t").ToList();
            if (ts.Count == 0) return;
            ts[0].Value = text;
            ts[0].SetAttributeValue(XNamespace.Xml + "space", "preserve");
            foreach (var extra in ts.Skip(1)) extra.Remove();
        }

        private static void SetUnicodeFont(XElement r)
        {
            var rPr = r.Element(W + "rPr");
            if (rPr == null)
            {
                rPr = new XElement(W + "rPr");
                r.AddFirst(rPr);
            }

            var rFonts = rPr.Element(W + "rFonts");
            if (rFonts == null)
            {
                rFonts = new XElement(W + "rFonts");
                rPr.AddFirst(rFonts);
            }

            foreach (var attr in rFonts.Attributes().Where(x => x.Name.LocalName.EndsWith("Theme") || x.Name.LocalName == "hint").ToList())
                attr.Remove();
            foreach (var a in new[] { "ascii", "hAnsi", "cs", "eastAsia" })
                rFonts.SetAttributeValue(W + a, DefaultUnicodeOutputFont);

            // Kannada is a complex script: Word applies bCs/iCs/szCs to it, not b/i/sz.
            MirrorComplexScript(rPr, "b", "bCs");
            MirrorComplexScript(rPr, "i", "iCs");
            MirrorComplexScript(rPr, "sz", "szCs");
        }

        private static void MirrorComplexScript(XElement rPr, string latin, string cs)
        {
            var src = rPr.Element(W + latin);
            if (src == null || rPr.Element(W + cs) != null) return;
            var copy = new XElement(W + cs);
            var val = src.Attribute(W + "val");
            if (val != null) copy.SetAttributeValue(W + "val", val.Value);
            src.AddAfterSelf(copy);
        }

        private static bool IsLegacyFont(string? font)
        {
            if (string.IsNullOrWhiteSpace(font)) return false;
            if (DefaultUnicodeFontExclusions.Any(x => font.StartsWith(x, StringComparison.OrdinalIgnoreCase)))
                return false;
            return DefaultLegacyFontPrefixes.Any(x => font.StartsWith(x, StringComparison.OrdinalIgnoreCase));
        }

        // ---------------------------------------------------------
        // Effective font: run rFonts -> run char style -> paragraph style -> defaults
        // ---------------------------------------------------------

        private sealed class StyleFonts
        {
            public Dictionary<string, (string? Font, string? BasedOn)> Styles { get; } = new(StringComparer.Ordinal);
            public string? DefaultParagraphStyle { get; set; }
            public string? DocDefaultFont { get; set; }

            public string? Resolve(string? styleId)
            {
                var seen = new HashSet<string>(StringComparer.Ordinal);
                while (styleId != null && seen.Add(styleId) && Styles.TryGetValue(styleId, out var s))
                {
                    if (!string.IsNullOrEmpty(s.Font)) return s.Font;
                    styleId = s.BasedOn;
                }
                return null;
            }
        }

        private static StyleFonts LoadStyles(ZipArchive zip)
        {
            var result = new StyleFonts();
            var entry = zip.GetEntry("word/styles.xml");
            if (entry == null) return result;

            using var s = entry.Open();
            var doc = XDocument.Load(s);

            result.DocDefaultFont = FontOf(doc.Root?.Element(W + "docDefaults")?.Element(W + "rPrDefault")?.Element(W + "rPr"));

            foreach (var st in doc.Descendants(W + "style"))
            {
                string? id = st.Attribute(W + "styleId")?.Value;
                if (id == null) continue;

                result.Styles[id] = (FontOf(st.Element(W + "rPr")),
                                     st.Element(W + "basedOn")?.Attribute(W + "val")?.Value);

                if (st.Attribute(W + "type")?.Value == "paragraph" &&
                    (st.Attribute(W + "default")?.Value is "1" or "true"))
                    result.DefaultParagraphStyle = id;
            }

            return result;
        }

        private static string? FontOf(XElement? rPr)
        {
            var f = rPr?.Element(W + "rFonts");
            return f?.Attribute(W + "ascii")?.Value ?? f?.Attribute(W + "hAnsi")?.Value;
        }

        private static string? EffectiveFont(XElement run, string? paraStyle, StyleFonts styles)
        {
            var rPr = run.Element(W + "rPr");

            string? font = FontOf(rPr);
            if (!string.IsNullOrEmpty(font)) return font;

            string? charStyle = rPr?.Element(W + "rStyle")?.Attribute(W + "val")?.Value;
            font = styles.Resolve(charStyle);
            if (!string.IsNullOrEmpty(font)) return font;

            font = styles.Resolve(paraStyle ?? styles.DefaultParagraphStyle);
            if (!string.IsNullOrEmpty(font)) return font;

            return styles.DocDefaultFont;
        }

        // =========================================================
        // Plain text (no font information) – word-level heuristic
        // =========================================================

        /// <summary>
        /// Converts only the Nudi-ASCII words of a mixed English/Kannada text.
        /// </summary>
        public static string ConvertMixedAsciiToUnicode(string text, ConversionConfig cfg)
        {
            if (string.IsNullOrEmpty(text)) return text ?? string.Empty;

            // Tokens = runs of non-whitespace; whitespace between tokens is kept as-is.
            var tokens = Regex.Matches(text, @"\S+").Cast<Match>().ToList();
            if (tokens.Count == 0) return text;

            var kind = tokens.Select(t => ClassifyToken(t.Value)).ToArray();

            // Pure-ASCII tokens surrounded by Nudi words on both sides are Nudi too
            // (e.g. "Cw" = ಅತಿ, "J" = ಎ in "ªÀÄvÀÄÛ J ¸ÀA").
            for (int i = 0; i < kind.Length; i++)
            {
                if (kind[i] != TokenKind.PlainAscii) continue;
                bool prevLegacy = i > 0 && kind[i - 1] == TokenKind.Legacy;
                bool nextLegacy = i + 1 < kind.Length && kind[i + 1] == TokenKind.Legacy;
                if (prevLegacy && nextLegacy && !EnglishWordInLegacyRun.IsMatch(tokens[i].Value))
                    kind[i] = TokenKind.LegacyByContext;
            }

            var sb = new StringBuilder(text.Length);
            int pos = 0;
            int k = 0;
            while (k < tokens.Count)
            {
                if (!IsLegacyKind(kind[k])) { k++; continue; }

                // Convert a maximal span of consecutive Nudi words in one go, so that
                // rules spanning a space (e.g. a vowel sign typed after a space) still apply.
                int start = tokens[k].Index;
                int m = k;
                while (m + 1 < tokens.Count && IsLegacyKind(kind[m + 1])) m++;
                int end = tokens[m].Index + tokens[m].Length;

                sb.Append(text, pos, start - pos);
                sb.Append(ConvertAsciiToUnicode(text.Substring(start, end - start), cfg));
                pos = end;
                k = m + 1;
            }
            sb.Append(text, pos, text.Length - pos);
            return sb.ToString();
        }

        private enum TokenKind { PlainAscii, Legacy, LegacyByContext, Other }

        private static bool IsLegacyKind(TokenKind k) => k == TokenKind.Legacy || k == TokenKind.LegacyByContext;

        private static TokenKind ClassifyToken(string token)
        {
            int asciiLetters = 0, glyphs = 0;
            foreach (char c in token)
            {
                if (c >= 'ಀ' && c <= '೿') return TokenKind.Other;       // already Unicode Kannada
                if (c > 'ÿ') return TokenKind.Other;                          // other scripts / symbols
                if (c >= '¡') glyphs++;                                       // Nudi glyph range
                else if (c < '\u0080' && char.IsLetter(c)) asciiLetters++;
            }

            // One accented letter in an otherwise English word ("café", "naïve", "37°C")
            // is English; Nudi words use several glyph characters or are very short.
            bool hasDigit = token.Any(char.IsAsciiDigit);
            if (glyphs >= 2 || (glyphs == 1 && asciiLetters <= 3 && !hasDigit)) return TokenKind.Legacy;
            if (glyphs == 1) return TokenKind.Other;
            return asciiLetters > 0 ? TokenKind.PlainAscii : TokenKind.Other;
        }
    }
}
