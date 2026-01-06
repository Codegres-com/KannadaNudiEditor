import { pipeline, env } from 'https://cdn.jsdelivr.net/npm/@xenova/transformers@2.14.0';

// Skip local model checks
env.allowLocalModels = false;

// Singleton for the pipeline
class SpeechRecognitionPipeline {
    static task = 'automatic-speech-recognition';
    static model = 'Xenova/whisper-tiny';
    static instance = null;

    static async getInstance(progress_callback = null) {
        if (this.instance === null) {
            this.instance = await pipeline(this.task, this.model, { progress_callback });
        }
        return this.instance;
    }
}

self.addEventListener('message', async (event) => {
    const message = event.data;

    if (message.type === 'transcribe') {
        try {
            const transcriber = await SpeechRecognitionPipeline.getInstance((data) => {
                self.postMessage({ type: 'progress', data });
            });

            // Perform transcription
            const output = await transcriber(message.audioUrl, {
                 chunk_length_s: 30,
                 stride_length_s: 5,
                 language: message.language || null,
                 task: 'transcribe',
                 return_timestamps: false
            });

            self.postMessage({ type: 'success', text: output.text });

        } catch (error) {
            self.postMessage({ type: 'error', error: error.message });
        }
    }
});
