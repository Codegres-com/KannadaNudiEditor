window.speechInterop = {
    recognition: null,
    dotNetRef: null,

    start: function (dotNetReference, lang) {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SpeechRecognition) {
            console.error("Web Speech API not supported.");
            return false;
        }

        this.dotNetRef = dotNetReference;
        this.recognition = new SpeechRecognition();
        this.recognition.continuous = true;
        this.recognition.interimResults = false;
        this.recognition.lang = lang || 'kn-IN';

        this.recognition.onresult = function (event) {
            let finalTranscript = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
                if (event.results[i].isFinal) {
                    finalTranscript += event.results[i][0].transcript;
                }
            }
            if (finalTranscript.length > 0) {
                dotNetReference.invokeMethodAsync('OnSpeechResult', finalTranscript);
            }
        };

        this.recognition.onerror = function (event) {
            console.error("Speech recognition error", event.error);
            dotNetReference.invokeMethodAsync('OnSpeechError', event.error);
        };

        this.recognition.start();
        return true;
    },

    stop: function () {
        if (this.recognition) {
            this.recognition.stop();
            this.recognition = null;
        }
    },

    worker: null,

    transcribeFile: function (inputId, dotNetReference) {
        const input = document.getElementById(inputId);
        if (!input || !input.files || input.files.length === 0) {
            console.error("No file selected for transcription");
            return;
        }

        const file = input.files[0];
        const audioUrl = URL.createObjectURL(file);

        if (!this.worker) {
            this.worker = new Worker('js/speech-worker.js', { type: 'module' });

            this.worker.onmessage = (e) => {
                const message = e.data;
                if (message.type === 'success') {
                    console.log("Transcription success:", message.text);
                    dotNetReference.invokeMethodAsync('OnTranscriptionResult', message.text);
                    // Revoke URL to free memory, but only if we know we are done with it.
                    // Since logic is one-shot, we can probably revoke it or wait.
                } else if (message.type === 'error') {
                    console.error("Transcription error:", message.error);
                    dotNetReference.invokeMethodAsync('OnSpeechError', message.error);
                } else if (message.type === 'progress') {
                    console.log("Transcription progress:", message.data);
                    // Optional: Update progress UI if we added a method for it
                }
            };
        }

        console.log("Sending file to worker for transcription...");
        this.worker.postMessage({
            type: 'transcribe',
            audioUrl: audioUrl,
            language: null // Auto-detect
        });
    }
};
