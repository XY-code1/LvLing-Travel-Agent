const { existsSync } = require('node:fs');
const path = require('node:path');
const { spawnSync } = require('node:child_process');

const hbuilderRoot = process.env.HX_APP_ROOT || 'E:\\HBuilderX\\HBuilderX';
const tsc = path.join(hbuilderRoot, 'plugins', 'compile-typescript', 'node_modules', 'typescript', 'bin', 'tsc');
const uniTypes = path.join(hbuilderRoot, 'plugins', 'uniapp-cli', 'node_modules', '@dcloudio', 'types', 'index.d.ts');
if (!existsSync(tsc)) {
  console.error(`HBuilderX TypeScript compiler not found: ${tsc}`);
  process.exit(1);
}
const result = spawnSync(process.execPath, [tsc, '--noEmit', '--skipLibCheck', '--target', 'ES2020',
  '--module', 'ESNext', '--moduleResolution', 'node',
  uniTypes, 'composables/useCityContext.ts', 'services/serviceFacilityService.ts',
  'services/scenicSpotService.ts'], {
  cwd: path.resolve(__dirname, '..'), stdio: 'inherit'
});
process.exit(result.status ?? 1);
