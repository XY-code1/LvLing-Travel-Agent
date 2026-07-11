import type { TouristInfoVO } from '../types';

const TOKEN_KEY = 'guido_tourist_token';
const USER_KEY = 'guido_tourist_user';

export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || '';
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token);
}

export function clearToken(): void {
  uni.removeStorageSync(TOKEN_KEY);
}

export function getStoredUser(): TouristInfoVO | null {
  const value = uni.getStorageSync(USER_KEY);
  return value ? (value as TouristInfoVO) : null;
}

export function setStoredUser(user: TouristInfoVO): void {
  uni.setStorageSync(USER_KEY, user);
}

export function clearStoredUser(): void {
  uni.removeStorageSync(USER_KEY);
}

export function isLoggedIn(): boolean {
  return Boolean(getToken());
}

export function requireLogin(): boolean {
  if (isLoggedIn()) {
    return true;
  }
  uni.redirectTo({ url: '/pages/login/index' });
  return false;
}

export function clearAuth(): void {
  clearToken();
  clearStoredUser();
}
