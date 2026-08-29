const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const tar = require('tar');

function createArHeader(filename, size, mtime = Math.floor(Date.now() / 1000), mode = 0o100644) {
  const buf = Buffer.alloc(60, ' ');
  const nameStr = (filename + '/').slice(0, 16);
  buf.write(nameStr, 0, 'ascii');
  buf.write(String(mtime).slice(0, 12), 16, 'ascii');
  buf.write('0', 28, 'ascii');
  buf.write('0', 34, 'ascii');
  buf.write(mode.toString(8).slice(0, 8), 40, 'ascii');
  buf.write(String(size).slice(0, 10), 48, 'ascii');
  buf[58] = 0x60; // `
  buf[59] = 0x0a; // \n
  return buf;
}

function writeArArchive(outFile, members) {
  const chunks = [Buffer.from('!<arch>\n', 'ascii')];

  for (const member of members) {
    const header = createArHeader(member.name, member.data.length, member.mtime, member.mode);
    chunks.push(header);
    chunks.push(member.data);
    if (member.data.length % 2 !== 0) {
      chunks.push(Buffer.from('\n', 'ascii'));
    }
  }

  const result = Buffer.concat(chunks);
  fs.writeFileSync(outFile, result);
}

async function buildDebianPackage() {
  const linuxDir = __dirname;
  const pkg = JSON.parse(fs.readFileSync(path.join(linuxDir, 'package.json'), 'utf8'));
  const version = pkg.version || '1.0.1';
  const unpackedDir = path.join(linuxDir, 'dist', 'linux-unpacked');
  const distDir = path.join(linuxDir, 'dist');
  const tempDir = path.join(distDir, 'temp-deb');
  const debOutput = path.join(distDir, `kannadanudi_${version}_amd64.deb`);

  if (!fs.existsSync(unpackedDir)) {
    console.error('Error: dist/linux-unpacked does not exist. Run electron-builder first.');
    process.exit(1);
  }

  console.log('Packaging Ubuntu / Debian (.deb) release...');

  if (fs.existsSync(tempDir)) {
    fs.rmSync(tempDir, { recursive: true, force: true });
  }

  const controlRoot = path.join(tempDir, 'control-root');
  const dataRoot = path.join(tempDir, 'data-root');
  fs.mkdirSync(controlRoot, { recursive: true });
  fs.mkdirSync(dataRoot, { recursive: true });

  // Data structure
  const appTargetDir = path.join(dataRoot, 'usr', 'lib', 'kannadanudi');
  const binDir = path.join(dataRoot, 'usr', 'bin');
  const desktopDir = path.join(dataRoot, 'usr', 'share', 'applications');
  const iconsDir = path.join(dataRoot, 'usr', 'share', 'pixmaps');

  fs.mkdirSync(appTargetDir, { recursive: true });
  fs.mkdirSync(binDir, { recursive: true });
  fs.mkdirSync(desktopDir, { recursive: true });
  fs.mkdirSync(iconsDir, { recursive: true });

  function copyDir(src, dest) {
    fs.mkdirSync(dest, { recursive: true });
    const entries = fs.readdirSync(src, { withFileTypes: true });
    for (const entry of entries) {
      const srcPath = path.join(src, entry.name);
      const destPath = path.join(dest, entry.name);
      if (entry.isDirectory()) {
        copyDir(srcPath, destPath);
      } else {
        fs.copyFileSync(srcPath, destPath);
      }
    }
  }

  copyDir(unpackedDir, appTargetDir);

  const iconSrc = path.join(linuxDir, 'icon.png');
  if (fs.existsSync(iconSrc)) {
    fs.copyFileSync(iconSrc, path.join(iconsDir, 'kannadanudi.png'));
  }

  const launcherContent = `#!/bin/bash\nexec /usr/lib/kannadanudi/kannadanudilinux "$@"\n`;
  const launcherPath = path.join(binDir, 'kannadanudi');
  fs.writeFileSync(launcherPath, launcherContent, { encoding: 'utf8', mode: 0o755 });

  const desktopContent = `[Desktop Entry]
Name=Kannada Nudi
Comment=Offline Kannada Nudi Text Editor
Exec=/usr/bin/kannadanudi %U
Icon=kannadanudi
Terminal=false
Type=Application
Categories=Office;Utility;TextEditor;
StartupWMClass=kannadanudilinux
`;
  fs.writeFileSync(path.join(desktopDir, 'kannadanudi.desktop'), desktopContent, 'utf8');

  let totalBytes = 0;
  const md5Entries = [];

  function scanData(dir, rel = '') {
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      const entryRel = rel ? `${rel}/${entry.name}` : entry.name;
      if (entry.isDirectory()) {
        scanData(fullPath, entryRel);
      } else {
        const stat = fs.statSync(fullPath);
        totalBytes += stat.size;
        const fileData = fs.readFileSync(fullPath);
        const md5 = crypto.createHash('md5').update(fileData).digest('hex');
        md5Entries.push(`${md5}  ${entryRel}`);
      }
    }
  }

  scanData(dataRoot);
  const installedSizeKb = Math.ceil(totalBytes / 1024);

  const controlContent = `Package: kannadanudi
Version: ${version}
Section: utils
Priority: optional
Architecture: amd64
Installed-Size: ${installedSizeKb}
Maintainer: Codegres <contact@codegres.com>
Homepage: https://codegres.com
Depends: libgtk-3-0, libnotify4, libnss3, libasound2, libxss1, libxtst6
Description: Kannada Nudi Editor desktop app for Linux / Ubuntu
 Powerful, offline-ready Kannada rich text editor built with .NET Blazor WebAssembly and Electron.
 Features include Kannada phonetic keyboard typing, Unicode <-> ASCII conversion, LaTeX formula editing, and DocX import/export.
`;

  fs.writeFileSync(path.join(controlRoot, 'control'), controlContent.replace(/\r\n/g, '\n'), 'utf8');
  fs.writeFileSync(path.join(controlRoot, 'md5sums'), md5Entries.join('\n') + '\n', 'utf8');

  const controlTarGz = path.join(tempDir, 'control.tar.gz');
  await tar.c(
    {
      gzip: true,
      file: controlTarGz,
      cwd: controlRoot,
      portable: true,
      mtime: new Date(1700000000000),
    },
    ['control', 'md5sums']
  );

  const dataTarGz = path.join(tempDir, 'data.tar.gz');
  const dataEntries = fs.readdirSync(dataRoot);
  await tar.c(
    {
      gzip: true,
      file: dataTarGz,
      cwd: dataRoot,
      portable: true,
      mtime: new Date(1700000000000),
    },
    dataEntries
  );

  const debianBinaryData = Buffer.from('2.0\n', 'ascii');
  const controlData = fs.readFileSync(controlTarGz);
  const dataData = fs.readFileSync(dataTarGz);

  const members = [
    { name: 'debian-binary', data: debianBinaryData, mode: 0o100644 },
    { name: 'control.tar.gz', data: controlData, mode: 0o100644 },
    { name: 'data.tar.gz', data: dataData, mode: 0o100644 }
  ];

  writeArArchive(debOutput, members);
  fs.rmSync(tempDir, { recursive: true, force: true });

  const stat = fs.statSync(debOutput);
  console.log(`[SUCCESS] Generated Ubuntu .deb installer: ${debOutput} (${(stat.size / (1024 * 1024)).toFixed(2)} MB)`);
}

buildDebianPackage().catch(err => {
  console.error('Error generating debian package:', err);
  process.exit(1);
});
