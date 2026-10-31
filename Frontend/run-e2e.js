const { spawnSync } = require('child_process');
const path = require('path');

const isWin = process.platform === 'win32';
const backendDir = path.resolve(__dirname, '../Backend/demo');
const cmd = isWin ? 'gradlew.bat' : './gradlew';

const result = spawnSync(cmd, ['e2e', '--no-daemon'], {
  cwd: backendDir,
  stdio: 'inherit',
  shell: true,
});

process.exit(result.status ?? 0);
