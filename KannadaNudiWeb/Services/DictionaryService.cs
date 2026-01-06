using System.Net.Http.Json;

namespace KannadaNudiWeb.Services
{
    public class Definition
    {
        public string Meaning { get; set; } = "";
        public string Type { get; set; } = "";
    }

    public class DictionaryService
    {
        private readonly HttpClient _httpClient;
        private Dictionary<string, List<Definition>>? _dictionary;

        public DictionaryService(HttpClient httpClient)
        {
            _httpClient = httpClient;
        }

        public async Task LoadDictionaryAsync()
        {
            try
            {
                _dictionary = await _httpClient.GetFromJsonAsync<Dictionary<string, List<Definition>>>("Resources/alar_dictionary.json");
                Console.WriteLine($"Dictionary loaded with {_dictionary?.Count ?? 0} entries.");
            }
            catch (Exception ex)
            {
                Console.WriteLine($"Error loading dictionary: {ex.Message}");
                _dictionary = new Dictionary<string, List<Definition>>();
            }
        }

        public List<Definition> GetDefinition(string word)
        {
            if (_dictionary != null && _dictionary.TryGetValue(word, out var definitions))
            {
                return definitions;
            }
            return new List<Definition>();
        }
    }
}
