import type { ApiResult } from '../types';
import { clearAuth, getToken } from './auth';
import { parseSseText, SseStreamParser, type SseEvent } from './sse';

export type UniRequestOptions = Omit<UniApp.RequestOptions, 'url'> & {
  url: string;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '';

function withBaseUrl(url: string): string {
  return url.startsWith('http') ? url : `${API_BASE_URL}${url}`;
}

function authHeader(): Record<string, string> {
  const token = getToken();
  return token ? { satoken: token } : {};
}

function handleUnauthorized(): void {
  clearAuth();
  uni.showToast({ title: '请先登录', icon: 'none' });
  uni.redirectTo({ url: '/pages/login/index' });
}

function unwrap<T>(payload: ApiResult<T>): T {
  if (payload.code === 401) {
    handleUnauthorized();
    throw new Error(payload.msg || '请先登录');
  }
  if (payload.code !== 200) {
    throw new Error(payload.msg || '请求失败');
  }
  return payload.data;
}

export function request<T>(options: UniRequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.request({
      ...options,
      url: withBaseUrl(options.url),
      header: {
        'content-type': 'application/json',
        ...authHeader(),
        ...(options.header || {})
      },
      success: (response) => {
        try {
          resolve(unwrap<T>(response.data as ApiResult<T>));
        } catch (error: unknown) {
          reject(error);
        }
      },
      fail: (error) => reject(new Error(error.errMsg || '网络请求失败'))
    });
  });
}

export function get<T>(url: string, data?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, data, method: 'GET' });
}

export function post<T>(url: string, data?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, data, method: 'POST' });
}

export function put<T>(url: string, data?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, data, method: 'PUT' });
}

export function upload<T>(
  url: string,
  filePath: string,
  name: string,
  formData?: Record<string, string | number>
): Promise<T> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: withBaseUrl(url),
      filePath,
      name,
      formData,
      header: authHeader(),
      success: (response) => {
        try {
          const parsed = JSON.parse(response.data) as ApiResult<T>;
          resolve(unwrap(parsed));
        } catch (error: unknown) {
          reject(error);
        }
      },
      fail: (error) => reject(new Error(error.errMsg || '文件上传失败'))
    });
  });
}

export function uploadRaw(
  url: string,
  filePath: string,
  name: string,
  formData?: Record<string, string | number>
): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: withBaseUrl(url),
      filePath,
      name,
      formData,
      header: {
        Accept: 'text/event-stream',
        ...authHeader()
      },
      success: (response) => {
        if (response.statusCode === 401) {
          handleUnauthorized();
          reject(new Error('请先登录'));
          return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error('请求失败'));
          return;
        }
        resolve(response.data);
      },
      fail: (error) => reject(new Error(error.errMsg || '文件上传失败'))
    });
  });
}

export function rawPost(url: string, data?: Record<string, unknown>): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.request({
      url: withBaseUrl(url),
      data,
      method: 'POST',
      header: {
        'content-type': 'application/json',
        Accept: 'text/event-stream',
        ...authHeader()
      },
      success: (response) => {
        if (response.statusCode === 401) {
          handleUnauthorized();
          reject(new Error('请先登录'));
          return;
        }
        if (response.statusCode < 200 || response.statusCode >= 300) {
          reject(new Error('请求失败'));
          return;
        }
        resolve(typeof response.data === 'string' ? response.data : JSON.stringify(response.data));
      },
      fail: (error) => reject(new Error(error.errMsg || '网络请求失败'))
    });
  });
}

export type SseEventHandler = (event: SseEvent) => void;

export async function streamPost(
  url: string,
  data: Record<string, unknown> | undefined,
  onEvent: SseEventHandler
): Promise<void> {
  if (!canUseFetchStream()) {
    parseSseText(await rawPost(url, data)).forEach(onEvent);
    return;
  }

  const response = await fetch(withBaseUrl(url), {
    method: 'POST',
    headers: {
      'content-type': 'application/json',
      Accept: 'text/event-stream',
      ...authHeader()
    },
    body: JSON.stringify(data || {})
  });
  await readSseResponse(response, onEvent, '网络请求失败');
}

export async function streamUpload(
  url: string,
  file: Blob | string,
  name: string,
  formData: Record<string, string | number> | undefined,
  onEvent: SseEventHandler
): Promise<void> {
  if (typeof file === 'string' || !canUseFetchStream()) {
    parseSseText(await uploadRaw(url, file as string, name, formData)).forEach(onEvent);
    return;
  }

  const body = new FormData();
  body.append(name, file, detectUploadName(file));
  Object.entries(formData || {}).forEach(([key, value]) => {
    body.append(key, String(value));
  });

  const response = await fetch(withBaseUrl(url), {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      ...authHeader()
    },
    body
  });
  await readSseResponse(response, onEvent, '文件上传失败');
}

function canUseFetchStream(): boolean {
  return typeof fetch === 'function' && typeof ReadableStream !== 'undefined';
}

async function readSseResponse(response: Response, onEvent: SseEventHandler, fallbackMessage: string): Promise<void> {
  if (response.status === 401) {
    handleUnauthorized();
    throw new Error('请先登录');
  }
  if (!response.ok) {
    throw new Error(await responseErrorMessage(response, fallbackMessage));
  }
  if (!response.body) {
    parseSseText(await response.text()).forEach(onEvent);
    return;
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder('utf-8');
  const parser = new SseStreamParser();
  while (true) {
    const { value, done } = await reader.read();
    if (done) break;
    const chunk = decoder.decode(value, { stream: true });
    parser.push(chunk).forEach(onEvent);
  }
  const tail = decoder.decode();
  if (tail) {
    parser.push(tail).forEach(onEvent);
  }
  parser.flush().forEach(onEvent);
}

async function responseErrorMessage(response: Response, fallbackMessage: string): Promise<string> {
  const text = await response.text().catch(() => '');
  if (!text) return fallbackMessage;
  try {
    const parsed = JSON.parse(text) as Partial<ApiResult<unknown>>;
    return parsed.msg || fallbackMessage;
  } catch {
    return text.length > 120 ? fallbackMessage : text;
  }
}

function detectUploadName(file: Blob): string {
  const typed = file as Blob & { name?: string };
  if (typed.name) return typed.name;
  if (file.type.includes('wav')) return 'voice.wav';
  if (file.type.includes('webm')) return 'voice.webm';
  if (file.type.includes('ogg')) return 'voice.ogg';
  if (file.type.includes('mp4')) return 'voice.m4a';
  return 'voice.wav';
}
