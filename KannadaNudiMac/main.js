const { app, BrowserWindow, Menu, shell, session, protocol, net } = require('electron');
const path = require('path');
const fs = require('fs');
const { pathToFileURL } = require('url');

// The editor is served from a custom scheme rather than a loopback HTTP server.
// A listening socket would require the com.apple.security.network.server sandbox
// entitlement, which App Review rejects for an app that only reads its own files.
// Must be declared before the app is ready.
const APP_SCHEME = 'app';
const APP_ORIGIN = `${APP_SCHEME}://kannadanudi`;

protocol.registerSchemesAsPrivileged([
  {
    scheme: APP_SCHEME,
    privileges: {
      standard: true,      // real origin, so relative URLs and history routing work
      secure: true,        // treated as a secure context (required by Blazor)
      supportFetchAPI: true, // Blazor fetches its .wasm/.dll assets
      stream: true,
      corsEnabled: true,
    },
  },
]);

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
  '.icns': 'image/x-icns',
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

let mainWindow = null;

function resolveAssetPath(requestUrl) {
  // Returns an absolute path inside WWWROOT, or null if the request escapes it.
  const { pathname } = new URL(requestUrl);
  let relativePath = decodeURIComponent(pathname);
  if (relativePath.startsWith('/')) {
    relativePath = relativePath.slice(1);
  }

  let filePath = path.normalize(path.join(WWWROOT, relativePath));

  // Security check: prevent path traversal
  if (filePath !== WWWROOT && !filePath.startsWith(WWWROOT + path.sep)) {
    return null;
  }

  if (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory()) {
    filePath = path.join(filePath, 'index.html');
  }

  if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
    return filePath;
  }

  // SPA fallback: Blazor does its own client-side routing
  const indexPath = path.join(WWWROOT, 'index.html');
  return fs.existsSync(indexPath) ? indexPath : null;
}

function registerAppProtocol() {
  protocol.handle(APP_SCHEME, async (request) => {
    let filePath;
    try {
      filePath = resolveAssetPath(request.url);
    } catch (err) {
      return new Response('Bad Request', { status: 400 });
    }

    if (!filePath) {
      return new Response('Not Found', { status: 404 });
    }

    const response = await net.fetch(pathToFileURL(filePath).toString());
    // Content-Type is set explicitly: WebAssembly.instantiateStreaming rejects
    // anything that is not application/wasm, and file URLs do not always carry it.
    const ext = path.extname(filePath).toLowerCase();
    const headers = new Headers(response.headers);
    headers.set('Content-Type', MIME_TYPES[ext] || 'application/octet-stream');
    return new Response(response.body, {
      status: response.status,
      headers,
    });
  });

  console.log(`Kannada Nudi serving ${WWWROOT} at ${APP_ORIGIN}/`);
}

function setupNativeMenu() {
  const isMac = process.platform === 'darwin';

  const template = [
    ...(isMac
      ? [
          {
            label: 'Kannada Nudi',
            submenu: [
              { role: 'about', label: 'About Kannada Nudi' },
              { type: 'separator' },
              { role: 'services' },
              { type: 'separator' },
              { role: 'hide', label: 'Hide Kannada Nudi' },
              { role: 'hideOthers' },
              { role: 'unhide' },
              { type: 'separator' },
              { role: 'quit', label: 'Quit Kannada Nudi' },
            ],
          },
        ]
      : []),
    {
      label: 'Edit',
      submenu: [
        { role: 'undo' },
        { role: 'redo' },
        { type: 'separator' },
        { role: 'cut' },
        { role: 'copy' },
        { role: 'paste' },
        { role: 'selectAll' },
      ],
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { role: 'toggleDevTools' },
        { type: 'separator' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
      ],
    },
    {
      label: 'Window',
      submenu: [
        { role: 'minimize' },
        { role: 'zoom' },
        ...(isMac
          ? [
              { type: 'separator' },
              { role: 'front' },
              { type: 'separator' },
              { role: 'window' },
            ]
          : [{ role: 'close' }]),
      ],
    },
    {
      role: 'help',
      submenu: [
        {
          label: 'Website & Documentation',
          click: async () => {
            await shell.openExternal('https://codegres.com');
          },
        },
      ],
    },
  ];

  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
}

function createWindow() {
  const iconPath = fs.existsSync(path.join(__dirname, 'icon.icns'))
    ? path.join(__dirname, 'icon.icns')
    : path.join(__dirname, 'icon.png');

  const win = new BrowserWindow({
    width: 1280,
    height: 850,
    minWidth: 800,
    minHeight: 600,
    title: 'Kannada Nudi Editor',
    icon: iconPath,
    show: false,
    backgroundColor: '#ffffff',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      webSecurity: true,
    },
  });

  win.once('ready-to-show', () => {
    win.show();
  });

  win.loadURL(`${APP_ORIGIN}/`);

  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  win.on('closed', () => {
    // Drop the reference: a closed BrowserWindow stays truthy but every method on
    // it throws "Object has been destroyed".
    if (mainWindow === win) {
      mainWindow = null;
    }
  });

  return win;
}

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow && !mainWindow.isDestroyed()) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
    } else {
      mainWindow = createWindow();
    }
  });

  app.whenReady().then(async () => {
    try {
      session.defaultSession.setPermissionRequestHandler((webContents, permission, callback) => {
        if (permission === 'media' || permission === 'microphone') {
          return callback(true);
        }
        callback(false);
      });

      session.defaultSession.setPermissionCheckHandler((webContents, permission) => {
        return permission === 'media' || permission === 'microphone';
      });

      setupNativeMenu();
      registerAppProtocol();
      mainWindow = createWindow();
    } catch (err) {
      console.error('Failed to start application:', err);
      app.quit();
    }
  });

  app.on('activate', () => {
    // On macOS, re-create or focus window when the dock icon is clicked
    if (BrowserWindow.getAllWindows().length === 0) {
      mainWindow = createWindow();
    } else if (mainWindow && !mainWindow.isDestroyed()) {
      mainWindow.show();
      mainWindow.focus();
    }
  });

  app.on('window-all-closed', () => {
    // On macOS, keep app active in dock until user explicitly Cmd+Q
    if (process.platform !== 'darwin') {
      app.quit();
    }
  });
}
