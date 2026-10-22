const { spawn } = require('child_process');
const { EventEmitter } = require('events');
const fs = require('fs');
const path = require('path');
const readline = require('readline');

// Native helper published by package-app.js. Executables cannot run from inside app.asar,
// so electron-builder unpacks it (see "asarUnpack" in package.json).
const HELPER_PATH = path
  .join(__dirname, 'app', 'global-keyboard', 'KannadaNudiGlobalKeyboard.exe')
  .replace(`app.asar${path.sep}`, `app.asar.unpacked${path.sep}`);

const MAX_RESTARTS = 3;

/**
 * Global (Direct Type) Kannada keyboard: drives KannadaNudiGlobalKeyboard.exe, which hooks the
 * keyboard system-wide so F9 switches every application (browser, Word, Excel, ...) to Kannada.
 *
 * Emits 'state' with { available, enabled, layout } whenever anything changes.
 */
class GlobalKeyboard extends EventEmitter {
  constructor({ layout = 'Nudi' } = {}) {
    super();
    this.state = { available: false, enabled: false, layout };
    this.child = null;
    this.restarts = 0;
    this.stopping = false;
  }

  start() {
    if (process.platform !== 'win32' || !fs.existsSync(HELPER_PATH)) {
      console.warn(`Global keyboard helper not found at ${HELPER_PATH}; Global mode disabled.`);
      return;
    }

    const args = ['--host-pid', String(process.pid), '--layout', this.state.layout];
    if (this.state.enabled) args.push('--enabled');

    const child = spawn(HELPER_PATH, args, { stdio: ['pipe', 'pipe', 'inherit'], windowsHide: true });
    this.child = child;

    readline.createInterface({ input: child.stdout }).on('line', (line) => this._onLine(line));

    child.on('error', (err) => console.error('Global keyboard helper failed to start:', err));
    child.on('exit', (code) => {
      this.child = null;
      this._setState({ available: false });
      if (!this.stopping && code !== 2 && this.restarts < MAX_RESTARTS) {
        this.restarts++;
        console.warn(`Global keyboard helper exited (${code}); restarting.`);
        setTimeout(() => this.start(), 1000);
      }
    });
  }

  stop() {
    this.stopping = true;
    if (this.child) {
      this._send('exit');
      this.child.stdin.end();
    }
  }

  toggle() {
    this._send('toggle');
  }

  setEnabled(enabled) {
    this._send(enabled ? 'enable' : 'disable');
  }

  setLayout(layout) {
    if (layout !== 'Nudi' && layout !== 'Baraha') return;
    this._send(`layout ${layout}`);
  }

  _send(command) {
    if (this.child && this.child.stdin.writable) {
      this.child.stdin.write(`${command}\n`);
    }
  }

  _onLine(line) {
    const [type, ...rest] = line.trim().split(' ');
    if (type === 'state') {
      this._setState({ available: true, enabled: rest[0] === 'on', layout: rest[1] || this.state.layout });
    } else if (type === 'error') {
      console.error('Global keyboard helper error:', rest.join(' '));
    }
  }

  _setState(changes) {
    const previous = this.state;
    this.state = { ...previous, ...changes };
    this.emit('state', this.state, previous);
  }
}

module.exports = { GlobalKeyboard };
