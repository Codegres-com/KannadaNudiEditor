#!/usr/bin/env node
/*
 * Signs a packaged KannadaNudi.app with @electron/osx-sign.
 *
 * Electron bundles must be signed inner-most first (helpers, then frameworks,
 * then the app) with different entitlements for the parent and the child
 * processes. `codesign --deep` gets this wrong, so the dedicated signer is used.
 *
 * Usage: node sign-mac.js <config.json>
 *
 * Config keys: app, platform ("darwin"|"mas"), identity, provisioningProfile,
 *              entitlements, entitlementsInherit
 */
const fs = require('fs');
const { signAsync } = require('@electron/osx-sign');

const cfg = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));

const isMas = cfg.platform === 'mas';

const opts = {
  app: cfg.app,
  platform: cfg.platform,
  type: 'distribution',
  strictVerify: true,
  optionsForFile: (filePath) => {
    // Helpers and other nested executables inherit the sandbox on MAS; on darwin
    // every binary gets the same Hardened Runtime exceptions.
    const isChild = /\.app\/Contents\/Frameworks\/.+\.app\//.test(filePath) ||
      /\/(Helper|chrome_crashpad_handler)/.test(filePath);
    return {
      entitlements: isMas && isChild ? cfg.entitlementsInherit : cfg.entitlements,
      hardenedRuntime: !isMas,
    };
  },
};

if (cfg.identity) opts.identity = cfg.identity;
if (cfg.provisioningProfile) opts.provisioningProfile = cfg.provisioningProfile;

signAsync(opts)
  .then(() => console.log(`  signed ${cfg.app} (${cfg.platform})`))
  .catch((err) => {
    console.error(`  signing failed: ${err && err.message ? err.message : err}`);
    process.exit(1);
  });
