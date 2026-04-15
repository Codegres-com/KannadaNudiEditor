import SwiftUI

struct ContentView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @State private var selectedTab = 0
    
    var body: some View {
        NavigationView {
            TabView(selection: $selectedTab) {
                OnboardingView()
                    .tabItem {
                        Label(langManager.getString(english: "Setup", kannada: "ಸೆಟಪ್"), systemImage: "gear")
                    }
                    .tag(0)
                
                KeyboardPreviewView()
                    .tabItem {
                        Label(langManager.getString(english: "Keyboard", kannada: "ಕೀಬೋರ್ಡ್"), systemImage: "keyboard")
                    }
                    .tag(1)
                
                SpeechView()
                    .tabItem {
                        Label(langManager.getString(english: "Speech", kannada: "ಧ್ವನಿ"), systemImage: "mic")
                    }
                    .tag(2)
                
                EditorView()
                    .tabItem {
                        Label(langManager.getString(english: "Editor", kannada: "ಸಂಪಾದಕ"), systemImage: "pencil.and.outline")
                    }
                    .tag(3)
                
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text(langManager.getString(english: "Kannada Nudi", kannada: "ಕನ್ನಡ ನುಡಿ"))
                        .font(.headline)
                        .foregroundColor(.blue)
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        langManager.toggleLanguage()
                    }) {
                        Text(langManager.currentLanguage == .kannada ? "English" : "ಕನ್ನಡ")
                            .font(.subheadline)
                            .bold()
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(Color.blue.opacity(0.1))
                            .cornerRadius(4)
                    }
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}
