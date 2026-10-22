const { execFile, spawn } = require('child_process');
const { EventEmitter } = require('events');
const fs = require('fs');
const path = require('path');

// Linux has no equivalent of the Windows low-level keyboard hook: Wayland (Ubuntu's default)
// does not let apps read or inject keystrokes system-wide, and snap confinement blocks uinput.
// Instead Global mode adds the Kannada IBus engine (ibus-m17n) to GNOME's input sources, so
// Kannada works in every application and Super+Space switches between English and Kannada.
const SCHEMA = 'org.gnome.desktop.input-sources';
const KEY = 'sources';

// Nudi is the KGP (Kannada Ganaka Parishat) layout; m17n's ITRANS is the closest to Baraha.
const ENGINES = {
  Nudi: 'm17n:kn:kgp',
  Baraha: 'm17n:kn:itrans',
};
const KANNADA_ENGINES = Object.values(ENGINES);

const SHORTCUT = 'Super+Space';

function hintFor(layout) {
  return `Adds Kannada (${layout}) to your keyboard input sources so you can type Kannada in any ` +
    `application. Switch language with ${SHORTCUT}. Requires ibus-m17n (sudo apt install ibus-m17n).`;
}

function findGsettings() {
  const candidates = [];
  if (process.env.SNAP) {
    candidates.push(
      path.join(process.env.SNAP, 'usr', 'bin', 'gsettings'),
      path.join(process.env.SNAP, 'gnome-platform', 'usr', 'bin', 'gsettings')
    );
  }
  candidates.push('/usr/bin/gsettings');
  return candidates.find((candidate) => fs.existsSync(candidate)) || 'gsettings';
}

// Parses a GVariant a(ss) such as "[('xkb', 'us'), ('ibus', 'm17n:kn:kgp')]".
function parseSources(text) {
  const sources = [];
  const pattern = /\(\s*'([^']*)'\s*,\s*'([^']*)'\s*\)/g;
  let match;
  while ((match = pattern.exec(text)) !== null) {
    sources.push([match[1], match[2]]);
  }
  return sources;
}

function formatSources(sources) {
  return `[${sources.map(([type, id]) => `('${type}', '${id}')`).join(', ')}]`;
}

/**
 * Global mode on Linux: toggles the Kannada IBus engine in GNOME input sources.
 *
 * Emits 'state' with { available, enabled, layout, shortcut, hint } whenever anything changes,
 * matching the Windows GlobalKeyboard so the editor's Global mode button works unchanged.
 */
class GlobalKeyboard extends EventEmitter {
  constructor({ layout = 'Nudi' } = {}) {
    super();
    this.gsettings = findGsettings();
    this.state = {
      available: false,
      enabled: false,
      layout,
      shortcut: SHORTCUT,
      hint: hintFor(layout),
    };
    this.monitor = null;
  }

  async start() {
    if (process.platform !== 'linux') return;

    const sources = await this._read();
    if (!sources) {
      console.warn('GNOME input sources are not available; Global mode disabled.');
      return;
    }
    this._apply(sources);

    // Keep the button in sync when the user changes input sources in GNOME Settings.
    this.monitor = spawn(this.gsettings, ['monitor', SCHEMA, KEY], { stdio: ['ignore', 'pipe', 'ignore'] });
    this.monitor.stdout.on('data', (chunk) => this._apply(parseSources(chunk.toString())));
    this.monitor.on('error', () => (this.monitor = null));
  }

  stop() {
    if (this.monitor) {
      this.monitor.kill();
      this.monitor = null;
    }
  }

  async toggle() {
    await this.setEnabled(!this.state.enabled);
  }

  async setEnabled(enabled) {
    if (!this.state.available) return;
    const sources = await this._read();
    if (!sources) return;

    const others = sources.filter(([type, id]) => !(type === 'ibus' && KANNADA_ENGINES.includes(id)));
    const next = enabled ? [...others, ['ibus', ENGINES[this.state.layout]]] : others;
    await this._write(next);
  }

  async setLayout(layout) {
    if (!ENGINES[layout] || layout === this.state.layout) return;
    this._setState({ layout });
    if (this.state.enabled) await this.setEnabled(true);
  }

  _apply(sources) {
    const kannada = sources.find(([type, id]) => type === 'ibus' && KANNADA_ENGINES.includes(id));
    const layout = kannada
      ? Object.keys(ENGINES).find((name) => ENGINES[name] === kannada[1])
      : this.state.layout;
    this._setState({ available: true, enabled: Boolean(kannada), layout });
  }

  _read() {
    return new Promise((resolve) => {
      execFile(this.gsettings, ['get', SCHEMA, KEY], (err, stdout) => {
        if (err) {
          console.warn('gsettings get failed:', err.message);
          resolve(null);
        } else {
          resolve(parseSources(stdout));
        }
      });
    });
  }

  _write(sources) {
    return new Promise((resolve) => {
      execFile(this.gsettings, ['set', SCHEMA, KEY, formatSources(sources)], (err) => {
        if (err) {
          console.error('gsettings set failed:', err.message);
        } else {
          this._apply(sources);
        }
        resolve();
      });
    });
  }

  _setState(changes) {
    const previous = this.state;
    this.state = { ...previous, ...changes };
    this.state.hint = hintFor(this.state.layout);
    if (JSON.stringify(previous) !== JSON.stringify(this.state)) {
      this.emit('state', this.state, previous);
    }
  }
}

module.exports = { GlobalKeyboard };
