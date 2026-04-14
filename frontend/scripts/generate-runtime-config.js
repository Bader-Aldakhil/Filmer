const fs = require('fs');
const path = require('path');

const frontendRoot = path.resolve(__dirname, '..');
const envPath = path.join(frontendRoot, '.env');
const outputPath = path.join(frontendRoot, 'src', 'assets', 'runtime-config.js');

function parseEnv(fileText) {
  const result = {};
  const lines = fileText.split(/\r?\n/);

  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) {
      continue;
    }

    const eqIndex = trimmed.indexOf('=');
    if (eqIndex <= 0) {
      continue;
    }

    const key = trimmed.substring(0, eqIndex).trim();
    const value = trimmed.substring(eqIndex + 1).trim();
    result[key] = value;
  }

  return result;
}

let env = {};
if (fs.existsSync(envPath)) {
  const envText = fs.readFileSync(envPath, 'utf8');
  env = parseEnv(envText);
}

const omdbApiKey = env.OMDB_API_KEY || '';
const tmdbApiKey = env.TMDB_API_KEY || '';
const fileContent = [
  'window.RUNTIME_CONFIG = {',
  `  TMDB_API_KEY: '${tmdbApiKey.replace(/\\/g, '\\\\').replace(/'/g, "\\'")}',`,
  `  OMDB_API_KEY: '${omdbApiKey.replace(/\\/g, '\\\\').replace(/'/g, "\\'")}'`,
  '};',
  ''
].join('\n');

fs.writeFileSync(outputPath, fileContent, 'utf8');
console.log('Generated runtime config at src/assets/runtime-config.js');
