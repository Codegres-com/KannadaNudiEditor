import Foundation
import AVFoundation
import Speech
import Combine

class SpeechManager: NSObject, ObservableObject, SFSpeechRecognizerDelegate {
    @Published var transcription: String = ""
    @Published var isListening: Bool = false
    @Published var error: String?
    
    private let speechRecognizer = SFSpeechRecognizer(locale: Locale(identifier: "kn-IN"))
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    
    override init() {
        super.init()
        self.speechRecognizer?.delegate = self
    }
    
    func start() {
        SFSpeechRecognizer.requestAuthorization { authStatus in
            DispatchQueue.main.async {
                switch authStatus {
                case .authorized:
                    self.performStart()
                case .denied, .restricted, .notDetermined:
                    self.error = "Microphone or Speech access denied."
                    self.isListening = false
                @unknown default:
                    break
                }
            }
        }
    }
    
    private func performStart() {
        // Stop any existing task
        reset()
        
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            self.error = "Audio Session Error: \(error.localizedDescription)"
            return
        }
        
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else { return }
        recognitionRequest.shouldReportPartialResults = true
        
        let inputNode = audioEngine.inputNode
        
        recognitionTask = speechRecognizer?.recognitionTask(with: recognitionRequest) { result, error in
            if let result = result {
                DispatchQueue.main.async {
                    self.transcription = result.bestTranscription.formattedString
                }
            }
            
            if error != nil || result?.isFinal == true {
                self.stop()
            }
        }
        
        // --- THE FIX ---
        // Get the hardware format from the input node.
        // We use the inputFormat because it matches the hardware better than outputFormat in some scenarios.
        let recordingFormat = inputNode.inputFormat(forBus: 0)
        
        // CRITICAL CHECK: If the sample rate is invalid (0), the engine will crash.
        // This is the cause of "IsFormatSampleRateAndChannelCountValid(format)" crash.
        guard recordingFormat.sampleRate > 0 && recordingFormat.channelCount > 0 else {
            self.error = "Invalid audio format detected. Please try again."
            self.stop()
            return
        }
        
        inputNode.removeTap(onBus: 0) // Ensure fresh tap
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            self.recognitionRequest?.append(buffer)
        }
        
        audioEngine.prepare()
        
        do {
            try audioEngine.start()
            isListening = true
        } catch {
            self.error = "Audio Engine Error: \(error.localizedDescription)"
            self.stop()
        }
    }
    
    func stop() {
        DispatchQueue.main.async {
            self.isListening = false
            self.audioEngine.stop()
            self.audioEngine.inputNode.removeTap(onBus: 0)
            self.recognitionRequest?.endAudio()
            self.recognitionRequest = nil
            self.recognitionTask?.cancel()
            self.recognitionTask = nil
        }
    }
    
    private func reset() {
        audioEngine.stop()
        audioEngine.inputNode.removeTap(onBus: 0)
        recognitionTask?.cancel()
        recognitionTask = nil
    }
    
    func clear() {
        transcription = ""
    }
}
