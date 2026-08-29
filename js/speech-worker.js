import { pipeline, env } from '../lib/transformers/transformers.min.js';

// Configure Transformers.js for 100% offline local execution
env.allowLocalModels = true;
env.allowRemoteModels = false;

// Set local paths relative to the web root
const origin = self.location.origin;
env.localModelPath = `${origin}/models/`;
env.backends.onnx.wasm.wasmPaths = `${origin}/lib/transformers/`;
env.backends.onnx.wasm.numThreads = 1; // 1 thread for maximum compatibility without requiring SharedArrayBuffer

let transcriber = null;
let isModelLoading = false;
let loadPromise = null;

async function getTranscriber() {
    if (transcriber) return transcriber;
    if (loadPromise) return loadPromise;

    isModelLoading = true;
    self.postMessage({ type: 'status', status: 'loading', message: 'Loading offline Whisper model...' });

    loadPromise = (async () => {
        try {
            console.log('[SpeechWorker] Initializing Whisper pipeline with local model...');
            transcriber = await pipeline('automatic-speech-recognition', 'Xenova/whisper-tiny', {
                progress_callback: (progress) => {
                    self.postMessage({ type: 'model_progress', progress });
                }
            });
            console.log('[SpeechWorker] Whisper model initialized successfully!');
            self.postMessage({ type: 'status', status: 'ready', message: 'Offline speech model ready' });
            return transcriber;
        } catch (err) {
            console.error('[SpeechWorker] Failed to load model:', err);
            self.postMessage({ type: 'status', status: 'error', error: err.message });
            loadPromise = null;
            throw err;
        } finally {
            isModelLoading = false;
        }
    })();

    return loadPromise;
}

// Normalize and transcribe Float32Array audio
async function transcribeAudio(audioData, languageCode) {
    const pipe = await getTranscriber();

    // Map language code to Whisper language name
    let lang = 'kannada';
    if (languageCode) {
        const lower = languageCode.toLowerCase();
        if (lower.startsWith('en') || lower === 'english') {
            lang = 'english';
        } else if (lower.startsWith('kn') || lower === 'kannada') {
            lang = 'kannada';
        }
    }

    console.log(`[SpeechWorker] Transcribing audio with language: ${lang} (${audioData.length} samples)`);

    const result = await pipe(audioData, {
        language: lang,
        task: 'transcribe',
        return_timestamps: false,
        chunk_length_s: 30,
        stride_length_s: 5,
    });

    let text = result && result.text ? result.text.trim() : '';

    // Filter out common Whisper blank/hallucination tokens on silence
    if (text === '[BLANK_AUDIO]' || text === '...' || text === '♪' || text === '♪♪') {
        text = '';
    }

    return text;
}

self.addEventListener('message', async (e) => {
    const { type, id, audio, language } = e.data;

    if (type === 'init') {
        try {
            await getTranscriber();
            self.postMessage({ type: 'initialized', id });
        } catch (err) {
            self.postMessage({ type: 'error', id, error: err.message });
        }
    } else if (type === 'transcribe') {
        try {
            self.postMessage({ type: 'transcribing', id });
            const text = await transcribeAudio(audio, language);
            self.postMessage({ type: 'result', id, text });
        } catch (err) {
            console.error('[SpeechWorker] Transcription error:', err);
            self.postMessage({ type: 'error', id, error: err.message });
        }
    }
});
