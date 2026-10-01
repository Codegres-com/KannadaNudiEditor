window.speechInterop = {
    dotNetRef: null,
    worker: null,
    audioContext: null,
    mediaStream: null,
    processorNode: null,
    sourceNode: null,
    isListening: false,
    currentLanguage: 'kn-IN',
    
    // Audio recording buffer & VAD state
    audioBuffer: [],
    accumulatedSamples: 0,
    silenceSamples: 0,
    hasSpokenInSegment: false,
    silenceThreshold: 0.012, // RMS energy threshold
    targetSampleRate: 16000,
    actualSampleRate: 16000,
    requestIdCounter: 0,
    isWorkerReady: false,
    isModelLoading: false,

    init: function (dotNetReference) {
        this.dotNetRef = dotNetReference;
        this.initWorker();
    },

    initWorker: function () {
        if (this.worker) return;

        try {
            this.worker = new Worker('js/speech-worker.js', { type: 'module' });
            
            this.worker.onmessage = (e) => {
                const data = e.data;
                if (!data) return;

                if (data.type === 'status') {
                    console.log(`[SpeechInterop] Worker status: ${data.status} - ${data.message || ''}`);
                    if (data.status === 'ready') {
                        this.isWorkerReady = true;
                        this.isModelLoading = false;
                    }
                } else if (data.type === 'model_progress') {
                    // Optional: log or handle progress
                } else if (data.type === 'result') {
                    if (data.text && data.text.length > 0 && this.dotNetRef) {
                        console.log(`[SpeechInterop] Transcription result: "${data.text}"`);
                        this.dotNetRef.invokeMethodAsync('OnSpeechResult', data.text);
                    }
                } else if (data.type === 'error') {
                    console.error('[SpeechInterop] Worker error:', data.error);
                    if (this.dotNetRef && this.isListening) {
                        this.dotNetRef.invokeMethodAsync('OnSpeechError', data.error || 'Speech recognition error');
                    }
                }
            };

            this.worker.onerror = (err) => {
                console.error('[SpeechInterop] Web Worker unhandled error:', err);
                if (this.dotNetRef && this.isListening) {
                    this.dotNetRef.invokeMethodAsync('OnSpeechError', 'Speech worker initialization error');
                }
            };

            // Preload model in the background
            this.isModelLoading = true;
            this.worker.postMessage({ type: 'init' });
        } catch (err) {
            console.error('[SpeechInterop] Failed to instantiate Speech Worker:', err);
        }
    },

    isSupported: function () {
        return !!(
            navigator.mediaDevices &&
            navigator.mediaDevices.getUserMedia &&
            (window.AudioContext || window.webkitAudioContext) &&
            window.Worker
        );
    },

    start: async function (lang) {
        if (!this.isSupported()) {
            const msg = "Audio recording or Web Worker is not supported in this browser.";
            console.error(msg);
            if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechError', msg);
            }
            return false;
        }

        if (this.isListening) {
            this.stop();
        }

        this.currentLanguage = lang || 'kn-IN';
        this.initWorker();

        try {
            // Request microphone access
            this.mediaStream = await navigator.mediaDevices.getUserMedia({
                audio: {
                    channelCount: 1,
                    echoCancellation: true,
                    noiseSuppression: true,
                    autoGainControl: true,
                }
            });

            const AudioCtx = window.AudioContext || window.webkitAudioContext;
            // Attempt 16kHz context directly, fallback to hardware rate
            try {
                this.audioContext = new AudioCtx({ sampleRate: this.targetSampleRate });
            } catch (e) {
                this.audioContext = new AudioCtx();
            }

            this.actualSampleRate = this.audioContext.sampleRate;
            this.sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream);

            // ScriptProcessorNode for recording audio buffers
            const bufferSize = 4096;
            this.processorNode = this.audioContext.createScriptProcessor(bufferSize, 1, 1);

            this.audioBuffer = [];
            this.accumulatedSamples = 0;
            this.silenceSamples = 0;
            this.hasSpokenInSegment = false;
            this.isListening = true;

            const sampleRatio = this.actualSampleRate / this.targetSampleRate;
            // 1.2 seconds of silence at native sample rate
            const silenceLimitSamples = Math.round(1.2 * this.actualSampleRate);
            // Minimum 0.6 seconds of speech before triggering transcription
            const minSpeechSamples = Math.round(0.6 * this.actualSampleRate);
            // Maximum 7 seconds before chunking continuous speech
            const maxSegmentSamples = Math.round(7.0 * this.actualSampleRate);

            this.processorNode.onaudioprocess = (e) => {
                if (!this.isListening) return;

                const inputChannel = e.inputBuffer.getChannelData(0);
                const frameCopy = new Float32Array(inputChannel.length);
                frameCopy.set(inputChannel);

                // Compute RMS volume
                let sum = 0;
                for (let i = 0; i < frameCopy.length; i++) {
                    sum += frameCopy[i] * frameCopy[i];
                }
                const rms = Math.sqrt(sum / frameCopy.length);

                this.audioBuffer.push(frameCopy);
                this.accumulatedSamples += frameCopy.length;

                if (rms > this.silenceThreshold) {
                    this.hasSpokenInSegment = true;
                    this.silenceSamples = 0;
                } else {
                    if (this.hasSpokenInSegment) {
                        this.silenceSamples += frameCopy.length;
                    }
                }

                // Check if we should segment and transcribe
                const shouldFlushSilence = this.hasSpokenInSegment &&
                    this.silenceSamples >= silenceLimitSamples &&
                    this.accumulatedSamples >= minSpeechSamples;

                const shouldFlushMax = this.hasSpokenInSegment &&
                    this.accumulatedSamples >= maxSegmentSamples;

                if (shouldFlushSilence || shouldFlushMax) {
                    this.flushCurrentSegment();
                }
            };

            // Connect nodes (mute through silent gain to avoid feedback loop)
            const silentGain = this.audioContext.createGain();
            silentGain.gain.value = 0;

            this.sourceNode.connect(this.processorNode);
            this.processorNode.connect(silentGain);
            silentGain.connect(this.audioContext.destination);

            if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechStarted');
            }
            return true;
        } catch (err) {
            console.error('[SpeechInterop] Error starting audio capture:', err);
            this.isListening = false;
            if (this.dotNetRef) {
                this.dotNetRef.invokeMethodAsync('OnSpeechError', err.message || 'Microphone access denied');
            }
            return false;
        }
    },

    flushCurrentSegment: function () {
        if (this.audioBuffer.length === 0 || this.accumulatedSamples === 0) return;

        const totalLength = this.accumulatedSamples;
        const merged = new Float32Array(totalLength);
        let offset = 0;
        for (let i = 0; i < this.audioBuffer.length; i++) {
            merged.set(this.audioBuffer[i], offset);
            offset += this.audioBuffer[i].length;
        }

        // Reset buffer state
        this.audioBuffer = [];
        this.accumulatedSamples = 0;
        this.silenceSamples = 0;
        const hadSpeech = this.hasSpokenInSegment;
        this.hasSpokenInSegment = false;

        // Resample to 16,000 Hz if necessary
        const samples16k = this.resampleTo16k(merged, this.actualSampleRate);

        // Only send if had speech activity
        if (hadSpeech && samples16k.length > (0.4 * 16000) && this.worker) {
            const reqId = ++this.requestIdCounter;
            this.worker.postMessage({
                type: 'transcribe',
                id: reqId,
                audio: samples16k,
                language: this.currentLanguage
            });
        }
    },

    resampleTo16k: function (audioBuffer, sourceRate) {
        if (sourceRate === 16000) return audioBuffer;
        const ratio = 16000 / sourceRate;
        const targetLength = Math.round(audioBuffer.length * ratio);
        const result = new Float32Array(targetLength);
        for (let i = 0; i < targetLength; i++) {
            const srcIndex = i / ratio;
            const indexLow = Math.floor(srcIndex);
            const indexHigh = Math.min(indexLow + 1, audioBuffer.length - 1);
            const weight = srcIndex - indexLow;
            result[i] = audioBuffer[indexLow] * (1 - weight) + audioBuffer[indexHigh] * weight;
        }
        return result;
    },

    stop: function () {
        if (!this.isListening) return;
        this.isListening = false;

        // Process any remaining audio
        if (this.hasSpokenInSegment && this.accumulatedSamples > 0) {
            this.flushCurrentSegment();
        }

        // Disconnect audio nodes
        if (this.processorNode) {
            this.processorNode.disconnect();
            this.processorNode.onaudioprocess = null;
            this.processorNode = null;
        }

        if (this.sourceNode) {
            this.sourceNode.disconnect();
            this.sourceNode = null;
        }

        if (this.mediaStream) {
            this.mediaStream.getTracks().forEach(t => t.stop());
            this.mediaStream = null;
        }

        if (this.audioContext && this.audioContext.state !== 'closed') {
            this.audioContext.close().catch(() => {});
            this.audioContext = null;
        }

        this.audioBuffer = [];
        this.accumulatedSamples = 0;

        if (this.dotNetRef) {
            this.dotNetRef.invokeMethodAsync('OnSpeechEnded');
        }
    }
};
