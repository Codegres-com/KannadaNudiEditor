import SwiftUI

struct ContentView: View {
    @ObservedObject var langManager = LanguageManager.shared
    @State private var selectedTab = 0
    
    init() {
        // Red bottom navigation that blends into the red half of the screen
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(red: 187/255, green: 0/255, blue: 30/255, alpha: 1.0) // #BB001E
        
        // Dynamic White & Yellow item state tint for navigation on red background
        // Unselected Item
        appearance.stackedLayoutAppearance.normal.iconColor = UIColor(red: 204/255, green: 204/255, blue: 204/255, alpha: 1.0) // #CCCCCC
        appearance.stackedLayoutAppearance.normal.titleTextAttributes = [
            .foregroundColor: UIColor(red: 204/255, green: 204/255, blue: 204/255, alpha: 1.0)
        ]
        
        // Selected Item
        appearance.stackedLayoutAppearance.selected.iconColor = UIColor(red: 255/255, green: 205/255, blue: 0/255, alpha: 1.0) // #FFCD00
        appearance.stackedLayoutAppearance.selected.titleTextAttributes = [
            .foregroundColor: UIColor(red: 255/255, green: 205/255, blue: 0/255, alpha: 1.0)
        ]
        
        UITabBar.appearance().standardAppearance = appearance
        if #available(iOS 15.0, *) {
            UITabBar.appearance().scrollEdgeAppearance = appearance
        }
        
        // Transparent top navigation bar
        let navAppearance = UINavigationBarAppearance()
        navAppearance.configureWithTransparentBackground()
        navAppearance.titleTextAttributes = [
            .foregroundColor: UIColor(red: 25/255, green: 28/255, blue: 30/255, alpha: 1.0), // #191C1E
            .font: UIFont.boldSystemFont(ofSize: 17)
        ]
        UINavigationBar.appearance().standardAppearance = navAppearance
        UINavigationBar.appearance().scrollEdgeAppearance = navAppearance
    }
    
    // Determine dynamic title for Navigation Bar
    private var pageTitle: String {
        switch selectedTab {
        case 0:
            return langManager.getString(english: "Kannada Voice", kannada: "ಕನ್ನಡ ಧ್ವನಿ")
        case 1:
            return langManager.getString(english: "Keyboard Setup", kannada: "ಕೀಬೋರ್ಡ್ ಸೆಟಪ್")
        case 2:
            return langManager.getString(english: "Translate", kannada: "ಅನುವಾದ")
        case 3:
            return langManager.getString(english: "Nudi Editor", kannada: "ನುಡಿ ಸಂಪಾದಕ")
        default:
            return ""
        }
    }
    
    var body: some View {
        NavigationView {
            TabView(selection: $selectedTab) {
                SpeechView()
                    .tabItem {
                        Label(langManager.getString(english: "Voice Type", kannada: "ಟೈಪ್"), systemImage: "mic.fill")
                    }
                    .tag(0)
                
                OnboardingView()
                    .tabItem {
                        Label(langManager.getString(english: "Setup", kannada: "ಸೆಟಪ್"), systemImage: "gearshape.fill")
                    }
                    .tag(1)
                
                TranslateView()
                    .tabItem {
                        Label(langManager.getString(english: "Translate", kannada: "ಅನುವಾದ"), systemImage: "arrow.left.and.right.righttriangle.left.righttriangle.right.fill")
                    }
                    .tag(2)
                
                EditorView()
                    .tabItem {
                        Label(langManager.getString(english: "Editor", kannada: "ಸಂಪಾದಕ"), systemImage: "pencil.and.outline")
                    }
                    .tag(3)
            }
            .background(Color(red: 187/255, green: 0/255, blue: 30/255)) // Fill bottom safe area with Red
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .principal) {
                    Text(pageTitle)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(Color(red: 25/255, green: 28/255, blue: 30/255))
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: {
                        langManager.toggleLanguage()
                    }) {
                        Text(langManager.currentLanguage == .kannada ? "English" : "ಕನ್ನಡ")
                            .font(.system(size: 13, weight: .bold))
                            .foregroundColor(Color(red: 115/255, green: 92/255, blue: 0/255))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.white)
                            .cornerRadius(12)
                            .shadow(color: Color.black.opacity(0.08), radius: 2, x: 0, y: 1)
                    }
                }
            }
        }
        .background(Color(red: 187/255, green: 0/255, blue: 30/255)) // Fill main stack with Red
        .navigationViewStyle(StackNavigationViewStyle())
    }
}
