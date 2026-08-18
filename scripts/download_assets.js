const https = require('https');
const fs = require('fs');
const path = require('path');

function downloadFile(url, dest) {
  return new Promise((resolve, reject) => {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    const file = fs.createWriteStream(dest);
    https.get(url, (response) => {
      if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
        return downloadFile(response.headers.location, dest).then(resolve).catch(reject);
      }
      if (response.statusCode !== 200) {
        return reject(new Error(`Failed to download ${url}: status ${response.statusCode}`));
      }
      response.pipe(file);
      file.on('finish', () => {
        file.close(() => resolve());
      });
    }).on('error', (err) => {
      fs.unlink(dest, () => reject(err));
    });
  });
}

async function main() {
  const baseDir = path.join(__dirname, '..', 'KannadaNudiWeb', 'wwwroot');
  
  // KaTeX fonts
  const katexFonts = [
    'KaTeX_AMS-Regular.woff2', 'KaTeX_AMS-Regular.woff', 'KaTeX_AMS-Regular.ttf',
    'KaTeX_Caligraphic-Bold.woff2', 'KaTeX_Caligraphic-Bold.woff', 'KaTeX_Caligraphic-Bold.ttf',
    'KaTeX_Caligraphic-Regular.woff2', 'KaTeX_Caligraphic-Regular.woff', 'KaTeX_Caligraphic-Regular.ttf',
    'KaTeX_Fraktur-Bold.woff2', 'KaTeX_Fraktur-Bold.woff', 'KaTeX_Fraktur-Bold.ttf',
    'KaTeX_Fraktur-Regular.woff2', 'KaTeX_Fraktur-Regular.woff', 'KaTeX_Fraktur-Regular.ttf',
    'KaTeX_Main-Bold.woff2', 'KaTeX_Main-Bold.woff', 'KaTeX_Main-Bold.ttf',
    'KaTeX_Main-Italic.woff2', 'KaTeX_Main-Italic.woff', 'KaTeX_Main-Italic.ttf',
    'KaTeX_Main-Regular.woff2', 'KaTeX_Main-Regular.woff', 'KaTeX_Main-Regular.ttf',
    'KaTeX_Math-Italic.woff2', 'KaTeX_Math-Italic.woff', 'KaTeX_Math-Italic.ttf',
    'KaTeX_SansSerif-Regular.woff2', 'KaTeX_SansSerif-Regular.woff', 'KaTeX_SansSerif-Regular.ttf',
    'KaTeX_Script-Regular.woff2', 'KaTeX_Script-Regular.woff', 'KaTeX_Script-Regular.ttf',
    'KaTeX_Size1-Regular.woff2', 'KaTeX_Size1-Regular.woff', 'KaTeX_Size1-Regular.ttf',
    'KaTeX_Size2-Regular.woff2', 'KaTeX_Size2-Regular.woff', 'KaTeX_Size2-Regular.ttf',
    'KaTeX_Size3-Regular.woff2', 'KaTeX_Size3-Regular.woff', 'KaTeX_Size3-Regular.ttf',
    'KaTeX_Size4-Regular.woff2', 'KaTeX_Size4-Regular.woff', 'KaTeX_Size4-Regular.ttf',
    'KaTeX_Typewriter-Regular.woff2', 'KaTeX_Typewriter-Regular.woff', 'KaTeX_Typewriter-Regular.ttf'
  ];

  console.log('Downloading KaTeX fonts...');
  for (const f of katexFonts) {
    const url = `https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.7.1/fonts/${f}`;
    const dest = path.join(baseDir, 'lib', 'katex', 'fonts', f);
    try {
      await downloadFile(url, dest);
    } catch (e) {
      console.warn(e.message);
    }
  }

  // Google Fonts Poppins (woff2)
  console.log('Downloading Poppins fonts...');
  // We can fetch Poppins woff2 or ttf files
  const poppinsFonts = [
    { name: 'Poppins-Regular.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-Regular.ttf' },
    { name: 'Poppins-Medium.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-Medium.ttf' },
    { name: 'Poppins-SemiBold.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-SemiBold.ttf' },
    { name: 'Poppins-Bold.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-Bold.ttf' },
    { name: 'NotoSansKannada-Regular.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/notosanskannada/NotoSansKannada%5Bwdth%2Cwght%5D.ttf' }
  ];

  for (const font of poppinsFonts) {
    const dest = path.join(baseDir, 'fonts', font.name);
    try {
      console.log(`Downloading ${font.name}...`);
      await downloadFile(font.url, dest);
    } catch (e) {
      console.warn(e.message);
    }
  }

  console.log('All downloads completed successfully!');
}

main().catch(console.error);
