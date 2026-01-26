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
    case digitChange // Toggle ASCII/Kannada digits
    case layoutChange // Nudi/Baraha
    case dismiss
}

class KeyboardViewModel: ObservableObject {
    @Published var isShifted = false
    @Published var currentLayout: KeyboardLayout = .baraha
    @Published var candidates: [String] = []
    @Published var isNumericMode = false
    @Published var isKannadaDigits = false

    func toggleShift() {
        isShifted.toggle()
    }

    func toggleLayout() {
        currentLayout = (currentLayout == .baraha) ? .nudi : .baraha
    }

    func toggleNumericMode() {
        isNumericMode.toggle()
    }

    func toggleDigitType() {
        isKannadaDigits.toggle()
    }
}

struct KeyboardView: View {
    @ObservedObject var viewModel: KeyboardViewModel
    var onAction: (KeyboardAction) -> Void
    var onCandidateSelected: (String) -> Void

    let qwertyRow1 = ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"]
    let qwertyRow2 = ["a", "s", "d", "f", "g", "h", "j", "k", "l"]
    let qwertyRow3 = ["z", "x", "c", "v", "b", "n", "m"]

    // Nudi Layout keys
    let nudiRow1 = ["ದ್", "ತ್", "ಎ", "ರ್", "ಟ್", "ಯ್", "ಉ", "ಇ", "ಒ", "ಪ್"]
    let nudiRow1Codes = ["q", "w", "e", "r", "t", "y", "u", "i", "o", "p"]

    let nudiRow2 = ["ಅ", "ಸ್", "ಡ್", "f", "ಗ್", "ಹ್", "ಜ್", "ಕ್", "ಲ್"]
    let nudiRow2Codes = ["a", "s", "d", "f", "g", "h", "j", "k", "l"]

    let nudiRow3 = ["z", "x", "ಚ್", "ವ್", "ಬ್", "ಣ್", "ಮ್"]
    let nudiRow3Codes = ["z", "x", "c", "v", "b", "n", "m"]

    // Numeric Layout keys
    let asciiDigits = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"]
    let kannadaDigits = ["೧", "೨", "೩", "೪", "೫", "೬", "೭", "೮", "೯", "೦"]
    let symbolsRow1 = ["-", "/", ":", ";", "(", ")", "$", "&", "@", "\""]
    let symbolsRow2 = [".", ",", "?", "!", "'"]

    var body: some View {
        VStack(spacing: 0) {
            // Candidates
            if !viewModel.candidates.isEmpty {
                CandidatesRow(candidates: viewModel.candidates, onSelect: onCandidateSelected)
            }

            // Toolbar (Logo + Switch)
            HStack {
                Text(getHeaderText())
                    .font(.caption)
                    .foregroundColor(Theme.keySpecialBackground)
                    .bold()
                Spacer()
                if !viewModel.isNumericMode {
                    Button(action: {
                        onAction(.layoutChange)
                        viewModel.toggleLayout()
                    }) {
                        Text("Switch Layout")
                            .font(.caption)
                            .bold()
                            .padding(6)
                            .foregroundColor(Theme.keySpecialText)
                            .background(Theme.keySpecialBackground)
                            .cornerRadius(4)
                    }
                }
            }
            .padding(4)
            .background(Color.white.opacity(0.8))

            // Keys
            VStack(spacing: 8) {
                if viewModel.isNumericMode {
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

    func getHeaderText() -> String {
        if viewModel.isNumericMode {
            return "Numeric"
        }
        return viewModel.currentLayout == .baraha ? "Nudi (Phonetic)" : "Nudi (Direct)"
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
                    keyButton(label: nudiRow1[i], code: nudiRow1Codes[i])
                }
            }
            HStack(spacing: 6) {
                Spacer(minLength: 10)
                ForEach(0..<nudiRow2.count, id: \.self) { i in
                    keyButton(label: nudiRow2[i], code: nudiRow2Codes[i])
                }
                Spacer(minLength: 10)
            }
            HStack(spacing: 6) {
                specialKey(label: "⇧", width: 40, isPressed: viewModel.isShifted) {
                    onAction(.shift)
                    viewModel.toggleShift()
                }
                ForEach(0..<nudiRow3.count, id: \.self) { i in
                    keyButton(label: nudiRow3[i], code: nudiRow3Codes[i])
                }
                specialKey(label: "⌫", width: 40) {
                    onAction(.backspace)
                }
            }
        }
    }

    func renderNumeric() -> some View {
        VStack(spacing: 10) {
            // Row 1: Digits
            HStack(spacing: 6) {
                let digits = viewModel.isKannadaDigits ? kannadaDigits : asciiDigits
                ForEach(digits, id: \.self) { key in
                    keyButton(label: key, code: key)
                }
            }
            // Row 2: Symbols
            HStack(spacing: 6) {
                ForEach(symbolsRow1, id: \.self) { key in
                    keyButton(label: key, code: key)
                }
            }
            // Row 3: Toggle + Symbols + Backspace
            HStack(spacing: 6) {
                // Toggle Digits Key
                specialKey(label: viewModel.isKannadaDigits ? "123" : "೧೨೩", width: 60) {
                    onAction(.digitChange)
                    viewModel.toggleDigitType()
                }

                Spacer(minLength: 10)

                ForEach(symbolsRow2, id: \.self) { key in
                    keyButton(label: key, code: key)
                }

                Spacer(minLength: 10)

                specialKey(label: "⌫", width: 40) {
                    onAction(.backspace)
                }
            }
        }
    }

    func renderBottomRow() -> some View {
        HStack(spacing: 6) {
            specialKey(label: viewModel.isNumericMode ? "ಅಆಇ" : "123", width: 40) {
                onAction(.modeChange)
                viewModel.toggleNumericMode()
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
        let displayLabel = viewModel.isShifted ? label.uppercased() : label

        return KeyButton(label: displayLabel) {
            var finalCode = code
            if viewModel.isShifted {
                finalCode = finalCode.uppercased()
            }
            onAction(.character(finalCode))
        }
    }

    func specialKey(label: String, width: CGFloat? = nil, isPressed: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(Theme.keySpecialText)
                .frame(maxWidth: width == nil ? .infinity : width, maxHeight: 50)
                .background(
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Theme.keySpecialBackground)
                        .shadow(color: Theme.keyShadow, radius: 1, x: 0, y: 1)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 6)
                        .stroke(Color.white.opacity(0.2), lineWidth: 1)
                )
                .scaleEffect(isPressed ? 0.95 : 1.0)
                .opacity(isPressed ? 0.8 : 1.0)
        }
    }
}
