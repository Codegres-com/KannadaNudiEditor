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
| `KannadaNudi-1.0.1-Mac-arm64.pkg` | macOS Installer package | Apple Silicon (M1/M2/M3/M4) |
| `KannadaNudi-1.0.1-Mac-x64.pkg` | macOS Installer package | Intel Mac (x86_64) |
| `KannadaNudi-1.0.1-Mac-arm64.zip` | Portable ZIP archive | Apple Silicon (M1/M2/M3/M4) |
| `KannadaNudi-1.0.1-Mac-x64.zip` | Portable ZIP archive | Intel Mac (x86_64) |
| `mac-arm64/` / `mac-x64/` | Unpacked `.app` bundle (`KannadaNudi.app`) | macOS direct run |

> The unpacked `darwin-<arch>/` folders are **not** produced by default, since the
> ZIP already contains the same bundle and each copy costs ~350 MB. Set
> `KANNADANUDI_KEEP_UNPACKED=1` when building to emit them as well.
>
> Mac App Store builds (`npm run dist:mas`) produce only
> `KannadaNudi-1.0.1-MAS-<arch>.pkg` -- see **Code Signing & Distribution** below.

---

## 🚀 Installation & Running on macOS

### 1. Install via DMG (Recommended)
1. Double-click the downloaded `KannadaNudi-1.0.1-Mac-arm64.dmg` (for Apple Silicon) or `KannadaNudi-1.0.1-Mac-x64.dmg` (for Intel).
2. Drag and drop **KannadaNudi** into your **Applications** folder.
3. Eject the DMG disk image.
4. Open **Kannada Nudi** from Spotlight (`Cmd + Space`), Launchpad, or the Applications folder.

### 2. Install via PKG
1. Double-click `KannadaNudi-1.0.1-Mac-arm64.pkg` (Apple Silicon) or `KannadaNudi-1.0.1-Mac-x64.pkg` (Intel).
2. Follow the installer prompts. The app is installed straight into `/Applications`.
3. Open **Kannada Nudi** from Spotlight (`Cmd + Space`), Launchpad, or the Applications folder.

The package is unsigned, so macOS may refuse to open it on the first attempt --
**right-click** the `.pkg` and choose **Open**, then confirm. See the Gatekeeper
section below.

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

5. **Build All Formats (DMG + PKG + ZIP for both architectures):**
   ```bash
   npm run dist
   ```

   The packaging step needs `ditto`, `hdiutil`, `codesign`, `pkgbuild` and
   `productbuild`, so it must run on macOS. Allow roughly 1.5 GB of free disk space.

---

## 🔏 Code Signing & Distribution

By default the build is **ad-hoc signed**: it runs anywhere but shows the
Gatekeeper warning above. Real signing is opt-in through environment variables,
so the script still works on a machine with no certificates.

| Variable | Purpose |
| :--- | :--- |
| `KANNADANUDI_SIGN_IDENTITY` | Certificate that signs the `.app` and `.dmg` |
| `KANNADANUDI_INSTALLER_IDENTITY` | Certificate that signs the `.pkg` |
| `KANNADANUDI_PROVISIONING_PROFILE` | Path to a `.provisionprofile` (App Store only) |
| `KANNADANUDI_NOTARY_PROFILE` | `notarytool` keychain profile (Developer ID only) |
| `KANNADANUDI_TEAM_ID` | Apple Developer team (default `T4YF8RZCVA`) |
| `KANNADANUDI_BUNDLE_ID` | Bundle identifier (default `editor.kannada.nudi`) |

The bundle identifier matches the iOS app so both platforms can share one App
Store Connect record via **Universal Purchase**.

### Option A — Developer ID (direct download from your website)

Produces a notarized DMG, PKG and ZIP with no Gatekeeper warning. Requires
**Developer ID Application** and **Developer ID Installer** certificates.

Store the notary credentials once (they live in the keychain, never in the build):

```bash
xcrun notarytool store-credentials KannadaNudiNotary \
  --apple-id you@example.com --team-id T4YF8RZCVA
```

Then build:

```bash
export KANNADANUDI_SIGN_IDENTITY="Developer ID Application: Your Name (T4YF8RZCVA)"
export KANNADANUDI_INSTALLER_IDENTITY="Developer ID Installer: Your Name (T4YF8RZCVA)"
export KANNADANUDI_NOTARY_PROFILE="KannadaNudiNotary"
npm run dist
```

Each DMG and PKG is submitted to Apple's notary service and stapled automatically.

### Option B — Mac App Store (upload with Transporter)

Produces a sandboxed, signed `.pkg` named `KannadaNudi-<version>-MAS-<arch>.pkg`.
The App Store accepts only the PKG, so no DMG or ZIP is built for this target.

Requirements, all of which must exist **before** building:

1. A **macOS** app record in App Store Connect for `editor.kannada.nudi`. If the
   iOS app already exists, add the macOS platform to it rather than creating a
   second record. *Transporter's "No suitable application records were found"
   error means this step is missing.*
2. A **3rd Party Mac Developer Application** certificate.
3. A **3rd Party Mac Developer Installer** certificate.
4. A **macOS** provisioning profile (`.provisionprofile`) for the bundle ID.

```bash
export KANNADANUDI_SIGN_IDENTITY="3rd Party Mac Developer Application: Your Name (T4YF8RZCVA)"
export KANNADANUDI_INSTALLER_IDENTITY="3rd Party Mac Developer Installer: Your Name (T4YF8RZCVA)"
export KANNADANUDI_PROVISIONING_PROFILE="$HOME/profiles/KannadaNudi_Mac.provisionprofile"
npm run dist:mas
```

App Store builds use Electron's **MAS** distribution, which is a different
download from the regular one and is fetched automatically by architecture. The
app is sandboxed with `com.apple.security.network.server` so the bundled
loopback editor server keeps working; entitlements are generated at build time
so the application identifier always matches the team and bundle ID.

Sign in to Transporter with an Apple Account that has access to the app record,
then upload the generated `.pkg`.

---

## 🌟 Offline Architecture & Features

- **100% Offline Capability**: Runs entirely locally via an embedded loopback server with zero external internet dependencies.
- **Embedded .NET 8 Blazor WebAssembly**: Compiles and executes C# Blazor code locally inside Chromium V8 engine.
- **Pre-bundled Kannada & Unicode Fonts**: Includes `SmartNudi1` (Regular, Bold, Light, ExtraBold), `Noto Sans Kannada`, `Nudi 01/02/05/10`, and `Poppins`.
- **Pre-bundled JS/CSS Libraries**: KaTeX math formula rendering, Mammoth/DocShift document processing, Quill rich text editor, Bootstrap 5 UI.
- **Native macOS Experience**: Supports macOS Application Menu, native dark/light modes, keyboard shortcuts (`Cmd+Q`, `Cmd+C`, `Cmd+V`, `Cmd+Z`), and Retina display rendering.
