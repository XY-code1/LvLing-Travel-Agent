const { existsSync, spawnSync } = require('node:fs');
const path = require('node:path');
const { spawnSync: run } = require('node:child_process');

const hbuilderRoot = process.env.HX_APP_ROOT || 'E:\\HBuilderX\\HBuilderX';
const plugins = process.env.UNI_HBUILDERX_PLUGINS || path.join(hbuilderRoot, 'plugins');
const cliRoot = path.join(plugins, 'uniapp-cli-vite');
const vite = path.join(cliRoot, 'node_modules', '.bin', process.platform === 'win32' ? 'vite.cmd' : 'vite');
if (!existsSync(vite)) {
  console.error(`HBuilderX Vite CLI not found: ${vite}`);
  process.exit(1);
}
const result = run(vite, ['build'], {
  cwd: path.resolve(__dirname, '..'),
  stdio: 'inherit',
  shell: process.platform === 'win32',
  env: { ...process.env, HX_APP_ROOT: hbuilderRoot, UNI_HBUILDERX_PLUGINS: plugins,
    NODE_PATH: [path.join(path.resolve(__dirname, '..'), 'node_modules'), path.join(cliRoot, 'node_modules')].join(path.delimiter) }
});
process.exit(result.status ?? 1);
