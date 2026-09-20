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
  const devProxyTarget = env.VITE_DEV_PROXY_TARGET || 'http://localhost:8080';
  return {
  root: projectRoot,
  define: {
    __AMAP_JS_KEY__: JSON.stringify(env.VITE_AMAP_JS_KEY || ''),
    __AMAP_SECURITY_CODE__: JSON.stringify(env.VITE_AMAP_SECURITY_CODE || ''),
    __API_BASE_URL__: JSON.stringify(env.VITE_API_BASE_URL || ''),
    __DEFAULT_CITY_NAME__: JSON.stringify(env.VITE_DEFAULT_CITY || '杭州'),
    __DEFAULT_CITY_CODE__: JSON.stringify(env.VITE_DEFAULT_CITY_CODE || '330100')
  },
  server: {
    port: 5176,
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: devProxyTarget,
        changeOrigin: true,
        bypass(req) {
          // Keep UniApp source modules under /api/*.ts in Vite; only API
          // requests should be forwarded to the backend.
          if (/\/api\/[^?]+\.(?:ts|js|vue)(?:\?.*)?$/.test(req.url || '')) return req.url;
        }
      },
      '/api/tourist': {
        target: devProxyTarget,
        changeOrigin: true
      },
      '/files': {
        target: devProxyTarget,
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
