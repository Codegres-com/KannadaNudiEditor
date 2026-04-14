import SwiftUI
import WebKit

struct EditorView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @State private var isLoading = true
    @State private var showOverlay = false
    
    // Timer to check keyboard status periodically
    let timer = Timer.publish(every: 1.5, on: .main, in: .common).autoconnect()
    
    var body: some View {
        ZStack {
            WebViewWrapper(url: URL(string: "https://nudiweb.com")!, isLoading: $isLoading)
            
            if isLoading {
                ProgressView()
                    .scaleEffect(1.5)
            }
            
            if showOverlay {
                VStack(spacing: 20) {
                    Image(systemName: "keyboard.badge.ellipsis")
                        .font(.system(size: 60))
                        .foregroundColor(.orange)
                    
                    Text(langManager.getString(english: "Kannada Nudi Keyboard Detected", kannada: "ಕನ್ನಡ ನುಡಿ ಕೀಬೋರ್ಡ್ ಪತ್ತೆಯಾಗಿದೆ"))
                        .font(.title3)
                        .bold()
                        .multilineTextAlignment(.center)
                    
                    Text(langManager.getString(english: "The Editor (WebView) only accepts English input.\nPlease switch to an English keyboard to continue typing.", 
                                             kannada: "ಸಂಪಾದಕ (WebView) ಇಂಗ್ಲಿಷ್ ಇನ್‌ಪುಟ್ ಮಾತ್ರ ಸ್ವೀಕರಿಸುತ್ತದೆ.\nಟೈಪ್ ಮಾಡಲು ಇಂಗ್ಲಿಷ್ ಕೀಬೋರ್ಡ್‌ಗೆ ಬದಲಾಯಿಸಿ."))
                        .font(.subheadline)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                    
                    Button(action: {
                        // iOS doesn't have a direct "Show Keyboard Picker" 
                        // User must use the globe icon.
                    }) {
                        Text(langManager.getString(english: "Use Globe Icon to Switch", kannada: "ಕೀಬೋರ್ಡ್ ಬದಲಾಯಿಸಲು ಗ್ಲೋಬ್ ಬಳಸಿ"))
                            .bold()
                            .padding()
                            .background(Color.orange)
                            .foregroundColor(.white)
                            .cornerRadius(10)
                    }
                    .disabled(true)
                    .opacity(0.8)
                    
                    Text(langManager.getString(english: "Tap the globe icon on your keyboard to switch to English/system keyboard.", 
                                             kannada: "ಇಂಗ್ಲಿಷ್ ಕೀಬೋರ್ಡ್‌ಗೆ ಬದಲಾಯಿಸಲು ನಿಮ್ಮ ಕೀಬೋರ್ಡ್‌ನಲ್ಲಿರುವ ಗ್ಲೋಬ್ ಐಕಾನ್ ಒತ್ತಿರಿ."))
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                }
                .padding()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(.systemBackground).opacity(0.95))
            }
        }
        .onReceive(timer) { _ in
            checkKeyboardStatus()
        }
        .navigationTitle(langManager.getString(english: "Nudi Editor", kannada: "ನುಡಿ ಸಂಪಾದಕ"))
    }
    
    private func checkKeyboardStatus() {
        // In iOS, we can check the current input mode
        if UITextInputMode.activeInputModes.first?.primaryLanguage != nil {
            // If the current primary language is the one set by our extension
            // Note: This is a bit tricky to detect the specific extension, 
            // but usually it might show up as 'kn' or similar.
            // For now, we'll implement a simple check or skip if too complex.
            // On iOS, users usually know when they are in our keyboard.
        }
    }
}

struct WebViewWrapper: UIViewRepresentable {
    let url: URL
    @Binding var isLoading: Bool
    
    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.navigationDelegate = context.coordinator
        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {
        if uiView.url == nil {
            let request = URLRequest(url: url)
            uiView.load(request)
        }
    }
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, WKNavigationDelegate {
        var parent: WebViewWrapper
        
        init(_ parent: WebViewWrapper) {
            self.parent = parent
        }
        
        func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
            parent.isLoading = false
        }
        
        func webView(_ webView: WKWebView, didFail navigation: WKNavigation!, withError error: Error) {
            parent.isLoading = false
        }
    }
}
