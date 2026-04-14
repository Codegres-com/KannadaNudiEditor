import SwiftUI

struct KeyboardPreviewView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @StateObject private var keyboardViewModel = KeyboardViewModel()
    @State private var typedText: String = ""
    private var engine = TransliterationEngine()
    
    var body: some View {
        VStack(spacing: 0) {
            // Preview Header
            VStack {
                Text(langManager.getString(english: "Keyboard Preview", kannada: "ಕೀಬೋರ್ಡ್ ಮುನ್ನೋಟ"))
                    .font(.headline)
                    .padding(.top)
                
                Text(langManager.getString(english: "Test the keyboard layout and transliteration here.", kannada: "ಕೀಬೋರ್ಡ್ ವಿನ್ಯಾಸ ಮತ್ತು ಲಿಪ್ಯಂತರವನ್ನು ಇಲ್ಲಿ ಪರೀಕ್ಷಿಸಿ."))
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.bottom)
            }
            .frame(maxWidth: .infinity)
            .background(Color(.systemBackground))
            
            // Text Area
            ScrollView {
                Text(typedText.isEmpty ? langManager.getString(english: "Type something...", kannada: "ಏನನ್ನಾದರೂ ಟೈಪ್ ಮಾಡಿ...") : typedText)
                    .font(.title3)
                    .foregroundColor(typedText.isEmpty ? .secondary : .primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
            }
            .frame(maxHeight: .infinity)
            .background(Color(.secondarySystemBackground))
            
            Divider()
            
            // The Shared Keyboard View
            KeyboardView(
                viewModel: keyboardViewModel,
                onAction: handleKeyboardAction,
                onCandidateSelected: handleCandidateSelection
            )
            .frame(height: 250) // Adjust height as needed
            .background(Theme.keyboardBackground)
        }
        .navigationTitle(langManager.getString(english: "Try Keyboard", kannada: "ಕೀಬೋರ್ಡ್ ಪರೀಕ್ಷಿಸಿ"))
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func handleKeyboardAction(_ action: KeyboardAction) {
        switch action {
        case .character(let char):
            let lastChar = typedText.last
            let result = engine.getTransliteration(key: char, lastCommittedChar: lastChar)
            
            // Remove chars if engine requested backspace for composition
            if result.backspaceCount > 0 {
                typedText = String(typedText.dropLast(result.backspaceCount))
            }
            
            typedText += result.text
            
        case .backspace:
            engine.clearBuffer()
            if !typedText.isEmpty {
                typedText.removeLast()
            }
            
        case .space:
            engine.clearBuffer()
            typedText += " "
            
        case .enter:
            engine.clearBuffer()
            typedText += "\n"
            
        case .globe, .dictation:
            // Optional: Show switch keyboard alert
            break
            
        case .modeChange, .alphaChange:
            engine.clearBuffer()
            
        case .layoutChange:
            engine.setLayout(keyboardViewModel.currentLayout == .baraha ? .nudi : .baraha)
            
        case .dismiss:
            // In app preview, maybe just clear
            typedText = ""
            
        case .shift:
            break
        }
    }
    
    private func handleCandidateSelection(_ candidate: String) {
        // Find last word and replace with candidate
        if let lastWord = typedText.components(separatedBy: .whitespacesAndNewlines).last {
            typedText = String(typedText.dropLast(lastWord.count))
            typedText += candidate + " "
        }
        engine.clearBuffer()
        keyboardViewModel.candidates = []
    }
}
