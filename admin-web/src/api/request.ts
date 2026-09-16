import axios from 'axios';
import { AxiosError, AxiosHeaders } from 'axios';

import type { ApiResult, QueryParams } from '../types';
import { getToken } from '../utils';

export const request = axios.create({
  baseURL: '/api',
  timeout: 20000
});

request.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    const headers = new AxiosHeaders(config.headers);
    headers.set('satoken', token);
    config.headers = headers;
  }
  return config;
});

request.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    let message = error instanceof Error ? error.message : '网络异常，请检查服务状态';
    if (error instanceof AxiosError) {
      const data = error.response?.data as Partial<ApiResult<unknown>> | undefined;
      message = data?.msg || message;
    }
    return Promise.reject(new Error(message));
  }
);

function unwrap<T>(body: ApiResult<T>): T {
  if (body.code !== 200) {
    throw new Error(body.msg || '业务请求失败');
  }
  return body.data;
}

export async function apiGet<T>(url: string, params?: QueryParams): Promise<T> {
  const response = await request.get<ApiResult<T>>(url, { params });
  return unwrap(response.data);
}

export async function apiPost<T>(url: string, data?: unknown): Promise<T> {
  const response = await request.post<ApiResult<T>>(url, data);
  return unwrap(response.data);
}

export async function apiPut<T>(url: string, data?: unknown): Promise<T> {
  const response = await request.put<ApiResult<T>>(url, data);
  return unwrap(response.data);
}

export async function apiDelete<T>(url: string): Promise<T> {
  const response = await request.delete<ApiResult<T>>(url);
  return unwrap(response.data);
}

export default request;
