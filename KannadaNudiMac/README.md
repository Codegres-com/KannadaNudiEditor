# Kannada Nudi Editor - macOS Desktop App (DMG)

This directory contains the desktop packaging for **Kannada Nudi Editor** targeting macOS (Apple Silicon M1/M2/M3/M4, Intel x64, and Universal macOS binaries).

The desktop app bundles the .NET 8 Blazor WebAssembly editor with Electron, running a lightweight local HTTP server completely offline with **zero external internet dependencies**.

---

## 📦 Build Artifacts (in `dist/`)

After running the build commands, the following artifacts are generated in `dist/`:

| Artifact | Description | Target Architecture |
| :--- | :--- | :--- |
| `KannadaNudi-1.0.1-Mac-arm64.dmg` | Apple Disk Image installer | Apple Silicon (M1/M2/M3/M4) |
| `KannadaNudi-1.0.1-Mac-x64.dmg` | Apple Disk Image installer | Intel Mac (x86_64) |
| `KannadaNudi-1.0.1-Mac-arm64.zip` | Portable ZIP archive | Apple Silicon (M1/M2/M3/M4) |
| `KannadaNudi-1.0.1-Mac-x64.zip` | Portable ZIP archive | Intel Mac (x86_64) |
| `mac-arm64/` / `mac/` | Unpacked `.app` bundle (`KannadaNudi.app`) | macOS direct run |

---

## 🚀 Installation & Running on macOS

### 1. Install via DMG (Recommended)
1. Double-click the downloaded `KannadaNudi-1.0.1-Mac-arm64.dmg` (for Apple Silicon) or `KannadaNudi-1.0.1-Mac-x64.dmg` (for Intel).
2. Drag and drop **KannadaNudi** into your **Applications** folder.
3. Eject the DMG disk image.
4. Open **Kannada Nudi** from Spotlight (`Cmd + Space`), Launchpad, or the Applications folder.

---

## 🔒 macOS Gatekeeper & First-Time Launch

If macOS displays a warning that the app cannot be opened because it is from an unidentified developer:

### Option A: Right-Click Open
1. In Finder, open the `/Applications` folder.
2. **Right-click** (or Control-click) on `KannadaNudi.app` and choose **Open**.
3. In the confirmation dialog, click **Open**.

### Option B: System Settings
1. Open **System Settings** > **Privacy & Security**.
2. Scroll down to the **Security** section.
3. Click **Open Anyway** next to the `KannadaNudi` notice.

### Option C: Terminal Command (Quickest)
```bash
sudo xattr -cr /Applications/KannadaNudi.app
```

---

## 🛠️ Building from Source

### Prerequisites
- [.NET 8.0 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- [Node.js](https://nodejs.org/) (v18+) and `npm`

### Build Steps

1. **Install dependencies:**
   ```bash
   cd KannadaNudiMac
   npm install
   ```

2. **Package Web Assets:**
   ```bash
   npm run package
   ```
   *Compiles `KannadaNudiWeb` via `dotnet publish -c Release` and syncs compiled assets to `KannadaNudiMac/app/wwwroot`.*

3. **Build DMG for Apple Silicon (M1/M2/M3/M4):**
   ```bash
   npm run dist:arm64
   ```

4. **Build DMG for Intel Mac:**
   ```bash
   npm run dist:x64
   ```

5. **Build Universal Binary DMG:**
   ```bash
   npm run dist:universal
   ```

6. **Build All Formats (DMG + ZIP for both architectures):**
   ```bash
   npm run dist
   ```

---

## 🌟 Offline Architecture & Features

- **100% Offline Capability**: Runs entirely locally via an embedded loopback server with zero external internet dependencies.
- **Embedded .NET 8 Blazor WebAssembly**: Compiles and executes C# Blazor code locally inside Chromium V8 engine.
- **Pre-bundled Kannada & Unicode Fonts**: Includes `SmartNudi1` (Regular, Bold, Light, ExtraBold), `Noto Sans Kannada`, `Nudi 01/02/05/10`, and `Poppins`.
- **Pre-bundled JS/CSS Libraries**: KaTeX math formula rendering, Mammoth/DocShift document processing, Quill rich text editor, Bootstrap 5 UI.
- **Native macOS Experience**: Supports macOS Application Menu, native dark/light modes, keyboard shortcuts (`Cmd+Q`, `Cmd+C`, `Cmd+V`, `Cmd+Z`), and Retina display rendering.
