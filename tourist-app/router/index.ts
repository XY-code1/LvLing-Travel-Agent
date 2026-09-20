import { isLoggedIn } from '../utils/auth';

/**
 * 游客端一级导航的统一入口。
 * 暂未独立成页的产品能力继续复用现有页面，避免 UI-1 破坏业务链路。
 */
export const APP_ROUTES = {
  home: '/pages/index/index',
  aiTravel: '/pages/ai-travel/index',
  aiGuide: '/pages/chat/index',
  cities: '/pages/cities/index',
  spots: '/pages/spots/index',
  planner: '/pages/route/index',
  services: '/pages/services/index',
  announcements: '/pages/announcements/index',
  sos: '/pages/sos/index',
  inspiration: '/pages/inspiration/index',
  vision: '/pages/vision/index',
  profile: '/pages/profile/index',
  history: '/pages/history/index'
} as const;

export type PrimaryRouteKey = 'home' | 'aiGuide' | 'cities' | 'spots' | 'planner' | 'services' | 'announcements' | 'sos';

export const PRIMARY_ROUTES: Array<{ key: PrimaryRouteKey; label: string; url: string }> = [
  { key: 'home', label: '首页', url: APP_ROUTES.home },
  { key: 'aiGuide', label: 'AI导览', url: APP_ROUTES.aiGuide },
  { key: 'cities', label: '城市探索', url: APP_ROUTES.cities },
  { key: 'spots', label: '景点', url: APP_ROUTES.spots },
  { key: 'planner', label: '路线规划', url: APP_ROUTES.planner },
  { key: 'services', label: '服务查询', url: APP_ROUTES.services },
  { key: 'announcements', label: '公告', url: APP_ROUTES.announcements },
  { key: 'sos', label: 'SOS求助', url: APP_ROUTES.sos }
];

export function currentPrimaryRoute(): PrimaryRouteKey | null {
  const pages = getCurrentPages();
  const route = pages[pages.length - 1]?.route || 'pages/index/index';
  return PRIMARY_ROUTES.find((item) => item.url.split('?')[0].replace(/^\//, '') === route)?.key || null;
}

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
