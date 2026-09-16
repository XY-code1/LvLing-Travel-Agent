const path = require('node:path');
const moduleAlias = require('module-alias');
const { defineConfig, loadEnv } = require('vite');
const uni = require('@dcloudio/vite-plugin-uni').default;

const projectRoot = __dirname;
process.env.UNI_CLI_CONTEXT = process.env.UNI_CLI_CONTEXT || projectRoot;
process.env.UNI_INPUT_DIR = process.env.UNI_INPUT_DIR || projectRoot;
process.env.UNI_OUTPUT_DIR = process.env.UNI_OUTPUT_DIR || path.resolve(projectRoot, 'unpackage/dist/build/h5');

const hbuilderxPlugins =
  process.env.UNI_HBUILDERX_PLUGINS ||
  (process.env.HX_APP_ROOT ? path.resolve(process.env.HX_APP_ROOT, 'plugins') : '');

if (hbuilderxPlugins) {
  const sassPackage = path.resolve(hbuilderxPlugins, 'compile-dart-sass/node_modules/sass');
  const typescriptPackage = path.resolve(
    hbuilderxPlugins,
    'compile-typescript/node_modules/typescript'
  );

  moduleAlias.addAlias('sass', sassPackage);
  moduleAlias.addAlias('typescript', typescriptPackage);

  const originalJoin = path.join;
  path.join = function joinWithHBuilderXPreprocessors(...paths) {
    const safePaths = paths.map((item) => (item == null ? '' : item));
    if (
      safePaths.length === 4 &&
      safePaths[1] === 'node_modules' &&
      safePaths[3] === 'package.json' &&
      safePaths[2] === 'sass'
    ) {
      return path.resolve(sassPackage, 'package.json');
    }
    return originalJoin(...safePaths);
  };
}

module.exports = defineConfig(({ mode }) => {
  const env = loadEnv(mode, projectRoot, 'VITE_');
  return {
  root: projectRoot,
  define: {
    __AMAP_JS_KEY__: JSON.stringify(env.VITE_AMAP_JS_KEY || ''),
    __AMAP_SECURITY_CODE__: JSON.stringify(env.VITE_AMAP_SECURITY_CODE || '')
  },
  server: {
    port: 5175,
    host: '0.0.0.0',
    proxy: {
      '/api/tourist': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/files': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: process.env.UNI_OUTPUT_DIR,
    emptyOutDir: true
  },
  plugins: [uni()]
  };
});
