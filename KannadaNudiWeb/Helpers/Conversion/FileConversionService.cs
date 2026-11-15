using System;
using System.IO;
using System.Net.Http;
using System.Net.Http.Json;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace KannadaNudiEditor.Helpers.Conversion
{
    public class FileConversionService
    {
        private readonly HttpClient _httpClient;
        private FileConversionUtilities.ConversionConfig? _a2uConfig;
        private FileConversionUtilities.ConversionConfig? _u2aConfig;

        public FileConversionService(HttpClient httpClient)
        {
            _httpClient = httpClient;
        }

        public async Task InitializeAsync()
        {
            if (_a2uConfig == null)
            {
                _a2uConfig = await LoadConfigAsync(Direction.AsciiToUnicode);
            }
            if (_u2aConfig == null)
            {
                _u2aConfig = await LoadConfigAsync(Direction.UnicodeToAscii);
            }
        }

        public string AsciiToUnicodeConverter(string input)
            => Convert(input, Direction.AsciiToUnicode);

        public string UnicodeToAsciiConverter(string input)
            => Convert(input, Direction.UnicodeToAscii);

        /// <summary>
        /// Mixed English + Nudi ASCII text: converts only the Nudi words, English is kept.
        /// </summary>
        public string MixedAsciiToUnicodeConverter(string input)
        {
            try
            {
                if (string.IsNullOrEmpty(input)) return input ?? string.Empty;
                return FileConversionUtilities.ConvertMixedAsciiToUnicode(input, GetCfg(Direction.AsciiToUnicode));
            }
            catch (Exception ex)
            {
                SimpleLogger.LogException(ex, "[MixedAsciiToUnicode] Failed");
                return input ?? string.Empty;
            }
        }

        /// <summary>
        /// Converts the Nudi-font runs of a .docx to Unicode Kannada, leaving English runs and
        /// formatting untouched. Returns the original bytes and 0 when the file has no legacy
        /// Kannada font (or cannot be parsed).
        /// </summary>
        public (byte[] Docx, int ConvertedRuns) DocxAsciiToUnicodeConverter(byte[] docx)
        {
            try
            {
                var bytes = FileConversionUtilities.ConvertDocxAsciiToUnicode(
                    docx, GetCfg(Direction.AsciiToUnicode), out int runs);
                return runs > 0 ? (bytes, runs) : (docx, 0);
            }
            catch (Exception ex)
            {
                SimpleLogger.LogException(ex, "[DocxAsciiToUnicode] Failed");
                return (docx, 0);
            }
        }

        private enum Direction { AsciiToUnicode, UnicodeToAscii }

        private FileConversionUtilities.ConversionConfig GetCfg(Direction dir)
        {
            if (dir == Direction.AsciiToUnicode) return _a2uConfig ?? throw new InvalidOperationException("Config not loaded");
            return _u2aConfig ?? throw new InvalidOperationException("Config not loaded");
        }

        private string Convert(string? input, Direction dir)
        {
            try
            {
                if (string.IsNullOrEmpty(input))
                    return input ?? string.Empty;

                var cfg = GetCfg(dir);

                return dir == Direction.AsciiToUnicode
                    ? FileConversionUtilities.ConvertAsciiToUnicode(input, cfg)
                    : FileConversionUtilities.ConvertUnicodeToAscii(input, cfg);
            }
            catch (Exception ex)
            {
                SimpleLogger.LogException(ex, $"[{dir}] Failed");
                return input ?? string.Empty;
            }
        }

        private async Task<FileConversionUtilities.ConversionConfig> LoadConfigAsync(Direction dir)
        {
            string fileName = dir == Direction.AsciiToUnicode
                ? "AsciiToUnicodeMapping.json"
                : "UnicodeToAsciiMapping.json";

            string url = $"Resources/{fileName}";

            try
            {
                var opts = CreateJsonOptions();
                // We use GetStringAsync and Deserialize because GetFromJsonAsync might struggle with specific options or formatting if complex
                string json = await _httpClient.GetStringAsync(url);

                var root = JsonSerializer.Deserialize<FileConversionUtilities.ConversionJson>(json, opts)
                           ?? throw new InvalidOperationException($"Failed to deserialize {fileName}.");

                return FileConversionUtilities.ConversionConfig.From(root, dir == Direction.AsciiToUnicode);
            }
            catch (Exception ex)
            {
                 SimpleLogger.LogException(ex, $"Failed to load config from {url}");
                 throw;
            }
        }

        private static JsonSerializerOptions CreateJsonOptions()
            => new()
            {
                PropertyNameCaseInsensitive = true,
                ReadCommentHandling = JsonCommentHandling.Skip,
                AllowTrailingCommas = true
            };
    }
}
