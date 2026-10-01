const { app, BrowserWindow, Menu, session } = require('electron');
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

function createWindow(port) {
  const iconPath = fs.existsSync(path.join(__dirname, 'icon.ico'))
    ? path.join(__dirname, 'icon.ico')
    : path.join(__dirname, 'icon.png');

  const win = new BrowserWindow({
    width: 1280,
    height: 850,
    minWidth: 800,
    minHeight: 600,
    title: 'Kannada Nudi Editor',
    icon: iconPath,
    autoHideMenuBar: true,
    webPreferences: {
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

const gotTheLock = app.requestSingleInstanceLock();

if (!gotTheLock) {
  app.quit();
} else {
  let mainWindow = null;

  app.on('second-instance', () => {
    if (mainWindow) {
      if (mainWindow.isMinimized()) mainWindow.restore();
      mainWindow.focus();
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
      mainWindow = createWindow(port);
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
