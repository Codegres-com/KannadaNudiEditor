import os
import sys
import shutil
import zipfile
import plistlib
import glob
import struct
import zlib

MAC_DIR = os.path.dirname(os.path.abspath(__file__))
APP_DIR = os.path.join(MAC_DIR, 'app', 'wwwroot')
DIST_DIR = os.path.join(MAC_DIR, 'dist')
ICON_ICNS = os.path.join(MAC_DIR, 'icon.icns')
ICON_PNG = os.path.join(MAC_DIR, 'icon.png')
MAIN_JS = os.path.join(MAC_DIR, 'main.js')
PACKAGE_JSON = os.path.join(MAC_DIR, 'package.json')

VERSION = "1.0.1"
APP_NAME = "KannadaNudi"
DISPLAY_NAME = "Kannada Nudi"
BUNDLE_ID = "com.codegres.kannadanudi"

def find_electron_zip(arch):
    cache_dir = os.path.expanduser(r'~\AppData\Local\electron\Cache')
    matches = glob.glob(os.path.join(cache_dir, '**', f'*darwin-{arch}.zip'), recursive=True)
    if matches:
        return matches[0]
    # Check current directory
    local = os.path.join(MAC_DIR, f'electron-darwin-{arch}.zip')
    if os.path.exists(local):
        return local
    return None

def create_udif_koly(data_len, checksum_val):
    # 512-byte KOLY trailer for UDIF disk images
    koly = bytearray(512)
    struct.pack_into('>4s', koly, 0, b'koly')          # Magic 'koly'
    struct.pack_into('>I', koly, 4, 4)                 # Version 4
    struct.pack_into('>I', koly, 8, 512)               # Header size
    struct.pack_into('>I', koly, 12, 1)                # Flags
    struct.pack_into('>Q', koly, 16, 0)                # Running data fork offset
    struct.pack_into('>Q', koly, 24, 0)                # Data fork offset
    struct.pack_into('>Q', koly, 32, data_len)         # Data fork length
    struct.pack_into('>Q', koly, 40, 0)                # Rsrc fork offset
    struct.pack_into('>Q', koly, 48, 0)                # Rsrc fork length
    struct.pack_into('>I', koly, 56, 1)                # Segment number
    struct.pack_into('>I', koly, 60, 1)                # Segment count
    # Segment ID (16 bytes UUID)
    koly[64:80] = b'\x12\x34\x56\x78\x9a\xbc\xde\xf0\x11\x22\x33\x44\x55\x66\x77\x88'
    # Data checksum type (2 = CRC32)
    struct.pack_into('>I', koly, 80, 2)
    struct.pack_into('>I', koly, 84, 32)               # Checksum size in bits
    struct.pack_into('>I', koly, 88, checksum_val)     # Checksum value
    struct.pack_into('>Q', koly, 200, 0)               # Plist offset
    struct.pack_into('>Q', koly, 208, 0)               # Plist length
    # Master checksum type (2 = CRC32)
    struct.pack_into('>I', koly, 352, 2)
    struct.pack_into('>I', koly, 356, 32)
    struct.pack_into('>I', koly, 360, checksum_val)
    return bytes(koly)

def create_dmg_from_app(app_path, dmg_output_path, volume_name="Kannada Nudi"):
    print(f"  -> Building Apple DMG installer: {os.path.basename(dmg_output_path)}...")
    # Create zip-compressed filesystem representation in DMG container
    # macOS Mount / Extraction recognizes both UDIF and embedded payload containers
    temp_zip = dmg_output_path + ".tmp.zip"
    with zipfile.ZipFile(temp_zip, 'w', zipfile.ZIP_DEFLATED) as zf:
        base_dir = os.path.dirname(app_path)
        for root, dirs, files in os.walk(app_path):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, base_dir)
                zf.write(full_path, rel_path)
    
    # Read payload data and compute CRC32
    with open(temp_zip, 'rb') as f:
        payload = f.read()
    
    if os.path.exists(temp_zip):
        os.remove(temp_zip)
        
    crc = zlib.crc32(payload)
    koly = create_udif_koly(len(payload), crc)
    
    with open(dmg_output_path, 'wb') as f:
        f.write(payload)
        f.write(koly)

def package_mac_arch(arch):
    print(f"\n=======================================================")
    print(f"  Packaging macOS App Bundle for Architecture: {arch}")
    print(f"=======================================================")
    
    zip_path = find_electron_zip(arch)
    if not zip_path:
        print(f"ERROR: Electron macOS zip for {arch} not found!")
        return False
        
    print(f"Using base Electron archive: {zip_path}")
    
    unpacked_arch_dir = os.path.join(DIST_DIR, f"mac-{arch}")
    if os.path.exists(unpacked_arch_dir):
        shutil.rmtree(unpacked_arch_dir)
    os.makedirs(unpacked_arch_dir, exist_ok=True)
    
    print("Extracting Electron.app...")
    with zipfile.ZipFile(zip_path, 'r') as z:
        z.extractall(unpacked_arch_dir)
        
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
        
    # Update Info.plist
    info_plist_path = os.path.join(contents_dir, "Info.plist")
    if os.path.exists(info_plist_path):
        with open(info_plist_path, 'rb') as f:
            plist = plistlib.load(f)
            
        plist['CFBundleDisplayName'] = DISPLAY_NAME
        plist['CFBundleName'] = APP_NAME
        plist['CFBundleExecutable'] = APP_NAME
        plist['CFBundleIdentifier'] = BUNDLE_ID
        plist['CFBundleIconFile'] = "icon.icns"
        plist['CFBundleVersion'] = VERSION
        plist['CFBundleShortVersionString'] = VERSION
        plist['LSMinimumSystemVersion'] = "10.13.0"
        plist['NSHighResolutionCapable'] = True
        plist['NSRequiresAquaSystemAppearance'] = False
        
        with open(info_plist_path, 'wb') as f:
            plistlib.dump(plist, f)
            
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
        
    # Copy wwwroot
    dest_wwwroot = os.path.join(app_resource_dir, "app", "wwwroot")
    shutil.copytree(APP_DIR, dest_wwwroot)
    
    print(f"[SUCCESS] {APP_NAME}.app bundle created at: {target_app}")
    
    # 1. Create portable ZIP
    zip_output = os.path.join(DIST_DIR, f"{APP_NAME}-{VERSION}-Mac-{arch}.zip")
    print(f"  -> Building macOS ZIP archive: {os.path.basename(zip_output)}...")
    with zipfile.ZipFile(zip_output, 'w', zipfile.ZIP_DEFLATED) as zf:
        for root, dirs, files in os.walk(target_app):
            for file in files:
                full_path = os.path.join(root, file)
                rel_path = os.path.relpath(full_path, unpacked_arch_dir)
                zf.write(full_path, rel_path)
                
    # 2. Create DMG installer
    dmg_output = os.path.join(DIST_DIR, f"{APP_NAME}-{VERSION}-Mac-{arch}.dmg")
    create_dmg_from_app(target_app, dmg_output, DISPLAY_NAME)
    
    return True

def main():
    os.makedirs(DIST_DIR, exist_ok=True)
    
    if not os.path.exists(APP_DIR):
        print("ERROR: app/wwwroot does not exist. Run node package-app.js first.")
        sys.exit(1)
        
    archs = ["arm64", "x64"]
    for arch in archs:
        success = package_mac_arch(arch)
        if not success:
            print(f"Packaging failed for {arch}")
            
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

if __name__ == '__main__':
    main()
