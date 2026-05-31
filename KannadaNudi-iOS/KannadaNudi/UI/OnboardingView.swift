import SwiftUI
import AVFoundation

struct OnboardingView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @State private var isKeyboardEnabled = false
    @State private var isFullAccessGranted = false
    @State private var isMicAuthorized = false
    
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
                VStack(spacing: 0) {
                    // Card Setup Container
                    VStack(spacing: 16) {
                        // Welcome Title
                        Text(langManager.getString(
                            english: "Welcome to Kannada Nudi Keyboard",
                            kannada: "ಕನ್ನಡ ನುಡಿ ಕೀಬೋರ್ಡ್‌ಗೆ ಸುಸ್ವಾಗತ"
                        ))
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                        .multilineTextAlignment(.center)
                        .padding(.top, 8)
                        
                        // Keyboard Preview Image
                        Image("nudi_keyboard_preview")
                            .resizable()
                            .aspectRatio(contentMode: .fill)
                            .frame(height: 120)
                            .clipped()
                            .cornerRadius(16)
                            .overlay(
                                RoundedRectangle(cornerRadius: 16)
                                    .stroke(Color.black.opacity(0.08), lineWidth: 1)
                            )
                            .padding(.bottom, 8)
                        
                        // Setup Instruction text
                        Text(langManager.getString(
                            english: "In Order to get a Kannada Keyboard across all Apps,",
                            kannada: "ಎಲ್ಲಾ ಅಪ್ಲಿಕೇಶನ್‌ಗಳಲ್ಲಿ ಕನ್ನಡ ಕೀಬೋರ್ಡ್ ಪಡೆಯಲು,"
                        ))
                        .font(.system(size: 14))
                        .foregroundColor(Color(red: 87/255, green: 69/255, blue: 0/255))
                        .multilineTextAlignment(.center)
                        
                        Text(langManager.getString(
                            english: "Switch Input method to Kannada Nudi",
                            kannada: "ಇಲ್ಲಿ ಇನ್‌ಪುಟ್ ವಿಧಾನವನ್ನು ಕನ್ನಡ ನುಡಿಗೆ ಬದಲಾಯಿಸಿ"
                        ))
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(Color(red: 187/255, green: 0/255, blue: 30/255))
                        .multilineTextAlignment(.center)
                        .padding(.bottom, 8)
                        
                        // Step Details Based on Status
                        if !isKeyboardEnabled {
                            VStack(spacing: 8) {
                                Text(langManager.getString(
                                    english: "Enable in System Settings to start typing:",
                                    kannada: "ಈ ಕೀಬೋರ್ಡ್ ಬಳಸಲು ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಸಕ್ರಿಯಗೊಳಿಸಿ:"
                                ))
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(Color(red: 87/255, green: 69/255, blue: 0/255))
                                .multilineTextAlignment(.center)
                                
                                Text(langManager.getString(
                                    english: "1. Click the button below.\n2. Go to 'Keyboards'.\n3. Turn ON 'Kannada Nudi' and 'Allow Full Access'.",
                                    kannada: "1. ಕೆಳಗಿನ ಬಟನ್ ಕ್ಲಿಕ್ ಮಾಡಿ.\n2. 'ಕೀಬೋರ್ಡ್‌ಗಳು' ಆಯ್ಕೆಮಾಡಿ.\n3. 'ಕನ್ನಡ ನುಡಿ' ಮತ್ತು 'ಪೂರ್ಣ ಪ್ರವೇಶ' ಆನ್ ಮಾಡಿ."
                                ))
                                .font(.system(size: 12))
                                .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                                .lineSpacing(4)
                                .padding(.bottom, 12)
                                
                                Button(action: openSettings) {
                                    Text(langManager.getString(
                                        english: "Enable Keyboard in Settings",
                                        kannada: "ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಕೀಬೋರ್ಡ್ ಸಕ್ರಿಯಗೊಳಿಸಿ"
                                    ))
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color(red: 187/255, green: 0/255, blue: 30/255))
                                    .cornerRadius(24)
                                }
                            }
                        } else if !isFullAccessGranted {
                            VStack(spacing: 8) {
                                Text(langManager.getString(
                                    english: "Allow Full Access to enable all features:",
                                    kannada: "ಎಲ್ಲಾ ವೈಶಿಷ್ಟ್ಯಗಳಿಗಾಗಿ ಪೂರ್ಣ ಪ್ರವೇಶವನ್ನು ನೀಡಿ:"
                                ))
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(Color(red: 87/255, green: 69/255, blue: 0/255))
                                .multilineTextAlignment(.center)
                                
                                Text(langManager.getString(
                                    english: "1. Click the button below.\n2. Select 'Keyboards'.\n3. Toggle 'Allow Full Access' to ON.",
                                    kannada: "1. ಕೆಳಗಿನ ಬಟನ್ ಕ್ಲಿಕ್ ಮಾಡಿ.\n2. 'ಕೀಬೋರ್ಡ್‌ಗಳು' ಆಯ್ಕೆಮಾಡಿ.\n3. 'ಪೂರ್ಣ ಪ್ರವೇಶವನ್ನು ಅನುಮತಿಸಿ' ಆನ್ ಮಾಡಿ."
                                ))
                                .font(.system(size: 12))
                                .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                                .lineSpacing(4)
                                .padding(.bottom, 12)
                                
                                Button(action: openSettings) {
                                    Text(langManager.getString(
                                        english: "Allow Full Access in Settings",
                                        kannada: "ಸೆಟ್ಟಿಂಗ್‌ಗಳಲ್ಲಿ ಪೂರ್ಣ ಪ್ರವೇಶ ಅನುಮತಿಸಿ"
                                    ))
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color(red: 187/255, green: 0/255, blue: 30/255))
                                    .cornerRadius(24)
                                }
                            }
                        } else {
                            VStack(spacing: 8) {
                                Text(langManager.getString(
                                    english: "You are all set! The keyboard is ready to use.",
                                    kannada: "ನೀವು ಸಿದ್ಧರಾಗಿದ್ದೀರಿ! ಕೀಬೋರ್ಡ್ ಬಳಸಲು ಸಿದ್ಧವಾಗಿದೆ."
                                ))
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(Color(red: 87/255, green: 69/255, blue: 0/255))
                                .multilineTextAlignment(.center)
                                
                                Text(langManager.getString(
                                    english: "Hold the globe icon 🌐 in any app to switch.",
                                    kannada: "ಬದಲಾಯಿಸಲು ಯಾವುದೇ ಅಪ್ಲಿಕೇಶನ್‌ನಲ್ಲಿ 🌐 ಐಕಾನ್ ಒತ್ತಿ ಹಿಡಿಯಿರಿ."
                                ))
                                .font(.system(size: 12))
                                .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                                .padding(.bottom, 12)
                                
                                Button(action: {}) {
                                    Text(langManager.getString(
                                        english: "Keyboard is Ready",
                                        kannada: "ಕೀಬೋರ್ಡ್ ಸಿದ್ಧವಾಗಿದೆ"
                                    ))
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white.opacity(0.8))
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color.green)
                                    .cornerRadius(24)
                                }
                                .disabled(true)
                            }
                        }
                        
                        // Divider
                        Rectangle()
                            .fill(Color(red: 115/255, green: 92/255, blue: 0/255).opacity(0.12))
                            .frame(height: 1)
                            .padding(.vertical, 8)
                        
                        // Microphone Permission Section
                        Text(langManager.getString(english: "Microphone Permission", kannada: "ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ"))
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                        
                        if isMicAuthorized {
                            Text(langManager.getString(
                                english: "Microphone permission granted — voice input is enabled.",
                                kannada: "ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ನೀಡಲಾಗಿದೆ — ಧ್ವನಿ ಇನ್‌ಪುಟ್ ಸಕ್ರಿಯವಾಗಿದೆ."
                            ))
                            .font(.system(size: 12))
                            .foregroundColor(Color(red: 87/255, green: 69/255, blue: 0/255))
                            .multilineTextAlignment(.center)
                        } else {
                            VStack(spacing: 12) {
                                Text(langManager.getString(
                                    english: "Microphone permission is needed for voice input on the keyboard.",
                                    kannada: "ಕೀಬೋರ್ಡ್‌ನಲ್ಲಿ ಧ್ವನಿ ಇನ್‌ಪುಟ್‌ಗಾಗಿ ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ಅಗತ್ಯವಿದೆ."
                                ))
                                .font(.system(size: 12))
                                .foregroundColor(Color(red: 87/255, green: 69/255, blue: 0/255))
                                .multilineTextAlignment(.center)
                                
                                Button(action: requestMic) {
                                    Text(langManager.getString(
                                        english: "Grant Microphone Permission",
                                        kannada: "ಮೈಕ್ರೋಫೋನ್ ಅನುಮತಿ ನೀಡಿ"
                                    ))
                                    .font(.system(size: 15, weight: .bold))
                                    .foregroundColor(.white)
                                    .frame(maxWidth: .infinity)
                                    .frame(height: 48)
                                    .background(Color(red: 115/255, green: 92/255, blue: 0/255))
                                    .cornerRadius(24)
                                }
                            }
                        }
                    }
                    .padding(24)
                    .background(Color(red: 250/255, green: 246/255, blue: 232/255))
                    .cornerRadius(32)
                    .shadow(color: Color.black.opacity(0.12), radius: 8, x: 0, y: 4)
                    .padding(.horizontal, 20)
                    .padding(.top, 72)
                    .padding(.bottom, 40)
                }
            }
        }
        .onTapGesture {
            UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        }
        .onAppear(perform: checkPermissions)
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willEnterForegroundNotification)) { _ in
            checkPermissions()
        }
    }
    
    private func checkPermissions() {
        // 1. Check if keyboard is enabled in System settings
        if let bundleID = Bundle.main.bundleIdentifier {
            let extensionID = "\(bundleID).KannadaKeyboard"
            let enabledKeyboards = UserDefaults.standard.array(forKey: "AppleKeyboards") as? [String] ?? []
            isKeyboardEnabled = enabledKeyboards.contains(extensionID)
        }
        
        // 2. Check if full access is toggled ON
        isFullAccessGranted = UIInputViewController().hasFullAccess
        
        // 3. Mic authorization status
        isMicAuthorized = AVAudioSession.sharedInstance().recordPermission == .granted
    }
    
    private func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }
    
    private func requestMic() {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            DispatchQueue.main.async {
                self.isMicAuthorized = granted
            }
        }
    }
}
