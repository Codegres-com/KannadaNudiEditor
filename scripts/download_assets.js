const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');

function downloadFile(url, dest, force = false) {
  return new Promise((resolve, reject) => {
    if (!force && fs.existsSync(dest) && fs.statSync(dest).size > 0) {
      console.log(`  [exists] ${path.basename(dest)}`);
      return resolve();
    }
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    const file = fs.createWriteStream(dest);
    const client = url.startsWith('https') ? https : http;

    client.get(url, (response) => {
      if (response.statusCode >= 300 && response.statusCode < 400 && response.headers.location) {
        let redirectUrl = response.headers.location;
        if (!redirectUrl.startsWith('http')) {
          const u = new URL(url);
          redirectUrl = `${u.protocol}//${u.host}${redirectUrl}`;
        }
        file.close();
        if (fs.existsSync(dest)) fs.unlinkSync(dest);
        return downloadFile(redirectUrl, dest, force).then(resolve).catch(reject);
      }
      if (response.statusCode !== 200) {
        file.close();
        if (fs.existsSync(dest)) fs.unlinkSync(dest);
        return reject(new Error(`Failed to download ${url}: status ${response.statusCode}`));
      }
      response.pipe(file);
      file.on('finish', () => {
        file.close(() => resolve());
      });
    }).on('error', (err) => {
      file.close();
      if (fs.existsSync(dest)) fs.unlinkSync(dest);
      reject(err);
    });
  });
}

async function main() {
  const baseDir = path.join(__dirname, '..', 'KannadaNudiWeb', 'wwwroot');
  
  console.log('--------------------------------------------------');
  console.log('[1/4] Downloading Core CSS & JS libraries...');
  console.log('--------------------------------------------------');
  const directFiles = [
    { url: 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css', dest: path.join(baseDir, 'lib', 'bootstrap', 'bootstrap.min.css') },
    { url: 'https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js', dest: path.join(baseDir, 'lib', 'bootstrap', 'bootstrap.bundle.min.js') },
    { url: 'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css', dest: path.join(baseDir, 'lib', 'bootstrap-icons', 'bootstrap-icons.min.css') },
    { url: 'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/fonts/bootstrap-icons.woff2', dest: path.join(baseDir, 'lib', 'bootstrap-icons', 'fonts', 'bootstrap-icons.woff2') },
    { url: 'https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/fonts/bootstrap-icons.woff', dest: path.join(baseDir, 'lib', 'bootstrap-icons', 'fonts', 'bootstrap-icons.woff') },
    { url: 'https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.7.1/katex.min.css', dest: path.join(baseDir, 'lib', 'katex', 'katex.min.css') },
    { url: 'https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.7.1/katex.min.js', dest: path.join(baseDir, 'lib', 'katex', 'katex.min.js') },
    { url: 'https://cdn.jsdelivr.net/npm/docshift@latest/dist/docshift.min.js', dest: path.join(baseDir, 'lib', 'docshift', 'docshift.min.js') },
    { url: 'https://cdnjs.cloudflare.com/ajax/libs/mammoth/1.6.0/mammoth.browser.min.js', dest: path.join(baseDir, 'lib', 'mammoth', 'mammoth.browser.min.js') },
  ];

  for (const f of directFiles) {
    try {
      console.log(`Downloading ${path.basename(f.dest)}...`);
      await downloadFile(f.url, f.dest);
    } catch (e) {
      console.warn(`Failed ${f.url}: ${e.message}`);
    }
  }

  // KaTeX fonts
  console.log('\n--------------------------------------------------');
  console.log('[2/4] Downloading KaTeX & Typography fonts...');
  console.log('--------------------------------------------------');
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

  for (const f of katexFonts) {
    const url = `https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.7.1/fonts/${f}`;
    const dest = path.join(baseDir, 'lib', 'katex', 'fonts', f);
    try {
      await downloadFile(url, dest);
    } catch (e) {
      console.warn(e.message);
    }
  }

  // Google Fonts Poppins & Noto Sans Kannada
  const fontList = [
    { name: 'Poppins-Regular.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-Regular.ttf' },
    { name: 'Poppins-Medium.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-Medium.ttf' },
    { name: 'Poppins-SemiBold.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-SemiBold.ttf' },
    { name: 'Poppins-Bold.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-Bold.ttf' },
    { name: 'NotoSansKannada-Regular.ttf', url: 'https://raw.githubusercontent.com/google/fonts/main/ofl/notosanskannada/NotoSansKannada%5Bwdth%2Cwght%5D.ttf' }
  ];

  for (const font of fontList) {
    const dest = path.join(baseDir, 'fonts', font.name);
    try {
      await downloadFile(font.url, dest);
    } catch (e) {
      console.warn(e.message);
    }
  }

  // Transformers.js and ONNX Runtime Web WebAssembly files
  console.log('\n--------------------------------------------------');
  console.log('[3/4] Downloading Transformers.js & ONNX WASM binaries...');
  console.log('--------------------------------------------------');
  const transformersVersion = '2.17.2';
  const transformersFiles = [
    { url: `https://cdn.jsdelivr.net/npm/@xenova/transformers@${transformersVersion}/dist/transformers.min.js`, dest: path.join(baseDir, 'lib', 'transformers', 'transformers.min.js') },
    { url: `https://cdn.jsdelivr.net/npm/@xenova/transformers@${transformersVersion}/dist/ort-wasm.wasm`, dest: path.join(baseDir, 'lib', 'transformers', 'ort-wasm.wasm') },
    { url: `https://cdn.jsdelivr.net/npm/@xenova/transformers@${transformersVersion}/dist/ort-wasm-simd.wasm`, dest: path.join(baseDir, 'lib', 'transformers', 'ort-wasm-simd.wasm') },
    { url: `https://cdn.jsdelivr.net/npm/@xenova/transformers@${transformersVersion}/dist/ort-wasm-threaded.wasm`, dest: path.join(baseDir, 'lib', 'transformers', 'ort-wasm-threaded.wasm') },
    { url: `https://cdn.jsdelivr.net/npm/@xenova/transformers@${transformersVersion}/dist/ort-wasm-simd-threaded.wasm`, dest: path.join(baseDir, 'lib', 'transformers', 'ort-wasm-simd-threaded.wasm') },
  ];

  for (const f of transformersFiles) {
    try {
      console.log(`Downloading ${path.basename(f.dest)}...`);
      await downloadFile(f.url, f.dest);
    } catch (e) {
      console.warn(`Failed ${f.url}: ${e.message}`);
    }
  }

  // Xenova/whisper-tiny Model & Tokenizer files for Offline STT (Kannada & English)
  console.log('\n--------------------------------------------------');
  console.log('[4/4] Downloading Whisper-tiny ONNX offline models...');
  console.log('--------------------------------------------------');
  const whisperBaseUrl = 'https://huggingface.co/Xenova/whisper-tiny/resolve/main/';
  const whisperModelDir = path.join(baseDir, 'models', 'Xenova', 'whisper-tiny');
  const whisperFiles = [
    'config.json',
    'generation_config.json',
    'preprocessor_config.json',
    'tokenizer.json',
    'tokenizer_config.json',
    'vocab.json',
    'merges.txt',
    'normalizer.json',
    'added_tokens.json',
    'special_tokens_map.json',
    'quant_config.json',
    'onnx/encoder_model_quantized.onnx',
    'onnx/decoder_model_merged_quantized.onnx'
  ];

  for (const relFile of whisperFiles) {
    const url = whisperBaseUrl + relFile;
    const dest = path.join(whisperModelDir, relFile);
    try {
      console.log(`Downloading whisper-tiny: ${relFile}...`);
      await downloadFile(url, dest);
    } catch (e) {
      console.warn(`Failed ${url}: ${e.message}`);
    }
  }

  console.log('\n==================================================');
  console.log('All offline assets & models downloaded successfully!');
  console.log('==================================================');
}

main().catch(console.error);
