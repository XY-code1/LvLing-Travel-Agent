import type { CityContextVO, CityPoiVO } from '../types';
import { searchAmapPoi } from './amapService';

export type ScenicCategory = '核心景观' | '历史文化' | '摄影打卡' | '建筑艺术' | '自然风光' | '文化演艺';

export interface ScenicSpot {
  id: string; cityId: number | null; cityCode: string; scenicId: number | null;
  name: string; category: ScenicCategory; description: string; address: string;
  longitude: number | null; latitude: number | null; coverImage: string; images: string[];
  recommendedDuration: number; recommendationLevel: number; popularity: number;
  openingHours: string | null; ticketInfo: string | null; distanceMeters: number | null;
  source: 'local' | 'amap' | 'demo'; imageSource: 'provider' | 'local-asset' | 'city-fallback' | 'placeholder';
  sourceUrl: string | null; updatedAt: string | null;
}

const memoryCache = new Map<string, ScenicSpot[]>();
const placeholder = '/static/images/vision-placeholder.webp';
const exactLocalImages: Record<string, string> = {
  '灵山大佛': '/static/images/spot-lingshan-buddha.webp',
  '九龙灌浴': '/static/images/spot-jiulong-guanyu.webp',
  '灵山梵宫': '/static/images/spot-lingshan-palace.webp',
  '五印坛城': '/static/images/spot-wuyin-tancheng.webp'
};
const exactCategories: Partial<Record<string, ScenicCategory>> = {
  '灵山大佛': '核心景观', '九龙灌浴': '文化演艺', '灵山梵宫': '建筑艺术', '五印坛城': '摄影打卡'
};

export async function loadScenicSpots(context: CityContextVO | null): Promise<ScenicSpot[]> {
  if (!context?.city) return [];
  const cityCode = context.city.cityCode || context.adcode || context.city.adcode || 'unknown';
  const cacheKey = `${cityCode}:all:`;
  const cached = memoryCache.get(cacheKey);
  if (cached) return cached;
  let pois = context.pois || [];
  if (!pois.length) {
    let remote: Awaited<ReturnType<typeof searchAmapPoi>>;
    try { remote = await searchAmapPoi('风景名胜', context.city.cityName); }
    catch (error) { console.warn('[DataSource] source=demo domain=poi reason=provider-failed', error); remote = demoHangzhouSpots().map(p => ({ id: p.id, name: p.name, address: p.address || '', type: p.type || '景点', distanceMeters: null, coordinates: { longitude: p.longitude!, latitude: p.latitude! } })); }
    pois = remote.map((poi) => ({
      id: poi.id, name: poi.name, address: poi.address || null, type: poi.type || '风景名胜',
      distanceMeters: poi.distanceMeters ?? null,
      longitude: poi.coordinates.longitude, latitude: poi.coordinates.latitude,
      images: null, source: 'amap' as const
    }));
  }
  const spots = pois.map((poi, index) => fromPoi(poi, context, index));
  memoryCache.set(cacheKey, spots);
  return spots;
}

function demoHangzhouSpots(): CityPoiVO[] {
  return [['demo-west-lake','西湖',120.1551,30.2741,'杭州市西湖区西湖风景名胜区'],['demo-lingyin','灵隐寺',120.1014,30.2405,'杭州市西湖区法云弄1号'],['demo-xixi','西溪湿地',120.0635,30.2727,'杭州市西湖区天目山路518号'],['demo-silk','中国丝绸博物馆',120.1615,30.2244,'杭州市西湖区玉皇山路73-1号'],['demo-zhejiang','浙江省博物馆',120.1485,30.2423,'杭州市西湖区孤山路25号']].map(([id,name,longitude,latitude,address]) => ({ id: String(id), name: String(name), address: String(address), type: '景点', distanceMeters: null, longitude: Number(longitude), latitude: Number(latitude), images: null, source: 'demo' }));
}

function fromPoi(poi: CityPoiVO, context: CityContextVO, index: number): ScenicSpot {
  const providerImages = parseImages(poi.images);
  const localAsset = exactLocalImages[poi.name];
  const cityFallback = safeImage(context.city?.coverImage);
  const images = [...new Set([...providerImages, ...(localAsset ? [localAsset] : []),
    ...(cityFallback ? [cityFallback] : []), placeholder])];
  const imageSource: ScenicSpot['imageSource'] = providerImages.length ? 'provider'
    : localAsset ? 'local-asset' : cityFallback ? 'city-fallback' : 'placeholder';
  const tags = poi.type || '';
  return {
    id: poi.id, cityId: context.city?.id ?? null,
    cityCode: context.city?.cityCode || context.adcode || '', scenicId: context.scenicAreas?.[0]?.id || null,
    name: poi.name, category: inferCategory(tags, poi.name),
    description: poi.address || `${poi.name}是${context.city?.cityName || '当前城市'}值得探索的旅行目的地。`,
    address: poi.address || '地址以景区现场信息为准', longitude: poi.longitude, latitude: poi.latitude,
    coverImage: images[0], images, recommendedDuration: inferDuration(tags),
    recommendationLevel: Math.max(3, 5 - Math.floor(index / 4)), popularity: Math.max(60, 100 - index * 4),
    openingHours: null, ticketInfo: null, distanceMeters: poi.distanceMeters, source: poi.source,
    imageSource, sourceUrl: providerImages[0] || null, updatedAt: null
  };
}

function parseImages(value?: string | null): string[] {
  if (!value?.trim()) return [];
  const raw = value.trim();
  if (raw.startsWith('[')) {
    try {
      const parsed: unknown = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed.filter((item): item is string => typeof item === 'string').map(safeImage).filter(Boolean) as string[];
    } catch { /* fall through to delimited values */ }
  }
  return raw.split(/[,;|]/).map(safeImage).filter(Boolean) as string[];
}

function safeImage(value?: string | null): string | null {
  const image = value?.trim();
  if (!image || image.startsWith('data:') || image.startsWith('javascript:')) return null;
  return image;
}

function inferCategory(tags: string, name: string): ScenicCategory {
  if (exactCategories[name]) return exactCategories[name]!;
  const value = `${tags},${name}`;
  if (/演艺|演出|表演|灌浴|剧场/.test(value)) return '文化演艺';
  if (/建筑|宫|塔|城墙|坛城/.test(value)) return '建筑艺术';
  if (/历史|文化|寺|庙|博物馆|兵马俑/.test(value)) return '历史文化';
  if (/摄影|打卡|桥|街/.test(value)) return '摄影打卡';
  if (/自然|山|湖|湿地|公园|风光/.test(value)) return '自然风光';
  return '核心景观';
}

function inferDuration(tags: string): number {
  if (/深度|博物馆|湿地/.test(tags)) return 120;
  if (/演艺|表演/.test(tags)) return 45;
  if (/轻松|摄影|打卡/.test(tags)) return 60;
  return 90;
}
