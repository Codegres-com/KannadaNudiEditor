using System;
using System.Collections.Generic;
using System.Net.Http;
using System.Text;
using System.Text.Json;
using System.Threading;
using System.Threading.Tasks;

namespace KannadaNudiWeb.Services
{
    public class TranslationService
    {
        public static readonly Dictionary<string, string> SupportedLanguages = new()
        {
            { "kn", "Kannada" },
            { "en", "English" },
            { "af", "Afrikaans" },
            { "sq", "Albanian" },
            { "am", "Amharic" },
            { "ar", "Arabic" },
            { "hy", "Armenian" },
            { "as", "Assamese" },
            { "ay", "Aymara" },
            { "az", "Azerbaijani" },
            { "bm", "Bambara" },
            { "eu", "Basque" },
            { "be", "Belarusian" },
            { "bn", "Bengali" },
            { "bho", "Bhojpuri" },
            { "bs", "Bosnian" },
            { "bg", "Bulgarian" },
            { "ca", "Catalan" },
            { "ceb", "Cebuano" },
            { "ny", "Chichewa" },
            { "zh-CN", "Chinese (Simplified)" },
            { "zh-TW", "Chinese (Traditional)" },
            { "co", "Corsican" },
            { "hr", "Croatian" },
            { "cs", "Czech" },
            { "da", "Danish" },
            { "dv", "Dhivehi" },
            { "doi", "Dogri" },
            { "nl", "Dutch" },
            { "eo", "Esperanto" },
            { "et", "Estonian" },
            { "ee", "Ewe" },
            { "tl", "Filipino" },
            { "fi", "Finnish" },
            { "fr", "French" },
            { "fy", "Frisian" },
            { "gl", "Galician" },
            { "ka", "Georgian" },
            { "de", "German" },
            { "el", "Greek" },
            { "gn", "Guarani" },
            { "gu", "Gujarati" },
            { "ht", "Haitian Creole" },
            { "ha", "Hausa" },
            { "haw", "Hawaiian" },
            { "he", "Hebrew" },
            { "hi", "Hindi" },
            { "hmn", "Hmong" },
            { "hu", "Hungarian" },
            { "is", "Icelandic" },
            { "ig", "Igbo" },
            { "ilo", "Ilocano" },
            { "id", "Indonesian" },
            { "ga", "Irish" },
            { "it", "Italian" },
            { "ja", "Japanese" },
            { "jw", "Javanese" },
            { "kk", "Kazakh" },
            { "km", "Khmer" },
            { "rw", "Kinyarwanda" },
            { "gom", "Konkani" },
            { "ko", "Korean" },
            { "kri", "Krio" },
            { "ku", "Kurdish (Kurmanji)" },
            { "ckb", "Kurdish (Sorani)" },
            { "ky", "Kyrgyz" },
            { "lo", "Lao" },
            { "la", "Latin" },
            { "lv", "Latvian" },
            { "ln", "Lingala" },
            { "lt", "Lithuanian" },
            { "lg", "Luganda" },
            { "lb", "Luxembourgish" },
            { "mk", "Macedonian" },
            { "mai", "Maithili" },
            { "mg", "Malagasy" },
            { "ms", "Malay" },
            { "ml", "Malayalam" },
            { "mt", "Maltese" },
            { "mi", "Maori" },
            { "mr", "Marathi" },
            { "mni-Mtei", "Meiteilon (Manipuri)" },
            { "lus", "Mizo" },
            { "mn", "Mongolian" },
            { "my", "Myanmar (Burmese)" },
            { "ne", "Nepali" },
            { "no", "Norwegian" },
            { "or", "Odia (Oriya)" },
            { "om", "Oromo" },
            { "ps", "Pashto" },
            { "fa", "Persian" },
            { "pl", "Polish" },
            { "pt", "Portuguese" },
            { "pa", "Punjabi" },
            { "qu", "Quechua" },
            { "ro", "Romanian" },
            { "ru", "Russian" },
            { "sm", "Samoan" },
            { "sa", "Sanskrit" },
            { "gd", "Scots Gaelic" },
            { "nso", "Sepedi" },
            { "sr", "Serbian" },
            { "st", "Sesotho" },
            { "sn", "Shona" },
            { "sd", "Sindhi" },
            { "si", "Sinhala" },
            { "sk", "Slovak" },
            { "sl", "Slovenian" },
            { "so", "Somali" },
            { "es", "Spanish" },
            { "su", "Sundanese" },
            { "sw", "Swahili" },
            { "sv", "Swedish" },
            { "tg", "Tajik" },
            { "ta", "Tamil" },
            { "tt", "Tatar" },
            { "te", "Telugu" },
            { "th", "Thai" },
            { "ti", "Tigrinya" },
            { "ts", "Tsonga" },
            { "tr", "Turkish" },
            { "tk", "Turkmen" },
            { "ak", "Twi" },
            { "uk", "Ukrainian" },
            { "ur", "Urdu" },
            { "ug", "Uyghur" },
            { "uz", "Uzbek" },
            { "vi", "Vietnamese" },
            { "cy", "Welsh" },
            { "xh", "Xhosa" },
            { "yi", "Yiddish" },
            { "yo", "Yoruba" },
            { "zu", "Zulu" }
        };

        private readonly HttpClient _httpClient;

        public TranslationService(HttpClient httpClient)
        {
            _httpClient = httpClient;
        }

        public async Task<string> TranslateTextAsync(
            string text,
            string sourceLanguage,
            string targetLanguage,
            Action<double>? onProgress = null,
            CancellationToken cancellationToken = default)
        {
            if (string.IsNullOrWhiteSpace(text))
                return string.Empty;

            // Split into safe chunks of up to 3000 characters
            var chunks = ChunkText(text, 3000);
            var translatedChunks = new List<string>();
            int totalChunks = chunks.Count;

            for (int i = 0; i < totalChunks; i++)
            {
                cancellationToken.ThrowIfCancellationRequested();

                var chunk = chunks[i];
                var translatedChunk = await TranslateChunkWithRetryAsync(chunk, sourceLanguage, targetLanguage, cancellationToken);
                translatedChunks.Add(translatedChunk);

                // Report progress percentage
                onProgress?.Invoke((double)(i + 1) / totalChunks * 100.0);

                // Add a small delay between requests to avoid rate limits
                if (i < totalChunks - 1)
                {
                    await Task.Delay(200, cancellationToken);
                }
            }

            return string.Join("\n", translatedChunks);
        }

        private List<string> ChunkText(string text, int maxChunkSize)
        {
            var chunks = new List<string>();
            var lines = text.Split('\n');
            var currentChunk = new StringBuilder();

            foreach (var line in lines)
            {
                // If adding this line exceeds the chunk limit
                if (currentChunk.Length + line.Length + 1 > maxChunkSize)
                {
                    if (currentChunk.Length > 0)
                    {
                        chunks.Add(currentChunk.ToString());
                        currentChunk.Clear();
                    }

                    // If a single line is wider than the max chunk size, we need to split it
                    if (line.Length > maxChunkSize)
                    {
                        var remaining = line;
                        while (remaining.Length > maxChunkSize)
                        {
                            // Find the last space before maxChunkSize to avoid splitting words
                            int splitIdx = remaining.LastIndexOf(' ', maxChunkSize);
                            if (splitIdx <= 0)
                            {
                                splitIdx = maxChunkSize; // fallback to hard split if no spaces exist
                            }

                            chunks.Add(remaining.Substring(0, splitIdx));
                            remaining = remaining.Substring(splitIdx).TrimStart();
                        }
                        if (remaining.Length > 0)
                        {
                            currentChunk.Append(remaining);
                        }
                    }
                    else
                    {
                        currentChunk.Append(line);
                    }
                }
                else
                {
                    if (currentChunk.Length > 0)
                    {
                        currentChunk.Append('\n');
                    }
                    currentChunk.Append(line);
                }
            }

            if (currentChunk.Length > 0)
            {
                chunks.Add(currentChunk.ToString());
            }

            return chunks;
        }

        private async Task<string> TranslateChunkWithRetryAsync(
            string chunk,
            string sourceLanguage,
            string targetLanguage,
            CancellationToken cancellationToken)
        {
            int maxRetries = 3;
            int delay = 500; // start with 500ms delay

            for (int attempt = 0; attempt < maxRetries; attempt++)
            {
                try
                {
                    return await TranslateChunkAsync(chunk, sourceLanguage, targetLanguage, cancellationToken);
                }
                catch (Exception ex) when (attempt < maxRetries - 1 && !cancellationToken.IsCancellationRequested)
                {
                    Console.WriteLine($"Translation attempt {attempt + 1} failed: {ex.Message}. Retrying in {delay}ms...");
                    await Task.Delay(delay, cancellationToken);
                    delay *= 2; // exponential backoff
                }
            }

            throw new Exception("Translation failed after multiple attempts.");
        }

        private async Task<string> TranslateChunkAsync(
            string chunk,
            string sourceLanguage,
            string targetLanguage,
            CancellationToken cancellationToken)
        {
            var url = "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t";

            var values = new Dictionary<string, string>
            {
                { "sl", sourceLanguage },
                { "tl", targetLanguage },
                { "q", chunk }
            };

            var content = new FormUrlEncodedContent(values);
            var response = await _httpClient.PostAsync(url, content, cancellationToken);
            response.EnsureSuccessStatusCode();

            var json = await response.Content.ReadAsStringAsync(cancellationToken);
            return ParseTranslationJson(json);
        }

        private string ParseTranslationJson(string json)
        {
            using var doc = JsonDocument.Parse(json);
            var root = doc.RootElement;
            if (root.ValueKind == JsonValueKind.Array && root.GetArrayLength() > 0)
            {
                var segments = root[0];
                if (segments.ValueKind == JsonValueKind.Array)
                {
                    var sb = new StringBuilder();
                    foreach (var segment in segments.EnumerateArray())
                    {
                        if (segment.ValueKind == JsonValueKind.Array && segment.GetArrayLength() > 0)
                        {
                            var firstElement = segment[0];
                            if (firstElement.ValueKind == JsonValueKind.String)
                            {
                                var translatedText = firstElement.GetString();
                                if (!string.IsNullOrEmpty(translatedText))
                                {
                                    sb.Append(translatedText);
                                }
                            }
                        }
                    }
                    return sb.ToString();
                }
            }
            throw new Exception("Invalid response format from translation API.");
        }
    }
}
