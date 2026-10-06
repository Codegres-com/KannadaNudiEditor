import Foundation
import AVFoundation
import Combine
import Metal
import whisper

/// Kannada speech-to-text, fully on device, using whisper.cpp with a Kannada fine-tuned Whisper model.
///
/// Apple's Speech framework and WebKit's SpeechRecognition do not support Kannada, and general
/// Whisper models mostly write Kannada speech in Devanagari. The model used here
/// (vasista22/whisper-kannada-base, IIT Madras, Apache 2.0, q5_1 ggml, ~60 MB) is trained on Kannada
/// and scored 2.2% character error rate in testing, vs 22-25% for general Whisper models up to 626 MB.
/// The model is bundled in the app, so voice typing works offline from first launch.
/// whisper.cpp runs on the CPU of every iPhone, so no Core ML / Neural Engine compilation is needed.
@MainActor
final class SpeechManager: ObservableObject {
    @Published var transcription: String = ""
    @Published var isListening: Bool = false
    /// True while the model is loading or the final chunk is being transcribed
    @Published var isBusy: Bool = false
    /// Progress text to show while busy (preparing...)
    @Published var status: String?
    @Published var error: String?

    private static let sampleRate = 16_000
    // Whisper decodes 30s windows; commit text and start a new window before that
    private static let maxWindowSamples = 20 * sampleRate

    private var engine: WhisperEngine?
    private let recorder = MicRecorder()
    private var loopTask: Task<Void, Never>?
    private var language = "kn"
    /// Text already finalized (existing text before this session + completed windows)
    private var committedText = ""

    // MARK: - Public API

    func start(localeIdentifier: String = "kn-IN") {
        guard !isListening, !isBusy else { return }
        error = nil
        language = String(localeIdentifier.prefix(2))

        Task {
            guard await Self.requestMicrophonePermission() else {
                error = "Microphone access denied. Enable it in Settings > Privacy > Microphone."
                return
            }

            do {
                let engine = try await loadEngine()
                try recorder.start()
                committedText = transcription
                isListening = true
                loopTask = Task { await transcribeLoop(engine) }
            } catch {
                isBusy = false
                status = nil
                self.error = "Voice typing failed: \(error.localizedDescription)"
                recorder.stop()
            }
        }
    }

    func stop() {
        guard isListening else { return }
        isListening = false
        recorder.stop()

        guard let engine else { return }
        let samples = recorder.samples
        let pending = loopTask
        loopTask = nil
        guard samples.count > Self.sampleRate / 2 else { return }

        // Final pass once the live loop has finished its current pass
        isBusy = true
        Task {
            await pending?.value
            let text = await transcribe(engine, samples)
            transcription = Self.join(committedText, text)
            committedText = transcription
            isBusy = false
        }
    }

    func clear() {
        transcription = ""
        committedText = ""
    }

    // MARK: - Model

    private func loadEngine() async throws -> WhisperEngine {
        if let engine { return engine }

        isBusy = true
        defer {
            isBusy = false
            status = nil
        }

        Self.removeOldDownloadedModels()
        guard let path = Bundle.main.path(forResource: "ggml-kn-base", ofType: "bin") else {
            throw SpeechError.modelLoadFailed
        }
        setStatus(english: "Preparing voice typing...", kannada: "ಧ್ವನಿ ಟೈಪಿಂಗ್ ಸಿದ್ಧವಾಗುತ್ತಿದೆ...")
        let engine = try await Task.detached(priority: .userInitiated) {
            try WhisperEngine(modelPath: path)
        }.value
        self.engine = engine
        return engine
    }

    /// Earlier builds downloaded voice models (~216 MB WhisperKit in Documents, ~60 MB in
    /// Application Support); the model is now bundled, so free that space
    private static func removeOldDownloadedModels() {
        let fileManager = FileManager.default
        if let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first {
            try? fileManager.removeItem(at: documents.appendingPathComponent("huggingface"))
        }
        if let support = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first {
            try? fileManager.removeItem(at: support.appendingPathComponent("VoiceModels"))
        }
        UserDefaults.standard.removeObject(forKey: "whisperModelFolder")
    }

    private func setStatus(english: String, kannada: String) {
        status = LanguageManager.shared.getString(english: english, kannada: kannada)
    }

    // MARK: - Transcription

    private func transcribeLoop(_ engine: WhisperEngine) async {
        var lastCount = 0
        while isListening && !Task.isCancelled {
            try? await Task.sleep(nanoseconds: 700_000_000)
            guard isListening else { break }

            let samples = recorder.samples
            // Wait for at least 0.5s of new audio
            guard samples.count - lastCount >= Self.sampleRate / 2 else { continue }
            lastCount = samples.count

            let text = await transcribe(engine, samples)
            guard isListening else { break }
            transcription = Self.join(committedText, text)

            // Window nearly full: finalize it and keep only audio that arrived since the snapshot
            if samples.count >= Self.maxWindowSamples {
                committedText = transcription
                recorder.purge(keepingLast: max(0, recorder.samples.count - samples.count))
                lastCount = 0
            }
        }
    }

    private func transcribe(_ engine: WhisperEngine, _ samples: [Float]) async -> String {
        // Whisper hallucinates text on silence, so skip windows without speech
        guard let peak = samples.lazy.map(abs).max(), peak > 0.02 else { return "" }
        // Surround speech with 1s of silence: when audio stops right at the last word (as it does
        // during live updates) the model invents extra words. Padding cut test-clip errors from 12.5% to 2.2%.
        let silence = [Float](repeating: 0, count: Self.sampleRate)
        let text = await engine.transcribe(silence + samples + silence, language: language)
        // Remove non-speech annotations such as "[BLANK_AUDIO]" or "(music)"
        return text
            .replacingOccurrences(of: #"\[[^\]]*\]|\([^)]*\)"#, with: "", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func join(_ base: String, _ addition: String) -> String {
        guard !addition.isEmpty else { return base }
        guard !base.isEmpty else { return addition }
        let separator = (base.last?.isWhitespace ?? true) ? "" : " "
        return base + separator + addition
    }

    private static func requestMicrophonePermission() async -> Bool {
        await withCheckedContinuation { continuation in
            AVAudioSession.sharedInstance().requestRecordPermission { continuation.resume(returning: $0) }
        }
    }
}

enum SpeechError: LocalizedError {
    case modelLoadFailed
    case microphoneUnavailable

    var errorDescription: String? {
        switch self {
        case .modelLoadFailed: return "Could not load the voice model."
        case .microphoneUnavailable: return "Microphone is not available."
        }
    }
}

// MARK: - whisper.cpp

/// Wrapper over a whisper.cpp context; the actor serializes all calls into the C library
actor WhisperEngine {
    private let context: OpaquePointer

    init(modelPath: String) throws {
        var params = whisper_context_default_params()
        // The GPU path is only reliable on A14 and newer (Apple7 GPU family); A12/A13 (iPhone XR/11) use the CPU
        let useGPU = MTLCreateSystemDefaultDevice()?.supportsFamily(.apple7) ?? false
        params.use_gpu = useGPU
        params.flash_attn = useGPU
        guard let context = whisper_init_from_file_with_params(modelPath, params) else {
            throw SpeechError.modelLoadFailed
        }
        self.context = context
    }

    deinit {
        whisper_free(context)
    }

    func transcribe(_ samples: [Float], language: String) -> String {
        // Beam search, as whisper-cli uses by default; greedy decoding invented whole sentences in testing
        var params = whisper_full_default_params(WHISPER_SAMPLING_BEAM_SEARCH)
        params.n_threads = Int32(max(1, min(4, ProcessInfo.processInfo.activeProcessorCount - 1)))
        params.translate = false
        params.no_context = true
        // Settings match whisper-cli (-l kn -nt), which scored 9.8% CER on the Kannada test clips.
        // Do not enable suppress_nst: with it this model invents news-style sentences.
        params.no_timestamps = true
        params.print_special = false
        params.print_progress = false
        params.print_realtime = false
        params.print_timestamps = false
        params.suppress_blank = true

        let result: Int32 = language.withCString { languagePointer in
            params.language = languagePointer
            return samples.withUnsafeBufferPointer { buffer in
                whisper_full(context, params, buffer.baseAddress, Int32(buffer.count))
            }
        }
        guard result == 0 else { return "" }

        var text = ""
        for segment in 0..<whisper_full_n_segments(context) {
            if let segmentText = whisper_full_get_segment_text(context, segment) {
                text += String(cString: segmentText)
            }
        }
        return text
    }
}

// MARK: - Microphone

/// Records the microphone as 16 kHz mono Float32 samples, the format Whisper expects
final class MicRecorder {
    private let engine = AVAudioEngine()
    private let lock = NSLock()
    private var buffer: [Float] = []
    private var converter: AVAudioConverter?
    private let targetFormat = AVAudioFormat(commonFormat: .pcmFormatFloat32, sampleRate: 16_000, channels: 1, interleaved: false)!

    var samples: [Float] {
        lock.lock()
        defer { lock.unlock() }
        return buffer
    }

    func start() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playAndRecord, mode: .measurement, options: [.defaultToSpeaker, .duckOthers])
        try session.setActive(true, options: .notifyOthersOnDeactivation)

        let input = engine.inputNode
        let inputFormat = input.outputFormat(forBus: 0)
        guard inputFormat.sampleRate > 0, inputFormat.channelCount > 0 else {
            throw SpeechError.microphoneUnavailable
        }
        converter = AVAudioConverter(from: inputFormat, to: targetFormat)

        lock.lock()
        buffer = []
        lock.unlock()

        input.removeTap(onBus: 0)
        input.installTap(onBus: 0, bufferSize: 4096, format: inputFormat) { [weak self] pcm, _ in
            self?.append(pcm)
        }
        engine.prepare()
        try engine.start()
    }

    func stop() {
        engine.stop()
        engine.inputNode.removeTap(onBus: 0)
        // Release the mic so the recording indicator goes away and other audio resumes
        do {
            let session = AVAudioSession.sharedInstance()
            try session.setActive(false, options: .notifyOthersOnDeactivation)
            try session.setCategory(.ambient, mode: .default, options: [])
        } catch {
            print("Failed to release audio session cleanly: \(error)")
        }
    }

    /// Drops recorded audio, keeping only the last `keep` samples
    func purge(keepingLast keep: Int) {
        lock.lock()
        if buffer.count > keep {
            buffer.removeFirst(buffer.count - keep)
        }
        lock.unlock()
    }

    private func append(_ pcm: AVAudioPCMBuffer) {
        guard let converter else { return }
        let ratio = targetFormat.sampleRate / pcm.format.sampleRate
        let capacity = AVAudioFrameCount(Double(pcm.frameLength) * ratio) + 32
        guard let output = AVAudioPCMBuffer(pcmFormat: targetFormat, frameCapacity: capacity) else { return }

        var consumed = false
        var conversionError: NSError?
        converter.convert(to: output, error: &conversionError) { _, status in
            if consumed {
                status.pointee = .noDataNow
                return nil
            }
            consumed = true
            status.pointee = .haveData
            return pcm
        }
        guard conversionError == nil, let channel = output.floatChannelData else { return }

        let converted = UnsafeBufferPointer(start: channel[0], count: Int(output.frameLength))
        lock.lock()
        buffer.append(contentsOf: converted)
        lock.unlock()
    }
}
