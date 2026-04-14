import Foundation
import Combine

class LanguageManager: ObservableObject {
    static let shared = LanguageManager()
    
    enum Language: String {
        case english = "en"
        case kannada = "kn"
    }
    
    @Published var currentLanguage: Language = .kannada
    
    private let prefKey = "app_language"
    
    private init() {
        if let stored = UserDefaults.standard.string(forKey: prefKey), let lang = Language(rawValue: stored) {
            self.currentLanguage = lang
        }
    }
    
    func toggleLanguage() {
        currentLanguage = (currentLanguage == .kannada) ? .english : .kannada
        UserDefaults.standard.set(currentLanguage.rawValue, forKey: prefKey)
    }
    
    func setLanguage(_ lang: Language) {
        currentLanguage = lang
        UserDefaults.standard.set(currentLanguage.rawValue, forKey: prefKey)
    }
    
    func isKannada() -> Bool {
        return currentLanguage == .kannada
    }
    
    func getString(english: String, kannada: String) -> String {
        return currentLanguage == .kannada ? kannada : english
    }
}
