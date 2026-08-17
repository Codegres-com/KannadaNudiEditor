const { app, BrowserWindow } = require('electron');
const path = require('path');
const http = require('http');
const fs = require('fs');

const WWWROOT = path.join(__dirname, 'app', 'wwwroot');
const PORT = 47123;

const MIME_TYPES = {
  '.html': 'text/html',
  '.js': 'text/javascript',
  '.css': 'text/css',
  '.json': 'application/json',
  '.wasm': 'application/wasm',
  '.png': 'image/png',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.dll': 'application/octet-stream',
  '.br': 'application/octet-stream',
  '.gz': 'application/octet-stream',
};

function startServer() {
  return new Promise((resolve) => {
    const server = http.createServer((req, res) => {
      let filePath = path.join(WWWROOT, decodeURIComponent(req.url.split('?')[0]));
      if (filePath.endsWith(path.sep)) filePath = path.join(filePath, 'index.html');

      fs.readFile(filePath, (err, data) => {
        if (err) {
          if (err.code === 'EISDIR') {
            fs.readFile(path.join(filePath, 'index.html'), (err2, data2) => {
              if (err2) {
                res.writeHead(404);
                res.end('Not found');
                return;
              }
              res.writeHead(200, { 'Content-Type': 'text/html' });
              res.end(data2);
            });
            return;
          }
          res.writeHead(404);
          res.end('Not found');
          return;
        }
        const ext = path.extname(filePath).toLowerCase();
        res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'application/octet-stream' });
        res.end(data);
      });
    });
    server.listen(PORT, '127.0.0.1', () => resolve(server));
  });
}

function createWindow() {
  const win = new BrowserWindow({
    width: 1200,
    height: 800,
    icon: path.join(__dirname, 'icon.png'),
  });
  win.loadURL(`http://localhost:${PORT}/`);
}

app.whenReady().then(async () => {
  await startServer();
  createWindow();
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});
