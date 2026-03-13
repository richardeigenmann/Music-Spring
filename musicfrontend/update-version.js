const fs = require('fs');
const path = require('path');

const packageJsonPath = path.join(__dirname, 'package.json');
const versionTsPath = path.join(__dirname, 'src', 'app', 'version.ts');

const packageJson = JSON.parse(fs.readFileSync(packageJsonPath, 'utf8'));
const version = packageJson.version;
const buildDate = new Date().toLocaleString('en-GB', {
  day: '2-digit',
  month: 'short',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
  hour12: false
}).replace(',', '');

const versionTsContent = `
export const VERSION = '${version}';
export const BUILD_DATE = '${buildDate}';
`;

fs.writeFileSync(versionTsPath, versionTsContent);
console.log(`Updated version.ts to version ${version} and build date ${buildDate}`);
