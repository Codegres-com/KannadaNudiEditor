const { app, BrowserWindow, Menu, Tray, ipcMain, screen, session } = require('electron');
const path = require('path');
const http = require('http');
const fs = require('fs');
const { GlobalKeyboard } = require('./global-keyboard');

const WWWROOT = path.join(__dirname, 'app', 'wwwroot');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json',
  '.wasm': 'application/wasm',
  '.onnx': 'application/octet-stream',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.otf': 'font/otf',
  '.eot': 'application/vnd.ms-fontobject',
  '.dll': 'application/octet-stream',
  '.dat': 'application/octet-stream',
  '.blat': 'application/octet-stream',
  '.bin': 'application/octet-stream',
  '.br': 'application/octet-stream',
  '.gz': 'application/octet-stream',
  '.map': 'application/json',
  '.webmanifest': 'application/manifest+json',
};

let httpServer = null;

function startServer(preferredPort = 47124) {
  return new Promise((resolve, reject) => {
    const server = http.createServer((req, res) => {
      try {
        const cleanUrl = req.url.split('?')[0].split('#')[0];
        let relativePath = decodeURIComponent(cleanUrl);
        if (relativePath.startsWith('/')) {
          relativePath = relativePath.slice(1);
        }

        let filePath = path.join(WWWROOT, relativePath);

        // Security check: prevent path traversal
        if (!filePath.startsWith(WWWROOT)) {
          res.writeHead(403);
          res.end('Forbidden');
          return;
        }

        // If directory or empty, serve index.html
        if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
          filePath = path.join(filePath, 'index.html');
        }

        // If file exists, serve it
        if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
          const ext = path.extname(filePath).toLowerCase();
          const contentType = MIME_TYPES[ext] || 'application/octet-stream';
          res.writeHead(200, {
            'Content-Type': contentType,
            'Access-Control-Allow-Origin': '*',
            'Cache-Control': 'no-cache',
          });
          fs.createReadStream(filePath).pipe(res);
          return;
        }

        // SPA Fallback: for Blazor client-side routing, serve index.html
        const indexPath = path.join(WWWROOT, 'index.html');
        if (fs.existsSync(indexPath)) {
          res.writeHead(200, {
            'Content-Type': 'text/html; charset=utf-8',
            'Access-Control-Allow-Origin': '*',
          });
          fs.createReadStream(indexPath).pipe(res);
          return;
        }

        res.writeHead(404);
        res.end('Not Found');
      } catch (err) {
        res.writeHead(500);
        res.end('Server Error: ' + err.message);
      }
    });

    server.listen(preferredPort, '127.0.0.1', () => {
      const port = server.address().port;
      httpServer = server;
      console.log(`Kannada Nudi Local Server running on http://127.0.0.1:${port}`);
      resolve(port);
    });

    server.on('error', (err) => {
      if (err.code === 'EADDRINUSE' && preferredPort !== 0) {
        console.warn(`Port ${preferredPort} is in use, attempting ephemeral port...`);
        server.listen(0, '127.0.0.1', () => {
          const port = server.address().port;
          httpServer = server;
          console.log(`Kannada Nudi Local Server running on http://127.0.0.1:${port}`);
          resolve(port);
        });
      } else {
        reject(err);
      }
    });
  });
}

const ICON_PATH = fs.existsSync(path.join(__dirname, 'icon.ico'))
  ? path.join(__dirname, 'icon.ico')
  : path.join(__dirname, 'icon.png');

function createWindow(port, { show = true } = {}) {
  const win = new BrowserWindow({
    show,
    width: 1280,
    height: 850,
    minWidth: 800,
    minHeight: 600,
    title: 'Kannada Nudi Editor',
    icon: ICON_PATH,
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: true,
    },
  });

  win.loadURL(`http://127.0.0.1:${port}/`);

  win.webContents.setWindowOpenHandler(({ url }) => {
    require('electron').shell.openExternal(url);
    return { action: 'deny' };
  });

  return win;
}

// ---------------------------------------------------------------------------
// Global (Direct Type) keyboard: F9 types Kannada in any application.
// ---------------------------------------------------------------------------

const SETTINGS_PATH = path.join(app.getPath('userData'), 'settings.json');

function readSettings() {
  try {
    return JSON.parse(fs.readFileSync(SETTINGS_PATH, 'utf8'));
  } catch {
    return {};
  }
}

function writeSettings(changes) {
  try {
    fs.writeFileSync(SETTINGS_PATH, JSON.stringify({ ...readSettings(), ...changes }, null, 2));
  } catch (err) {
    console.warn('Could not save settings:', err);
  }
}

const INDICATOR_WIDTH = 260;
const INDICATOR_HEIGHT = 44;
const INDICATOR_MARGIN = 16;

let indicatorWindow = null;

function positionModeIndicator() {
  if (!indicatorWindow || indicatorWindow.isDestroyed()) return;
  const { workArea } = screen.getPrimaryDisplay();
  indicatorWindow.setBounds({
    x: workArea.x + workArea.width - INDICATOR_WIDTH - INDICATOR_MARGIN,
    y: workArea.y + workArea.height - INDICATOR_HEIGHT - INDICATOR_MARGIN,
    width: INDICATOR_WIDTH,
    height: INDICATOR_HEIGHT,
  });
}

// Sticky, click-through, non-focusable toast in the bottom-right corner showing the current
// Global mode language for as long as the app runs. It pulses briefly when F9 switches it.
function updateModeIndicator({ available, enabled, layout }, changed) {
  if (!available) {
    if (indicatorWindow && !indicatorWindow.isDestroyed()) indicatorWindow.hide();
    return;
  }

  const width = INDICATOR_WIDTH;
  const height = INDICATOR_HEIGHT;

  if (!indicatorWindow || indicatorWindow.isDestroyed()) {
    indicatorWindow = new BrowserWindow({
      width,
      height,
      frame: false,
      transparent: true,
      resizable: false,
      movable: false,
      focusable: false,
      skipTaskbar: true,
      alwaysOnTop: true,
      show: false,
      webPreferences: { nodeIntegration: false, contextIsolation: true, javascript: false },
    });
    indicatorWindow.setIgnoreMouseEvents(true);
    indicatorWindow.setAlwaysOnTop(true, 'screen-saver');
  }

  const language = enabled ? 'Kannada' : 'English';
  const detail = enabled ? `ಕನ್ನಡ · ${layout}` : 'Press F9 for Kannada';
  const accent = enabled ? '#e65100' : '#546e7a';
  const html = `<!doctype html><meta charset="utf-8">
    <style>
      @keyframes pulse { 0% { transform: scale(1.08); background: ${accent}; } 100% { transform: scale(1); } }
      .toast { ${changed ? 'animation: pulse .6s ease-out;' : ''} }
    </style>
    <body style="margin:0;overflow:hidden;font-family:'Nirmala UI','Segoe UI',sans-serif">
    <div class="toast" style="box-sizing:border-box;height:${height}px;padding:0 12px;border-radius:8px;
      background:rgba(33,33,33,.9);color:#fff;border-left:5px solid ${accent};display:flex;align-items:center;gap:10px">
      <div style="flex:1;min-width:0;line-height:1.15">
        <div style="font-size:14px;font-weight:600">Global Mode: ${language}</div>
        <div style="font-size:11px;opacity:.75;white-space:nowrap">${detail}</div>
      </div>
      <div style="font-size:11px;font-weight:600;padding:2px 6px;border:1px solid rgba(255,255,255,.5);border-radius:4px">F9</div>
    </div></body>`;

  positionModeIndicator();
  indicatorWindow.loadURL(`data:text/html;charset=utf-8,${encodeURIComponent(html)}`);
  if (!indicatorWindow.isVisible()) indicatorWindow.showInactive();
}

function buildTrayMenu(globalKeyboard, showEditor) {
  const { available, enabled, layout } = globalKeyboard.state;
  const template = [
    {
      label: 'Global mode (F9)',
      type: 'checkbox',
      checked: enabled,
      enabled: available,
      click: () => globalKeyboard.toggle(),
    },
    {
      label: 'Nudi / KGP layout',
      type: 'radio',
      checked: layout === 'Nudi',
      enabled: available,
      click: () => globalKeyboard.setLayout('Nudi'),
    },
    {
      label: 'Baraha (Phonetic) layout',
      type: 'radio',
      checked: layout === 'Baraha',
      enabled: available,
      click: () => globalKeyboard.setLayout('Baraha'),
    },
    { type: 'separator' },
    { label: 'Open Kannada Nudi Editor', click: showEditor },
  ];

  // Store (MSIX) builds manage startup through the package manifest instead.
  if (!process.windowsStore) {
    template.push({
      label: 'Start with Windows',
      type: 'checkbox',
      checked: app.getLoginItemSettings({ args: ['--hidden'] }).openAtLogin,
      click: (item) => app.setLoginItemSettings({ openAtLogin: item.checked, args: ['--hidden'] }),
    });
  }

  template.push({ type: 'separator' }, { label: 'Quit', click: () => app.quit() });
  return Menu.buildFromTemplate(template);
}

function trayTooltip({ available, enabled, layout }) {
  if (!available) return 'Kannada Nudi Editor';
  return enabled
    ? `Kannada Nudi - Global mode ON (${layout}). F9 for English.`
    : 'Kannada Nudi - Global mode OFF. F9 to type Kannada anywhere.';
}

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  app.quit();
} else {
  let mainWindow = null;
  let tray = null;
  let isQuitting = false;
  const globalKeyboard = new GlobalKeyboard({ layout: readSettings().globalKeyboardLayout || 'Nudi' });

  function showEditor() {
    if (!mainWindow) return;
    if (mainWindow.isMinimized()) mainWindow.restore();
    mainWindow.show();
    mainWindow.focus();
  }

  app.on('second-instance', showEditor);

  app.on('before-quit', () => {
    isQuitting = true;
    globalKeyboard.stop();
  });

  ipcMain.handle('global-keyboard:get-state', () => globalKeyboard.state);
  ipcMain.on('global-keyboard:toggle', () => globalKeyboard.toggle());
  ipcMain.on('global-keyboard:set-layout', (_event, layout) => globalKeyboard.setLayout(layout));

  globalKeyboard.on('state', (state, previous) => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.webContents.send('global-keyboard:state', state);
    }
    if (tray) {
      tray.setToolTip(trayTooltip(state));
      tray.setContextMenu(buildTrayMenu(globalKeyboard, showEditor));
    }
    if (state.layout !== previous.layout) {
      writeSettings({ globalKeyboardLayout: state.layout });
    }
    const changed = previous.available && state.enabled !== previous.enabled;
    if (state.available !== previous.available || state.enabled !== previous.enabled || state.layout !== previous.layout) {
      updateModeIndicator(state, changed);
    }
  });

  app.whenReady().then(async () => {
    try {
      // Configure microphone and media permissions for offline speech recognition
      session.defaultSession.setPermissionRequestHandler((webContents, permission, callback) => {
        if (permission === 'media' || permission === 'microphone') {
          return callback(true);
        }
        callback(false);
      });

      session.defaultSession.setPermissionCheckHandler((webContents, permission) => {
        return permission === 'media' || permission === 'microphone';
      });

      const port = await startServer(47124);
      // "--hidden" is passed when launched by "Start with Windows": stay in the tray.
      mainWindow = createWindow(port, { show: !process.argv.includes('--hidden') });

      globalKeyboard.start();

      // Keep the Global Mode toast in the corner when resolution, scaling or monitors change.
      for (const event of ['display-metrics-changed', 'display-added', 'display-removed']) {
        screen.on(event, positionModeIndicator);
      }

      tray = new Tray(ICON_PATH);
      tray.setToolTip(trayTooltip(globalKeyboard.state));
      tray.setContextMenu(buildTrayMenu(globalKeyboard, showEditor));
      tray.on('click', showEditor);

      // The toast window would otherwise keep the app alive after the editor really closes.
      mainWindow.on('closed', () => {
        if (indicatorWindow && !indicatorWindow.isDestroyed()) indicatorWindow.destroy();
      });

      // Closing the editor keeps the app in the tray so Global mode (F9) keeps working.
      let trayHintShown = false;
      mainWindow.on('close', (event) => {
        if (isQuitting || !globalKeyboard.state.available) return;
        event.preventDefault();
        mainWindow.hide();
        if (!trayHintShown) {
          trayHintShown = true;
          tray.displayBalloon({
            iconType: 'info',
            title: 'Kannada Nudi is still running',
            content: 'Press F9 in any application to type Kannada. Right-click the tray icon to quit.',
          });
        }
      });
    } catch (err) {
      console.error('Failed to start application:', err);
      app.quit();
    }
  });

  app.on('window-all-closed', () => {
    if (httpServer) {
      httpServer.close();
    }
    if (process.platform !== 'darwin') {
      app.quit();
    }
  });
}
