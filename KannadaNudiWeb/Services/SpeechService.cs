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
        public event Action? OnStarted;
        public event Action? OnEnded;

        public SpeechService(IJSRuntime jsRuntime)
        {
            _jsRuntime = jsRuntime;
        }

        public async Task InitializeAsync()
        {
            _objRef = DotNetObjectReference.Create(this);
            await _jsRuntime.InvokeVoidAsync("speechInterop.init", _objRef);
        }

        public async Task<bool> IsSupportedAsync()
        {
            return await _jsRuntime.InvokeAsync<bool>("speechInterop.isSupported");
        }

        public async Task StartAsync(string languageCode = "kn-IN")
        {
            if (_objRef == null)
            {
                await InitializeAsync();
            }

            if (_jsRuntime is IJSInProcessRuntime inProcess)
            {
                inProcess.InvokeVoid("speechInterop.start", languageCode);
            }
            else
            {
                await _jsRuntime.InvokeVoidAsync("speechInterop.start", languageCode);
            }
        }

        public async Task StopAsync()
        {
            if (_jsRuntime is IJSInProcessRuntime inProcess)
            {
                inProcess.InvokeVoid("speechInterop.stop");
            }
            else
            {
                await _jsRuntime.InvokeVoidAsync("speechInterop.stop");
            }
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
        public void OnSpeechStarted()
        {
            OnStarted?.Invoke();
        }

        [JSInvokable]
        public void OnSpeechEnded()
        {
            OnEnded?.Invoke();
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
