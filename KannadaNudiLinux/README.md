# Kannada Nudi Editor - Linux / Ubuntu Desktop App

This directory contains the desktop packaging for **Kannada Nudi Editor** targeting Linux (Ubuntu, Debian, Snap Store, and generic Linux x64).

The desktop app bundles the .NET 8 Blazor WebAssembly editor with Electron, running a lightweight local HTTP server completely offline with **zero external internet dependencies**.

---

## 📦 Build Artifacts (in `dist/`)

After building, the following artifacts are generated in `dist/`:

| Artifact | Description | Target |
| :--- | :--- | :--- |
| `kannadanudi_1.0.1_amd64.deb` | Ubuntu / Debian native installer package | Ubuntu / Debian / Mint |
| `kannadanudi_1.0.1_amd64.snap` | Canonical Snap package for Snap Store | Ubuntu / All Snap-enabled distros |
| `kannadanudilinux-1.0.1.tar.gz` | Portable compressed archive | All Linux distributions |
| `kannadanudilinux-1.0.1.zip` | Portable zip archive | All Linux distributions |
| `linux-unpacked/` | Unpacked executable folder (`./kannadanudilinux`) | Linux x64 direct run |

---

## 🚀 Installation & Running on Ubuntu / Linux

### Option 1: Install Snap Package (`.snap`)
```bash
sudo snap install dist/kannadanudi_1.0.1_amd64.snap --dangerous
```

### Option 2: Install Debian / Ubuntu Package (`.deb`)
```bash
sudo dpkg -i dist/kannadanudi_1.0.1_amd64.deb
# If there are missing dependencies:
sudo apt-get install -f
```

### Option 3: Portable `tar.gz`
```bash
tar -xzf dist/kannadanudilinux-1.0.1.tar.gz
cd kannadanudilinux-1.0.1
chmod +x kannadanudilinux
./kannadanudilinux
```

---

## 🛍️ Publishing to the Canonical Snap Store

### 1. Prerequisites & Snapcraft Setup
Install Snapcraft:
```bash
sudo snap install snapcraft --classic
```

### 2. Login & Register Snap Name
```bash
# Login with your Ubuntu One account
snapcraft login

# Register the package name on the Snap Store
snapcraft register kannadanudi
```

### 3. Build & Upload Snap Locally
```bash
# Build the snap package using snapcraft
snapcraft

# Upload and release to the stable channel
snapcraft upload kannadanudi_1.0.1_amd64.snap --release=stable
```

### 4. Automated Publishing via GitHub Actions
To enable automated publishing on every new tag release:
1. Export store credentials:
   ```bash
   snapcraft export-login --snaps=kannadanudi --channels=stable snap-login.txt
   ```
2. In your GitHub repository, go to **Settings > Secrets and variables > Actions**.
3. Create a new secret named `SNAPCRAFT_STORE_CREDENTIALS` and paste the contents of `snap-login.txt`.
4. Delete `snap-login.txt` locally for security.

Whenever you push a tag (e.g. `git tag v1.0.0 && git push origin v1.0.0`), GitHub Actions will automatically build and publish the snap to the Snap Store.

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

3. **Build Full Linux Distribution:**
   ```bash
   npm run dist
   ```

4. **Build Specific Formats:**
   - Unpacked binary folder: `npm run dist:unpacked`
   - Debian package (`.deb`): `npm run dist:deb`
   - Snap package: `npm run dist:snap` (or `snapcraft`)
   - Tarball (`.tar.gz`): `npm run dist:tar`
