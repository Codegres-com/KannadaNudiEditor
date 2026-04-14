import SwiftUI
import AVFoundation

struct OnboardingView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @State private var isKeyboardEnabled = false
    @State private var isFullAccessGranted = false
    @State private var isMicAuthorized = false
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Header
                Text(langManager.getString(english: "Welcome to Kannada Nudi", kannada: "ಕನ್ನಡ ನುಡಿ ಸುಸ್ವಾಗತ"))
                    .font(.title)
                    .bold()
                
                // Keyboard Setup Status
                VStack(alignment: .leading, spacing: 16) {
                    StatusRow(title: langManager.getString(english: "Keyboard Enabled", kannada: "ಕೀಬೋರ್ಡ್ ಸಕ್ರಿಯಗೊಳಿಸಲಾಗಿದೆ"), 
                               isActive: isKeyboardEnabled)
                    
                    StatusRow(title: langManager.getString(english: "Full Access Granted", kannada: "ಪೂರ್ಣ ಪ್ರವೇಶ ನೀಡಲಾಗಿದೆ"), 
                               isActive: isFullAccessGranted)
                    
                    if !isKeyboardEnabled || !isFullAccessGranted {
                        VStack(alignment: .leading, spacing: 12) {
                            Text(langManager.getString(english: "Action Required:", kannada: "ಕ್ರಮ ಅಗತ್ಯವಿದೆ:"))
                                .font(.headline)
                                .foregroundColor(.red)
                            
                            InstructionStep(number: 1, text: langManager.getString(english: "Tap 'Go to Settings' below", kannada: "'ಸೆಟ್ಟಿಂಗ್‌ಗಳಿಗೆ ಹೋಗಿ' ಕ್ಲಿಕ್ ಮಾಡಿ"))
                            InstructionStep(number: 2, text: langManager.getString(english: "Select 'Keyboards'", kannada: "'ಕೀಬೋರ್ಡ್‌ಗಳು' ಆಯ್ಕೆಮಾಡಿ"))
                            InstructionStep(number: 3, text: langManager.getString(english: "Enable 'Kannada Nudi' and 'Allow Full Access'", kannada: "'ಕನ್ನಡ ನುಡಿ' ಮತ್ತು 'Allow Full Access' ಸಕ್ರಿಯಗೊಳಿಸಿ"))
                        }
                        .padding()
                        .background(Color.red.opacity(0.05))
                        .cornerRadius(8)
                        
                        Button(action: openSettings) {
                            Text(langManager.getString(english: "Go to Settings", kannada: "ಸೆಟ್ಟಿಂಗ್‌ಗಳಿಗೆ ಹೋಗಿ"))
                                .bold()
                                .frame(maxWidth: .infinity)
                                .padding()
                                .background(Color.blue)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                    } else {
                        HStack {
                            Image(systemName: "sparkles")
                            Text(langManager.getString(english: "Keyboard is fully initialized and ready!", kannada: "ಕೀಬೋರ್ಡ್ ಸಂಪೂರ್ಣವಾಗಿ ಸಿದ್ಧವಾಗಿದೆ!"))
                        }
                        .font(.headline)
                        .foregroundColor(.green)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.green.opacity(0.1))
                        .cornerRadius(10)
                    }
                    
                    // Manual Refresh
                    Button(action: checkPermissions) {
                        Label(langManager.getString(english: "Check Status Again", kannada: "ಸ್ಥಿತಿಯನ್ನು ಮತ್ತೊಮ್ಮೆ ಪರಿಶೀಲಿಸಿ"), systemImage: "arrow.clockwise")
                            .font(.caption)
                    }
                    .frame(maxWidth: .infinity, alignment: .center)
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .shadow(radius: 2)
                
                // Microphone Permission
                MicrophonePermissionCard(isAuthorized: $isMicAuthorized)
                
                // Tips
                VStack(alignment: .leading, spacing: 12) {
                    Text(langManager.getString(english: "Pro Tips", kannada: "ಸಲಹೆಗಳು"))
                        .font(.headline)
                    
                    Text(langManager.getString(english: "• Switch keyboards by holding the 🌐 icon.", kannada: "• 🌐 ಐಕಾನ್ ಒತ್ತಿ ಹಿಡಿಯುವ ಮೂಲಕ ಕೀಬೋರ್ಡ್ ಬದಲಾಯಿಸಿ."))
                    Text(langManager.getString(english: "• 'Full Access' is safe—it only enables the translation feature.", kannada: "• 'ಪೂರ್ಣ ಪ್ರವೇಶ' ಸುರಕ್ಷಿತವಾಗಿದೆ—ಇದು ಅನುವಾದ ವೈಶಿಷ್ಟ್ಯಕ್ಕಾಗಿ ಮಾತ್ರ."))
                }
                .font(.footnote)
                .foregroundColor(.secondary)
                .padding()
            }
            .padding()
        }
        .background(Color(.systemGroupedBackground))
        .onAppear(perform: checkPermissions)
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            checkPermissions()
        }
    }
    
    private func checkPermissions() {
        // 1. Check if keyboard is enabled
        if let bundleID = Bundle.main.bundleIdentifier {
            let extensionID = "\(bundleID).KannadaKeyboard"
            let enabledKeyboards = UserDefaults.standard.array(forKey: "AppleKeyboards") as? [String] ?? []
            isKeyboardEnabled = enabledKeyboards.contains(extensionID)
        }
        
        // 2. Check if Full Access is granted
        // Full access allows the use of shared containers and network access (for translation models)
        isFullAccessGranted = UIInputViewController().hasFullAccess
        
        // 3. Mic status
        isMicAuthorized = AVAudioSession.sharedInstance().recordPermission == .granted
    }
    
    private func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }
}

struct StatusRow: View {
    let title: String
    let isActive: Bool
    
    var body: some View {
        HStack {
            Text(title)
                .font(.body)
            Spacer()
            HStack(spacing: 4) {
                Image(systemName: isActive ? "checkmark.circle.fill" : "xmark.circle.fill")
                Text(isActive ? "OK" : "Setup Required")
                    .bold()
            }
            .font(.subheadline)
            .foregroundColor(isActive ? .green : .red)
        }
    }
}

struct InstructionStep: View {
    let number: Int
    let text: String
    
    var body: some View {
        HStack(alignment: .top) {
            Text("\(number).")
                .bold()
                .foregroundColor(.blue)
            Text(text)
                .font(.subheadline)
        }
    }
}

struct MicrophonePermissionCard: View {
    @ObservedObject var langManager = LanguageManager.shared
    @Binding var isAuthorized: Bool
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(langManager.getString(english: "Microphone Access", kannada: "ಮೈಕ್ರೋಫೋನ್ ಪ್ರವೇಶ"))
                .font(.headline)
            
            Text(langManager.getString(english: "Required for voice-to-text features.", kannada: "ಧ್ವನಿ ಮೂಲಕ ಟೈಪ್ ಮಾಡಲು ಇದು ಅಗತ್ಯವಿದೆ."))
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            if !isAuthorized {
                Button(action: requestMic) {
                    Text(langManager.getString(english: "Enable Microphone", kannada: "ಮೈಕ್ರೋಫೋನ್ ಸಕ್ರಿಯಗೊಳಿಸಿ"))
                        .font(.subheadline)
                        .bold()
                        .padding(.vertical, 8)
                        .padding(.horizontal, 16)
                        .background(Color.blue.opacity(0.1))
                        .foregroundColor(.blue)
                        .cornerRadius(8)
                }
            } else {
                Text(langManager.getString(english: "✓ Authorized", kannada: "✓ ಅನುಮತಿಸಲಾಗಿದೆ"))
                    .font(.subheadline)
                    .foregroundColor(.green)
            }
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(radius: 2)
    }
    
    private func requestMic() {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            DispatchQueue.main.async {
                self.isAuthorized = granted
            }
        }
    }
}
