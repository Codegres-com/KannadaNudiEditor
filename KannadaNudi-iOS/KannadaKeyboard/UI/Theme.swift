import SwiftUI

struct Theme {
    // Karnataka Flag Colors (Refined for Premium Aesthetic)
    static let karnatakaRed = Color(red: 0.85, green: 0.12, blue: 0.12) // Sophisticated Carmine Red
    static let karnatakaYellow = Color(red: 1.0, green: 0.84, blue: 0.0) // Golden Yellow

    // UI Component Colors
    static let keyboardBackground = Color(red: 0.98, green: 0.95, blue: 0.85) // Softer cream/yellow for background

    static let keyBackground = Color.white
    static let keyNormalText = Color(red: 0.1, green: 0.1, blue: 0.1) // Soft Black

    static let keySpecialBackground = karnatakaRed
    static let keySpecialText = Color.white

    static let keyShadow = Color.black.opacity(0.15)
}
