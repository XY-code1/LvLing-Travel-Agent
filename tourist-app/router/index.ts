import { isLoggedIn } from '../utils/auth';

const protectedPages = new Set([
  '/pages/index/index',
  '/pages/chat/index',
  '/pages/vision/index',
  '/pages/route/index',
  '/pages/spot/index',
  '/pages/history/index',
  '/pages/profile/index',
  '/pages/feedback/index'
]);

export function canEnter(url: string): boolean {
  return !protectedPages.has(url.split('?')[0]) || isLoggedIn();
}

export function navigateTo(url: string): void {
  if (!canEnter(url)) {
    uni.redirectTo({ url: '/pages/login/index' });
    return;
  }
  uni.navigateTo({ url });
}

export function switchToHome(): void {
  if (!isLoggedIn()) {
    uni.redirectTo({ url: '/pages/login/index' });
    return;
  }
  uni.redirectTo({ url: '/pages/index/index' });
}
