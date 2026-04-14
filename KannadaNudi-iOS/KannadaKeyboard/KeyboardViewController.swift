import UIKit
import SwiftUI

class KeyboardViewController: UIInputViewController {

    private var transliterationEngine = TransliterationEngine()
    private var dictionaryLoader = DictionaryLoader()
    private var viewModel = KeyboardViewModel()

    override func viewDidLoad() {
        super.viewDidLoad()

        // Initialize Dictionary
        dictionaryLoader.loadDictionary()

        // Create the SwiftUI view
        let keyboardView = KeyboardView(
            viewModel: viewModel,
            onAction: { [weak self] action in
                self?.handleAction(action)
            },
            onCandidateSelected: { [weak self] candidate in
                self?.handleCandidateSelection(candidate)
            }
        )

        // Host the SwiftUI view
        let hostingController = UIHostingController(rootView: keyboardView)
        hostingController.view.translatesAutoresizingMaskIntoConstraints = false
        hostingController.view.backgroundColor = .clear

        addChild(hostingController)
        view.addSubview(hostingController.view)

        NSLayoutConstraint.activate([
            hostingController.view.leftAnchor.constraint(equalTo: view.leftAnchor),
            hostingController.view.rightAnchor.constraint(equalTo: view.rightAnchor),
            hostingController.view.topAnchor.constraint(equalTo: view.topAnchor),
            hostingController.view.bottomAnchor.constraint(equalTo: view.bottomAnchor)
        ])

        hostingController.didMove(toParent: self)
    }

    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
    }

    // MARK: - Action Handling

    private func handleAction(_ action: KeyboardAction) {
        let proxy = textDocumentProxy

        switch action {
        case .character(let char):
            // Get context for Matra handling
            let lastChar = proxy.documentContextBeforeInput?.last
            
            // Pass to engine
            let result = transliterationEngine.getTransliteration(key: char, lastCommittedChar: lastChar)

            // Handle backspace count (removing previous chars for composition)
            if result.backspaceCount > 0 {
                for _ in 0..<result.backspaceCount {
                    proxy.deleteBackward()
                }
            }

            // Insert new text
            proxy.insertText(result.text)

            // Update suggestions
            updateSuggestions()

        case .backspace:
            transliterationEngine.clearBuffer()
            proxy.deleteBackward()
            updateSuggestions()

        case .space:
            transliterationEngine.clearBuffer()
            proxy.insertText(" ")
            updateSuggestions()

        case .enter:
            transliterationEngine.clearBuffer()
            proxy.insertText("\n")

        case .shift:
            // Handled in ViewModel usually, but if needed here
            break

        case .globe:
            advanceToNextInputMode()

        case .dictation:
            // Switch to next input mode to allow user to access system dictation
            advanceToNextInputMode()

        case .modeChange, .alphaChange:
            // Handled via ViewModel in KeyboardView, but we can clear buffer here
            transliterationEngine.clearBuffer()

        case .layoutChange:
            transliterationEngine.setLayout(viewModel.currentLayout == .baraha ? .nudi : .baraha)

        case .dismiss:
            dismissKeyboard()
        }
    }

    private func handleCandidateSelection(_ candidate: String) {
        let proxy = textDocumentProxy

        // Remove current word being typed
        // Strategy: We need to know how much to delete.
        // Android does: inputConnection.deleteSurroundingText(lastWord.length, 0)

        // iOS doesn't give easy access to "textBeforeCursor" length easily without requesting full context.
        // However, we can approximate or use the buffer if we were tracking it perfectly.
        // But since we use TransliterationEngine which clears buffer on space/backspace,
        // we might not have the exact "word" in buffer if the user moved cursor.

        // Simple approach: Request context
        if let context = proxy.documentContextBeforeInput {
            // Find last word boundary
            // This is a naive implementation
            if let lastWord = context.components(separatedBy: CharacterSet.whitespacesAndNewlines).last {
                for _ in 0..<lastWord.count {
                    proxy.deleteBackward()
                }
                proxy.insertText(candidate + " ")
            }
        } else {
            // Fallback if no context
            proxy.insertText(candidate + " ")
        }

        transliterationEngine.clearBuffer()
        viewModel.candidates = []
    }

    private func updateSuggestions() {
        // Debounce or async
        DispatchQueue.main.async { [weak self] in
            guard let self = self else { return }
            let proxy = self.textDocumentProxy

            if let context = proxy.documentContextBeforeInput {
                if let lastWord = context.components(separatedBy: CharacterSet.whitespacesAndNewlines).last, !lastWord.isEmpty {
                    // We shouldn't predict on just one char maybe?
                    let suggestions = self.dictionaryLoader.getSuggestions(lastWord)
                    self.viewModel.candidates = suggestions
                } else {
                    self.viewModel.candidates = []
                }
            } else {
                self.viewModel.candidates = []
            }
        }
    }
}
