import SwiftUI
import Speech
import AVFoundation

struct SpeechView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @StateObject private var speechManager = SpeechManager()
    
    // In-App Custom Keyboard Integration
    @State private var showInAppKeyboard = false
    @StateObject private var keyboardViewModel = KeyboardViewModel()
    private var engine = TransliterationEngine()
    
    // UI Feedback States
    @State private var showToast = false
    @State private var toastMessage = ""
    @State private var synthesizer = AVSpeechSynthesizer()
    @FocusState private var isInputFocused: Bool
    
    var body: some View {
        ZStack {
            // Karnataka Split Background
            GeometryReader { geo in
                VStack(spacing: 0) {
                    Color(red: 255/255, green: 205/255, blue: 0/255)
                        .frame(height: geo.size.height * 0.55)
                    Color(red: 187/255, green: 0/255, blue: 30/255)
                        .frame(height: geo.size.height * 0.45)
                }
                .ignoresSafeArea()
            }
            
            VStack(spacing: 0) {
                ScrollView(showsIndicators: false) {
                    VStack(spacing: 16) {
                        
                        // Transcription Card
                        VStack(spacing: 16) {
                            
                            // Soundwave Visualizer + Status Layout
                            VStack(spacing: 8) {
                                WaveVisualizer(isListening: speechManager.isListening)
                                
                                Text(speechManager.isListening ? 
                                     langManager.getString(english: "LISTENING...", kannada: "ಆಲಿಸಲಾಗುತ್ತಿದೆ...") :
                                     langManager.getString(english: "TAP MICROPHONE TO SPEAK", kannada: "ಮಾತನಾಡಲು ಮೈಕ್ರೊಫೋನ್ ಟ್ಯಾಪ್ ಮಾಡಿ")
                                )
                                .font(.system(size: 11, weight: .bold))
                                .tracking(1.5)
                                .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                                .multilineTextAlignment(.center)
                            }
                            .padding(.top, 8)
                            
                            // Transcription Input Editor
                            ZStack(alignment: .top) {
                                if speechManager.transcription.isEmpty {
                                    Text(langManager.getString(english: "Type or speak...", kannada: "ಟೈಪ್ ಮಾಡಿ ಅಥವಾ ಮಾತನಾಡಿ..."))
                                        .font(.system(size: 24, weight: .bold))
                                        .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255).opacity(0.35))
                                        .multilineTextAlignment(.center)
                                        .padding(.horizontal, 16)
                                        .padding(.top, 8)
                                }
                                
                                TextEditor(text: $speechManager.transcription)
                                    .font(.system(size: 24, weight: .bold))
                                    .foregroundColor(Color(red: 87/255, green: 69/255, blue: 0/255))
                                    .multilineTextAlignment(.center)
                                    .frame(minHeight: 140)
                                    .background(Color.clear)
                                    .focused($isInputFocused)
                                    .onChange(of: isInputFocused) { focused in
                                        if focused {
                                            // When focus is requested, hide the custom keyboard to let the system keyboard show up, or toggle.
                                            showInAppKeyboard = false
                                        }
                                    }
                            }
                            
                            // Actions Row
                            HStack(spacing: 20) {
                                // Speak / Volume Button
                                ActionCircleButton(systemImage: "speaker.wave.2.fill") {
                                    speakText()
                                }
                                .disabled(speechManager.transcription.isEmpty)
                                .opacity(speechManager.transcription.isEmpty ? 0.5 : 1.0)
                                
                                // Copy Button
                                ActionCircleButton(systemImage: "doc.on.doc.fill") {
                                    copyText()
                                }
                                .disabled(speechManager.transcription.isEmpty)
                                .opacity(speechManager.transcription.isEmpty ? 0.5 : 1.0)
                                
                                // Keyboard Toggle Button
                                ActionCircleButton(systemImage: "keyboard.fill") {
                                    toggleKeyboard()
                                }
                            }
                            .padding(.bottom, 8)
                        }
                        .padding(24)
                        .background(Color(red: 250/255, green: 246/255, blue: 232/255))
                        .cornerRadius(32)
                        .shadow(color: Color.black.opacity(0.12), radius: 8, x: 0, y: 4)
                        .padding(.horizontal, 20)
                        .padding(.top, 72)
                        
                        // Mic Section Container (Bottom Red Area Context)
                        VStack(spacing: 12) {
                            Button(action: toggleListening) {
                                PulsingMicCircle(isListening: speechManager.isListening)
                            }
                            .buttonStyle(PlainButtonStyle())
                            
                            Text(speechManager.isListening ? 
                                 langManager.getString(english: "SPEAK NOW", kannada: "ನಡು ಮಾತನಾಡುತ್ತಿದೆ") :
                                 langManager.getString(english: "TAP AND SPEAK", kannada: "ಟ್ಯಾಪ್ ಮಾಡಿ ಮತ್ತು ಮಾತನಾಡಿ")
                            )
                            .font(.system(size: 20, weight: .bold))
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)
                            
                            Text(speechManager.isListening ?
                                 langManager.getString(english: "LISTENING...", kannada: "ಆಲಿಸಲಾಗುತ್ತಿದೆ...") :
                                 langManager.getString(english: "TAP TO SPEAK", kannada: "ಮಾತನಾಡಿ")
                            )
                            .font(.system(size: 12, weight: .bold))
                            .tracking(1.0)
                            .foregroundColor(.white.opacity(0.8))
                        }
                        .padding(.top, 24)
                        .padding(.bottom, 40)
                    }
                }
                
                // In-App Custom Keyboard Overlay Drawer
                if showInAppKeyboard {
                    VStack(spacing: 0) {
                        HStack {
                            Spacer()
                            Button(action: { showInAppKeyboard = false }) {
                                Text(langManager.getString(english: "Done", kannada: "ಮುಗಿದಿದೆ"))
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.blue)
                                    .padding(.horizontal, 16)
                                    .padding(.vertical, 8)
                            }
                        }
                        .background(Color(.secondarySystemBackground))
                        
                        KeyboardView(
                            viewModel: keyboardViewModel,
                            onAction: handleKeyboardAction,
                            onCandidateSelected: handleCandidateSelection
                        )
                        .frame(height: 250)
                        .background(Theme.keyboardBackground)
                    }
                    .transition(.move(edge: .bottom))
                    .zIndex(10)
                }
            }
            
            // Toast Notification Overlay
            if showToast {
                VStack {
                    Spacer()
                    Text(toastMessage)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                        .background(Color.black.opacity(0.75))
                        .cornerRadius(20)
                        .transition(.opacity)
                        .padding(.bottom, 160)
                }
                .ignoresSafeArea(.keyboard)
            }
        }
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
            showInAppKeyboard = false
        }
        .onAppear {
            engine.setLayout(.nudi)
            keyboardViewModel.currentLayout = .nudi
            UITextView.appearance().backgroundColor = .clear
        }
        .onDisappear {
            if speechManager.isListening {
                speechManager.stop()
            }
        }
        .onChange(of: speechManager.error) { newError in
            if let newError = newError {
                triggerToast(newError)
                speechManager.error = nil
            }
        }
    }
    
    // Core Handlers
    private func toggleListening() {
        if speechManager.isListening {
            speechManager.stop()
            triggerToast(langManager.getString(english: "Stopped Listening", kannada: "ಧ್ವನಿ ಗ್ರಹಿಕೆ ನಿಲ್ಲಿಸಲಾಗಿದೆ"))
        } else {
            // Dismiss keyboard
            isInputFocused = false
            showInAppKeyboard = false
            speechManager.start(localeIdentifier: "kn-IN")
            triggerToast(langManager.getString(english: "Listening...", kannada: "ಆಲಿಸಲಾಗುತ್ತಿದೆ..."))
        }
    }
    
    private func speakText() {
        let text = speechManager.transcription.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else { return }
        
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        
        let utterance = AVSpeechUtterance(string: text)
        let isEnglish = text.first?.isLetter == true && text.first?.isASCII == true
        let voiceLang = isEnglish ? "en-US" : "kn-IN"
        
        guard let voice = AVSpeechSynthesisVoice(language: voiceLang) else {
            let errorMsg = isEnglish ? 
                "English voice is not available." :
                "Kannada TTS voice is not downloaded on this device.\nGo to Settings -> Accessibility -> Spoken Content -> Voices -> Kannada and download one."
            triggerToast(errorMsg)
            return
        }
        
        utterance.voice = voice
        synthesizer.speak(utterance)
    }
    
    private func copyText() {
        let text = speechManager.transcription
        guard !text.isEmpty else { return }
        UIPasteboard.general.string = text
        triggerToast(langManager.getString(english: "Text copied to clipboard", kannada: "ಪಠ್ಯವನ್ನು ನಕಲಿಸಲಾಗಿದೆ"))
    }
    
    private func toggleKeyboard() {
        if showInAppKeyboard {
            showInAppKeyboard = false
        } else {
            isInputFocused = false // Resign system focus
            showInAppKeyboard = true
        }
    }
    
    private func triggerToast(_ message: String) {
        toastMessage = message
        withAnimation {
            showToast = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            withAnimation {
                showToast = false
            }
        }
    }
    
    // Keyboard Actions
    private func handleKeyboardAction(_ action: KeyboardAction) {
        switch action {
        case .character(let char):
            let lastChar = speechManager.transcription.last
            let result = engine.getTransliteration(key: char, lastCommittedChar: lastChar)
            
            if result.backspaceCount > 0 {
                speechManager.transcription = String(speechManager.transcription.dropLast(result.backspaceCount))
            }
            speechManager.transcription += result.text
            
        case .backspace:
            engine.clearBuffer()
            if !speechManager.transcription.isEmpty {
                speechManager.transcription.removeLast()
            }
            
        case .space:
            engine.clearBuffer()
            speechManager.transcription += " "
            
        case .enter:
            engine.clearBuffer()
            speechManager.transcription += "\n"
            
        case .globe, .dictation:
            break
            
        case .modeChange, .alphaChange:
            engine.clearBuffer()
            
        case .layoutChange:
            let nextLayout: KeyboardLayout = keyboardViewModel.currentLayout == .baraha ? .nudi : .baraha
            engine.setLayout(nextLayout)
            keyboardViewModel.currentLayout = nextLayout
            
        case .dismiss:
            showInAppKeyboard = false
            
        case .shift:
            break
        }
    }
    
    private func handleCandidateSelection(_ candidate: String) {
        if let lastWord = speechManager.transcription.components(separatedBy: .whitespacesAndNewlines).last {
            speechManager.transcription = String(speechManager.transcription.dropLast(lastWord.count))
            speechManager.transcription += candidate + " "
        }
        engine.clearBuffer()
        keyboardViewModel.candidates = []
    }
}

// Sub components for Premium Styling
struct WaveVisualizer: View {
    let isListening: Bool
    @State private var waveHeights: [CGFloat] = [10, 10, 10, 10, 10]
    let timer = Timer.publish(every: 0.15, on: .main, in: .common).autoconnect()
    
    var body: some View {
        HStack(spacing: 4) {
            ForEach(0..<5) { index in
                RoundedRectangle(cornerRadius: 2)
                    .fill(Color(red: 187/255, green: 0/255, blue: 30/255))
                    .frame(width: 4, height: isListening ? waveHeights[index] : 10)
                    .animation(.easeInOut(duration: 0.15), value: waveHeights[index])
            }
        }
        .frame(height: 40)
        .opacity(0.8)
        .onReceive(timer) { _ in
            if isListening {
                for i in 0..<5 {
                    waveHeights[i] = CGFloat.random(in: 6...30)
                }
            }
        }
    }
}

struct ActionCircleButton: View {
    let systemImage: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            ZStack {
                Circle()
                    .fill(Color.white)
                    .frame(width: 44, height: 44)
                    .shadow(color: Color.black.opacity(0.08), radius: 2, x: 0, y: 1)
                
                Image(systemName: systemImage)
                    .font(.system(size: 18))
                    .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
            }
        }
        .buttonStyle(PlainButtonStyle())
    }
}

struct PulsingMicCircle: View {
    let isListening: Bool
    @State private var scale: CGFloat = 1.0
    
    var body: some View {
        ZStack {
            Circle()
                .fill(isListening ? Color(red: 255/255, green: 179/255, blue: 174/255) : Color(red: 255/255, green: 218/255, blue: 215/255))
                .frame(width: 120, height: 120)
                .overlay(
                    Circle()
                        .stroke(isListening ? Color(red: 187/255, green: 0/255, blue: 30/255) : Color.white, lineWidth: isListening ? 4 : 2)
                )
                .shadow(color: Color.black.opacity(0.2), radius: 10, x: 0, y: 5)
                .scaleEffect(isListening ? scale : 1.0)
            
            Image(systemName: "mic.fill")
                .font(.system(size: 48))
                .foregroundColor(isListening ? .white : Color(red: 187/255, green: 0/255, blue: 30/255))
        }
        .onAppear {
            if isListening {
                startPulsing()
            }
        }
        .onChange(of: isListening) { newValue in
            if newValue {
                startPulsing()
            } else {
                scale = 1.0
            }
        }
    }
    
    private func startPulsing() {
        withAnimation(Animation.easeInOut(duration: 0.9).repeatForever(autoreverses: true)) {
            scale = 1.12
        }
    }
}
