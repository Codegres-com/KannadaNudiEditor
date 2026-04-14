import SwiftUI
import Speech
import AVFoundation

struct SpeechView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @StateObject private var speechManager = SpeechManager()
    
    var body: some View {
        VStack(spacing: 20) {
            Text(langManager.getString(english: "Voice to Text", kannada: "ಧ್ವನಿಯಿಂದ ಪಠ್ಯ"))
                .font(.title2)
                .bold()
            
            TextEditor(text: $speechManager.transcription)
                .frame(maxHeight: .infinity)
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .overlay(
                    RoundedRectangle(cornerRadius: 12)
                        .stroke(Color.secondary.opacity(0.2), lineWidth: 1)
                )
            
            if let error = speechManager.error {
                Text(error)
                    .foregroundColor(.red)
                    .font(.caption)
            }
            
            HStack(spacing: 20) {
                Button(action: toggleListening) {
                    VStack {
                        Image(systemName: speechManager.isListening ? "stop.circle.fill" : "mic.circle.fill")
                            .font(.system(size: 60))
                            .foregroundColor(speechManager.isListening ? .red : .blue)
                        
                        Text(speechManager.isListening ? 
                             langManager.getString(english: "Stop", kannada: "ನಿಲ್ಲಿಸಿ") : 
                             langManager.getString(english: "Start Listening", kannada: "ಧ್ವನಿ ಪ್ರಾರಂಭಿಸಿ"))
                            .font(.subheadline)
                    }
                }
                
                Button(action: copyToClipboard) {
                    VStack {
                        Image(systemName: "doc.on.doc.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.gray)
                        Text(langManager.getString(english: "Copy", kannada: "ನಕಲಿಸಿ"))
                            .font(.subheadline)
                    }
                }
                .disabled(speechManager.transcription.isEmpty)
                
                Button(action: { speechManager.clear() }) {
                    VStack {
                        Image(systemName: "trash.fill")
                            .font(.system(size: 40))
                            .foregroundColor(.gray)
                        Text(langManager.getString(english: "Clear", kannada: "ಅಳಿಸಿ"))
                            .font(.subheadline)
                    }
                }
                .disabled(speechManager.transcription.isEmpty)
            }
            .padding(.bottom)
        }
        .padding()
        .background(Color(.systemGroupedBackground))
    }
    
    private func toggleListening() {
        if speechManager.isListening {
            speechManager.stop()
        } else {
            speechManager.start()
        }
    }
    
    private func copyToClipboard() {
        UIPasteboard.general.string = speechManager.transcription
    }
}
