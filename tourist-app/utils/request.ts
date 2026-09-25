import { clearAuth, getToken } from './auth';
import { parseSseText, SseStreamParser, type SseEvent } from './sse';

declare const __API_BASE_URL__: string;
export type UniRequestOptions = Omit<UniApp.RequestOptions, 'url'> & { url: string };
export type TransportError = Error & {
  errorType: 'TRANSPORT_ERROR' | 'HTTP_ERROR';
  statusCode: number;
  requestUrl: string;
  method: string;
  originalError?: unknown;
  response?: unknown;
};
const API_BASE_URL = typeof __API_BASE_URL__ === 'string' ? __API_BASE_URL__.replace(/\/$/, '') : '';
function withBaseUrl(url: string): string { return url.startsWith('http') ? url : `${API_BASE_URL}${url}`; }
function authHeader(): Record<string, string> { const token = getToken(); return token ? { satoken: token } : {}; }
function isPublicAgentPath(url: string): boolean { return url === '/api/tourist/agent/plan' || url.endsWith('/api/tourist/agent/plan'); }
function handleUnauthorized(): void { clearAuth(); uni.showToast({ title: '请先登录', icon: 'none' }); uni.redirectTo({ url: '/pages/login/index' }); }
function serverMessage(data: unknown): string {
  if (!data || typeof data !== 'object' || !('msg' in data)) return '';
  const msg = (data as { msg?: unknown }).msg;
  return typeof msg === 'string' ? msg : '';
}

export function request<T>(options: UniRequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    const requestUrl = withBaseUrl(options.url); const method = options.method || 'GET';
    console.log('[RequestStart]', { url: requestUrl, method });
    const headers = isPublicAgentPath(options.url) ? {} : authHeader();
    uni.request({ url: requestUrl, method, data: options.data, header: { 'content-type': 'application/json', ...headers, ...(options.header || {}) }, success: (response) => {
      const statusCode = Number(response.statusCode || 0); console.log('[RequestSuccess]', { url: requestUrl, method, statusCode, data: response.data });
      if (statusCode < 200 || statusCode >= 300) { console.error('[HTTPError]', { statusCode, url: requestUrl, body: response.data }); const detail = serverMessage(response.data); const error = new Error(`HTTP_${statusCode || 'UNKNOWN'}${detail ? `: ${detail}` : ''}`) as TransportError; error.errorType = 'HTTP_ERROR'; error.statusCode = statusCode; error.requestUrl = requestUrl; error.method = String(method); error.response = response.data; reject(error); return; }
      const body = response.data as unknown;
      if (body && typeof body === 'object' && 'code' in (body as object) && 'data' in (body as object)) {
        const wrapped = body as { code: number; msg?: string; data: T };
        if (wrapped.code !== 200 && wrapped.code !== 0) {
          const error = new Error(wrapped.msg || `API_${wrapped.code}`) as TransportError;
          error.errorType = 'HTTP_ERROR'; error.statusCode = wrapped.code; error.requestUrl = requestUrl; error.method = String(method); error.response = body;
          reject(error); return;
        }
        resolve(wrapped.data);
        return;
      }
      resolve(body as T);
    }, fail: (error) => { console.error('[RequestFail]', { url: requestUrl, method, errMsg: error?.errMsg, originalError: error }); const requestError = new Error(error?.errMsg || '') as TransportError; requestError.errorType = 'TRANSPORT_ERROR'; requestError.statusCode = 0; requestError.requestUrl = requestUrl; requestError.method = String(method); requestError.originalError = error; reject(requestError); } });
  });
}
export function get<T>(url: string, data?: any): Promise<T> { return request<T>({ url, data, method: 'GET' }); }
export function post<T>(url: string, data?: any): Promise<T> { return request<T>({ url, data, method: 'POST' }); }
export function put<T>(url: string, data?: any): Promise<T> { return request<T>({ url, data, method: 'PUT' }); }
export function upload<T>(url: string, filePath: string, name: string, formData?: Record<string, string | number>): Promise<T> {
  return new Promise((resolve, reject) => { uni.uploadFile({ url: withBaseUrl(url), filePath, name, formData, header: authHeader(), success: (response) => { try { resolve(JSON.parse(response.data) as T); } catch (error: unknown) { reject(error); } }, fail: (error) => reject(new Error(error.errMsg || '文件上传失败')) }); });
}
export function uploadRaw(url: string, filePath: string, name: string, formData?: Record<string, string | number>): Promise<string> {
  return new Promise((resolve, reject) => { uni.uploadFile({ url: withBaseUrl(url), filePath, name, formData, header: { Accept: 'text/event-stream', ...authHeader() }, success: (response) => { if (response.statusCode === 401) { handleUnauthorized(); reject(new Error('请先登录')); return; } if (response.statusCode < 200 || response.statusCode >= 300) { reject(new Error(`HTTP_${response.statusCode}`)); return; } resolve(response.data); }, fail: (error) => reject(new Error(error.errMsg || '文件上传失败')) }); });
}
export function rawPost(url: string, data?: any): Promise<string> { return new Promise((resolve, reject) => { uni.request({ url: withBaseUrl(url), data, method: 'POST', header: { 'content-type': 'application/json', Accept: 'text/event-stream', ...authHeader() }, success: (response) => { if (response.statusCode < 200 || response.statusCode >= 300) { reject(new Error(`HTTP_${response.statusCode}`)); return; } resolve(typeof response.data === 'string' ? response.data : JSON.stringify(response.data)); }, fail: (error) => reject(new Error(error.errMsg || '网络请求失败')) }); }); }
export type SseEventHandler = (event: SseEvent) => void;
export async function streamPost(url: string, data: any, onEvent: SseEventHandler): Promise<void> { parseSseText(await rawPost(url, data)).forEach(onEvent); }
export async function streamUpload(url: string, file: Blob | string, name: string, formData: Record<string, string | number> | undefined, onEvent: SseEventHandler): Promise<void> { if (typeof file !== 'string') throw new Error('H5 upload stream unsupported'); parseSseText(await uploadRaw(url, file, name, formData)).forEach(onEvent); }
