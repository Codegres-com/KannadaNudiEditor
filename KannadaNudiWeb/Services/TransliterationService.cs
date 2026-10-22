using KannadaNudiEditor.Helpers.Conversion;
using System.Threading.Tasks;

namespace KannadaNudiWeb.Services
{
    public class TransliterationService : KannadaKeyboardEngine
    {
        private readonly FileConversionService _conversionService;

        public TransliterationService(FileConversionService conversionService)
        {
            _conversionService = conversionService;
        }

        public async Task InitializeAsync()
        {
            await _conversionService.InitializeAsync();
        }
    }
}
