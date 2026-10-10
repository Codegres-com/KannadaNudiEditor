# Kannada Nudi Editor - Windows Desktop Application

This directory contains the desktop packaging for **Kannada Nudi Editor** targeting Microsoft Windows (Windows 10, Windows 11, Windows Server 64-bit).

The desktop app packages the .NET 8 Blazor WebAssembly editor with Electron, running a lightweight local HTTP server completely offline with **zero external internet dependencies**.

---

## 📦 Build Artifacts (in `dist/`)

After building, the following URL-safe artifacts are generated in `dist/`:

| Artifact | Description | Target |
| :--- | :--- | :--- |
| `KannadaNudi-Setup-1.0.0.exe` | Windows NSIS Setup installer (Desktop shortcut, Start Menu) | Windows 64-bit Installer |
| `KannadaNudi-Portable-1.0.0.exe` | Portable single-file standalone executable | Windows 64-bit Portable |
| `KannadaNudi-1.0.0-win-x64.zip` | Portable zipped folder | Windows 64-bit Portable |
| `win-unpacked/KannadaNudi.exe` | Direct standalone unpacked executable folder | Windows 64-bit Direct Run |

---

## 🚀 Running on Windows

### Option 1: Installer (`KannadaNudi-Setup-1.0.0.exe`)
Double-click `KannadaNudi-Setup-1.0.0.exe` to install Kannada Nudi Editor with desktop and start menu shortcuts.

### Option 2: Portable Executable (`KannadaNudi-Portable-1.0.0.exe`)
Double-click `KannadaNudi-Portable-1.0.0.exe` to run immediately with no installation required.

### Option 3: Unpacked folder (`win-unpacked/KannadaNudi.exe`)
Run `dist\win-unpacked\KannadaNudi.exe` directly.

---

## ⌨️ Global Mode (Direct Type) — Kannada in any application

Press **F9** anywhere (browser, Word, Excel, Notepad, ...) to switch the whole system between Kannada and English, using the same Nudi / Baraha key bindings as the editor. It can also be toggled with the **Global mode** button next to Save / Open, or from the tray icon.

- A small sticky toast in the bottom-right corner always shows **Global Mode: English** or **Global Mode: Kannada** while the app is running (it pulses when F9 switches it, and is click-through so it never blocks other windows).
- The layout (Nudi or Baraha) is shared with the editor's layout selector and remembered.
- Closing the editor window keeps the app in the tray so F9 keeps working; use **Quit** in the tray menu to exit. Enable **Start with Windows** in the tray menu to have it always available.
- Implemented by `GlobalKeyboard/` (a small .NET 8 helper with a low-level keyboard hook), which `npm run package` publishes into `app/global-keyboard/`.
- Windows does not let a normal app type into windows running as Administrator, so Global mode has no effect there.

---

## 🛠️ Building from Source

### Prerequisites
- [.NET 8.0 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- [Node.js](https://nodejs.org/) (v18+) and `npm`

### Commands

1. **Install dependencies:**
   ```bash
   npm install
   ```

2. **Package Web Assets only:**
   ```bash
   npm run package
   ```
   *Publishes `KannadaNudiWeb` via `dotnet publish` in Release mode and syncs `publish/wwwroot` to `KannadaNudiWindows/app/wwwroot`.*

3. **Build Full Windows Distribution (All formats):**
   ```bash
   npm run dist
   ```

4. **Build Specific Formats:**
   - Unpacked binary folder: `npm run dist:unpacked`
   - NSIS Setup installer: `npm run dist:nsis`
   - Portable EXE: `npm run dist:portable`
