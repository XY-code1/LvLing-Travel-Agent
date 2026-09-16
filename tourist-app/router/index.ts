import { isLoggedIn } from '../utils/auth';

/**
 * 游客端一级导航的统一入口。
 * 暂未独立成页的产品能力继续复用现有页面，避免 UI-1 破坏业务链路。
 */
export const APP_ROUTES = {
  home: '/pages/index/index',
  planner: '/pages/chat/index',
  cities: '/pages/index/index?section=cities',
  spot: '/pages/index/index?section=cities',
  route: '/pages/route/index',
  services: '/pages/chat/index?mode=services',
  inspiration: '/pages/inspiration/index',
  vision: '/pages/vision/index',
  profile: '/pages/profile/index',
  history: '/pages/history/index'
} as const;

const protectedPages = new Set([
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
  uni.redirectTo({ url: APP_ROUTES.home });
}
