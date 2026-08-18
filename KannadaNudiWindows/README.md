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
