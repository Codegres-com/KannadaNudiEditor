import os
import sys
import shutil
import glob
import json
import subprocess

MAC_DIR = os.path.dirname(os.path.abspath(__file__))
APP_DIR = os.path.join(MAC_DIR, 'app', 'wwwroot')
DIST_DIR = os.path.join(MAC_DIR, 'dist')

# The .app is assembled outside the project tree on purpose: when the repo lives in
# an iCloud Drive / file-provider synced folder (~/Documents, ~/Desktop), the provider
# stamps com.apple.FinderInfo on bundle directories and re-applies it after removal.
# codesign refuses to sign through that, and an unsigned bundle will not launch on
# Apple Silicon at all. Override with KANNADANUDI_BUILD_ROOT if needed.
BUILD_ROOT = os.environ.get(
    'KANNADANUDI_BUILD_ROOT',
    os.path.expanduser('~/Library/Caches/kannadanudi-mac-build'),
)
# Keep the unpacked dist/mac-<arch>/ bundle as well. Off by default: it is a byproduct
# (the ZIP holds the same bundle) and costs ~350 MB per architecture.
KEEP_UNPACKED = os.environ.get('KANNADANUDI_KEEP_UNPACKED') == '1'
ICON_ICNS = os.path.join(MAC_DIR, 'icon.icns')
ICON_PNG = os.path.join(MAC_DIR, 'icon.png')
MAIN_JS = os.path.join(MAC_DIR, 'main.js')
PACKAGE_JSON = os.path.join(MAC_DIR, 'package.json')

VERSION = "1.0.1"


def _default_build_number():
    """CFBundleVersion: must strictly increase with every App Store upload.

    Kept separate from VERSION (CFBundleShortVersionString), which is the
    marketing version users see and may stay the same across resubmissions.
    Read from package.json so the value is tracked in git rather than guessed.
    """
    try:
        with open(os.path.join(os.path.dirname(os.path.abspath(__file__)),
                               'package.json')) as f:
            return str(json.load(f).get('build', {}).get('buildVersion') or VERSION)
    except (OSError, ValueError):
        return VERSION


BUILD_NUMBER = os.environ.get('KANNADANUDI_BUILD_NUMBER') or _default_build_number()
APP_NAME = "KannadaNudi"
DISPLAY_NAME = "Kannada Nudi"

# Matches the iOS app so both platforms sit under one App Store Connect record
# (Universal Purchase). Must correspond to an existing macOS app record.
BUNDLE_ID = os.environ.get('KANNADANUDI_BUNDLE_ID', 'editor.kannada.nudi')
TEAM_ID = os.environ.get('KANNADANUDI_TEAM_ID', 'T4YF8RZCVA')

# Distribution target: "darwin" = Developer ID / direct download,
# "mas" = Mac App Store (sandboxed, uploaded with Transporter).
TARGET = 'darwin'

# Signing is opt-in. With nothing set the build stays ad-hoc signed and the
# installers unsigned, exactly as before -- so this script still runs on a
# machine with no certificates.
#   KANNADANUDI_SIGN_IDENTITY       "Developer ID Application: ..."      (darwin)
#                                   "3rd Party Mac Developer Application: ..." (mas)
#   KANNADANUDI_INSTALLER_IDENTITY  "Developer ID Installer: ..."        (darwin)
#                                   "3rd Party Mac Developer Installer: ..."   (mas)
#   KANNADANUDI_PROVISIONING_PROFILE  path to a .provisionprofile        (mas)
#   KANNADANUDI_NOTARY_PROFILE      notarytool keychain profile name     (darwin)
SIGN_IDENTITY = os.environ.get('KANNADANUDI_SIGN_IDENTITY')
INSTALLER_IDENTITY = os.environ.get('KANNADANUDI_INSTALLER_IDENTITY')
PROVISIONING_PROFILE = os.environ.get('KANNADANUDI_PROVISIONING_PROFILE')
NOTARY_PROFILE = os.environ.get('KANNADANUDI_NOTARY_PROFILE')

# Leave unset to inherit Electron's own LSMinimumSystemVersion.
MIN_OS_OVERRIDE = os.environ.get('KANNADANUDI_MIN_OS')


def run(cmd, **kwargs):
    subprocess.run(cmd, check=True, **kwargs)


def find_electron_zip(arch):
    """Locate the cached Electron macOS zip for the given architecture.

    Electron's downloader caches archives under ~/Library/Caches/electron on macOS.
    A copy dropped next to this script is also accepted.
    """
    cache_dir = os.path.expanduser('~/Library/Caches/electron')
    matches = glob.glob(os.path.join(cache_dir, '**', f'*{TARGET}-{arch}.zip'), recursive=True)
    if matches:
        return matches[0]
    local = os.path.join(MAC_DIR, f'electron-{TARGET}-{arch}.zip')
    if os.path.exists(local):
        return local
    return None


def create_dmg_from_app(app_path, dmg_output_path, volume_name=DISPLAY_NAME):
    """Build a real, mountable UDZO disk image containing the .app and an
    /Applications drop target."""
    print(f"  -> Building Apple DMG installer: {os.path.basename(dmg_output_path)}...")

    stage_dir = dmg_output_path + ".stage"
    if os.path.exists(stage_dir):
        shutil.rmtree(stage_dir)
    os.makedirs(stage_dir)

    staged_app = os.path.join(stage_dir, os.path.basename(app_path))
    try:
        # APFS clone: instant and consumes almost no extra disk for the staging copy
        run(['cp', '-Rc', app_path, staged_app])
    except subprocess.CalledProcessError:
        # ditto preserves symlinks, permissions and code signatures inside the bundle
        run(['ditto', app_path, staged_app])
    os.symlink('/Applications', os.path.join(stage_dir, 'Applications'))

    if os.path.exists(dmg_output_path):
        os.remove(dmg_output_path)

    run([
        'hdiutil', 'create',
        '-volname', volume_name,
        '-srcfolder', stage_dir,
        # APFS, not HFS+: HFS+ keeps Finder info for every folder, which lands on the
        # nested helper .app bundles as a com.apple.FinderInfo xattr and makes
        # `codesign --verify --deep --strict` reject the mounted app.
        '-fs', 'APFS',
        '-format', 'UDZO',
        '-imagekey', 'zlib-level=9',
        '-ov',
        '-quiet',
        dmg_output_path,
    ])

    shutil.rmtree(stage_dir)

    if SIGN_IDENTITY and TARGET != 'mas':
        # A signed disk image lets Gatekeeper validate the container itself.
        try:
            run(['codesign', '--force', '--sign', SIGN_IDENTITY,
                 '--timestamp', dmg_output_path])
        except subprocess.CalledProcessError as err:
            print(f"  WARNING: signing the disk image failed ({err}).")

def read_min_os(app_path):
    """Minimum macOS version the bundle actually declares."""
    info_plist = os.path.join(app_path, 'Contents', 'Info.plist')
    try:
        out = subprocess.run(
            ['/usr/libexec/PlistBuddy', '-c', 'Print :LSMinimumSystemVersion', info_plist],
            capture_output=True, text=True, check=True)
        value = out.stdout.strip()
        if value:
            return value
    except subprocess.CalledProcessError:
        pass
    return '10.13.0'


def create_pkg_from_app(app_path, pkg_output_path):
    """Build a macOS Installer package that drops the .app into /Applications."""
    print(f"  -> Building macOS PKG installer: {os.path.basename(pkg_output_path)}...")

    stage_dir = pkg_output_path + ".stage"
    if os.path.exists(stage_dir):
        shutil.rmtree(stage_dir)
    os.makedirs(stage_dir)

    staged_app = os.path.join(stage_dir, os.path.basename(app_path))
    try:
        # APFS clone: instant, and costs almost no extra disk
        run(['cp', '-Rc', app_path, staged_app])
    except subprocess.CalledProcessError:
        run(['ditto', app_path, staged_app])

    component_plist = pkg_output_path + ".component.plist"
    component_pkg = pkg_output_path + ".component.pkg"

    try:
        # Pin the install location: without this the installer may "relocate" the
        # payload over an older copy of the app found elsewhere on the disk.
        run(['pkgbuild', '--analyze', '--root', stage_dir, component_plist],
            stdout=subprocess.DEVNULL)
        subprocess.run(
            ['plutil', '-replace', '0.BundleIsRelocatable', '-bool', 'false', component_plist],
            check=False)

        run([
            'pkgbuild',
            '--root', stage_dir,
            '--component-plist', component_plist,
            '--install-location', '/Applications',
            '--identifier', BUNDLE_ID,
            '--version', VERSION,
            component_pkg,
        ], stdout=subprocess.DEVNULL)

        if os.path.exists(pkg_output_path):
            os.remove(pkg_output_path)

        # The product archive must declare the same minimum OS as the app's
        # LSMinimumSystemVersion, or App Store validation fails with:
        #   "The lowest minimum system version in the product definition property
        #    list, none, must equal the LSMinimumSystemVersion value"
        # This has to go through --product with a product definition plist: an
        # <os-version> element written straight into the distribution XML is
        # ignored, because the installer only reads it from inside
        # <volume-check><allowed-os-versions>, which productbuild generates here.
        min_os = read_min_os(app_path)
        product_plist = pkg_output_path + ".product.plist"
        with open(product_plist, 'w') as f:
            f.write(
                '<?xml version="1.0" encoding="UTF-8"?>\n'
                '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" '
                '"http://www.apple.com/DTDs/PropertyList-1.0.dtd">\n'
                '<plist version="1.0"><dict>\n'
                '  <key>os</key>\n'
                f'  <array><string>{min_os}</string></array>\n'
                '</dict></plist>\n')
        print(f"     product definition minimum OS: {min_os}")

        productbuild_cmd = [
            'productbuild',
            '--package', component_pkg,
            '--product', product_plist,
            '--identifier', f'{BUNDLE_ID}.installer',
            '--version', VERSION,
        ]
        if INSTALLER_IDENTITY:
            # Transporter rejects an unsigned .pkg, and a Developer ID installer
            # must carry an installer signature to pass Gatekeeper.
            productbuild_cmd += ['--sign', INSTALLER_IDENTITY]
        else:
            print("     (no installer identity set -- producing an UNSIGNED .pkg)")
        run(productbuild_cmd + [pkg_output_path], stdout=subprocess.DEVNULL)

        # Verify the requirement actually landed where the installer reads it.
        check = subprocess.run(['pkgutil', '--expand', pkg_output_path,
                                pkg_output_path + '.check'],
                               capture_output=True, text=True)
        if check.returncode == 0:
            dist_path = os.path.join(pkg_output_path + '.check', 'Distribution')
            emitted = open(dist_path).read() if os.path.exists(dist_path) else ''
            shutil.rmtree(pkg_output_path + '.check', ignore_errors=True)
            if 'allowed-os-versions' not in emitted:
                raise RuntimeError(
                    'product archive is missing <allowed-os-versions>; '
                    'App Store validation would reject it')
        if os.path.exists(product_plist):
            os.remove(product_plist)
    finally:
        shutil.rmtree(stage_dir, ignore_errors=True)
        for tmp in (component_plist, component_pkg):
            if os.path.exists(tmp):
                os.remove(tmp)


def write_mas_entitlements(dest_dir):
    """Write the sandbox entitlements for a Mac App Store build.

    Generated rather than checked in because the application identifier has to
    track TEAM_ID/BUNDLE_ID exactly or the App Store upload is rejected.
    """
    parent = f"""<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>com.apple.security.app-sandbox</key>
  <true/>
  <!-- No network entitlements: the editor is served from the custom app:// scheme
       off local files, and the only external link is handed to the user's default
       browser via shell.openExternal, which the app does not fetch itself. -->
  <!-- Opening and saving documents chosen by the user. -->
  <key>com.apple.security.files.user-selected.read-write</key>
  <true/>
  <key>com.apple.application-identifier</key>
  <string>{TEAM_ID}.{BUNDLE_ID}</string>
  <key>com.apple.developer.team-identifier</key>
  <string>{TEAM_ID}</string>
</dict>
</plist>
"""
    child = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>com.apple.security.app-sandbox</key>
  <true/>
  <key>com.apple.security.inherit</key>
  <true/>
</dict>
</plist>
"""
    parent_path = os.path.join(dest_dir, 'entitlements.mas.plist')
    child_path = os.path.join(dest_dir, 'entitlements.mas.inherit.plist')
    with open(parent_path, 'w') as f:
        f.write(parent)
    with open(child_path, 'w') as f:
        f.write(child)
    return parent_path, child_path


def codesign_app(app_path):
    """Sign the bundle.

    Modifying Electron.app invalidates its signature, and macOS on Apple Silicon
    refuses to launch an arm64 binary with a broken one -- so the bundle is always
    signed, falling back to an ad-hoc signature when no certificate is configured.
    """
    # codesign refuses to sign a bundle carrying extended attributes
    # (quarantine/provenance flags picked up from the downloaded archive)
    try:
        run(['xattr', '-cr', app_path])
    except subprocess.CalledProcessError:
        pass

    if not SIGN_IDENTITY and TARGET != 'mas':
        print("  -> Ad-hoc code signing the app bundle...")
        try:
            run(['codesign', '--force', '--deep', '--sign', '-', app_path],
                stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        except (subprocess.CalledProcessError, FileNotFoundError) as err:
            print(f"  WARNING: ad-hoc codesign failed ({err}); "
                  f"the app may be blocked by Gatekeeper.")
        return

    print(f"  -> Code signing the app bundle ({TARGET})...")
    work_dir = os.path.dirname(app_path)
    if TARGET == 'mas':
        entitlements, entitlements_inherit = write_mas_entitlements(work_dir)
    else:
        entitlements = os.path.join(MAC_DIR, 'entitlements.darwin.plist')
        entitlements_inherit = entitlements

    cfg = {
        'app': app_path,
        'platform': TARGET,
        'entitlements': entitlements,
        'entitlementsInherit': entitlements_inherit,
    }
    if SIGN_IDENTITY:
        cfg['identity'] = SIGN_IDENTITY
    if TARGET == 'mas' and PROVISIONING_PROFILE:
        cfg['provisioningProfile'] = PROVISIONING_PROFILE

    cfg_path = os.path.join(work_dir, 'sign-config.json')
    with open(cfg_path, 'w') as f:
        json.dump(cfg, f)
    try:
        run(['node', os.path.join(MAC_DIR, 'sign-mac.js'), cfg_path])
    finally:
        os.remove(cfg_path)


def notarize(artifact_path):
    """Submit an artifact to Apple's notary service and staple the ticket.

    Only meaningful for the darwin target; App Store builds are reviewed instead.
    Credentials are read from a notarytool keychain profile, so no secrets are
    passed on the command line -- create one with:
        xcrun notarytool store-credentials <profile> --apple-id ... --team-id ...
    """
    if not NOTARY_PROFILE or TARGET == 'mas':
        return
    name = os.path.basename(artifact_path)
    print(f"  -> Notarizing {name} (this can take several minutes)...")
    try:
        run(['xcrun', 'notarytool', 'submit', artifact_path,
             '--keychain-profile', NOTARY_PROFILE, '--wait'])
        run(['xcrun', 'stapler', 'staple', artifact_path])
        print(f"  -> Notarized and stapled {name}")
    except subprocess.CalledProcessError as err:
        print(f"  WARNING: notarization failed for {name} ({err}).")


def package_mac_arch(arch):
    print(f"\n=======================================================")
    print(f"  Packaging macOS App Bundle for Architecture: {arch}")
    print(f"=======================================================")

    zip_path = find_electron_zip(arch)
    if not zip_path:
        print(f"ERROR: Electron {TARGET} zip for {arch} not found!")
        print(f"  Expected electron-v<version>-{TARGET}-{arch}.zip under "
              f"~/Library/Caches/electron (or beside this script).")
        if TARGET == 'mas':
            print("  The Mac App Store needs Electron's sandboxed 'mas' build, which is a "
                  "separate download from the regular one.")
        print(f"  Fetch it with:  npx @electron/get --platform={TARGET} --arch={arch}")
        return False

    print(f"Using base Electron archive: {zip_path}")

    unpacked_arch_dir = os.path.join(BUILD_ROOT, f"{TARGET}-{arch}")
    if os.path.exists(unpacked_arch_dir):
        shutil.rmtree(unpacked_arch_dir)
    os.makedirs(unpacked_arch_dir, exist_ok=True)

    print("Extracting Electron.app...")
    # ditto -xk keeps the symlinks and executable bits that a .app bundle needs
    run(['ditto', '-xk', zip_path, unpacked_arch_dir])

    orig_app = os.path.join(unpacked_arch_dir, "Electron.app")
    target_app = os.path.join(unpacked_arch_dir, f"{APP_NAME}.app")

    if os.path.exists(orig_app):
        os.rename(orig_app, target_app)
    else:
        print(f"ERROR: Electron.app not found in extracted folder.")
        return False

    contents_dir = os.path.join(target_app, "Contents")
    macos_dir = os.path.join(contents_dir, "MacOS")
    resources_dir = os.path.join(contents_dir, "Resources")

    # Rename executable
    orig_exe = os.path.join(macos_dir, "Electron")
    target_exe = os.path.join(macos_dir, APP_NAME)
    if os.path.exists(orig_exe):
        os.rename(orig_exe, target_exe)

    # Update Info.plist (plutil keeps the binary plist format Electron ships)
    info_plist_path = os.path.join(contents_dir, "Info.plist")
    if os.path.exists(info_plist_path):
        plist_values = [
            ('CFBundleDisplayName', 'string', DISPLAY_NAME),
            ('CFBundleName', 'string', APP_NAME),
            ('CFBundleExecutable', 'string', APP_NAME),
            ('CFBundleIdentifier', 'string', BUNDLE_ID),
            ('CFBundleIconFile', 'string', 'icon.icns'),
            ('CFBundleVersion', 'string', BUILD_NUMBER),
            ('CFBundleShortVersionString', 'string', VERSION),

            ('NSHighResolutionCapable', 'bool', 'true'),
            ('NSRequiresAquaSystemAppearance', 'bool', 'false'),
        ]
        for key, kind, value in plist_values:
            subprocess.run(['plutil', '-replace', key, f'-{kind}', value, info_plist_path],
                           check=False)

        # LSMinimumSystemVersion is deliberately NOT overwritten: Electron ships the
        # real floor (43.x requires macOS 12.0) and claiming anything lower offers the
        # app to users whose machines cannot launch it. Override only on purpose.
        if MIN_OS_OVERRIDE:
            subprocess.run(['plutil', '-replace', 'LSMinimumSystemVersion', '-string',
                            MIN_OS_OVERRIDE, info_plist_path], check=False)

    # Install Icons
    if os.path.exists(ICON_ICNS):
        shutil.copyfile(ICON_ICNS, os.path.join(resources_dir, "icon.icns"))
        shutil.copyfile(ICON_ICNS, os.path.join(resources_dir, "electron.icns"))

    # Remove default_app.asar if present
    default_asar = os.path.join(resources_dir, "default_app.asar")
    if os.path.exists(default_asar):
        os.remove(default_asar)

    # Copy app code and assets into Contents/Resources/app
    app_resource_dir = os.path.join(resources_dir, "app")
    if os.path.exists(app_resource_dir):
        shutil.rmtree(app_resource_dir)
    os.makedirs(app_resource_dir, exist_ok=True)

    shutil.copyfile(MAIN_JS, os.path.join(app_resource_dir, "main.js"))
    shutil.copyfile(PACKAGE_JSON, os.path.join(app_resource_dir, "package.json"))
    if os.path.exists(ICON_ICNS):
        shutil.copyfile(ICON_ICNS, os.path.join(app_resource_dir, "icon.icns"))
    if os.path.exists(ICON_PNG):
        shutil.copyfile(ICON_PNG, os.path.join(app_resource_dir, "icon.png"))

    # Copy wwwroot. Prefer an APFS clone: it costs almost no extra disk and avoids the
    # slow per-file reads that time out when the source sits in an iCloud-synced folder.
    dest_wwwroot = os.path.join(app_resource_dir, "app", "wwwroot")
    os.makedirs(os.path.dirname(dest_wwwroot), exist_ok=True)
    try:
        run(['cp', '-Rc', APP_DIR, dest_wwwroot])
    except subprocess.CalledProcessError:
        shutil.rmtree(dest_wwwroot, ignore_errors=True)
        shutil.copytree(APP_DIR, dest_wwwroot)

    codesign_app(target_app)

    print(f"[SUCCESS] {APP_NAME}.app bundle created at: {target_app}")

    suffix = f"Mac-{arch}" if TARGET == 'darwin' else f"MAS-{arch}"

    if TARGET == 'darwin':
        # 1. Create portable ZIP
        zip_output = os.path.join(DIST_DIR, f"{APP_NAME}-{VERSION}-{suffix}.zip")
        print(f"  -> Building macOS ZIP archive: {os.path.basename(zip_output)}...")
        if os.path.exists(zip_output):
            os.remove(zip_output)
        run(['ditto', '-c', '-k', '--sequesterRsrc', '--keepParent', target_app, zip_output])

        # 2. Create DMG installer
        dmg_output = os.path.join(DIST_DIR, f"{APP_NAME}-{VERSION}-{suffix}.dmg")
        create_dmg_from_app(target_app, dmg_output, DISPLAY_NAME)
        notarize(dmg_output)

    # 3. Create PKG installer (the only artifact the App Store accepts)
    pkg_output = os.path.join(DIST_DIR, f"{APP_NAME}-{VERSION}-{suffix}.pkg")
    create_pkg_from_app(target_app, pkg_output)
    notarize(pkg_output)

    # 4. Optionally publish the unpacked bundle next to the artifacts
    if KEEP_UNPACKED:
        published = os.path.join(DIST_DIR, f"{TARGET}-{arch}")
        if os.path.exists(published):
            shutil.rmtree(published)
        os.makedirs(published, exist_ok=True)
        run(['ditto', target_app, os.path.join(published, f"{APP_NAME}.app")])

    shutil.rmtree(unpacked_arch_dir, ignore_errors=True)

    return True


def main():
    if sys.platform != 'darwin':
        print("ERROR: This packaging script must run on macOS (it needs ditto, hdiutil and codesign).")
        sys.exit(1)

    os.makedirs(DIST_DIR, exist_ok=True)
    os.makedirs(BUILD_ROOT, exist_ok=True)

    if not os.path.exists(APP_DIR):
        print("ERROR: app/wwwroot does not exist. Run node package-app.js first.")
        sys.exit(1)

    global TARGET
    args = sys.argv[1:]

    if '--target' in args:
        i = args.index('--target')
        if i + 1 >= len(args):
            print("ERROR: --target requires a value (darwin or mas)")
            sys.exit(2)
        TARGET = args[i + 1]
        del args[i:i + 2]
    elif '--mas' in args:
        TARGET = 'mas'
        args.remove('--mas')

    if TARGET not in ('darwin', 'mas'):
        print(f"ERROR: unknown target '{TARGET}' (expected darwin or mas)")
        sys.exit(2)

    archs = args or ["arm64", "x64"]
    for a in archs:
        if a not in ("arm64", "x64"):
            print(f"ERROR: unknown architecture '{a}' (expected arm64 or x64)")
            sys.exit(2)

    print(f"Target: {TARGET}   bundle: {BUNDLE_ID}   team: {TEAM_ID}")
    print(f"Version: {VERSION} (marketing)   build number: {BUILD_NUMBER} "
          f"(must increase for every App Store upload)")
    if TARGET == 'mas':
        if not SIGN_IDENTITY:
            print("NOTE: KANNADANUDI_SIGN_IDENTITY is unset -- @electron/osx-sign will "
                  "look for a '3rd Party Mac Developer Application' certificate.")
        if not INSTALLER_IDENTITY:
            print("NOTE: KANNADANUDI_INSTALLER_IDENTITY is unset -- the resulting .pkg "
                  "will be UNSIGNED and Transporter will reject it.")
        if not PROVISIONING_PROFILE:
            print("NOTE: KANNADANUDI_PROVISIONING_PROFILE is unset -- App Store builds "
                  "require an embedded macOS provisioning profile.")

    failures = []
    for arch in archs:
        if not package_mac_arch(arch):
            print(f"Packaging failed for {arch}")
            failures.append(arch)

    print("\n=======================================================")
    print("           macOS Dist Packaging Completed!             ")
    print("=======================================================")
    print("Generated artifacts in dist/:")
    for f in sorted(os.listdir(DIST_DIR)):
        fp = os.path.join(DIST_DIR, f)
        if os.path.isfile(fp):
            size_mb = os.path.getsize(fp) / (1024 * 1024)
            print(f"  - {f} ({size_mb:.2f} MB)")
        elif os.path.isdir(fp):
            print(f"  - {f}/ (Unpacked .app bundle)")

    if failures:
        sys.exit(1)


if __name__ == '__main__':
    main()
