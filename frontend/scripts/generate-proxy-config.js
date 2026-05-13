/**
 * Generates proxy.conf.json for Angular development server.
 * The proxy forwards /api/** requests to the Spring Boot backend.
 * Run automatically via `prestart` / `predev` npm scripts.
 */
const fs = require('fs');
const path = require('path');

const frontendRoot = path.resolve(__dirname, '..');
const envPath = path.join(frontendRoot, '.env');
const outputPath = path.join(frontendRoot, 'proxy.conf.json');

// Parse .env file if it exists
function parseEnv(fileText) {
  const result = {};
  for (const line of fileText.split(/\r?\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eqIndex = trimmed.indexOf('=');
    if (eqIndex <= 0) continue;
    result[trimmed.substring(0, eqIndex).trim()] = trimmed.substring(eqIndex + 1).trim();
  }
  return result;
}

let env = {};
if (fs.existsSync(envPath)) {
  env = parseEnv(fs.readFileSync(envPath, 'utf8'));
}

// Default to HTTPS backend on port 8443 (Phase 6 requirement)
const backendUrl = env.BACKEND_URL || 'https://localhost:8443';

const proxyConfig = {
  '/api': {
    target: backendUrl,
    secure: false,          // accept self-signed certs in dev
    changeOrigin: true,
    logLevel: 'warn'
  }
};

fs.writeFileSync(outputPath, JSON.stringify(proxyConfig, null, 2) + '\n', 'utf8');
console.log(`Generated proxy.conf.json → ${backendUrl}`);
