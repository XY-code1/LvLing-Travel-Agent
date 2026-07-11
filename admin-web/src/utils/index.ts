const ADMIN_TOKEN_KEY = 'guido_admin_token';

export function getToken(): string {
  return localStorage.getItem(ADMIN_TOKEN_KEY) ?? '';
}

export function setToken(token: string): void {
  localStorage.setItem(ADMIN_TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(ADMIN_TOKEN_KEY);
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return '请求失败，请稍后重试';
}

export function formatPercent(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return '--';
  }
  return `${value}%`;
}
