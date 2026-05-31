import Foundation
import AVFoundation
import Speech
import Combine
import UIKit
import WebKit

class SpeechManager: NSObject, ObservableObject, SFSpeechRecognizerDelegate {
    @Published var transcription: String = ""
    @Published var isListening: Bool = false
    @Published var error: String?
    
    private var speechRecognizer: SFSpeechRecognizer?
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?
    private var recognitionTask: SFSpeechRecognitionTask?
    private let audioEngine = AVAudioEngine()
    private var webSpeechManager: SpeechWebViewManager?
    
    override init() {
        super.init()
    }
    
    func start(localeIdentifier: String = "kn-IN") {
        // 1. Request Microphone Permission sequentially first
        AVAudioSession.sharedInstance().requestRecordPermission { micGranted in
            guard micGranted else {
                DispatchQueue.main.async {
                    self.error = "Microphone access denied."
                    self.isListening = false
                }
                return
            }
            
            // Check if native speech recognition supports this locale
            let supported = SFSpeechRecognizer.supportedLocales().map { $0.identifier }
            if !supported.contains(localeIdentifier) {
                DispatchQueue.main.async {
                    self.startWebSpeech(localeIdentifier: localeIdentifier)
                }
                return
            }
            
            // 2. Request Speech Recognition permission second
            SFSpeechRecognizer.requestAuthorization { authStatus in
                DispatchQueue.main.async {
                    switch authStatus {
                    case .authorized:
                        self.performStart(localeIdentifier: localeIdentifier)
                    case .denied, .restricted, .notDetermined:
                        self.startWebSpeech(localeIdentifier: localeIdentifier)
                    @unknown default:
                        break
                    }
                }
            }
        }
    }
    
    private func performStart(localeIdentifier: String) {
        // 1. Establish playAndRecord Audio Session category and activate FIRST
        // This ensures the hardware inputNode is safely instantiated and accessible without crashing
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playAndRecord, mode: .measurement, options: [.duckOthers, .defaultToSpeaker])
            try audioSession.setActive(true, options: .notifyOthersOnDeactivation)
        } catch {
            self.startWebSpeech(localeIdentifier: localeIdentifier)
            return
        }
        
        // 2. Now call reset safely (accesses inputNode to removeTap)
        reset()
        
        // 3. Initialize SFSpeechRecognizer for the dynamic locale
        let recognizer = SFSpeechRecognizer(locale: Locale(identifier: localeIdentifier))
        
        if recognizer == nil || !recognizer!.isAvailable {
            self.startWebSpeech(localeIdentifier: localeIdentifier)
            return
        }
        
        guard let speechRecognizer = recognizer else { return }
        speechRecognizer.delegate = self
        self.speechRecognizer = speechRecognizer
        
        recognitionRequest = SFSpeechAudioBufferRecognitionRequest()
        guard let recognitionRequest = recognitionRequest else { return }
        recognitionRequest.shouldReportPartialResults = true
        
        let inputNode = audioEngine.inputNode
        
        recognitionTask = speechRecognizer.recognitionTask(with: recognitionRequest) { result, error in
            if let result = result {
                DispatchQueue.main.async {
                    self.transcription = result.bestTranscription.formattedString
                }
            }
            
            if let error = error {
                DispatchQueue.main.async {
                    self.error = "Recognition error: \(error.localizedDescription)"
                }
                self.stop()
            } else if result?.isFinal == true {
                self.stop()
            }
        }
        
        // --- THE FIX ---
        // Get the audio format from the input node.
        // We check both outputFormat and inputFormat to ensure a valid sample rate/channel count is used.
        var recordingFormat = inputNode.outputFormat(forBus: 0)
        if recordingFormat.sampleRate == 0 || recordingFormat.channelCount == 0 {
            recordingFormat = inputNode.inputFormat(forBus: 0)
        }
        
        // CRITICAL CHECK: If the sample rate is invalid (0), the engine will crash.
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
            self.startWebSpeech(localeIdentifier: localeIdentifier)
        }
    }
    
    private func startWebSpeech(localeIdentifier: String) {
        if self.webSpeechManager == nil {
            self.webSpeechManager = SpeechWebViewManager()
            
            self.webSpeechManager?.onStart = { [weak self] in
                self?.isListening = true
                self?.error = nil
            }
            
            self.webSpeechManager?.onResult = { [weak self] text in
                self?.transcription = text
            }
            
            self.webSpeechManager?.onError = { [weak self] errMsg in
                self?.error = "Web Speech Error: \(errMsg)"
                self?.stop()
            }
            
            self.webSpeechManager?.onEnd = { [weak self] in
                self?.isListening = false
            }
        }
        
        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playAndRecord, mode: .default, options: [.duckOthers, .defaultToSpeaker])
            try audioSession.setActive(true)
        } catch {
            self.error = "Audio Session Error: \(error.localizedDescription)"
            return
        }
        
        self.isListening = true
        self.webSpeechManager?.start(locale: localeIdentifier)
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
            
            self.webSpeechManager?.stop()
            
            // Deactivate and reset audio session cleanly to mute/unmute and release mic indicator
            do {
                let audioSession = AVAudioSession.sharedInstance()
                try audioSession.setActive(false, options: .notifyOthersOnDeactivation)
                try audioSession.setCategory(.ambient, mode: .default, options: [])
                try audioSession.setActive(true)
            } catch {
                print("Failed to release audio session cleanly: \(error)")
            }
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

class SpeechWebViewManager: NSObject, WKScriptMessageHandler, WKUIDelegate {
    private var webView: WKWebView!
    var onStart: (() -> Void)?
    var onResult: ((String) -> Void)?
    var onError: ((String) -> Void)?
    var onEnd: (() -> Void)?
    
    deinit {
        let viewToRemove = webView
        DispatchQueue.main.async {
            viewToRemove?.removeFromSuperview()
        }
    }
    
    override init() {
        super.init()
        let config = WKWebViewConfiguration()
        let controller = WKUserContentController()
        controller.add(self, name: "speechCallback")
        config.userContentController = controller
        config.mediaTypesRequiringUserActionForPlayback = []
        
        webView = WKWebView(frame: .zero, configuration: config)
        webView.uiDelegate = self
        
        let html = """
        <!DOCTYPE html>
        <html>
        <head><meta name="viewport" content="width=device-width, initial-scale=1.0"></head>
        <body>
        <script>
            var recognition;
            function startRecognition(locale) {
                try {
                    window.SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
                    if (!window.SpeechRecognition) {
                        window.webkit.messageHandlers.speechCallback.postMessage(JSON.stringify({type: "error", message: "Web Speech API not supported"}));
                        return;
                    }
                    if (recognition) { recognition.stop(); }
                    recognition = new window.SpeechRecognition();
                    recognition.continuous = true;
                    recognition.interimResults = true;
                    recognition.lang = locale;
                    
                    recognition.onstart = function() {
                        window.webkit.messageHandlers.speechCallback.postMessage(JSON.stringify({type: "start"}));
                    };
                    recognition.onresult = function(event) {
                        var transcription = "";
                        for (var i = 0; i < event.results.length; ++i) {
                            transcription += event.results[i][0].transcript;
                        }
                        window.webkit.messageHandlers.speechCallback.postMessage(JSON.stringify({type: "result", text: transcription}));
                    };
                    recognition.onerror = function(event) {
                        window.webkit.messageHandlers.speechCallback.postMessage(JSON.stringify({type: "error", message: event.error}));
                    };
                    recognition.onend = function() {
                        window.webkit.messageHandlers.speechCallback.postMessage(JSON.stringify({type: "end"}));
                    };
                    recognition.start();
                } catch (e) {
                    window.webkit.messageHandlers.speechCallback.postMessage(JSON.stringify({type: "error", message: e.message}));
                }
            }
            function stopRecognition() {
                if (recognition) { recognition.stop(); }
            }
        </script>
        </body>
        </html>
        """
        webView.loadHTMLString(html, baseURL: URL(string: "https://nudiweb.com"))
        
        // Add to active window hierarchy on main thread to unlock background mic capture and permission checks
        DispatchQueue.main.async {
            if let keyWindow = UIApplication.shared.windows.first(where: { $0.isKeyWindow }) {
                self.webView.frame = CGRect(x: 0, y: 0, width: 1, height: 1)
                self.webView.isHidden = true
                keyWindow.addSubview(self.webView)
            }
        }
    }
    
    func start(locale: String) {
        webView.evaluateJavaScript("startRecognition('\(locale)')", completionHandler: nil)
    }
    
    func stop() {
        webView.evaluateJavaScript("stopRecognition()", completionHandler: nil)
    }
    
    // MARK: - WKScriptMessageHandler
    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard let bodyString = message.body as? String,
              let data = bodyString.data(using: .utf8) else { return }
        
        struct CallbackMessage: Codable {
            let type: String
            let text: String?
            let message: String?
        }
        
        do {
            let decoder = JSONDecoder()
            let msg = try decoder.decode(CallbackMessage.self, from: data)
            switch msg.type {
            case "start":
                onStart?()
            case "result":
                if let t = msg.text { onResult?(t) }
            case "error":
                if let e = msg.message { onError?(e) }
            case "end":
                onEnd?()
            default:
                break
            }
        } catch {
            print("Failed to decode speech callback: \(error)")
        }
    }
    
    // MARK: - WKUIDelegate for Media Capture Permission
    @available(iOS 15.0, *)
    func webView(_ webView: WKWebView, requestMediaCapturePermissionFor origin: WKSecurityOrigin, initiatedByFrame frame: WKFrameInfo, type: WKMediaCaptureType, decisionHandler: @escaping (WKPermissionDecision) -> Void) {
        decisionHandler(.grant)
    }
}
