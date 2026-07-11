declare module '*.vue' {
  import type { DefineComponent } from 'vue';

  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>;
  export default component;
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare module 'vue' {
  import type { App, Component } from 'vue';

  export function createSSRApp(rootComponent: Component, rootProps?: Record<string, unknown>): App;
}

export {};
