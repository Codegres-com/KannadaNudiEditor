# Kannada Nudi Editor - Linux / Ubuntu Desktop App

This directory contains the desktop packaging for **Kannada Nudi Editor** targeting Linux (Ubuntu, Debian, and generic Linux x64).

The desktop app bundles the .NET 8 Blazor WebAssembly editor with Electron, running a lightweight local HTTP server completely offline with **zero external internet dependencies**.

---

## 📦 Build Artifacts (in `dist/`)

After building, the following artifacts are generated in `dist/`:

| Artifact | Description | Target |
| :--- | :--- | :--- |
| `kannadanudi_1.0.0_amd64.deb` | Ubuntu / Debian native installer package | Ubuntu / Debian / Mint |
| `kannadanudilinux-1.0.0.tar.gz` | Portable compressed archive | All Linux distributions |
| `kannadanudilinux-1.0.0.zip` | Portable zip archive | All Linux distributions |
| `linux-unpacked/` | Unpacked executable folder (`./kannadanudilinux`) | Linux x64 direct run |

---

## 🚀 Installation & Running on Ubuntu / Linux

### Option 1: Install Debian / Ubuntu Package (`.deb`)
```bash
sudo dpkg -i dist/kannadanudi_1.0.0_amd64.deb
# If there are missing dependencies:
sudo apt-get install -f
```
Once installed, you can launch **Kannada Nudi** from your application menu or run `kannadanudi` in the terminal.

### Option 2: Portable `tar.gz`
```bash
tar -xzf dist/kannadanudilinux-1.0.0.tar.gz
cd kannadanudilinux-1.0.0
chmod +x kannadanudilinux
./kannadanudilinux
```

### Option 3: Run directly from `linux-unpacked`
```bash
cd dist/linux-unpacked
chmod +x kannadanudilinux
./kannadanudilinux
```

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
   *Publishes `KannadaNudiWeb` via `dotnet publish` in Release mode and syncs `publish/wwwroot` to `KannadaNudiLinux/app/wwwroot`.*

3. **Build Full Linux Distribution (All formats):**
   ```bash
   npm run dist
   ```

4. **Build Specific Formats:**
   - Unpacked binary folder: `npm run dist:unpacked`
   - Debian package (`.deb`): `npm run dist:deb`
   - Tarball (`.tar.gz`): `npm run dist:tar`
