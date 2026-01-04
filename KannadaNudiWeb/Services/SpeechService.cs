using Microsoft.JSInterop;
using System;
using System.Threading.Tasks;

namespace KannadaNudiWeb.Services
{
    public class SpeechService : IAsyncDisposable
    {
        private readonly IJSRuntime _jsRuntime;
        private DotNetObjectReference<SpeechService>? _objRef;

        public event Action<string>? OnResult;
        public event Action<string>? OnError;
        public event Action<string>? OnStatus;

        public SpeechService(IJSRuntime jsRuntime)
        {
            _jsRuntime = jsRuntime;
        }

        public async Task StartAsync(string languageCode = "kn-IN")
        {
            _objRef = DotNetObjectReference.Create(this);
            await _jsRuntime.InvokeVoidAsync("speechInterop.start", _objRef, languageCode);
        }

        public async Task StopAsync()
        {
            await _jsRuntime.InvokeVoidAsync("speechInterop.stop");
        }

        [JSInvokable]
        public void OnSpeechResult(string text)
        {
            OnResult?.Invoke(text);
        }

        [JSInvokable]
        public void OnSpeechError(string error)
        {
            OnError?.Invoke(error);
        }

        [JSInvokable]
        public void OnSpeechStatus(string status)
        {
            OnStatus?.Invoke(status);
        }

        public async ValueTask DisposeAsync()
        {
            if (_objRef != null)
            {
                await StopAsync();
                _objRef.Dispose();
            }
        }
    }
}
