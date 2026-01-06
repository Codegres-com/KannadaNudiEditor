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

    transcribeFile: async function (inputId, dotNetReference) {
        const input = document.getElementById(inputId);
        if (!input || !input.files || input.files.length === 0) {
            console.error("No file selected for transcription");
            return;
        }

        const file = input.files[0];

        // Decode and resample audio on Main Thread
        let audioData;
        try {
            audioData = await this.decodeAudio(file);
        } catch (err) {
            console.error("Audio decoding failed:", err);
            dotNetReference.invokeMethodAsync('OnSpeechError', "Audio decoding failed: " + err.message);
            return;
        }

        if (!this.worker) {
            this.worker = new Worker('js/speech-worker.js', { type: 'module' });

            this.worker.onmessage = (e) => {
                const message = e.data;
                if (message.type === 'success') {
                    console.log("Transcription success:", message.text);
                    dotNetReference.invokeMethodAsync('OnTranscriptionResult', message.text);
                } else if (message.type === 'error') {
                    console.error("Transcription error:", message.error);
                    dotNetReference.invokeMethodAsync('OnSpeechError', message.error);
                } else if (message.type === 'progress') {
                    console.log("Transcription progress:", message.data);
                }
            };
        }

        console.log("Sending audio data to worker for transcription...", audioData.length, "samples");
        this.worker.postMessage({
            type: 'transcribe',
            audio: audioData,
            language: null // Auto-detect
        }, [audioData.buffer]); // Transfer buffer
    },

    decodeAudio: async function(file) {
        const arrayBuffer = await file.arrayBuffer();
        const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
        const decoded = await audioCtx.decodeAudioData(arrayBuffer);

        // Resample to 16000Hz (Whisper requirement)
        const targetSampleRate = 16000;
        const offlineCtx = new OfflineAudioContext(1, decoded.duration * targetSampleRate, targetSampleRate);
        const source = offlineCtx.createBufferSource();
        source.buffer = decoded;
        source.connect(offlineCtx.destination);
        source.start(0);

        const rendered = await offlineCtx.startRendering();
        return rendered.getChannelData(0);
    }
};
