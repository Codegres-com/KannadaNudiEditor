const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

const ROOT_DIR = path.resolve(__dirname, '..');
const WEB_DIR = path.join(ROOT_DIR, 'KannadaNudiWeb');
const WEB_CSPROJ = path.join(WEB_DIR, 'KannadaNudiWeb.csproj');
const WEB_PUBLISH_DIR = path.join(WEB_DIR, 'publish');
const WEB_PUBLISH_WWWROOT = path.join(WEB_PUBLISH_DIR, 'wwwroot');
const LINUX_APP_DIR = path.join(__dirname, 'app');
const LINUX_WWWROOT = path.join(LINUX_APP_DIR, 'wwwroot');

function copyRecursive(src, dest) {
  if (!fs.existsSync(src)) {
    throw new Error(`Source path does not exist: ${src}`);
  }
  const stat = fs.statSync(src);
  if (stat.isDirectory()) {
    if (!fs.existsSync(dest)) {
      fs.mkdirSync(dest, { recursive: true });
    }
    const entries = fs.readdirSync(src);
    for (const entry of entries) {
      copyRecursive(path.join(src, entry), path.join(dest, entry));
    }
  } else {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.copyFileSync(src, dest);
  }
}

function removeDirRecursive(dir) {
  if (fs.existsSync(dir)) {
    fs.rmSync(dir, { recursive: true, force: true });
  }
}

console.log('====================================================');
console.log('  Packaging Kannada Nudi Web into Linux Application ');
console.log('====================================================');

console.log('\n[1/3] Publishing .NET Blazor WebAssembly project in Release mode...');
try {
  const publishCmd = `dotnet publish "${WEB_CSPROJ}" -c Release -o "${WEB_PUBLISH_DIR}"`;
  console.log(`> ${publishCmd}`);
  execSync(publishCmd, { stdio: 'inherit', cwd: WEB_DIR });
} catch (err) {
  console.error('\nERROR: Failed to publish dotnet project.', err);
  process.exit(1);
}

console.log('\n[2/3] Cleaning previous app directory...');
removeDirRecursive(LINUX_APP_DIR);
fs.mkdirSync(LINUX_WWWROOT, { recursive: true });

console.log('\n[3/3] Copying published assets to KannadaNudiLinux/app/wwwroot...');
if (fs.existsSync(WEB_PUBLISH_WWWROOT)) {
  copyRecursive(WEB_PUBLISH_WWWROOT, LINUX_WWWROOT);
  console.log('Successfully copied published wwwroot to app/wwwroot.');
} else {
  console.error(`\nERROR: Publish output not found at ${WEB_PUBLISH_WWWROOT}`);
  process.exit(1);
}

// Verification checks
const essentialFiles = [
  path.join(LINUX_WWWROOT, 'index.html'),
  path.join(LINUX_WWWROOT, '_framework', 'blazor.webassembly.js'),
  path.join(LINUX_WWWROOT, 'lib', 'bootstrap', 'bootstrap.min.css'),
  path.join(LINUX_WWWROOT, 'lib', 'bootstrap', 'bootstrap.bundle.min.js'),
  path.join(LINUX_WWWROOT, 'lib', 'bootstrap-icons', 'bootstrap-icons.min.css'),
  path.join(LINUX_WWWROOT, 'lib', 'katex', 'katex.min.css'),
  path.join(LINUX_WWWROOT, 'lib', 'katex', 'katex.min.js'),
  path.join(LINUX_WWWROOT, 'lib', 'docshift', 'docshift.min.js'),
  path.join(LINUX_WWWROOT, 'lib', 'mammoth', 'mammoth.browser.min.js'),
  path.join(LINUX_WWWROOT, 'fonts', 'SmartNudi1-Regular_0.ttf'),
  path.join(LINUX_WWWROOT, 'fonts', 'Poppins-Regular.ttf')
];

let allPassed = true;
for (const file of essentialFiles) {
  if (!fs.existsSync(file)) {
    console.warn(`WARNING: Missing expected file: ${file}`);
    allPassed = false;
  }
}

if (allPassed) {
  console.log('\n[SUCCESS] Kannada Nudi Web packaged successfully with all offline assets!');
} else {
  console.warn('\n[COMPLETED WITH WARNINGS] Some expected files were not found.');
}
