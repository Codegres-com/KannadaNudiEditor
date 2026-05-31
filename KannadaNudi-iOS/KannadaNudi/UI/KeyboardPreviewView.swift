import SwiftUI
import Speech
import AVFoundation

struct TranslationLanguage: Identifiable, Hashable {
    let id = UUID()
    let name: String
    let code: String
    let locale: String
    let knName: String
}

struct MyMemoryResponse: Codable {
    let responseData: MyMemoryResponseData
}

struct MyMemoryResponseData: Codable {
    let translatedText: String
}

struct TranslateView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @StateObject private var speechManager = SpeechManager()
    
    // Translation States
    @State private var isSourceKannada = true
    @State private var selectedLanguage = TranslationLanguage(name: "English", code: "en", locale: "en-US", knName: "ಇಂಗ್ಲಿಷ್")
    @State private var sourceText = ""
    @State private var translatedText = ""
    
    // UI states
    @State private var showToast = false
    @State private var toastMessage = ""
    @FocusState private var isSourceFocused: Bool
    @State private var synthesizer = AVSpeechSynthesizer()
    
    // Core Translation Target Languages
    private let languages = [
        TranslationLanguage(name: "English", code: "en", locale: "en-US", knName: "ಇಂಗ್ಲಿಷ್"),
        TranslationLanguage(name: "Hindi", code: "hi", locale: "hi-IN", knName: "ಹಿಂದಿ"),
        TranslationLanguage(name: "Tamil", code: "ta", locale: "ta-IN", knName: "ತಮಿಳು"),
        TranslationLanguage(name: "Telugu", code: "te", locale: "te-IN", knName: "ತೆಲುಗು"),
        TranslationLanguage(name: "Malayalam", code: "ml", locale: "ml-IN", knName: "ಮಲಯಾಳಂ"),
        TranslationLanguage(name: "Spanish", code: "es", locale: "es-ES", knName: "ಸ್ಪ್ಯಾನಿಷ್"),
        TranslationLanguage(name: "French", code: "fr", locale: "fr-FR", knName: "ಫ್ರೆಂಚ್"),
        TranslationLanguage(name: "German", code: "de", locale: "de-DE", knName: "ಜರ್ಮನ್"),
        TranslationLanguage(name: "Chinese", code: "zh", locale: "zh-CN", knName: "ಚೈನೀಸ್")
    ]
    
    // Bidirectional Fast Phrase Map
    private let localPhrasesToEnglish = [
        "ನಮಸ್ಕಾರ, ಹೇಗಿದ್ದೀರಿ?": "Hello, how are you?",
        "ನಮಸ್ಕಾರ": "Hello / Welcome",
        "ಹೇಗಿದ್ದೀರಿ?": "How are you?",
        "ನಮಗೆ ಕನ್ನಡ ಗೊತ್ತು": "I know Kannada",
        "ಧನ್ಯವಾದಗಳು": "Thank you",
        "ಶುಭೋದಯ": "Good morning",
        "ಶುಭ ರಾತ್ರಿ": "Good night",
        "ನಿಮ್ಮ ಹೆಸರೇನು?": "What is your name?",
        "ನನ್ನ ಹೆಸರು": "My name is"
    ]

    private let localPhrasesToKannada = [
        "hello, how are you?": "ನಮಸ್ಕಾರ, ಹೇಗಿದ್ದೀರಿ?",
        "hello": "ನಮಸ್ಕಾರ",
        "how are you?": "ಹೇಗಿದ್ದೀರಿ?",
        "i know kannada": "ನಮಗೆ ಕನ್ನಡ ಗೊತ್ತು",
        "thank you": "ಧನ್ಯವಾದಗಳು",
        "good morning": "ಶುಭೋದಯ",
        "good night": "ಶುಭ ರಾತ್ರಿ",
        "what is your name?": "ನಿಮ್ಮ ಹೆಸರೇನು?",
        "my name is": "ನನ್ನ ಹೆಸರು"
    ]
    
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
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 16) {
                    
                    // Translation Card Container
                    VStack(spacing: 16) {
                        
                        // Language Selector Row
                        HStack(spacing: 0) {
                            leftLanguageSelector
                            
                            Button(action: swapTranslationDirection) {
                                Image(systemName: "arrow.left.and.right.righttriangle.left.righttriangle.right.fill")
                                    .font(.system(size: 16))
                                    .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                                    .frame(width: 40, height: 40)
                            }
                            .buttonStyle(PlainButtonStyle())
                            
                            rightLanguageSelector
                        }
                        .frame(height: 44)
                        .padding(.horizontal, 16)
                        .background(Color(red: 115/255, green: 92/255, blue: 0/255).opacity(0.08))
                        .cornerRadius(22)
                        .padding(.bottom, 4)
                        
                        // Soundwave + Status Layout
                        VStack(spacing: 8) {
                            WaveVisualizer(isListening: speechManager.isListening)
                            
                            Text(speechManager.isListening ? 
                                 langManager.getString(english: "LISTENING...", kannada: "ಆಲಿಸಲಾಗುತ್ತಿದೆ...") :
                                 langManager.getString(english: "TAP MICROPHONE TO TRANSLATE", kannada: "ಅನುವಾದಿಸಲು ಮೈಕ್ರೊಫೋನ್ ಟ್ಯಾಪ್ ಮಾಡಿ")
                            )
                            .font(.system(size: 10, weight: .bold))
                            .tracking(1.0)
                            .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                            .multilineTextAlignment(.center)
                        }
                        
                        // Source Area
                        ZStack(alignment: .top) {
                            if sourceText.isEmpty {
                                Text(langManager.getString(english: "Type or speak to translate...", kannada: "ಇಲ್ಲಿ ಬರೆಯಿರಿ ಅಥವಾ ಮಾತನಾಡಿ..."))
                                    .font(.system(size: 20, weight: .bold))
                                    .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255).opacity(0.35))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 16)
                                    .padding(.top, 8)
                            }
                            
                            TextEditor(text: $sourceText)
                                .font(.system(size: 20, weight: .bold))
                                .foregroundColor(Color(red: 87/255, green: 69/255, blue: 0/255))
                                .multilineTextAlignment(.center)
                                .frame(minHeight: 90)
                                .background(Color.clear)
                                .focused($isSourceFocused)
                        }
                        
                        // Source Actions Row
                        HStack(spacing: 12) {
                            // Translate Button
                            Button(action: translateText) {
                                HStack(spacing: 6) {
                                    Image(systemName: "translate")
                                        .font(.system(size: 14, weight: .bold))
                                    Text(langManager.getString(english: "TRANSLATE", kannada: "ಅನುವಾದಿಸಿ"))
                                        .font(.system(size: 11, weight: .bold))
                                        .tracking(0.5)
                                }
                                .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                                .padding(.horizontal, 16)
                                .frame(height: 40)
                                .background(Color.white)
                                .cornerRadius(20)
                                .shadow(color: Color.black.opacity(0.08), radius: 2, x: 0, y: 1)
                            }
                            .buttonStyle(PlainButtonStyle())
                            .disabled(sourceText.isEmpty)
                            .opacity(sourceText.isEmpty ? 0.5 : 1.0)
                            
                            // Speak Source Button
                            ActionCircleButton(systemImage: "speaker.wave.2.fill") {
                                speakText(isSource: true)
                            }
                            .disabled(sourceText.isEmpty)
                            .opacity(sourceText.isEmpty ? 0.5 : 1.0)
                            
                            // Copy Source Button
                            ActionCircleButton(systemImage: "doc.on.doc.fill") {
                                copyText(isSource: true)
                            }
                            .disabled(sourceText.isEmpty)
                            .opacity(sourceText.isEmpty ? 0.5 : 1.0)
                        }
                        
                        // Divider
                        Rectangle()
                            .fill(Color(red: 115/255, green: 92/255, blue: 0/255).opacity(0.12))
                            .frame(height: 1)
                            .padding(.vertical, 4)
                        
                        // Target Output Area
                        VStack(spacing: 12) {
                            if translatedText.isEmpty {
                                Text(langManager.getString(english: "Translation will appear here...", kannada: "ಅನುವಾದವು ಇಲ್ಲಿ ಗೋಚರಿಸುತ್ತದೆ..."))
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(Color.black.opacity(0.3))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 16)
                                    .frame(minHeight: 40)
                            } else {
                                Text(translatedText)
                                    .font(.system(size: 18, weight: .bold))
                                    .foregroundColor(Color(red: 45/255, green: 49/255, blue: 51/255))
                                    .multilineTextAlignment(.center)
                                    .padding(.horizontal, 16)
                                    .frame(minHeight: 40)
                            }
                            
                            // Target Actions Row
                            HStack(spacing: 16) {
                                // Speak Target Button
                                ActionCircleButton(systemImage: "speaker.wave.2.fill") {
                                    speakText(isSource: false)
                                }
                                .disabled(translatedText.isEmpty || translatedText == "Translating..." || translatedText == "ಅನುವಾದಿಸಲಾಗುತ್ತಿದೆ...")
                                .opacity(translatedText.isEmpty || translatedText == "Translating..." || translatedText == "ಅನುವಾದಿಸಲಾಗುತ್ತಿದೆ..." ? 0.5 : 1.0)
                                
                                // Copy Target Button
                                ActionCircleButton(systemImage: "doc.on.doc.fill") {
                                    copyText(isSource: false)
                                }
                                .disabled(translatedText.isEmpty || translatedText == "Translating..." || translatedText == "ಅನುವಾದಿಸಲಾಗುತ್ತಿದೆ...")
                                .opacity(translatedText.isEmpty || translatedText == "Translating..." || translatedText == "ಅನುವಾದಿಸಲಾಗುತ್ತಿದೆ..." ? 0.5 : 1.0)
                            }
                        }
                    }
                    .padding(20)
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
                             langManager.getString(english: "SPEAK NOW", kannada: "ಮಾತನಾಡಿ...") :
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
                    .padding(.top, 16)
                    .padding(.bottom, 40)
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
                .zIndex(100)
            }
        }
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
        .onAppear {
            UITextView.appearance().backgroundColor = .clear
        }
        .onDisappear {
            if speechManager.isListening {
                speechManager.stop()
            }
        }
        .onChange(of: speechManager.transcription) { newTranscription in
            if speechManager.isListening && !newTranscription.isEmpty {
                sourceText = newTranscription
            }
        }
        .onChange(of: speechManager.error) { newError in
            if let newError = newError {
                triggerToast(newError)
                speechManager.error = nil
            }
        }
    }
    
    // Left dropdown selector
    private var leftLanguageSelector: some View {
        Group {
            if isSourceKannada {
                Text(langManager.getString(english: "KANNADA", kannada: "ಕನ್ನಡ"))
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
            } else {
                Menu {
                    ForEach(languages, id: \.self) { lang in
                        Button(action: {
                            selectedLanguage = lang
                            translatedText = ""
                        }) {
                            Text(langManager.getString(english: lang.name, kannada: lang.knName))
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Text(langManager.getString(english: selectedLanguage.name, kannada: selectedLanguage.knName).uppercased())
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                        Image(systemName: "chevron.down")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
    
    // Right dropdown selector
    private var rightLanguageSelector: some View {
        Group {
            if !isSourceKannada {
                Text(langManager.getString(english: "KANNADA", kannada: "ಕನ್ನಡ"))
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
            } else {
                Menu {
                    ForEach(languages, id: \.self) { lang in
                        Button(action: {
                            selectedLanguage = lang
                            translatedText = ""
                        }) {
                            Text(langManager.getString(english: lang.name, kannada: lang.knName))
                        }
                    }
                } label: {
                    HStack(spacing: 4) {
                        Text(langManager.getString(english: selectedLanguage.name, kannada: selectedLanguage.knName).uppercased())
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                        Image(systemName: "chevron.down")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                    }
                }
            }
        }
        .frame(maxWidth: .infinity)
    }
    
    // Core Handlers
    private func swapTranslationDirection() {
        isSourceKannada.toggle()
        
        let tempSrc = sourceText
        sourceText = translatedText
        translatedText = tempSrc
        
        let targetName = langManager.getString(english: selectedLanguage.name, kannada: selectedLanguage.knName)
        let directionMsg = isSourceKannada ?
            langManager.getString(english: "Translating from Kannada", kannada: "ಕನ್ನಡದಿಂದ ಅನುವಾದ") :
            langManager.getString(english: "Translating from \(targetName)", kannada: "\(targetName)ದಿಂದ ಅನುವಾದ")
            
        triggerToast(directionMsg)
    }
    
    private func toggleListening() {
        if speechManager.isListening {
            speechManager.stop()
            triggerToast(langManager.getString(english: "Stopped Listening", kannada: "ಧ್ವನಿ ಗ್ರಹಿಕೆ ನಿಲ್ಲಿಸಲಾಗಿದೆ"))
            
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
                if !sourceText.isEmpty {
                    translateText()
                }
            }
        } else {
            isSourceFocused = false
            
            let targetLocale = isSourceKannada ? "kn-IN" : selectedLanguage.locale
            speechManager.clear()
            speechManager.start(localeIdentifier: targetLocale)
            triggerToast(langManager.getString(english: "Listening...", kannada: "ಆಲಿಸಲಾಗುತ್ತಿದೆ..."))
        }
    }
    
    private func speakText(isSource: Bool) {
        let text = isSource ? sourceText : translatedText
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        
        let speakInKannada = (isSource && isSourceKannada) || (!isSource && !isSourceKannada)
        if synthesizer.isSpeaking {
            synthesizer.stopSpeaking(at: .immediate)
        }
        let utterance = AVSpeechUtterance(string: trimmed)
        let voiceLang = speakInKannada ? "kn-IN" : selectedLanguage.locale
        
        guard let voice = AVSpeechSynthesisVoice(language: voiceLang) else {
            let targetName = langManager.getString(english: selectedLanguage.name, kannada: selectedLanguage.knName)
            let errorMsg = speakInKannada ?
                "Kannada TTS voice is not downloaded on this device.\nGo to Settings -> Accessibility -> Spoken Content -> Voices -> Kannada and download one." :
                "\(targetName) TTS voice is not downloaded on this device.\nGo to Settings -> Accessibility -> Spoken Content -> Voices and download it."
            triggerToast(errorMsg)
            return
        }
        
        utterance.voice = voice
        synthesizer.speak(utterance)
    }
    
    private func copyText(isSource: Bool) {
        let text = isSource ? sourceText : translatedText
        guard !text.isEmpty else { return }
        UIPasteboard.general.string = text
        triggerToast(langManager.getString(english: "Text copied to clipboard", kannada: "ಪಠ್ಯವನ್ನು ನಕಲಿಸಲಾಗಿದೆ"))
    }
    
    private func translateText() {
        let text = sourceText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !text.isEmpty else {
            translatedText = ""
            return
        }
        
        translatedText = langManager.getString(english: "Translating...", kannada: "ಅನುವಾದಿಸಲಾಗುತ್ತಿದೆ...")
        
        if isSourceKannada {
            for (kn, en) in localPhrasesToEnglish {
                if text.lowercased() == kn.lowercased() {
                    translatedText = en
                    return
                }
            }
        } else {
            for (en, kn) in localPhrasesToKannada {
                if text.lowercased() == en.lowercased() {
                    translatedText = kn
                    return
                }
            }
        }
        
        let langPair = isSourceKannada ? "kn|\(selectedLanguage.code)" : "\(selectedLanguage.code)|kn"
        
        var components = URLComponents(string: "https://api.mymemory.translated.net/get")
        components?.queryItems = [
            URLQueryItem(name: "q", value: text),
            URLQueryItem(name: "langpair", value: langPair)
        ]
        
        guard let url = components?.url else {
            translatedText = langManager.getString(english: "Translation failed", kannada: "ಅನುವಾದ ವಿಫಲವಾಗಿದೆ")
            return
        }
        
        URLSession.shared.dataTask(with: url) { data, response, error in
            DispatchQueue.main.async {
                if error != nil {
                    translatedText = langManager.getString(english: "Translation failed (Check network)", kannada: "ಅನುವಾದ ವಿಫಲವಾಗಿದೆ (ನೆಟ್‌ವರ್ಕ್ ಪರಿಶೀಲಿಸಿ)")
                    return
                }
                
                guard let data = data else {
                    translatedText = langManager.getString(english: "Translation failed", kannada: "ಅನುವಾದ ವಿಫಲವಾಗಿದೆ")
                    return
                }
                
                do {
                    let decoder = JSONDecoder()
                    let result = try decoder.decode(MyMemoryResponse.self, from: data)
                    let translation = result.responseData.translatedText
                    
                    var cleanTrans = translation.replacingOccurrences(of: "\\\"", with: "\"")
                    cleanTrans = cleanTrans.replacingOccurrences(of: "\\/", with: "/")
                    self.translatedText = cleanTrans
                } catch {
                    translatedText = langManager.getString(english: "Translation failed", kannada: "ಅನುವಾದ ವಿಫಲವಾಗಿದೆ")
                }
            }
        }.resume()
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
}
