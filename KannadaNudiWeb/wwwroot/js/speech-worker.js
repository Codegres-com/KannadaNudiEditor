import { pipeline, env } from 'https://cdn.jsdelivr.net/npm/@xenova/transformers@2.14.0';

// Skip local model checks since we are running in browser
env.allowLocalModels = false;

let transcriber = null;

self.addEventListener('message', async (event) => {
    const message = event.data;

    if (message.type === 'init') {
        try {
            // Load the model
            // We use 'Xenova/whisper-tiny' as it is small (~40MB) and multilingual
            self.postMessage({ type: 'status', status: 'loading' });
            transcriber = await pipeline('automatic-speech-recognition', 'Xenova/whisper-tiny');
            self.postMessage({ type: 'status', status: 'ready' });
        } catch (error) {
            console.error(error);
            self.postMessage({ type: 'error', error: error.message });
        }
    } else if (message.type === 'transcribe') {
        if (!transcriber) {
            self.postMessage({ type: 'error', error: 'Model not initialized' });
            return;
        }

        try {
            const audio = message.audio; // Float32Array
            // Map full locale (e.g., 'kn-IN') to 2-letter code ('kn')
            let lang = message.language || 'kn';
            if (lang.includes('-')) {
                lang = lang.split('-')[0];
            }

            console.log(`Worker processing audio chunk. Length: ${audio.length}, Language: ${lang}`);

            // Run transcription
            // chunk_length_s: 30 helps prevent hallucinations on short audio by simulating a full context window
            const output = await transcriber(audio, {
                language: lang,
                task: 'transcribe',
                chunk_length_s: 30
            });

            // Output is usually { text: "..." }
            let text = output.text;
            if (Array.isArray(output)) {
                text = output.map(x => x.text).join(' ');
            }

            self.postMessage({ type: 'result', text: text });
        } catch (error) {
            console.error(error);
            self.postMessage({ type: 'error', error: error.message });
        }
    }
});
