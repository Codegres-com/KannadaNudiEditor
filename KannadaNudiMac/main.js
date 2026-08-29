const { app, BrowserWindow, Menu, shell, session } = require('electron');
const path = require('path');
const http = require('http');
const fs = require('fs');

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

let httpServer = null;
let currentPort = null;
let mainWindow = null;

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
      currentPort = port;
      console.log(`Kannada Nudi Local Server running on http://127.0.0.1:${port}`);
      resolve(port);
    });

    server.on('error', (err) => {
      if (err.code === 'EADDRINUSE' && preferredPort !== 0) {
        console.warn(`Port ${preferredPort} is in use, attempting fallback port...`);
        server.listen(0, '127.0.0.1', () => {
          const port = server.address().port;
          httpServer = server;
          currentPort = port;
          console.log(`Kannada Nudi Local Server running on http://127.0.0.1:${port}`);
          resolve(port);
        });
      } else {
        reject(err);
      }
    });
  });
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

function createWindow(port) {
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

  win.loadURL(`http://127.0.0.1:${port}/`);

  win.webContents.setWindowOpenHandler(({ url }) => {
    shell.openExternal(url);
    return { action: 'deny' };
  });

  return win;
}

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  app.quit();
} else {
  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
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
      const port = await startServer(47124);
      mainWindow = createWindow(port);
    } catch (err) {
      console.error('Failed to start application:', err);
      app.quit();
    }
  });

  app.on('activate', () => {
    // On macOS, re-create or focus window when the dock icon is clicked
    if (BrowserWindow.getAllWindows().length === 0) {
      if (currentPort) {
        mainWindow = createWindow(currentPort);
      }
    } else if (mainWindow) {
      mainWindow.show();
      mainWindow.focus();
    }
  });

  app.on('window-all-closed', () => {
    // On macOS, keep app active in dock until user explicitly Cmd+Q
    if (process.platform !== 'darwin') {
      if (httpServer) {
        httpServer.close();
      }
      app.quit();
    }
  });

  app.on('will-quit', () => {
    if (httpServer) {
      httpServer.close();
    }
  });
}
