using Microsoft.AspNetCore.Components.WebAssembly.Hosting;
using Microsoft.AspNetCore.Components.Web;
using KannadaNudiWeb.Services;
using KannadaNudiEditor.Helpers.Conversion;

namespace KannadaNudiWeb
{
    public class Program
    {
        public static async Task Main(string[] args)
        {
            var builder = WebAssemblyHostBuilder.CreateDefault(args);
            builder.RootComponents.Add<App>("#app");
            builder.RootComponents.Add<HeadOutlet>("head::after");

            builder.Services.AddScoped(sp => new HttpClient { BaseAddress = new Uri(builder.HostEnvironment.BaseAddress) });

            // Register Services
            builder.Services.AddScoped<FileConversionService>();
            builder.Services.AddScoped<TransliterationService>();
            builder.Services.AddScoped<SpeechService>();
            builder.Services.AddScoped<DictionaryService>();

            await builder.Build().RunAsync();
        }
    }
}
