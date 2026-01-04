window.speechInterop = {
    recognition: null,
    dotNetRef: null,

    // Whisper State
    worker: null,
    audioContext: null,
    mediaStream: null,
    scriptProcessor: null,
    audioBuffer: [],
    isRecording: false,
    chunkInterval: null,
    workerReady: false,

    start: function (dotNetReference, lang) {
        this.dotNetRef = dotNetReference;
        console.log("Speech Start requested for language:", lang);

        // Feature Detection: Prefer Native in Chrome/Edge, fallback for Firefox/Others
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        const isFirefox = navigator.userAgent.toLowerCase().indexOf('firefox') > -1;

        if (SpeechRecognition && !isFirefox) {
            return this.startNative(SpeechRecognition, lang);
        } else {
            return this.startWhisper(lang);
        }
    },

    stop: function () {
        console.log("Speech Stop requested");
        if (this.recognition) {
            this.recognition.stop();
            this.recognition = null;
        }
        this.stopWhisper();
    },

    // --- Native Implementation ---
    startNative: function (SpeechRecognition, lang) {
        if (!SpeechRecognition) return false;

        console.log("Starting Native Speech Recognition for:", lang);
        this.dotNetRef.invokeMethodAsync('OnSpeechStatus', 'listening');
        this.recognition = new SpeechRecognition();
        this.recognition.continuous = true;
        this.recognition.interimResults = false;
        this.recognition.lang = lang || 'kn-IN';

        this.recognition.onresult = (event) => {
            let finalTranscript = '';
            for (let i = event.resultIndex; i < event.results.length; ++i) {
                if (event.results[i].isFinal) {
                    finalTranscript += event.results[i][0].transcript;
                }
            }
            if (finalTranscript.length > 0) {
                this.dotNetRef.invokeMethodAsync('OnSpeechResult', finalTranscript);
            }
        };

        this.recognition.onerror = (event) => {
            console.error("Speech recognition error", event.error);
            this.dotNetRef.invokeMethodAsync('OnSpeechError', event.error);
        };

        try {
            this.recognition.start();
            return true;
        } catch (e) {
            console.error(e);
            return false;
        }
    },

    // --- Whisper Implementation ---
    initWorker: function() {
        if (!this.worker) {
            this.worker = new Worker('js/speech-worker.js', { type: 'module' });
            this.worker.onmessage = (e) => {
                const msg = e.data;
                if (msg.type === 'status') {
                    console.log("Whisper Worker Status:", msg.status);
                    this.dotNetRef.invokeMethodAsync('OnSpeechStatus', msg.status);
                    if (msg.status === 'ready') this.workerReady = true;
                } else if (msg.type === 'result') {
                    if (msg.text && msg.text.trim().length > 0) {
                        this.dotNetRef.invokeMethodAsync('OnSpeechResult', msg.text);
                    }
                } else if (msg.type === 'error') {
                    console.error("Whisper Worker Error:", msg.error);
                    this.dotNetRef.invokeMethodAsync('OnSpeechError', msg.error);
                }
            };
            this.worker.postMessage({ type: 'init' });
        }
    },

    startWhisper: async function(lang) {
        // If already recording, stop first to reset state/language
        if (this.isRecording) {
            this.stopWhisper();
        }

        console.log("Starting Whisper (WASM) Recognition for:", lang);

        // Notify UI that we are starting (intermediate state)
        this.dotNetRef.invokeMethodAsync('OnSpeechStatus', 'starting');

        this.initWorker();

        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            this.mediaStream = stream;

            const AudioContext = window.AudioContext || window.webkitAudioContext;
            // Do not force sampleRate; use system default to avoid "different sample-rate" errors
            this.audioContext = new AudioContext();

            const source = this.audioContext.createMediaStreamSource(stream);

            // Use ScriptProcessor (bufferSize 4096)
            this.scriptProcessor = this.audioContext.createScriptProcessor(4096, 1, 1);

            this.audioBuffer = []; // Clear buffer
            this.isRecording = true;

            this.scriptProcessor.onaudioprocess = (e) => {
                if (!this.isRecording) return;
                const inputData = e.inputBuffer.getChannelData(0);
                // Accumulate
                const float32 = new Float32Array(inputData);
                this.audioBuffer.push(float32);
            };

            source.connect(this.scriptProcessor);
            this.scriptProcessor.connect(this.audioContext.destination);

            // Start Chunking Interval (e.g. every 5 seconds)
            this.chunkInterval = setInterval(() => {
                this.processAudioChunk(lang);
            }, 5000);

            this.dotNetRef.invokeMethodAsync('OnSpeechStatus', 'listening');
            return true;
        } catch (err) {
            console.error("Error accessing microphone", err);
            this.dotNetRef.invokeMethodAsync('OnSpeechError', "Microphone access denied or error: " + err.message);
            return false;
        }
    },

    stopWhisper: function() {
        console.log("Stopping Whisper...");
        this.isRecording = false;
        if (this.chunkInterval) {
            clearInterval(this.chunkInterval);
            this.chunkInterval = null;
        }
        if (this.scriptProcessor) {
            this.scriptProcessor.disconnect();
            this.scriptProcessor = null;
        }
        if (this.mediaStream) {
            this.mediaStream.getTracks().forEach(track => track.stop());
            this.mediaStream = null;
        }
        if (this.audioContext) {
            this.audioContext.close();
            this.audioContext = null;
        }
        this.audioBuffer = [];
    },

    processAudioChunk: function(lang) {
        if (this.audioBuffer.length === 0 || !this.workerReady) return;

        // Flatten buffer
        const totalLength = this.audioBuffer.reduce((acc, val) => acc + val.length, 0);
        const result = new Float32Array(totalLength);
        let offset = 0;
        for (let chunk of this.audioBuffer) {
            result.set(chunk, offset);
            offset += chunk.length;
        }
        this.audioBuffer = []; // Clear buffer

        // Resample if necessary
        let finalAudio = result;
        if (this.audioContext.sampleRate !== 16000) {
            finalAudio = this.downsampleBuffer(result, this.audioContext.sampleRate, 16000);
        }

        console.log("Sending chunk to worker, Lang:", lang);
        // Send to worker
        this.worker.postMessage({
            type: 'transcribe',
            audio: finalAudio,
            language: lang
        });
    },

    downsampleBuffer: function(buffer, sampleRate, outSampleRate) {
        if (outSampleRate === sampleRate) {
            return buffer;
        }
        if (outSampleRate > sampleRate) {
            console.error("Downsampling rate should be smaller than original sample rate");
            return buffer;
        }
        var sampleRateRatio = sampleRate / outSampleRate;
        var newLength = Math.round(buffer.length / sampleRateRatio);
        var result = new Float32Array(newLength);
        var offsetResult = 0;
        var offsetBuffer = 0;

        while (offsetResult < result.length) {
            var nextOffsetBuffer = Math.round((offsetResult + 1) * sampleRateRatio);
            var accum = 0, count = 0;
            for (var i = offsetBuffer; i < nextOffsetBuffer && i < buffer.length; i++) {
                accum += buffer[i];
                count++;
            }
            result[offsetResult] = accum / count;
            offsetResult++;
            offsetBuffer = nextOffsetBuffer;
        }
        return result;
    }
};
