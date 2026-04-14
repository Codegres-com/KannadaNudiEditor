import SwiftUI

enum KeyboardAction {
    case character(String)
    case backspace
    case enter
    case shift
    case space
    case globe
    case dictation
    case modeChange // 123
    case alphaChange // ABC
    case layoutChange // Nudi/Baraha
    case dismiss
}

enum KeyboardMode {
    case alpha
    case numeric
}

class KeyboardViewModel: ObservableObject {
    @Published var isShifted = false
    @Published var currentLayout: KeyboardLayout = .baraha
    @Published var currentMode: KeyboardMode = .alpha
    @Published var candidates: [String] = []

    func toggleShift() {
        isShifted.toggle()
    }

    func toggleLayout() {
        currentLayout = (currentLayout == .baraha) ? .nudi : .baraha
    }

    func toggleMode() {
        currentMode = (currentMode == .alpha) ? .numeric : .alpha
    }
}

struct KeyboardView: View {
    @ObservedObject var viewModel: KeyboardViewModel
    var onAction: (KeyboardAction) -> Void
    var onCandidateSelected: (String) -> Void

    let qwertyRow1 = ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"]
    let qwertyRow2 = ["a", "s", "d", "f", "g", "h", "j", "k", "l"]
    let qwertyRow3 = ["z", "x", "c", "v", "b", "n", "m"]

    // Nudi Layout keys (Normal)
    let nudiRow1 = ["ಟ", "ಡ", "ಎ", "ರ", "ತ", "ಯ", "ಉ", "ಇ", "ಒ", "ಪ"]
    let nudiRow1Shift = ["ಠ", "ಢ", "ಏ", "ಋ", "ಥ", "ಐ", "ಊ", "ಈ", "ಓ", "ಫ"]
    let nudiRow1Codes = ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"]

    let nudiRow2 = ["ಅ", "ಸ", "ದ", "್", "ಗ", "ಹ", "ಜ", "ಕ", "ಲ"]
    let nudiRow2Shift = ["ಆ", "ಶ", "ಧ", "್", "ಘ", "ಃ", "ಝ", "ಖ", "ಳ"]
    let nudiRow2Codes = ["a", "s", "d", "f", "g", "h", "j", "k", "l"]

    let nudiRow3 = ["ಞ", "ಷ", "ಚ", "ವ", "ಬ", "ನ", "ಮ"]
    let nudiRow3Shift = ["ಙ", "ಷ", "ಛ", "ಔ", "ಭ", "ಣ", "ಮ"]
    let nudiRow3Codes = ["z", "x", "c", "v", "b", "n", "m"]

    // Numeric Layout
    let numRow1 = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"]
    let numRow2 = ["-", "/", ":", ";", "(", ")", "$", "&", "@", "\""]
    let numRow3 = [".", ",", "?", "!", "\'"]

    var body: some View {
        VStack(spacing: 0) {
            // Candidates
            if !viewModel.candidates.isEmpty {
                CandidatesRow(candidates: viewModel.candidates, onSelect: onCandidateSelected)
            }

            // Toolbar
            HStack {
                Text(viewModel.currentLayout == .baraha ? "Baraha (Phonetic)" : "Nudi (Direct)")
                    .font(.caption)
                    .foregroundColor(Theme.keySpecialBackground)
                    .bold()
                Spacer()
                Button(action: {
                    onAction(.layoutChange)
                    viewModel.toggleLayout()
                }) {
                    Text("Switch Engine")
                        .font(.caption)
                        .bold()
                        .padding(6)
                        .foregroundColor(Theme.keySpecialText)
                        .background(Theme.keySpecialBackground)
                        .cornerRadius(4)
                }
            }
            .padding(4)
            .background(Color.white.opacity(0.8))

            // Keys
            VStack(spacing: 8) {
                if viewModel.currentMode == .numeric {
                    renderNumeric()
                } else if viewModel.currentLayout == .baraha {
                    renderQwerty()
                } else {
                    renderNudi()
                }

                renderBottomRow()
            }
            .padding(4)
            .background(Theme.keyboardBackground)
        }
    }

    // MARK: - Render Methods

    func renderQwerty() -> some View {
        VStack(spacing: 10) {
            HStack(spacing: 6) {
                ForEach(qwertyRow1, id: \.self) { key in
                    keyButton(label: key, code: key)
                }
            }
            HStack(spacing: 6) {
                Spacer(minLength: 10)
                ForEach(qwertyRow2, id: \.self) { key in
                    keyButton(label: key, code: key)
                }
                Spacer(minLength: 10)
            }
            HStack(spacing: 6) {
                specialKey(label: "⇧", width: 40, isPressed: viewModel.isShifted) {
                    onAction(.shift)
                    viewModel.toggleShift()
                }
                ForEach(qwertyRow3, id: \.self) { key in
                    keyButton(label: key, code: key)
                }
                specialKey(label: "⌫", width: 40) {
                    onAction(.backspace)
                }
            }
        }
    }

    func renderNudi() -> some View {
        VStack(spacing: 10) {
            HStack(spacing: 6) {
                ForEach(0..<nudiRow1.count, id: \.self) { i in
                    keyButton(label: viewModel.isShifted ? nudiRow1Shift[i] : nudiRow1[i], 
                              code: viewModel.isShifted ? nudiRow1Codes[i].uppercased() : nudiRow1Codes[i])
                }
            }
            HStack(spacing: 6) {
                Spacer(minLength: 5)
                ForEach(0..<nudiRow2.count, id: \.self) { i in
                    keyButton(label: viewModel.isShifted ? nudiRow2Shift[i] : nudiRow2[i], 
                              code: viewModel.isShifted ? nudiRow2Codes[i].uppercased() : nudiRow2Codes[i])
                }
                Spacer(minLength: 5)
            }
            HStack(spacing: 6) {
                specialKey(label: "⇧", width: 40, isPressed: viewModel.isShifted) {
                    onAction(.shift)
                    viewModel.toggleShift()
                }
                ForEach(0..<nudiRow3.count, id: \.self) { i in
                    keyButton(label: viewModel.isShifted ? nudiRow3Shift[i] : nudiRow3[i], 
                              code: viewModel.isShifted ? nudiRow3Codes[i].uppercased() : nudiRow3Codes[i])
                }
                specialKey(label: "⌫", width: 40) {
                    onAction(.backspace)
                }
            }
        }
    }

    func renderNumeric() -> some View {
        VStack(spacing: 10) {
            HStack(spacing: 6) {
                ForEach(numRow1, id: \.self) { key in
                    keyButton(label: key, code: key)
                }
            }
            HStack(spacing: 6) {
                ForEach(numRow2, id: \.self) { key in
                    keyButton(label: key, code: key)
                }
            }
            HStack(spacing: 6) {
                Spacer(minLength: 40)
                ForEach(numRow3, id: \.self) { key in
                    keyButton(label: key, code: key)
                }
                specialKey(label: "⌫", width: 40) {
                    onAction(.backspace)
                }
            }
        }
    }

    func renderBottomRow() -> some View {
        HStack(spacing: 6) {
            specialKey(label: viewModel.currentMode == .numeric ? "ABC" : "123", width: 60) {
                onAction(viewModel.currentMode == .numeric ? .alphaChange : .modeChange)
                viewModel.toggleMode()
            }
            specialKey(label: "🌐", width: 40) {
                onAction(.globe)
            }
            specialKey(label: "🎤", width: 40) {
                onAction(.dictation)
            }
            KeyButton(label: "space") {
                onAction(.space)
            }
            specialKey(label: "⏎", width: 60) {
                onAction(.enter)
            }
        }
    }

    // Helper to generate key
    func keyButton(label: String, code: String) -> some View {
        // For Baraha, we just use the code directly but display it uppercase if shifted
        let displayLabel = (viewModel.currentMode == .alpha && viewModel.currentLayout == .baraha && viewModel.isShifted) ? label.uppercased() : label

        return KeyButton(label: displayLabel) {
            var finalCode = code
            // Shift handling for Baraha is done by uppercasing the key code
            if viewModel.currentLayout == .baraha && viewModel.isShifted {
                finalCode = finalCode.uppercased()
            }
            onAction(.character(finalCode))
        }
    }

    func specialKey(label: String, width: CGFloat? = nil, isPressed: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(Theme.keySpecialText)
                .frame(maxWidth: width == nil ? .infinity : width, maxHeight: 45)
                .background(
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Theme.keySpecialBackground)
                        .shadow(color: Theme.keyShadow, radius: 1, x: 0, y: 1)
                )
                .scaleEffect(isPressed ? 0.95 : 1.0)
        }
    }
}

